package me.tagavari.airmessage.compose.remember

import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import me.tagavari.airmessage.compose.util.rememberAsyncLauncherForActivityResult
import me.tagavari.airmessage.contract.ContractPickMultipleImages

/**
 * Launches an intent to capture a picture or a video with the camera
 */
fun interface RequestMediaCallback {
	suspend fun requestMedia(limit: Int?): List<Uri>
}

@Composable
fun rememberMediaRequest(): RequestMediaCallback {
	return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		val launcher = rememberAsyncLauncherForActivityResult(contract = ContractPickMultipleImages())
		RequestMediaCallback { limit ->
			launcher.launch(limit)
		}
	} else {
		val launcher = rememberAsyncLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents())
		RequestMediaCallback {
			launcher.launch("image/* video/*")
		}
	}
}