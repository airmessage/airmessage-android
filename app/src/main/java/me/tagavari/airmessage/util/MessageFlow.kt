package me.tagavari.airmessage.util

/**
 * A message's position in the thread in accordance with other nearby messages
 */
data class MessageFlow(
	//Whether this message should be anchored to the message above
	val anchorTop: Boolean,
	//Whether this message should be anchored to the message below
	val anchorBottom: Boolean
)
