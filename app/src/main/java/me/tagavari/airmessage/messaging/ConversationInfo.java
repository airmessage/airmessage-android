package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.util.ConversationTarget;

public class ConversationInfo implements Parcelable {
	//Creating the values
	private long localID;
	private String guid;
	private long externalID = -1;
	@ConversationState private int state;
	@ServiceHandler private int serviceHandler;
	@ServiceType private String serviceType;
	private int conversationColor = 0xFF000000; //Black
	private List<MemberInfo> members;
	@Nullable private String title = null;
	private transient int unreadMessageCount = 0;
	private boolean isArchived = false;
	private boolean isMuted = false;
	@Nullable private ConversationPreview messagePreview;
	@Nullable private String draftMessage;
	private final List<FileDraft> draftFiles;
	private long draftUpdateTime;
	
	public ConversationInfo(long localID, String guid, long externalID, int state, int serviceHandler, String serviceType, int conversationColor, List<MemberInfo> members, @Nullable String title, int unreadMessageCount, boolean isArchived, boolean isMuted, @Nullable ConversationPreview messagePreview, @Nullable String draftMessage, List<FileDraft> draftFiles, long draftUpdateTime) {
		this.localID = localID;
		this.guid = guid;
		this.externalID = externalID;
		this.state = state;
		this.serviceHandler = serviceHandler;
		this.serviceType = serviceType;
		this.conversationColor = conversationColor;
		this.members = members;
		this.title = title;
		this.unreadMessageCount = unreadMessageCount;
		this.isArchived = isArchived;
		this.isMuted = isMuted;
		this.messagePreview = messagePreview;
		this.draftMessage = draftMessage;
		this.draftFiles = draftFiles;
		this.draftUpdateTime = draftUpdateTime;
	}
	
	public ConversationInfo(long localID, String guid, long externalID, int state, int serviceHandler, String serviceType, int conversationColor, List<MemberInfo> members, @Nullable String title) {
		this(localID, guid, externalID, state, serviceHandler, serviceType, conversationColor, members, title, 0, false, false, null, null, new ArrayList<>(), -1);
	}
	
	/**
	 * Gets this conversation's local ID
	 */
	public long getLocalID() {
		return localID;
	}
	
	/**
	 * Sets this conversation's local ID
	 */
	public void setLocalID(long localID) {
		this.localID = localID;
	}
	
	/**
	 * Gets this conversation's server-linked GUID
	 */
	public String getGUID() {
		return guid;
	}
	
	/**
	 * Sets this conversation's server-linked GUID
	 */
	public void setGUID(String guid) {
		this.guid = guid;
	}
	
	/**
	 * Gets this conversation's external ID
	 */
	public long getExternalID() {
		return externalID;
	}
	
	/**
	 * Sets this conversation's external ID
	 */
	public void setExternalID(long externalID) {
		this.externalID = externalID;
	}
	
	/**
	 * Gets this conversation's state
	 */
	public int getState() {
		return state;
	}
	
	/**
	 * Sets this conversation's state
	 */
	public void setState(@ConversationState int state) {
		this.state = state;
	}
	
	/**
	 * Gets this conversation's service handler
	 */
	public int getServiceHandler() {
		return serviceHandler;
	}
	
	/**
	 * Sets this conversation's service handler
	 */
	public void setServiceHandler(@ServiceType int serviceHandler) {
		this.serviceHandler = serviceHandler;
	}
	
	/**
	 * Gets this conversation's service type
	 */
	public String getServiceType() {
		return serviceType;
	}
	
	/**
	 * Sets this conversation's service type
	 */
	public void setServiceType(@ServiceType String serviceType) {
		this.serviceType = serviceType;
	}
	
	/**
	 * Gets this conversation's members
	 */
	public List<MemberInfo> getMembers() {
		return members;
	}
	
	/**
	 * Sets this conversation's members
	 */
	public void setMembers(List<MemberInfo> members) {
		this.members = members;
	}
	
	/**
	 * Gets whether this conversation is a group conversation
	 */
	public boolean isGroupChat() {
		return members.size() > 1;
	}
	
	/**
	 * Gets this conversation's static title
	 */
	@Nullable
	public String getTitle() {
		return title;
	}
	
	/**
	 * Sets this conversation's static title
	 */
	public void setTitle(@Nullable String title) {
		this.title = title;
	}
	
	/**
	 * Gets this conversation's unread message count
	 */
	public int getUnreadMessageCount() {
		return unreadMessageCount;
	}
	
	/**
	 * Sets this conversation's unread message count
	 */
	public void setUnreadMessageCount(int unreadMessageCount) {
		this.unreadMessageCount = unreadMessageCount;
	}
	
	/**
	 * Gets whether this conversation is archived
	 */
	public boolean isArchived() {
		return isArchived;
	}
	
	/**
	 * Sets whether this conversation is archived
	 */
	public void setArchived(boolean archived) {
		isArchived = archived;
	}
	
	/**
	 * Gets whether this conversation is muted
	 */
	public boolean isMuted() {
		return isMuted;
	}
	
