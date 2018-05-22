package me.tagavari.airmessage;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.common.SharedValues;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;

public class Conversations extends AppCompatCompositeActivity {
	//Creating the plugin values
	private ConversationsBase conversationsBasePlugin;
	private PluginMessageBar pluginMessageBar;
	
	//Creating the view model and info bar values
	private ActivityViewModel viewModel;
	private PluginMessageBar.InfoBar infoBarConnection, infoBarContacts, infoBarSystemUpdate;
	
	//Creating the menu values
	private MenuItem searchMenuItem = null;
	
	//Creating the state values
	private boolean listingArchived = false;
	
	//Creating the state values
	private static final byte appBarStateDefault = 0;
	private static final byte appBarStateSearch = 1;
	private byte currentAppBarState = appBarStateDefault;
	
	//Creating the listener values
	private ActionMode actionMode = null;
	private final CountingActionModeCallback actionModeCallbacks = new CountingActionModeCallback();
	
	/* private final AbsListView.MultiChoiceModeListener listMultiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {
		//Creating the selected conversations variable
		private int selectedConversations = 0;
		private int mutedConversations = 0;
		private int nonMutedConversations = 0;
		private int archivedConversations = 0;
		private int nonArchivedConversations = 0;
		
		@Override
		public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = ((ConversationManager.ConversationInfo) conversationsBasePlugin.listView.getItemAtPosition(position));
			
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
			actionMode.setTitle(getResources().getQuantityString(R.plurals.message_selectioncount, selectedConversations, Integer.toString(selectedConversations)));
			
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
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
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
					for(long conversationID : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationID, contentValues);
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
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
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
					for(long conversationID : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationID, contentValues);
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
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
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
					conversationsBasePlugin.updateList(false);
					
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					
					//Setting the archived value
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
					
					//Updating the conversations
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), contentValues);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
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
							DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), undoContentValues);
						}
						
						//Updating the list
						conversationsBasePlugin.updateList(false);
					}).show();
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
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
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
					conversationsBasePlugin.updateList(false);
					
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					
					//Setting the archived value
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, false);
					
					//Updating the conversations
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), contentValues);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationunarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
						//Creating the content values
						ContentValues undoContentValues = new ContentValues();
						undoContentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
						
						//Iterating over the conversations
						for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) {
							//Archiving the conversation
							conversationInfo.setArchived(true);
							
							//Updating the conversations
							DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), undoContentValues);
						}
						
						//Updating the list
						conversationsBasePlugin.updateList(false);
					}).show();
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
						.setMessage(getResources().getQuantityString(R.plurals.message_confirm_deleteconversation, selectedConversations))
						//Setting the button
						.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
						.setPositiveButton(R.string.action_delete, (dialog, which) -> {
							//Creating the to remove list
							ArrayList<ConversationManager.ConversationInfo> toRemove = new ArrayList<>();
							
							//Marking all selected conversations for removal
							for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations)
								if(conversationInfo.isSelected())
									toRemove.add(conversationInfo);
							
							//Deleting the conversations
							for(ConversationManager.ConversationInfo conversationInfo : toRemove) conversationInfo.delete(Conversations.this);
							
							//Updating the conversation activity list
							LocalBroadcastManager.getInstance(Conversations.this).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
							
							//Updating the list
							conversationsBasePlugin.updateList(false);
							
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
			for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
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
	}; */
	private final BroadcastReceiver clientConnectionResultBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(Constants.intentParamState, -1);
			if(state == ConnectionService.stateDisconnected) {
				int code = intent.getIntExtra(Constants.intentParamCode, -1);
				showServerWarning(code);
				infoBarSystemUpdate.hide();
			} else {
				hideServerWarning();
				if(state == ConnectionService.stateConnected) {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService != null && connectionService.getActiveCommunicationsVersion() < SharedValues.mmCommunicationsVersion) infoBarSystemUpdate.show();
					else infoBarSystemUpdate.hide();
				}
			}
		}
	};
	
	public Conversations() {
		//Setting the plugins;
		addPlugin(conversationsBasePlugin = new ConversationsBase(() -> new RecyclerAdapter(conversationsBasePlugin.conversations)));
		addPlugin(pluginMessageBar = new PluginMessageBar());
		addPlugin(new PluginThemeUpdater());
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Checking if there is no hostname
		if(!((MainApplication) getApplication()).isServerConfigured()) {
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
		
		//Setting the content view
		setContentView(R.layout.activity_conversations);
		
		//Enabling the toolbar
		setSupportActionBar(findViewById(R.id.toolbar));
		
		//Getting the view model
		viewModel = ViewModelProviders.of(this).get(ActivityViewModel.class);
		
		//Setting the plugin views
		conversationsBasePlugin.setViews(findViewById(R.id.list), findViewById(R.id.syncview_progress), findViewById(R.id.no_conversations));
		pluginMessageBar.setParentView(findViewById(R.id.infobar_container));
		
		//Enforcing the maximum content width
		Constants.enforceContentWidth(getResources(), conversationsBasePlugin.recyclerView);
		
		//Configuring the list
		//conversationsBasePlugin.listView.setOnItemClickListener(onListItemClickListener);
		//conversationsBasePlugin.listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		//conversationsBasePlugin.listView.setMultiChoiceModeListener(listMultiChoiceModeListener);
		
		//Setting the listeners
		findViewById(R.id.fab).setOnClickListener(view -> startActivity(new Intent(Conversations.this, NewMessage.class)));
		
		conversationsBasePlugin.addConversationsLoadedListener(() -> {
			//Restoring the action mode
			restoreActionMode();
		});
		conversationsBasePlugin.addUpdateListListener(() -> {
			TextView noConversations = findViewById(R.id.no_conversations);
			if(listingArchived) noConversations.setText(R.string.message_blankstate_conversations_archived);
			else noConversations.setText(R.string.message_blankstate_conversations);
		});
		
		//Creating the info bars
		infoBarConnection = pluginMessageBar.create(R.drawable.disconnection, null);
		infoBarContacts = pluginMessageBar.create(R.drawable.contacts, getResources().getString(R.string.message_permissiondetails_contacts_listing));
		infoBarContacts.setButton(R.string.action_enable, view -> requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, Constants.permissionReadContacts));
		infoBarSystemUpdate = pluginMessageBar.create(R.drawable.update, getResources().getString(R.string.message_serverupdate));
	}
	
	@Override
	public void onResume() {
		//Calling the super method
		super.onResume();
		
		//Showing the server warning if necessary
		ConnectionService connectionService = ConnectionService.getInstance();
		if(connectionService == null) showServerWarning(ConnectionService.intentResultCodeConnection);
		else if(connectionService.getCurrentState() == ConnectionService.stateDisconnected && ConnectionService.getLastConnectionResult() != -1) showServerWarning(ConnectionService.getLastConnectionResult());
		else {
			hideServerWarning();
			if(connectionService.getCurrentState() == ConnectionService.stateConnected && connectionService.getActiveCommunicationsVersion() < SharedValues.mmCommunicationsVersion) infoBarSystemUpdate.show();
			else infoBarSystemUpdate.hide();
		}
		
		//Updating the contacts info bar
		if(MainApplication.canUseContacts(this)) infoBarContacts.hide();
		else infoBarContacts.show();
	}
	
	@Override
	public void onStart() {
		//Calling the super method
		super.onStart();
		
		//Adding the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(clientConnectionResultBroadcastReceiver, new IntentFilter(ConnectionService.localBCStateUpdate));
		
		//Starting the service
		startService(new Intent(Conversations.this, ConnectionService.class));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Inflating the menu resource
		getMenuInflater().inflate(R.menu.menu_conversations, menu);
		searchMenuItem = menu.findItem(R.id.action_search);
		
		//Returning true
		return true;
	}
	
	/* private class ListAdapter extends ConversationsBase.ListAdapter {
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
			view = conversationInfo.createView(Conversations.this, convertView, parent);
			
			//Setting the view source
			conversationInfo.setViewSource(() -> filteredItems.contains(conversationInfo) ? conversationsBasePlugin.listView.getChildAt(filteredItems.indexOf(conversationInfo) - conversationsBasePlugin.listView.getFirstVisiblePosition()) : null);
			
			//Returning the view
			return view;
		}
		
		@Override
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
		
		@Override
		boolean isListEmpty() {
			return filteredItems.isEmpty();
		}
	} */
	
	private boolean isSelectedActionMode(ConversationManager.ConversationInfo conversationInfo) {
		return viewModel.actionModeSelections.contains(conversationInfo.getLocalID());
	}
	
	class RecyclerAdapter extends ConversationsBase.RecyclerAdapter<ConversationManager.ConversationInfo.ItemViewHolder> {
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
		
		/* private final class DetailsLookup extends ItemDetailsLookup<Long> {
			public ItemDetails<Long> getItemDetails(MotionEvent event) {
				View view = conversationsBasePlugin.recyclerView.findChildViewUnder(event.getX(), event.getY());
				if(view == null) return null;
				
				return new ItemDetails<Long>() {
					@Override
					public int getPosition() {
						return recyclerView.getChildAdapterPosition(view);
					}
					
					@Override
					public Long getSelectionKey() {
						return recyclerView.getChildItemId(view);
					}
				};
			}
		} */
		
		@Override
		public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
			this.recyclerView = recyclerView;
			
			/* SelectionTracker<Long> selectionTracker = new SelectionTracker.Builder<>(
					"conversations",
					recyclerView,
					new StableIdKeyProvider(recyclerView),
					new DetailsLookup(),
					StorageStrategy.createLongStorage()).build(); */
		}
		
		@Override
		public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
			this.recyclerView = null;
		}
		
		@Override @NonNull
		public ConversationManager.ConversationInfo.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			//Returning the view holder
			return new ConversationManager.ConversationInfo.ItemViewHolder(LayoutInflater.from(Conversations.this).inflate(R.layout.listitem_conversation, parent, false));
		}
		
		@Override
		public void onBindViewHolder(@NonNull ConversationManager.ConversationInfo.ItemViewHolder holder, int position) {
			//Getting the conversation info
			ConversationManager.ConversationInfo conversationInfo = filteredItems.get(position);
			
			//Creating the view
			conversationInfo.bindView(holder, Conversations.this);
			
			//Setting the view's click listeners
			holder.itemView.setOnClickListener(view -> {
				//Checking if action mode is active
				if(actionMode != null) {
					//Toggling the item's checked state
					actionModeCallbacks.onItemCheckedStateChanged(conversationInfo, !isSelectedActionMode(conversationInfo));
					
					return;
				}
				
				//Launching the conversation activity
				//new Handler().postDelayed(() -> startActivity(new Intent(Conversations.this, Messaging.class).putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID())), 1000);
				startActivity(new Intent(Conversations.this, Messaging.class).putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID()));
			});
			holder.itemView.setOnLongClickListener(view -> {
				//Returning if action mode isn't running
				if(actionMode != null) return false;
				
				//Starting the action mode
				startActionMode();
				
				//Toggling the item's checked state
				actionModeCallbacks.onItemCheckedStateChanged(conversationInfo, !isSelectedActionMode(conversationInfo));
				/* RecyclerView.ViewHolder viewHolder = conversationInfo.getViewHolder();
				if(viewHolder != null) {
					viewHolder.itemView.cancelPendingInputEvents();
				} */
				
				return true;
			});
			
			//Setting the view source
			conversationInfo.setViewHolderSource(new Constants.ViewHolderSourceImpl<>(recyclerView, conversationInfo.getLocalID()));
			/* LinearLayoutManager layout = (LinearLayoutManager) recyclerView.getLayoutManager();
			conversationInfo.setViewSource(() -> layout.findViewByPosition(filteredItems.indexOf(conversationInfo))); */
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
				//Skipping non-listed conversations
				if(conversationInfo.isArchived() != listingArchived) continue;
				
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
	
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		//Checking if the request code is contacts access
		if(requestCode == Constants.permissionReadContacts) {
			//Checking if the result is a success
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				//Hiding the contact request info bar
				infoBarContacts.hide();
			}
			//Otherwise checking if the result is a denial
			else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
				//Showing a snackbar
				Snackbar.make(findViewById(R.id.root), R.string.message_permissionrejected, Snackbar.LENGTH_LONG)
						.setAction(R.string.screen_settings, view -> {
							//Opening the application settings
							Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							intent.setData(Uri.parse("package:" + getPackageName()));
							startActivity(intent);
						})
						.show();
			}
		}
	}
	
	@Override
	public void onStop() {
		//Calling the super method
		super.onStop();
		
		//Removing the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.unregisterReceiver(clientConnectionResultBroadcastReceiver);
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
				if(conversationsBasePlugin.currentState == ConversationsBase.stateReady) setAppBarState(appBarStateSearch);
				
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
						.setTitle(R.string.action_sendfeedback)
						.setMessage(R.string.dialog_feedback_message)
						.setNeutralButton(R.string.dialog_feedback_email, (dialog, which) -> {
							//Creating the intent
							Intent intent = new Intent(Intent.ACTION_SENDTO);
							intent.setData(Uri.parse("mailto:"));
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.feedbackEmail});
							intent.putExtra(Intent.EXTRA_SUBJECT, "AirMessage Feedback");
							intent.putExtra(Intent.EXTRA_TEXT, "\r\n\r\n" +
									"---------- DEVICE INFORMATION ----------" + "\r\n" +
									"Device model: " + Build.MODEL + "\r\n" +
									"Android version: " + Build.VERSION.RELEASE + "\r\n" +
									"Client version: " + BuildConfig.VERSION_NAME + "\r\n" +
									"AM communications version: " + SharedValues.mmCommunicationsVersion + '.' + SharedValues.mmCommunicationsSubVersion);
							//intent.setType("message/rfc822");
							
							//Launching the intent
							if(intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
							else Toast.makeText(this, R.string.message_intenterror_email, Toast.LENGTH_SHORT).show();
						})
						.setPositiveButton(R.string.dialog_feedback_googleplus, (dialog, which) -> {
							//Creating the intent
							Intent intent = new Intent(Intent.ACTION_VIEW, Constants.googlePlusCommunityAddress);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							
							//Launching the intent
							if(intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
							else Toast.makeText(this, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show();
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
	
	void showServerWarning(int reason) {
		switch(reason) {
			case ConnectionService.intentResultCodeInternalException:
				infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(R.string.message_serverstatus_internalexception)));
				infoBarConnection.setButton(R.string.action_retry, view -> {
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
			case ConnectionService.intentResultCodeBadRequest:
				infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(R.string.message_serverstatus_badrequest)));
				infoBarConnection.setButton(R.string.action_retry, view -> {
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
			case ConnectionService.intentResultCodeClientOutdated:
				infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(R.string.message_serverstatus_clientoutdated)));
				infoBarConnection.setButton(R.string.action_update, view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()))));
				break;
			case ConnectionService.intentResultCodeServerOutdated:
				infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(R.string.message_serverstatus_serveroutdated)));
				infoBarConnection.setButton(R.string.screen_help, view -> startActivity(new Intent(Intent.ACTION_VIEW, Constants.serverUpdateAddress)));
				break;
			case ConnectionService.intentResultCodeUnauthorized:
				infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(R.string.message_serverstatus_authfail)));
				infoBarConnection.setButton(R.string.action_reconfigure, view -> startActivity(new Intent(Conversations.this, ServerSetup.class)));
				break;
			case ConnectionService.intentResultCodeConnection:
				infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(R.string.message_serverstatus_noconnection)));
				infoBarConnection.setButton(R.string.action_reconnect, view -> {
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
				infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(R.string.message_serverstatus_unknown)));
				infoBarConnection.setButton(R.string.action_retry, view -> {
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
		
		//Showing the info bar
		infoBarConnection.show();
	}
	
	void hideServerWarning() {
		infoBarConnection.hide();
	}
	
	void startActionMode() {
		//Starting action mode
		actionMode = startSupportActionMode(actionModeCallbacks);
	}
	
	void restoreActionMode() {
		//Returning if the action mode is already set up or there are no items selected
		if(actionMode != null || viewModel.actionModeSelections.isEmpty()) return;
		
		//Calculating the values
		for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
			//Skipping the remainder of the iteration if the conversation is not selected
			if(!isSelectedActionMode(conversationInfo)) continue;
			
			//Adding to the values
			actionModeCallbacks.selectedConversations++;
			if(conversationInfo.isMuted()) actionModeCallbacks.mutedConversations++;
			else actionModeCallbacks.nonMutedConversations++;
			if(conversationInfo.isArchived()) actionModeCallbacks.archivedConversations++;
			else actionModeCallbacks.nonArchivedConversations++;
		}
		
		//Starting the action mode
		startActionMode();
		actionModeCallbacks.updateActionModeContext();
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
			actionBar.setTitle(R.string.screen_archived);
		} else {
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(R.string.app_name);
		}
		
		//Updating the list adapter
		conversationsBasePlugin.updateList(false);
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
	
	public static class ActivityViewModel extends ViewModel {
		final List<Long> actionModeSelections = new ArrayList<>();
		
		public ActivityViewModel() {
			final WeakReference<List<Long>> actionModeSelectionsReference = new WeakReference<>(actionModeSelections);
			ConversationManager.ConversationInfo.setSelectionSource(id -> {
				List<Long> selections = actionModeSelectionsReference.get();
				return selections != null && selections.contains(id);
			});
		}
	}
	
	private class CountingActionModeCallback implements ActionMode.Callback {
		//Creating the selected conversations variable
		int selectedConversations = 0;
		int mutedConversations = 0;
		int nonMutedConversations = 0;
		int archivedConversations = 0;
		int nonArchivedConversations = 0;
		
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			MenuInflater inflater = actionMode.getMenuInflater();
			inflater.inflate(R.menu.menu_conversation_actionmode, menu);
			
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			//Checking if the item is the mute button
			if(menuItem.getItemId() == R.id.action_mute) {
				//Creating the updated conversation list
				final ArrayList<Long> updatedConversations = new ArrayList<>();
				
				//Looping through all conversations
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!isSelectedActionMode(conversationInfo)) continue;
					
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
					for(long conversationID : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationID, contentValues);
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
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!isSelectedActionMode(conversationInfo)) continue;
					
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
					for(long conversationID : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationID, contentValues);
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
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!isSelectedActionMode(conversationInfo)) continue;
					
					//Archiving the conversation
					conversationInfo.setArchived(true);
					
					//Adding the conversation to the updated conversations list
					updatedConversations.add(conversationInfo);
				}
				
				//Checking if there are conversations to update in the database
				if(!updatedConversations.isEmpty()) {
					//Updating the list
					conversationsBasePlugin.updateList(false);
					
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					
					//Setting the archived value
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
					
					//Updating the conversations
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), contentValues);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
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
							DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), undoContentValues);
						}
						
						//Updating the list
						conversationsBasePlugin.updateList(false);
					}).show();
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
				for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
					//Skipping the remainder of the iteration if the conversation is not selected
					if(!isSelectedActionMode(conversationInfo)) continue;
					
					//Unarchiving the conversation
					conversationInfo.setArchived(false);
					
					//Adding the conversation to the updated conversations list
					updatedConversations.add(conversationInfo);
				}
				
				//Checking if there are conversations to update in the database
				if(!updatedConversations.isEmpty()) {
					//Updating the list
					conversationsBasePlugin.updateList(false);
					
					//Creating the content values
					ContentValues contentValues = new ContentValues();
					
					//Setting the archived value
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, false);
					
					//Updating the conversations
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), contentValues);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationunarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
						//Creating the content values
						ContentValues undoContentValues = new ContentValues();
						undoContentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
						
						//Iterating over the conversations
						for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) {
							//Archiving the conversation
							conversationInfo.setArchived(true);
							
							//Updating the conversations
							DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), undoContentValues);
						}
						
						//Updating the list
						conversationsBasePlugin.updateList(false);
					}).show();
				}
				
				//Finishing the action mode
				actionMode.finish();
				
				//Returning true
				return true;
			}
			//Checking if the item is the delete button
			else if(menuItem.getItemId() == R.id.action_delete) {
				//Displaying a dialog
				new AlertDialog.Builder(Conversations.this)
						//Setting the message
						.setMessage(getResources().getQuantityString(R.plurals.message_confirm_deleteconversation, selectedConversations))
						//Setting the button
						.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
						.setPositiveButton(R.string.action_delete, (dialog, which) -> {
							//Creating the to remove list
							ArrayList<ConversationManager.ConversationInfo> toRemove = new ArrayList<>();
							
							//Marking all selected conversations for removal
							for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations)
								if(isSelectedActionMode(conversationInfo)) toRemove.add(conversationInfo);
							
							//Deleting the conversations
							for(ConversationManager.ConversationInfo conversationInfo : toRemove) conversationInfo.delete(Conversations.this);
							
							//Updating the conversation activity list
							LocalBroadcastManager.getInstance(Conversations.this).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
							
							//Updating the list
							conversationsBasePlugin.updateList(false);
							
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
			//Invalidating the action mode
			Conversations.this.actionMode = null;
			
			//Deselecting all items
			List<Long> selectionsCopy = new ArrayList<>(viewModel.actionModeSelections);
			viewModel.actionModeSelections.clear();
			for(ConversationManager.ConversationInfo conversationInfo : conversationsBasePlugin.conversations) {
				if(!selectionsCopy.contains(conversationInfo.getLocalID())) continue;
				conversationInfo.updateSelected();
			}
			
			//Resetting the selection counts
			selectedConversations = mutedConversations = nonMutedConversations = archivedConversations = nonArchivedConversations = 0;
		}
		
		public void onItemCheckedStateToggled(ConversationManager.ConversationInfo item) {
			onItemCheckedStateChanged(item, !isSelectedActionMode(item));
		}
		
		public void onItemCheckedStateChanged(ConversationManager.ConversationInfo item, boolean checked) {
			//Setting the item's checked state
			if(checked) viewModel.actionModeSelections.add(item.getLocalID());
			else viewModel.actionModeSelections.remove(item.getLocalID());
			item.updateSelected();
			
			//Updating the selected conversations
			int value = checked ? 1 : -1;
			selectedConversations += value;
			if(item.isMuted()) mutedConversations += value;
			else nonMutedConversations += value;
			if(item.isArchived()) archivedConversations += value;
			else nonArchivedConversations += value;
			
			//Updating the context
			updateActionModeContext();
			
			//Finishing the action mode if there are no more items selected
			if(selectedConversations == 0) actionMode.finish();
		}
		
		public void updateActionModeContext() {
			//Updating the title
			actionMode.setTitle(getResources().getQuantityString(R.plurals.message_selectioncount, selectedConversations, selectedConversations));
			
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
	}
}