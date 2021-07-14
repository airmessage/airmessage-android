package me.tagavari.airmessage.util

open class ConnectionParams {
	open class Security(val password: String) : ConnectionParams()
	open class Direct(val address: String, val fallbackAddress: String?, password: String) : Security(password)
}