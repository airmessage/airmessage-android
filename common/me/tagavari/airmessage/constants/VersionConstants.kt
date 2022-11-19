package me.tagavari.airmessage.constants

object VersionConstants {
	/**
	 * The latest communications version that this client supports
	 */
	@JvmStatic
	val latestCommVer = listOf(5, 5)
	
	/**
	 * * The latest communications version that this client supports as a human-readable string
	 */
	@JvmStatic
	val latestCommVerString get() = latestCommVer.joinToString(".")
}