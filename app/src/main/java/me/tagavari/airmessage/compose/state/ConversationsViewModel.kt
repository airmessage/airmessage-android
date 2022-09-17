package me.tagavari.airmessage.compose.state

import android.app.Application
import android.provider.Telephony
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.container.ConversationReceivedContent
import me.tagavari.airmessage.container.PendingConversationReceivedContent
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.ForegroundState
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.ConversationColorHelper
import me.tagavari.airmessage.helper.ConversationHelper
import me.tagavari.airmessage.helper.ConversationPreviewHelper
import me.tagavari.airmessage.helper.ShortcutHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.task.ConversationActionTask

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {
	var conversations by mutableStateOf<Result<List<ConversationInfo>>?>(null)
		private set
	val hasUnreadConversations by derivedStateOf { conversations?.getOrNull()?.any { it.unreadMessageCount > 0 } ?: false }
	var detailPage by mutableStateOf<ConversationsDetailPage?>(null)
	private val pendingReceivedContent = MutableStateFlow<PendingConversationReceivedContent?>(null)
	
	init {
		loadConversations()
		
		//Record conversation shortcut usages
		viewModelScope.launch {
			snapshotFlow { detailPage }
				.filterIsInstance<ConversationsDetailPage.Messaging>()
				.collect { page ->
					ShortcutHelper.reportShortcutUsed(getApplication(), page.conversationID)
				}
		}
		
		//Keep track of whether we're in the foreground
		viewModelScope.launch {
			snapshotFlow { detailPage }
				.map { it != null }
				.distinctUntilChanged()
				.collect { detailPageSelected ->
					if(detailPageSelected) ForegroundState.conversationListLoadCount++
					else ForegroundState.conversationListLoadCount--
				}
		}
	}
	
	override fun onCleared() {
		//Make sure we update the count
		if(detailPage != null) {
			ForegroundState.conversationListLoadCount--
		}
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
						list.add(insertionIndex, updatedConversation)
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
						val updatedConversation = (conversation ?: transferredConversation.clientConversation).run {
							copy(
								messagePreview = ConversationHelper.getLatestPreview(messagePreview, preview)
							)
						}
						val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
						list.add(insertionIndex, updatedConversation)
					}
					
					//Handle new conversations
					for((newConversation, newConversationMessages) in event.newConversations) {
						//Build a conversation preview based on the new messages
						val preview = ConversationPreviewHelper.latestItemToPreview(newConversationMessages)
						
						val updatedConversation = newConversation.run {
							copy(
								messagePreview = ConversationHelper.getLatestPreview(messagePreview, preview),
								unreadMessageCount = newConversationMessages.size
							)
						}
						
						val insertionIndex = ConversationHelper.findInsertionIndex(updatedConversation, list)
						list.add(insertionIndex, updatedConversation)
					}
				})
			}
			is ReduxEventMessaging.ConversationUnread -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].copy(
						unreadMessageCount = event.unreadCount
					)
				})
			}
			is ReduxEventMessaging.ConversationMember -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].copy(
						members = list[index].members.toMutableList().also { list ->
							if(event.isJoin) {
								list.add(event.member)
							} else {
								list.removeAll { it.address == event.member.address }
							}
						}
					)
				})
			}
			is ReduxEventMessaging.ConversationMute -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].copy(isMuted = event.isMuted)
				})
			}
			is ReduxEventMessaging.ConversationArchive -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].copy(isArchived = event.isArchived)
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
					list[index] = list[index].copy(title = event.title)
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
					list.add(insertionIndex, updatedConversation)
					
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
					list.add(insertionIndex, updatedConversation)
					
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
					list.add(insertionIndex, updatedConversation)
					
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
			ConversationActionTask.unreadConversations(unreadConversations, 0).await()
		}
	}
	
	/**
	 * Sets the active conversation for a text message conversation, resolving the conversation
	 * and creating it if necessary
	 */
	fun selectTextMessageConversation(participants: List<String>, receivedContent: ConversationReceivedContent) {
		viewModelScope.launch {
			val conversationID = withContext(Dispatchers.IO) {
				//Look up the conversation in the messages database
				val threadID = Telephony.Threads.getOrCreateThreadId(getApplication(), participants.toSet())
				
				//Find the conversation in the database
				val matchingConversationInfo = DatabaseManager.getInstance()
					.findConversationByExternalID(
						getApplication(),
						threadID,
						ServiceHandler.systemMessaging,
						ServiceType.systemSMS
					)
				
				matchingConversationInfo?.let {
					return@withContext it.localID
				}
				
				//Create a new conversation
				val conversationColor = ConversationColorHelper.getDefaultConversationColor(threadID)
				val coloredMembers = ConversationColorHelper.getColoredMembers(
					participants,
					conversationColor,
					threadID
				)
				val newConversationInfo = ConversationInfo(
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
				val result = DatabaseManager.getInstance().addConversationInfo(newConversationInfo)
				if(!result) return@withContext null
				
				//Add the conversation created message
				val firstMessage = DatabaseManager.getInstance().addConversationCreatedMessage(newConversationInfo.localID)
				
				//Emit an update
				withContext(Dispatchers.Main) {
					ReduxEmitterNetwork.messageUpdateSubject.onNext(
						ReduxEventMessaging.ConversationUpdate(
							newConversations = mapOf(newConversationInfo to listOf(firstMessage)),
							transferredConversations = listOf()
						)
					)
				}
				
				return@withContext newConversationInfo.localID
			}
			
			if(conversationID != null) {
				setSelectedConversation(
					conversationID = conversationID,
					content = receivedContent
				)
			}
		}
	}
	
	/**
	 * Sets the selected conversation, along with its associated received content
	 */
	fun setSelectedConversation(conversationID: Long, content: ConversationReceivedContent? = null) {
		detailPage = ConversationsDetailPage.Messaging(conversationID)
		
		if(content != null) {
			viewModelScope.launch {
				pendingReceivedContent.emit(
					PendingConversationReceivedContent(conversationID = conversationID, content = content)
				)
			}
		}
	}
	
	/**
	 * Gets a flow that emits received content for a specific conversation
	 */
	fun getPendingReceivedContentFlowForConversation(conversationID: Long): Flow<ConversationReceivedContent> {
		return pendingReceivedContent
			//Filter for when we have received content
			.filterNotNull()
			//Match the content to the requested conversation
			.filter { it.conversationID == conversationID }
			//Map to the content object
			.map { it.content }
	}
	
	/**
	 * Sets the pending received content, or NULL to clear
	 */
	fun setPendingReceivedContent(content: PendingConversationReceivedContent?) {
		viewModelScope.launch {
			pendingReceivedContent.emit(content)
		}
	}
	
	/**
	 * Replaces the current pending received content's conversation ID
	 * with the specified one if it is currently NULL.
	 */
	fun updatePendingReceivedContentTarget(conversationID: Long) {
		viewModelScope.launch {
			pendingReceivedContent.value?.let { content ->
				if(content.conversationID == null) {
					pendingReceivedContent.emit(PendingConversationReceivedContent(conversationID, content.content))
				}
			}
		}
	}
}

sealed interface ConversationsDetailPage {
	class Messaging(val conversationID: Long) : ConversationsDetailPage
	object NewConversation : ConversationsDetailPage
}
