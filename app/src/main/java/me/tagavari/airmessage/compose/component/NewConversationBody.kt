package me.tagavari.airmessage.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.state.NewConversationContactsState
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.MessageServiceDescription
import me.tagavari.airmessage.util.AddressInfo
import me.tagavari.airmessage.util.ContactInfo

@Composable
fun NewConversationBody(
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	contactsState: NewConversationContactsState,
	requiredService: MessageServiceDescription,
	onRequestPermission: () -> Unit,
	onReloadContacts: () -> Unit,
	directAddText: String?,
	onDirectAdd: () -> Unit,
	onAddRecipient: (ContactInfo, AddressInfo) -> Unit,
	isLoading: Boolean
) {
	Box(
		modifier = modifier
	) {
		when(contactsState) {
			is NewConversationContactsState.Loading -> {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(contentPadding)
						.padding(16.dp),
					contentAlignment = Alignment.Center
				) {
					Text(
						text = stringResource(id = R.string.progress_loading),
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			is NewConversationContactsState.NeedsPermission -> {
				MessageButtonCombo(
					contentPadding = contentPadding,
					message = stringResource(R.string.message_permissiondetails_contacts_suggestions),
					buttonText = stringResource(R.string.action_enable),
					onClick = onRequestPermission
				)
			}
			is NewConversationContactsState.Error -> {
				MessageButtonCombo(
					contentPadding = contentPadding,
					message = stringResource(R.string.message_loaderror_contacts),
					buttonText = stringResource(R.string.action_retry),
					onClick = onReloadContacts
				)
			}
			is NewConversationContactsState.Loaded -> {
				val scrollState = rememberLazyListState()
				
				//Scroll to the top when we add the direct add text row
				LaunchedEffect(directAddText) {
					if(directAddText != null
						&& scrollState.firstVisibleItemIndex == 1
						&& scrollState.firstVisibleItemScrollOffset == 0) {
						scrollState.scrollToItem(0)
					}
				}
				
				//Group contacts by first letter
				val groupedContacts = remember(contactsState.contacts) {
					val groupedContactsList = mutableListOf<ContactsListGroup>()
					var activeGroup: Pair<Char, MutableList<ContactInfo>>? = null
					
					fun applyActiveGroup() {
						val group = activeGroup ?: return
						val contactsListGroup = ContactsListGroup(
							key = group.first,
							contacts = group.second
						)
						groupedContactsList.add(contactsListGroup)
					}
					
					//Contacts are sorted in order, so we can iterate
					//linearly and check for differences
					val contactsList = contactsState.contacts
					for(contact in contactsList) {
						val contactKey = getNameHeader(contact.name)
						
						//If this contact is part of the same group, add it
						if(activeGroup != null && activeGroup.first == contactKey) {
							activeGroup.second.add(contact)
						} else {
							//Refresh the list
							applyActiveGroup()
							activeGroup = Pair(contactKey, mutableListOf(contact))
						}
					}
					
					//Add the last contact group
					applyActiveGroup()
					
					return@remember groupedContactsList.toList()
				}
				
				Box(modifier = Modifier.fillMaxSize()) {
					LazyColumn(
						contentPadding = contentPadding,
						state = scrollState
					) {
						if(directAddText != null) {
							item {
								AddressRowDirect(
									address = directAddText,
									onClick = onDirectAdd
								)
							}
						}
						
						for(group in groupedContacts) {
							//Show name headers between names that start with a different letter
							println("Using key ${group.key} for group ${group.contacts.firstOrNull()?.name}")
							item(
								key = group.key
							) {
								Box(
									modifier = Modifier
										.padding(horizontal = 16.dp)
										.size(40.dp),
									contentAlignment = Alignment.Center
								) {
									Text(
										text = group.key.toString(),
										color = MaterialTheme.colorScheme.onSurfaceVariant,
										fontSize = 20.sp
									)
								}
							}
							
							for(contact in group.contacts) {
								item(
									key = contact.contactID
								) {
									ContactRow(
										requiredService = requiredService,
										contact = contact,
										onSelectAddress = { addressInfo -> onAddRecipient(contact, addressInfo) }
									)
								}
							}
						}
					}
					
					//Scrim
					AnimatedVisibility(
						modifier = Modifier,
						visible = isLoading,
						enter = fadeIn(),
						exit = fadeOut()
					) {
						Box(
							modifier = Modifier
								.alpha(0.5F)
								.background(MaterialTheme.colorScheme.background)
								.pointerInput(Unit) {}
								.fillMaxSize()
						)
					}
				}
			}
		}
		
		//Progress bar
		AnimatedVisibility(
			visible = isLoading,
			enter = fadeIn(),
			exit = fadeOut()
		) {
			LinearProgressIndicator(
				modifier = Modifier
					.padding(contentPadding)
					.fillMaxWidth()
			)
		}
	}
}

/**
 * Gets the header character for use for a certain name
 */
private fun getNameHeader(name: String?): Char {
	if(name.isNullOrEmpty()) return '?'
	val firstChar = name[0].uppercaseChar()
	return if(firstChar.isDigit() || firstChar == '(') '#' else firstChar
}

@Composable
private fun MessageButtonCombo(
	contentPadding: PaddingValues = PaddingValues(0.dp),
	message: String,
	buttonText: String,
	onClick: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(contentPadding)
			.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			text = message,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		
		Spacer(modifier = Modifier.height(16.dp))
		
		OutlinedButton(onClick = onClick) {
			Text(text = buttonText)
		}
	}
}

@Immutable
private data class ContactsListGroup(
	val key: Char,
	val contacts: List<ContactInfo>
)

@Preview(name = "Loading", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyLoading() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.Loading,
			requiredService = MessageServiceDescription.IMESSAGE,
			directAddText = null,
			onRequestPermission = {},
			onReloadContacts = {},
			onDirectAdd = {},
			onAddRecipient = { _, _ -> },
			isLoading = false
		)
	}
}

@Preview(name = "Needs permission", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyPermission() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.NeedsPermission,
			requiredService = MessageServiceDescription.IMESSAGE,
			directAddText = null,
			onRequestPermission = {},
			onReloadContacts = {},
			onDirectAdd = {},
			onAddRecipient = { _, _ -> },
			isLoading = false
		)
	}
}

@Preview(name = "Error", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyError() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.Error(Throwable()),
			requiredService = MessageServiceDescription.IMESSAGE,
			directAddText = null,
			onRequestPermission = {},
			onReloadContacts = {},
			onDirectAdd = {},
			onAddRecipient = { _, _ -> },
			isLoading = false
		)
	}
}

@Preview(name = "Loaded", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyLoaded() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.Loaded(listOf(
				ContactInfo(0, "Some Guy", null, mutableListOf(AddressInfo("some@guy.com", "Home"))),
				ContactInfo(1, null, null, mutableListOf(AddressInfo("(604) 739-7997", null))),
			)),
			requiredService = MessageServiceDescription.IMESSAGE,
			directAddText = "hello@airmessage.org",
			onRequestPermission = {},
			onReloadContacts = {},
			onDirectAdd = {},
			onAddRecipient = { _, _ -> },
			isLoading = false
		)
	}
}
