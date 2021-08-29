package me.tagavari.airmessage.activitypart

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.MessageComponentType
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.helper.TypeHelper
import me.tagavari.airmessage.messaging.AttachmentInfo
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.MessageComponent

class MessagingActionModeCallback(
	var onDetails: Runnable?,
	var onCopy: Runnable?,
	var onShare: Runnable?,
	var onSave: Runnable?,
	var onDeleteData: Runnable?,
	var onCreate: Runnable?,
	var onDestroy: Runnable?
): ActionMode.Callback {
	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		//Inflating the menu
		val inflater = mode.menuInflater
		inflater.inflate(R.menu.menu_conversationitem_contextual, menu)
		
		//Calling the listener
		onCreate?.run()
		
		return true
	}
	
	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		return false
	}
	
	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_details -> onDetails?.run()
			R.id.action_copytext -> onCopy?.run()
			R.id.action_share -> onShare?.run()
			R.id.action_save -> onSave?.run()
			R.id.action_deletedata -> onDeleteData?.run()
			else -> return false
		}
		
		mode.finish()
		return true
	}
	
	override fun onDestroyActionMode(mode: ActionMode) {
		onDestroy?.run()
	}
	
	fun updateMenu(mode: ActionMode, conversationInfo: ConversationInfo, messageComponent: MessageComponent) {
		//Getting the component type
		val componentType = TypeHelper.getMessageComponentType(messageComponent)
		
		//Updating the item visibility
		val menu = mode.menu
		val isText = componentType == MessageComponentType.text
		val hasAttachmentFile = !isText && (messageComponent as AttachmentInfo).file != null
		
		menu.findItem(R.id.action_save).isVisible = !isText //Only for attachments
		menu.findItem(R.id.action_copytext).isVisible = isText //Only for text
		menu.findItem(R.id.action_share).isVisible = isText || hasAttachmentFile //Only for content
		menu.findItem(R.id.action_save).isVisible = hasAttachmentFile //Only attachments with content
		menu.findItem(R.id.action_deletedata).isVisible = !isText && hasAttachmentFile &&
				conversationInfo.serviceHandler == ServiceHandler.appleBridge //Only for attachments with files served over AirMessage
	}
}