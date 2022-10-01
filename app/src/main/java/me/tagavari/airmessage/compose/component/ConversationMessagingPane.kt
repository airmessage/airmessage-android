package me.tagavari.airmessage.compose.component

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.FoldingFeature
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.state.ConversationsDetailPage
import me.tagavari.airmessage.compose.state.ConversationsSinglePaneTarget
import me.tagavari.airmessage.compose.state.ConversationsViewModel
import me.tagavari.airmessage.helper.ProgressState
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMassRetrieval
import soup.compose.material.motion.MaterialFadeThrough
import soup.compose.material.motion.MaterialSharedAxisX

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ConversationMessagingPane(
	devicePosture: FoldingFeature? = null,
	windowSizeClass: WindowSizeClass
) {
	val context = LocalContext.current
	val viewModel = viewModel<ConversationsViewModel>()
	
	//Pane states
	val useSplitPane = remember(windowSizeClass) {
		windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
	}
	
	val scrollStateConversations = rememberLazyListState()
	val topAppBarStateConversations = key(useSplitPane) { rememberTopAppBarState() }
	
	val scrollStateConversationsArchived = rememberLazyListState()
	val topAppBarStateConversationsArchived = rememberTopAppBarState()
	
	val scrollStateDetail = key(viewModel.lastSelectedDetailPage.collectAsState()) {
		rememberLazyListState()
	}
	
	val syncState by remember {
		ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow()
			.mapNotNull { event ->
				when(event) {
					is ReduxEventMassRetrieval.Start -> ProgressState.Indeterminate
					is ReduxEventMassRetrieval.Progress -> ProgressState.Determinate(
						event.receivedItems.toFloat() / event.totalItems.toFloat()
					)
					else -> null
				}
			}
	}.collectAsState(initial = null)
	
	val conversationPaneProps = ConversationPaneProps(
		scrollState = scrollStateConversations,
		topAppBarState = topAppBarStateConversations,
		
		isArchived = viewModel.showArchivedConversations,
		isFloatingPane = useSplitPane,
		
		conversations = viewModel.conversations,
		activeConversationID = (viewModel.detailPage as? ConversationsDetailPage.Messaging)?.conversationID,
		syncState = syncState,
		
		onNavigateBack = {
			when {
				viewModel.showHelpPane -> viewModel.showHelpPane = false
				viewModel.showArchivedConversations -> viewModel.showArchivedConversations = false
			}
		},
		onSelectConversation = { conversationID ->
			viewModel.detailPage = ConversationsDetailPage.Messaging(conversationID)
		},
		onReloadConversations = {
			viewModel.loadConversations()
		},
		onNavigateArchived = {
			viewModel.showArchivedConversations = true
		},
		onNavigateSettings = {
			context.startActivity(Intent(context, Preferences::class.java))
		},
		onNavigateHelp = {
			viewModel.showHelpPane = true
		},
		onNewConversation = {
			viewModel.detailPage = ConversationsDetailPage.NewConversation
		},
		onMarkConversationsAsRead = {
			viewModel.markConversationsAsRead()
		}
	)
	
	if(useSplitPane) {
		val hingeBounds = devicePosture?.bounds
		
		val hingeOffset: Dp
		val hingeWidth: Dp
		if(hingeBounds != null) {
			with(LocalDensity.current) {
				hingeOffset = hingeBounds.left.toDp()
				hingeWidth = hingeBounds.width().toDp()
			}
		} else {
			//Hardcode for non-foldables
			hingeOffset = 384.dp
			hingeWidth = 0.dp
		}
		
		//Show the conversation list on the left and the detail
		//view on the right
		Row(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.inverseOnSurface)
				.fillMaxSize()
		) {
			ConversationPane(
				modifier = Modifier.width(hingeOffset),
				props = conversationPaneProps
			)
			
			Spacer(modifier = Modifier.width(hingeWidth))
			
			MaterialFadeThrough(
				targetState = viewModel.detailPage,
			) { detailPage ->
				val useFloatingPane = devicePosture?.isSeparating != true
				
				Box(
					modifier = if(useFloatingPane) {
						Modifier
							.statusBarsPadding()
							.padding(end = 16.dp)
							.clip(
								RoundedCornerShape(
									topStart = 20.dp,
									topEnd = 20.dp
								)
							)
					} else {
						Modifier
					}
				) {
					when(detailPage) {
						is ConversationsDetailPage.Messaging -> {
							val activeConversationID = detailPage.conversationID
							
							key(activeConversationID) {
								MessagingScreen(
									conversationID = activeConversationID,
									floatingPane = useFloatingPane,
									receivedContentFlow = viewModel.getPendingReceivedContentFlowForConversation(activeConversationID),
									scrollState = scrollStateDetail,
									onProcessedReceivedContent = { viewModel.setPendingReceivedContent(null) }
								)
							}
						}
						is ConversationsDetailPage.NewConversation -> {
							NewConversationPane(
								onSelectConversation = { conversation ->
									viewModel.updatePendingReceivedContentTarget(conversation.localID)
									viewModel.detailPage = ConversationsDetailPage.Messaging(conversation.localID)
								}
							)
						}
						null -> {}
					}
				}
			}
		}
	} else {
		//Handle back presses
		BackHandler(
			enabled = viewModel.detailPage != null
					|| viewModel.showArchivedConversations
		) {
			when {
				viewModel.detailPage != null ->
					viewModel.detailPage = null
				viewModel.showArchivedConversations ->
					viewModel.showArchivedConversations = false
			}
		}
		
		//Keep track of if we're navigating forwards or backwards
		val isNavigatingForwards by remember {
			snapshotFlow { viewModel.singlePaneTarget }
				.runningFold(emptyList<ConversationsSinglePaneTarget>()) { accumulator, value ->
					//Capture the last 2 items
					if(accumulator.size < 2) {
						accumulator + value
					} else {
						listOf(accumulator[1], value)
					}
				}
				.map { lastScreens ->
					if(lastScreens.size < 2) {
						//Always assume we're navigating forwards
						true
					} else {
						val screenFrom = lastScreens[0]
						val screenTo = lastScreens[1]
						
						screenTo.depth > screenFrom.depth
					}
				}
		}.collectAsState(initial = true)
		
		//Transition between pages individually
		MaterialSharedAxisX(
			modifier = Modifier.background(MaterialTheme.colorScheme.background),
			targetState = viewModel.singlePaneTarget,
			forward = isNavigatingForwards,
		) { target ->
			when(target) {
				is ConversationsSinglePaneTarget.Conversations -> {
					ConversationPane(
						props = conversationPaneProps.copy(
							isArchived = false
						)
					)
				}
				is ConversationsSinglePaneTarget.ArchivedConversations -> {
					ConversationPane(
						props = conversationPaneProps.copy(
							isArchived = true,
							scrollState = scrollStateConversationsArchived,
							topAppBarState = topAppBarStateConversationsArchived
						)
					)
				}
				is ConversationsSinglePaneTarget.Detail -> {
					when(val detailPage = target.page) {
						is ConversationsDetailPage.Messaging -> {
							val activeConversationID = detailPage.conversationID
							
							key(detailPage) {
								MessagingScreen(
									conversationID = activeConversationID,
									navigationIcon = {
										IconButton(onClick = { viewModel.detailPage = null }) {
											Icon(
												imageVector = Icons.Filled.ArrowBack,
												contentDescription = stringResource(id = R.string.action_back)
											)
										}
									},
									receivedContentFlow = viewModel.getPendingReceivedContentFlowForConversation(activeConversationID),
									scrollState = scrollStateDetail,
									onProcessedReceivedContent = { viewModel.setPendingReceivedContent(null) }
								)
							}
						}
						is ConversationsDetailPage.NewConversation -> {
							NewConversationPane(
								navigationIcon = {
									IconButton(onClick = { viewModel.detailPage = null }) {
										Icon(
											imageVector = Icons.Filled.ArrowBack,
											contentDescription = stringResource(id = R.string.action_back)
										)
									}
								},
								onSelectConversation = { conversation ->
									viewModel.updatePendingReceivedContentTarget(conversation.localID)
									viewModel.detailPage = ConversationsDetailPage.Messaging(conversation.localID)
								}
							)
						}
					}
				}
			}
		}
	}
	
	//Help dialog
	if(viewModel.showHelpPane) {
		HelpPane(
			onDismissRequest = { viewModel.showHelpPane = false }
		)
	}
}