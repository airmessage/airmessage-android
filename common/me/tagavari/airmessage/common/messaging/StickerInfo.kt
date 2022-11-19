package me.tagavari.airmessage.common.messaging

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import java.io.File

@Immutable
class StickerInfo : Parcelable {
	//Creating the sticker values
	val localID: Long
	val guid: String?
	val sender: String?
	val date: Long
	val file: File
	
	constructor(localID: Long, guid: String?, sender: String?, date: Long, file: File) {
		this.localID = localID
		this.guid = guid
		this.sender = sender
		this.date = date
		this.file = file
	}
	
	override fun describeContents(): Int {
		return 0
	}
	
	override fun writeToParcel(out: Parcel, flags: Int) {
		out.writeLong(localID)
		out.writeString(guid)
		out.writeString(sender)
		out.writeLong(date)
		out.writeString(file.path)
	}
	
	private constructor(parcel: Parcel) {
		localID = parcel.readLong()
		guid = parcel.readString()
		sender = parcel.readString()
		date = parcel.readLong()
		file = File(parcel.readString()!!)
	}
	
	companion object {
		@JvmField val CREATOR: Parcelable.Creator<StickerInfo> = object : Parcelable.Creator<StickerInfo> {
			override fun createFromParcel(parcel: Parcel) = StickerInfo(parcel)
			
			override fun newArray(size: Int) = arrayOfNulls<StickerInfo>(size)
		}
	}
}