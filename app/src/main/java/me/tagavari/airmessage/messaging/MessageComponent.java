package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.enums.MessagePreviewState;

public abstract class MessageComponent implements Parcelable {
	//Creating the data values
	private long localID;
	private String guid;
	
	//Creating the modifier values
	private final List<StickerInfo> stickers;
	private final List<TapbackInfo> tapbacks;
	
	//Creating the message preview values
	@MessagePreviewState private int messagePreviewState = MessagePreviewState.notTried;
	private long messagePreviewID = -1;
	
	public MessageComponent(long localID, String guid) {
		//Setting the values
		this.localID = localID;
		this.guid = guid;
		
		//Setting the modifiers to empty lists
		stickers = new ArrayList<>();
		tapbacks = new ArrayList<>();
	}
	
	public MessageComponent(long localID, String guid, List<StickerInfo> stickers, List<TapbackInfo> tapbacks) {
		//Setting the values
		this.localID = localID;
		this.guid = guid;
		
		//Setting the modifiers
		this.stickers = stickers;
		this.tapbacks = tapbacks;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public void setLocalID(long localID) {
		this.localID = localID;
	}
	
	public String getGUID() {
		return guid;
	}
	
	public void setGUID(String guid) {
		this.guid = guid;
	}
	
	public List<StickerInfo> getStickers() {
		return stickers;
	}
	
	public List<TapbackInfo> getTapbacks() {
		return tapbacks;
	}
	
	public void setMessagePreviewState(@MessagePreviewState int state) {
		messagePreviewState = state;
	}
	
	@MessagePreviewState
	public int getMessagePreviewState() {
		return messagePreviewState;
	}
	
	public void setMessagePreviewID(long id) {
		messagePreviewID = id;
	}
	
	public long getMessagePreviewID() {
		return messagePreviewID;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(localID);
		out.writeString(guid);
		
		out.writeTypedList(stickers);
		out.writeTypedList(tapbacks);
		
		out.writeInt(messagePreviewState);
		out.writeLong(messagePreviewID);
	}
	
	protected MessageComponent(Parcel in) {
		localID = in.readLong();
		guid = in.readString();
		
		stickers = new ArrayList<>();
		in.readTypedList(stickers, StickerInfo.CREATOR);
		tapbacks = new ArrayList<>();
		in.readTypedList(tapbacks, TapbackInfo.CREATOR);
		
		messagePreviewState = in.readInt();
		messagePreviewID = in.readLong();
	}
}
