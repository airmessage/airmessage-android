package me.tagavari.airmessage.messaging;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

import androidx.annotation.Nullable;

/**
 * Represents a suggested action that can be taken in a conversation
 *
 * Actions can either be reply actions that send a specified message,
 * or remote actions that launch an external intent
 */
public class AMConversationAction {
	private final boolean isReplyAction;
	private final CharSequence replyString;
	private final RemoteAction remoteAction;
	
	private AMConversationAction(boolean isReplyAction, CharSequence replyString, RemoteAction remoteAction) {
		this.isReplyAction = isReplyAction;
		this.replyString = replyString;
		this.remoteAction = remoteAction;
	}
	
	public static AMConversationAction createReplyAction(CharSequence replyString) {
		return new AMConversationAction(true, replyString, null);
	}
	
	public static AMConversationAction createRemoteAction(RemoteAction remoteAction) {
		return new AMConversationAction(false, null, remoteAction);
	}
	
	public boolean isReplyAction() {
		return isReplyAction;
	}
	
	public CharSequence getReplyString() {
		return replyString;
	}
	
	public RemoteAction getRemoteAction() {
		return remoteAction;
	}
	
	public static class RemoteAction {
		@Nullable private final Icon icon;
		private final CharSequence title;
		private final PendingIntent actionIntent;
		
		public RemoteAction(@Nullable Icon icon, CharSequence title, PendingIntent actionIntent) {
			this.icon = icon;
			this.title = title;
			this.actionIntent = actionIntent;
		}
		
		@Nullable
		public Icon getIcon() {
			return icon;
		}
		
		public CharSequence getTitle() {
			return title;
		}
		
		public PendingIntent getActionIntent() {
			return actionIntent;
		}
	}
}