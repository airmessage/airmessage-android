package me.tagavari.airmessage;

import android.animation.ValueAnimator;
import androidx.appcompat.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;

public class Conversations extends AppCompatCompositeActivity {
	//Creating the constants
	private static final int permissionRequestContacts = 0;
	
	//Creating the plugin values
	private ConversationsBase conversationsBasePlugin;
	private PluginMessageBar pluginMessageBar;
	
	//Creating the view model and info bar values
	private ActivityViewModel viewModel;
	private PluginMessageBar.InfoBar infoBarConnection, infoBarContacts, infoBarSystemUpdate;
	
	//Creating the menu values
	private MenuItem menuItemSearch = null;
	
	//Creating the view values
	private AppBarLayout appBarLayout;
	private Toolbar toolbar;
	private ViewGroup groupBarSearch;
	private EditText editTextBarSearch;
	private ImageButton buttonBarSearchClear;
	private FloatingActionButton floatingActionButton;
	
	private ViewGroup groupSearch;
	private SearchRecyclerAdapter searchRecyclerAdapter = null;
	
	private int currentToolbarColor;
	private int currentStatusBarColor;
	
	//Creating the listener values
	private ActionMode actionMode = null;
	private final CountingActionModeCallback actionModeCallbacks = new CountingActionModeCallback();
	private final TextWatcher searchTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
		}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Setting the clear button state
			buttonBarSearchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
			
			//Updating the search filter
			updateSearchFilter(s.toString());
		}
		
		@Override
		public void afterTextChanged(Editable s) {
		
		}
	};
	private final BroadcastReceiver clientConnectionResultBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Returning if this is not the latest launch
			if(ConnectionService.compareLaunchID(intent.getByteExtra(Constants.intentParamLaunchID, (byte) -1)) != 0) return;
			
			int state = intent.getIntExtra(Constants.intentParamState, -1);
			if(state == ConnectionService.stateDisconnected) {
				int code = intent.getIntExtra(Constants.intentParamCode, -1);
				showServerWarning(code);
				infoBarSystemUpdate.hide();
			} else {
				hideServerWarning();
				if(state == ConnectionService.stateConnected) {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService != null && connectionService.getActiveCommunicationsVersion() < ConnectionService.mmCommunicationsVersion) infoBarSystemUpdate.show();
					else infoBarSystemUpdate.hide();
				}
			}
		}
	};
	
	public Conversations() {
		//Setting the plugins
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
		
		//Getting the views
		appBarLayout = findViewById(R.id.appbar);
		toolbar = findViewById(R.id.toolbar);
		groupBarSearch = findViewById(R.id.layout_search);
		editTextBarSearch = findViewById(R.id.search_edittext);
		buttonBarSearchClear = findViewById(R.id.search_buttonclear);
		
		floatingActionButton = findViewById(R.id.fab);
		
		groupSearch = findViewById(R.id.viewgroup_search);
		
		//Setting the plugin views
		conversationsBasePlugin.setViews(findViewById(R.id.list), findViewById(R.id.syncview_progress), findViewById(R.id.no_conversations));
		pluginMessageBar.setParentView(findViewById(R.id.infobar_container));
		
		//Getting the colors
		currentToolbarColor = getResources().getColor(R.color.colorPrimary, null);
		currentStatusBarColor = getResources().getColor(R.color.colorPrimaryDark, null);
		
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
			
			//Setting the search recycler adapter
			searchRecyclerAdapter = new SearchRecyclerAdapter(conversationsBasePlugin.conversations);
			((RecyclerView) findViewById(R.id.list_search)).setAdapter(searchRecyclerAdapter);
			
			//Updating the search applicability
			//updateSearchApplicability();
		});
		conversationsBasePlugin.addUpdateListListener(() -> {
			//Updating the no conversations text
			TextView noConversations = findViewById(R.id.no_conversations);
			if(viewModel.listingArchived) noConversations.setText(R.string.message_blankstate_conversations_archived);
			else noConversations.setText(R.string.message_blankstate_conversations);
			
			//Updating the search list adapter
			if(viewModel.isSearching && searchRecyclerAdapter != null) searchRecyclerAdapter.updateAndFilter();
		});
		editTextBarSearch.addTextChangedListener(searchTextWatcher);
		buttonBarSearchClear.setOnClickListener(view -> editTextBarSearch.setText(""));
		
		//Creating the info bars
		infoBarConnection = pluginMessageBar.create(R.drawable.disconnection, null);
		infoBarContacts = pluginMessageBar.create(R.drawable.contacts, getResources().getString(R.string.message_permissiondetails_contacts_listing));
		infoBarContacts.setButton(R.string.action_enable, view -> requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, permissionRequestContacts));
		infoBarSystemUpdate = pluginMessageBar.create(R.drawable.update, getResources().getString(R.string.message_serverupdate));
		
		//Restoring the state
		restoreListingArchivedState();
		restoreSearchState();
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
			if(connectionService.getCurrentState() == ConnectionService.stateConnected && connectionService.getActiveCommunicationsVersion() < ConnectionService.mmCommunicationsVersion) infoBarSystemUpdate.show();
			else infoBarSystemUpdate.hide();
		}
		
		//Updating the contacts info bar
		if(MainApplication.canUseContacts(this)) infoBarContacts.hide();
		else infoBarContacts.show();
		
		//Hiding the search view if the state is syncing
		if(conversationsBasePlugin.currentState != ConversationsBase.stateReady) setSearchState(false, true);
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
		
		//Configuring the search widget
		menuItemSearch = menu.findItem(R.id.action_search);
		menuItemSearch.setVisible(!viewModel.listingArchived);
		
		//Returning true
		return true;
	}
	
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
		
		@Override
		@NonNull
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
				if(conversationInfo.isArchived() != viewModel.listingArchived) continue;
				
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
		if(requestCode == permissionRequestContacts) {
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
				/* if(currentAppBarState == appBarStateSearch) {
					//Setting the state to normal
					setAppBarState(appBarStateDefault);
				}
				//Checking if the "archived" view is active
				else */
				if(viewModel.listingArchived) {
					//Exiting the archived state
					setArchivedListingState(false);
				} else {
					//Finishing the activity
					finish();
				}
				return true;
			case R.id.action_search:
				//Setting the state to search
				if(conversationsBasePlugin.currentState != ConversationsBase.stateIdle && conversationsBasePlugin.currentState != ConversationsBase.stateSyncing) {
					//setAppBarState(appBarStateSearch);
					setSearchState(true, true);
				}
				
				return true;
			case R.id.action_archived: //Archived conversations
				//Starting the archived conversations activity
				//startActivity(new Intent(this, ConversationsArchived.class));
				setArchivedListingState(!viewModel.listingArchived);
				
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
									"AM communications version: " + ConnectionService.mmCommunicationsVersion + '.' + ConnectionService.mmCommunicationsSubVersion);
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
		if(viewModel.isSearching) {
			//Closing the search
			setSearchState(false, true);
		}
		//Checking if the "archived" view is active
		else if(viewModel.listingArchived) {
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
	
	void setSearchState(boolean enabled, boolean updateContext) {
		//Returning if the requested state matches the current state
		if(viewModel.isSearching == enabled) return;
		
		//Setting the search state
		viewModel.isSearching = enabled;
		
		long duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
		if(enabled) {
			//Hiding the toolbar
			if(updateContext) toolbar.animate().alpha(0).setDuration(duration).withEndAction(() -> toolbar.setVisibility(View.GONE));
			
			//Showing the search group
			groupBarSearch.animate().alpha(1).setDuration(duration).withStartAction(() -> {
				groupBarSearch.setVisibility(View.VISIBLE);
				
				//Clearing the text field
				if(editTextBarSearch.getText().length() > 0) editTextBarSearch.setText("");
				
				//Opening the keyboard
				editTextBarSearch.requestFocus();
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editTextBarSearch, InputMethodManager.SHOW_IMPLICIT);
			}).withEndAction(() -> groupBarSearch.setClickable(true));
			
			//Hiding the FAB
			if(updateContext) floatingActionButton.hide();
		} else {
			//Showing the toolbar
			if(updateContext) toolbar.animate().alpha(1).setDuration(duration).withStartAction(() -> toolbar.setVisibility(View.VISIBLE));
			
			//Hiding the search group
			groupBarSearch.animate().alpha(0).setDuration(duration).withStartAction(() -> {
				groupBarSearch.setClickable(false);
				
				//Closing the keyboard
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editTextBarSearch.getWindowToken(), 0);
			}).withEndAction(() -> groupBarSearch.setVisibility(View.GONE));
			
			//Showing the FAB
			if(updateContext) floatingActionButton.show();
			
			//Clearing the search query
			updateSearchFilter("");
		}
	}
	
	void restoreSearchState() {
		//Returning if the search is not active
		if(!viewModel.isSearching) return;
		
		//Hiding the toolbar
		toolbar.setVisibility(View.GONE);
		toolbar.setAlpha(0);
		
		//Showing the views
		groupBarSearch.setVisibility(View.VISIBLE);
		groupBarSearch.setAlpha(1);
		editTextBarSearch.requestFocus();
		groupSearch.setVisibility(View.VISIBLE);
		
		//Updating the close button
		buttonBarSearchClear.setVisibility(editTextBarSearch.getText().length() > 0 ? View.VISIBLE : View.GONE);
		
		//Hiding the FAB
		floatingActionButton.hide();
	}
	
	public void onCloseSearchClicked(View view) {
		setSearchState(false, true);
	}
	
	private void updateSearchFilter(String query) {
		//Returning if the search is not active
		if(!query.isEmpty() && !viewModel.isSearching) return;
		
		//Updating the recycler adapter
		if(searchRecyclerAdapter != null) searchRecyclerAdapter.updateFilterText(query);
		
		//Setting the search group state
		groupSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
	}
	
	private class SearchRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the reference values
		private static final int itemTypeSubheader = -1;
		private static final int itemTypeConversation = 0;
		private static final int itemTypeMessage = 1;
		
		//Creating the list values
		private final List<ConversationManager.ConversationInfo> conversationSourceList;
		
		private final List<ConversationManager.ConversationInfo> conversationFilterList = new ArrayList<>();
		private int conversationFilterListMemberSeparator = 0;
		private final List<ConversationManager.MessageInfo> messageFilterList = new ArrayList<>();
		
		SearchRecyclerAdapter(List<ConversationManager.ConversationInfo> conversationList) {
			//Setting the list
			conversationSourceList = conversationList;
		}
		
		private class SubheaderViewHolder extends RecyclerView.ViewHolder {
			final TextView label;
			
			private SubheaderViewHolder(View itemView) {
				super(itemView);
				label = (TextView) itemView;
			}
		}
		
		void updateAndFilter() {
			updateFilterText(lastFilterText);
		}
		
		private int searchRequestID = 0;
		private String lastFilterText = "";
		void updateFilterText(String text) {
			//Setting the search request ID
			int currentRequestID = ++searchRequestID;
			
			//Setting the last filter text
			lastFilterText = text;
			
			//Clearing the filter lists
			conversationFilterList.clear();
			conversationFilterListMemberSeparator = 0;
			messageFilterList.clear();
			
			//Returning if there is no filter text
			if(text.isEmpty()) {
				notifyDataSetChanged();
				return;
			}
			
			//Iterating over the conversations
			conversationLoop:
			for(ConversationManager.ConversationInfo conversationInfo : conversationSourceList) {
				//Filtering the conversation based on its static name
				if(conversationInfo.getStaticTitle() != null && searchString(conversationInfo.getStaticTitle(), text)) {
					conversationFilterList.add(conversationFilterListMemberSeparator++, conversationInfo);
					continue conversationLoop;
				}
				
				//Filtering the conversation based on its members
				for(ConversationManager.MemberInfo member : conversationInfo.getConversationMembers()) {
					if(searchString(Constants.normalizeAddress(member.getName()), text)) {
						conversationFilterList.add(conversationInfo);
						continue conversationLoop;
					}
					MainApplication.getInstance().getUserCacheHelper().getUserInfo(Conversations.this, member.getName(), new SearchRecyclerAdapterUserFetchResult(currentRequestID, this, conversationInfo));
				}
			}
			
			//Updating the list
			notifyDataSetChanged();
		}
		
		void processUserResult(int requestID, String userName, ConversationManager.ConversationInfo conversationInfo, boolean wasTasked) {
			//Returning if the request IDs don't match or the user doesn't match
			if(requestID != searchRequestID || !searchString(userName, lastFilterText)) return;
			
			//Adding the conversation
			conversationFilterList.add(conversationInfo);
			
			//Updating the list if the operation was tasked
			if(wasTasked) notifyItemInserted(conversationFilterList.size() - 1);
		}
		
		private boolean searchString(String target, String query) {
			return Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE).matcher(target).find();
		}
		
		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			switch(viewType) {
				case itemTypeSubheader:
					return new SubheaderViewHolder(getLayoutInflater().inflate(R.layout.listitem_subheader, parent, false));
				case itemTypeConversation:
					return new ConversationManager.ConversationInfo.ItemViewHolder(getLayoutInflater().inflate(R.layout.listitem_conversation, parent, false));
				case itemTypeMessage:
					return null;
				default:
					throw new IllegalArgumentException("Invalid view type requested: " + viewType);
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			switch(getItemViewType(position)) {
				case itemTypeSubheader:
					//Setting the header label text
					if(position == 0 && !conversationFilterList.isEmpty()) ((SubheaderViewHolder) holder).label.setText(R.string.part_conversations);
					else ((SubheaderViewHolder) holder).label.setText(R.string.part_messages);
					break;
				case itemTypeConversation: {
					//Getting the conversation
					ConversationManager.ConversationInfo conversation = conversationFilterList.get(position - 1);
					
					//Passing the view to the conversation to bind
					conversation.bindViewOnce((ConversationManager.ConversationInfo.ItemViewHolder) holder, Conversations.this);
					
					//Setting the view's click listener
					holder.itemView.setOnClickListener(view -> startActivity(new Intent(Conversations.this, Messaging.class).putExtra(Constants.intentParamTargetID, conversation.getLocalID())));
					
					break;
				}
				case itemTypeMessage:
					break;
			}
		}
		
		@Override
		public int getItemCount() {
			int count = 0;
			if(!conversationFilterList.isEmpty()) count += conversationFilterList.size() + 1;
			if(!messageFilterList.isEmpty()) count += messageFilterList.size() + 1;
			return count;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(position == 0) return itemTypeSubheader; //The first position will always be a header
			if(conversationFilterList.isEmpty()) return itemTypeMessage; //If there are no conversations listed and the position is not 0, the item must be a message
			if(position - 1 < conversationFilterList.size()) return itemTypeConversation; //If the position is not 0 and is less than the size of the conversation list, it must be a conversation
			if(position - 1 == conversationFilterList.size()) return itemTypeSubheader; //If the position is the same size as the conversation list, it reaches the first entry of of the "messages" section, and is therefore the messages section subheader
			return itemTypeMessage; //Otherwise, the item is a message
		}
	}
	
	private static class SearchRecyclerAdapterUserFetchResult extends UserCacheHelper.UserFetchResult {
		//Creating the request values
		private final int requestID;
		
		//Creating the reference values
		private final WeakReference<SearchRecyclerAdapter> adapterReference;
		private final WeakReference<ConversationManager.ConversationInfo> conversationReference;
		
		SearchRecyclerAdapterUserFetchResult(int requestID, SearchRecyclerAdapter adapter, ConversationManager.ConversationInfo conversationInfo) {
			this.requestID = requestID;
			adapterReference = new WeakReference<>(adapter);
			conversationReference = new WeakReference<>(conversationInfo);
		}
		
		@Override
		void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
			//Returning if the user info is invalid
			if(userInfo == null || userInfo.getContactName() == null) return;
			
			//Getting the references
			SearchRecyclerAdapter adapter = adapterReference.get();
			if(adapter == null) return;
			
			ConversationManager.ConversationInfo conversationInfo = conversationReference.get();
			if(conversationInfo == null) return;
			
			//Giving the adapter the information
			adapter.processUserResult(requestID, userInfo.getContactName(), conversationInfo, wasTasked);
		}
	}
	
	void setArchivedListingState(boolean state) {
		//Returning if the current state matches the requested state
		if(viewModel.listingArchived == state) return;
		
		//Setting the new state
		viewModel.listingArchived = state;
		
		//Updating the menu item
		menuItemSearch.setVisible(!state);
		
		//Animating the app bar color
		{
			int colorPrimary = getResources().getColor(R.color.colorPrimary, null);
			int colorPrimaryDark = getResources().getColor(R.color.colorPrimaryDark, null);
			int colorArchived = getResources().getColor(R.color.colorArchived, null);
			int colorArchivedDark = getResources().getColor(R.color.colorArchivedDark, null);
			
			if(state) animateAppBarColor(colorPrimary, colorArchived, colorPrimaryDark, colorArchivedDark, getResources().getInteger(android.R.integer.config_mediumAnimTime));
			else animateAppBarColor(colorArchived, colorPrimary, colorArchivedDark, colorPrimaryDark, getResources().getInteger(android.R.integer.config_mediumAnimTime));
		}
		
		//Configuring the toolbar
		ActionBar actionBar = getSupportActionBar();
		if(state) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.screen_archived);
		} else {
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(R.string.app_name);
		}
		
		//Setting the fab
		if(state) floatingActionButton.hide();
		else floatingActionButton.show();
		
		//Updating the list adapter
		conversationsBasePlugin.updateList(false);
		//((ListAdapter) listView.getAdapter()).filterAndUpdate();
	}
	
	void animateAppBarColor(int targetToolbarColor, int targetStatusBarColor, int duration) {
		animateAppBarColor(currentToolbarColor, targetToolbarColor, currentStatusBarColor, targetStatusBarColor, duration);
	}
	
	void animateAppBarColor(int startToolbarColor, int targetToolbarColor, int startStatusBarColor, int targetStatusBarColor, int duration) {
		//Animating the app bar
		ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
		anim.addUpdateListener(animation -> {
			//Getting the position
			float position = animation.getAnimatedFraction();
			
			//Tinting the app bar
			{
				int blended = ColorUtils.blendARGB(startToolbarColor, targetToolbarColor, position);
				toolbar.setBackgroundColor(blended);
				appBarLayout.setBackgroundColor(blended);
			}
			
			//Tinting the status bar
			{
				int blended = ColorUtils.blendARGB(startStatusBarColor, targetStatusBarColor, position);
				getWindow().setStatusBarColor(blended);
			}
		});
		anim.setDuration(duration);
		anim.setInterpolator(new DecelerateInterpolator());
		anim.start();
		
		//Setting the colors
		currentToolbarColor = targetToolbarColor;
		currentStatusBarColor = targetStatusBarColor;
	}
	
	void setAppBarColor(int targetToolbarColor, int targetStatusBarColor) {
		//Tinting the app bar
		getSupportActionBar().setBackgroundDrawable(new ColorDrawable(targetToolbarColor));
		appBarLayout.setBackground(new ColorDrawable(targetToolbarColor));
		getWindow().setStatusBarColor(targetStatusBarColor);
		
		//Setting the colors
		currentToolbarColor = targetToolbarColor;
		currentStatusBarColor = targetStatusBarColor;
	}
	
	void restoreListingArchivedState() {
		//Returning if the state is not archived
		if(!viewModel.listingArchived) return;
		
		//Tinting the window
		int colorArchived = getResources().getColor(R.color.colorArchived, null);
		int colorArchivedDark = getResources().getColor(R.color.colorArchivedDark, null);
		setAppBarColor(colorArchived, colorArchivedDark);
		
		//Configuring the toolbar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.screen_archived);
		
		if(menuItemSearch != null) menuItemSearch.setVisible(false);
		
		//Hiding the FAB
		floatingActionButton.hide();
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
		boolean listingArchived = false;
		boolean isSearching = false;
		
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
		
		int oldToolbarColor;
		int oldStatusBarColor;
		
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			//Inflating the menu
			MenuInflater inflater = actionMode.getMenuInflater();
			inflater.inflate(R.menu.menu_conversation_actionmode, menu);
			
			//Closing the search
			setSearchState(false, false);
			
			//Hiding the toolbar
			toolbar.animate().alpha(0).withEndAction(() -> toolbar.setVisibility(View.INVISIBLE));
			
			//Recording the old colors
			oldToolbarColor = currentToolbarColor;
			oldStatusBarColor = currentStatusBarColor;
			
			//Animating the app bar
			int toolbarColor = getResources().getColor(R.color.colorContextualAppBar, null);
			int statusBarColor = getResources().getColor(R.color.colorContextualAppBarDark, null);
			animateAppBarColor(toolbarColor, statusBarColor, getResources().getInteger(android.R.integer.config_mediumAnimTime));
			
			//Hiding the FAB
			floatingActionButton.hide();
			
			//Returning true
			return true;
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
			
			//Showing the toolbar
			toolbar.setVisibility(View.VISIBLE);
			toolbar.animate().setStartDelay(50).alpha(1);
			
			//Animating the app bar
			animateAppBarColor(oldToolbarColor, oldStatusBarColor, getResources().getInteger(android.R.integer.config_mediumAnimTime));
			
			//Showing the FAB
			floatingActionButton.show();
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
					//Updating the conversations
					for(long conversationID : updatedConversations) DatabaseManager.getInstance().updateConversationMuted(conversationID, true);
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
					//Updating the conversations
					for(long conversationID : updatedConversations) DatabaseManager.getInstance().updateConversationMuted(conversationID, false);
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
					
					//Updating the conversations
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.getInstance().updateConversationArchived(conversationInfo.getLocalID(), true);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
						//Updating the conversations
						for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) {
							conversationInfo.setArchived(false);
							DatabaseManager.getInstance().updateConversationArchived(conversationInfo.getLocalID(), false);
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
					
					//Updating the conversations
					for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) DatabaseManager.getInstance().updateConversationArchived(conversationInfo.getLocalID(), false);
					
					//Creating a snackbar
					int affectedCount = updatedConversations.size();
					Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationunarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
						//Updating the conversations
						for(ConversationManager.ConversationInfo conversationInfo : updatedConversations) {
							conversationInfo.setArchived(true);
							DatabaseManager.getInstance().updateConversationArchived(conversationInfo.getLocalID(), true);
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