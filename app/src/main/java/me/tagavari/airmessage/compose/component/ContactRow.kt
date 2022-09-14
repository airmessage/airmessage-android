package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.util.AddressInfo
import me.tagavari.airmessage.util.ContactInfo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContactRow(
	modifier: Modifier = Modifier,
	contact: ContactInfo,
	onSelectAddress: (String) -> Unit
) {
	fun selectAddress() {
		//Select the first address if there's only one
		if(contact.addresses.size == 1) {
			onSelectAddress(contact.addresses.first().address)
		} else if(contact.addresses.size > 1) {
			//Prompt the user to select an address
		}
	}
	
	Row(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp)
			.height(56.dp)
			.clickable(onClick = ::selectAddress),
		verticalAlignment = Alignment.CenterVertically
	) {
		MemberImage(
			modifier = Modifier.size(40.dp),
			color = MaterialTheme.colorScheme.primary,
			contactID = contact.identifier
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
						firstAddress, addressCount - 1
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
			contact = ContactInfo(0, "Some Guy", mutableListOf(AddressInfo("some@guy.com", "Home"))),
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
			contact = ContactInfo(0, "Some Guy", mutableListOf(
				AddressInfo("some@guy.com", "Home"),
				AddressInfo("(604) 739-7997", "Mobile")
			)),
			onSelectAddress = {}
		)
	}
}