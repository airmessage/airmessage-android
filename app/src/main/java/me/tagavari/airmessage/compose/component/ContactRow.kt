package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.compose.util.ImmutableHolder
import me.tagavari.airmessage.compose.util.wrapImmutableHolder
import me.tagavari.airmessage.enums.MessageServiceDescription
import me.tagavari.airmessage.helper.AddressHelper
import me.tagavari.airmessage.util.AddressInfo
import me.tagavari.airmessage.util.ContactInfo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContactRow(
	modifier: Modifier = Modifier,
	requiredService: MessageServiceDescription,
	contact: ContactInfo,
	onSelectAddress: (AddressInfo) -> Unit
) {
	var showAddressSelector by remember { mutableStateOf(false) }
	
	fun selectAddress() {
		//Select the first address if there's only one
		if(contact.addresses.size == 1) {
			onSelectAddress(contact.addresses.first())
		} else if(contact.addresses.size > 1) {
			//Prompt the user to select an address
			showAddressSelector = true
		}
	}
	
	if(showAddressSelector) {
		ContentAlertDialog(
			onDismissRequest = { showAddressSelector = false },
			title = {
				Text(stringResource(id = R.string.imperative_selectdestination))
			}
		) {
			Column {
				for(address in contact.addresses) {
					val enabled = remember {
						requiredService.serviceSupportsEmail || AddressHelper.validatePhoneNumber(address.address)
					}
					
					Row(
						modifier = Modifier
							.alpha(if(enabled) 1F else 0.38F)
							.clickable(enabled = enabled) {
								onSelectAddress(address)
								showAddressSelector = false
							}
							.fillMaxWidth()
							.height(48.dp)
							.padding(horizontal = 24.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(address.getDisplay(LocalContext.current.resources))
					}
				}
			}
		}
	}
	
	Row(
		modifier = modifier
			.clickable(onClick = ::selectAddress)
			.fillMaxWidth()
			.padding(horizontal = 16.dp)
			.height(56.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		MemberImage(
			modifier = Modifier.size(40.dp),
			color = MaterialTheme.colorScheme.primary,
			thumbnailURI = contact.thumbnailURI.wrapImmutableHolder()
		)
		
		Spacer(modifier = Modifier.width(16.dp))
		
		Column(modifier = Modifier.weight(1F)) {
			contact.name?.let { name ->
				Text(
					text = name,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					style = MaterialTheme.typography.bodyLarge
				)
			}
			
			contact.addresses.let { addresses ->
				if(addresses.isEmpty()) return@let
				
				val addressCount = addresses.size
				val firstAddress = addresses.first()
				
				val text = if(addressCount == 1) {
					firstAddress.address
				} else {
					pluralStringResource(
						id = R.plurals.message_multipledestinations,
						count = addressCount,
						firstAddress.address, addressCount - 1
					)
				}
				
				Text(
					text = text,
					style = MaterialTheme.typography.bodyMedium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Preview(
	widthDp = 384,
	showBackground = true
)
@Composable
private fun PreviewContactRowSimple() {
	AirMessageAndroidTheme {
		ContactRow(
			requiredService = MessageServiceDescription.IMESSAGE,
			contact = ContactInfo(0, "Some Guy", null, mutableListOf(AddressInfo("some@guy.com", "Home"))),
			onSelectAddress = {}
		)
	}
}

@Preview(
	widthDp = 384,
	showBackground = true
)
@Composable
private fun PreviewContactRowMultipleAddresses() {
	AirMessageAndroidTheme {
		ContactRow(
			requiredService = MessageServiceDescription.IMESSAGE,
			contact = ContactInfo(0, "Some Guy", null, mutableListOf(
				AddressInfo("some@guy.com", "Home"),
				AddressInfo("(604) 739-7997", "Mobile")
			)),
			onSelectAddress = {}
		)
	}
}