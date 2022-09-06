package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.NotListedLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo
import me.tagavari.airmessage.task.ConversationActionTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetails(
	conversation: ConversationInfo,
	onClickMember: (MemberInfo, UserCacheHelper.UserInfo?) -> Unit,
	isLoadingCurrentLocation: Boolean,
	onSendCurrentLocation: () -> Unit,
	noLocationDialog: Boolean,
	onHideNoLocationDialog: () -> Unit,
	locationDisabledDialog: Boolean,
	onHideLocationDisabledDialog: () -> Unit,
	onPromptEnableLocationServices: () -> Unit,
	onFinish: () -> Unit
) {
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	
	Scaffold(
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			SmallTopAppBar(
				title = {},
				navigationIcon = {
					IconButton(onClick = onFinish) {
						Icon(
							imageVector = Icons.Filled.ArrowBack,
							contentDescription = stringResource(id = R.string.action_back)
						)
					}
				},
				scrollBehavior = scrollBehavior,
			)
		},
		content = { innerPadding ->
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = innerPadding
			) {
				//Header
				item {
					ConversationDetailsHeader(
						modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
						conversation = conversation
					)
				}
				
				//Conversation members
				item {
					Text(
						modifier = Modifier.padding(16.dp),
						text = stringResource(R.string.title_conversation_members),
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				
				items(
					items = conversation.members,
					key = { it.address }
				) { member ->
					ConversationDetailsMember(
						modifier = Modifier.fillMaxWidth(),
						member = member,
						onClick = onClickMember
					)
				}
				
				item {
					OutlinedButton(
						modifier = Modifier
							.padding(16.dp)
							.fillMaxWidth(),
						onClick = onSendCurrentLocation,
						enabled = !isLoadingCurrentLocation
					) {
						Text(stringResource(R.string.action_sendcurrentlocation))
					}
				}
				
				item {
					Row(
						modifier = Modifier
							.clickable(onClick = {
								@OptIn(DelicateCoroutinesApi::class)
								GlobalScope.launch {
									ConversationActionTask
										.muteConversations(
											setOf(conversation),
											!conversation.isMuted
										)
										.await()
								}
							})
							.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(stringResource(R.string.action_mutenotifications))
						
						Spacer(modifier = Modifier.weight(1F))
						
						Switch(
							checked = conversation.isMuted,
							onCheckedChange = null
						)
					}
				}
			}
		}
	)
	
	if(noLocationDialog) {
		AlertDialog(
			onDismissRequest = onHideNoLocationDialog,
			confirmButton = {
				TextButton(onClick = onHideNoLocationDialog) {
					Text(stringResource(android.R.string.ok))
				}
			},
			icon = {
				Icon(
					imageVector = Icons.Outlined.NotListedLocation,
					contentDescription = null
				)
			},
			title = {
				Text(stringResource(id = R.string.message_locationerror_unavailable))
			},
			text = {
				Text(stringResource(id = R.string.message_locationerror_unavailable_desc))
			}
		)
	}
	
	if(locationDisabledDialog) {
		AlertDialog(
			onDismissRequest = onHideLocationDisabledDialog,
			confirmButton = {
				TextButton(onClick = {
					onPromptEnableLocationServices()
					onHideLocationDisabledDialog()
				}) {
					Text(stringResource(R.string.action_enablelocation))
				}
			},
			dismissButton = {
				TextButton(onClick = onHideLocationDisabledDialog) {
					Text(stringResource(android.R.string.cancel))
				}
			},
			icon = {
				Icon(
					imageVector = Icons.Outlined.GpsOff,
					contentDescription = null
				)
			},
			title = {
				Text(stringResource(id = R.string.message_locationerror_disabled))
			},
			text = {
				Text(stringResource(id = R.string.message_locationerror_disabled_desc))
			}
		)
	}
}

@Preview
@Composable
private fun PreviewConversationDetails() {
	AirMessageAndroidTheme {
		ConversationDetails(
			conversation = ConversationInfo(
				localID = 0,
				guid = null,
				externalID = -1,
				state = ConversationState.ready,
				serviceHandler = ServiceHandler.appleBridge,
				serviceType = ServiceType.appleMessage,
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
			),
			onClickMember = { _, _ -> },
			isLoadingCurrentLocation = false,
			onSendCurrentLocation = {},
			noLocationDialog = false,
			onHideNoLocationDialog = {},
			locationDisabledDialog = false,
			onHideLocationDisabledDialog = {},
			onPromptEnableLocationServices = {},
			onFinish = {}
		)
	}
}

@Preview
@Composable
private fun PreviewConversationDetailsLocation() {
	AirMessageAndroidTheme {
		ConversationDetails(
			conversation = ConversationInfo(
				localID = 0,
				guid = null,
				externalID = -1,
				state = ConversationState.ready,
				serviceHandler = ServiceHandler.appleBridge,
				serviceType = ServiceType.appleMessage,
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
			),
			onClickMember = { _, _ -> },
			isLoadingCurrentLocation = true,
			onSendCurrentLocation = {},
			noLocationDialog = true,
			onHideNoLocationDialog = {},
			locationDisabledDialog = false,
			onHideLocationDisabledDialog = {},
			onPromptEnableLocationServices = {},
			onFinish = {}
		)
	}
}
