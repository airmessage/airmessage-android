package me.tagavari.airmessage.compose.component

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.IntentHelper
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.messaging.QueuedFile

private const val messageLengthButtonsCollapse = 16
private const val messageLengthButtonsExpand = 12

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MessageInputBarText(
	messageText: String,
	onMessageTextChange: (String) -> Unit,
	attachments: List<QueuedFile>,
	onRemoveAttachment: (QueuedFile) -> Unit,
	collapseButtons: Boolean,
	onChangeCollapseButtons: (Boolean) -> Unit,
	onTakePhoto: () -> Unit,
	onOpenContentPicker: () -> Unit,
	onStartAudioRecording: () -> Unit,
	@ServiceHandler serviceHandler: Int?,
	@ServiceType serviceType: String?,
	onSend: () -> Unit
) {
	val validTextFieldValue = remember(messageText) {
		messageText.isNotBlank()
	}
	
	val canSend = validTextFieldValue || attachments.isNotEmpty()
	
	//Automatically expand or collapse the buttons
	//depending on how long a message the user has entered
	val currentOnChangeCollapseButtons by rememberUpdatedState(onChangeCollapseButtons)
	LaunchedEffect(messageText, attachments.isNotEmpty()) {
		if(messageText.length > messageLengthButtonsCollapse || attachments.isNotEmpty()) {
			currentOnChangeCollapseButtons(true)
		} else if(messageText.length < messageLengthButtonsExpand) {
			currentOnChangeCollapseButtons(false)
		}
	}
	
	Row(
		verticalAlignment = Alignment.Bottom
	) {
		CompositionLocalProvider(
			LocalMinimumTouchTargetEnforcement provides false,
			LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
		) {
			AnimatedContent(
				targetState = collapseButtons,
				transitionSpec = {
					val slideSpring = spring(
						dampingRatio = Spring.DampingRatioLowBouncy,
						stiffness = Spring.StiffnessMediumLow,
						visibilityThreshold = IntOffset.VisibilityThreshold
					)
					
					if(targetState) {
						//Slide right to left
						slideInHorizontally(
							animationSpec = slideSpring,
							initialOffsetX = { width -> width * 2 }
						) +
								fadeIn() with
								slideOutHorizontally(
									animationSpec = slideSpring,
									targetOffsetX = { width -> -width * 2 }
								) +
								fadeOut()
					} else {
						//Slide left to right
						slideInHorizontally(
							animationSpec = slideSpring,
							initialOffsetX = { width -> -width * 2 }
						) +
								fadeIn() with
								slideOutHorizontally(
									animationSpec = slideSpring,
									targetOffsetX = { width -> width * 2 }
								) +
								fadeOut()
					}.using(
						//Disable clipping since the faded slide-in/out should
						//be displayed out of bounds
						SizeTransform(
							clip = false,
							sizeAnimationSpec = { _, _ -> spring(
								dampingRatio = Spring.DampingRatioLowBouncy,
								stiffness = Spring.StiffnessMediumLow,
								visibilityThreshold = IntSize.VisibilityThreshold
							) }
						)
					)
				}
			) { collapseButtons ->
				if(collapseButtons) {
					IconButton(
						modifier = Modifier.padding(end = 4.dp),
						onClick = { onChangeCollapseButtons(false) }
					) {
						Icon(painterResource(id = R.drawable.square_expand), contentDescription = "")
					}
				} else {
					Row {
						IconButton(
							modifier = Modifier.padding(end = 4.dp),
							onClick = onTakePhoto,
						) {
							Icon(Icons.Outlined.PhotoCamera, contentDescription = "")
						}
						
						IconButton(
							modifier = Modifier.padding(end = 4.dp),
							onClick = onOpenContentPicker
						) {
							Icon(Icons.Outlined.AddCircleOutline, contentDescription = "")
						}
					}
				}
			}
		}
		
		var inputFieldFocus by remember { mutableStateOf(false) }
		
		//Input field
		Surface(
			modifier = Modifier
				.border(
					if(inputFieldFocus) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
					else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
					RoundedCornerShape(20.dp)
				)
				.clip(RoundedCornerShape(20.dp))
		) {
			Column {
				AnimatedVisibility(
					visible = attachments.isNotEmpty(),
					enter = expandVertically() + fadeIn(),
					exit = shrinkVertically() + fadeOut()
				) {
					val context = LocalContext.current
					
					AttachmentQueueRow(
						attachments = attachments,
						onClick = { attachment ->
							//Open the file
							attachment.file.accept(
								consumerA = { uri -> IntentHelper.launchUri(context, uri)},
								consumerB = { file -> IntentHelper.openAttachmentFile(context, file, attachment.fileType) },
							)
						},
						onRemove = onRemoveAttachment
					)
				}
				
				Row {
					BasicTextField(
						value = messageText,
						onValueChange = onMessageTextChange,
						modifier = Modifier
							.weight(1F)
							.heightIn(min = 40.dp, max = 100.dp)
							.padding(horizontal = 12.dp, vertical = 8.dp)
							.align(Alignment.CenterVertically)
							.onFocusChanged { focusState ->
								inputFieldFocus = focusState.isFocused || focusState.hasFocus
							},
						textStyle = MaterialTheme.typography.bodyLarge.copy(
							fontSize = 16.sp,
							color = LocalContentColor.current
						),
						cursorBrush = SolidColor(LocalContentColor.current),
						decorationBox = { innerTextField ->
							//Display the current service as a placeholder
							if(messageText.isEmpty()) {
								val placeholder = LanguageHelper.getMessageFieldPlaceholder(
									LocalContext.current.resources,
									serviceHandler,
									serviceType
								)
								Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
							}
							
							innerTextField()
						}
					)
					
					//Send button
					CompositionLocalProvider(
						LocalMinimumTouchTargetEnforcement provides false,
					) {
						if(canSend) {
							IconButton(
								onClick = onSend,
								modifier = Modifier.align(Alignment.Bottom),
								colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
							) {
								Icon(
									painter = painterResource(id = R.drawable.push_rounded),
									contentDescription = stringResource(id = R.string.action_send)
								)
							}
						} else {
							Icon(
								modifier = Modifier
									.padding(8.dp)
									.pointerInput(Unit) {
										forEachGesture {
											awaitPointerEventScope {
												awaitFirstDown()
												onStartAudioRecording()
											}
										}
									},
								painter = painterResource(id = R.drawable.record_circle),
								contentDescription = stringResource(id = R.string.action_recordaudio),
								tint = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}
				}
			}
		}
	}
}
