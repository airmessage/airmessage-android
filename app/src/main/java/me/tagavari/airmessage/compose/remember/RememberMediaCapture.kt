package me.tagavari.airmessage.compose.remember

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import me.tagavari.airmessage.R
import me.tagavari.airmessage.constants.FileNameConstants
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import java.io.File

enum class MessagingMediaCaptureType {
	PHOTO,
	VIDEO,
	LOW_RES_VIDEO
}

/**
 * Launches an intent to capture a picture or a video with the camera
 */
fun interface RequestCameraCallback {
	fun requestCamera(type: MessagingMediaCaptureType)
}

/**
 * Remembers logic for capturing media for a conversation
 * @param onCapture A callback invoked when a media file is taken by the user
 * @return A function that launches the camera
 */
@Composable
fun rememberMediaCapture(onCapture: (File) -> Unit): RequestCameraCallback {
	val context = LocalContext.current
	
	var activeRequestType by rememberSaveable { mutableStateOf<MessagingMediaCaptureType?>(null) }
	var activeRequestFile by rememberSaveable { mutableStateOf<File?>(null) }
	
	fun handleCaptureResult(captureResult: Boolean) {
		val file = activeRequestFile ?: return
		
		//Ignore if the user cancelled
		if(!captureResult) {
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, file)
			return
		}
		
		//Return the saved file
		onCapture(file)
	}
	
	val takePictureLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture(), ::handleCaptureResult)
	val captureVideoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CaptureVideo(), ::handleCaptureResult)
	val captureLowResVideoLauncher = rememberLauncherForActivityResult(contract = object : ActivityResultContracts.CaptureVideo() {
		override fun createIntent(context: Context, input: Uri): Intent {
			return super.createIntent(context, input)
				.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
		}
	}, ::handleCaptureResult)
	
	fun openCamera() {
		val requestType = activeRequestType ?: return
		
		//Find a free file
		val targetFile = AttachmentStorageHelper.prepareContentFile(
			context = context,
			directory = AttachmentStorageHelper.dirNameDraftPrepare,
			fileName = if(requestType == MessagingMediaCaptureType.PHOTO) FileNameConstants.pictureName
			else FileNameConstants.videoName
		)
		val targetURI = FileProvider.getUriForFile(
			context,
			AttachmentStorageHelper.getFileAuthority(context),
			targetFile
		)
		activeRequestFile = targetFile
		
		//Take the picture
		try {
			when(requestType) {
				MessagingMediaCaptureType.PHOTO -> takePictureLauncher.launch(targetURI)
				MessagingMediaCaptureType.VIDEO -> captureVideoLauncher.launch(targetURI)
				MessagingMediaCaptureType.LOW_RES_VIDEO -> captureLowResVideoLauncher.launch(targetURI)
			}
		} catch(exception: ActivityNotFoundException) {
			exception.printStackTrace()
			
			//Tell the user via a toast
			Toast.makeText(context, R.string.message_intenterror_camera, Toast.LENGTH_SHORT).show()
			
			//Clean up the unused file
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, targetFile)
		}
	}
	
	val requestPermissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { permissionGranted ->
		//Continue the flow if the user granted access
		if(permissionGranted) {
			openCamera()
		}
	}
	
	return RequestCameraCallback { type ->
		//Record the request type
		activeRequestType = type
		
		//Ask for permission if required
		if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			requestPermissionLauncher.launch(Manifest.permission.CAMERA)
		} else {
			//Proceed to open the camera
			openCamera()
		}
	}
}