package me.tagavari.airmessage.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract

/**
 * A contract to launch the system ringtone selector
 * Takes a currently selected ringtone URI (or null for silent), and returns a result
 */
class ContractNotificationRingtoneSelector: ActivityResultContract<Uri?, ContractNotificationRingtoneSelector.Result>() {
	override fun createIntent(context: Context, input: Uri?): Intent {
		return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
			putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
			putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
			putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
			putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
			putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, input)
		}
	}
	
	override fun parseResult(resultCode: Int, intent: Intent?): Result {
		return Result(resultCode != Activity.RESULT_OK, intent?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI))
	}
	
	/**
	 * A result of a ringtone selector
	 * @param canceled Whether the user cancelled the selection dialog
	 * @param selectedURI The URI of the ringtone the user selected, or NULL for silent
	 */
	data class Result(val canceled: Boolean, val selectedURI: Uri?)
}