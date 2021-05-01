package me.tagavari.airmessage.messaging

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MemberInfo(val address: String, var color: Int) : Parcelable {
	fun clone(): MemberInfo = MemberInfo(address, color)
}