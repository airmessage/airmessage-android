package me.tagavari.airmessage.messaging

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.enums.MessageSendErrorCode
import me.tagavari.airmessage.enums.MessageState
import me.tagavari.airmessage.enums.MessageViewType

class MessageInfo @JvmOverloads constructor(
	localID: Long,
	serverID: Long,
	guid: String?,
	date: Long,
	var sender: String?,
	var messageTextComponent: MessageComponentText?,
	var attachments: MutableList<AttachmentInfo>,
	var sendStyle: String?,
	@get:JvmName("isSendStyleViewed") var sendStyleViewed: Boolean,
	var dateRead: Long,
	@field:MessageState @get:MessageState @setparam:MessageState @param:MessageState var messageState: Int,
	@field:MessageSendErrorCode @get:MessageSendErrorCode @setparam:MessageSendErrorCode @param:MessageSendErrorCode var errorCode: Int,
	@get:JvmName("isErrorDetailsAvailable") @set:JvmName("setErrorDetailsAvailable") var errorDetailsAvailable: Boolean,
	var errorDetails: String? = null
) : ConversationItem(localID, serverID, guid, date), Parcelable {
	constructor(
		localID: Long,
		serverID: Long,
		guid: String?,
		date: Long,
		sender: String?,
		messageText: String?,
		messageSubject: String?,
		attachments: MutableList<AttachmentInfo>,
		sendStyle: String?,
		sendStyleViewed: Boolean,
		dateRead: Long,
		messageState: Int,
		errorCode: Int,
		errorDetailsAvailable: Boolean,
		errorDetails: String?
	) : this(
		localID, serverID, guid, date, sender,
		MessageComponentText.fromText(localID, guid, messageText, messageSubject),
		attachments, sendStyle, sendStyleViewed, dateRead, messageState, errorCode, errorDetailsAvailable, errorDetails
	)
	
	override val itemType = ConversationItemType.message
	override val itemViewType = MessageViewType.message
	
	//Updating the local ID of the message text
	override var localID: Long
		get() = super.localID
		set(value) {
			super.localID = value
			
			//Updating the local ID of the message text
			messageTextComponent?.localID = value
		}
	
	val isOutgoing: Boolean
		get() = sender == null
	
	val messageText: String?
		get() = messageTextComponent?.text
	
	val messageSubject: String?
		get() = messageTextComponent?.subject
	
	/**
	 * Gets a list of this message's components (text and attachments)
	 */
	val components: List<MessageComponent>
		get() {
			val list = mutableListOf<MessageComponent>()
			messageTextComponent?.let(list::add)
			list.addAll(attachments)
			return list
		}
	
	/**
	 * Gets the total number of components of this message
	 */
	val componentCount: Int
		get() = if(messageTextComponent != null) attachments.size + 1 else attachments.size
	
	/**
	 * Gets the component at the specified index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * (`index < 0 || index >= getComponentCount()`)
	 */
	fun getComponentAt(index: Int): MessageComponent {
		return if(messageTextComponent != null) {
			if(index == 0) messageTextComponent!! else attachments[index - 1]
		} else {
			attachments[index]
		}
	}
	
	@get:JvmName("hasError")
	val hasError: Boolean
		get() = errorCode != MessageSendErrorCode.none
	
	override fun clone(): MessageInfo {
		return MessageInfo(
			localID,
			serverID,
			guid,
			date,
			sender,
			messageTextComponent,
			attachments,
			sendStyle,
			sendStyleViewed,
			dateRead,
			messageState,
			errorCode,
			errorDetailsAvailable,
			errorDetails
		)
	}
	
	override fun describeContents(): Int {
		return 0
	}
	
	override fun writeToParcel(out: Parcel, flags: Int) {
		//Superclass
		out.writeLong(localID)
		out.writeLong(serverID)
		out.writeString(guid)
		out.writeLong(date)
		out.writeString(sender)
		out.writeParcelable(messageTextComponent, 0)
		out.writeTypedList(attachments)
		out.writeString(sendStyle)
		ParcelCompat.writeBoolean(out, sendStyleViewed)
		out.writeInt(messageState)
		out.writeLong(dateRead)
		out.writeInt(errorCode)
		ParcelCompat.writeBoolean(out, errorDetailsAvailable)
		out.writeString(errorDetails)
	}
	
	private constructor(parcel: Parcel) : this(
		localID = parcel.readLong(),
		serverID = parcel.readLong(),
		guid = parcel.readString(),
		date = parcel.readLong(),
		
		sender = parcel.readString(),
		messageTextComponent = parcel.readParcelable<MessageComponentText>(MessageComponentText::class.java.classLoader),
		attachments = mutableListOf<AttachmentInfo>().also { parcel.readTypedList(it, AttachmentInfo.CREATOR) },
		sendStyle = parcel.readString(),
		sendStyleViewed = ParcelCompat.readBoolean(parcel),
		messageState = parcel.readInt(),
		dateRead = parcel.readLong(),
		errorCode = parcel.readInt(),
		errorDetailsAvailable = ParcelCompat.readBoolean(parcel),
		errorDetails = parcel.readString()
	)
	
	companion object {
		/**
		 * Creates a new message in a default unsent state from the provided text
		 */
		@kotlin.jvm.JvmStatic
		fun blankFromText(message: String?): MessageInfo {
			return MessageInfo(
				-1,
				-1,
				null,
				System.currentTimeMillis(),
				null,
				message,
				null,
				mutableListOf(),
				null,
				false,
				-1,
				MessageState.ghost,
				MessageSendErrorCode.none,
				false,
				null
			)
		}
		
		@JvmField val CREATOR: Parcelable.Creator<MessageInfo> = object : Parcelable.Creator<MessageInfo> {
			override fun createFromParcel(parcel: Parcel) = MessageInfo(parcel)
			override fun newArray(size: Int) = arrayOfNulls<MessageInfo>(size)
		}
	}
}