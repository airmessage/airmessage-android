package me.tagavari.airmessage.compose.state

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.helper.AddressHelper
import me.tagavari.airmessage.task.ContactsTask
import me.tagavari.airmessage.util.ContactInfo

class NewConversationViewModel(application: Application) : AndroidViewModel(application) {
	//Contacts list
	var contactsState by mutableStateOf<NewConversationContactsState>(NewConversationContactsState.Loading)
		private set
	
	//Selected recipients
	var selectedRecipients by mutableStateOf(LinkedHashSet<SelectedRecipient>())
		private set
	
	init {
		loadContacts()
	}
	
	fun loadContacts() {
		//Check if we need permission
		if(ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
			contactsState = NewConversationContactsState.NeedsPermission
			return
		}
		
		//Set the state to loading
		contactsState = NewConversationContactsState.Loading
		
		viewModelScope.launch {
			val contacts = try {
				//Load contacts
				ContactsTask.loadContacts(getApplication()).asFlow()
					.fold(mutableListOf<ContactInfo>()) { list, contactPart ->
						//Fold multiple contact parts into a single contact
						val matchingContact = list.firstOrNull { it.identifier == contactPart.id }
						if(matchingContact != null) {
							matchingContact.addresses.add(contactPart.address)
						} else {
							list.add(
								ContactInfo(
									contactPart.id,
									contactPart.name,
									mutableListOf(contactPart.address)
								)
							)
						}
						
						list
					}
			} catch(exception: Throwable) {
				contactsState = NewConversationContactsState.Error(exception)
				return@launch
			}
			
			contactsState = NewConversationContactsState.Loaded(contacts)
		}
	}
	
	/**
	 * Adds a selected recipient to the list
	 */
	fun addSelectedRecipient(recipient: SelectedRecipient) {
		selectedRecipients = LinkedHashSet(selectedRecipients).also { collection ->
			collection.add(recipient)
		}
	}
	
	/**
	 * Removes a selected recipient from the list
	 */
	fun removeSelectedRecipient(recipient: SelectedRecipient) {
		selectedRecipients = LinkedHashSet(selectedRecipients).also { collection ->
			collection.remove(recipient)
		}
	}
}

data class SelectedRecipient(
	val address: String,
	val name: String? = null
) {
	val formattedAddress: String by lazy { AddressHelper.formatAddress(address) }
	val displayLabel: String
		get() = name ?: formattedAddress
	
	override fun equals(other: Any?): Boolean {
		if(other !is SelectedRecipient) return false
		return address == other.address
	}
	
	override fun hashCode() = address.hashCode()
}

sealed class NewConversationContactsState {
	object Loading : NewConversationContactsState()
	object NeedsPermission : NewConversationContactsState()
	class Error(val exception: Throwable) : NewConversationContactsState()
	class Loaded(val contacts: List<ContactInfo>) : NewConversationContactsState()
}