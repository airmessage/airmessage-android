package me.tagavari.airmessage.messaging;

import android.content.Context;

import androidx.annotation.DrawableRes;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.common.SharedValues;

public class TapbackInfo {
	//Creating the reference values
	private static final int tapbackLove = 0;
	private static final int tapbackLike = 1;
	private static final int tapbackDislike = 2;
	private static final int tapbackLaugh = 3;
	private static final int tapbackEmphasis = 4;
	private static final int tapbackQuestion = 5;
	
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
	
	public static int convertToPrivateCode(int publicCode) {
		//Returning if the code is not in the 2000s
		if(publicCode < SharedValues.TapbackModifierInfo.tapbackBaseAdd || publicCode >= SharedValues.TapbackModifierInfo.tapbackBaseRemove) return -1;
		
		//Returning the associated version
		switch(publicCode) {
			case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackLove:
				return tapbackLove;
			case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackLike:
				return tapbackLike;
			case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackDislike:
				return tapbackDislike;
			case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackLaugh:
				return tapbackLaugh;
			case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackEmphasis:
				return tapbackEmphasis;
			case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackQuestion:
				return tapbackQuestion;
			default:
				return -1;
		}
	}
	
	public static TapbackDisplay getTapbackDisplay(int code, Context context) {
		switch(code) {
			case tapbackLove:
				return new TapbackDisplay(R.drawable.love_rounded, context.getResources().getColor(R.color.tapback_love, null));
			case tapbackLike:
				return new TapbackDisplay(R.drawable.like_rounded, context.getResources().getColor(R.color.tapback_like, null));
			case tapbackDislike:
				return new TapbackDisplay(R.drawable.dislike_rounded, context.getResources().getColor(R.color.tapback_dislike, null));
			case tapbackLaugh:
				return new TapbackDisplay(R.drawable.excited_rounded, context.getResources().getColor(R.color.tapback_laugh, null));
			case tapbackEmphasis:
				return new TapbackDisplay(R.drawable.exclamation_rounded, context.getResources().getColor(R.color.tapback_exclamation, null));
			case tapbackQuestion:
				return new TapbackDisplay(R.drawable.question_rounded, context.getResources().getColor(R.color.tapback_question, null));
			default:
				return null;
		}
	}
	
	public static class TapbackDisplay {
		@DrawableRes
		final int iconResource;
		final int color;
		
		private TapbackDisplay(int iconResource, int color) {
			this.iconResource = iconResource;
			this.color = color;
		}
	}
}
