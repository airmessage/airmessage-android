package me.tagavari.airmessage.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi

@RequiresApi(33)
class ContractPickMultipleImages: ActivityResultContract<Int?, List<Uri>>() {
	override fun createIntent(context: Context, input: Int?): Intent {
		val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
		input?.let { limit ->
			intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, limit.coerceAtMost(MediaStore.getPickImagesMaxLimit()))
		}
		return intent
	}
	
	override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
		if(resultCode != Activity.RESULT_OK) {
			return emptyList()
		}
		
		val clipData = intent!!.clipData!!
		return List(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
	}
}