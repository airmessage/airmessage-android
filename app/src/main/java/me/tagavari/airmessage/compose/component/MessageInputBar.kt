package me.tagavari.airmessage.compose.component

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.LanguageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(
	modifier: Modifier = Modifier,
	onMessageSent: (String) -> Unit,
	showContentPicker: Boolean,
	onChangeShowContentPicker: (Boolean) -> Unit,
	@ServiceHandler serviceHandler: Int?,
	@ServiceType serviceType: String?
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
	
	Box(modifier = modifier.padding(8.dp)) {
		Row(
			verticalAlignment = Alignment.Bottom
		) {
			CompositionLocalProvider(
				LocalMinimumTouchTargetEnforcement provides false,
				LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
			) {
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
			
			//Input field
			Surface(
				modifier = Modifier.clip(RoundedCornerShape(20.dp)),
				tonalElevation = 2.dp
			) {
				Row {
					BasicTextField(
						value = textFieldValue,
						onValueChange = { textFieldValue = it },
						modifier = Modifier
							.weight(1F)
							.heightIn(min = 40.dp, max = 100.dp)
							.padding(horizontal = 12.dp, vertical = 8.dp)
							.align(Alignment.CenterVertically),
						textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
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

@Preview
@Composable
private fun PreviewMessageInputBar() {
	Surface {
		MessageInputBar(
			onMessageSent = {},
			showContentPicker = false,
			onChangeShowContentPicker = {},
			serviceHandler = ServiceHandler.appleBridge,
			serviceType = ServiceType.appleMessage
		)
	}
}