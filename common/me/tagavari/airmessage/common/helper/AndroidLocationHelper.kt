package me.tagavari.airmessage.common.helper

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import me.tagavari.airmessage.common.exception.LocationDisabledException
import me.tagavari.airmessage.common.exception.LocationUnavailableException
import me.tagavari.airmessage.common.util.LatLngInfo
import kotlin.coroutines.resume

object AndroidLocationHelper {
	/**
	 * Gets the user's current location uses Android's built-in location provider
	 */
	@SuppressLint("MissingPermission")
	suspend fun getLocation(context: Context): LatLngInfo {
		//Get the location manager
		val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java)
			?: throw LocationUnavailableException()
		
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
				ContextCompat.getMainExecutor(context)
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