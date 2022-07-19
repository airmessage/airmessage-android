package me.tagavari.airmessage.flavor

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.annotation.IntDef
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import me.tagavari.airmessage.enums.LocationState
import me.tagavari.airmessage.fragment.FragmentMessagingAttachments

class FragmentMessagingAttachmentsViewModelMaps(private val viewModel: FragmentMessagingAttachments.FragmentViewModel) {
    val locationStateLD = MutableLiveData<Int>()
    var attachmentsLocationResult: Location? = null
    var attachmentsLocationResolvable: ResolvableApiException? = null
    
    /**
     * Updates the location state and attempts to initialize location services
     */
    fun loadLocation(): Unit = viewModel.run {
        //Checking if we don't have permission
        if(getApplication<Application>().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Setting the state to permission requested
            locationStateLD.value = LocationState.permission
            return
        }
        
        //Setting the state to loading
        locationStateLD.value = LocationState.loading
        
        val locationProvider: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplication() as Context)
        val locationRequest: LocationRequest = LocationRequest.create()
        
        //Requesting location services status
        val task = LocationServices.getSettingsClient(getApplication() as Context)
                .checkLocationSettings(LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build())
        task.addOnCompleteListener { taskResult ->
            //Restoring the loading state
            try {
                //Getting the result
                taskResult.getResult(ApiException::class.java) //Forces exception to be thrown if needed
                
                //Getting user's location
                locationProvider.lastLocation.addOnSuccessListener { location: Location? ->
                    if(location == null) {
                        //Pulling an update from location services
                        locationProvider.requestLocationUpdates(locationRequest, object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                //Removing the updater
                                locationProvider.removeLocationUpdates(this)
                                
                                //Setting the location
                                attachmentsLocationResult = locationResult.lastLocation
                                locationStateLD.value = LocationState.ready
                            }
                        }, Looper.getMainLooper())
                    } else {
                        //Setting the location
                        attachmentsLocationResult = location
                        locationStateLD.setValue(LocationState.ready)
                    }
                }
            } catch(exception: ApiException) {
                when(exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        //Setting the resolvable
                        attachmentsLocationResolvable = exception as ResolvableApiException
                        locationStateLD.setValue(LocationState.resolvable)
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> locationStateLD.setValue(LocationState.unavailable)
                    else -> locationStateLD.setValue(LocationState.failed)
                }
            }
        }
    }
}