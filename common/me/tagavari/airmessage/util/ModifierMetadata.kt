package me.tagavari.airmessage.util

/**
 * A helper class to hold data about the positioning of a modifier
 * @param messageID The ID of the message the modifier is attached to
 * @param componentIndex The index of the component the modifier is attached to
 */
data class ModifierMetadata(val messageID: Long, val componentIndex: Int)