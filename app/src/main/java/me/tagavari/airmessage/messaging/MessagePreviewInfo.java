package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.view.View;

import me.tagavari.airmessage.util.ConversationUtils;

public abstract class MessagePreviewInfo<VH extends MessagePreviewInfo.ViewHolder> {
	public static final int typeLink = 0;
	
	public static final int stateNotTried = 0;
	public static final int stateUnavailable = 1;
	public static final int stateAvailable = 2;
	
	private final long messageID;
	private final byte[] data;
	private final String target;
	private final String title;
	private final String subtitle;
	private final String caption;
	
	public MessagePreviewInfo(long messageID, byte[] data, String target, String title, String subtitle, String caption) {
		this.messageID = messageID;
		this.data = data;
		this.target = target;
		this.title = title;
		this.subtitle = subtitle;
		this.caption = caption;
	}
	
	public static MessagePreviewInfo getMessagePreview(long messageID, int type, byte[] data, String target, String title, String subtitle, String caption) {
		switch(type) {
			case typeLink:
				return new MessagePreviewLink(messageID, data, target, title, subtitle, caption);
			default:
				throw new IllegalArgumentException("Unknown message preview info type: " + type);
		}
	}
	
	public abstract int getType();
	
	public byte[] getData() {
		return data;
	}
	
	public String getTarget() {
		return target;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getSubtitle() {
		return subtitle;
	}
	
	public String getCaption() {
		return caption;
	}
	
	public abstract void bind(VH viewHolder, Context context);
	
	public static abstract class ViewHolder {
		final View viewRoot;
		
		public ViewHolder(View view) {
			viewRoot = view;
		}
		
		public void setVisibility(boolean visible) {
			viewRoot.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
		
		public abstract void updateViewEdges(boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored);
	}
}
