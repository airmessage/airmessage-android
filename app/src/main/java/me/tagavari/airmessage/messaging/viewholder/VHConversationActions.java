package me.tagavari.airmessage.messaging.viewholder;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.messaging.AMConversationAction;

public class VHConversationActions extends RecyclerView.ViewHolder {
	private final HorizontalScrollView scrollView;
	private final LinearLayout container;
	
	public VHConversationActions(View itemView, HorizontalScrollView scrollView, LinearLayout container) {
		super(itemView);
		this.scrollView = scrollView;
		this.container = container;
	}
	
	/**
	 * Restores the default scroll position of this view
	 */
	public void resetScroll() {
		scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_RIGHT));
	}
	
	/**
	 * Binds this view to an array of conversation actions
	 * @param context The context to use
	 * @param actions The actions to bind
	 * @param colorAccent The color to use as the accent
	 * @param sendMessageCallback A callback used when a 'reply' action is selected
	 */
	public void setActions(Context context, AMConversationAction[] actions, int colorAccent, Consumer<String> sendMessageCallback) {
		//Gathering active views
		List<TextView> childViewList = new ArrayList<>(actions.length);
		for(int i = 0; i < container.getChildCount(); i++) {
			TextView childView = (TextView) container.getChildAt(i);
			if(i < actions.length) {
				childView.setVisibility(View.VISIBLE);
				childViewList.add(childView);
			} else childView.setVisibility(View.GONE);
		}
		
		//Adding more views if necessary
		while(childViewList.size() < actions.length) {
			TextView item = (TextView) LayoutInflater.from(context).inflate(R.layout.listitem_replysuggestions_item, container, false);
			item.setTextColor(colorAccent);
			container.addView(item);
			childViewList.add(item);
		}
		
		//Configuring the suggestion
		for(int i = 0; i < actions.length; i++) {
			TextView childView = childViewList.get(i);
			AMConversationAction conversationAction = actions[i];
			
			if(conversationAction.isReplyAction()) {
				childView.setCompoundDrawables(null, null, null, null);
				childView.setText(conversationAction.getReplyString());
				childView.setOnClickListener(view -> sendMessageCallback.accept(conversationAction.getReplyString().toString()));
			} else {
				//Getting the remote action
				AMConversationAction.RemoteAction remoteAction = conversationAction.getRemoteAction();
				
				//Configuring the remote action
				if(remoteAction.getIcon() != null) {
					Drawable icon = remoteAction.getIcon().loadDrawable(context);
					int iconSize = ResourceHelper.dpToPx(24);
					icon.setBounds(0, 0, iconSize, iconSize);
					childView.setCompoundDrawablesRelative(icon, null, null, null);
				} else {
					childView.setCompoundDrawables(null, null, null, null);
				}
				childView.setText(remoteAction.getTitle());
				childView.setOnClickListener(view -> {
					//Launching the intent
					try {
						remoteAction.getActionIntent().send();
					} catch(PendingIntent.CanceledException exception) {
						exception.printStackTrace();
					}
				});
			}
		}
	}
}