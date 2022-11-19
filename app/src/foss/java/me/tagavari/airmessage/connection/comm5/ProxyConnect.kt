package me.tagavari.airmessage.connection.comm5

import android.content.Context
import me.tagavari.airmessage.common.connection.DataProxy
import me.tagavari.airmessage.common.connection.comm5.EncryptedPacket
import me.tagavari.airmessage.common.util.ConnectionParams

class ProxyConnect: DataProxy<EncryptedPacket>() {
	override fun start(context: Context, override: ConnectionParams?) = Unit
	
	override fun stop(code: Int) = Unit
	
	override fun send(packet: EncryptedPacket) = false
	
	override fun isUsingFallback() = false
	
	fun sendTokenAdd(token: String) = Unit
	
	companion object {
		const val isAvailable = true
	}
}