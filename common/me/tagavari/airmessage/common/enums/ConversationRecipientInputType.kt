package me.tagavari.airmessage.common.enums

import androidx.compose.ui.text.input.KeyboardType

/**
 * A keyboard input method for entering message recipients
 */
enum class ConversationRecipientInputType(val keyboardType: KeyboardType) {
	PHONE(KeyboardType.Phone),
	EMAIL(KeyboardType.Email)
}
