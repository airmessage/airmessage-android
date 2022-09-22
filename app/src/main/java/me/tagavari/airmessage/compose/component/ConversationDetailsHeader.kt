package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.ConversationBuildHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo
import me.tagavari.airmessage.task.ConversationActionTask

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ConversationDetailsHeader(
	modifier: Modifier = Modifier,
	conversation: ConversationInfo
) {
	val context = LocalContext.current
	
	val fallbackTitle = remember(conversation) {
		ConversationBuildHelper.buildConversationTitleDirect(context, conversation)
	}
	val generatedTitle by produceState<String?>(initialValue = null, conversation) {
		value = ConversationBuildHelper.buildConversationTitle(context, conversation).await()
	}
	val title: String = generatedTitle ?: fallbackTitle
	
	val isRenameSupported = conversation.serviceHandler != ServiceHandler.appleBridge
	
	var showRenameDialog by remember { mutableStateOf(false) }
	var conversationNameInput by remember { mutableStateOf("") }
	
	Column(
		modifier = modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		UserIconGroup(
			modifier = Modifier.size(96.dp),
			members = conversation.members,
			highRes = true
		)
		
		Spacer(modifier = Modifier.height(16.dp))
		
		Text(
			text = title,
			style = MaterialTheme.typography.titleLarge,
			textAlign = TextAlign.Center
		)
		
		if(isRenameSupported) {
			TextButton(onClick = {
				//Only let the user rename the conversation after we've determined
				//the conversation's actual name
				generatedTitle?.let { generatedTitle ->
					showRenameDialog = true
					conversationNameInput = generatedTitle
				}
			}) {
				Text(stringResource(id = R.string.action_renamegroup))
			}
		}
	}
	
	//Rename dialog
	if(showRenameDialog) {
		AlertDialog(
			onDismissRequest = { showRenameDialog = false },
			title = { Text(stringResource(R.string.action_renamegroup)) },
			text = {
				Column {
					val focusRequester = remember { FocusRequester() }
					val keyboardController = LocalSoftwareKeyboardController.current
					
					OutlinedTextField(
						modifier = Modifier.focusRequester(focusRequester),
						value = conversationNameInput,
						onValueChange = { conversationNameInput = it },
						singleLine = true
					)
					
					Text(
						text = stringResource(R.string.message_renamegroup_onlyyouvisibility),
						color = MaterialTheme.colorScheme.onSurface,
						style = MaterialTheme.typography.bodySmall,
						modifier = Modifier.padding(start = 16.dp, top = 4.dp)
					)
					
					LaunchedEffect(Unit) {
						delay(100)
						focusRequester.requestFocus()
						keyboardController?.show()
					}
				}
			},
			confirmButton = {
				TextButton(
					onClick = {
						//Hide the dialog
						showRenameDialog = false
						
						//Update the title
						@OptIn(DelicateCoroutinesApi::class)
						GlobalScope.launch {
							ConversationActionTask.setConversationTitle(conversation.localID, title).await()
						}
					}
				) {
					Text(stringResource(android.R.string.ok))
				}
			},
			dismissButton = {
				TextButton(
					onClick = { showRenameDialog = false }
				) {
					Text(stringResource(android.R.string.cancel))
				}
			}
		)
	}
}

@Preview
@Composable
private fun PreviewConversationDetailsHeader() {
	AirMessageAndroidTheme {
		Surface {
			ConversationDetailsHeader(
				modifier = Modifier.padding(16.dp),
				conversation = ConversationInfo(
					localID = 0,
					guid = null,
					externalID = -1,
					state = ConversationState.ready,
					serviceHandler = ServiceHandler.systemMessaging,
					serviceType = ServiceType.systemSMS,
					conversationColor = 0xFFFF1744.toInt(),
					members = mutableListOf(
						MemberInfo("1", 0xFFFF1744.toInt()),
						MemberInfo("2", 0xFFF50057.toInt()),
						MemberInfo("3", 0xFFB317CF.toInt())
					),
					title = "A cool conversation",
					unreadMessageCount = 1,
					isArchived = false,
					isMuted = true,
					messagePreview = ConversationPreview.Message(
						date = System.currentTimeMillis(),
						isOutgoing = false,
						message = "Test message",
						subject = null,
						attachments = listOf(),
						sendStyle = null,
						isError = false
					)
				)
			)
		}
	}
}
