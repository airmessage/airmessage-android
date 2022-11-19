package me.tagavari.airmessage.common.util

import me.tagavari.airmessage.common.enums.MessageState

/**
 * A class for passing around data related to an activity status update
 * @param messageID The ID of the message that is being updated
 * @param messageState The message's new state
 * @param dateRead The date the message was read
 */
data class ActivityStatusUpdate @JvmOverloads constructor(
	val messageID: Long,
	@field:MessageState @get:MessageState @param:MessageState val messageState: Int,
	val dateRead: Long = -1
)