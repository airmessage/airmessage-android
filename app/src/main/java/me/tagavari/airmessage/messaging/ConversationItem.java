package me.tagavari.airmessage.messaging;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.enums.MessageViewType;

public abstract class ConversationItem {
	//Creating the reference values
	public static final int viewTypeMessage = 0;
	public static final int viewTypeAction = 1;
	
	//Creating the conversation item values
	private long localID;
	private long serverID;
	private String guid;
	private long date;
	
	public ConversationItem(long localID, long serverID, String guid, long date) {
		//Setting the identifiers
		this.localID = localID;
		this.serverID = serverID;
		this.guid = guid;
		
		//Setting the date
		this.date = date;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public void setLocalID(long value) {
		localID = value;
	}
	
	public long getServerID() {
		return serverID;
	}
	
	public void setServerID(long value) {
		serverID = value;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public void setGuid(String value) {
		guid = value;
	}
	
	public long getDate() {
		return date;
	}
	
	public void setDate(long value) {
		date = value;
	}
	
	@ConversationItemType
	public abstract int getItemType();
	
	@MessageViewType
	public abstract int getItemViewType();
	
	@NonNull
	@Override
	public abstract ConversationItem clone();
}