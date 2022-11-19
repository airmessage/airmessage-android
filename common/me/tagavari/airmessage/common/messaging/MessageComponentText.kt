package me.tagavari.airmessage.common.messaging

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable

/**
 * An object that represents the text part of
 * a message
 */
@Immutable
class MessageComponentText : MessageComponent {
	val text: String?
	val subject: String?
	
	constructor(
		localID: Long,
		guid: String?,
		messageText: String?,
		messageSubject: String?
	) : super(localID, guid) {
		text = messageText
		subject = messageSubject
	}
	
	constructor(
		localID: Long,
		guid: String?,
		stickers: MutableList<StickerInfo>,
		tapbacks: MutableList<TapbackInfo>,
		previewState: Int,
		previewID: Long,
		messageText: String?,
		messageSubject: String?
	) : super(localID, guid, stickers, tapbacks, previewState, previewID) {
		text = messageText
		subject = messageSubject
	}
	
	override fun describeContents(): Int {
		return 0
	}
	
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		super.writeToParcel(parcel, flags)
		parcel.writeString(text)
		parcel.writeString(subject)
	}
	
	override fun copy(
		localID: Long,
		guid: String?,
		stickers: MutableList<StickerInfo>,
		tapbacks: MutableList<TapbackInfo>,
		previewState: Int,
		previewID: Long
	) = MessageComponentText(
		localID,
		guid,
		stickers,
		tapbacks,
		previewState,
		previewID,
		text,
		subject
	)
	
	private constructor(parcel: Parcel) : super(parcel) {
		text = parcel.readString()
		subject = parcel.readString()
	}
	
	companion object {
		/**
		 * Gets a text message component from a body string and a subject string
		 * @param localID The local ID of this text component
		 * @param guid The GUID of this text component
		 * @param body The body text of this message component (or NULL if unavailable)
		 * @param subject The subject text of this message component (or NULL if unavailable)
		 * @return A message component if valid, otherwise NULL
		 */
		fun fromText(localID: Long, guid: String?, body: String?, subject: String?): MessageComponentText? {
			//No message text if there is no text to begin with
			return if(body == null && subject == null) {
				null
			} else {
				MessageComponentText(localID, guid, body, subject)
			}
		}
		
		fun fromText(localID: Long, guid: String?, body: String?, subject: String?, previewState: Int, previewID: Long): MessageComponentText? {
			//No message text if there is no text to begin with
			return if(body == null && subject == null) {
				null
			} else {
				MessageComponentText(localID, guid, mutableListOf(), mutableListOf(), previewState, previewID, body, subject)
			}
		}
		
		@JvmField val CREATOR: Parcelable.Creator<MessageComponentText> = object : Parcelable.Creator<MessageComponentText> {
			override fun createFromParcel(parcel: Parcel) = MessageComponentText(parcel)
			override fun newArray(size: Int) = arrayOfNulls<MessageComponentText>(size)
		}
	}
}