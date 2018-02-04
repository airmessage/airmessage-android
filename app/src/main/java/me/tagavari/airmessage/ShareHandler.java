package me.tagavari.airmessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShareHandler extends Activity {
	//Creating the view values
	private ListView listView;
	
	//Creating the target values
	private String targetText = null;
	
	//Creating the listener values
	private final AdapterView.OnItemClickListener onListItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			//Creating the intent
			Intent launchMessaging = new Intent(ShareHandler.this, Messaging.class);
			
			//Setting the target conversation
			launchMessaging.putExtra(Constants.intentParamTargetID, ((ConversationManager.ConversationInfo) listView.getItemAtPosition(position)).getLocalID());
			
			//Setting the fill text
			if(targetText != null) launchMessaging.putExtra(Constants.intentParamDataText, targetText);
			
			//Launching the activity
			startActivity(launchMessaging);
		}
	};
	private final BroadcastReceiver syncStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Setting the state
			if(intent.getBooleanExtra(Constants.intentParamAction, true)) setState(Conversations.stateSyncing);
			else setState(Conversations.stateReady);
		}
	};
	private final BroadcastReceiver updateConversationsBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateList();
		}
	};
	
	//Creating the other values
	private MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> conversations;
	private byte currentState = Conversations.stateIdle;
	private boolean conversationsExist = false;
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
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
		
		//Setting the content
		setContentView(R.layout.activity_share);
		getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		
		//Getting the views
		listView = findViewById(R.id.list);
		
		//Configuring the list view
		listView.setOnItemClickListener(onListItemClickListener);
		
		//Preventing the activity from finishing if the user touches outside of its bounds
		this.setFinishOnTouchOutside(false);
		
		//Checking if the messages haven't been loaded
		
	}
	
	@Override
	protected void onStart() {
		//Calling the super method
		super.onStart();
		
		//Adding the broadcast receivers
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(syncStateBroadcastReceiver, new IntentFilter(ConnectionService.localBCMassRetrieval));
		localBroadcastManager.registerReceiver(updateConversationsBroadcastReceiver, new IntentFilter(Conversations.localBCConversationUpdate));
		
		//Getting the conversations
		conversations = ConversationManager.getConversations();
		
		//Setting the conversations to an empty list if they are invalid
		if(conversations == null) {
			conversations = new MainApplication.LoadFlagArrayList<>(false);
			((MainApplication) getApplication()).setConversations(conversations);
		}
		
		//Getting the connection service
		ConnectionService connectionService = ConnectionService.getInstance();
		
		//Checking if a mass retrieval is in progress
		if(connectionService != null && connectionService.isMassRetrievalInProgress()) setState(Conversations.stateSyncing);
		//Otherwise checking if the conversations are loaded
		else if(conversations != null && conversations.isLoaded()) conversationLoadFinished(null);
		else {
			//Setting the state to loading
			setState(Conversations.stateLoading);
			
			//Loading the messages
			new LoadConversationsTask(this).execute();
		}
	}
	
	@Override
	protected void onResume() {
		//Calling the super method
		super.onResume();
		
		//Refreshing the list
		updateList();
	}
	
	@Override
	protected void onStop() {
		//Calling the super method
		super.onStop();
		
		//Unregistering the broadcast receivers
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.unregisterReceiver(syncStateBroadcastReceiver);
		localBroadcastManager.unregisterReceiver(updateConversationsBroadcastReceiver);
	}
	
	public void closeDialog(View view) {
		finish();
	}
	
	public void createNewConversation(View view) {
		//Launching the conversation manager
		startActivity(new Intent(this, NewMessage.class));
	}
	
	void conversationLoadFinished(ArrayList<ConversationManager.ConversationInfo> result) {
		//Replacing the conversations
		if(result != null) {
			conversations.clear();
			conversations.addAll(result);
			conversations.setLoaded(true);
		}
		
		//Setting the state
		setState(Conversations.stateReady);
		
		//Updating the list
		listView.setAdapter(new ListAdapter(this, R.layout.listitem_conversation_simple, conversations));
		updateList();
	}
	
	void conversationLoadFailed() {
		//Setting the state to failed
		setState(Conversations.stateLoadError);
	}
	
	private void setState(byte state) {
		//Returning if the current state matches the requested state
		if(currentState == state) return;
		
		//Disabling the old state
		switch(currentState) {
			case Conversations.stateLoading:
				findViewById(R.id.loading_text).setVisibility(View.GONE);
				break;
			case Conversations.stateSyncing: {
				View syncView = findViewById(R.id.syncview);
				syncView.setVisibility(View.GONE);
				syncView.findViewById(R.id.syncview_icon).setAnimation(null);
				break;
			}
			case Conversations.stateReady:
				findViewById(R.id.list).setVisibility(View.GONE);
				findViewById(R.id.no_conversations).setVisibility(View.GONE);
				break;
			case Conversations.stateLoadError:
				findViewById(R.id.errorview).setVisibility(View.GONE);
				break;
		}
		
		//Enabling the new state
		switch(state) {
			case Conversations.stateLoading:
				findViewById(R.id.loading_text).setVisibility(View.VISIBLE);
				break;
			case Conversations.stateSyncing: {
				View syncView = findViewById(R.id.syncview);
				syncView.setVisibility(View.VISIBLE);
				
				RotateAnimation rotateAnimation = new RotateAnimation(0, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				rotateAnimation.setInterpolator(new LinearInterpolator());
				rotateAnimation.setDuration(2500);
				rotateAnimation.setRepeatCount(Animation.INFINITE);
				syncView.findViewById(R.id.syncview_icon).setAnimation(rotateAnimation);
				
				break;
			}
			case Conversations.stateReady:
				findViewById(R.id.list).setVisibility(View.VISIBLE);
				if(conversations.isEmpty()) findViewById(R.id.no_conversations).setVisibility(View.VISIBLE);
				break;
			case Conversations.stateLoadError:
				findViewById(R.id.errorview).setVisibility(View.VISIBLE);
				break;
		}
		
		//Setting the new state
		currentState = state;
	}
	
	public void updateList() {
		//Returning if the conversations aren't ready
		if(conversations == null || !conversations.isLoaded()) return;
		
		//Updating the list
		//if(sort) Collections.sort(ConversationManager.getConversations(), ConversationManager.conversationComparator);
		ArrayAdapter arrayAdapter = (ArrayAdapter) listView.getAdapter();
		if(arrayAdapter != null) arrayAdapter.notifyDataSetChanged();
		
		//Returning if the state is not ready
		if(currentState != Conversations.stateReady) return;
		
		//Getting and checking if there are conversations
		boolean newConversationsExist = conversations.isEmpty();
		if(newConversationsExist != conversationsExist) {
			//Setting "no conversations" view state
			(findViewById(R.id.no_conversations)).animate().alpha(newConversationsExist ? 1 : 0).start();
			
			//Setting the new state
			conversationsExist = newConversationsExist;
		}
	}
	
	private class ListAdapter extends ArrayAdapter<ConversationManager.ConversationInfo> {
		//Creating the list values
		private List<ConversationManager.ConversationInfo> conversationList;
		
		ListAdapter(Context context, int resource, ArrayList<ConversationManager.ConversationInfo> items) {
			//Calling the super method
			super(context, resource, items);
			
			//Setting the data
			conversationList = items;
			//filteredData = (ArrayList<ConversationManager.ConversationInfo>) items.clone();
		}
		
		private void filterList() {
			//Cloning the list
			ArrayList<ConversationManager.ConversationInfo> clonedList = new ArrayList<>();
			clonedList.addAll(conversationList);
			conversationList = clonedList;
			
			//Iterating over the conversations
			Iterator<ConversationManager.ConversationInfo> iterator = conversationList.iterator();
			while(iterator.hasNext()) {
				//Filtering out archived conversations
				ConversationManager.ConversationInfo conversationInfo = iterator.next();
				if(conversationInfo.isArchived()) iterator.remove();
			}
		}
		
		@Override
		public void notifyDataSetChanged() {
			//Updating and filtering the list
			conversationList = conversations;
			filterList();
			
			//Calling the super method
			super.notifyDataSetChanged();
		}
		
		@Override
		@NonNull
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = getItem(position);
			
			//Returning if the conversation is invalid
			if(conversationInfo == null) return convertView;
			
			//Creating and returning the view
			return conversationInfo.createSimpleView(ShareHandler.this, convertView, parent, () -> listView.getChildAt(position - listView.getFirstVisiblePosition()));
			
			//Setting the view source
			//conversationInfo.setViewSource(() -> listView.getChildAt(position - listView.getFirstVisiblePosition()));
		}
	}
	
	private static class LoadConversationsTask extends AsyncTask<Void, Void, ArrayList<ConversationManager.ConversationInfo>> {
		private final WeakReference<ShareHandler> superclassReference;
		
		//Creating the values
		LoadConversationsTask(ShareHandler superclass) {
			//Setting the references
			superclassReference = new WeakReference<>(superclass);
		}
		
		@Override
		protected ArrayList<ConversationManager.ConversationInfo> doInBackground(Void... params) {
			//Getting the context
			Context context = superclassReference.get();
			if(context == null) return null;
			
			//Loading the conversations
			return DatabaseManager.fetchSummaryConversations(DatabaseManager.getReadableDatabase(context), context);
		}
		
		@Override
		protected void onPostExecute(ArrayList<ConversationManager.ConversationInfo> result) {
			//Checking if the result is a fail
			if(result == null) {
				//Telling the superclass
				ShareHandler superclass = superclassReference.get();
				if(superclass != null) superclass.conversationLoadFailed();
			} else {
				//Telling the superclass
				ShareHandler superclass = superclassReference.get();
				if(superclass != null) superclass.conversationLoadFinished(result);
			}
		}
	}
}