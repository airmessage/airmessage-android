package me.tagavari.airmessage.common.util

/**
 * Represents a conversation that can be sent to
 */
interface ConversationTarget {
	//A serverlinked Apple Messages chat
	data class AppleLinked(val guid: String) : ConversationTarget
	
	//An unlinked potential Apple Messages chat
	data class AppleUnlinked(val members: List<String>, val service: String) : ConversationTarget
	
	//A local SMS chat
	data class SystemSMS(val externalID: Long) : ConversationTarget
}