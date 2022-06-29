package me.tagavari.airmessage.flavor

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import me.tagavari.airmessage.R
import me.tagavari.airmessage.fragment.FragmentOnboardingConnect
import me.tagavari.airmessage.fragment.FragmentOnboardingWelcome

class WelcomeGoogleSignIn constructor(private val fragment: FragmentOnboardingWelcome) {
    val isSupported = true

    private val mAuth = FirebaseAuth.getInstance()
    private val googleSignInClient = GoogleSignIn.getClient(
            fragment.requireContext(),
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
                requestIdToken(fragment.getString(R.string.default_web_client_id))
                requestEmail()
            }.build()
    )

    private val googleSignInLauncher: ActivityResultLauncher<Intent> = fragment.registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // Google Sign In was successful, authenticate with Firebase
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch(exception: ApiException) {
            exception.printStackTrace()
            if(exception.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                //Displaying an error snackbar
                Snackbar.make(fragment.requireView(), R.string.message_signinerror, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(FragmentOnboardingWelcome.TAG, "firebaseAuthWithGoogle:" + acct.id)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener(fragment.requireActivity()) { task: Task<AuthResult> ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(FragmentOnboardingWelcome.TAG, "signInWithCredential:success")

                //Advancing to the connection fragment
                fragment.communicationsCallback?.swapFragment(FragmentOnboardingConnect())
            } else {
                // If sign in fails, display a message to the user.
                Log.w(FragmentOnboardingWelcome.TAG, "signInWithCredential:failure", task.exception)

                //Displaying an error snackbar
                Snackbar.make(fragment.requireView(), R.string.message_signinerror, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    fun launch() {
        //Checking if Google Play Services are available
        val googleAPIAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(fragment.requireContext())
        if(googleAPIAvailability == ConnectionResult.SUCCESS) {
            //Starting Google sign-in
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        } else {
            //Prompting the user
            GoogleApiAvailability.getInstance().getErrorDialog(fragment.requireActivity(), googleAPIAvailability, 0)?.show()
        }
    }
}