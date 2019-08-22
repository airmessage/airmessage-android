package me.tagavari.airmessage.connection.task;

import android.content.Context;

import com.crashlytics.android.Crashlytics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.common.SharedValues;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.util.ConversationUtils;

public class ModifierUpdateAsyncTask extends QueueTask<Void, Void> {
	//Creating the reference values
	private final WeakReference<Context> contextReference;
	
	//Creating the request values
	private final List<Blocks.ModifierInfo> structModifiers;
	private final List<StickerInfo> stickerModifiers = new ArrayList<>();
	private final List<TapbackInfo> tapbackModifiers = new ArrayList<>();
	private final List<TapbackRemovalStruct> tapbackRemovals = new ArrayList<>();
	
	private final ConnectionManager.Packager packager;
	
	public ModifierUpdateAsyncTask(Context context, List<Blocks.ModifierInfo> structModifiers, ConnectionManager.Packager packager) {
		contextReference = new WeakReference<>(context);
		
		this.structModifiers = structModifiers;
		
		this.packager = packager;
	}
	
	@Override
	public Void doInBackground() {
		//Getting the context
		Context context = contextReference.get();
		if(context == null) return null;
		
		//Iterating over the modifiers
		for(Blocks.ModifierInfo modifierInfo : structModifiers) {
			//Checking if the modifier is an activity status modifier
			if(modifierInfo instanceof Blocks.ActivityStatusModifierInfo) {
				//Casting to the activity status modifier
				Blocks.ActivityStatusModifierInfo activityStatusModifierInfo = (Blocks.ActivityStatusModifierInfo) modifierInfo;
				
				//Updating the modifier in the database
				DatabaseManager.getInstance().updateMessageState(activityStatusModifierInfo.message, activityStatusModifierInfo.state, activityStatusModifierInfo.dateRead);
			}
			//Otherwise checking if the modifier is a sticker update
			else if(modifierInfo instanceof Blocks.StickerModifierInfo) {
				//Updating the modifier in the database
				Blocks.StickerModifierInfo stickerInfo = (Blocks.StickerModifierInfo) modifierInfo;
				try {
					stickerInfo.image = packager.unpackageData(stickerInfo.image);
					StickerInfo sticker = DatabaseManager.getInstance().addMessageSticker(stickerInfo);
					if(sticker != null) stickerModifiers.add(sticker);
				} catch(OutOfMemoryError exception) {
					exception.printStackTrace();
					Crashlytics.logException(exception);
				}
			}
			//Otherwise checking if the modifier is a tapback update
			else if(modifierInfo instanceof Blocks.TapbackModifierInfo) {
				//Getting the tapback modifier
				Blocks.TapbackModifierInfo tapbackModifierInfo = (Blocks.TapbackModifierInfo) modifierInfo;
				
				//Checking if the tapback is negative
				if(tapbackModifierInfo.code >= SharedValues.TapbackModifierInfo.tapbackBaseRemove) {
					//Deleting the modifier in the database
					DatabaseManager.getInstance().removeMessageTapback(tapbackModifierInfo);
					tapbackRemovals.add(new TapbackRemovalStruct(tapbackModifierInfo.sender, tapbackModifierInfo.message, tapbackModifierInfo.messageIndex));
				} else {
					//Updating the modifier in the database
					TapbackInfo tapback = DatabaseManager.getInstance().addMessageTapback(tapbackModifierInfo);
					if(tapback != null) tapbackModifiers.add(tapback);
				}
			}
		}
		
		//Returning
		return null;
	}
	
