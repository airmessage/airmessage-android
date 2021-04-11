package me.tagavari.airmessage.messaging;

import android.content.Context;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.R;

/**
 * A message used to signify the creation of a new chat
 */
public class ChatCreateAction extends ConversationAction {
	public ChatCreateAction(long localID, long date) {
		super(localID, -1, null, date);
	}
	
	@Override
	@ConversationItemType
	public int getItemType() {
		return ConversationItemType.chatCreate;
	}
	
	@Override
	public String getMessageDirect(Context context) {
		return context.getResources().getString(R.string.message_conversationcreated);
	}
	
	@Override
	public boolean supportsBuildMessageAsync() {
		return false;
	}
	
	@NonNull
	@Override
	public ChatCreateAction clone() {
		return new ChatCreateAction(getLocalID(), getDate());
	}
}
