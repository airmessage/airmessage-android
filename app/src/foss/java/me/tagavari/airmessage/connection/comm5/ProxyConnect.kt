package me.tagavari.airmessage.connection.comm5

import android.content.Context
import me.tagavari.airmessage.connection.DataProxy
import me.tagavari.airmessage.util.ConnectionParams

class ProxyConnect: DataProxy<EncryptedPacket>() {
	override fun start(context: Context, override: ConnectionParams?) = Unit
	
	override fun stop(code: Int) = Unit
	
	override fun send(packet: EncryptedPacket) = false
	
	override fun isUsingFallback() = false
	
	fun sendTokenAdd(token: String) = Unit
}