package me.tagavari.airmessage.flavor

import android.content.Context
import com.google.android.gms.maps.MapsInitializer

object MapsBridge {
	@JvmStatic
	fun initialize(context: Context) {
		//Initializing Google Maps
		MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST, null)
	}
}