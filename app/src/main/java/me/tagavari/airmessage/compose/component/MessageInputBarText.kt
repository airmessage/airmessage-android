package me.tagavari.airmessage.compose.component

import android.util.Pair
import android.util.TypedValue
import android.widget.EditText
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import me.tagavari.airmessage.R
import me.tagavari.airmessage.container.ReadableBlob
import me.tagavari.airmessage.container.ReadableBlobContentInfo
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
	onInputContent: (List<ReadableBlob>) -> Unit,
	attachmentsScrollState: ScrollState = rememberScrollState(),
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
								consumerA = { blob -> context.startActivity(blob.getOpenIntent(context)) },
								consumerB = { file -> IntentHelper.openAttachmentFile(context, file, attachment.fileType) },
							)
						},
						onRemove = onRemoveAttachment,
						scrollState = attachmentsScrollState
					)
				}
				
				Row {
					/* BasicTextField(
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
					) */
					
					val context = LocalContext.current
					MaterialTheme.typography.labelMedium.letterSpacing
					val textColor = MaterialTheme.colorScheme.onSurface
					val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
					AndroidView(
						modifier = Modifier
							.weight(1F)
							.heightIn(min = 40.dp, max = 100.dp)
							.padding(horizontal = 12.dp, vertical = 8.dp)
							.align(Alignment.CenterVertically),
						factory = { ctx ->
							EditText(ctx).apply {
								addTextChangedListener { text ->
									text?.toString()?.let(onMessageTextChange)
								}
								
								setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
								letterSpacing
								background = null
								setPadding(0, 0, 0, 0)
								setOnFocusChangeListener { _, hasFocus ->
									inputFieldFocus = hasFocus
								}
								
								ViewCompat.setOnReceiveContentListener(
									this,
									arrayOf("image/*")
								) { _, contentInfo ->
									val split: Pair<ContentInfoCompat?, ContentInfoCompat?> = contentInfo.partition { it.uri != null }
									val (uriPayloads, otherPayloads) = split
									
									if(uriPayloads != null) {
										val contentList = List(uriPayloads.clip.itemCount) { i -> ReadableBlobContentInfo(contentInfo, i) }
										onInputContent(contentList)
									}
									
									// Return anything that we didn't handle ourselves. This preserves the default platform
									// behavior for text and anything else for which we are not implementing custom handling.
									otherPayloads
								}
							}
						},
						update = { editText ->
							//Update the text color
							editText.setTextColor(textColor.toArgb())
							editText.setHintTextColor(placeholderColor.toArgb())
							
							//Update the text
							if(editText.text!!.toString() != messageText) {
								editText.setText(messageText)
							}
							
							//Update the placeholder
							editText.hint = LanguageHelper.getMessageFieldPlaceholder(
								context.resources,
								serviceHandler,
								serviceType
							)
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

@Preview
@Composable
private fun PreviewMessageInputBarText() {
	Surface {
		Box(modifier = Modifier.padding(8.dp)) {
			MessageInputBarText(
				messageText = "",
				onMessageTextChange = {},
				attachments = listOf(),
				onRemoveAttachment = {},
				onInputContent = {},
				onTakePhoto = {},
				onOpenContentPicker = {},
				collapseButtons = false,
				onChangeCollapseButtons = {},
				onStartAudioRecording = {},
				serviceHandler = ServiceHandler.appleBridge,
				serviceType = ServiceType.appleMessage,
				onSend = {}
			)
		}
	}
}
