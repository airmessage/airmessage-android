package me.tagavari.airmessage.flavor

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import me.tagavari.airmessage.util.LatLngInfo

typealias ResolvableApiException = com.google.android.gms.common.api.ResolvableApiException

object LocationBridge {
	/**
	 * Gets the user's current location.
	 * On FULL, this uses GMS' FusedLocationProvider.
	 */
	@OptIn(ExperimentalCoroutinesApi::class)
	@SuppressLint("MissingPermission")
	@Throws(ResolvableApiException::class)
	suspend fun getLocation(activity: Activity): LatLngInfo? {
		//Get the location manager
		val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
		
		//Request the user's current location
		val cancellationTokenSource = CancellationTokenSource()
		val location: Location? = fusedLocationClient.getCurrentLocation(
			Priority.PRIORITY_HIGH_ACCURACY,
			cancellationTokenSource.token
		).await(cancellationTokenSource)
		
		return location?.let {
			LatLngInfo(it.latitude, it.longitude)
		}
	}
}
