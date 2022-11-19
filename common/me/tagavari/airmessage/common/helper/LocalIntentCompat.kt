package me.tagavari.airmessage.common.helper

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable

inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Intent.getParcelableArrayExtraCompat(key: String): Array<T>? = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayExtra(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelableArrayExtra(key) as? Array<T>?
}

inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(key: String): ArrayList<T>? = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayListExtra(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

inline fun <reified T> Bundle.getParcelableCompat(key: String): T? = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Bundle.getParcelableArrayCompat(key: String): Array<T>? = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArray(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelableArray(key) as? Array<T>?
}

inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayList(key, T::class.java)
	else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}
