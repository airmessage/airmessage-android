package me.tagavari.airmessage.messaging.viewholder

import android.app.PendingIntent.CanceledException
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.util.Consumer
import androidx.recyclerview.widget.RecyclerView
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.ResourceHelper.dpToPx
import me.tagavari.airmessage.messaging.AMConversationAction

class VHConversationActions(
	itemView: View,
	private val scrollView: HorizontalScrollView,
	private val container: LinearLayout
) : RecyclerView.ViewHolder(itemView) {
	/**
	 * Restores the default scroll position of this view
	 */
	fun resetScroll() {
		scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_RIGHT) }
	}
	
	/**
	 * Binds this view to an array of conversation actions
	 * @param context The context to use
	 * @param actions The actions to bind
	 * @param colorAccent The color to use as the accent
	 * @param sendMessageCallback A callback used when a 'reply' action is selected
	 */
	fun setActions(
		context: Context,
		actions: List<AMConversationAction>,
		colorAccent: Int,
		sendMessageCallback: Consumer<String>
	) {
		//Gathering active views
		val childViewList: MutableList<TextView> = ArrayList(actions.size)
		for(i in 0 until container.childCount) {
			val childView = container.getChildAt(i) as TextView
			if(i < actions.size) {
				childView.visibility = View.VISIBLE
				childViewList.add(childView)
			} else {
				childView.visibility = View.GONE
			}
		}
		
		//Adding more views if necessary
		while(childViewList.size < actions.size) {
			val item = LayoutInflater.from(context).inflate(R.layout.listitem_replysuggestions_item, container, false) as TextView
			item.setTextColor(colorAccent)
			container.addView(item)
			childViewList.add(item)
		}
		
		//Configuring the suggestion
		for(i in actions.indices) {
			val childView = childViewList[i]
			val conversationAction = actions[i]
			
			if(conversationAction.isReplyAction) {
				childView.setCompoundDrawables(null, null, null, null)
				childView.text = conversationAction.replyString
				childView.setOnClickListener { sendMessageCallback.accept(conversationAction.replyString.toString()) }
			} else {
				//Getting the remote action
				val remoteAction = conversationAction.remoteAction!!
				
				//Configuring the remote action
				if(remoteAction.icon != null) {
					val icon = remoteAction.icon.loadDrawable(context)
					val iconSize = dpToPx(24f)
					icon.setBounds(0, 0, iconSize, iconSize)
					childView.setCompoundDrawablesRelative(icon, null, null, null)
				} else {
					childView.setCompoundDrawables(null, null, null, null)
				}
				childView.text = remoteAction.title
				childView.setOnClickListener {
					//Launching the intent
					try {
						remoteAction.actionIntent.send()
					} catch(exception: CanceledException) {
						exception.printStackTrace()
					}
				}
			}
		}
	}
}