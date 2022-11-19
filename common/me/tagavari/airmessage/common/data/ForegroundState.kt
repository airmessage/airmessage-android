package me.tagavari.airmessage.common.data

object ForegroundState {
	var isInForeground = false
	
	//A list of all conversation IDs that are loaded
	val loadedConversationIDs = mutableListOf<Long>()
	
	//All list of all conversation IDs loaded in the foreground,
	//or an empty list if the app is not in the foreground
	val foregroundConversationIDs: Set<Long>
		get() {
			return if(isInForeground) {
				loadedConversationIDs.toSet()
			} else {
				setOf()
			}
		}
	
	var conversationListLoadCount = 0
	
	//Whether the conversation list is being displayed,
	//and the app is in the foreground
	val isConversationListForeground get() = isInForeground && conversationListLoadCount > 0
}