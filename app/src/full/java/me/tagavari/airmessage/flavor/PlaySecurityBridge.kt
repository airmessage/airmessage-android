package me.tagavari.airmessage.flavor

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener
import java.util.function.Consumer

object PlaySecurityBridge {
	val isSupported = true
	
	@JvmStatic
	fun showDialog(activity: Activity, errorCode: Int, resultCode: Int) {
		GoogleApiAvailability.getInstance().showErrorDialogFragment(activity, errorCode, resultCode)
	}
	
	@JvmStatic
	fun update(context: Context, onError: Consumer<Int>) {
		ProviderInstaller.installIfNeededAsync(context, object : ProviderInstallListener {
			override fun onProviderInstalled() = Unit
			override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
				val availability = GoogleApiAvailability.getInstance()
				if(availability.isUserResolvableError(errorCode)) {
					onError.accept(errorCode)
				}
			}
		})
	}
}