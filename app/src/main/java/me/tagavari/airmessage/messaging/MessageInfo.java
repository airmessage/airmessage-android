package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ParcelCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.enums.MessageViewType;
import me.tagavari.airmessage.util.ProcessProgress;

public class MessageInfo extends ConversationItem implements Parcelable {
	//Creating the values
	@Nullable private final String sender;
	@Nullable private final MessageComponentText messageText;
	@NonNull private final ArrayList<AttachmentInfo> attachments;
	@Nullable private final String sendStyle;
	private boolean sendStyleViewed;
	private long dateRead;
	@MessageState private int messageState;
	@MessageSendErrorCode private int errorCode;
	private boolean errorDetailsAvailable;
	@Nullable private String errorDetails;
	
	public MessageInfo(long localID, long serverID, String guid, long date, @Nullable String sender, String messageText, String messageSubject, @NonNull ArrayList<AttachmentInfo> attachments, @Nullable String sendStyle, boolean sendStyleViewed, long dateRead, @MessageState int messageState, @MessageSendErrorCode int errorCode, boolean errorDetailsAvailable) {
		super(localID, serverID, guid, date);
		this.sender = sender;
		this.messageText = MessageComponentText.fromText(localID, guid, messageText, messageSubject);
		this.attachments = attachments;
		this.sendStyle = sendStyle;
		this.sendStyleViewed = sendStyleViewed;
		this.dateRead = dateRead;
		this.messageState = messageState;
		this.errorCode = errorCode;
		this.errorDetailsAvailable = errorDetailsAvailable;
	}
	
	public MessageInfo(long localID, long serverID, String guid, long date, @Nullable String sender, @Nullable MessageComponentText messageText, @NonNull ArrayList<AttachmentInfo> attachments, @Nullable String sendStyle, boolean sendStyleViewed, long dateRead, @MessageState int messageState, @MessageSendErrorCode int errorCode, boolean errorDetailsAvailable, @Nullable String errorDetails) {
		super(localID, serverID, guid, date);
		this.sender = sender;
		this.messageText = messageText;
		this.attachments = attachments;
		this.sendStyle = sendStyle;
		this.sendStyleViewed = sendStyleViewed;
		this.dateRead = dateRead;
		this.messageState = messageState;
		this.errorCode = errorCode;
		this.errorDetailsAvailable = errorDetailsAvailable;
		this.errorDetails = errorDetails;
	}
	
	/**
	 * Creates a new message in a default unsent state from the provided text
	 */
	public static MessageInfo blankFromText(String message) {
		return new MessageInfo(-1, -1, null, System.currentTimeMillis(), null, message, null, new ArrayList<>(), null, false, -1, MessageState.ghost, MessageSendErrorCode.none, false);
	}
	
	@Override
	public int getItemType() {
		return ConversationItemType.message;
	}
	
	@Override
	public int getItemViewType() {
		return MessageViewType.message;
	}
	
	@Override
	public void setLocalID(long value) {
		super.setLocalID(value);
		
		//Updating the local ID of the message text
		if(messageText != null) messageText.setLocalID(value);
	}
	
	@Nullable
	public String getSender() {
		return sender;
	}
	
	public boolean isOutgoing() {
		//Returning if the message is outgoing
		return sender == null;
	}
	
	@Nullable
	public String getMessageText() {
		return messageText == null ? null : messageText.getText();
	}
	
	@Nullable
	public String getMessageSubject() {
		return messageText == null ? null : messageText.getSubject();
	}
	
	@Nullable
	public MessageComponentText getMessageTextInfo() {
		return messageText;
	}
	
	@NonNull
	public List<AttachmentInfo> getAttachments() {
		return attachments;
	}
	
	/**
	 * Gets a list of this message's components (text and attachments)
	 */
	public List<MessageComponent> getComponents() {
		List<MessageComponent> list = new ArrayList<>();
		if(messageText != null) list.add(messageText);
		list.addAll(attachments);
		return list;
	}
	
