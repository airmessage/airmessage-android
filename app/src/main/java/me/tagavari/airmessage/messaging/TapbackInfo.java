package me.tagavari.airmessage.messaging;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.util.Consumer;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.data.UserCacheHelper;

public class TapbackInfo {
	//Creating the reference values
	public static final int tapbackHeart = 0;
	public static final int tapbackLike = 1;
	public static final int tapbackDislike = 2;
	public static final int tapbackLaugh = 3;
	public static final int tapbackExclamation = 4;
	public static final int tapbackQuestion = 5;
	
	//Creating the tapback values
	private long localID;
	private long messageID;
	private int messageIndex;
	private String sender;
	private int code;
	
	public TapbackInfo(long localID, long messageID, int messageIndex, String sender, int code) {
		this.localID = localID;
		this.messageID = messageID;
		this.messageIndex = messageIndex;
		this.sender = sender;
		this.code = code;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public long getMessageID() {
		return messageID;
	}
	
	public void setMessageID(long value) {
		messageID = value;
	}
	
	public int getMessageIndex() {
		return messageIndex;
	}
	
	public String getSender() {
		return sender;
	}
	
	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	public static TapbackDisplay getTapbackDisplay(int code, Context context) {
		switch(code) {
			case tapbackHeart:
				return new TapbackDisplay(R.drawable.love_rounded, context.getResources().getColor(R.color.tapback_love, null), R.string.part_tapback_heart);
			case tapbackLike:
				return new TapbackDisplay(R.drawable.like_rounded, context.getResources().getColor(R.color.tapback_like, null), R.string.part_tapback_like);
			case tapbackDislike:
				return new TapbackDisplay(R.drawable.dislike_rounded, context.getResources().getColor(R.color.tapback_dislike, null), R.string.part_tapback_dislike);
			case tapbackLaugh:
				return new TapbackDisplay(R.drawable.excited_rounded, context.getResources().getColor(R.color.tapback_laugh, null), R.string.part_tapback_laugh);
			case tapbackExclamation:
				return new TapbackDisplay(R.drawable.exclamation_rounded, context.getResources().getColor(R.color.tapback_exclamation, null), R.string.part_tapback_exclamation);
			case tapbackQuestion:
				return new TapbackDisplay(R.drawable.question_rounded, context.getResources().getColor(R.color.tapback_question, null), R.string.part_tapback_question);
			default:
				return null;
		}
	}
	
	public void getSummary(Context context, String messageText, Consumer<String> resultListener) {
		@StringRes
		int stringID;
		
		//Checking if there is no sender
		if(sender == null) {
			//Getting the string ID to use
			switch(code) {
				case tapbackHeart:
					stringID = R.string.message_tapback_add_heart_you;
					break;
				case tapbackLike:
					stringID = R.string.message_tapback_add_like_you;
					break;
				case tapbackDislike:
					stringID = R.string.message_tapback_add_dislike_you;
					break;
				case tapbackLaugh:
					stringID = R.string.message_tapback_add_laugh_you;
					break;
				case tapbackExclamation:
					stringID = R.string.message_tapback_add_exclamation_you;
					break;
				case tapbackQuestion:
					stringID = R.string.message_tapback_add_question_you;
					break;
				default:
					throw new IllegalStateException("Unknown tapback code: " + code);
			}
			
			//Returning the string immediately
			resultListener.accept(context.getResources().getString(stringID, messageText));
		} else {
			//Getting the string ID to use
			switch(code) {
				case tapbackHeart:
					stringID = R.string.message_tapback_add_heart;
					break;
				case tapbackLike:
					stringID = R.string.message_tapback_add_like;
					break;
				case tapbackDislike:
					stringID = R.string.message_tapback_add_dislike;
					break;
				case tapbackLaugh:
					stringID = R.string.message_tapback_add_laugh;
					break;
				case tapbackExclamation:
					stringID = R.string.message_tapback_add_exclamation;
					break;
				case tapbackQuestion:
					stringID = R.string.message_tapback_add_question;
					break;
				default:
					throw new IllegalStateException("Unknown tapback code: " + code);
			}
			
			//Getting the sender's name
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, sender, new UserCacheHelper.UserFetchResult() {
				@Override
				public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Getting the sender name
					String senderName = userInfo == null ? sender : userInfo.getContactName();
					
					//Returning the result
					resultListener.accept(context.getResources().getString(stringID, senderName, messageText));
				}
			});
		}
	}
	
	public static class TapbackDisplay {
		@DrawableRes
		final int iconResource;
		final int color;
		@StringRes
		final int label;
		
		private TapbackDisplay(int iconResource, int color, @StringRes int label) {
			this.iconResource = iconResource;
			this.color = color;
			this.label = label;
		}
	}
}
