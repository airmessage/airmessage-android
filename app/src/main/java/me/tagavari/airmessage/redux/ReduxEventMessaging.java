package me.tagavari.airmessage.redux;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.annotations.Nullable;
import kotlin.Pair;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.FileDraft;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.util.ModifierMetadata;
import me.tagavari.airmessage.util.ReplaceInsertResult;
import me.tagavari.airmessage.util.TransferredConversation;

//An event to represent updates to messages and conversations
public abstract class ReduxEventMessaging {
	//An abstract class for an action performed on a conversation
	public static abstract class ReduxConversationAction extends ReduxEventMessaging {
		private final ConversationInfo conversationInfo;
		
		public ReduxConversationAction(ConversationInfo conversationInfo) {
			this.conversationInfo = conversationInfo;
		}
		
		public ConversationInfo getConversationInfo() {
			return conversationInfo;
		}
	}
	
	//An abstract class for an action performed on a message
	public static abstract class ReduxMessageAction extends ReduxConversationAction {
		private final MessageInfo messageInfo;
		
		public ReduxMessageAction(ConversationInfo conversationInfo, MessageInfo messageInfo) {
			super(conversationInfo);
			this.messageInfo = messageInfo;
		}
		
		public MessageInfo getMessageInfo() {
			return messageInfo;
		}
	}
	
	//When new conversation items are received
	public static final class Message extends ReduxEventMessaging {
		//Conversation items that were added to an available conversation
		private final List<Pair<ConversationInfo, List<ReplaceInsertResult>>> conversationItems;
		
		public Message(List<Pair<ConversationInfo, List<ReplaceInsertResult>>> conversationItems) {
			this.conversationItems = conversationItems;
		}
		
		public List<Pair<ConversationInfo, List<ReplaceInsertResult>>> getConversationItems() {
			return conversationItems;
		}
	}
	
	//When a message's state changes
	public static final class MessageState extends ReduxEventMessaging {
		private final long messageID;
		private final @me.tagavari.airmessage.enums.MessageState int stateCode;
		private final long dateRead;
		
		public MessageState(long messageID, @me.tagavari.airmessage.enums.MessageState int stateCode, long dateRead) {
			this.messageID = messageID;
			this.stateCode = stateCode;
			this.dateRead = dateRead;
		}
		
		public MessageState(long messageID, @me.tagavari.airmessage.enums.MessageState int stateCode) {
			this(messageID, stateCode, -1);
		}
		
		public long getMessageID() {
			return messageID;
		}
		
		@me.tagavari.airmessage.enums.MessageState
		public int getStateCode() {
			return stateCode;
		}
		
		public long getDateRead() {
			return dateRead;
		}
	}
	
	//When a message's error changes
	public static final class MessageError extends ReduxMessageAction {
		private final @ConnectionErrorCode int errorCode;
		private final @Nullable String errorDetails;
		
		public MessageError(ConversationInfo conversationInfo, MessageInfo messageInfo, int errorCode, @Nullable String errorDetails) {
			super(conversationInfo, messageInfo);
			
			this.errorCode = errorCode;
			this.errorDetails = errorDetails;
		}
		
		@ConnectionErrorCode
		public int getErrorCode() {
			return errorCode;
		}
		
		@Nullable
		public String getErrorDetails() {
			return errorDetails;
		}
	}
	
	//When a message is deleted
	public static final class MessageDelete extends ReduxMessageAction {
		public MessageDelete(ConversationInfo conversationInfo, MessageInfo messageInfo) {
			super(conversationInfo, messageInfo);
		}
	}
	
	//When an attachment's file is updated
	public static final class AttachmentFile extends ReduxEventMessaging {
		private final long messageID;
		private final long attachmentID;
		@Nullable private final File file;
		
		public AttachmentFile(long messageID, long attachmentID, @Nullable File file) {
			this.messageID = messageID;
			this.attachmentID = attachmentID;
			this.file = file;
		}
		
		public long getMessageID() {
			return messageID;
		}
		
		public long getAttachmentID() {
			return attachmentID;
		}
		
		@Nullable
		public File getFile() {
			return file;
		}
	}
	
