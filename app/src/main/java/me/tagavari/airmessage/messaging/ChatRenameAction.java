package me.tagavari.airmessage.messaging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.rxjava3.core.Single;
import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.ContactHelper;

public class ChatRenameAction extends ConversationAction {
	//Creating the values
	final String agent;
	final String title;
	
	public ChatRenameAction(long localID, long serverID, String guid, long date, String agent, String title) {
		//Calling the super constructor
		super(localID, serverID, guid, date);
		
		//Setting the values
		this.agent = agent;
		this.title = title;
	}
	
	@Override
	public int getItemType() {
		return ConversationItemType.chatRename;
	}
	
	@Override
	public String getMessageDirect(Context context) {
		return buildMessage(context, agent, title);
	}
	
	@Override
	public boolean supportsBuildMessageAsync() {
		return true;
	}
	
	@Override
	public Single<String> buildMessageAsync(Context context) {
		return ContactHelper.getUserDisplayName(context, agent).map(userName -> buildMessage(context, userName.orElse(null), title));
	}
	
	/**
	 * Builds a summary message with the provided details
	 * @param context The context to use
	 * @param agent The name of the user who took this action, or NULL if the user is the local user
	 * @param title The title of the conversation, or NULL if the title is removed
	 * @return The message to display in the chat
	 */
	private static String buildMessage(@NonNull Context context, @Nullable String agent, @Nullable String title) {
		if(agent == null) {
			if(title == null) return context.getString(R.string.message_eventtype_chatrename_remove_you);
			else return context.getString(R.string.message_eventtype_chatrename_change_you, title);
		} else {
			if(title == null) return context.getString(R.string.message_eventtype_chatrename_remove, agent);
			else return context.getString(R.string.message_eventtype_chatrename_change, agent, title);
		}
	}
	
	public String getAgent() {
		return agent;
	}
	
	public String getTitle() {
		return title;
	}
	
	@NonNull
	@Override
	public ChatRenameAction clone() {
		return new ChatRenameAction(getLocalID(), getServerID(), getGuid(), getDate(), agent, title);
	}
}
