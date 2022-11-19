package me.tagavari.airmessage.common.messaging

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import me.tagavari.airmessage.common.enums.TapbackType

@Immutable
data class TapbackInfo(
	val localID: Long,
	val sender: String?,
	@TapbackType val code: Int
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