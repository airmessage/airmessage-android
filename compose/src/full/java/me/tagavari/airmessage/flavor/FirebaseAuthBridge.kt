package me.tagavari.airmessage.flavor

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.compose.util.findActivity

object FirebaseAuthBridge {
	/**
	 * Whether the Firebase auth bridge is supported
	 */
	@JvmStatic
	val isSupported = true
	
	/**
	 * Gets a human-readable summary of the currently signed in user,
	 * or NULL if there is no signed-in user
	 */
	@JvmStatic
	val userSummary: String?
		get() = FirebaseAuth.getInstance().currentUser?.run { "$displayName ($email)" }
	
	/**
	 * Signs out the currently signed-in user
	 */
	suspend fun signOut(context: Context) {
		FirebaseAuth.getInstance().signOut()
		GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().await()
	}
	
	/**
	 * Handles the Google sign-in flow
	 * @param onResult A callback invoked when the user is signed in, or with
	 * an exception if the user couldn't be signed in
	 * @return A callback to start the sign-in flow
	 */
	@Composable
	fun rememberGoogleSignIn(onResult: (Result<Unit>) -> Unit): () -> Unit {
		if(LocalInspectionMode.current) {
			return {}
		}
		
		val activity = LocalContext.current.findActivity()
		val scope = rememberCoroutineScope()
		
		val googleSignInClient = remember {
			GoogleSignIn.getClient(
				activity,
				GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
					//requestIdToken(activity.resources.getString(R.string.default_web_client_id))
					requestEmail()
				}.build()
			)
		}
		
		val googleSignInLauncher = rememberLauncherForActivityResult(ContractGoogleSignIn()) { result ->
			//Propagate failures upwards
			result.onFailure { exception ->
				//Ignore if the user cancels
				if(exception !is ApiException
					|| exception.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
					onResult(Result.failure(exception))
				}
				
				return@rememberLauncherForActivityResult
			}
			
			//Get the signed in account
			val account = result.getOrThrow()
			
			scope.launch {
				//Authenticate with Firebase
				val credential = GoogleAuthProvider.getCredential(account.idToken, null)
				try {
					FirebaseAuth.getInstance().signInWithCredential(credential).await()
				} catch(exception: Throwable) {
					onResult(Result.failure(exception))
					return@launch
				}
				
				//Successfully authenticated!
				onResult(Result.success(Unit))
			}
		}
		
		return function@{
			//Make sure Google Play Services are available
			val googleAPIAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
			if(googleAPIAvailability != ConnectionResult.SUCCESS) {
				GoogleApiAvailability.getInstance().getErrorDialog(activity, googleAPIAvailability, 0)?.show()
				return@function
			}
			
			//Launch the sign-in request
			googleSignInLauncher.launch(googleSignInClient)
		}
	}
}

private class ContractGoogleSignIn : ActivityResultContract<GoogleSignInClient, Result<GoogleSignInAccount>>() {
	override fun createIntent(context: Context, input: GoogleSignInClient): Intent {
		return input.signInIntent
	}
	
	override fun parseResult(resultCode: Int, intent: Intent?): Result<GoogleSignInAccount> {
		return runCatching { GoogleSignIn.getSignedInAccountFromIntent(intent).getResult(ApiException::class.java) }
	}
}