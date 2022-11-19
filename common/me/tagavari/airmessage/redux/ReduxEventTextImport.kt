package me.tagavari.airmessage.redux

import me.tagavari.airmessage.messaging.ConversationInfo

//An event to represent the status of a mass retrieval
abstract class ReduxEventTextImport {
	class Start(val itemCount: Int) : ReduxEventTextImport()
	class Progress(val receivedItems: Int, val totalItems: Int) : ReduxEventTextImport()
	class Complete(val conversations: Collection<ConversationInfo>) : ReduxEventTextImport()
	class Fail : ReduxEventTextImport()
}