	/**
	 * Sets whether this conversation is muted
	 */
	public void setMuted(boolean muted) {
		isMuted = muted;
	}
	
	/**
	 * Gets this conversation's color
	 */
	public int getConversationColor() {
		return conversationColor;
	}
	
	/**
	 * Sets this conversation's color
	 */
	public void setConversationColor(int conversationColor) {
		this.conversationColor = conversationColor;
	}
	
	/**
	 * Gets the current message preview for this conversation
	 */
	@Nullable
	public ConversationPreview getMessagePreview() {
		return messagePreview;
	}
	
	/**
	 * Constructs and returns a {@link ConversationPreview} from the draft data
	 */
	@Nullable
	public ConversationPreview getDraftPreview() {
		if(draftUpdateTime == -1 || (draftMessage == null && draftFiles.isEmpty())) return null;
		return new ConversationPreview.Draft(draftUpdateTime, draftMessage, draftFiles.stream().map(draft -> new AttachmentPreview(draft.getFileName(), draft.getFileType())).toArray(AttachmentPreview[]::new));
	}
	
	/**
	 * Gets either the message preview or draft preview for this conversation, whichever is more recent
	 */
	@Nullable
	public ConversationPreview getDynamicPreview() {
		ConversationPreview messagePreview = getMessagePreview();
		ConversationPreview draftPreview = getDraftPreview();
		if(messagePreview == null && draftPreview == null) {
			return null;
		} if((messagePreview != null && draftPreview == null) || messagePreview.getDate() > draftPreview.getDate()) {
			return messagePreview;
		} else if((draftPreview != null && messagePreview == null) || draftPreview.getDate() > messagePreview.getDate()) {
			return draftPreview;
		} else {
			return null;
		}
	}
	
	/**
	 * Sets this conversation's message preview
	 */
	public void setMessagePreview(@Nullable ConversationPreview preview) {
		this.messagePreview = preview;
	}
	
	/**
	 * Gets if this conversation has a draft message or draft files
	 */
	public boolean hasDraft() {
		return draftUpdateTime != -1;
	}
	
	/**
	 * Gets this conversation's draft message
	 */
	@Nullable
	public String getDraftMessage() {
		return draftMessage;
	}
	
	/**
	 * Sets this conversation's draft message
	 */
	public void setDraftMessage(@Nullable String draftMessage) {
		this.draftMessage = draftMessage;
	}
	
	/**
	 * Gets this conversation's list of draft files
	 */
	public List<FileDraft> getDraftFiles() {
		return draftFiles;
	}
	
	/**
	 * Gets the time this conversation's draft data was updated, or -1 if this conversation has no draft data
	 */
	public long getDraftUpdateTime() {
		return draftUpdateTime;
	}
	
	/**
	 * Sets the time this conversation's draft data was updated
	 */
	public void setDraftUpdateTime(long draftUpdateTime) {
		this.draftUpdateTime = draftUpdateTime;
	}
	
	/**
	 * Clears the draft message and draft files, and sets this conversation as not having a draft
	 */
	public void clearDrafts() {
		draftMessage = null;
		draftFiles.clear();
		draftUpdateTime = -1;
	}
	
	public ConversationTarget getConversationTarget() {
		if(serviceHandler == ServiceHandler.appleBridge) {
			if(state == ConversationState.incompleteClient) {
				return new ConversationTarget.AppleUnlinked(members.stream().map(MemberInfo::getAddress).toArray(String[]::new), serviceType);
			} else {
				return new ConversationTarget.AppleLinked(guid);
			}
		} else {
			return new ConversationTarget.SystemSMS(externalID);
		}
	}
	
	@NonNull
	@Override
	public ConversationInfo clone() {
		return new ConversationInfo(localID, guid, externalID, state, serviceHandler, serviceType, conversationColor, new ArrayList<>(members), title, unreadMessageCount, isArchived, isMuted, messagePreview, draftMessage, new ArrayList<>(draftFiles), draftUpdateTime);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(localID);
		out.writeString(guid);
		out.writeLong(externalID);
		out.writeInt(state);
		out.writeInt(serviceHandler);
		out.writeString(serviceType);
		out.writeInt(conversationColor);
		out.writeTypedList(members);
		out.writeString(title);
	}
	
	public static final Parcelable.Creator<ConversationInfo> CREATOR = new Parcelable.Creator<ConversationInfo>() {
		public ConversationInfo createFromParcel(Parcel in) {
			return new ConversationInfo(in);
		}
		
		public ConversationInfo[] newArray(int size) {
			return new ConversationInfo[size];
		}
	};
	
	private ConversationInfo(Parcel in) {
		localID = in.readLong();
		guid = in.readString();
		externalID = in.readLong();
		state = in.readInt();
		serviceHandler = in.readInt();
		serviceType = in.readString();
		conversationColor = in.readInt();
		members = new ArrayList<>();
		in.readTypedList(members, MemberInfo.CREATOR);
		title = in.readString();
		
		draftFiles = new ArrayList<>();
		draftUpdateTime = -1;
	}
}