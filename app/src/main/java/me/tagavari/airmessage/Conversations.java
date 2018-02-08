package me.tagavari.airmessage;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.common.SharedValues;

//TODO implement launcher shortcuts
//TODO implement Google Assistant integration
public class Conversations extends AppCompatActivity {
	//Creating the reference values
	//static final String localBCRemoveConversation = "LocalMSG-Conversations-RemoveConversation";
	//static final String localBCPurgeConversations = "LocalMSG-Conversations-PurgeConversations";
	//static final String localBCAttachmentFragmentFailed = "LocalMSG-Conversations-Attachment-Failed";
	//static final String localBCAttachmentFragmentConfirmed = "LocalMSG-Conversations-Attachment-Confirmed";
	//static final String localBCAttachmentFragmentData = "LocalMSG-Conversations-Attachment-Data";
	//static final String localBCUpdateConversationViews = "LocalMSG-Conversations-UpdateUserViews";
	static final String localBCConversationUpdate = "LocalMSG-Conversations-ConversationUpdate";
	
	//Creating the view values
	private ListView listView;
	//private RecyclerView recyclerView;
	private ViewGroup serverWarning;
	
	//Creating the menu values
	private MenuItem searchMenuItem = null;
	
	//Creating the server warning animation values
	private int serverWarningHeight;
	private boolean serverWarningVisible = false;
	private boolean serverWarningFirst = true;
	
	//Creating the state values
	static final byte stateIdle = 0;
	static final byte stateLoading = 1;
	static final byte stateSyncing = 2;
	static final byte stateReady = 3;
	static final byte stateLoadError = 4;
	private byte currentState = stateIdle;
	private boolean conversationsExist = false;
	private boolean listingArchived = false;
	
	//Creating the state values
	private static final byte appBarStateDefault = 0;
	private static final byte appBarStateSearch = 1;
	private byte currentAppBarState = appBarStateDefault;
	
	//Creating the listener values
	private final AdapterView.OnItemClickListener onListItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			//Creating the intent
			Intent launchMessaging = new Intent(Conversations.this, Messaging.class);
			
			//Setting the extra
			launchMessaging.putExtra(Constants.intentParamTargetID, ((ConversationManager.ConversationInfo) listView.getItemAtPosition(position)).getLocalID());
			
			//Launching the intent
			startActivity(launchMessaging);
			
