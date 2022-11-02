package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ContentAlertDialog(
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	icon: (@Composable () -> Unit)? = null,
	title: @Composable (() -> Unit)? = null,
	shape: Shape = AlertDialogDefaults.shape,
	containerColor: Color = AlertDialogDefaults.containerColor,
	iconContentColor: Color = AlertDialogDefaults.iconContentColor,
	titleContentColor: Color = AlertDialogDefaults.titleContentColor,
	bodyContentColor: Color = AlertDialogDefaults.textContentColor,
	tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
	properties: DialogProperties = DialogProperties(),
	body: @Composable () -> Unit
) {
	Dialog(
		onDismissRequest = onDismissRequest,
		properties = properties
	) {
		Surface(
			modifier = modifier,
			shape = shape,
			color = containerColor,
			tonalElevation = tonalElevation,
		) {
			Column(
				modifier = Modifier
					.sizeIn(minWidth = MinWidth, maxWidth = MaxWidth)
					.padding(DialogPadding)
			) {
				icon?.let {
					CompositionLocalProvider(LocalContentColor provides iconContentColor) {
						Box(
							Modifier
								.padding(DialogEntryPadding)
								.padding(IconPadding)
								.align(Alignment.CenterHorizontally)
						) {
							icon()
						}
					}
				}
				title?.let {
					CompositionLocalProvider(LocalContentColor provides titleContentColor) {
						val textStyle = MaterialTheme.typography.headlineSmall
						ProvideTextStyle(textStyle) {
							Box(
								// Align the title to the center when an icon is present.
								Modifier
									.padding(DialogEntryPadding)
									.padding(TitlePadding)
									.align(
										if(icon == null) {
											Alignment.Start
										} else {
											Alignment.CenterHorizontally
										}
									)
							) {
								title()
							}
						}
					}
				}
				
				CompositionLocalProvider(LocalContentColor provides bodyContentColor) {
					val textStyle =
						MaterialTheme.typography.bodyMedium
					ProvideTextStyle(textStyle) {
						Box(
							Modifier
								.weight(weight = 1f, fill = false)
								.align(Alignment.Start)
						) {
							body()
						}
					}
				}
			}
		}
	}
}

// Paddings for each of the dialog's parts.
private val DialogPadding = PaddingValues(top = 24.dp, bottom = 12.dp)
private val DialogEntryPadding = PaddingValues(horizontal = 24.dp)
private val IconPadding = PaddingValues(bottom = 16.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)

private val MinWidth = 280.dp
private val MaxWidth = 560.dp
