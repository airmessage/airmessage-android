package me.tagavari.airmessage.flavor

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import me.tagavari.airmessage.exception.LocationDisabledException
import me.tagavari.airmessage.exception.LocationUnavailableException
import me.tagavari.airmessage.util.LatLngInfo
import kotlin.coroutines.resume

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
	suspend fun getLocation(activity: Activity): LatLngInfo {
		//Get the location manager
		val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		
		//Fail if location is disabled
		if(!LocationManagerCompat.isLocationEnabled(locationManager)) {
			throw LocationDisabledException()
		}
		
		//Find a valid provider
		val provider = buildList {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				add(LocationManager.FUSED_PROVIDER)
			}
			
			add(LocationManager.GPS_PROVIDER)
			add(LocationManager.NETWORK_PROVIDER)
		}
			.firstOrNull { locationManager.isProviderEnabled(it) }
			?: throw LocationUnavailableException()
		
		//Get the user's current location
		val location = suspendCancellableCoroutine<Location?> { cont ->
			val cancellationSignal = CancellationSignal()
			LocationManagerCompat.getCurrentLocation(
				locationManager,
				provider,
				cancellationSignal,
				ContextCompat.getMainExecutor(activity)
			) { location: Location? ->
				cont.resume(location)
			}
			
			cont.invokeOnCancellation {
				cancellationSignal.cancel()
			}
		} ?: throw LocationUnavailableException()
		
		return LatLngInfo(location.latitude, location.longitude)
	}
}
