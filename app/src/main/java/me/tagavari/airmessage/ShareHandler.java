package me.tagavari.airmessage;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;

public class ShareHandler extends AppCompatCompositeActivity {
	//Creating the plugin values
	private ConversationsBase conversationsBasePlugin;
	
	//Creating the target values
	private String targetText = null;
	private Uri[] targetUris = null;
	
	public ShareHandler() {
		//Setting the plugins
		conversationsBasePlugin = new ConversationsBase(() -> new RecyclerAdapter(conversationsBasePlugin.conversations));
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
		
		boolean dataValid = false;
		
		//Checking if the intent is a single object
		if(Intent.ACTION_SEND.equals(intentAction)) {
			//Checking if the content type is text
			if("text/plain".equals(intentType)) {
				//Setting the target text
				targetText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
				dataValid = targetText != null;
			} else {
				//Getting the target URI
				Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
				if(dataValid = uri != null) targetUris = new Uri[]{uri};
			}
		} else if(Intent.ACTION_SEND_MULTIPLE.equals(intentAction)) {
			//Getting the target URI array
			ArrayList<Parcelable> uriList = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if(dataValid = uriList != null && !uriList.isEmpty()) targetUris = uriList.toArray(new Uri[0]);
		}
		
		if(!dataValid) {
			//Finishing the activity
			finish();
			return;
		}
		
		//Setting the window flags
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		
		//Setting the content
		setContentView(R.layout.activity_share);
		
		//Configuring the window
		DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
		getWindow().setLayout(Math.min(displayMetrics.widthPixels, getResources().getDimensionPixelSize(R.dimen.dialogwidth_max)), ViewGroup.LayoutParams.MATCH_PARENT);
		
		//Setting the plugin views
		conversationsBasePlugin.setViews(findViewById(R.id.list), findViewById(R.id.syncview_progress), findViewById(R.id.no_conversations));
		
		//Preventing the activity from finishing if the user touches outside of its bounds
		this.setFinishOnTouchOutside(false);
		
		//Checking if the server is not configured
		if(!((MainApplication) getApplication()).isServerConfigured()) {
			Button button = findViewById(R.id.button_new);
			button.setEnabled(false);
			button.setAlpha(Constants.resolveFloatAttr(this, android.R.attr.disabledAlpha));
		}
	}
	
	public void closeDialog(View view) {
		finish();
	}
	
	public void createNewConversation(View view) {
		startActivity(new Intent(this, NewMessage.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
	}
	
	private class RecyclerAdapter extends ConversationsBase.RecyclerAdapter<ConversationManager.ConversationInfo.SimpleItemViewHolder> {
		//Creating the list values
		private final List<ConversationManager.ConversationInfo> originalItems;
		private final List<ConversationManager.ConversationInfo> filteredItems = new ArrayList<>();
		
		//Creating the recycler values
		private RecyclerView recyclerView;
		
		RecyclerAdapter(ArrayList<ConversationManager.ConversationInfo> items) {
			//Setting the original items
			originalItems = items;
			
			//Enabling stable IDs
			setHasStableIds(true);
			
			//Filtering the data
			filterAndUpdate();
		}
		
		@Override
		public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
			this.recyclerView = recyclerView;
		}
		
		@Override
		public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
			this.recyclerView = null;
		}
		
		@NonNull
		@Override
		public ConversationManager.ConversationInfo.SimpleItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			//Returning the view holder
			return new ConversationManager.ConversationInfo.SimpleItemViewHolder(LayoutInflater.from(ShareHandler.this).inflate(R.layout.listitem_conversation_simple, parent, false));
		}
		
		@Override
		public void onBindViewHolder(@NonNull ConversationManager.ConversationInfo.SimpleItemViewHolder holder, int position) {
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = filteredItems.get(position);
			
			//Creating the view
			conversationInfo.bindSimpleView(ShareHandler.this, holder, new Constants.ViewHolderSourceImpl<>(recyclerView, conversationInfo.getLocalID()));
			
			//Setting the view's click listeners
			holder.itemView.setOnClickListener(view -> {
				//Creating the intent
				Intent launchMessaging = new Intent(ShareHandler.this, Messaging.class);
				launchMessaging.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				
				//Setting the target conversation
				launchMessaging.putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID());
				
				//Setting the fill data
				if(targetText != null) launchMessaging.putExtra(Constants.intentParamDataText, targetText);
				if(targetUris != null) launchMessaging.putExtra(Constants.intentParamDataFile, targetUris);
				
				//Launching the activity
				startActivity(launchMessaging);
			});
		}
		
		@Override
		public int getItemCount() {
			return filteredItems.size();
		}
		
		@Override
		public long getItemId(int position) {
			return filteredItems.get(position).getLocalID();
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