	//When a tapback is updated
	public static final class TapbackUpdate extends ReduxEventMessaging {
		private final TapbackInfo tapbackInfo;
		private final ModifierMetadata metadata;
		private final boolean isAddition;
		
		/**
		 * Constructs a new tapback update
		 * @param tapbackInfo The tapback info that is being updated
		 * @param metadata The tapback's positioning metadata
		 * @param isAddition TRUE if this update is an addition, FALSE if this update is a removal
		 */
		public TapbackUpdate(TapbackInfo tapbackInfo, ModifierMetadata metadata, boolean isAddition) {
			this.tapbackInfo = tapbackInfo;
			this.metadata = metadata;
			this.isAddition = isAddition;
		}
		
		public TapbackInfo getTapbackInfo() {
			return tapbackInfo;
		}
		
		public ModifierMetadata getMetadata() {
			return metadata;
		}
		
		public boolean isAddition() {
			return isAddition;
		}
	}
	
	//When a sticker is added
	public static final class StickerAdd extends ReduxEventMessaging {
		private final StickerInfo stickerInfo;
		private final ModifierMetadata metadata;
		
		public StickerAdd(StickerInfo stickerInfo, ModifierMetadata metadata) {
			this.stickerInfo = stickerInfo;
			this.metadata = metadata;
		}
		
		public StickerInfo getStickerInfo() {
			return stickerInfo;
		}
		
		public ModifierMetadata getMetadata() {
			return metadata;
		}
	}
	
	//When new conversation updates are received
	public static final class ConversationUpdate extends ReduxEventMessaging {
		private final Map<ConversationInfo, List<ConversationItem>> newConversations;
		private final Collection<TransferredConversation> transferredConversations;
		
		/**
		 * Constructs a new conversation update message
		 * @param newConversations A map of conversations to conversation items for freshly updated server-created conversations (that didn't need transfers)
		 * @param transferredConversations A list of transferred conversation details
		 */
		public ConversationUpdate(Map<ConversationInfo, List<ConversationItem>> newConversations, Collection<TransferredConversation> transferredConversations) {
			this.newConversations = newConversations;
			this.transferredConversations = transferredConversations;
		}
		
		public Map<ConversationInfo, List<ConversationItem>> getNewConversations() {
			return newConversations;
		}
		
		public Collection<TransferredConversation> getTransferredConversations() {
			return transferredConversations;
		}
	}
	
	//When a sync is determined to be needed
	public static final class Sync extends ReduxEventMessaging {
		private final String serverInstallationID;
		private final String serverName;
		
		/**
		 * Constructs a new sync trigger update
		 * @param serverInstallationID The installation ID of the server to sync from
		 * @param serverName The name of the server to sync from
		 */
		public Sync(String serverInstallationID, String serverName) {
			this.serverInstallationID = serverInstallationID;
			this.serverName = serverName;
		}
		
		/**
		 * Gets the installation ID of the server to sync from
		 */
		public String getServerInstallationID() {
			return serverInstallationID;
		}
		
		/**
		 * Gets the name of the server to sync from
		 */
		public String getServerName() {
			return serverName;
		}
	}
	
	//When a conversation's unread count changes
	public static final class ConversationUnread extends ReduxConversationAction {
		private final int unreadCount;
		
		public ConversationUnread(ConversationInfo conversationInfo, int unreadCount) {
			super(conversationInfo);
			this.unreadCount = unreadCount;
		}
		
		public int getUnreadCount() {
			return unreadCount;
		}
	}
	
	//When a conversation member joins or leaves
	public static final class ConversationMember extends ReduxConversationAction {
		private final MemberInfo member;
		private final boolean isJoin;
		
		public ConversationMember(ConversationInfo conversationInfo, MemberInfo member, boolean isJoin) {
			super(conversationInfo);
			this.member = member;
			this.isJoin = isJoin;
		}
		
		public MemberInfo getMember() {
			return member;
		}
		
		public boolean isJoin() {
			return isJoin;
		}
	}
	
	//When a conversation is muted or unmuted
	public static final class ConversationMute extends ReduxConversationAction {
		private final boolean isMuted;
		
		public ConversationMute(ConversationInfo conversationInfo, boolean isMuted) {
			super(conversationInfo);
			this.isMuted = isMuted;
		}
		
