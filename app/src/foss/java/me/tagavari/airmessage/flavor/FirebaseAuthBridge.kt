package me.tagavari.airmessage.flavor

import android.content.Context
import androidx.compose.runtime.Composable

object FirebaseAuthBridge {
	/**
	 * Whether the Firebase auth bridge is supported
	 */
	@JvmStatic
	val isSupported = false
	
	/**
	 * Gets a human-readable summary of the currently signed in user,
	 * or NULL if there is no signed-in user
	 */
	@JvmStatic
	val userSummary: String? = null
	
	/**
	 * Signs out the currently signed-in user
	 */
	@Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
	suspend fun signOut(context: Context) = Unit
	
	/**
	 * Handles the Google sign-in flow
	 * @param onResult A callback invoked when the user is signed in, or with
	 * an exception if the user couldn't be signed in
	 * @return A callback to start the sign-in flow
	 */
	@Suppress("UNUSED_PARAMETER")
	@Composable
	fun rememberGoogleSignIn(onResult: (Result<Unit>) -> Unit): () -> Unit {
		return { throw UnsupportedOperationException() }
	}
}