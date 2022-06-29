package me.tagavari.airmessage.flavor

import android.Manifest
import android.app.Activity
import android.location.Location
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.LocationPicker
import me.tagavari.airmessage.activity.LocationPicker.ResultContract
import me.tagavari.airmessage.enums.LocationState
import me.tagavari.airmessage.fragment.FragmentMessagingAttachments
import me.tagavari.airmessage.helper.ThemeHelper.isNightMode
import me.tagavari.airmessage.util.LatLngInfo
import java.lang.IllegalArgumentException

class FragmentMessagingAttachmentsMaps(val fragment: FragmentMessagingAttachments) {
    private val requestLocationPermissionLauncher: ActivityResultLauncher<Array<String>> = fragment.registerForActivityResult<Array<String>, Map<String, Boolean>>(RequestMultiplePermissions()) { permissions: Map<String, Boolean> ->
        //Check if all permissions are granted
        if(permissions.values.stream().allMatch { it }) {
            //Loading the location
            fragment.viewModel.flavorExtensionMaps.loadLocation()
        }
    }
    
    private val locationPickerLauncher: ActivityResultLauncher<Location> = fragment.registerForActivityResult(ResultContract()) { result: LocationPicker.Result? ->
        if(result == null) return@registerForActivityResult
    
        //Checking if this is an iMessage conversation
        if(fragment.supportsAppleContent) {
            //Writing the file and creating the attachment data
            fragment.queueLocation(LatLngInfo(result.location.latitude, result.location.longitude), result.address, result.name)
        } else {
            //Creating the query string
            val query = result.address ?: (result.location.latitude.toString() + "," + result.location.longitude.toString())
        
            //Building the Google Maps URL
            val mapsUri = Uri.Builder()
                    .scheme("https")
                    .authority("www.google.com")
                    .appendPath("maps")
                    .appendPath("search")
                    .appendPath("")
                    .appendQueryParameter("api", "1")
                    .appendQueryParameter("query", query)
                    .build()
        
            //Appending the generated URL to the text box
            fragment.communicationsCallback?.queueText(mapsUri.toString())
        }
    }
    
    private val resolveLocationServicesLauncher: ActivityResultLauncher<IntentSenderRequest> = fragment.registerForActivityResult(StartIntentSenderForResult()) { result: ActivityResult? ->
        //Updating the attachment section
        if(result?.resultCode == Activity.RESULT_OK) {
            fragment.viewModel.flavorExtensionMaps.loadLocation()
        }
    }
    
    //Location views
    private lateinit var viewGroupLocation: ViewGroup
    private lateinit var viewGroupLocationAction: ViewGroup
    private lateinit var labelLocationAction: TextView
    private lateinit var viewGroupLocationContent: ViewGroup
    
    fun initViews(view: View): Unit = fragment.run {
        //Loading views
        viewGroupLocation = view.findViewById(R.id.viewgroup_attachment_location)
        viewGroupLocationAction = viewGroupLocation.findViewById(R.id.button_attachment_location_action)
        labelLocationAction = viewGroupLocationAction.findViewById(R.id.button_attachment_location_action_label)
        viewGroupLocationContent = viewGroupLocation.findViewById(R.id.frame_attachment_location_content)
    
        //Setting up the location section
        viewModel.flavorExtensionMaps.locationStateLD.observe(viewLifecycleOwner) { state -> updateViewLocation(state) }
    }
    
    /**
     * Updates the location view in response to a change in state
     */
    private fun updateViewLocation(@LocationState state: Int): Unit = fragment.run {
        //Checking if the state is OK
        if(state == LocationState.ready) {
            //Swapping to the content view
            viewGroupLocationAction.visibility = View.GONE
            viewGroupLocationContent.visibility = View.VISIBLE
        
            //Configuring the map
            val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_attachment_location_map) as SupportMapFragment
            mapFragment.getMapAsync { googleMap: GoogleMap ->
                googleMap.isBuildingsEnabled = true
                googleMap.uiSettings.isMapToolbarEnabled = false
                googleMap.uiSettings.setAllGesturesEnabled(false)
                val currentLocation = viewModel.flavorExtensionMaps.attachmentsLocationResult!!
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation.latitude, currentLocation.longitude), 15f))
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), if(isNightMode(resources)) R.raw.map_plaindark else R.raw.map_plainlight))
            }
            viewGroupLocationContent.findViewById<View>(R.id.frame_attachment_location_click).setOnClickListener { locationPickerLauncher.launch(viewModel.flavorExtensionMaps.attachmentsLocationResult!!) }
        } else {
            //Showing the action view
            viewGroupLocationAction.visibility = View.VISIBLE
            viewGroupLocationContent.visibility = View.GONE
            val buttonText: String
            val buttonClickListener: View.OnClickListener?
            when(state) {
                LocationState.loading -> {
                    buttonText = resources.getString(R.string.message_generalloading)
                    buttonClickListener = null
                }
                LocationState.permission -> {
                    buttonText = resources.getString(R.string.imperative_permission_location)
                    buttonClickListener = View.OnClickListener { requestLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
                }
                LocationState.failed -> {
                    buttonText = resources.getString(R.string.message_loaderror_location)
                    buttonClickListener = null
                }
                LocationState.unavailable -> {
                    buttonText = resources.getString(R.string.message_notsupported)
                    buttonClickListener = null
                }
                LocationState.resolvable -> {
                    buttonText = resources.getString(R.string.imperative_enablelocationservices)
                    buttonClickListener = View.OnClickListener { resolveLocationServicesLauncher.launch(IntentSenderRequest.Builder(viewModel.flavorExtensionMaps.attachmentsLocationResolvable!!.resolution.intentSender).build()) }
                }
                else -> throw IllegalArgumentException("Invalid attachment location state $state provided")
            }
        
            //Setting the details
            labelLocationAction.text = buttonText
            if(buttonClickListener != null) {
                viewGroupLocationAction.setOnClickListener(buttonClickListener)
                viewGroupLocationAction.isClickable = true
            } else {
                viewGroupLocationAction.isClickable = false
            }
        }
    }
}