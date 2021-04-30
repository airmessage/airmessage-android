package me.tagavari.airmessage.redux

import me.tagavari.airmessage.redux.ReduxEventMassRetrieval
import me.tagavari.airmessage.enums.MassRetrievalErrorCode
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem

//An event to represent the status of a mass retrieval
abstract class ReduxEventMassRetrieval(val requestID: Short) {
	class Start(
		requestID: Short,
		val conversations: Collection<ConversationInfo>,
		val messageCount: Int
	) : ReduxEventMassRetrieval(requestID)
	
	class Progress(
		requestID: Short,
		val items: Collection<ConversationItem>,
		val receivedItems: Int,
		val totalItems: Int
	) : ReduxEventMassRetrieval(requestID)
	
	class File(requestID: Short) : ReduxEventMassRetrieval(requestID)
	
	class Complete(requestID: Short) : ReduxEventMassRetrieval(requestID)
	
	class Error(requestID: Short, @field:MassRetrievalErrorCode val code: Int) : ReduxEventMassRetrieval(requestID)
}