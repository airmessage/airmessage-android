package me.tagavari.airmessage.common.compose.remember

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import me.tagavari.airmessage.contract.ContractPickMultipleImages

/**
 * Launches an intent to capture a picture or a video with the camera
 */
fun interface RequestMediaCallback {
	fun requestMedia(limit: Int?)
}

/**
 * Remembers logic for prompting the user to select media files
 * @param onSelect A callback invoked when the user selects media files
 * @return A function that launches the media picker
 */
@Composable
fun rememberMediaRequest(onSelect: (List<Uri>) -> Unit): RequestMediaCallback {
	return if(ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable()) {
		val launcher = rememberLauncherForActivityResult(ContractPickMultipleImages(), onSelect)
		RequestMediaCallback { limit ->
			launcher.launch(limit)
		}
	} else {
		val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents(), onSelect)
		RequestMediaCallback {
			launcher.launch("image/* video/*")
		}
	}
}
