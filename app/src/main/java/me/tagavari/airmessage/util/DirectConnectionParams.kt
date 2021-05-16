package me.tagavari.airmessage.util

import java.util.StringJoiner

class DirectConnectionParams(val address: String?, val fallbackAddress: String?, val password: String?) {
	override fun toString(): String {
		return StringJoiner(" | ", DirectConnectionParams::class.java.simpleName + " [", "]")
			.add("address=$address")
			.add("fallbackAddress=$fallbackAddress")
			.add("password=$password")
			.toString()
	}
}