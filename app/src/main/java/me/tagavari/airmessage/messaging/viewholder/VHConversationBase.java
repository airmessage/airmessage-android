package me.tagavari.airmessage.messaging.viewholder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.util.DisposableViewHolder;

/**
 * A view holder that contains a conversation title and an icon
 */
public class VHConversationBase extends DisposableViewHolder {
	//Creating the view values
	public final ViewGroup iconGroup;
	public final TextView conversationTitle;
	
	public VHConversationBase(@NonNull View itemView, @NonNull ViewGroup iconGroup, @NonNull TextView conversationTitle) {
		super(itemView);
		this.iconGroup = iconGroup;
		this.conversationTitle = conversationTitle;
	}
}