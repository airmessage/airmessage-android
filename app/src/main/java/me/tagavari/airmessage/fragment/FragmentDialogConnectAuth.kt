package me.tagavari.airmessage.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.rxjava3.disposables.Disposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.connection.ConnectionOverride
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.enums.ProxyType
import me.tagavari.airmessage.helper.StringHelper.nullifyEmptyString
import me.tagavari.airmessage.redux.ReduxEmitterNetwork.connectionStateSubject
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.service.ConnectionService
import me.tagavari.airmessage.util.ConnectionParams
import java.io.IOException
import java.security.GeneralSecurityException

class FragmentDialogConnectAuth : DialogFragment() {
	private lateinit var connectionSubscription: Disposable
	
	private var dialogInitialized = false
	private lateinit var inputField: TextInputLayout
	private lateinit var primaryButton: Button
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		//Subscribing to connection updates
		connectionSubscription = connectionStateSubject.subscribe(this::onConnectionUpdate)
	}
	
	override fun onStart() {
		super.onStart()
		
		val dialog = dialog as AlertDialog
		
		//Overriding the functionality of the dialog's primary button
		primaryButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
		primaryButton.isEnabled = false
		inputField.editText!!.addTextChangedListener(onTextChanged = { text, _, _, _ ->
			//Hide the error
			inputField.isErrorEnabled = false
			
			//Enable the button if the user entered a valid password
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !text.isNullOrBlank()
		})
		primaryButton.setOnClickListener {
			//Getting the connection manager
			ConnectionService.getConnectionManager()?.let { connectionManager ->
				//Updating the password and reconnecting
				val password: String = inputField.editText!!.text.toString()
				connectionManager.setConnectionOverride(ConnectionOverride(ProxyType.connect, ConnectionParams.Security(password)))
				connectionManager.connect()
			}
		}
		
		dialogInitialized = true
	}
	
	override fun onDestroy() {
		super.onDestroy()
		
		connectionSubscription.dispose()
	}
	
	private fun onConnectionUpdate(event: ReduxEventConnection) {
		if(!dialogInitialized) return
		
		//Disabling the input field and primary button if the state is connecting
		val isConnecting = event is ReduxEventConnection.Connecting
		inputField.isEnabled = !isConnecting
		primaryButton.isEnabled = !isConnecting
		
		if(event is ReduxEventConnection.Connected) {
			//Connection is OK, close the dialog
			dismiss()
			
			//Save the new password to disk
			try {
				SharedPreferencesManager.setDirectConnectionPassword(context, nullifyEmptyString(inputField.editText!!.text.toString().trim()))
			} catch(exception: GeneralSecurityException) {
				exception.printStackTrace()
				FirebaseCrashlytics.getInstance().recordException(exception)
			} catch(exception: IOException) {
				exception.printStackTrace()
				FirebaseCrashlytics.getInstance().recordException(exception)
			}
			
			//Show a confirmation toast
			Toast.makeText(context, R.string.message_passwordupdated, Toast.LENGTH_SHORT).show()
		} else if(event is ReduxEventConnection.Disconnected) {
			if(event.code == ConnectionErrorCode.unauthorized) {
				//Show an error
				inputField.isErrorEnabled = true
				inputField.error = resources.getString(R.string.message_serverstatus_authfail)
			} else {
				//Dismiss the dialog and let the user see the connection error
				dismiss()
			}
		}
	}
	
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		activity?.let { activity ->
			//Creating the view
			val dialogView: View = activity.layoutInflater.inflate(R.layout.dialog_connectpassword, null)
			inputField = dialogView.findViewById(R.id.input)
			
			//Creating the dialog
			return MaterialAlertDialogBuilder(activity).apply {
				setView(dialogView)
				setTitle(R.string.action_updatepassword)
				setPositiveButton(R.string.action_continue, null)
				setNegativeButton(android.R.string.cancel, null)
			}.create().apply {
				setOnShowListener {
					//Focusing the text view
					inputField.requestFocus()
					context.getSystemService(InputMethodManager::class.java).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
				}
				setOnDismissListener {
					//Hiding the keyboard
					context.getSystemService(InputMethodManager::class.java).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
				}
			}
		} ?: throw IllegalStateException("Activity cannot be null")
	}
}