			//Enabling transitions
			//overridePendingTransition(R.anim.slide_in, R.anim.fade_out_light);
		}
	};
	private final AbsListView.MultiChoiceModeListener listMultiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {
		//Creating the selected conversations variable
		private int selectedConversations = 0;
		private int mutedConversations = 0;
		private int nonMutedConversations = 0;
		private int archivedConversations = 0;
		private int nonArchivedConversations = 0;
		
		@Override
		public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = ((ConversationManager.ConversationInfo) listView.getItemAtPosition(position));
			
			//Setting the item's checked state
			conversationInfo.setSelected(checked);
			conversationInfo.updateSelected();
			
			//Updating the selected conversations
			selectedConversations += checked ? 1 : -1;
			if(conversationInfo.isMuted()) mutedConversations += checked ? 1 : -1;
			else nonMutedConversations += checked ? 1 : -1;
			if(conversationInfo.isArchived()) archivedConversations += checked ? 1 : -1;
			else nonArchivedConversations += checked ? 1 : -1;
			
			//Setting the name
			SpannableString title = new SpannableString(getResources().getQuantityString(R.plurals.conversationsselected, selectedConversations, Integer.toString(selectedConversations)));
			title.setSpan(new ForegroundColorSpan(Color.WHITE), 0, title.length(), 0);
			actionMode.setTitle(title);
			
			//Showing or hiding the mute / unmute buttons
			if(mutedConversations > 0) actionMode.getMenu().findItem(R.id.action_unmute).setVisible(true);
			else actionMode.getMenu().findItem(R.id.action_unmute).setVisible(false);
			if(nonMutedConversations > 0) actionMode.getMenu().findItem(R.id.action_mute).setVisible(true);
			else actionMode.getMenu().findItem(R.id.action_mute).setVisible(false);
			
			//Showing or hiding the archive / unarchive buttons
			if(archivedConversations > 0) actionMode.getMenu().findItem(R.id.action_unarchive).setVisible(true);
			else actionMode.getMenu().findItem(R.id.action_unarchive).setVisible(false);
			if(nonArchivedConversations > 0) actionMode.getMenu().findItem(R.id.action_archive).setVisible(true);
			else actionMode.getMenu().findItem(R.id.action_archive).setVisible(false);
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			//Inflating the menu
			MenuInflater inflater = actionMode.getMenuInflater();
			inflater.inflate(R.menu.menu_conversation_contextual, menu);
			
			//Returning true
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}
		
		@Override
		public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
			//Checking if the item is the mute button
			if(menuItem.getItemId() == R.id.action_mute) {
				//Creating the updated conversation list
				final ArrayList<Long> updatedConversations = new ArrayList<>();
				
				//Looping through all conversations
				for(ConversationManager.ConversationInfo conversationInfo : conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!conversationInfo.isSelected()) continue;
					
					//Checking if the conversation is not muted
					if(!conversationInfo.isMuted()) {
						//Muting the conversation
						conversationInfo.setMuted(true);
						
						//Adding the conversation to the updated conversations list
						updatedConversations.add(conversationInfo.getLocalID());
					}
				}
				
				//Checking if there are conversations to update in the database
				if(!updatedConversations.isEmpty()) {
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_MUTED, true);
					
					//Updating the conversations
					SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(Conversations.this);
					for(long conversationID : updatedConversations) DatabaseManager.updateConversation(writableDatabase, conversationID, contentValues);
				}
				
				//Finishing the action mode
				actionMode.finish();
				
				//Returning true
				return true;
			}
			//Checking if the item is the unmute button
			else if(menuItem.getItemId() == R.id.action_unmute) {
				//Creating the updated conversation list
				ArrayList<Long> updatedConversations = new ArrayList<>();
				
				//Looping through all conversations
				for(ConversationManager.ConversationInfo conversationInfo : conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!conversationInfo.isSelected()) continue;
					
					//Checking if the conversation is muted
					if(conversationInfo.isMuted()) {
						//Unmuting the conversation
						conversationInfo.setMuted(false);
						
						//Adding the conversation to the updated conversations list
						updatedConversations.add(conversationInfo.getLocalID());
					}
				}
				
				//Checking if there are conversations to update in the database
				if(!updatedConversations.isEmpty()) {
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					
					//Setting the muted value
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_MUTED, false);
					
					//Updating the conversations
					SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(Conversations.this);
					for(long conversationID : updatedConversations) DatabaseManager.updateConversation(writableDatabase, conversationID, contentValues);
				}
				
				//Finishing the action mode
				actionMode.finish();
				
				//Returning true
				return true;
			}
			//Checking if the item is the archive button
			else if(menuItem.getItemId() == R.id.action_archive) {
				//Creating the updated conversation list
				ArrayList<ConversationManager.ConversationInfo> updatedConversations = new ArrayList<>();
				
				//Looping through all conversations
				for(ConversationManager.ConversationInfo conversationInfo : conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!conversationInfo.isSelected()) continue;
					
					//Archiving the conversation
					conversationInfo.setArchived(true);
					
					//Adding the conversation to the updated conversations list
					updatedConversations.add(conversationInfo);
				}
				
				//Checking if there are conversations to update in the database
				if(!updatedConversations.isEmpty()) {
					//Updating the list
					updateList();
					
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					
					//Setting the archived value
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
					
					//Updating the conversations
					SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(Conversations.this);
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.updateConversation(writableDatabase, conversationInfo.getLocalID(), contentValues);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar snackbar = Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.dialog_conversationarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.button_undo, view -> {
						//Creating the content values
						ContentValues undoContentValues = new ContentValues();
						undoContentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
						
						//Iterating over the conversations
						for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) {
							//Unarchiving the conversation
							conversationInfo.setArchived(false);
							
							//Setting the archived value
							contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, false);
							
							//Updating the conversations
							DatabaseManager.updateConversation(writableDatabase, conversationInfo.getLocalID(), undoContentValues);
						}
						
						//Updating the list
						updateList();
					});
					
					//Setting the snackbar's action button's color
					snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent, null));
					
					//Showing the snackbar
					snackbar.show();
				}
				
				//Finishing the action mode
				actionMode.finish();
				
				//Returning true
				return true;
			}
			//Checking if the item is the unarchive button
			else if(menuItem.getItemId() == R.id.action_unarchive) {
				//Creating the updated conversation list
				ArrayList<ConversationManager.ConversationInfo> updatedConversations = new ArrayList<>();
				
				//Looping through all conversations
				for(ConversationManager.ConversationInfo conversationInfo : conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!conversationInfo.isSelected()) continue;
					
					//Unarchiving the conversation
					conversationInfo.setArchived(false);
					
					//Adding the conversation to the updated conversations list
					updatedConversations.add(conversationInfo);
				}
				
				//Checking if there are conversations to update in the database
				if(!updatedConversations.isEmpty()) {
					//Updating the list
					updateList();
					
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					
					//Setting the archived value
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, false);
					
					//Updating the conversations
					SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(Conversations.this);
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.updateConversation(writableDatabase, conversationInfo.getLocalID(), contentValues);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar snackbar = Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.dialog_conversationunarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.button_undo, view -> {
						//Creating the content values
						ContentValues undoContentValues = new ContentValues();
						undoContentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
						
						//Iterating over the conversations
						for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) {
							//Archiving the conversation
							conversationInfo.setArchived(true);
							
							//Updating the conversations
							DatabaseManager.updateConversation(writableDatabase, conversationInfo.getLocalID(), undoContentValues);
						}
						
						//Updating the list
						updateList();
					});
					
					//Setting the snackbar's action button's color
					snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent, null));
					
					//Showing the snackbar
					snackbar.show();
				}
				
				//Finishing the action mode
				actionMode.finish();
				
				//Returning true
				return true;
			}
			//Checking if the item is the delete button
			else if(menuItem.getItemId() == R.id.action_delete) {
				//Displaying a dialog
				//Displaying a dialog warning about the message types
				new AlertDialog.Builder(Conversations.this)
						//Setting the message
						.setMessage(getResources().getQuantityString(R.plurals.dialog_deleteconversation, selectedConversations))
						//Setting the button
						.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
						.setPositiveButton(R.string.button_delete, (dialog, which) -> {
							//Creating the to remove list
							ArrayList<ConversationManager.ConversationInfo> toRemove = new ArrayList<>();
							
							//Marking all selected conversations for removal
							for(ConversationManager.ConversationInfo conversationInfo : conversations)
								if(conversationInfo.isSelected())
									toRemove.add(conversationInfo);
							
							//Deleting the conversations
							for(ConversationManager.ConversationInfo conversationInfo : toRemove) conversationInfo.delete(Conversations.this);
							
							//Updating the conversation activity list
							LocalBroadcastManager.getInstance(Conversations.this).sendBroadcast(new Intent(Conversations.localBCConversationUpdate));
							
							//Updating the list
							updateList();
							
							//Dismissing the dialog
							dialog.dismiss();
							
							//Finishing the action mode
							actionMode.finish();
						})
						//Creating the dialog
						.create()
						//Showing the dialog
						.show();
				
				//Returning true
				return true;
			}
			
			//Returning false
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			//Setting all items as unchecked
			for(ConversationManager.ConversationInfo conversationInfo : conversations) {
				conversationInfo.setSelected(false);
				conversationInfo.updateSelected();
			}
			
			//Clearing the selected conversations
			selectedConversations = 0;
			mutedConversations = 0;
			nonMutedConversations = 0;
			archivedConversations = 0;
			nonArchivedConversations = 0;
		}
	};
	private final BroadcastReceiver clientConnectionResultBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Getting the result
			final byte result = intent.getByteExtra(Constants.intentParamResult, ConnectionService.intentResultValueConnection);
			
			//Showing the server warning
			serverWarning.post(() -> {
				if(result == ConnectionService.intentResultValueSuccess) hideServerWarning();
				else showServerWarning(result);
			});
		}
	};
	private final BroadcastReceiver syncStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Setting the state
			if(intent.getBooleanExtra(Constants.intentParamAction, true)) setState(stateSyncing);
			else setState(stateReady);
		}
	};
	private final BroadcastReceiver updateConversationsBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateList();
		}
	};
	
	/* private final BroadcastReceiver fileFragmentBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Getting the request data
			String message = intent.getStringExtra(Constants.intentParamGuid);
			long requestTime = intent.getLongExtra(Constants.intentParamTime, -1);
			
			switch(intent.getAction()) {
				case Conversations.localBCAttachmentFragmentConfirmed:
					//Processing the message
					processFileFragmentConfirmed(message, requestTime);
					break;
				case Conversations.localBCAttachmentFragmentFailed:
					//Processing the message
					processFileFragmentFailed(message, requestTime);
					break;
				case Conversations.localBCAttachmentFragmentData:
					//Getting the data
					byte[] compressedBytes = intent.getByteArrayExtra(Constants.intentParamData);
					int index = intent.getIntExtra(Constants.intentParamIndex, -1);
					boolean isLast = intent.getBooleanExtra(Constants.intentParamIsLast, false);
					long fileSize = intent.getLongExtra(Constants.intentParamSize, -1);
					
					//Processing the message
					processFileFragmentData(context, message, requestTime, compressedBytes, index, isLast, fileSize);
					break;
			}
		}
	}; */
	//static final int updateConversationViewsActionRefresh = 0;
	//static final int updateConversationViewsActionClear = 1;
	/* private final BroadcastReceiver updateConversationViewsBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch(intent.getIntExtra(Constants.intentParamAction, 0)) {
				case updateConversationViewsActionRefresh:
					UserCacheHelper.refreshUsers(context, ConversationManager.getConversations().toArray(new ConversationManager.ConversationInfo[0]));
					break;
				case updateConversationViewsActionClear:
					UserCacheHelper.clearUsers(context, ConversationManager.getConversations().toArray(new ConversationManager.ConversationInfo[0]));
					break;
			}
		}
	}; */
	
	//Creating the timer values
	static final long timeUpdateHandlerDelay = 60 * 1000; //1 minute
	private Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
	private Runnable timeUpdateHandlerRunnable = new Runnable() {
		@Override
		public void run() {
			//Updating the time
			if(conversations != null) for(ConversationManager.ConversationInfo conversationInfo : conversations) conversationInfo.updateTime(Conversations.this);
			
			//Running again
			timeUpdateHandler.postDelayed(this, timeUpdateHandlerDelay);
		}
	};
	
	//Creating the other values
	private MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> conversations;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Checking if there is no hostname
		if(getSharedPreferences(MainApplication.sharedPreferencesFile, Context.MODE_PRIVATE).getString(MainApplication.sharedPreferencesKeyHostname, "").isEmpty()) {
			//Creating the intent
			Intent launchServerSetup = new Intent(this, ServerSetup.class);
			
			//Setting the change as required
			launchServerSetup.putExtra(ServerSetup.intentExtraRequired, true);
			
			//Launching the intent
			startActivity(launchServerSetup);
			
			//Finishing the current activity
			finish();
			
			//Returning
			return;
		}/* else {
			//Starting the connection service
			Intent serviceIntent = new Intent(this, ConnectionService.class);
			startService(serviceIntent);
			
			//Launching the conversations activity
			startActivity(new Intent(this, Conversations.class));
		} */
		
		//Enabling transitions
		//getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
		//getWindow().setExitTransition(new Slide());
		
		//Setting the content view
		setContentView(R.layout.activity_conversations);
		
		//Enabling the toolbar
		//setSupportActionBar(findViewById(R.id.toolbar));
		
		//Getting the views
		listView = findViewById(R.id.list);
		serverWarning = findViewById(R.id.serverwarning);
		
		//Enforcing the maximum content width
		Constants.enforceContentWidth(getResources(), listView);
		
		//Getting the server warning height
		serverWarning.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				//Setting the height
				serverWarningHeight = serverWarning.getHeight();
				
				//Setting the visibility to gone
				serverWarning.setVisibility(View.GONE);
				
				//Removing the listener
				serverWarning.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
		
		//Configuring the list
		listView.setOnItemClickListener(onListItemClickListener);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(listMultiChoiceModeListener);
		
		//Setting the listeners
		findViewById(R.id.fab).setOnClickListener(view -> startActivity(new Intent(Conversations.this, NewMessage.class)));
	}
	
	@Override
	protected void onStart() {
		//Calling the super method
		super.onStart();
		
		//Adding the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(clientConnectionResultBroadcastReceiver, new IntentFilter(ConnectionService.localBCResult));
		localBroadcastManager.registerReceiver(syncStateBroadcastReceiver, new IntentFilter(ConnectionService.localBCMassRetrieval));
		localBroadcastManager.registerReceiver(updateConversationsBroadcastReceiver, new IntentFilter(localBCConversationUpdate));
		
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
		if(connectionService != null && connectionService.isMassRetrievalInProgress()) setState(stateSyncing);
		//Otherwise checking if the conversations are loaded
		else if(conversations != null && conversations.isLoaded()) conversationLoadFinished(null);
		else {
			//Setting the state to loading
			setState(stateLoading);
			
			//Loading the messages
			new LoadConversationsTask(this).execute();
		}
		
		//Starting the service
		startService(new Intent(Conversations.this, ConnectionService.class));
		
		//Starting the time updater
		timeUpdateHandler.postDelayed(timeUpdateHandlerRunnable, timeUpdateHandlerDelay);
	}
	
	private void setState(byte state) {
		//Returning if the current state matches the requested state
		if(currentState == state) return;
		
		//Disabling the old state
		switch(currentState) {
			case stateLoading:
				findViewById(R.id.loading_text).setVisibility(View.GONE);
				break;
			case stateSyncing: {
				View syncView = findViewById(R.id.syncview);
				syncView.setVisibility(View.GONE);
				syncView.findViewById(R.id.syncview_icon).setAnimation(null);
				break;
			}
			case stateReady:
				findViewById(R.id.list).setVisibility(View.GONE);
				findViewById(R.id.no_conversations).setVisibility(View.GONE);
				break;
			case stateLoadError:
				findViewById(R.id.errorview).setVisibility(View.GONE);
				break;
		}
		
		//Enabling the new state
		switch(state) {
			case stateLoading:
				findViewById(R.id.loading_text).setVisibility(View.VISIBLE);
				break;
			case stateSyncing: {
				View syncView = findViewById(R.id.syncview);
				syncView.setVisibility(View.VISIBLE);
				
				RotateAnimation rotateAnimation = new RotateAnimation(0, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				rotateAnimation.setInterpolator(new LinearInterpolator());
				rotateAnimation.setDuration(2500);
				rotateAnimation.setRepeatCount(Animation.INFINITE);
				syncView.findViewById(R.id.syncview_icon).setAnimation(rotateAnimation);
				
				break;
			}
			case stateReady:
				findViewById(R.id.list).setVisibility(View.VISIBLE);
				if(conversations.isEmpty()) findViewById(R.id.no_conversations).setVisibility(View.VISIBLE);
				break;
			case stateLoadError:
				findViewById(R.id.errorview).setVisibility(View.VISIBLE);
				break;
		}
		
		//Setting the new state
		currentState = state;
	}
	
	void conversationLoadFinished(ArrayList<ConversationManager.ConversationInfo> result) {
		//Replacing the conversations
		if(result != null) {
			conversations.clear();
			conversations.addAll(result);
			conversations.setLoaded(true);
		}
		
		//Setting the state
		setState(stateReady);
		
		//Updating the list
		listView.setAdapter(new ListAdapter(conversations));
		updateList();
		
		//Updating the views
		//for(ConversationManager.ConversationInfo conversationInfo : ConversationManager.getConversations()) conversationInfo.updateView(Conversations.this);
	}
	
	void conversationLoadFailed() {
		//Setting the state to failed
		setState(stateLoadError);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Inflating the menu resource
		getMenuInflater().inflate(R.menu.menu_conversations, menu);
		searchMenuItem = menu.findItem(R.id.action_search);
		
		//Returning true
		return true;
	}
	
	private class ListAdapter extends BaseAdapter {
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
		@NonNull
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			//Getting the view
			View view = convertView;
			
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = getItem(position);
			
			//Returning if the conversation info is invalid
			if(conversationInfo == null) return view;
			
			//Getting the view
			view = conversationInfo.createView(Conversations.this, convertView, parent);
			
			//Setting the view source
			conversationInfo.setViewSource(() -> listView.getChildAt(filteredItems.indexOf(conversationInfo) - listView.getFirstVisiblePosition()));
			
			//Returning the view
			return view;
		}
		
		void filterAndUpdate() {
			//Clearing the filtered data
			filteredItems.clear();
			
			//Iterating over the original data
			for(ConversationManager.ConversationInfo conversationInfo : originalItems) {
				//Skipping non-listed conversations
				if(conversationInfo.isArchived() ^ listingArchived) continue;
				
				//Adding the item to the filtered data
				filteredItems.add(conversationInfo);
			}
			
			//Notifying the adapter
			notifyDataSetChanged();
		}
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
				contactName = view.findViewById(R.id.label_name);
				contactAddress = view.findViewById(R.id.label_address);
				
				header = view.findViewById(R.id.header);
				headerLabel = view.findViewById(R.id.header_label);
				
				profileDefault = view.findViewById(R.id.profile_default);
				profileImage = view.findViewById(R.id.profile_image);
				
				contentArea = view.findViewById(R.id.area_content);
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
		
		//Showing the server warning if necessary
		ConnectionService connectionService = ConnectionService.getInstance();
		if(connectionService == null || (!connectionService.isConnected() && !connectionService.isConnecting() && ConnectionService.lastConnectionResult != -1)) showServerWarning(ConnectionService.lastConnectionResult);
		else hideServerWarning();
		
		//Checking if the contacts permission has been granted
		findViewById(R.id.contactnotice).setVisibility(MainApplication.canUseContacts(this) ? View.GONE : View.VISIBLE);
		
		//Refreshing the list
		updateList();
	}
	
	public void onRequestContactsClick(View view) {
		//Requesting the permission
		requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, Constants.permissionReadContacts);
	}
	
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		//Checking if the request code is contacts access
		if(requestCode == Constants.permissionReadContacts) {
			//Checking if the result is a success
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				//Hiding the request box
				findViewById(R.id.contactnotice).setVisibility(View.GONE);
			}
			//Otherwise checking if the result is a denial
			else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
				//Showing a snackbar
				Snackbar.make(findViewById(R.id.root), R.string.permission_rejected, Snackbar.LENGTH_LONG)
						.setAction(R.string.settings, view -> {
							//Opening the application settings
							Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							intent.setData(Uri.parse("package:" + getPackageName()));
							startActivity(intent);
						})
						.setActionTextColor(getResources().getColor(R.color.colorAccent, null))
						.show();
			}
		}
	}
	
	@Override
	protected void onStop() {
		//Calling the super method
		super.onStop();
		
		//Removing the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.unregisterReceiver(clientConnectionResultBroadcastReceiver);
		localBroadcastManager.unregisterReceiver(syncStateBroadcastReceiver);
		localBroadcastManager.unregisterReceiver(updateConversationsBroadcastReceiver);
		
		//Stopping the time updater
		timeUpdateHandler.removeCallbacks(timeUpdateHandlerRunnable);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home: //Up button
				//Checking if the state is in a search
				if(currentAppBarState == appBarStateSearch) {
					//Setting the state to normal
					setAppBarState(appBarStateDefault);
				}
				//Checking if the "archived" view is active
				else if(listingArchived) {
					//Exiting the archived state
					setArchivedListingState(false);
				} else {
					//Finishing the activity
					finish();
				}
				return true;
			case R.id.action_search:
				//Setting the state to search
				if(currentState == stateReady) setAppBarState(appBarStateSearch);
				
				return true;
			case R.id.action_archived: //Archived conversations
				//Starting the archived conversations activity
				//startActivity(new Intent(this, ConversationsArchived.class));
				setArchivedListingState(!listingArchived);
				
				return true;
			/* case R.id.action_blocked: //Blocked contacts
				return true; */
			case R.id.action_settings: //Settings
				//Starting the settings activity
				startActivity(new Intent(this, Preferences.class));
				
				//Returning true
				return true;
			case R.id.action_feedback: //Send feedback
				//Showing a dialog
				new AlertDialog.Builder(this)
						.setTitle(R.string.feedback)
						.setMessage(R.string.feedback_message)
						.setNeutralButton(R.string.feedback_email, (dialog, which) -> {
							//Creating the intent
							Intent intent = new Intent(Intent.ACTION_SENDTO);
							intent.setData(Uri.parse("mailto:"));
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.feedbackEmail});
							intent.putExtra(Intent.EXTRA_SUBJECT, "AirMessage Feedback");
							intent.putExtra(Intent.EXTRA_TEXT, "\r\n\r\n" +
									"---------- DEVICE INFORMATION----------" + "\r\n" +
									"Device model: " + Build.MODEL + "\r\n" +
									"Android version: " + Build.VERSION.RELEASE + "\r\n" +
									"Client version: " + BuildConfig.VERSION_NAME + "\r\n" +
									"AM communications version: " + SharedValues.mmCommunicationsVersion);
							//intent.setType("message/rfc822");
							
							//Launching the intent
							if(intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
							else Toast.makeText(this, R.string.intent_noemail, Toast.LENGTH_SHORT).show();
						})
						.setPositiveButton(R.string.feedback_googleplus, (dialog, which) -> {
							//Creating the intent
							Intent intent = new Intent(Intent.ACTION_VIEW, Constants.googlePlusCommunityAddress);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							
							//Launching the intent
							if(intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
							else Toast.makeText(this, R.string.intent_nobrowser, Toast.LENGTH_SHORT).show();
						})
						.create().show();
				
				//Returning true
				return true;
		}
		
		//Returning false
		return false;
	}
	
	@Override
	public void onBackPressed() {
		//Checking if the state is in a search
		if(currentAppBarState == appBarStateSearch) {
			//Setting the state to normal
			setAppBarState(appBarStateDefault);
		}
		//Checking if the "archived" view is active
		else if(listingArchived) {
			//Exiting the archived state
			setArchivedListingState(false);
		} else {
			//Calling the super method
			super.onBackPressed();
		}
	}
	
	void showServerWarning(byte reason) {
		//Showing the warning box
		serverWarning.setVisibility(View.VISIBLE);
		
		//Resetting the view's position if it is the first animation
		if(serverWarningFirst) {
			//Setting the Y to below the app bar
			serverWarning.setY(getSupportActionBar().getHeight() - serverWarningHeight);
			
			//Unflagging the first view position
			serverWarningFirst = false;
		}
		
		//Checking if the warning not visible
		if(!serverWarningVisible) {
			//Showing the warning box
			serverWarning.setVisibility(View.VISIBLE);
			
			//Animating the warning box downwards
			serverWarning.animate().translationY(0).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(300).start();
			
			//Setting the server warning as visible
			serverWarningVisible = true;
		}
		
		//Getting the warning box components
		TextView message = serverWarning.findViewById(R.id.serverwarning_message);
		Button button = serverWarning.findViewById(R.id.serverwarning_button);
		
		//Enabling the button
		button.setEnabled(true);
		
		switch(reason) {
			case ConnectionService.intentResultValueInternalException:
				message.setText(getResources().getString(R.string.serverstatus_limitedfunctionality, getResources().getString(R.string.serverstatus_internalexception)));
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Conversations.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
			case ConnectionService.intentResultValueBadRequest:
				message.setText(getResources().getString(R.string.serverstatus_limitedfunctionality, getResources().getString(R.string.serverstatus_badrequest)));
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Conversations.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
			case ConnectionService.intentResultValueClientOutdated:
				message.setText(getResources().getString(R.string.serverstatus_limitedfunctionality, getResources().getString(R.string.serverstatus_clientoutdated)));
				button.setText(R.string.button_update);
				button.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()))));
				break;
			case ConnectionService.intentResultValueServerOutdated:
				message.setText(getResources().getString(R.string.serverstatus_limitedfunctionality, getResources().getString(R.string.serverstatus_serveroutdated)));
				button.setText(R.string.button_help);
				button.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Constants.serverUpdateAddress)));
				break;
			case ConnectionService.intentResultValueUnauthorized:
				message.setText(getResources().getString(R.string.serverstatus_limitedfunctionality, getResources().getString(R.string.serverstatus_authfail)));
				button.setText(R.string.button_reconfigure);
				button.setOnClickListener(view -> startActivity(new Intent(Conversations.this, ServerSetup.class)));
				break;
			case ConnectionService.intentResultValueConnection:
				message.setText(getResources().getString(R.string.serverstatus_limitedfunctionality, getResources().getString(R.string.serverstatus_noconnection)));
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Conversations.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
			default:
				message.setText(getResources().getString(R.string.serverstatus_limitedfunctionality, getResources().getString(R.string.serverstatus_unknown)));
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Conversations.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
		}
	}
	
	void hideServerWarning() {
		//Checking if the server warning is visible
		if(serverWarningVisible) {
			//Animating the warning box upwards
			serverWarning.animate().translationY(-serverWarningHeight).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(300).withEndAction(() -> serverWarning.setVisibility(View.GONE)).start();
			
			//Setting the server warning as invisible
			serverWarningVisible = false;
		}
		
		//Disabling the action button
		serverWarning.findViewById(R.id.serverwarning_button).setEnabled(false);
	}
	
	public void updateList() {
		//Returning if the conversations aren't ready
		if(conversations == null || !conversations.isLoaded()) return;
		
		//Updating the list
		//if(sort) Collections.sort(ConversationManager.getConversations(), ConversationManager.conversationComparator);
		ListAdapter listAdapter = ((ListAdapter) listView.getAdapter());
		if(listAdapter == null) return;
		listAdapter.filterAndUpdate();
		
		//Returning if the state is not ready
		if(currentState != stateReady) return;
		
		//Getting and checking if there are conversations
		boolean newConversationsExist = listAdapter.filteredItems.isEmpty();
		if(newConversationsExist != conversationsExist) {
			//Setting "no conversations" view state
			(findViewById(R.id.no_conversations)).animate().alpha(newConversationsExist ? 1 : 0).start();
			
			//Setting the new state
			conversationsExist = newConversationsExist;
		}
	}
	
	void setAppBarState(byte state) {
		//Returning if the requested state matches the current state
		if(currentAppBarState == state) return;
		
		//Getting the action bar
		ActionBar actionBar = getSupportActionBar();
		
		//Checking if the state is to the app bar
		if(state == appBarStateDefault) {
			actionBar.setDisplayShowCustomEnabled(false);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(listingArchived);
			searchMenuItem.setVisible(true);
		} else {
			actionBar.setDisplayShowCustomEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			searchMenuItem.setVisible(false);
		}
		
		//Preparing the new state
		switch(state) {
			case appBarStateSearch:
				//Setting the view
				actionBar.setCustomView(R.layout.appbar_search);
				
				//Configuring the listeners
				((EditText) actionBar.getCustomView().findViewById(R.id.search_edittext)).addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
					
					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						/* //Returning if the conversation info is invalid
						if(conversationInfo == null) return;
						
						//Checking if there is no search text
						if(s.length() == 0) {
							//Hiding the result bar
							findViewById(R.id.searchresults).setVisibility(View.GONE);
							
							//Hiding the clear text button
							actionBar.getCustomView().findViewById(R.id.search_buttonclear).setVisibility(View.INVISIBLE);
							
							//Returning
							return;
						}
						
						//Showing the result bar
						View searchResultBar = findViewById(R.id.searchresults);
						searchResultBar.setVisibility(View.VISIBLE);
						
						//Showing the clear text button
						actionBar.getCustomView().findViewById(R.id.search_buttonclear).setVisibility(View.VISIBLE);
						
						//Conducting the search
						searchFilteredMessages.clear();
						searchListIndexes.clear();
						
						for(int i = 0; i < conversationInfo.getConversationItems().size(); i++) {
							ConversationManager.ConversationItem conversationItem = conversationInfo.getConversationItems().get(i);
							if(!(conversationItem instanceof ConversationManager.MessageInfo)) continue;
							ConversationManager.MessageInfo messageInfo = (ConversationManager.MessageInfo) conversationItem;
							if(!Constants.containsIgnoreCase(messageInfo.getMessageText(), s.toString())) continue;
							
							searchFilteredMessages.add(messageInfo);
							searchListIndexes.add(i);
						}
						
						//Updating the search result bar
						searchCount = searchFilteredMessages.size();
						searchIndex = searchCount - 1;
						((TextView) searchResultBar.findViewById(R.id.searchresults_message)).setText(getResources().getString(R.string.search_results, searchIndex + 1, searchCount));
						
						//Scrolling to the item
						if(!searchListIndexes.isEmpty()) messageList.setSelection(searchListIndexes.get(searchIndex)); */
					}
					
					@Override
					public void afterTextChanged(Editable s) {}
				});
				
				//Clear the text box
				actionBar.getCustomView().findViewById(R.id.search_buttonclear).setOnClickListener(view ->
						((EditText) getSupportActionBar().getCustomView().findViewById(R.id.search_edittext)).setText(""));
				
				//Focusing the text box
				actionBar.getCustomView().findViewById(R.id.search_edittext).requestFocus();
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
		}
		
		//Updating the current state
		currentAppBarState = state;
	}
	
	void setArchivedListingState(boolean state) {
		//Returning if the current state matches the requested state
		if(listingArchived == state) return;
		
		//Setting the new state
		listingArchived = state;
		
		//Animating the action bar color
		int colorPrimary = getResources().getColor(R.color.colorPrimary, null);
		int colorPrimaryDark = getResources().getColor(R.color.colorPrimaryDark, null);
		int colorArchived = getResources().getColor(R.color.colorArchived, null);
		int colorArchivedDark = getResources().getColor(R.color.colorArchivedDark, null);
		int colorList[];
		int colorListDark[];
		if(state) {
			colorList = new int[]{colorPrimary, colorArchived};
			colorListDark = new int[]{colorPrimaryDark, colorArchivedDark};
		} else {
			colorList = new int[]{colorArchived, colorPrimary};
			colorListDark = new int[]{colorArchivedDark, colorPrimaryDark};
		}
		
		ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
		anim.addUpdateListener(animation -> {
			//Getting the position
			float position = animation.getAnimatedFraction();
			
			//Tinting the status bar
			int blended = ColorUtils.blendARGB(colorListDark[0], colorListDark[1], position);
			getWindow().setStatusBarColor(blended);
			
			//Tinting the app bar
			blended = ColorUtils.blendARGB(colorList[0], colorList[1], position);
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(blended));
		});
		anim.setDuration(250);
		anim.setInterpolator(new DecelerateInterpolator());
		anim.start();
		
		//Configuring the action bar
		ActionBar actionBar = getSupportActionBar();
		if(state) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.archived_conversations);
		} else {
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(R.string.app_name);
		}
		
		//Updating the list adapter
		updateList();
		//((ListAdapter) listView.getAdapter()).filterAndUpdate();
	}
	
	/**
	 * Defines callbacks for service binding, passed to bindService()
	 */
	/* private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			ConnectionService.ConnectionBinder binder = (ConnectionService.ConnectionBinder) service;
			connectionService = binder.getService();
			isServiceBound = true;
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			isServiceBound = false;
		}
	}; */
	
	private static class LoadConversationsTask extends AsyncTask<Void, Void, MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo>> {
		private final WeakReference<Conversations> superclassReference;
		
		//Creating the values
		LoadConversationsTask(Conversations superclass) {
			//Setting the references
			superclassReference = new WeakReference<>(superclass);
		}
		
		@Override
		protected MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> doInBackground(Void... params) {
			//Getting the context
			Context context = superclassReference.get();
			if(context == null) return null;
			
			//Loading the conversations
			return DatabaseManager.fetchSummaryConversations(DatabaseManager.getReadableDatabase(context), context);
		}
		
		@Override
		protected void onPostExecute(MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> result) {
			//Checking if the result is a fail
			if(result == null) {
				//Telling the superclass
				Conversations superclass = superclassReference.get();
				if(superclass != null) superclass.conversationLoadFailed();
			} else {
				//Telling the superclass
				Conversations superclass = superclassReference.get();
				if(superclass != null) {
					superclass.conversationLoadFinished(result);
				}
			}
		}
	}
	
	static class DeleteAttachmentsTask extends AsyncTask<Void, Void, Void> {
		//Creating the values
		private final WeakReference<Context> contextReference;
		private final WeakReference<View> snackbarParentReference;
		
		DeleteAttachmentsTask(Context context, View snackbarParent) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			snackbarParentReference = new WeakReference<>(snackbarParent);
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
		
		@Override
		protected void onPostExecute(Void result) {
			//Getting the snackbar parent view
			View parentView = snackbarParentReference.get();
			if(parentView == null) return;
			
			//Showing a snackbar
			Snackbar snackbar = Snackbar.make(parentView, R.string.preferences_deleteattachments_finished, Snackbar.LENGTH_LONG);
			
			//Setting the snackbar's action button's color
			snackbar.setActionTextColor(parentView.getContext().getResources().getColor(R.color.colorAccent, null));
			
			//Showing the snackbar
			snackbar.show();
		}
	}
	
	static class DeleteMessagesTask extends AsyncTask<Void, Void, Void> {
		//Creating the values
		private final WeakReference<Context> contextReference;
		private final WeakReference<View> snackbarParentReference;
		
		DeleteMessagesTask(Context context, View snackbarParent) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			snackbarParentReference = new WeakReference<>(snackbarParent);
		}
		
		@Override
		protected void onPreExecute() {
			//Clearing all conversations
			ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null) conversations.clear();
			
			//Updating the conversation activity list
			Context context = contextReference.get();
			if(context != null) LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Conversations.localBCConversationUpdate));
		}
		
		@Override
		protected Void doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Removing the messages from the database
			DatabaseManager.deleteEverything(context);
			
			//Clearing the attachments directory
			MainApplication.clearAttachmentsDirectory(context);
			
			//Returning
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//Getting the snackbar parent view
			View parentView = snackbarParentReference.get();
			if(parentView == null) return;
			
			//Showing a snackbar
			Snackbar snackbar = Snackbar.make(parentView, R.string.preferences_deleteall_finished, Snackbar.LENGTH_LONG);
			
			//Setting the snackbar text to white
			((TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
			
			//Showing the snackbar
			snackbar.show();
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
			//Clearing all conversations
			ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null) conversations.clear();
			
			//Updating the conversation activity list
			Context context = contextReference.get();
			if(context != null) LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Conversations.localBCConversationUpdate));
			/* for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
				callbacks.updateList(false); */
		}
		
		@Override
		protected Void doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Removing the messages from the database
			DatabaseManager.deleteEverything(context);
			
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
			
			//Syncing the messages
			ConnectionService connectionService = ConnectionService.getInstance();
			boolean messageResult = connectionService != null && connectionService.requestMassRetrieval(context);
			if(!messageResult) {
				Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT).show();
				return;
			}
			
			//Getting the snackbar parent view
			View parentView = snackbarParentReference.get();
			if(parentView == null) return;
			
			//Showing a snackbar
			Snackbar snackbar = Snackbar.make(parentView, R.string.dialog_resyncmessages_finished, Snackbar.LENGTH_LONG);
			
			//Setting the snackbar text to white
			((TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
			
			//Showing the snackbar
			snackbar.show();
		}
	}
}