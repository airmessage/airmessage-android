package me.tagavari.airmessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pascalwelsch.compositeandroid.activity.ActivityPlugin;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class ConversationsBase extends ActivityPlugin {
	//Creating the reference values
	//static final String localBCRemoveConversation = "LocalMSG-Conversations-RemoveConversation";
	//static final String localBCPurgeConversations = "LocalMSG-Conversations-PurgeConversations";
	//static final String localBCAttachmentFragmentFailed = "LocalMSG-Conversations-Attachment-Failed";
	//static final String localBCAttachmentFragmentConfirmed = "LocalMSG-Conversations-Attachment-Confirmed";
	//static final String localBCAttachmentFragmentData = "LocalMSG-Conversations-Attachment-Data";
	//static final String localBCUpdateConversationViews = "LocalMSG-Conversations-UpdateUserViews";
	static final String localBCConversationUpdate = "LocalMSG-Conversations-ConversationUpdate";
	
	//Creating the view values
	RecyclerView recyclerView;
	ProgressBar massRetrievalProgressBar;
	TextView noConversationsLabel;
	
	//Creating the state values
	static final byte stateIdle = 0;
	static final byte stateLoading = 1;
	static final byte stateSyncing = 2;
	static final byte stateReady = 3;
	static final byte stateLoadError = 4;
	byte currentState = stateIdle;
	boolean conversationsAvailable = false;
	
	//Creating the listener values
	private final BroadcastReceiver massRetrievalStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Getting the state
			switch(intent.getIntExtra(Constants.intentParamState, 0)) {
				case ConnectionService.intentExtraStateMassRetrievalStarted:
					//Setting the state to syncing
					setState(stateSyncing);
					
					break;
				case ConnectionService.intentExtraStateMassRetrievalProgress:
					//Checking if there is a maximum value provided
					if(intent.hasExtra(Constants.intentParamSize)) {
						//Setting the progress bar's maximum
						massRetrievalProgressBar.setMax(intent.getIntExtra(Constants.intentParamSize, 0));
						
						//Setting the progress bar as determinate
						massRetrievalProgressBar.setIndeterminate(false);
					}
					
					//Setting the progress bar's progress
					if(intent.hasExtra(Constants.intentParamProgress)) {
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) massRetrievalProgressBar.setProgress(intent.getIntExtra(Constants.intentParamProgress, 0), true);
						else massRetrievalProgressBar.setProgress(intent.getIntExtra(Constants.intentParamProgress, 0));
					}
					
					break;
				case ConnectionService.intentExtraStateMassRetrievalFailed:
					//Displaying a snackbar
					Snackbar.make(getActivity().findViewById(R.id.root), R.string.message_syncerror, Snackbar.LENGTH_LONG)
							.setAction(R.string.action_retry, view -> {
								//Getting the connection service
								ConnectionService service = ConnectionService.getInstance();
								if(service == null || service.getCurrentState() != ConnectionService.stateConnected) return;
								
								//Requesting another mass retrieval
								service.requestMassRetrieval(getActivity().getApplicationContext());
							})
							.show();
					
					//Advancing the conversation state
					advanceConversationState();
					
					break;
				case ConnectionService.intentExtraStateMassRetrievalFinished:
					//Filling the progress bar
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) massRetrievalProgressBar.setProgress(massRetrievalProgressBar.getMax(), true);
					else massRetrievalProgressBar.setProgress(massRetrievalProgressBar.getMax());
					
					//Advancing the conversation state
					advanceConversationState();
					
					break;
			}
		}
	};
	private final BroadcastReceiver updateConversationsBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateList(false);
		}
	};
	
	//Creating the receiver values
	private final List<Runnable> updateListListener = new ArrayList<>();
	private final List<Runnable> conversationsLoadedListener = new ArrayList<>();
	
	//Creating the timer values
	static final long timeUpdateHandlerDelay = 60 * 1000; //1 minute
	private Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
	private Runnable timeUpdateHandlerRunnable = new Runnable() {
		@Override
		public void run() {
			//Updating the time
			if(conversations != null) for(ConversationManager.ConversationInfo conversationInfo : conversations) conversationInfo.updateTime(getActivity());
			
			//Running again
			timeUpdateHandler.postDelayed(this, timeUpdateHandlerDelay);
		}
	};
	
	//Creating the other values
	MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> conversations;
	private RecyclerAdapterSource recyclerAdapterSource;
	
	ConversationsBase(RecyclerAdapterSource recyclerAdapterSource) {
		this.recyclerAdapterSource = recyclerAdapterSource;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onStart() {
		//Calling the super method
		super.onStart();
		
		//Adding the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
		localBroadcastManager.registerReceiver(massRetrievalStateBroadcastReceiver, new IntentFilter(ConnectionService.localBCMassRetrieval));
		localBroadcastManager.registerReceiver(updateConversationsBroadcastReceiver, new IntentFilter(localBCConversationUpdate));
		
		//Getting the conversations
		conversations = ConversationManager.getConversations();
		
		//Setting the conversations to an empty list if they are invalid
		if(conversations == null) {
			conversations = new MainApplication.LoadFlagArrayList<>(false);
			((MainApplication) getActivity().getApplication()).setConversations(conversations);
		}
		
		//Advancing the conversation state
		advanceConversationState();
		
		//Starting the time updater
		timeUpdateHandler.postDelayed(timeUpdateHandlerRunnable, timeUpdateHandlerDelay);
	}
	
	void setViews(RecyclerView recyclerView, ProgressBar massRetrievalProgressBar, TextView noConversationsLabel) {
		//Setting the views
		this.recyclerView = recyclerView;
		this.massRetrievalProgressBar = massRetrievalProgressBar;
		this.noConversationsLabel = noConversationsLabel;
	}
	
	private void advanceConversationState() {
		//Getting the connection service
		ConnectionService connectionService = ConnectionService.getInstance();
		
		//Checking if a mass retrieval is in progress
		if(connectionService != null && connectionService.isMassRetrievalInProgress()) setState(stateSyncing);
			//Otherwise checking if the conversations are loaded
		else if(conversations != null && conversations.isLoaded()) conversationLoadFinished(null);
		else {
			//Setting the state to loading
			setState(stateLoading);
			
			//Loading the messages
			new LoadConversationsTask(getActivity(), this).execute();
		}
	}
	
	private void setState(byte state) {
		//Disabling the old state
		if(currentState != state) {
			switch(currentState) {
				case stateLoading: {
					View loadingText = getActivity().findViewById(R.id.loading_text);
					loadingText.animate()
							.alpha(0)
							.withEndAction(() -> loadingText.setVisibility(View.GONE));
					break;
				}
				case stateSyncing: {
					View syncView = getActivity().findViewById(R.id.syncview);
					syncView.animate()
							.alpha(0)
							.withEndAction(() -> syncView.setVisibility(View.GONE));
					break;
				}
				case stateReady: {
					recyclerView.animate()
							.alpha(0)
							.withEndAction(() -> recyclerView.setVisibility(View.GONE));
					
					View noConversations = getActivity().findViewById(R.id.no_conversations);
					if(noConversations.getVisibility() == View.VISIBLE) noConversations.animate()
							.alpha(0)
							.withEndAction(() -> noConversations.setVisibility(View.GONE));
					break;
				}
				case stateLoadError: {
					View errorView = getActivity().findViewById(R.id.errorview);
					errorView.animate()
							.alpha(0)
							.withEndAction(() -> errorView.setVisibility(View.GONE));
					break;
				}
			}
		}
		
		//Setting the new state
		currentState = state;
		
		//Enabling the new state
		switch(state) {
			case stateLoading: {
				View loadingView = getActivity().findViewById(R.id.loading_text);
				loadingView.setAlpha(0);
				loadingView.setVisibility(View.VISIBLE);
				loadingView.animate().alpha(1);
				break;
			}
			case stateSyncing: {
				View syncView = getActivity().findViewById(R.id.syncview);
				syncView.setAlpha(0);
				syncView.setVisibility(View.VISIBLE);
				syncView.animate().alpha(1);
				
				int progress = ConnectionService.getInstance().getMassRetrievalProgress();
				
				if(progress == -1) {
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) massRetrievalProgressBar.setProgress(0, false);
					else massRetrievalProgressBar.setProgress(0);
					
					massRetrievalProgressBar.setIndeterminate(true);
				} else {
					massRetrievalProgressBar.setMax(ConnectionService.getInstance().getMassRetrievalProgressCount());
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) massRetrievalProgressBar.setProgress(progress, true);
					else massRetrievalProgressBar.setProgress(progress);
					
					massRetrievalProgressBar.setIndeterminate(false);
				}
				
				break;
			}
			case stateReady:
				recyclerView.setAlpha(0);
				recyclerView.setVisibility(View.VISIBLE);
				recyclerView.animate()
						.alpha(1);
				
				updateList(true);
				/* if(conversations.isEmpty()) {
					View noConversations = getActivity().findViewById(R.id.no_conversations);
					noConversations.animate()
							.alpha(1)
							.withStartAction(() -> noConversations.setVisibility(View.VISIBLE));
				} */
				break;
			case stateLoadError: {
				View errorView = getActivity().findViewById(R.id.errorview);
				errorView.setAlpha(0);
				errorView.setVisibility(View.VISIBLE);
				errorView.animate()
						.alpha(1);
				break;
			}
		}
	}
	
	void conversationLoadFinished(ArrayList<ConversationManager.ConversationInfo> result) {
		//Replacing the conversations
		if(result != null) {
			conversations.clear();
			conversations.addAll(result);
			conversations.setLoaded(true);
		}
		
		//Setting the list adapter
		recyclerView.setAdapter(recyclerAdapterSource.get());
		
		//Setting the state
		setState(stateReady);
		
		//Calling the listeners
		for(Runnable listener : conversationsLoadedListener) listener.run();
		
		//Updating the views
		//for(ConversationManager.ConversationInfo conversationInfo : ConversationManager.getConversations()) conversationInfo.updateView(Conversations.this);
	}
	
	void conversationLoadFailed() {
		//Setting the state to failed
		setState(stateLoadError);
	}
	
	static abstract class RecyclerAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
		abstract void filterAndUpdate();
		abstract boolean isListEmpty();
	}
	
	/* private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the list values
		private final List<ConversationManager.ConversationInfo> originalItems;
		private final List<ConversationManager.ConversationInfo> filteredItems = new ArrayList<>();
		
		//Creating the recycler values
		private RecyclerView recyclerView;
		
		RecyclerAdapter(ArrayList<ConversationManager.ConversationInfo> items, RecyclerView recyclerView) {
			//Setting the original items
			originalItems = items;
			
			//Setting the recycler view
			this.recyclerView = recyclerView;
			
			//Filtering the data
			filterAndUpdate();
		}
		
		class ViewHolder extends RecyclerView.ViewHolder {
			ViewHolder(View itemView) {
				super(itemView);
			}
		}
		
		class ItemViewHolder extends RecyclerView.ViewHolder {
			//Creating the view values
			private final TextView contactName;
			private final TextView contactAddress;
			
			private final View header;
			private final TextView headerLabel;
			
			private final ImageView profileDefault;
			private final ImageView profileImage;
			
			private final View contentArea;
			
			private ItemViewHolder(View view) {
				//Calling the super method
				super(view);
				
				//Getting the views
				contactName = view.getActivity().findViewById(R.id.label_name);
				contactAddress = view.getActivity().findViewById(R.id.label_address);
				
				header = view.getActivity().findViewById(R.id.header);
				headerLabel = view.getActivity().findViewById(R.id.header_label);
				
				profileDefault = view.getActivity().findViewById(R.id.profile_default);
				profileImage = view.getActivity().findViewById(R.id.profile_image);
				
				contentArea = view.getActivity().findViewById(R.id.area_content);
			}
		}
		
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Returning the view holder
			return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_conversation, parent, false));
		}
		
		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = filteredItems.get(position);
			
			//Setting the view's click listener
			viewHolder.itemView.setOnClickListener(view -> {
				//Creating the intent
				Intent launchMessaging = new Intent(Conversations.this, Messaging.class);
				
				//Setting the extra
				launchMessaging.putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID());
				
				//Launching the intent
				startActivity(launchMessaging);
			});
			
			//Setting the view source
			LinearLayoutManager layout = (LinearLayoutManager) recyclerView.getLayoutManager();
			conversationInfo.setViewSource(() -> layout.findViewByPosition(filteredItems.indexOf(conversationInfo)));
		}
		
		@Override
		public int getItemCount() {
			return filteredItems.size();
		}
		
		void filterAndUpdate() {
			//Clearing the filtered data
			filteredItems.clear();
			
			//Iterating over the original data
			for(ConversationManager.ConversationInfo conversationInfo : originalItems) {
				//Skipping non-listed conversations
				if(conversationInfo.isArchived() != listingArchived) continue;
				
				//Adding the item to the filtered data
				filteredItems.add(conversationInfo);
			}
			
			//Notifying the adapter
			notifyDataSetChanged();
		}
	} */
	
	@Override
	public void onResume() {
		//Calling the super method
		super.onResume();
		
		//Refreshing the list
		updateList(false);
	}
	
	@Override
	public void onStop() {
		//Calling the super method
		super.onStop();
		
		//Removing the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
		localBroadcastManager.unregisterReceiver(massRetrievalStateBroadcastReceiver);
		localBroadcastManager.unregisterReceiver(updateConversationsBroadcastReceiver);
		
		//Stopping the time updater
		timeUpdateHandler.removeCallbacks(timeUpdateHandlerRunnable);
	}
	
	void updateList(boolean forceUpdate) {
		//Returning if the conversations aren't ready
		if(conversations == null || !conversations.isLoaded()) return;
		
		//Updating the list
		//if(sort) Collections.sort(ConversationManager.getConversations(), ConversationManager.conversationComparator);
		RecyclerAdapter<?> recyclerAdapter = (RecyclerAdapter<?>) recyclerView.getAdapter();
		if(recyclerAdapter == null) return;
		recyclerAdapter.filterAndUpdate();
		
		//Returning if the state is not ready
		if(currentState != stateReady) return;
		
		//Getting and checking if there are conversations
		boolean newConversationsAvailable = !recyclerAdapter.isListEmpty();
		if(forceUpdate || newConversationsAvailable != conversationsAvailable) {
			//Checking if there are conversations to display
			if(newConversationsAvailable) {
				//Hiding the label
				noConversationsLabel.animate().alpha(0).withEndAction(() -> noConversationsLabel.setVisibility(View.GONE)).start();
			} else {
				//Showing the label
				noConversationsLabel.animate().alpha(1).withStartAction(() -> {
					noConversationsLabel.setAlpha(0);
					noConversationsLabel.setVisibility(View.VISIBLE);
				}).start();
			}
			
			//Setting the new state
			conversationsAvailable = newConversationsAvailable;
		}
		
		//Calling the listeners
		for(Runnable listener : updateListListener) listener.run();
	}
	
	void addUpdateListListener(Runnable listener) {
		updateListListener.add(listener);
	}
	
	void addConversationsLoadedListener(Runnable listener) {
		conversationsLoadedListener.add(listener);
	}
	
	//TODO implement in ViewModel
	private static class LoadConversationsTask extends AsyncTask<Void, Void, MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo>> {
		private final WeakReference<Context> contextReference;
		private final WeakReference<ConversationsBase> superclassReference;
		
		//Creating the values
		LoadConversationsTask(Context context, ConversationsBase superclass) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			superclassReference = new WeakReference<>(superclass);
		}
		
		@Override
		protected MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> doInBackground(Void... params) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Loading the conversations
			return DatabaseManager.getInstance().fetchSummaryConversations(context);
		}
		
		@Override
		protected void onPostExecute(MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> result) {
			//Checking if the result is a fail
			if(result == null) {
				//Telling the superclass
				ConversationsBase superclass = superclassReference.get();
				if(superclass != null) superclass.conversationLoadFailed();
			} else {
				//Telling the superclass
				ConversationsBase superclass = superclassReference.get();
				if(superclass != null) {
					superclass.conversationLoadFinished(result);
				}
			}
		}
	}
	
	static class DeleteAttachmentsTask extends AsyncTask<Void, Void, Void> {
		//Creating the values
		private final WeakReference<Context> contextReference;
		
		DeleteAttachmentsTask(Context context) {
			//Setting the references
			contextReference = new WeakReference<>(context);
		}
		
		@Override
		protected Void doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Clearing the attachments directory
			MainApplication.clearAttachmentsDirectory(context);
			
			//Returning
			return null;
		}
	}
	
	static class DeleteMessagesTask extends AsyncTask<Void, Void, Void> {
		//Creating the values
		private final WeakReference<Context> contextReference;
		
		DeleteMessagesTask(Context context) {
			//Setting the references
			contextReference = new WeakReference<>(context);
		}
		
		@Override
		protected void onPreExecute() {
			//Clearing all conversations
			ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null) conversations.clear();
			
			//Updating the conversation activity list
			Context context = contextReference.get();
			if(context != null) LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCConversationUpdate));
		}
		
		@Override
		protected Void doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Removing the messages from the database
			DatabaseManager.getInstance().deleteEverything();
			
			//Clearing the attachments directory
			MainApplication.clearAttachmentsDirectory(context);
			
			//Returning
			return null;
		}
	}
	
	static class SyncMessagesTask extends AsyncTask<Void, Void, Void> {
		//Creating the values
		private final WeakReference<Context> contextReference;
		private final WeakReference<View> snackbarParentReference;
		
		SyncMessagesTask(Context context, View snackbarParent) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			snackbarParentReference = new WeakReference<>(snackbarParent);
		}
		
		@Override
		protected void onPreExecute() {
			//Clearing the conversations in memory
			ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null) conversations.clear();
			
			//Updating the conversation activity list
			Context context = contextReference.get();
			if(context != null) LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCConversationUpdate));
			/* for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
				callbacks.updateList(false); */
		}
		
		@Override
		protected Void doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Removing the messages from the database
			DatabaseManager.getInstance().deleteEverything();
			
			//Clearing the attachments directory
			MainApplication.clearAttachmentsDirectory(context);
			
			//Returning
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return;
			
			//Getting the snackbar parent view
			View parentView = snackbarParentReference.get();
			
			//Syncing the messages
			ConnectionService connectionService = ConnectionService.getInstance();
			boolean messageResult = connectionService != null && connectionService.requestMassRetrieval(context);
			if(!messageResult) {
				//Displaying a snackbar
				if(parentView != null) Snackbar.make(parentView, R.string.message_serverstatus_noconnection, Snackbar.LENGTH_LONG).show();
				return;
			}
			
			//Showing a snackbar
			if(parentView != null) Snackbar.make(parentView, R.string.message_confirm_resyncmessages_started, Snackbar.LENGTH_LONG).show();
		}
	}
	
	interface RecyclerAdapterSource {
		RecyclerAdapter<?> get();
	}
}