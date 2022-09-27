package me.tagavari.airmessage.compose.component

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.mms.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.compose.provider.LocalAudioPlayback
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.compose.remember.deriveAmplitudeList
import me.tagavari.airmessage.compose.remember.rememberAudioCapture
import me.tagavari.airmessage.constants.FileNameConstants
import me.tagavari.airmessage.container.LocalFile
import me.tagavari.airmessage.container.ReadableBlob
import me.tagavari.airmessage.container.ReadableBlobLocalFile
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.messaging.QueuedFile

private data class RecordingData(
	val file: LocalFile,
	val duration: Int
)

@Composable
fun MessageInputBar(
	modifier: Modifier = Modifier,
	messageText: String,
	onMessageTextChange: (String) -> Unit,
	attachments: List<QueuedFile>,
	onRemoveAttachment: (QueuedFile) -> Unit,
	onAddAttachments: (List<ReadableBlob>) -> Unit,
	attachmentsScrollState: ScrollState = rememberScrollState(),
	onSend: () -> Unit,
	onSendFile: (ReadableBlob) -> Unit,
	onTakePhoto: () -> Unit,
	onOpenContentPicker: () -> Unit,
	collapseButtons: Boolean = false,
	onChangeCollapseButtons: (Boolean) -> Unit,
	@ServiceHandler serviceHandler: Int?,
	@ServiceType serviceType: String?,
	floating: Boolean = false,
	rounded: Boolean = false
) {
	val surfaceElevation by animateDpAsState(
		if(floating) 2.dp else 0.dp
	)
	
	CompositionLocalProvider(
		LocalContentColor provides MaterialTheme.colorScheme.surface,
		LocalAbsoluteTonalElevation provides surfaceElevation
	) {
		Box(
			modifier = Modifier
				.background(
					color = MaterialTheme.colorScheme.surfaceColorAtElevation(surfaceElevation),
					shape = if(rounded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) else RectangleShape
				)
				.padding(8.dp)
				.then(modifier)
		) {
			var recordingData by remember { mutableStateOf<RecordingData?>(null) }
			val audioCapture = rememberAudioCapture()
			
			val showRecording = audioCapture.isRecording.value || recordingData != null
			
			val playbackManager = LocalAudioPlayback.current
			val playbackState by playbackManager.stateForKey(key = recordingData?.file)
			
			val context = LocalContext.current
			val scope = rememberCoroutineScope()
			
			fun discardRecording() {
				//Stop recording if we're recording
				if(audioCapture.isRecording.value) {
					audioCapture.stopRecording(true)
				}
				
				//Delete the recording file
				recordingData?.file?.also { file ->
					playbackManager.stop(key = file)
					file.deleteFile()
				}
				recordingData = null
			}
			DisposableEffect(Unit) {
				//Clean up recording files when we go out of scope
				onDispose(::discardRecording)
			}
			
			if(!showRecording) {
				MessageInputBarText(
					messageText = messageText,
					onMessageTextChange = onMessageTextChange,
					attachments = attachments,
					onRemoveAttachment = onRemoveAttachment,
					onInputContent = onAddAttachments,
					attachmentsScrollState = attachmentsScrollState,
					collapseButtons = collapseButtons,
					onChangeCollapseButtons = onChangeCollapseButtons,
					onTakePhoto = onTakePhoto,
					onOpenContentPicker = onOpenContentPicker,
					onStartAudioRecording = {
						scope.launch {
							//Find a target file
							val targetFile = withContext(Dispatchers.IO) {
								AttachmentStorageHelper.prepareContentFile(
									context,
									AttachmentStorageHelper.dirNameDraftPrepare,
									FileNameConstants.recordingName
								)
							}
							
							val recordingOK = audioCapture.startRecording(targetFile)
							
							//If the recording failed, clean up the file
							if(!recordingOK) {
								AttachmentStorageHelper.deleteContentFile(
									AttachmentStorageHelper.dirNameDraftPrepare,
									targetFile
								)
								return@launch
							}
							
							val recordingDuration = audioCapture.duration.value
							
							//Set the recording data
							recordingData = RecordingData(
								file = LocalFile(
									file = targetFile,
									fileName = targetFile.name,
									fileType = ContentType.AUDIO_AMR,
									fileSize = targetFile.length(),
									directoryID = AttachmentStorageHelper.dirNameDraftPrepare
								),
								duration = recordingDuration
							)
						}
					},
					serviceHandler = serviceHandler,
					serviceType = serviceType,
					onSend = onSend
				)
			}
			
			MessageInputBarAudio(
				duration = audioCapture.duration.value,
				isRecording = audioCapture.isRecording.value,
				onStopRecording = {
					if(audioCapture.isRecording.value) {
						audioCapture.stopRecording()
					}
				},
				onSend = {
					//Send the file
					recordingData?.file?.let { file ->
						playbackManager.stop(key = file)
						onSendFile(ReadableBlobLocalFile(file, deleteOnInvalidate = true))
					}
					recordingData = null
				},
				onDiscard = ::discardRecording,
				onTogglePlay = {
					val file = recordingData?.file ?: return@MessageInputBarAudio
					val state: AudioPlaybackState = playbackState
					
					scope.launch {
						if(state is AudioPlaybackState.Playing) {
							if(state.playing) {
								playbackManager.pause()
							} else {
								playbackManager.resume()
							}
						} else {
							playbackManager.play(key = file, uri = Uri.fromFile(file.file))
						}
					}
				},
				playbackState = playbackState,
				amplitudeList = if(LocalInspectionMode.current) {
					listOf()
				} else {
					deriveAmplitudeList(
						mediaRecorder = audioCapture.mediaRecorder,
						enable = audioCapture.isRecording.value
					)
				},
				visible = showRecording
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
			onAddAttachments = {},
			onSend = {},
			onSendFile = {},
			onTakePhoto = {},
			onOpenContentPicker = {},
			collapseButtons = false,
			onChangeCollapseButtons = {},
			serviceHandler = ServiceHandler.appleBridge,
			serviceType = ServiceType.appleMessage,
		)
	}
}
