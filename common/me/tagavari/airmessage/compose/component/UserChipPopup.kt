package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.deriveUserInfo
import me.tagavari.airmessage.compose.state.SelectedRecipient
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.compose.util.wrapImmutableHolder

@Composable
fun UserChipPopup(
	onDismissRequest: () -> Unit,
	recipient: SelectedRecipient,
	onRemove: () -> Unit
) {
	Popup(
		onDismissRequest = onDismissRequest,
		popupPositionProvider = PositionProvider
	) {
		Surface(
			shape = MaterialTheme.shapes.medium,
			color = MaterialTheme.colorScheme.surface,
			tonalElevation = AlertDialogDefaults.TonalElevation,
			shadowElevation = AlertDialogDefaults.TonalElevation
		) {
			UserChipLayout(
				recipient = recipient,
				onRemove = onRemove
			)
		}
	}
}

private object PositionProvider : PopupPositionProvider {
	override fun calculatePosition(
		anchorBounds: IntRect,
		windowSize: IntSize,
		layoutDirection: LayoutDirection,
		popupContentSize: IntSize
	): IntOffset {
		//Attach to the bottom of the parent view
		return IntOffset(anchorBounds.left, anchorBounds.bottom)
	}
}

@Composable
private fun UserChipLayout(
	recipient: SelectedRecipient,
	onRemove: () -> Unit
) {
	val userInfo by deriveUserInfo(recipient.address)
	
	Row(
		modifier = Modifier
			.height(56.dp)
			.padding(start = 16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		MemberImage(
			modifier = Modifier.size(40.dp),
			color = MaterialTheme.colorScheme.primary,
			thumbnailURI = userInfo?.thumbnailURI.wrapImmutableHolder()
		)
		
		Spacer(modifier = Modifier.width(16.dp))
		
		Column {
			recipient.name?.let { name ->
				Text(
					text = name,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					style = MaterialTheme.typography.bodyLarge
				)
			}
			
			Text(
				text = recipient.address,
				style = MaterialTheme.typography.bodyMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		
		Spacer(modifier = Modifier.width(16.dp))
		
		IconButton(onClick = onRemove) {
			Icon(
				imageVector = Icons.Filled.Cancel,
				contentDescription = stringResource(id = R.string.action_remove),
				tint = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun PreviewUserChipPopup() {
	AirMessageAndroidTheme {
		UserChipLayout(
			recipient = SelectedRecipient("cool@guy.com", "Cool Guy"),
			onRemove = {}
		)
	}
}