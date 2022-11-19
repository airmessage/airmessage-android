package me.tagavari.airmessage.common.util

import java.util.*

/**
 * Represents details for the user to configure for a direct connection.
 * This cannot actually be used to create a direct connection, since the address and password are nullable.
 * For a class that holds data to create a direct connection, see [ConnectionParams.Direct].
 */
data class DirectConnectionDetails(val address: String?, val fallbackAddress: String?, val password: String?) {
	override fun toString(): String {
		return StringJoiner(" | ", DirectConnectionDetails::class.java.simpleName + " [", "]")
			.add("address=$address")
			.add("fallbackAddress=$fallbackAddress")
			.add("password=$password")
			.toString()
	}
	
	/**
	 * Converts these connections details to a [ConnectionParams.Direct] if applicable, or NULL otherwise
	 */
	fun toConnectionParams(): ConnectionParams.Direct? {
		if(address == null || password == null) return null
		
		return ConnectionParams.Direct(address, fallbackAddress, password)
	}
}