package me.tagavari.airmessage;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.pascalwelsch.compositeandroid.activity.CompositeActivity;

import java.util.ArrayList;
import java.util.List;

public class ShareHandler extends CompositeActivity {
	//Creating the plugin values
	private ConversationsBase conversationsBasePlugin = null;
	
	//Creating the target values
	private String targetText = null;
	
	//Creating the listener values
	private final AdapterView.OnItemClickListener onListItemClickListener = (AdapterView<?> parent, View view, int position, long id) -> {
		//Creating the intent
		Intent launchMessaging = new Intent(ShareHandler.this, Messaging.class);
		
		//Setting the target conversation
		launchMessaging.putExtra(Constants.intentParamTargetID, ((ConversationManager.ConversationInfo) conversationsBasePlugin.listView.getItemAtPosition(position)).getLocalID());
		
		//Setting the fill text
		if(targetText != null) launchMessaging.putExtra(Constants.intentParamDataText, targetText);
		
		//Launching the activity
		startActivity(launchMessaging);
	};
	
	public ShareHandler() {
		//Setting the plugins
		conversationsBasePlugin = new ConversationsBase(() -> new ListAdapter(conversationsBasePlugin.conversations));
		addPlugin(conversationsBasePlugin);
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		//Getting the intent data
		String intentAction = getIntent().getAction();
		String intentType = getIntent().getType();
		
		//Checking if the intent type is invalid
		if(intentType == null) {
			//Finishing the activity
			finish();
			return;
		}
		
		//Checking if the intent is a single object
		if(Intent.ACTION_SEND.equals(intentAction)) {
			//Checking if the content type is text
			if("text/plain".equals(intentType)) {
				//Setting the target text
				targetText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			} else {
				//Finishing the activity
				finish();
				return;
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(intentAction)) {
			//Finishing the activity
			finish();
			return;
		} else {
			//Finishing the activity
			finish();
			return;
		}
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		
		//Setting the content
		setContentView(R.layout.activity_share);
		
		//Configuring the window
		DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
		getWindow().setLayout(Math.min(displayMetrics.widthPixels, getResources().getDimensionPixelSize(R.dimen.dialogwidth_max)), ViewGroup.LayoutParams.MATCH_PARENT);
		
		//Setting the plugin views
		conversationsBasePlugin.setViews(findViewById(R.id.list), findViewById(R.id.syncview_progress), findViewById(R.id.no_conversations));
		
		//Configuring the list
		conversationsBasePlugin.listView.setOnItemClickListener(onListItemClickListener);
		
		//Preventing the activity from finishing if the user touches outside of its bounds
		this.setFinishOnTouchOutside(false);
	}
	
	public void closeDialog(View view) {
		finish();
	}
	
	public void createNewConversation(View view) {
		//Launching the new message activity
		startActivity(new Intent(this, NewMessage.class));
	}
	
	private class ListAdapter extends ConversationsBase.ListAdapter {
		//Creating the list values
		private final List<ConversationManager.ConversationInfo> originalItems;
		private final List<ConversationManager.ConversationInfo> filteredItems = new ArrayList<>();
		
		ListAdapter(ArrayList<ConversationManager.ConversationInfo> items) {
			//Setting the original items
			originalItems = items;
			
			//Filtering the data
			filterAndUpdate();
		}
		
		@Override
		public int getCount() {
			return filteredItems.size();
		}
		
		@Override
		public ConversationManager.ConversationInfo getItem(int position) {
			return filteredItems.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Getting the view
			View view = convertView;
			
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = getItem(position);
			
			//Returning if the conversation info is invalid
			if(conversationInfo == null) return view;
			
			//Getting the view
			view = conversationInfo.createSimpleView(ShareHandler.this, convertView, parent, () -> conversationsBasePlugin.listView.getChildAt(filteredItems.indexOf(conversationInfo) - conversationsBasePlugin.listView.getFirstVisiblePosition()));
			
			//Returning the view
			return view;
		}
		
		@Override
		void filterAndUpdate() {
			//Clearing the filtered data
			filteredItems.clear();
			
			//Iterating over the original data
			for(ConversationManager.ConversationInfo conversationInfo : originalItems) {
				//Skipping archived conversations
				if(conversationInfo.isArchived()) continue;
				
				//Adding the item to the filtered data
				filteredItems.add(conversationInfo);
			}
			
			//Notifying the adapter
			notifyDataSetChanged();
		}
		
		@Override
		boolean isListEmpty() {
			return filteredItems.isEmpty();
		}
	}
}