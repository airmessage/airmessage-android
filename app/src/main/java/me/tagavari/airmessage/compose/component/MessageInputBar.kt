package me.tagavari.airmessage.compose.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.tagavari.airmessage.compose.remember.rememberAudioCapture
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
	
	val audioCapture = rememberAudioCapture()
	
	CompositionLocalProvider(
		LocalContentColor provides MaterialTheme.colorScheme.surface,
		LocalAbsoluteTonalElevation provides surfaceElevation
	) {
		Box(
			modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(surfaceElevation))
		) {
			Box(
				modifier = modifier
					.padding(8.dp)
					.pointerInput(Unit) {
						detectTapGestures(
							onPress = {
								awaitRelease()
								if(audioCapture.isRecording.value) {
									audioCapture.stopRecording()
								}
							}
						)
					}
			) {
				if(audioCapture.isRecording.value) {
					MessageInputBarAudio(
						audioCapture.duration.value
					)
				} else {
					val scope = rememberCoroutineScope()
					MessageInputBarText(
						messageText = messageText,
						onMessageTextChange = onMessageTextChange,
						attachments = attachments,
						onRemoveAttachment = onRemoveAttachment,
						collapseButtons = collapseButtons,
						onChangeCollapseButtons = onChangeCollapseButtons,
						onTakePhoto = onTakePhoto,
						onOpenContentPicker = onOpenContentPicker,
						onStartAudioRecording = {
							scope.launch {
								audioCapture.startRecording()
							}
						},
						serviceHandler = serviceHandler,
						serviceType = serviceType,
						onSend = onSend
					)
				}
			}
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
