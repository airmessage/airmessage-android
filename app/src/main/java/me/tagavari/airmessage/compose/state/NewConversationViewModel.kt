package me.tagavari.airmessage.compose.state

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Parcelable
import android.provider.Telephony
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.enums.*
import me.tagavari.airmessage.helper.AddressHelper
import me.tagavari.airmessage.helper.ConversationColorHelper.getColoredMembers
import me.tagavari.airmessage.helper.ConversationColorHelper.getDefaultConversationColor
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview.ChatCreation
import me.tagavari.airmessage.redux.ReduxEmitterNetwork.messageUpdateSubject
import me.tagavari.airmessage.redux.ReduxEventMessaging.ConversationUpdate
import me.tagavari.airmessage.task.ContactsTask
import me.tagavari.airmessage.util.ContactInfo

@OptIn(SavedStateHandleSaveableApi::class)
class NewConversationViewModel(
	application: Application,
	savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
	//Contacts list
	private var contactsList by mutableStateOf<List<ContactInfo>?>(null)
	var contactsState by mutableStateOf<NewConversationContactsState>(NewConversationContactsState.Loading)
		private set
	
	//Text field input
	var recipientInput by savedStateHandle.saveable { mutableStateOf("") }
	var recipientInputType by savedStateHandle.saveable { mutableStateOf(ConversationRecipientInputType.EMAIL) }
	
	val recipientInputValid by derivedStateOf {
		if(selectedService.serviceSupportsEmail) {
			AddressHelper.validateAddress(recipientInput)
		} else {
			AddressHelper.validatePhoneNumber(recipientInput)
		}
	}
	
	//Selected service
	var selectedService by savedStateHandle.saveable { mutableStateOf(MessageServiceDescription.IMESSAGE) }
	
	//Selected recipients
	var selectedRecipients by savedStateHandle.saveable(stateSaver = LinkedHashSetSaver()) { mutableStateOf(LinkedHashSet<SelectedRecipient>()) }
		private set
	
	//Loading state
	var isLoading by mutableStateOf(false)
		private set
	
	//Assigned when a conversation is ready to be launched
	val launchFlow = MutableStateFlow<ConversationInfo?>(null)
	
	init {
		loadContacts()
		
		viewModelScope.launch {
			snapshotFlow { contactsList?.toList() }
				.filterNotNull()
				.combine(snapshotFlow { Pair(recipientInput, selectedService) }) { list, (input, service) -> Triple(list, input, service) }
				.collectLatest { (list, input, service) ->
					val filteredContacts = withContext(Dispatchers.Default) {
						//Clean the input text
						val filter = input.trim().lowercase()
						
						//If there's no filter text, don't do any work
						if(filter.isEmpty() && service.serviceSupportsEmail) {
							return@withContext list
						}
						
						val filterPhoneNumber = ContactsTask.formatPhoneFilter(filter)
						
						list.filter { contact ->
							//Filter out contacts that don't have any phone numbers
							if(!service.serviceSupportsEmail && contact.addresses.none { AddressHelper.validatePhoneNumber(it.normalizedAddress) }) {
								return@filter false
							}
							
							//Filter by text
							if(filter.isNotEmpty()
								&& contact.name?.lowercase()?.contains(filter) != true
								&& contact.addresses.none { address ->
									//The contact's email address matches the filter
									address.normalizedAddress.lowercase().startsWith(filter)
											////The contact's phone number matches the filter
											|| (filterPhoneNumber != null
											&& AddressHelper.validatePhoneNumber(address.normalizedAddress)
											&& AddressHelper.stripPhoneNumber(address.normalizedAddress).startsWith(filterPhoneNumber))
								}) {
								return@filter false
							}
							
							return@filter true
						}
					}
					
					//Update the contacts state
					contactsState = NewConversationContactsState.Loaded(filteredContacts)
				}
		}
	}
	
	fun loadContacts() {
		//Check if we need permission
		if(ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
			contactsState = NewConversationContactsState.NeedsPermission
			return
		}
		
		//Set the state to loading
		contactsState = NewConversationContactsState.Loading
		contactsList = null
		
		viewModelScope.launch {
			val contacts = try {
				//Load contacts
				ContactsTask.loadContacts(getApplication()).asFlow()
					.fold(mutableListOf<ContactInfo.Builder>()) { list, contactPart ->
						//Fold multiple contact parts into a single contact
						val matchingContact = list.firstOrNull { it.contactID == contactPart.id }
						if(matchingContact != null) {
							matchingContact.addresses.add(contactPart.address)
						} else {
							list.add(
								ContactInfo.Builder(contactPart.id).apply {
									name = contactPart.name
									thumbnailURI = contactPart.thumbnailURI
									addresses.add(contactPart.address)
								}
							)
						}
						
						list
					}
					.map { it.build() }
			} catch(exception: Throwable) {
				contactsState = NewConversationContactsState.Error(exception)
				return@launch
			}
			
			//Set the contacts list, and delegate the state update to
			//NewConversationContactsState.Loaded once we finish filtering
			contactsList = contacts
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
	
	fun createConversation(connectionManager: ConnectionManager?) {
		viewModelScope.launch {
			isLoading = true
			
			val serviceHandler = selectedService.serviceHandler
			val serviceType = selectedService.serviceType
			val recipients = selectedRecipients.map { AddressHelper.normalizeAddress(it.address) }
			
			try {
				val (conversation, isConversationNew) = if(serviceHandler == ServiceHandler.appleBridge) {
					//Try to create the chat on the server
					try {
						//Fail immediately if we have no network connection
						if(connectionManager == null) {
							throw AMRequestException(ChatCreateErrorCode.network)
						}
						
						//Request the creation of the chat
						val chatGUID = connectionManager.createChat(recipients.toTypedArray(), serviceType).await()
						
						//Try to find a matching conversation in the database, or create a new one
						withContext(Dispatchers.IO) {
							DatabaseManager.getInstance().addRetrieveMixedConversationInfoAMBridge(getApplication(), chatGUID, recipients, serviceType)
						}
					} catch(exception: Exception) {
						//Create an unlinked conversation locally
						withContext(Dispatchers.IO) {
							DatabaseManager.getInstance().addRetrieveClientCreatedConversationInfo(
								getApplication(),
								recipients,
								ServiceHandler.appleBridge,
								selectedService.serviceType
							)
						}
					}
				} else if(serviceHandler == ServiceHandler.systemMessaging) {
					if(serviceType == ServiceType.systemSMS) {
						withContext(Dispatchers.IO) {
							//Find or create a matching conversation in Android's message database
							val threadID = Telephony.Threads.getOrCreateThreadId(
								getApplication(),
								recipients.toSet()
							)
							
							//Find a matching conversation in AirMessage's database
							DatabaseManager.getInstance()
								.findConversationByExternalID(
									getApplication(),
									threadID,
									serviceHandler,
									serviceType
								)
								?.let { Pair(it, false) }
								?: run {
									//Create the conversation
									val conversationColor = getDefaultConversationColor()
									val coloredMembers = getColoredMembers(recipients, conversationColor)
									val conversationInfo = ConversationInfo(
										-1,
										null,
										threadID,
										ConversationState.ready,
										ServiceHandler.systemMessaging,
										ServiceType.systemSMS,
										conversationColor,
										coloredMembers,
										null
									)
									
									//Write the conversation to disk
									DatabaseManager.getInstance().addConversationInfo(conversationInfo)
									val chatCreateAction = DatabaseManager.getInstance().addConversationCreatedMessage(conversationInfo.localID)
									
									//Set the conversation preview
									conversationInfo.messagePreview = ChatCreation(chatCreateAction.date)
									
									Pair(conversationInfo, true)
								}
						}
					} else {
						throw IllegalStateException("Unsupported service type $serviceType")
					}
				} else {
					throw IllegalStateException("Unsupported service handler $serviceHandler")
				}
				
				//Notify listeners of the new conversation
				if(isConversationNew) {
					messageUpdateSubject.onNext(ConversationUpdate(mapOf(conversation to listOf()), listOf()))
				}
				
				//Launch the conversation
				launchFlow.emit(conversation)
			} finally {
				//Reset the state
				isLoading = false
				selectedService = MessageServiceDescription.IMESSAGE
				selectedRecipients = LinkedHashSet()
			}
		}
	}
}

@Parcelize
data class SelectedRecipient(
	val address: String,
	val name: String? = null
) : Parcelable {
	val displayLabel: String
		get() = name ?: address
	
	override fun equals(other: Any?): Boolean {
		if(other !is SelectedRecipient) return false
		return address == other.address
	}
	
	override fun hashCode() = address.hashCode()
}

@Immutable
sealed class NewConversationContactsState {
	object Loading : NewConversationContactsState()
	object NeedsPermission : NewConversationContactsState()
	@Immutable class Error(val exception: Throwable) : NewConversationContactsState()
	@Immutable class Loaded(val contacts: List<ContactInfo>) : NewConversationContactsState()
}

private class LinkedHashSetSaver<T> : Saver<LinkedHashSet<T>, List<T>> {
	override fun restore(value: List<T>): LinkedHashSet<T> {
		return LinkedHashSet(value)
	}
	
	override fun SaverScope.save(value: LinkedHashSet<T>): List<T> {
		return value.toList()
	}
}
