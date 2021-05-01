package me.tagavari.airmessage.messaging.viewholder

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * A view holder that contains a complete conversation listing
 */
class VHConversationDetailed(
	itemView: View,
	iconGroup: ViewGroup,
	conversationTitle: TextView,
	
	//A selection indicator, used in multi-select mode
	val selectionIndicator: View,
	val selectionHighlight: View,
	
	//The latest message and its time
	val labelMessage: TextView,
	val labelStatus: TextView,
	
	//A counter for unread messages
	val labelUnread: TextView,
	
	//Icon indicators for muted and draft status
	val flagMuted: View,
	val flagDraft: View
) : VHConversationBase(itemView, iconGroup, conversationTitle)