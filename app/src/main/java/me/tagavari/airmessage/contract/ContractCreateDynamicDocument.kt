package me.tagavari.airmessage.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * A [ActivityResultContracts.CreateDocument] that accepts a content
 * type as part of its input
 */
class ContractCreateDynamicDocument : ActivityResultContract<ContractCreateDynamicDocument.Params, Uri?>() {
	override fun createIntent(context: Context, input: Params): Intent {
		return Intent(Intent.ACTION_CREATE_DOCUMENT)
			.setType(input.type)
			.putExtra(Intent.EXTRA_TITLE, input.name)
	}
	
	override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
		return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
	}
	
	data class Params(
		val name: String?,
		val type: String?
	)
}

