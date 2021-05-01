package me.tagavari.airmessage.messaging.viewholder

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import me.tagavari.airmessage.util.DisposableViewHolder

/**
 * A view holder that contains a conversation title and an icon
 */
open class VHConversationBase(
	itemView: View,
	val iconGroup: ViewGroup,
	val conversationTitle: TextView
) : DisposableViewHolder(itemView)