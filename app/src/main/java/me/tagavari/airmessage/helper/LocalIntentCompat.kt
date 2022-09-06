package me.tagavari.airmessage.helper

import android.content.Intent
import android.os.Build
import android.os.Bundle

inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? = when {
	Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T> Bundle.getParcelableCompat(key: String): T? = when {
	Build.VERSION.SDK_INT >= 33 -> getParcelable(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}