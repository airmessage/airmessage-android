package me.tagavari.airmessage.messaging;

import android.content.Context;

import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.util.Constants;

public class ChatRenameActionInfo extends ConversationItem<ConversationUtils.ActionLineViewHolder> {
	//Creating the constants
	public static final int itemType = 2;
	public static final int itemViewType = viewTypeAction;
	
	//Creating the values
	final String agent;
	final String title;
	
	public ChatRenameActionInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, String agent, String title, long date) {
		//Calling the super constructor
		super(localID, serverID, guid, date, conversationInfo);
		
		//Setting the values
		this.agent = agent;
		this.title = title;
	}
	
	@Override
	public void bindView(ConversationUtils.ActionLineViewHolder viewHolder, Context context) {
		//Setting the message
		viewHolder.labelMessage.setText(getDirectSummary(context, agent, title));
		if(agent != null) getSummary(context, (wasTasked, result) -> {
			ConversationUtils.ActionLineViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
			if(newViewHolder != null) newViewHolder.labelMessage.setText(result);
		});
	}
	
	@Override
	public void getSummary(Context context, Constants.ResultCallback<String> callback) {
		//Returning the summary
		getSummary(context, agent, title, callback);
	}
	
	public static void getSummary(Context context, String agent, String title, Constants.ResultCallback<String> callback) {
		//Checking if the agent is invalid
		if(agent == null) {
			//Returning the message
			callback.onResult(false, getDirectSummary(context, null, title));
		} else {
			//Getting the data
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, agent, new UserCacheHelper.UserFetchResult() {
				@Override
				public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Getting the agent
					String namedAgent = userInfo == null ? agent : userInfo.getContactName();
					
					//Returning the message
					callback.onResult(wasTasked, getDirectSummary(context, namedAgent, title));
				}
			});
		}
	}
	
	public static String getDirectSummary(Context context, String agent, String title) {
		if(agent == null) {
			if(title == null) return context.getString(R.string.message_eventtype_chatrename_remove_you);
			else return context.getString(R.string.message_eventtype_chatrename_change_you, title);
		} else {
			if(title == null) return context.getString(R.string.message_eventtype_chatrename_remove, agent);
			else return context.getString(R.string.message_eventtype_chatrename_change, agent, title);
		}
	}
	
	@Override
	public int getItemType() {
		return itemType;
	}
	
	@Override
	public int getItemViewType() {
		return itemViewType;
	}
	
	public String getAgent() {
		return agent;
	}
	
	public String getTitle() {
		return title;
	}
	
	@Override
	public void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
		getSummary(context, (wasTasked, result) -> {
			callback.onResult(wasTasked, new LightConversationItem(result, getDate(), getLocalID(), getServerID()));
		});
	}
	
	@Override
	public LightConversationItem toLightConversationItemSync(Context context) {
		//Getting the summary
		String summary;
		
		//Checking if the agent is invalid
		if(agent == null) {
			//Returning the message
			summary = getDirectSummary(context, null, title);
		} else {
			//Getting the data
			UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, agent);
			
			//Getting the agent
			String namedAgent = userInfo == null ? agent : userInfo.getContactName();
			
			//Returning the message
			summary = getDirectSummary(context, namedAgent, title);
		}
		
		//Returning the light conversation item
		return new LightConversationItem(summary, getDate(), getLocalID(), getServerID());
	}
}
