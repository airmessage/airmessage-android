package me.tagavari.airmessage.flavor

import android.view.textclassifier.ConversationActions
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.loadConversationForMLKit
import me.tagavari.airmessage.messaging.MessageInfo

object MLKitBridge {
	val isSupported = true
	
	/**
	 * Maps a list of [MessageInfo] to [ConversationActions.Message]
	 */
	private fun messagesToMLKitMessageList(messageList: List<MessageInfo>): List<TextMessage> {
		return messageList
				.filter { message -> message.messageText != null } //Filter out empty messages
				.map { message ->
					if(message.sender == null) {
						return@map TextMessage.createForLocalUser(message.messageText!!, message.date)
					} else {
						return@map TextMessage.createForRemoteUser(message.messageText!!, message.date, message.sender!!)
					}
				}
	}
	
	/**
	 * Generate a list of suggested replies using ML Kit
	 * @param messages The ML Kit messages to base the replies off of
	 * @return A single for an array of strings representing the suggested responses
	 */
	@CheckReturnValue
	private fun generateResponsesMLKit(messages: List<TextMessage>): Single<List<String>> {
		return if(messages.isEmpty()) {
			Single.just(emptyList())
		} else {
			Single.create { emitter: SingleEmitter<List<String>> ->
				SmartReply.getClient().suggestReplies(messages)
						.addOnSuccessListener { result: SmartReplySuggestionResult ->
							if(result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
								emitter.onSuccess(result.suggestions.map { it.text })
							} else {
								emitter.onSuccess(emptyList())
							}
						}.addOnFailureListener { emitter.onError(it) }
			}
		}
	}
	
	@CheckReturnValue
	@JvmStatic
	fun generate(messages: List<MessageInfo>): Single<List<String>> {
		return generateResponsesMLKit(messagesToMLKitMessageList(messages))
	}
	
	@CheckReturnValue
	@JvmStatic
	fun generateFromDatabase(conversationID: Long): Single<List<String>> {
		return Single.fromCallable { DatabaseManager.getInstance().loadConversationForMLKit(conversationID) }
				.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
				.flatMap { messages -> generateResponsesMLKit(messages) }
	}
}