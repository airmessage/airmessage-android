package me.tagavari.airmessage.flavor

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.IntentSender
import me.tagavari.airmessage.helper.AndroidLocationHelper

//Stub implementation of GMS' ResolvableApiException
@Suppress("UNUSED", "UNUSED_PARAMETER")
class ResolvableApiException private constructor() : Exception() {
	val resolution: PendingIntent
		get() = throw IllegalStateException()
	
	@Throws(IntentSender.SendIntentException::class)
	fun startResolutionForResult(activity: Activity, requestCode: Int) {
		throw IllegalStateException()
	}
}

object LocationBridge {
	/**
	 * Gets the user's current location.
	 * On FOSS, this uses Android's location services
	 */
	@SuppressLint("MissingPermission")
	suspend fun getLocation(activity: Activity) =
		AndroidLocationHelper.getLocation(activity)
}
