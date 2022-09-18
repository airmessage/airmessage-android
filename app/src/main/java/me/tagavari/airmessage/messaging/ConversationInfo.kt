package me.tagavari.airmessage.messaging

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.tagavari.airmessage.BuildConfig
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.MMSSMSHelper.getMaxMessageSize
import me.tagavari.airmessage.util.ConversationTarget
import me.tagavari.airmessage.util.ConversationTarget.*

@Parcelize
data class ConversationInfo @JvmOverloads constructor(
	var localID: Long,
	@get:JvmName("getGUID") @set:JvmName("setGUID") var guid: String?,
	var externalID: Long,
	var state: Int,
	var serviceHandler: Int,
	var serviceType: String?,
	var conversationColor: Int = 0xFF000000.toInt(), //Black
	var members: MutableList<MemberInfo>,
	var title: String?,
	var unreadMessageCount: Int = 0,
	var isArchived: Boolean = false,
	var isMuted: Boolean = false,
	var messagePreview: ConversationPreview? = null,
	var draftMessage: String? = null,
	var draftFiles: MutableList<FileDraft> = mutableListOf(),
	var draftUpdateTime: Long = -1
) : Parcelable {
	/**
	 * Gets whether this conversation is a group conversation
	 */
	val isGroupChat: Boolean
		get() = members.size > 1
	
	/**
	 * Constructs and returns a [ConversationPreview] from the draft data
	 */
	val draftPreview: ConversationPreview?
		get() {
			val draftUpdateTime = draftUpdateTime
			
			return if(draftUpdateTime == -1L || draftMessage == null && draftFiles.isEmpty()) {
				null
			} else {
				ConversationPreview.Draft(
					draftUpdateTime,
					draftMessage,
					draftFiles.map { draft: FileDraft -> AttachmentPreview(draft.fileName, draft.fileType) })
			}
		}
	
	/**
	 * Gets either the message preview or draft preview for this conversation, whichever is more recent
	 */
	val dynamicPreview: ConversationPreview?
		get() {
			val messagePreview = messagePreview
			val draftPreview = draftPreview
			
			return when {
				messagePreview == null && draftPreview == null -> null
				messagePreview == null -> draftPreview
				draftPreview == null -> messagePreview
				messagePreview.date > draftPreview.date -> messagePreview
				else -> draftPreview
			}
		}
	
	/**
	 * Gets the conversation target of this
	 */
	val conversationTarget: ConversationTarget
		get() = if(serviceHandler == ServiceHandler.appleBridge) {
			if(state == ConversationState.incompleteClient) {
				if(BuildConfig.DEBUG && serviceType == null) error("Service type is null")
				AppleUnlinked(members.map { it.address }, serviceType!!)
			} else {
				if(BuildConfig.DEBUG && guid == null) error("GUID ID is null")
				AppleLinked(guid!!)
			}
		} else {
			if(BuildConfig.DEBUG && externalID == -1L) error("External ID is null")
			SystemSMS(externalID)
		}
	
	/**
	 * Gets the maximum file size for attachments for the current messaging service
	 */
	val fileCompressionTarget: Int?
		get() {
			//Apple continuity MMS messaging
			if(serviceHandler == ServiceHandler.appleBridge && serviceType == ServiceType.appleSMS) {
				//Default to 300 KB
				return 300 * 1024
			}
			
			//Android system MMS messaging
			if(serviceHandler == ServiceHandler.systemMessaging && serviceType == ServiceType.systemSMS) {
				//Return the carrier-specific information
				return getMaxMessageSize(MainApplication.instance)
			}
			
			//No compression required
			return null
		}
	
	fun clone(): ConversationInfo {
		return ConversationInfo(
			localID,
			guid,
			externalID,
			state,
			serviceHandler,
			serviceType,
			conversationColor,
			members.toMutableList(),
			title,
			unreadMessageCount,
			isArchived,
			isMuted,
			messagePreview,
			draftMessage,
			draftFiles.toMutableList(),
			draftUpdateTime
		)
	}
}