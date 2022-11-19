package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.IntSize

private enum class SlotsEnum { Main, Dependent }

/**
 * A layout that puts dependentContent in a box that
 * copies the size of mainContent
 */
@Composable
fun FillLayout(
	modifier: Modifier = Modifier,
	mainContent: @Composable () -> Unit,
	dependentContent: @Composable () -> Unit
) {
	SubcomposeLayout(modifier = modifier) { constraints ->
		val mainPlaceables = subcompose(SlotsEnum.Main, mainContent).map {
			it.measure(constraints)
		}
		val maxSize = mainPlaceables.fold(IntSize.Zero) { currentMax, placeable ->
			IntSize(
				width = maxOf(currentMax.width, placeable.width),
				height = maxOf(currentMax.height, placeable.height)
			)
		}
		
		layout(maxSize.width, maxSize.height) {
			mainPlaceables.forEach { it.placeRelative(0, 0) }
			subcompose(SlotsEnum.Dependent) {
				Box(modifier = Modifier.size(width = maxSize.width.toDp(), height = maxSize.height.toDp())) {
					dependentContent()
				}
			}.forEach {
				it.measure(constraints).placeRelative(0, 0)
			}
		}
	}
}
