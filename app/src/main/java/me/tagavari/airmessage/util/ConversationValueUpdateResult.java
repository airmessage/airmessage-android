package me.tagavari.airmessage.util;

import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;

public class ConversationValueUpdateResult {
	private final boolean unarchived;
	private final int unreadIncrement;
	
	public ConversationValueUpdateResult(boolean unarchived, int unreadIncrement) {
		this.unarchived = unarchived;
		this.unreadIncrement = unreadIncrement;
	}
	
	public boolean isUnarchived() {
		return unarchived;
	}
	
	public int getUnreadIncrement() {
		return unreadIncrement;
	}
	
	public List<ReduxEventMessaging> getEvents(ConversationInfo conversationInfo) {
		List<ReduxEventMessaging> events = new ArrayList<>(2);
		if(unarchived) events.add(new ReduxEventMessaging.ConversationArchive(conversationInfo, false));
		if(unreadIncrement > 0) events.add(new ReduxEventMessaging.ConversationUnread(conversationInfo, conversationInfo.getUnreadMessageCount() + unreadIncrement));
		return events;
	}
	
	public void emitUpdate(ConversationInfo conversationInfo) {
		//Emitting updates
		if(unarchived) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationArchive(conversationInfo, false));
		if(unreadIncrement > 0) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationUnread(conversationInfo, conversationInfo.getUnreadMessageCount() + unreadIncrement));
	}
}