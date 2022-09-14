package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.state.NewConversationContactsState
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.util.AddressInfo
import me.tagavari.airmessage.util.ContactInfo

@Composable
fun NewConversationBody(
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	contactsState: NewConversationContactsState,
	onRequestPermission: () -> Unit,
	onReloadContacts: () -> Unit,
	directAddText: String?,
	onAddRecipient: (String) -> Unit
) {
	when(contactsState) {
		is NewConversationContactsState.Loading -> {
			Box(
				modifier = modifier
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
				modifier = modifier,
				contentPadding = contentPadding,
				message = stringResource(R.string.message_permissiondetails_contacts_suggestions),
				buttonText = stringResource(R.string.action_enable),
				onClick = onRequestPermission
			)
		}
		is NewConversationContactsState.Error -> {
			MessageButtonCombo(
				modifier = modifier,
				contentPadding = contentPadding,
				message = stringResource(R.string.message_loaderror_contacts),
				buttonText = stringResource(R.string.action_retry),
				onClick = onReloadContacts
			)
		}
		is NewConversationContactsState.Loaded -> {
			LazyColumn(
				modifier = modifier,
				contentPadding = contentPadding
			) {
				if(directAddText != null) {
					item {
						AddressRowDirect(
							address = directAddText,
							onClick = { onAddRecipient(directAddText) }
						)
					}
				}
				
				var lastContact: ContactInfo? = null
				for(contact in contactsState.contacts) {
					val nameHeader = getNameHeader(contact.name)
					val showNameHeader = getNameHeader(lastContact?.name) != nameHeader
					
					//Show name headers between names that start with a different letter
					if(showNameHeader) {
						item(
							key = nameHeader
						) {
							Box(
								modifier = Modifier
									.padding(horizontal = 16.dp)
									.size(40.dp),
								contentAlignment = Alignment.Center
							) {
								Text(
									text = nameHeader.toString(),
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									fontSize = 20.sp
								)
							}
						}
					}
					
					item(
						key = contact.identifier
					) {
						ContactRow(
							contact = contact,
							onSelectAddress = onAddRecipient
						)
					}
					
					lastContact = contact
				}
			}
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
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	message: String,
	buttonText: String,
	onClick: () -> Unit
) {
	Column(
		modifier = modifier
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

@Preview(name = "Loading", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyLoading() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.Loading,
			directAddText = null,
			onRequestPermission = {},
			onReloadContacts = {},
			onAddRecipient = {}
		)
	}
}

@Preview(name = "Needs permission", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyPermission() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.NeedsPermission,
			directAddText = null,
			onRequestPermission = {},
			onReloadContacts = {},
			onAddRecipient = {}
		)
	}
}

@Preview(name = "Error", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyError() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.Error(Throwable()),
			directAddText = null,
			onRequestPermission = {},
			onReloadContacts = {},
			onAddRecipient = {}
		)
	}
}

@Preview(name = "Loaded", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun PreviewNewConversationBodyLoaded() {
	AirMessageAndroidTheme {
		NewConversationBody(
			contactsState = NewConversationContactsState.Loaded(listOf(
				ContactInfo(0, "Some Guy", mutableListOf(AddressInfo("some@guy.com", "Home"))),
				ContactInfo(1, null, mutableListOf(AddressInfo("(604) 739-7997", null))),
			)),
			directAddText = "hello@airmessage.org",
			onRequestPermission = {},
			onReloadContacts = {},
			onAddRecipient = {}
		)
	}
}
