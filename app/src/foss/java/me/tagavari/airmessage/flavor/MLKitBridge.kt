package me.tagavari.airmessage.flavor

import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Single
import me.tagavari.airmessage.messaging.MessageInfo

object MLKitBridge {
	val isSupported = false
	
	@CheckReturnValue
	@JvmStatic
	fun generate(messages: List<MessageInfo>): Single<List<String>> = Single.just(listOf())
	
	@CheckReturnValue
	@JvmStatic
	fun generateFromDatabase(conversationID: Long): Single<List<String>> = Single.just(listOf())
}