package me.tagavari.airmessage.flavor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import me.tagavari.airmessage.util.LatLngInfo
import kotlin.coroutines.resume

typealias ResolvableApiException = Nothing

object LocationBridge {
	/**
	 * Gets the user's current location.
	 * On FOSS, this uses Android's location services
	 */
	@SuppressLint("MissingPermission")
	suspend fun getLocation(activity: Activity): LatLngInfo? {
		//Get the location manager
		val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		
		//Fail if location is disabled
		if(!LocationManagerCompat.isLocationEnabled(locationManager)) {
			//Ask user to enable location?
			return null
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
			?: return null
		
		//Get the user's current location
		val location = suspendCancellableCoroutine { cont ->
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
		}
		
		return location?.let {
			LatLngInfo(it.latitude, it.longitude)
		}
	}
}
