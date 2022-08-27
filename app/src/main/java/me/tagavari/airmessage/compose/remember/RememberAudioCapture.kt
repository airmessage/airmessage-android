package me.tagavari.airmessage.compose.remember

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.compose.util.rememberAsyncLauncherForActivityResult
import me.tagavari.airmessage.constants.FileNameConstants
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberAudioCapture(): AudioCaptureState {
	val context = LocalContext.current
	
	val requestAudioPermission = rememberAsyncLauncherForActivityResult(ActivityResultContracts.RequestPermission())
	
	var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
	DisposableEffect(mediaRecorder) {
		//Clean up media recorder when out of scope
		onDispose {
			mediaRecorder?.release()
		}
	}
	
	var isRecording by remember { mutableStateOf(false) }
	
	var recordingFile by remember { mutableStateOf<File?>(null) }
	var recordingDuration by remember { mutableStateOf(0) }
	LaunchedEffect(isRecording) {
		//Count seconds while recording
		if(isRecording) {
			while(true) {
				delay(1.seconds)
				recordingDuration++
			}
		} else {
			recordingDuration = 0
		}
	}
	
	fun stopAudioRecording(forceDiscard: Boolean = false): File? {
		//Get the recording state
		val localMediaRecorder = mediaRecorder ?: return null
		val localRecordingFile = recordingFile ?: return null
		
		//Reset the recording state
		isRecording = false
		
		val cleanStop: Boolean = try {
			localMediaRecorder.stop()
			true
		} catch(exception: RuntimeException) {
			//Media couldn't be captured, file is invalid
			false
		}
		
		if(!cleanStop || forceDiscard) {
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, localRecordingFile)
			return null
		}
		
		return localRecordingFile
	}
	
	suspend fun startAudioRecording(): Boolean {
		//Check if we have permission
		if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
			!= PackageManager.PERMISSION_GRANTED) {
			val granted = requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
			if(!granted) return false
		}
		
		//Reset or create the media recorder
		val preparedMediaRecorder = mediaRecorder?.apply { reset() }
			?: if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				MediaRecorder(context)
			} else {
				@Suppress("DEPRECATION")
				MediaRecorder()
			}
		
		//Configure the media recorder
		preparedMediaRecorder.apply {
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
			
			val targetFile = withContext(Dispatchers.IO) {
				//Find a target file
				AttachmentStorageHelper.prepareContentFile(
					context,
					AttachmentStorageHelper.dirNameDraftPrepare,
					FileNameConstants.recordingName
				)
			}
			
			//Set the media recorder file
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setOutputFile(targetFile)
			else setOutputFile(targetFile.absolutePath)
			recordingFile = targetFile
			
			//Prepare the media recorder
			@Suppress("BlockingMethodInNonBlockingContext")
			withContext(Dispatchers.IO) {
				prepare()
			}
		}
		
		//Start recording
		mediaRecorder = preparedMediaRecorder
		preparedMediaRecorder.start()
		isRecording = true
		
		return true
	}
	
	return object : AudioCaptureState(isRecording, recordingDuration) {
		override suspend fun startRecording() = startAudioRecording()
		
		override suspend fun stopRecording(forceDiscard: Boolean): File? = stopAudioRecording(forceDiscard)
	}
}

abstract class AudioCaptureState(
	val isRecording: Boolean,
	val duration: Int
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
	abstract suspend fun stopRecording(forceDiscard: Boolean = false): File?
}
