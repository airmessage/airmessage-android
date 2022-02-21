package me.tagavari.airmessage.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerUpdateData(
    val id: Int,
    val protocolRequirement: List<Int>,
    val version: String,
    val notes: String,
    val remoteInstallable: Boolean
): Parcelable