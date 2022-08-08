package me.tagavari.airmessage.compose.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.LanguageHelper

private const val messageLengthButtonsCollapse = 16
private const val messageLengthButtonsExpand = 12

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MessageInputBar(
	modifier: Modifier = Modifier,
	onMessageSent: (String) -> Unit,
	showContentPicker: Boolean,
	onChangeShowContentPicker: (Boolean) -> Unit,
	collapseButtons: Boolean = false,
	onChangeCollapseButtons: (Boolean) -> Unit,
	@ServiceHandler serviceHandler: Int?,
	@ServiceType serviceType: String?,
	floating: Boolean = false,
) {
	var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
	
	val cleanTextFieldValue by remember {
		derivedStateOf {
			textFieldValue.text.trim()
		}
	}
	
	val validTextFieldValue by remember {
		derivedStateOf {
			cleanTextFieldValue.isNotEmpty()
		}
	}
	
	//Automatically expand or collapse the buttons
	//depending on how long a message the user has entered
	val currentOnChangeCollapseButtons by rememberUpdatedState(onChangeCollapseButtons)
	LaunchedEffect(textFieldValue.text) {
		if(textFieldValue.text.length < messageLengthButtonsExpand) {
			currentOnChangeCollapseButtons(false)
		} else if(textFieldValue.text.length > messageLengthButtonsCollapse) {
			currentOnChangeCollapseButtons(true)
		}
	}
	
	val surfaceElevation by animateDpAsState(
		if(floating) 2.dp else 0.dp
	)
	
	Surface(
		tonalElevation = surfaceElevation
	) {
		Box(modifier = modifier.padding(8.dp)) {
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
									onClick = {}
								) {
									Icon(Icons.Outlined.PhotoCamera, contentDescription = "")
								}
								
								IconToggleButton(
									modifier = Modifier.padding(end = 4.dp),
									checked = showContentPicker,
									onCheckedChange = onChangeShowContentPicker
								) {
									if(showContentPicker) {
										Icon(Icons.Outlined.AddCircle, contentDescription = "")
									} else {
										Icon(Icons.Outlined.AddCircleOutline, contentDescription = "")
									}
								}
							}
						}
					}
				}
				
				var inputFieldFocus by remember { mutableStateOf(false) }
				
				//Input field
				Surface(
					modifier = Modifier.border(
						if(inputFieldFocus) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
						else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
						RoundedCornerShape(20.dp)
					),
				) {
					Row {
						BasicTextField(
							value = textFieldValue,
							onValueChange = { textFieldValue = it },
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
								if(textFieldValue.text.isEmpty()) {
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
							IconButton(
								onClick = { onMessageSent(textFieldValue.text) },
								modifier = Modifier.align(Alignment.Bottom),
								colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
								enabled = validTextFieldValue
							) {
								Icon(
									painter = painterResource(id = R.drawable.push_rounded),
									contentDescription = stringResource(id = R.string.action_send)
								)
							}
						}
					}
				}
			}
		}
	}
}

@Preview
@Composable
private fun PreviewMessageInputBar() {
	Surface {
		MessageInputBar(
			onMessageSent = {},
			showContentPicker = false,
			onChangeShowContentPicker = {},
			collapseButtons = false,
			onChangeCollapseButtons = {},
			serviceHandler = ServiceHandler.appleBridge,
			serviceType = ServiceType.appleMessage,
		)
	}
}