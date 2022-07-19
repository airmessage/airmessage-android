package me.tagavari.airmessage.flavor

import android.content.Context
import android.view.View
import io.reactivex.rxjava3.core.Completable
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentLocation
import me.tagavari.airmessage.util.LatLngInfo

class VHMessageComponentLocationMap(private val component: VHMessageComponentLocation) {
	val mapLoadCompletable: Completable = Completable.complete()
	fun setMapLocation(context: Context, location: LatLngInfo?) {
		//Just hide the map view
		component.mapContainer.visibility = View.GONE
	}
}