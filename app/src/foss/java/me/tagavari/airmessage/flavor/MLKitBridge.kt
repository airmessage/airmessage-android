package me.tagavari.airmessage.flavor

import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Single
import me.tagavari.airmessage.messaging.MessageInfo

object MLKitBridge {
	const val isSupported = false
	
	@CheckReturnValue
	@JvmStatic
	@Suppress("UNUSED_PARAMETER")
	fun generate(messages: List<MessageInfo>): Single<List<String>> = Single.just(listOf())
	
	@CheckReturnValue
	@JvmStatic
	@Suppress("UNUSED_PARAMETER")
	fun generateFromDatabase(conversationID: Long): Single<List<String>> = Single.just(listOf())
}