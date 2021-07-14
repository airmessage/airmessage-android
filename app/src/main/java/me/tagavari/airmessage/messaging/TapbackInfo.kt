package me.tagavari.airmessage.messaging

import android.os.Parcel
import android.os.Parcelable
import me.tagavari.airmessage.enums.TapbackType

data class TapbackInfo(
	var localID: Long,
	var sender: String?,
	@get:TapbackType @field:TapbackType @param:TapbackType var code: Int
) : Parcelable {
	override fun describeContents(): Int {
		return 0
	}
	
	override fun writeToParcel(out: Parcel, flags: Int) {
		out.writeLong(localID)
		out.writeString(sender)
		out.writeInt(code)
	}
	
	private constructor(parcel: Parcel): this(
		localID = parcel.readLong(),
		sender = parcel.readString(),
		code = parcel.readInt()
	)
	
	companion object {
		@JvmField val CREATOR: Parcelable.Creator<TapbackInfo> = object : Parcelable.Creator<TapbackInfo> {
			override fun createFromParcel(parcel: Parcel) = TapbackInfo(parcel)
			
			override fun newArray(size: Int) = arrayOfNulls<TapbackInfo>(size)
		}
	}
}