package me.tagavari.airmessage.compose.remember

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.BuildConfig
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

private const val TAG = "rememberAudioCapture"

@Composable
fun <Payload> rememberAudioCapture(): AudioCaptureState<Payload> {
	if(LocalInspectionMode.current) {
		return NoOpAudioCaptureState()
	}
	
	val context = LocalContext.current
	
	var audioPermissionGranted by remember {
		mutableStateOf(
			ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
					== PackageManager.PERMISSION_GRANTED
		)
	}
	val requestAudioPermission = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { audioPermissionGranted = it }
	
	val mediaRecorder = remember {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			MediaRecorder(context)
		} else {
			@Suppress("DEPRECATION")
			MediaRecorder()
		}
	}
	DisposableEffect(mediaRecorder) {
		//Clean up media recorder when out of scope
		onDispose {
			mediaRecorder.release()
		}
	}
	
	val isRecording = remember { mutableStateOf(false) }
	val recordingStopFlow = remember { MutableSharedFlow<AudioCaptureUpdate<Payload>>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	) }
	
	val recordingFile = remember { mutableStateOf<File?>(null) }
	val recordingDuration = remember { mutableStateOf(0) }
	LaunchedEffect(isRecording.value) {
		//Count seconds while recording
		if(isRecording.value) {
			recordingDuration.value = 0
			
			while(true) {
				delay(1.seconds)
				recordingDuration.value++
			}
		}
	}
	
	//Stops audio recording, and returns whether the recording was stopped cleanly
	fun stopAudioRecording() {
		if(BuildConfig.DEBUG && !isRecording.value) {
			Log.w(TAG, "Tried to stop audio recording while no recording is present!")
		}
		
		//Reset the recording state
		isRecording.value = false
		
		//Stop recording
		mediaRecorder.stop()
	}
	
	fun cancelAudioRecording() {
		//Stop the recording
		try {
			stopAudioRecording()
		} catch(exception: RuntimeException) {
			exception.printStackTrace()
		}
		
		//Return a failed result
		recordingStopFlow.tryEmit(AudioCaptureUpdate.Error(RecordingCancellationException("Audio recording cancelled")))
	}
	
	fun completeAudioRecording(payload: Payload): Boolean {
		//Stop the recording
		try {
			stopAudioRecording()
		} catch(exception: RuntimeException) {
			//Emit the exception
			recordingStopFlow.tryEmit(AudioCaptureUpdate.Error(exception))
			return false
		}
		
		//Emit a successful result
		recordingStopFlow.tryEmit(AudioCaptureUpdate.Success(payload))
		return true
	}
	
	suspend fun startAudioRecording(targetFile: File): Flow<AudioCaptureUpdate<Payload>> = flow {
		if(BuildConfig.DEBUG && isRecording.value) {
			Log.w(TAG, "Tried to start audio recording while already recording!")
			Thread.dumpStack()
		}
		
		//Check if we have permission
		if(!audioPermissionGranted) {
			requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
			emit(AudioCaptureUpdate.Error(RecordingCancellationException("REQUEST_AUDIO permission not granted")))
			return@flow
		}
		
		//Configure the media recorder
		mediaRecorder.apply {
			reset()
			setAudioSource(MediaRecorder.AudioSource.DEFAULT)
			setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
			setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
			setMaxDuration(10 * 60 * 1000) //10 minutes
			
			setOnInfoListener { _, what, _ ->
				if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
					|| what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					stopAudioRecording()
				}
			}
			setOnErrorListener { _, _, _ ->
				stopAudioRecording()
			}
			
			//Set the media recorder file
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setOutputFile(targetFile)
			else setOutputFile(targetFile.absolutePath)
			recordingFile.value = targetFile
			
			//Prepare the media recorder
			try {
				@Suppress("BlockingMethodInNonBlockingContext")
				withContext(Dispatchers.IO) {
					prepare()
				}
			} catch(exception: IOException) {
				exception.printStackTrace()
				emit(AudioCaptureUpdate.Error(exception))
				return@flow
			}
		}
		
		//Start recording
		mediaRecorder.start()
		isRecording.value = true
		emit(AudioCaptureUpdate.Started())
		
		//Wait until we finish recording
		emit(recordingStopFlow.first())
	}
	
	return AudioCaptureStateImpl(
		mediaRecorder = mediaRecorder,
		isRecording = isRecording,
		duration = recordingDuration,
		startRecordingCallback = ::startAudioRecording,
		cancelRecordingCallback = ::cancelAudioRecording,
		completeRecordingCallback = ::completeAudioRecording
	)
}

abstract class AudioCaptureState<Payload> {
	abstract val mediaRecorder: MediaRecorder
	abstract val isRecording: State<Boolean>
	abstract val duration: State<Int>
	
	/**
	 * Starts audio recording, and suspends until recording is complete
	 * @param file The file to write to
	 * @return Whether the recording succeeded or not
	 */
	abstract suspend fun startRecording(file: File): Flow<AudioCaptureUpdate<Payload>>
	
	/**
	 * Cancels recording with an unsuccessful result.
	 */
	abstract fun cancelRecording()
	
	/**
	 * Finishes audio recording. If recording was successful, a reference to the
	 * output file is returned.
	 * @return Whether the recording succeeded or not
	 */
	abstract fun completeRecording(payload: Payload): Boolean
}

sealed class AudioCaptureUpdate<Payload> {
	class Started<Payload> : AudioCaptureUpdate<Payload>()
	class Success<Payload>(val payload: Payload) : AudioCaptureUpdate<Payload>()
	class Error<Payload>(val error: Throwable) : AudioCaptureUpdate<Payload>()
}

private class AudioCaptureStateImpl<Payload>(
	override val mediaRecorder: MediaRecorder,
	override val isRecording: State<Boolean>,
	override val duration: State<Int>,
	private val startRecordingCallback: suspend (file: File) -> Flow<AudioCaptureUpdate<Payload>>,
	private val cancelRecordingCallback: () -> Unit,
	private val completeRecordingCallback: (payload: Payload) -> Boolean
) : AudioCaptureState<Payload>() {
	override suspend fun startRecording(file: File) = startRecordingCallback(file)
	override fun cancelRecording() = cancelRecordingCallback()
	override fun completeRecording(payload: Payload) = completeRecordingCallback(payload)
}

private class NoOpAudioCaptureState<Payload> : AudioCaptureState<Payload>() {
	override val mediaRecorder: MediaRecorder
		get() = throw IllegalStateException()
	override val isRecording: State<Boolean> = mutableStateOf(false)
	override val duration: State<Int> = mutableStateOf(0)
	
	private val payloadFlow = MutableSharedFlow<Payload>()
	
	override suspend fun startRecording(file: File) = flowOf(AudioCaptureUpdate.Success(payloadFlow.first()))
	override fun cancelRecording() = Unit
	override fun completeRecording(payload: Payload) = true
}

class RecordingCancellationException: Exception {
	constructor(message: String?) : super(message)
	constructor(message: String?, cause: Throwable?) : super(message, cause)
	constructor(cause: Throwable?) : super(cause)
}