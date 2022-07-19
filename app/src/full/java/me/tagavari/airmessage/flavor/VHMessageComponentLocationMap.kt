package me.tagavari.airmessage.flavor

import android.content.Context
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.subjects.SingleSubject
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.ThemeHelper
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentLocation
import me.tagavari.airmessage.util.LatLngInfo

class VHMessageComponentLocationMap(private val component: VHMessageComponentLocation) {
	private val googleMap = SingleSubject.create<GoogleMap>()
	val mapLoadCompletable: Completable
		get() = googleMap.ignoreElement()
	
	init {
		val mapView = component.mapView as MapView
		
		//Initializing the map
		mapView.onCreate(null)
		mapView.getMapAsync(googleMap::onSuccess)
	}
	
	fun setMapLocation(context: Context, location: LatLngInfo?): Unit = component.run {
		//If the location is null, hide the view
		if(location == null) {
			mapContainer.visibility = View.GONE
			return
		}
		
		googleMap.doOnSuccess { googleMap ->
			mapView as MapView
			
			//Showing the map view and setting it as non-clickable (so that the card view parent will handle clicks instead)
			mapContainer.visibility = View.VISIBLE
			mapView.isClickable = false
			
			//Setting the map location
			val targetLocation = LatLng(location.latitude, location.longitude)
			googleMap.clear()
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 15f))
			val markerOptions = MarkerOptions().position(targetLocation)
			googleMap.addMarker(markerOptions)
			
			//Setting the map theme
			googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, if(ThemeHelper.isNightMode(context.resources)) R.raw.map_dark else R.raw.map_light))
			googleMap.uiSettings.isMapToolbarEnabled = false
		}.subscribe()
	}
}