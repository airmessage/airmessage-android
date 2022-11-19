package me.tagavari.airmessage.common.util

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class ServerUpdateData(
    val id: Int,
    val protocolRequirement: List<Int>,
    val version: String,
    val notes: String,
    val remoteInstallable: Boolean
): Parcelable
