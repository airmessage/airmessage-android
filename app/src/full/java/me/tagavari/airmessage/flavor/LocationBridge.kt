package me.tagavari.airmessage.flavor

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import me.tagavari.airmessage.common.exception.LocationUnavailableException
import me.tagavari.airmessage.common.helper.AndroidLocationHelper
import me.tagavari.airmessage.common.util.LatLngInfo

typealias ResolvableApiException = com.google.android.gms.common.api.ResolvableApiException

object LocationBridge {
	/**
	 * Gets the user's current location.
	 * On FULL, this uses GMS' FusedLocationProvider.
	 */
	@OptIn(ExperimentalCoroutinesApi::class)
	@SuppressLint("MissingPermission")
	@Throws(ResolvableApiException::class)
	suspend fun getLocation(activity: Activity): LatLngInfo {
		//Check if Google Play Services is usable
		if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
			!= ConnectionResult.SUCCESS) {
			//If Google Play Services aren't available, fall back to
			//a built-in Android method
			return AndroidLocationHelper.getLocation(activity)
		}
		
		//Check if the device is set up for location services
		LocationServices.getSettingsClient(activity)
			.checkLocationSettings(
				LocationSettingsRequest.Builder()
					.addLocationRequest(LocationRequest.create())
					.build()
			).await()
		
		//Request the user's current location
		val cancellationTokenSource = CancellationTokenSource()
		val location = LocationServices.getFusedLocationProviderClient(activity).getCurrentLocation(
			Priority.PRIORITY_HIGH_ACCURACY,
			cancellationTokenSource.token
		)
			.await<Location?>(cancellationTokenSource)
			?: throw LocationUnavailableException()
		
		return LatLngInfo(location.latitude, location.longitude)
	}
}
