package me.tagavari.airmessage.helper;

import android.app.Person;
import android.app.RemoteAction;
import android.content.Context;
import android.os.Build;
import android.view.textclassifier.ConversationAction;
import android.view.textclassifier.ConversationActions;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;

import androidx.annotation.RequiresApi;

import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.messaging.AMConversationAction;
import me.tagavari.airmessage.messaging.MessageInfo;

public class SmartReplyHelper {
	public static final int smartReplyHistoryLength = 10;
	
	/**
	 * Maps a list of {@link MessageInfo} to {@link ConversationActions.Message}
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	public static List<ConversationActions.Message> messagesToTextClassifierMessageList(List<MessageInfo> messageList) {
		return messageList.stream()
				.filter(message -> message.getMessageText() != null) //Filter out empty messages
				.map(message -> {
					Person author = message.isOutgoing() ? ConversationActions.Message.PERSON_USER_SELF : new Person.Builder().setKey(message.getSender()).build();
					return new ConversationActions.Message.Builder(author)
							.setReferenceTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.getDate()), ZoneId.systemDefault()))
							.setText(message.getMessageText())
							.build();
				}).collect(Collectors.toList());
	}
	
	/**
	 * Maps a list of {@link MessageInfo} to {@link ConversationActions.Message}
	 */
	public static List<TextMessage> messagesToMLKitMessageList(List<MessageInfo> messageList) {
		return messageList.stream()
				.filter(message -> message.getMessageText() != null) //Filter out empty messages
				.map(message -> {
					if(message.getSender() == null) {
						return TextMessage.createForLocalUser(message.getMessageText(), message.getDate());
					} else {
						return TextMessage.createForRemoteUser(message.getMessageText(), message.getDate(), message.getSender());
					}
				}).collect(Collectors.toList());
	}
	
	/**
	 * Generate a list of suggested replies using an automatically determined engine
	 * @param context The context to use
	 * @param messages The messages to base the replies off of
	 * @return A single for an array of suggested conversation actions
	 */
	@CheckReturnValue
	public static Single<AMConversationAction[]> generateResponses(Context context, List<MessageInfo> messages) {
		List<MessageInfo> sortedMessages = new ArrayList<>(messages);
		Collections.sort(sortedMessages, (message1, message2) -> Long.compare(message1.getDate(), message2.getDate()));
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			//Use TextClassifier
			return generateResponsesTextClassifier(context, messagesToTextClassifierMessageList(sortedMessages))
					.map(result -> result.getConversationActions().stream().map(action -> {
						if(action.getType().equals(ConversationAction.TYPE_TEXT_REPLY)) {
							//Text replies
							return Optional.of(AMConversationAction.createReplyAction(action.getTextReply()));
						} else {
							//Action replies
							RemoteAction remoteAction = action.getAction();
							if(remoteAction == null) return Optional.<AMConversationAction>empty();
							else return Optional.of(AMConversationAction.createRemoteAction(new AMConversationAction.RemoteAction(remoteAction.shouldShowIcon() ? remoteAction.getIcon() : null, remoteAction.getTitle(), remoteAction.getActionIntent())));
						}
					}).filter(Optional::isPresent).map(Optional::get).toArray(AMConversationAction[]::new));
		} else {
			//Use MLKit
			return generateResponsesMLKit(messagesToMLKitMessageList(sortedMessages))
					.map(result -> Arrays.stream(result).map(AMConversationAction::createReplyAction).toArray(AMConversationAction[]::new));
		}
	}
	
	/**
	 * Generate a list of suggested replies using Android's built-in {@link TextClassifier}
	 * @param context The context to use
	 * @param messages The TextClassifier messages to base the replies off of
	 * @return A single for an array of strings representing the suggested responses
	 */
	@RequiresApi(api = Build.VERSION_CODES.Q)
	@CheckReturnValue
	public static Single<ConversationActions> generateResponsesTextClassifier(Context context, List<ConversationActions.Message> messages) {
		return Single.fromCallable(() -> {
			TextClassifier textClassifier = context.getSystemService(TextClassificationManager.class).getTextClassifier();
			return textClassifier.suggestConversationActions(
					new ConversationActions.Request.Builder(messages)
							.setMaxSuggestions(3)
							.setHints(Collections.singletonList(ConversationActions.Request.HINT_FOR_IN_APP))
							.build()
			);
		}).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Generate a list of suggested replies using ML Kit
	 * @param messages The ML Kit messages to base the replies off of
	 * @return A single for an array of strings representing the suggested responses
	 */
	@CheckReturnValue
	public static Single<String[]> generateResponsesMLKit(List<TextMessage> messages) {
		if(messages.isEmpty()) return Single.just(new String[0]);
		
		return Single.create(emitter -> SmartReply.getClient().suggestReplies(messages)
				.addOnSuccessListener(result -> {
					if(result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
						emitter.onSuccess(result.getSuggestions().stream().map(SmartReplySuggestion::getText).toArray(String[]::new));
					} else {
						emitter.onSuccess(new String[0]);
					}
				}).addOnFailureListener(emitter::onError));
	}
}