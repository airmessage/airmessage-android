package me.tagavari.airmessage.data

object ForegroundState {
	//A list of all conversation IDs that are in the foreground
	val conversationIDs = mutableListOf<Long>()
	
	var conversationListCount = 0
	val conversationListForegrounded get() = conversationListCount > 0
}