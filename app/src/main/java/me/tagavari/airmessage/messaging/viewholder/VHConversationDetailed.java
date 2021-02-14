package me.tagavari.airmessage.messaging.viewholder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * A view holder that contains a complete conversation listing
 */
public class VHConversationDetailed extends VHConversationBase {
	//A selection indicator, used in multi-select mode
	public final View selectionIndicator;
	public final View selectionHighlight;
	
	//The latest message and its time
	public final TextView labelMessage;
	public final TextView labelStatus;
	
	//A counter for unread messages
	public final TextView labelUnread;
	
	//Icon indicators for muted and draft status
	public final View flagMuted;
	public final View flagDraft;
	
	public VHConversationDetailed(@NonNull View itemView, @NonNull ViewGroup iconGroup, @NonNull TextView conversationTitle, @NonNull View selectionIndicator, @NonNull View selectionHighlight, @NonNull TextView labelMessage, @NonNull TextView labelStatus, @NonNull TextView labelUnread, @NonNull View flagMuted, @NonNull View flagDraft) {
		super(itemView, iconGroup, conversationTitle);
		this.selectionIndicator = selectionIndicator;
		this.selectionHighlight = selectionHighlight;
		this.labelMessage = labelMessage;
		this.labelStatus = labelStatus;
		this.labelUnread = labelUnread;
		this.flagMuted = flagMuted;
		this.flagDraft = flagDraft;
	}
}