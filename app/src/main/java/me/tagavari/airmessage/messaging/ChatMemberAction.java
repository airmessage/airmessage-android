package me.tagavari.airmessage.messaging;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Objects;

import io.reactivex.rxjava3.core.Single;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.GroupAction;
import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.helper.ContactHelper;

public class ChatMemberAction extends ConversationAction {
	//Creating the values
	@GroupAction final int actionType;
	final String agent;
	final String other;
	
	public ChatMemberAction(long localID, long serverID, String guid, long date, @GroupAction int actionType, String agent, String other) {
		//Calling the super constructor
		super(localID, serverID, guid, date);
		
		//Setting the values
		this.actionType = actionType;
		this.agent = agent;
		this.other = other;
	}
	
	@Override
	public int getItemType() {
		return ConversationItemType.member;
	}
	
	@Override
	public String getMessageDirect(Context context) {
		return buildMessage(context, agent, other, actionType);
	}
	
	@Override
	public boolean supportsBuildMessageAsync() {
		return true;
	}
	
	@Override
	public Single<String> buildMessageAsync(Context context) {
		return Single.zip(ContactHelper.getUserDisplayName(context, agent), ContactHelper.getUserDisplayName(context, other), (agentName, otherName) -> buildMessage(context, agentName.orElse(null), otherName.orElse(null), actionType));
	}
	
	/**
	 * Builds a summary message with the provided details
	 * @param context The context to use
	 * @param agent The name of the user who took this action, or NULL if the user is the local user
	 * @param other The name of the user who was acted upon, or NULL if the user is the local user
	 * @param actionType The type of action that was taken
	 * @return The message to display in the chat
	 */
	public static String buildMessage(Context context, String agent, String other, int actionType) {
		//Returning the message based on the action type
		if(actionType == GroupAction.join) {
			if(Objects.equals(agent, other)) {
				if(agent == null) return context.getString(R.string.message_eventtype_join_you);
				else return context.getString(R.string.message_eventtype_join, agent);
			} else {
				if(agent == null) return context.getString(R.string.message_eventtype_invite_you_agent, other);
				else if(other == null) return context.getString(R.string.message_eventtype_invite_you_object, agent);
				else return context.getString(R.string.message_eventtype_invite, agent, other);
			}
		} else if(actionType == GroupAction.leave) {
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
	
	@GroupAction
	public int getActionType() {
		return actionType;
	}
	
	public String getAgent() {
		return agent;
	}
	
	public String getOther() {
		return other;
	}
	
	@NonNull
	@Override
	public ChatMemberAction clone() {
		return new ChatMemberAction(getLocalID(), getServerID(), getGuid(), getDate(), actionType, agent, other);
	}
}
