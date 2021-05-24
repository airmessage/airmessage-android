package me.tagavari.airmessage.messaging

import android.os.Parcel
import android.os.Parcelable
import me.tagavari.airmessage.enums.MessagePreviewState

abstract class MessageComponent(
	var localID: Long,
	@get:JvmName("getGUID") @set:JvmName("setGUID") var guid: String?,
	val stickers: MutableList<StickerInfo> = mutableListOf(),
	val tapbacks: MutableList<TapbackInfo> = mutableListOf()
) : Parcelable {
	@get:MessagePreviewState @MessagePreviewState var messagePreviewState = MessagePreviewState.notTried
	var messagePreviewID: Long = -1
	
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeLong(localID)
		parcel.writeString(guid)
		parcel.writeTypedList(stickers)
		parcel.writeTypedList(tapbacks)
		
		parcel.writeInt(messagePreviewState)
		parcel.writeLong(messagePreviewID)
	}
	
	protected constructor(parcel: Parcel) : this(
		localID = parcel.readLong(),
		guid = parcel.readString(),
		stickers = mutableListOf<StickerInfo>().also { parcel.readTypedList(it, StickerInfo.CREATOR) },
		tapbacks = mutableListOf<TapbackInfo>().also { parcel.readTypedList(it, TapbackInfo.CREATOR) }
	) {
		messagePreviewState = parcel.readInt()
		messagePreviewID = parcel.readLong()
	}
}