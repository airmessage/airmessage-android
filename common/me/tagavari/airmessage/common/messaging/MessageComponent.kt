package me.tagavari.airmessage.common.messaging

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import me.tagavari.airmessage.common.enums.MessagePreviewState

/**
 * An object that represents a part of a message,
 * such as text or an attachment file
 */
@Immutable
abstract class MessageComponent(
	var localID: Long,
	@get:JvmName("getGUID") val guid: String?,
	val stickers: MutableList<StickerInfo> = mutableListOf(),
	val tapbacks: MutableList<TapbackInfo> = mutableListOf(),
	@MessagePreviewState val previewState: Int = MessagePreviewState.notTried,
	val previewID: Long = -1L
) : Parcelable {
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeLong(localID)
		parcel.writeString(guid)
		parcel.writeTypedList(stickers)
		parcel.writeTypedList(tapbacks)
		
		parcel.writeInt(previewState)
		parcel.writeLong(previewID)
	}
	
	protected constructor(parcel: Parcel) : this(
		localID = parcel.readLong(),
		guid = parcel.readString(),
		stickers = mutableListOf<StickerInfo>().also { parcel.readTypedList(it, StickerInfo.CREATOR) },
		tapbacks = mutableListOf<TapbackInfo>().also { parcel.readTypedList(it, TapbackInfo.CREATOR) },
		previewState = parcel.readInt(),
		previewID = parcel.readLong()
	)
	
	abstract fun copy(
		localID: Long = this.localID,
		guid: String? = this.guid,
		stickers: MutableList<StickerInfo> = this.stickers,
		tapbacks: MutableList<TapbackInfo> = this.tapbacks,
		@MessagePreviewState previewState: Int = this.previewState,
		previewID: Long = this.previewID
	): MessageComponent
}