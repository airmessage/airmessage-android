package me.tagavari.airmessage.flavor

import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthBridge {
	@JvmStatic
	val isSupported = true
	
	@JvmStatic
	fun getUserSummary(): String? = FirebaseAuth.getInstance().currentUser?.run { "$displayName ($email)" }
}