	@Override
	public void onPostExecute(Void result) {
		//Getting the context
		Context context = contextReference.get();
		if(context == null) return;
		
		//Iterating over the modifier structs
		for(Blocks.ModifierInfo modifierInfo : structModifiers) {
			//Finding the referenced item
			ConversationItem conversationItem;
			MessageInfo messageInfo = null;
			for(ConversationInfo loadedConversation : ConversationUtils.getLoadedConversations()) {
				conversationItem = loadedConversation.findConversationItem(modifierInfo.message);
				if(conversationItem == null) continue;
				if(!(conversationItem instanceof MessageInfo)) break;
				messageInfo = (MessageInfo) conversationItem;
				break;
			}
			
			//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
			if(messageInfo == null) return;
			
			//Checking if the modifier is an activity status modifier
			if(modifierInfo instanceof Blocks.ActivityStatusModifierInfo) {
				//Getting the modifier
				Blocks.ActivityStatusModifierInfo activityStatusModifierInfo = (Blocks.ActivityStatusModifierInfo) modifierInfo;
				
				//Updating the message
				messageInfo.setMessageState(activityStatusModifierInfo.state);
				messageInfo.setDateRead(activityStatusModifierInfo.dateRead);
				
				//Getting the parent conversation
				ConversationInfo parentConversation = messageInfo.getConversationInfo();
				
				//Updating the activity state target
				parentConversation.tryActivityStateTarget(messageInfo, true, context);
				
				/* //Checking if the message is the activity state target
				if(parentConversation.getActivityStateTarget() == messageInfo) {
					//Updating the message's activity state display
					messageInfo.updateActivityStateDisplay(context);
				} else {
					//Comparing (and replacing) the conversation's activity state target
					parentConversation.tryActivityStateTarget(messageInfo, true, context);
				} */
			}
		}
		
		//Iterating over the sticker modifiers
		for(StickerInfo sticker : stickerModifiers) {
			//Finding the referenced item
			ConversationItem conversationItem;
			MessageInfo messageInfo = null;
			for(ConversationInfo loadedConversation : ConversationUtils.getLoadedConversations()) {
				conversationItem = loadedConversation.findConversationItem(sticker.getMessageID());
				if(conversationItem == null) continue;
				if(!(conversationItem instanceof MessageInfo)) break;
				messageInfo = (MessageInfo) conversationItem;
				break;
			}
			
			//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
			if(messageInfo == null) return;
			
			//Updating the message
			messageInfo.addLiveSticker(sticker, context);
		}
		
		//Iterating over the tapback modifiers
		for(TapbackInfo tapback : tapbackModifiers) {
			//Finding the referenced item
			MessageInfo messageInfo = null;
			for(ConversationInfo loadedConversation : ConversationUtils.getLoadedConversations()) {
				ConversationItem conversationItem;
				conversationItem = loadedConversation.findConversationItem(tapback.getMessageID());
				if(conversationItem == null) continue;
				if(!(conversationItem instanceof MessageInfo)) break;
				messageInfo = (MessageInfo) conversationItem;
				break;
			}
			
			//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
			if(messageInfo == null) return;
			
			//Updating the message
			messageInfo.addLiveTapback(tapback, context);
		}
		
		//Iterating over the removed tapbacks
		for(TapbackRemovalStruct tapback : tapbackRemovals) {
			//Finding the referenced item
			ConversationItem conversationItem;
			MessageInfo messageInfo = null;
			for(ConversationInfo loadedConversation : ConversationUtils.getLoadedConversations()) {
				conversationItem = loadedConversation.findConversationItem(tapback.message);
				if(conversationItem == null) continue;
				if(!(conversationItem instanceof MessageInfo)) break;
				messageInfo = (MessageInfo) conversationItem;
				break;
			}
			
			//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
			if(messageInfo == null) return;
			
			//Updating the message
			messageInfo.removeLiveTapback(tapback.sender, tapback.messageIndex, context);
		}
	}
	
	private static class TapbackRemovalStruct {
		final String sender;
		final String message;
		final int messageIndex;
		
		TapbackRemovalStruct(String sender, String message, int messageIndex) {
			this.sender = sender;
			this.message = message;
			this.messageIndex = messageIndex;
		}
	}
}
