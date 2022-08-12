package me.tagavari.airmessage.compose.remember

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.util.rememberAsyncLauncherForActivityResult
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
	suspend fun requestCamera(type: MessagingMediaCaptureType): File?
}

@Composable
fun rememberMediaCapture(): RequestCameraCallback {
	val context = LocalContext.current
	
	val requestPermissionLauncher = rememberAsyncLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission())
	val takePictureLauncher = rememberAsyncLauncherForActivityResult(contract = ActivityResultContracts.TakePicture())
	val captureVideoLauncher = rememberAsyncLauncherForActivityResult(contract = ActivityResultContracts.CaptureVideo())
	val captureLowResVideoLauncher = rememberAsyncLauncherForActivityResult(contract = object : ActivityResultContracts.CaptureVideo() {
		override fun createIntent(context: Context, input: Uri): Intent {
			return super.createIntent(context, input)
				.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
		}
	})
	
	return RequestCameraCallback { type ->
		//Ask for permission if required
		if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			val permissionGranted = requestPermissionLauncher.launch(Manifest.permission.CAMERA)
			if(!permissionGranted) {
				return@RequestCameraCallback null
			}
		}
		
		//Find a free file
		val targetFile = AttachmentStorageHelper.prepareContentFile(
			context,
			AttachmentStorageHelper.dirNameDraftPrepare,
			if(type == MessagingMediaCaptureType.PHOTO) FileNameConstants.pictureName else FileNameConstants.videoName
		)
		val targetURI = FileProvider.getUriForFile(
			context,
			AttachmentStorageHelper.getFileAuthority(context),
			targetFile
		)
		
		//Take the picture
		val captureResult = try {
			when(type) {
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
			
			return@RequestCameraCallback null
		}
		
		//Ignore if the user cancelled
		if(!captureResult) {
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, targetFile)
			return@RequestCameraCallback null
		}
		
		//Return the saved file
		return@RequestCameraCallback targetFile
	}
}