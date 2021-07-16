package me.tagavari.airmessage.task;

import android.content.Context;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationActionTask {
	/**
	 * Sets the unread count of the conversations, and emits an update
	 * @param conversations The conversations to mute / unmute
	 * @param unreadCount The unread count to apply
	 * @return A completable to represent this task
	 */
	@CheckReturnValue
	public static Completable unreadConversations(Collection<ConversationInfo> conversations, int unreadCount) {
		List<Long> conversationIDs = conversations.stream().map(ConversationInfo::getLocalID).collect(Collectors.toList());
		
		return Completable.fromAction(() -> {
			for(long conversationID : conversationIDs) DatabaseManager.getInstance().setUnreadMessageCount(conversationID, unreadCount);
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			for(ConversationInfo conversationInfo : conversations) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationUnread(conversationInfo, unreadCount));
		});
	}
	
	/**
	 * Sets the muted state of the conversations, and emits an update
	 * @param conversations The conversations to mute / unmute
	 * @param mute Whether to mute these conversations, or unmute them
	 * @return A completable to represent this task
	 */
	@CheckReturnValue
	public static Completable muteConversations(Collection<ConversationInfo> conversations, boolean mute) {
		List<Long> conversationIDs = conversations.stream().map(ConversationInfo::getLocalID).collect(Collectors.toList());
		
		return Completable.fromAction(() -> {
			for(long conversationID : conversationIDs) DatabaseManager.getInstance().updateConversationMuted(conversationID, mute);
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			for(ConversationInfo conversationInfo : conversations) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationMute(conversationInfo, mute));
		});
	}
	
	/**
	 * Sets the archival state of the conversations, and emits an update
	 * @param conversations The conversations to archive / unarchive
	 * @param archive Whether to archive these conversations, or unarchive them
	 * @return A completable to represent this task
	 */
	@CheckReturnValue
	public static Completable archiveConversations(Collection<ConversationInfo> conversations, boolean archive) {
		List<Long> conversationIDs = conversations.stream().map(ConversationInfo::getLocalID).collect(Collectors.toList());
		
		return Completable.fromAction(() -> {
			for(long conversationID : conversationIDs) DatabaseManager.getInstance().updateConversationArchived(conversationID, archive);
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			for(ConversationInfo conversationInfo : conversations) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationArchive(conversationInfo, archive));
		});
	}
	
	/**
	 * Deletes the conversations on disk and emits an update
	 */
	@CheckReturnValue
	public static Completable deleteConversations(Context context, Collection<ConversationInfo> conversations) {
		List<ConversationInfo> clonedConversations = conversations.stream().map(ConversationInfo::clone).collect(Collectors.toList());
		
		return Completable.fromAction(() -> {
			for(ConversationInfo conversationInfo : clonedConversations) {
				//Delete the conversation from AirMessage's database
				DatabaseManager.getInstance().deleteConversation(context, conversationInfo.getLocalID());
				
				//Deleting the conversation from the external database
				if(conversationInfo.getServiceHandler() == ServiceHandler.systemMessaging && Preferences.isTextMessageIntegrationActive(context)) {
					MMSSMSHelper.deleteConversation(context, conversationInfo.getExternalID());
				}
			}
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			for(ConversationInfo conversationInfo : conversations) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationDelete(conversationInfo));
		});
	}
	
	/**
	 * Sets a conversation's draft message
	 * @param conversationInfo The conversation to update
	 * @param draftMessage The conversation's draft message, or NULL if unavailable
	 * @param updateTime The time this update was completed
	 * @return A completable of this process
	 */
	@CheckReturnValue
	public static Completable setConversationDraft(ConversationInfo conversationInfo, @Nullable String draftMessage, long updateTime) {
		return Completable.fromAction(() -> {
			DatabaseManager.getInstance().updateConversationDraftMessage(conversationInfo.getLocalID(), draftMessage, updateTime);
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationDraftMessageUpdate(conversationInfo, draftMessage, updateTime));
		});
	}
	
	/**
	 * Sets a conversation's color
	 * @param conversationInfo The conversation to update
	 * @param color The new color to apply
	 * @return A completable of this process
	 */
	@CheckReturnValue
	public static Completable setConversationColor(ConversationInfo conversationInfo, int color) {
		return Completable.fromAction(() -> {
			DatabaseManager.getInstance().updateConversationColor(conversationInfo.getLocalID(), color);
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationColor(conversationInfo, color));
		});
	}
	
	/**
	 * Sets a conversation's color
	 * @param conversationInfo The conversation to update
	 * @param color The new color to apply
	 * @return A completable of this process
	 */
	@CheckReturnValue
	public static Completable setConversationMemberColor(ConversationInfo conversationInfo, MemberInfo memberInfo, int color) {
		return Completable.fromAction(() -> {
			DatabaseManager.getInstance().updateMemberColor(conversationInfo.getLocalID(), memberInfo.getAddress(), color);
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationMemberColor(conversationInfo, memberInfo, color));
		});
	}
	
	/**
	 * Sets a conversation's title
	 * @param conversationInfo The conversation to update
	 * @param title The new title to apply
	 * @return A completable of this process
	 */
	@CheckReturnValue
	public static Completable setConversationTitle(ConversationInfo conversationInfo, @Nullable String title) {
		return Completable.fromAction(() -> {
			DatabaseManager.getInstance().updateConversationTitle(conversationInfo.getLocalID(), title);
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationTitle(conversationInfo, title));
		});
	}
}