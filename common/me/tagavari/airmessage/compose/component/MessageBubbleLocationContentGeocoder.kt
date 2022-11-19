package me.tagavari.airmessage.compose.component

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.util.LatLngInfo
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A component that displays a map view for a given position.
 * This implementation displays the address of the location.
 */
@Composable
fun MessageBubbleLocationContentGeocoder(
	modifier: Modifier = Modifier,
	coords: LatLngInfo
) {
	val context = LocalContext.current
	
	//Use Android's built-in geocoder to get a human-readable address
	val displayLocation by produceState(
		initialValue = LanguageHelper.coordinatesToString(coords), coords,
	) {
		//Ignore if we have no geocoder
		if(!Geocoder.isPresent()) return@produceState
		
		//Ignore if the coordinates are invalid
		if(coords.latitude < -90 || coords.latitude > 90
			|| coords.longitude < -180 || coords.longitude > 180) return@produceState
		
		//Call the geocoder
		val geocoder = Geocoder(context)
		val geocoderResult: List<Address> = if(Build.VERSION.SDK_INT >= 33) {
			suspendCoroutine { continuation ->
				geocoder.getFromLocation(coords.latitude, coords.longitude, 1) { result ->
					continuation.resume(result)
				}
			}
		} else {
			@Suppress("DEPRECATION")
			try {
				@Suppress("BlockingMethodInNonBlockingContext")
				withContext(Dispatchers.IO) {
					geocoder.getFromLocation(coords.latitude, coords.longitude, 1)
				} ?: listOf()
			} catch(exception: IOException) {
				exception.printStackTrace()
				return@produceState
			}
		}
		
		//Get the first result
		val address = geocoderResult.firstOrNull() ?: return@produceState
		val addressLine = address.getAddressLine(0) ?: return@produceState
		
		//Display the address line
		value = addressLine
	}
	
	Column(
		modifier = modifier.padding(32.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			text = displayLocation,
			textAlign = TextAlign.Center
		)
		
		Spacer(modifier = Modifier.height(16.dp))
		
		Icon(
			imageVector = Icons.Outlined.Place,
			contentDescription = null
		)
	}
}