		public boolean isMuted() {
			return isMuted;
		}
	}
	
	//When a conversation is archived or unarchived
	public static final class ConversationArchive extends ReduxConversationAction {
		private final boolean isArchived;
		
		public ConversationArchive(ConversationInfo conversationInfo, boolean isArchived) {
			super(conversationInfo);
			this.isArchived = isArchived;
		}
		
		public boolean isArchived() {
			return isArchived;
		}
	}
	
	//When a conversation is deleted
	public static final class ConversationDelete extends ReduxConversationAction {
		public ConversationDelete(ConversationInfo conversationInfo) {
			super(conversationInfo);
		}
	}
	
	//When a bunch of conversations are deleted by service handler
	public static final class ConversationServiceHandlerDelete extends ReduxEventMessaging {
		@ServiceHandler private final int serviceHandler;
		private final long[] deletedIDs;
		
		/**
		 * When a bunch of conversations are deleted by service handler
		 * @param serviceHandler The service handler of the deleted conversations
		 * @param deletedIDs A list of the local IDs of all deleted conversations
		 */
		public ConversationServiceHandlerDelete(@ServiceHandler int serviceHandler, long[] deletedIDs) {
			this.serviceHandler = serviceHandler;
			this.deletedIDs = deletedIDs;
		}
		
		@ServiceHandler
		public int getServiceHandler() {
			return serviceHandler;
		}
		
		public long[] getDeletedIDs() {
			return deletedIDs;
		}
	}
	
	//When a conversation's title changes
	public static final class ConversationTitle extends ReduxConversationAction {
		private final @Nullable String title;
		
		public ConversationTitle(ConversationInfo conversationInfo, @Nullable String title) {
			super(conversationInfo);
			this.title = title;
		}
		
		@Nullable
		public String getTitle() {
			return title;
		}
	}
	
	//When a conversation's draft message changes
	public static final class ConversationDraftMessageUpdate extends ReduxConversationAction {
		@Nullable private final String draftMessage;
		private final long updateTime;
		
		public ConversationDraftMessageUpdate(ConversationInfo conversationInfo, @Nullable String draftMessage, long updateTime) {
			super(conversationInfo);
			this.draftMessage = draftMessage;
			this.updateTime = updateTime;
		}
		
		@Nullable
		public String getDraftMessage() {
			return draftMessage;
		}
		
		public long getUpdateTime() {
			return updateTime;
		}
	}
	
	//When a conversation's draft files changes
	public static final class ConversationDraftFileUpdate extends ReduxConversationAction {
		private final FileDraft draft;
		private final boolean isAddition;
		private final long updateTime;
		
		public ConversationDraftFileUpdate(ConversationInfo conversationInfo, FileDraft draft, boolean isAddition, long updateTime) {
			super(conversationInfo);
			this.draft = draft;
			this.isAddition = isAddition;
			this.updateTime = updateTime;
		}
		
		public FileDraft getDraft() {
			return draft;
		}
		
		public boolean isAddition() {
			return isAddition;
		}
		
		public long getUpdateTime() {
			return updateTime;
		}
	}
	
	//When a conversation's draft files are cleared
	public static final class ConversationDraftFileClear extends ReduxConversationAction {
		public ConversationDraftFileClear(ConversationInfo conversationInfo) {
			super(conversationInfo);
		}
	}
	
	//When a conversation's color changes
	public static final class ConversationColor extends ReduxConversationAction {
		private final int color;
		
		public ConversationColor(ConversationInfo conversationInfo, int color) {
			super(conversationInfo);
			this.color = color;
		}
		
		public int getColor() {
			return color;
		}
	}
	
	//When a conversation members's color changes
	public static final class ConversationMemberColor extends ReduxConversationAction {
		private final MemberInfo memberInfo;
		private final int color;
		
		public ConversationMemberColor(ConversationInfo conversationInfo, MemberInfo memberInfo, int color) {
			super(conversationInfo);
			this.memberInfo = memberInfo;
			this.color = color;
		}
		
		public MemberInfo getMemberInfo() {
			return memberInfo;
		}
		
		public int getColor() {
			return color;
		}
	}
}