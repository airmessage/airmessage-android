package me.tagavari.airmessage.compose.state

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.helper.ConversationHelper
import me.tagavari.airmessage.helper.ConversationPreviewHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.task.ConversationActionTask

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {
	var conversations by mutableStateOf<Result<List<ConversationInfo>>?>(null)
		private set
	
	val hasUnreadConversations by derivedStateOf { conversations?.getOrNull()?.any { it.unreadMessageCount > 0 } ?: false }
	
	init {
		loadConversations()
	}
	
	/**
	 * Loads conversations into the conversations state
	 */
	fun loadConversations() {
		viewModelScope.launch {
			//Load conversations
			conversations = null
			conversations = withContext(Dispatchers.IO) {
				try {
					Result.success(DatabaseManager.getInstance().fetchSummaryConversations(getApplication(), false))
				} catch(throwable: Throwable) {
					Result.failure(throwable)
				}
			}
			
			//Subscribe to updates
			ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect(this@ConversationsViewModel::applyMessageUpdate)
		}
	}
	
	private fun applyMessageUpdate(event: ReduxEventMessaging) {
		//Get the conversation list
		val conversationList = conversations?.getOrNull() ?: return
		
		when(event) {
			is ReduxEventMessaging.Message -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					for((eventConversation, changes) in event.conversationItems) {
						//Find the conversation in the list, and ignore if it wasn't found
						val conversation = list.firstOrNull { it.localID == eventConversation.localID } ?: continue
						
						//Build a new conversation preview from new messages
						val preview = ConversationPreviewHelper.latestItemToPreview(changes.map { it.targetItem }) ?: continue
						
						//Ignore if the new preview is older than the conversation's current preview
						if((conversation.messagePreview?.date ?: Long.MIN_VALUE) > preview.date) continue
						
						//Update the conversation's preview
						list.remove(conversation)
						val updatedConversation = conversation.copy(messagePreview = preview)
						val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
						list[insertionIndex] = updatedConversation
					}
				})
			}
			is ReduxEventMessaging.ConversationUpdate -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					//Handle transferred conversations
					for(transferredConversation in event.transferredConversations) {
						//Find the conversation in the list
						val conversation = list.firstOrNull { it.localID == transferredConversation.clientConversation.localID }
						
						//Build a conversation preview based on the new messages
						val preview = ConversationPreviewHelper.latestItemToPreview(transferredConversation.serverConversationItems.map { it.targetItem })
						
						//Remove the conversation if it was found
						conversation?.let { list.remove(it) }
						
						//Add or re-sort the conversation
						val updatedConversation = (conversation ?: transferredConversation.clientConversation)
							.copy(messagePreview = preview)
						val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
						list[insertionIndex] = updatedConversation
					}
					
					//Handle new conversations
					for((newConversation, newConversationMessages) in event.newConversations) {
						//Build a conversation preview based on the new messages
						val preview = ConversationPreviewHelper.latestItemToPreview(newConversationMessages)
						
						val updatedConversation = newConversation.copy(
							messagePreview = preview,
							unreadMessageCount = newConversationMessages.size
						)
						
						val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
						list[insertionIndex] = updatedConversation
					}
				})
			}
			is ReduxEventMessaging.ConversationMember -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].clone().apply {
						members = members.toMutableList().also { list ->
							if(event.isJoin) {
								list.add(event.member)
							} else {
								list.removeAll { it.address == event.member.address }
							}
						}
					}
				})
			}
			is ReduxEventMessaging.ConversationMute -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].clone().apply { isMuted = event.isMuted }
				})
			}
			is ReduxEventMessaging.ConversationArchive -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].clone().apply { isArchived = event.isArchived }
				})
			}
			is ReduxEventMessaging.ConversationDelete -> {
				conversations = Result.success(conversationList.filter { it.localID != event.conversationInfo.localID })
			}
			is ReduxEventMessaging.ConversationServiceHandlerDelete -> {
				conversations = Result.success(conversationList.filter { it.serviceHandler == event.serviceHandler })
			}
			is ReduxEventMessaging.ConversationTitle -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].clone().apply { title = event.title }
				})
			}
			is ReduxEventMessaging.ConversationDraftMessageUpdate -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					//Find the conversation
					val conversation = list.firstOrNull { it.localID == event.conversationInfo.localID } ?: return@also
					
					//Update and re-sort the conversation
					list.remove(conversation)
					
					val updatedConversation = conversation.copy(
						draftMessage = event.draftMessage,
						draftUpdateTime = event.updateTime
					)
					val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
					list[insertionIndex] = updatedConversation
					
				})
			}
			is ReduxEventMessaging.ConversationDraftFileUpdate -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					//Find the conversation
					val conversation = list.firstOrNull { it.localID == event.conversationInfo.localID } ?: return@also
					
					//Update and re-sort the conversation
					list.remove(conversation)
					
					val updatedConversation = conversation.copy(
						draftFiles = conversation.draftFiles.toMutableList().also { draftFilesList ->
							if(event.isAddition) {
								draftFilesList.add(event.draft)
							} else {
								draftFilesList.removeAll { it.localID == event.draft.localID }
							}
						},
						draftUpdateTime = event.updateTime
					)
					val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
					list[insertionIndex] = updatedConversation
					
				})
			}
			is ReduxEventMessaging.ConversationDraftFileClear -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					//Find the conversation
					val conversation = list.firstOrNull { it.localID == event.conversationInfo.localID } ?: return@also
					
					//Update and re-sort the conversation
					list.remove(conversation)
					
					val updatedConversation = conversation.copy(
						draftFiles = mutableListOf(),
						draftUpdateTime = System.currentTimeMillis()
					)
					val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
					list[insertionIndex] = updatedConversation
					
				})
			}
			else -> {}
		}
	}
	
	/**
	 * Marks all conversations as read
	 */
	@OptIn(DelicateCoroutinesApi::class)
	fun markConversationsAsRead() {
		//Get the conversations list
		val conversations = conversations?.getOrNull() ?: return
		
		//Search for unread conversations
		val unreadConversations = conversations.filter { it.unreadMessageCount > 0 }
		
		//Mark the conversations as read
		GlobalScope.launch {
			ConversationActionTask.unreadConversations(unreadConversations, 0)
		}
	}
}