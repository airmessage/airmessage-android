package me.tagavari.airmessage.util

/**
 * Represents a collection of data that can be used to configure the connection of a certain type of proxy
 */
sealed class ConnectionParams {
	open class Security(val password: String) : ConnectionParams()
	open class Direct(val address: String, val fallbackAddress: String?, password: String) : Security(password)
}