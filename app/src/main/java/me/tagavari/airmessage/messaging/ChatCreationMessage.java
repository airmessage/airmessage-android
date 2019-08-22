package me.tagavari.airmessage.messaging;

import android.content.Context;

import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.util.Constants;

public class ChatCreationMessage extends ConversationItem<ConversationUtils.ActionLineViewHolder> {
	//Creating the constants
	public static final int itemType = 3;
	public static final int itemViewType = viewTypeAction;
	
	public ChatCreationMessage(long localID, long date, ConversationInfo conversationInfo) {
		super(localID, -1, null, date, conversationInfo);
	}
	
	@Override
	public void bindView(ConversationUtils.ActionLineViewHolder viewHolder, Context context) {
		//Setting the message
		viewHolder.labelMessage.setText(getDirectSummary(context));
	}
	
	@Override
	public int getItemType() {
		return itemType;
	}
	
	@Override
	public int getItemViewType() {
		return itemViewType;
	}
	
	@Override
	public void getSummary(Context context, Constants.ResultCallback<String> callback) {
		callback.onResult(false, getDirectSummary(context));
	}
	
	private static String getDirectSummary(Context context) {
		//Returning the string
		return context.getResources().getString(R.string.message_conversationcreated);
	}
	
	@Override
	public void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
		callback.onResult(false, new LightConversationItem(getDirectSummary(context), getDate(), getLocalID(), getServerID()));
	}
	
	@Override
	public LightConversationItem toLightConversationItemSync(Context context) {
		return new LightConversationItem(getDirectSummary(context), getDate(), getLocalID(), getServerID());
	}
}
