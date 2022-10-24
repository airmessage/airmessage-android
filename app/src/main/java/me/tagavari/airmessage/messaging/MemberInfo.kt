package me.tagavari.airmessage.messaging

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class MemberInfo(val address: String, val color: Int) : Parcelable {
	fun clone(): MemberInfo = MemberInfo(address, color)
}
