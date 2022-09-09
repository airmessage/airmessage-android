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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.BuildConfig
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

private const val TAG = "rememberAudioCapture"

@Composable
fun rememberAudioCapture(): AudioCaptureState {
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
	val recordingStopFlow = remember { MutableSharedFlow<Boolean>(
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
	
	fun stopAudioRecording(forceDiscard: Boolean = false): Boolean {
		if(BuildConfig.DEBUG && !isRecording.value) {
			Log.w(TAG, "Tried to stop audio recording while no recording is present!")
		}
		
		//Reset the recording state
		isRecording.value = false
		
		//Stop recording
		val cleanStop: Boolean = try {
			mediaRecorder.stop()
			true
		} catch(exception: RuntimeException) {
			//Media couldn't be captured, file is invalid
			exception.printStackTrace()
			false
		}
		
		//Return the result
		val recordingOK = cleanStop && !forceDiscard
		recordingStopFlow.tryEmit(recordingOK)
		return recordingOK
	}
	
	suspend fun startAudioRecording(targetFile: File): Boolean {
		if(BuildConfig.DEBUG && isRecording.value) {
			Log.w(TAG, "Tried to start audio recording while already recording!")
		}
		
		//Check if we have permission
		if(!audioPermissionGranted) {
			requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
			return false
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
				return false
			}
		}
		
		//Start recording
		mediaRecorder.start()
		isRecording.value = true
		
		//Wait until we finish recording
		return try {
			recordingStopFlow.first()
		} catch(exception: InterruptedException) {
			Log.w(TAG, "Interrupted while recording audio!")
			throw exception
		}
	}
	
	return object : AudioCaptureState(mediaRecorder, isRecording, recordingDuration) {
		override suspend fun startRecording(file: File) = startAudioRecording(file)
		
		override fun stopRecording(forceDiscard: Boolean) = stopAudioRecording(forceDiscard)
	}
}

abstract class AudioCaptureState(
	val mediaRecorder: MediaRecorder,
	val isRecording: State<Boolean>,
	val duration: State<Int>
) {
	/**
	 * Starts audio recording, and suspends until recording is complete
	 * @param file The file to write to
	 * @return Whether the recording succeeded or not
	 */
	abstract suspend fun startRecording(file: File): Boolean
	
	/**
	 * Stops audio recording. If recording was successful, a reference to the
	 * output file is returned.
	 * @param forceDiscard Whether to deliberately fail this recording
	 * @return Whether the recording succeeded or not
	 */
	abstract fun stopRecording(forceDiscard: Boolean = false): Boolean
}
