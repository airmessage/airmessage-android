package me.tagavari.airmessage.helper

import android.app.Person
import android.content.Context
import android.os.Build
import android.view.textclassifier.ConversationAction
import android.view.textclassifier.ConversationActions
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.flavor.MLKitBridge
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
	 * Generate a list of suggested replies using an automatically determined engine
	 * @param context The context to use
	 * @param messages The messages to base the replies off of
	 * @return A single for an array of suggested conversation actions
	 */
	@JvmStatic
	@CheckReturnValue
	fun generateResponses(context: Context, messages: Collection<MessageInfo>): Single<List<AMConversationAction>> {
		val sortedMessages = messages.sortedBy { it.date }
		return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			//Use TextClassifier
			generateResponsesTextClassifier(context, messagesToTextClassifierMessageList(sortedMessages))
					.map { result: ConversationActions ->
						result.conversationActions.mapNotNull { action: ConversationAction ->
							if(action.type == ConversationAction.TYPE_TEXT_REPLY) {
								//Text replies
								return@mapNotNull AMConversationAction.createReplyAction(action.textReply!!.toString())
							} else {
								//Action replies
								return@mapNotNull action.action?.let {
									AMConversationAction.createRemoteAction(AMConversationAction.RemoteAction(if(it.shouldShowIcon()) it.icon else null, it.title.toString(), it.actionIntent))
								}
							}
						}
					}
		} else if(MLKitBridge.isSupported) {
			//Use MLKit
			MLKitBridge.generate(sortedMessages)
					.map { it.map { message -> AMConversationAction.createReplyAction(message) } }
		} else {
			Single.just(listOf())
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
			val textClassifier = ContextCompat.getSystemService(context, TextClassificationManager::class.java)?.textClassifier ?: TextClassifier.NO_OP
			
			textClassifier.suggestConversationActions(
					ConversationActions.Request.Builder(messages)
							.setMaxSuggestions(3)
							.setHints(listOf(ConversationActions.Request.HINT_FOR_IN_APP))
							.build()
			)
		}.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
	}
}