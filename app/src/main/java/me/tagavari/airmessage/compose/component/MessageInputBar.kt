package me.tagavari.airmessage.compose.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.messaging.QueuedFile

@Composable
fun MessageInputBar(
	modifier: Modifier = Modifier,
	messageText: String,
	onMessageTextChange: (String) -> Unit,
	attachments: List<QueuedFile>,
	onRemoveAttachment: (QueuedFile) -> Unit,
	onSend: () -> Unit,
	onTakePhoto: () -> Unit,
	onOpenContentPicker: () -> Unit,
	collapseButtons: Boolean = false,
	onChangeCollapseButtons: (Boolean) -> Unit,
	@ServiceHandler serviceHandler: Int?,
	@ServiceType serviceType: String?,
	floating: Boolean = false,
) {
	val surfaceElevation by animateDpAsState(
		if(floating) 2.dp else 0.dp
	)
	
	Surface(
		tonalElevation = surfaceElevation
	) {
		Box(modifier = modifier.padding(8.dp)) {
			MessageInputBarText(
				messageText = messageText,
				onMessageTextChange = onMessageTextChange,
				attachments = attachments,
				onRemoveAttachment = onRemoveAttachment,
				collapseButtons = collapseButtons,
				onChangeCollapseButtons = onChangeCollapseButtons,
				onTakePhoto = onTakePhoto,
				onOpenContentPicker = onOpenContentPicker,
				serviceHandler = serviceHandler,
				serviceType = serviceType,
				onSend = onSend
			)
		}
	}
}

@Preview
@Composable
private fun PreviewMessageInputBar() {
	Surface {
		MessageInputBar(
			messageText = "",
			onMessageTextChange = {},
			attachments = listOf(),
			onRemoveAttachment = {},
			onSend = {},
			onTakePhoto = {},
			onOpenContentPicker = {},
			collapseButtons = false,
			onChangeCollapseButtons = {},
			serviceHandler = ServiceHandler.appleBridge,
			serviceType = ServiceType.appleMessage,
		)
	}
}
