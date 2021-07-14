package me.tagavari.airmessage.recyclerupdate;

import androidx.annotation.Nullable;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.MemberInfo;

import java.util.List;

//Represents an update to a conversation
public abstract class PayloadConversationUpdate {
	//Represents an update to a conversation's preview
	public static final class Preview extends PayloadConversationUpdate {
		@Nullable
		private final ConversationPreview preview;
		
		public Preview(@Nullable ConversationPreview preview) {
			this.preview = preview;
		}
		
		@Nullable
		public ConversationPreview getPreview() {
			return preview;
		}
	}
	
	//Represents an update to a conversation's title
	public static final class Title extends PayloadConversationUpdate {
		@Nullable
		private final String title;
		
		public Title(@Nullable String title) {
			this.title = title;
		}
		
		@Nullable
		public String getTitle() {
			return title;
		}
	}
	
	//Represents an update to a conversation's members
	public static final class Member extends PayloadConversationUpdate {
		private final List<MemberInfo> members;
		
		public Member(List<MemberInfo> members) {
			this.members = members;
		}
		
		public List<MemberInfo> getMembers() {
			return members;
		}
	}
	
	//Represents an update to a conversation's muted value
	public static final class Muted extends PayloadConversationUpdate {
		private final boolean isMuted;
		
		public Muted(boolean isMuted) {
			this.isMuted = isMuted;
		}
		
		public boolean isMuted() {
			return isMuted;
		}
	}
	
	//Represents an update to a conversation's unread message count
	public static final class Unread extends PayloadConversationUpdate {
		private final int count;
		
		public Unread(int count) {
			this.count = count;
		}
		
		public int getCount() {
			return count;
		}
	}
	
	//Represents an update to the conversation's selection status
	public static final class Selection extends PayloadConversationUpdate {
		private final boolean isSelected;
		
		public Selection(boolean isSelected) {
			this.isSelected = isSelected;
		}
		
		public boolean isSelected() {
			return isSelected;
		}
	}
}