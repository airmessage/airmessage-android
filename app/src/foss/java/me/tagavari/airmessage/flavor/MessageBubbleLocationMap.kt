package me.tagavari.airmessage.flavor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import me.tagavari.airmessage.compose.component.MessageBubbleLocationContentGeocoder
import me.tagavari.airmessage.util.LatLngInfo

/**
 * A component that displays a map view for a given position.
 * In FOSS, this simply displays the address.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun MessageBubbleLocationMap(
	modifier: Modifier = Modifier,
	coords: LatLngInfo,
	highlight: Color? = null
) {
	MessageBubbleLocationContentGeocoder(
		modifier = modifier,
		coords = coords
	)
}
