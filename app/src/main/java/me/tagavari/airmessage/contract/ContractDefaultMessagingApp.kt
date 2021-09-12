package me.tagavari.airmessage.contract

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.activity.result.contract.ActivityResultContract

/**
 * A contract to request the app to be set as the default messaging app.
 * Takes no input, and returns a boolean, whether the user accepted the request.
 */
class ContractDefaultMessagingApp: ActivityResultContract<Void?, Boolean>() {
	override fun createIntent(context: Context, input: Void?): Intent {
		return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			context.getSystemService(RoleManager::class.java)
				.createRequestRoleIntent(RoleManager.ROLE_SMS)
		} else {
			Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
				.apply {
					putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
				}
		}
	}
	
	override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
		return resultCode == Activity.RESULT_OK
	}
}