package me.tagavari.airmessage.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLngInfo(val latitude: Double, val longitude: Double) : Parcelable
