package me.tagavari.airmessage.messaging;

import android.content.Context;

import me.tagavari.airmessage.util.Constants;

public abstract class ConversationItem<VH> {
	//Creating the reference values
	public static final int viewTypeMessage = 0;
	public static final int viewTypeAction = 1;
	
	//Creating the conversation item values
	private long localID;
	private long serverID;
	private String guid;
	private long date;
	private ConversationInfo conversationInfo;
	//private Constants.ViewSource viewSource;
	private Constants.ViewHolderSource<VH> viewHolderSource;
	
	public ConversationItem(long localID, long serverID, String guid, long date, ConversationInfo conversationInfo) {
		//Setting the identifiers
		this.localID = localID;
		this.serverID = serverID;
		this.guid = guid;
		
		//Setting the date
		this.date = date;
		
		//Setting the conversation info
		this.conversationInfo = conversationInfo;
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
	
	public void setViewHolderSource(Constants.ViewHolderSource<VH> viewHolderSource) {
		this.viewHolderSource = viewHolderSource;
	}
	
	public VH getViewHolder() {
		if(viewHolderSource == null) return null;
		return viewHolderSource.get();
	}
	
	public ConversationInfo getConversationInfo() {
		return conversationInfo;
	}
	
	public void setConversationInfo(ConversationInfo conversationInfo) {
		this.conversationInfo = conversationInfo;
	}
	
	public abstract void bindView(VH viewHolder, Context context);
	
	public void updateViewColor(Context context) {}
	
	public abstract void getSummary(Context context, Constants.ResultCallback<String> resultCallback);
	
	public abstract int getItemType();
	
	public abstract int getItemViewType();
	
	public abstract void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback);
	
	public abstract LightConversationItem toLightConversationItemSync(Context context);
}