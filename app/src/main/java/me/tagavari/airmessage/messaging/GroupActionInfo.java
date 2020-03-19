package me.tagavari.airmessage.messaging;

import android.content.Context;

import java.util.Objects;

import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.util.Constants;

public class GroupActionInfo extends ConversationItem<ConversationUtils.ActionLineViewHolder> {
	//Creating the constants
	public static final int itemType = 1;
	public static final int itemViewType = viewTypeAction;
	
	//Creating the values
	final int actionType; //0 - Invite / 1 - Leave
	final String agent;
	final String other;
	
	//Creating the other values
	public transient int color;
	
	public GroupActionInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, int actionType, String agent, String other, long date) {
		//Calling the super constructor
		super(localID, serverID, guid, date, conversationInfo);
		
		//Setting the values
		this.actionType = actionType;
		this.agent = agent;
		this.other = other;
	}
	
	@Override
	public void bindView(ConversationUtils.ActionLineViewHolder viewHolder, Context context) {
		//Setting the message
		viewHolder.labelMessage.setText(getDirectSummary(context, agent, other, actionType));
		if(agent != null || other != null) getSummary(context, (wasTasked, result) -> {
			ConversationUtils.ActionLineViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
			if(newViewHolder != null) newViewHolder.labelMessage.setText(result);
		});
	}
	
	@Override
	public void getSummary(Context context, Constants.ResultCallback<String> resultCallback) {
		//Calling the static method
		getSummary(context, agent, other, actionType, resultCallback);
	}
	
	public static void getSummary(Context context, final String agent, final String other, int actionType, Constants.ResultCallback<String> resultCallback) {
		//Creating the availability values
		Constants.ValueWrapper<String> agentWrapper = new Constants.ValueWrapper<>(null);
		Constants.ValueWrapper<Boolean> agentAvailableWrapper = new Constants.ValueWrapper<>(false);
		Constants.ValueWrapper<String> otherWrapper = new Constants.ValueWrapper<>(null);
		Constants.ValueWrapper<Boolean> otherAvailableWrapper = new Constants.ValueWrapper<>(false);
		
		//Setting the agent to null if the agent is invalid
		if(agent == null) {
			agentWrapper.value = null;
			agentAvailableWrapper.value = true;
		} else {
			//Setting the agent's name if there is user information
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, agent, new UserCacheHelper.UserFetchResult() {
				@Override
				public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Setting the user information
					agentWrapper.value = userInfo == null ? agent : userInfo.getContactName();
					agentAvailableWrapper.value = true;
					
					//Returning the result if both values are available
					if(otherAvailableWrapper.value) resultCallback.onResult(wasTasked, getDirectSummary(context, agentWrapper.value, otherWrapper.value, actionType));
				}
			});
		}
		
		//Setting the other to null if the other is invalid
		if(other == null) {
			otherWrapper.value = null;
			otherAvailableWrapper.value = true;
		} else {
			//Setting the agent's name if there is user information
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, other, new UserCacheHelper.UserFetchResult() {
				@Override
				public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Setting the user information
					otherWrapper.value = userInfo == null ? other : userInfo.getContactName();
					otherAvailableWrapper.value = true;
					
					//Returning the result if both values are available
					if(agentAvailableWrapper.value) resultCallback.onResult(wasTasked, getDirectSummary(context, agentWrapper.value, otherWrapper.value, actionType));
				}
			});
		}
		
		//Returning the result if both values are available
		//if(agentAvailableWrapper.value && otherAvailableWrapper.value) resultCallback.onResult(false, getDirectSummary(context, agentWrapper.value, otherWrapper.value, actionType));
	}
	
	public static String getDirectSummary(Context context, String agent, String other, int actionType) {
		//Returning the message based on the action type
		if(actionType == Constants.groupActionJoin) {
			if(Objects.equals(agent, other)) {
				if(agent == null) return context.getString(R.string.message_eventtype_join_you);
				else return context.getString(R.string.message_eventtype_join, agent);
			} else {
				if(agent == null) return context.getString(R.string.message_eventtype_invite_you_agent, other);
				else if(other == null) return context.getString(R.string.message_eventtype_invite_you_object, agent);
				else return context.getString(R.string.message_eventtype_invite, agent, other);
			}
		}
		else if(actionType == Constants.groupActionLeave) {
			if(Objects.equals(agent, other)) {
				if(agent == null) return context.getString(R.string.message_eventtype_leave_you);
				else return context.getString(R.string.message_eventtype_leave, agent);
			} else {
				if(agent == null) return context.getString(R.string.message_eventtype_kick_you_agent, other);
				else if(other == null) return context.getString(R.string.message_eventtype_kick_you_object, agent);
				else return context.getString(R.string.message_eventtype_kick, agent, other);
			}
		}
		
		//Returning an unknown message
		return context.getString(R.string.message_eventtype_unknown);
	}
	
	@Override
	public int getItemType() {
		return itemType;
	}
	
	@Override
	public int getItemViewType() {
		return itemViewType;
	}
	
	public int getActionType() {
		return actionType;
	}
	
	public String getAgent() {
		return agent;
	}
	
	public String getOther() {
		return other;
	}
	
	@Override
	public void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
		getSummary(context, (wasTasked, result) -> callback.onResult(wasTasked, new LightConversationItem(result, getDate(), getLocalID(), getServerID(), false)));
	}
	
	@Override
	public LightConversationItem toLightConversationItemSync(Context context) {
		//Getting the titled agent
		String titledAgent = agent;
		if(titledAgent != null) {
			UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, agent);
			if(userInfo == null) titledAgent = agent;
			else titledAgent = userInfo.getContactName();
		}
		
		//Getting the titled other
		String titledOther = other;
		if(titledOther != null) {
			UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, other);
			if(userInfo == null) titledOther = other;
			else titledOther = userInfo.getContactName();
		}
		
		//Returning the light conversation item
		return new LightConversationItem(getDirectSummary(context, titledAgent, titledOther, actionType), getDate(), getLocalID(), getServerID(), false);
	}
}
