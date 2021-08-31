package me.tagavari.airmessage.messaging.viewholder

import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

class VHTapbackSelector(
	itemView: View,
	private val buttonLove: ImageButton,
	private val buttonLike: ImageButton,
	private val buttonDislike: ImageButton,
	private val buttonLaugh: ImageButton,
	private val buttonExclamation: ImageButton,
	private val buttonQuestion: ImageButton,
) : RecyclerView.ViewHolder(itemView)