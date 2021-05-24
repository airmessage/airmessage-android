package me.tagavari.airmessage.messaging.viewholder

import android.view.View
import android.widget.TextView
import me.tagavari.airmessage.util.DisposableViewHolder

/**
 * A view holder that contains a label for an action message
 */
class VHMessageAction(itemView: View, val label: TextView) : DisposableViewHolder(itemView)