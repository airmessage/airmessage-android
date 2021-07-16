package me.tagavari.airmessage.helper

import android.app.Person
import android.content.Context
import android.os.Build
import android.view.textclassifier.ConversationAction
import android.view.textclassifier.ConversationActions
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import androidx.annotation.RequiresApi
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.messaging.AMConversationAction
import me.tagavari.airmessage.messaging.MessageInfo
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object SmartReplyHelper {
	const val smartReplyHistoryLength = 10
	
	/**
	 * Maps a list of [MessageInfo] to [ConversationActions.Message]
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	@JvmStatic
	fun messagesToTextClassifierMessageList(messageList: List<MessageInfo>): List<ConversationActions.Message> {
		return messageList
				.filter { message -> message.messageText != null } //Filter out empty messages
				.map { message ->
					val author = if(message.isOutgoing) ConversationActions.Message.PERSON_USER_SELF else Person.Builder().setKey(message.sender).build()
					ConversationActions.Message.Builder(author)
							.setReferenceTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.date), ZoneId.systemDefault()))
							.setText(message.messageText)
							.build()
				}
	}
	
	/**
	 * Maps a list of [MessageInfo] to [ConversationActions.Message]
	 */
	@JvmStatic
	fun messagesToMLKitMessageList(messageList: List<MessageInfo>): List<TextMessage> {
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
	 * Generate a list of suggested replies using an automatically determined engine
	 * @param context The context to use
	 * @param messages The messages to base the replies off of
	 * @return A single for an array of suggested conversation actions
	 */
	@JvmStatic
	@CheckReturnValue
	fun generateResponses(context: Context, messages: List<MessageInfo>): Single<List<AMConversationAction>> {
		val sortedMessages = messages.sortedBy { it.date }
		return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			//Use TextClassifier
			generateResponsesTextClassifier(context, messagesToTextClassifierMessageList(sortedMessages))
					.map { result: ConversationActions ->
						result.conversationActions.mapNotNull { action: ConversationAction ->
							if(action.type == ConversationAction.TYPE_TEXT_REPLY) {
								//Text replies
								return@mapNotNull AMConversationAction.createReplyAction(action.textReply!!)
							} else {
								//Action replies
								return@mapNotNull action.action?.let {
									AMConversationAction.createRemoteAction(AMConversationAction.RemoteAction(if(it.shouldShowIcon()) it.icon else null, it.title, it.actionIntent))
								}
							}
						}
					}
		} else {
			//Use MLKit
			generateResponsesMLKit(messagesToMLKitMessageList(sortedMessages))
					.map { it.map { message -> AMConversationAction.createReplyAction(message) } }
		}
	}
	
	/**
	 * Generate a list of suggested replies using Android's built-in [TextClassifier]
	 * @param context The context to use
	 * @param messages The TextClassifier messages to base the replies off of
	 * @return A single for an array of strings representing the suggested responses
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	@CheckReturnValue
	fun generateResponsesTextClassifier(context: Context, messages: List<ConversationActions.Message>): Single<ConversationActions> {
		return Single.fromCallable {
			val textClassifier = context.getSystemService(TextClassificationManager::class.java).textClassifier
			textClassifier.suggestConversationActions(
					ConversationActions.Request.Builder(messages)
							.setMaxSuggestions(3)
							.setHints(listOf(ConversationActions.Request.HINT_FOR_IN_APP))
							.build()
			)
		}.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
	}
	
	/**
	 * Generate a list of suggested replies using ML Kit
	 * @param messages The ML Kit messages to base the replies off of
	 * @return A single for an array of strings representing the suggested responses
	 */
	@CheckReturnValue
	fun generateResponsesMLKit(messages: List<TextMessage>): Single<List<String>> {
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
}