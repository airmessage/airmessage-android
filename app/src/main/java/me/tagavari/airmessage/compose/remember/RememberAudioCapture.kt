package me.tagavari.airmessage.compose.remember

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import me.tagavari.airmessage.BuildConfig
import me.tagavari.airmessage.compose.util.rememberAsyncLauncherForActivityResult
import me.tagavari.airmessage.constants.FileNameConstants
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

private const val TAG = "rememberAudioCapture"

@Composable
fun rememberAudioCapture(): AudioCaptureState {
	val context = LocalContext.current
	
	val requestAudioPermission = rememberAsyncLauncherForActivityResult(ActivityResultContracts.RequestPermission())
	
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
	
	val recordingFile = remember { mutableStateOf<File?>(null) }
	val recordingDuration = remember { mutableStateOf(0) }
	LaunchedEffect(isRecording.value) {
		//Count seconds while recording
		if(isRecording.value) {
			while(true) {
				delay(1.seconds)
				recordingDuration.value++
			}
		} else {
			recordingDuration.value = 0
		}
	}
	
	fun stopAudioRecording(forceDiscard: Boolean = false): File? {
		if(BuildConfig.DEBUG && !isRecording.value) {
			Log.w(TAG, "Tried to stop audio recording while no recording is present!")
		}
		
		//Get the recording state
		val localRecordingFile = recordingFile.value ?: return null
		
		//Reset the recording state
		isRecording.value = false
		
		val cleanStop: Boolean = try {
			mediaRecorder.stop()
			true
		} catch(exception: RuntimeException) {
			//Media couldn't be captured, file is invalid
			exception.printStackTrace()
			false
		}
		
		if(!cleanStop || forceDiscard) {
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, localRecordingFile)
			return null
		}
		
		return localRecordingFile
	}
	
	suspend fun startAudioRecording(): Boolean {
		if(BuildConfig.DEBUG && isRecording.value) {
			Log.w(TAG, "Tried to start audio recording while already recording!")
		}
		
		//Check if we have permission
		if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
			!= PackageManager.PERMISSION_GRANTED) {
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
			
			//Find a target file
			val targetFile = withContext(Dispatchers.IO) {
				AttachmentStorageHelper.prepareContentFile(
					context,
					AttachmentStorageHelper.dirNameDraftPrepare,
					FileNameConstants.recordingName
				)
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
		
		return true
	}
	
	return object : AudioCaptureState(mediaRecorder, isRecording, recordingDuration) {
		override suspend fun startRecording() = startAudioRecording()
		
		override fun stopRecording(forceDiscard: Boolean): File? = stopAudioRecording(forceDiscard)
	}
}

abstract class AudioCaptureState(
	val mediaRecorder: MediaRecorder,
	val isRecording: State<Boolean>,
	val duration: State<Int>
) {
	/**
	 * Starts audio recording
	 */
	abstract suspend fun startRecording(): Boolean
	
	/**
	 * Stops audio recording. If recording was successful, a reference to the
	 * output file is returned.
	 * @param forceDiscard Whether to deliberately fail this recording
	 * @return A reference to the output file, or null if the recording failed
	 */
	abstract fun stopRecording(forceDiscard: Boolean = false): File?
}
