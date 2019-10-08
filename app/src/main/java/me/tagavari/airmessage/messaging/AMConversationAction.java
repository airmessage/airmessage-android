package me.tagavari.airmessage.messaging;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

public class AMConversationAction {
	private final boolean isReplyAction;
	private final CharSequence replyString;
	private final AMRemoteAction remoteAction;
	
	private AMConversationAction(boolean isReplyAction, CharSequence replyString, AMRemoteAction remoteAction) {
		this.isReplyAction = isReplyAction;
		this.replyString = replyString;
		this.remoteAction = remoteAction;
	}
	
	public static AMConversationAction createReplyAction(CharSequence replyString) {
		return new AMConversationAction(true, replyString, null);
	}
	
	public static AMConversationAction createRemoteAction(AMRemoteAction remoteAction) {
		return new AMConversationAction(false, null, remoteAction);
	}
	
	public boolean isReplyAction() {
		return isReplyAction;
	}
	
	public CharSequence getReplyString() {
		return replyString;
	}
	
	public AMRemoteAction getRemoteAction() {
		return remoteAction;
	}
	
	public static class AMRemoteAction {
		private final Icon icon;
		private final CharSequence title;
		private final PendingIntent actionIntent;
		
		public AMRemoteAction(Icon icon, CharSequence title, PendingIntent actionIntent) {
			this.icon = icon;
			this.title = title;
			this.actionIntent = actionIntent;
		}
		
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