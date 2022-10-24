package me.tagavari.airmessage.util

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class LatLngInfo(val latitude: Double, val longitude: Double) : Parcelable
