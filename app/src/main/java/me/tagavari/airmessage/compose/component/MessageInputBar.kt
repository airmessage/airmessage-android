package me.tagavari.airmessage.compose.component

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.compose.remember.deriveAmplitudeList
import me.tagavari.airmessage.compose.remember.rememberAudioCapture
import me.tagavari.airmessage.compose.remember.rememberAudioPlayback
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.messaging.QueuedFile
import java.io.File

private data class RecordingData(
	val file: File,
	val duration: Int
)

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
	
	CompositionLocalProvider(
		LocalContentColor provides MaterialTheme.colorScheme.surface,
		LocalAbsoluteTonalElevation provides surfaceElevation
	) {
		Box(
			modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(surfaceElevation))
		) {
			Box(
				modifier = modifier.padding(8.dp)
			) {
				var recordingData by remember { mutableStateOf<RecordingData?>(null) }
				val audioCapture = rememberAudioCapture()
				
				if(audioCapture.isRecording.value || recordingData != null) {
					val playbackManager = rememberAudioPlayback()
					var playbackState by remember { mutableStateOf<AudioPlaybackState>(AudioPlaybackState.Stopped) }
					val scope = rememberCoroutineScope()
					
					MessageInputBarAudio(
						duration = audioCapture.duration.value,
						isRecording = audioCapture.isRecording.value,
						onStopRecording = {
							if(audioCapture.isRecording.value) {
								val recordingDuration = audioCapture.duration.value
								val recordingFile = audioCapture.stopRecording()
								
								if(recordingFile != null) {
									recordingData = RecordingData(
										file = recordingFile,
										duration = recordingDuration
									)
								}
							}
						},
						onSend = {
							//Stop recording and get the file, or grab a pending file
							val file = if(audioCapture.isRecording.value) {
								audioCapture.stopRecording(true)
							} else recordingData?.file
							if(file == null) return@MessageInputBarAudio
							
							//Send the file
							
						},
						onDiscard = {
							//Stop recording if we're recording
							if(audioCapture.isRecording.value) {
								audioCapture.stopRecording(true)
							}
							
							//Delete the recording file
							recordingData?.let { recordingData ->
								AttachmentStorageHelper.deleteContentFile(
									AttachmentStorageHelper.dirNameDraftPrepare,
									recordingData.file
								)
							}
							recordingData = null
						},
						onTogglePlay = {
							val file = recordingData?.file ?: return@MessageInputBarAudio
							
							scope.launch {
								if(playbackState is AudioPlaybackState.Stopped) {
									playbackManager.play(Uri.fromFile(file)).collect { playbackState = it }
								} else {
									playbackManager.pause()
								}
							}
						},
						playbackState = playbackState,
						amplitudeList = deriveAmplitudeList(
							mediaRecorder = audioCapture.mediaRecorder,
							enable = audioCapture.isRecording.value
						)
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