	/**
	 * Gets the total number of components of this message
	 */
	public int getComponentCount() {
		if(messageText != null) return attachments.size() + 1;
		else return attachments.size();
	}
	
	/**
	 * Gets the component at the specified index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * ({@code index < 0 || index >= getComponentCount()})
	 */
	public MessageComponent getComponentAt(int index) {
		if(messageText != null) {
			if(index == 0) return messageText;
			else return attachments.get(index - 1);
		} else {
			return attachments.get(index);
		}
	}
	
	@Nullable
	public String getSendStyle() {
		return sendStyle;
	}
	
	public boolean isSendStyleViewed() {
		return sendStyleViewed;
	}
	
	public void setSendStyleViewed(boolean sendStyleViewed) {
		this.sendStyleViewed = sendStyleViewed;
	}
	
	@MessageState
	public int getMessageState() {
		return messageState;
	}
	
	public void setMessageState(@MessageState int messageState) {
		this.messageState = messageState;
	}
	
	@MessageSendErrorCode
	public int getErrorCode() {
		return errorCode;
	}
	
	public void setErrorCode(@MessageSendErrorCode int errorCode) {
		this.errorCode = errorCode;
	}
	
	public boolean isErrorDetailsAvailable() {
		return errorDetailsAvailable;
	}
	
	public void setErrorDetailsAvailable(boolean errorDetailsAvailable) {
		this.errorDetailsAvailable = errorDetailsAvailable;
	}
	
	@Nullable
	public String getErrorDetails() {
		return errorDetails;
	}
	
	public void setErrorDetails(@Nullable String errorDetails) {
		this.errorDetails = errorDetails;
	}
	
	public boolean hasError() {
		return getErrorCode() != MessageSendErrorCode.none;
	}
	
	public long getDateRead() {
		return dateRead;
	}
	
	public void setDateRead(long dateRead) {
		this.dateRead = dateRead;
	}
	
	@NonNull
	@Override
	public MessageInfo clone() {
		return new MessageInfo(getLocalID(), getServerID(), getGuid(), getDate(), sender, messageText, attachments, sendStyle, sendStyleViewed, dateRead, messageState, errorCode, errorDetailsAvailable, errorDetails);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		//Superclass
		out.writeLong(getLocalID());
		out.writeLong(getServerID());
		out.writeString(getGuid());
		out.writeLong(getDate());
		
		out.writeString(sender);
		out.writeParcelable(messageText, 0);
		out.writeTypedList(attachments);
		out.writeString(sendStyle);
		ParcelCompat.writeBoolean(out, sendStyleViewed);
		out.writeInt(messageState);
		out.writeLong(dateRead);
		out.writeInt(errorCode);
		ParcelCompat.writeBoolean(out, errorDetailsAvailable);
		out.writeString(errorDetails);
	}
	
	public static final Parcelable.Creator<MessageInfo> CREATOR = new Parcelable.Creator<MessageInfo>() {
		public MessageInfo createFromParcel(Parcel in) {
			return new MessageInfo(in);
		}
		
		public MessageInfo[] newArray(int size) {
			return new MessageInfo[size];
		}
	};
	
	private MessageInfo(Parcel in) {
		super(in.readLong(), in.readLong(), in.readString(), in.readLong());
		
		sender = in.readString();
		messageText = in.readParcelable(MessageComponentText.class.getClassLoader());
		attachments = new ArrayList<>();
		in.readTypedList(attachments, AttachmentInfo.CREATOR);
		sendStyle = in.readString();
		sendStyleViewed = ParcelCompat.readBoolean(in);
		messageState = in.readInt();
		dateRead = in.readLong();
		errorCode = in.readInt();
		errorDetailsAvailable = ParcelCompat.readBoolean(in);
		errorDetails = in.readString();
	}
}
