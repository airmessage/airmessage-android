package me.tagavari.airmessage.compose.component

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.tagavari.airmessage.activity.MediaViewer
import me.tagavari.airmessage.compose.state.MessageLazyLoadState
import me.tagavari.airmessage.compose.state.MessageSelectionState
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.constants.TimingConstants
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.messaging.*
import me.tagavari.airmessage.util.MessageFlow
import me.tagavari.airmessage.util.MessageFlowSpacing

private const val scrollProgressRateLimit = 100

@Composable
fun MessageList(
	modifier: Modifier = Modifier,
	conversation: ConversationInfo,
	messages: List<ConversationItem>,
	messageStateIndices: Collection<Int>,
	messageSelectionState: MessageSelectionState,
	scrollState: LazyListState = rememberLazyListState(),
	onDownloadAttachment: (MessageInfo, AttachmentInfo) -> Unit,
	onLoadPastMessages: () -> Unit,
	lazyLoadState: MessageLazyLoadState,
	actionSuggestions: List<AMConversationAction>,
	onSelectActionSuggestion: (AMConversationAction) -> Unit,
	isPlayingEffect: Boolean,
	onPlayEffect: (String) -> Unit
) {
	val scrollOffsetMap by remember {
		var scrollProgressModificationTime: Long? = null
		var lastScrollOffsetMap: Map<Long, Float>? = null
		
		derivedStateOf {
			val timeNow = SystemClock.uptimeMillis() / scrollProgressRateLimit
			
			val visibleItemsInfo = scrollState.layoutInfo.visibleItemsInfo
			val height = scrollState.layoutInfo.viewportEndOffset.toFloat()
			
			if(scrollProgressModificationTime != timeNow) {
				scrollProgressModificationTime = timeNow
				
				lastScrollOffsetMap = visibleItemsInfo
					.filter { it.key is Long }
					.associate { it.key as Long to (it.offset / height).coerceIn(0F, 1F) }
			}
			
			lastScrollOffsetMap!!
		}
	}
	
	val endOfListReached by remember {
		derivedStateOf {
			scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == scrollState.layoutInfo.totalItemsCount - 1
		}
	}
	
	val currentOnLoadPastMessages by rememberUpdatedState(onLoadPastMessages)
	
	LaunchedEffect(endOfListReached) {
		currentOnLoadPastMessages()
	}
	
	//Remember the horizontal drag state
	val dragX = remember { Animatable(0F) }
	val dragXState by dragX.asState()
	
	//Track left drags with 50% friction
	val density = LocalDensity.current
	val dragProgress by remember {
		derivedStateOf {
			with(density) {
				((dragXState * -0.5F) / MessageList.dragThreshold.toPx()).coerceIn(0F, 1F)
			}
		}
	}
	
	val scope = rememberCoroutineScope()
	
	LazyColumn(
		modifier = modifier
			.draggable(
				orientation = Orientation.Horizontal,
				state = rememberDraggableState { delta ->
					scope.launch {
						dragX.snapTo(dragX.value + delta)
					}
				},
				onDragStopped = {
					scope.launch {
						dragX.animateTo(
							targetValue = 0F,
							animationSpec = spring(stiffness = Spring.StiffnessLow)
						)
					}
				}
			),
		reverseLayout = true,
		state = scrollState,
		contentPadding = PaddingValues(8.dp)
	) {
		if(actionSuggestions.isNotEmpty()) {
			item {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 16.dp)
						.horizontalScroll(rememberScrollState()),
					horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.End)
				) {
					actionSuggestions.map { action ->
						MessageSuggestionChip(
							action = action,
							onClick = { onSelectActionSuggestion(action) }
						)
					}
				}
			}
		}
		
		//Message items
		items(
			key = { messages[messages.lastIndex - it].localID },
			count = messages.size
		) { reversedIndex ->
			val adjustedIndex = messages.lastIndex - reversedIndex
			val conversationItem = messages[adjustedIndex]
			
			if(conversationItem is MessageInfo) {
				val messageAbove = messages.getOrNull(adjustedIndex - 1)
				val messageBelow = messages.getOrNull(adjustedIndex + 1)
				
				val flow = remember(conversationItem, messageAbove, messageBelow) {
					MessageFlow(
						anchorTop = messageAbove is MessageInfo
								&& conversationItem.sender == messageAbove.sender
								&& (conversationItem.date - messageAbove.date) < TimingConstants.conversationBurstTimeMillis,
						anchorBottom = messageBelow is MessageInfo
								&& conversationItem.sender == messageBelow.sender
								&& (messageBelow.date - conversationItem.date) < TimingConstants.conversationBurstTimeMillis
					)
				}
				val spacing = when {
					messageAbove == null -> MessageFlowSpacing.NONE
					flow.anchorTop -> MessageFlowSpacing.RELATED
					else -> MessageFlowSpacing.GAP
				}
				val showTimeDivider = remember(conversationItem, messageAbove) {
					messageAbove is MessageInfo
							&& (conversationItem.date - messageAbove.date) > TimingConstants.conversationSessionTimeMillis
				}
				val scrollProgress = scrollOffsetMap[conversationItem.localID] ?: 1F
				
				val context = LocalContext.current
				
				MessageInfoListEntry(
					conversationInfo = conversation,
					messageInfo = conversationItem,
					flow = flow,
					selectionState = messageSelectionState,
					showTimeDivider = showTimeDivider,
					showStatus = messageStateIndices.contains(adjustedIndex),
					spacing = spacing,
					scrollProgress = scrollProgress,
					horizontalDragProgress = dragProgress,
					onDownloadAttachment = onDownloadAttachment,
					onOpenVisualAttachment = { targetAttachment ->
						//Collect attachment files
						val loadedAttachments = messages
							.filterIsInstance<MessageInfo>()
							.flatMap { it.attachments }
							.filter { attachment ->
								attachment.file != null
										&& (FileHelper.compareMimeTypes(attachment.computedContentType, MIMEConstants.mimeTypeImage)
										|| FileHelper.compareMimeTypes(attachment.computedContentType, MIMEConstants.mimeTypeVideo))
							}
							.let { ArrayList(it) }
						
						//Get the target attachment index
						val index = loadedAttachments.indexOf(targetAttachment)
						if(index == -1) {
							Log.e("MessageList", "Failed to match requested attachment file ${targetAttachment.localID} (${targetAttachment.file})")
							return@MessageInfoListEntry
						}
						
						//Open the media viewer
						Intent(context, MediaViewer::class.java).apply {
							putParcelableArrayListExtra(MediaViewer.intentParamDataList, loadedAttachments)
							putExtra(MediaViewer.intentParamIndex, index)
						}.let { context.startActivity(it) }
					},
					isPlayingEffect = isPlayingEffect,
					onPlayEffect = onPlayEffect
				)
			} else if(conversationItem is ConversationAction) {
				ConversationActionListEntry(
					conversationAction = conversationItem
				)
			}
		}
		
		//Loading indicator
		if(lazyLoadState == MessageLazyLoadState.LOADING) {
			item {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp)
				) {
					CircularProgressIndicator(
						modifier = Modifier.align(Alignment.Center)
					)
				}
			}
		}
	}
}

object MessageList {
	//The padding to apply around list items
	val innerPadding = 8.dp
	
	//The distance the user has to drag to fully reveal time indicators
	val dragThreshold = 56.dp
}
