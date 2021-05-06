package me.tagavari.airmessage.activity;

import android.app.Application;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Pair;
import me.tagavari.airmessage.BuildConfig;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginConnectionService;
import me.tagavari.airmessage.compositeplugin.PluginMessageBar;
import me.tagavari.airmessage.compositeplugin.PluginQNavigation;
import me.tagavari.airmessage.compositeplugin.PluginRXDisposable;
import me.tagavari.airmessage.compositeplugin.PluginThemeUpdater;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.constants.ExternalLinkConstants;
import me.tagavari.airmessage.constants.VersionConstants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionState;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.fragment.FragmentSync;
import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.ConversationHelper;
import me.tagavari.airmessage.helper.ConversationPreviewHelper;
import me.tagavari.airmessage.helper.ErrorDetailsHelper;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.helper.PlatformHelper;
import me.tagavari.airmessage.helper.ShortcutHelper;
import me.tagavari.airmessage.helper.StringHelper;
import me.tagavari.airmessage.helper.ThemeHelper;
import me.tagavari.airmessage.helper.WindowHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.viewbinder.VBConversation;
import me.tagavari.airmessage.messaging.viewholder.VHConversationDetailed;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventConnection;
import me.tagavari.airmessage.redux.ReduxEventMassRetrieval;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.redux.ReduxEventTextImport;
import me.tagavari.airmessage.task.ConversationActionTask;
import me.tagavari.airmessage.util.DisposableViewHolder;
import me.tagavari.airmessage.util.ObjIntConsumer;
import me.tagavari.airmessage.util.ReplaceInsertResult;
import me.tagavari.airmessage.util.TransferredConversation;

public class Conversations extends AppCompatCompositeActivity {
	//Creating the constants
	public static final String intentExtraArchived = "archived";
	
	private static final String savedInstanceStateArchived = "archived";
	
	private static final int permissionRequestContacts = 0;
	private static final String keyFragmentSync = "fragment_sync";
	
	private static final int activityResultPlayServices = 0;
	
	private static final int conversationPayloadPreview = 0;
	private static final int conversationPayloadTitle = 1;
	private static final int conversationPayloadMember = 2;
	private static final int conversationPayloadMuted = 3;
	private static final int conversationPayloadUnread = 4;
	private static final int conversationPayloadSelection = 5;
	
	private static final long timeUpdateHandlerDelay = 60 * 1000; //1 minute
	
	//Creating the static values
	private static final List<WeakReference<Conversations>> foregroundActivities = new ArrayList<>();
	
	//Creating the parameter values
	private boolean isViewArchived;
	
	//Creating the plugin values
	private final PluginMessageBar pluginMessageBar;
	private final PluginRXDisposable pluginRXD;
	private final PluginConnectionService pluginCS;
	
	//Creating the view model and info bar values
	private ActivityViewModel viewModel;
	private PluginMessageBar.InfoBar infoBarConnection, infoBarContacts, infoBarServerUpdate, infoBarSecurityUpdate;
	
	//Creating the menu values
	private MenuItem menuItemMarkAllRead = null;
	
	//Creating the view values
	private AppBarLayout viewAppBar;
	private Toolbar viewToolbar;
	private FloatingActionButton viewFAB;
	private RecyclerView viewMainList;
	private View viewLoading;
	private ViewGroup viewGroupBlank;
	
	private ViewGroup viewGroupSearch;
	private RecyclerView viewSearchList;
	private ViewGroup viewGroupToolbarSearch;
	private EditText viewSearchField;
	private ImageButton viewSearchClear;
	
	private ViewGroup viewGroupSync;
	private LinearProgressIndicator progressBarSync;
	
	private ViewGroup viewGroupError;
	
	private ConversationRecyclerAdapter conversationRecyclerAdapter = null;
	private SearchRecyclerAdapter searchRecyclerAdapter = null;
	
	private ActionMode actionMode = null;
	private final CountingActionModeCallback actionModeCallbacks = new CountingActionModeCallback();
	
	private final TextWatcher searchTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
		}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Setting the clear button state
			viewSearchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
			
			//Updating the search filter
			updateSearchFilter(s.toString());
		}
		
		@Override
		public void afterTextChanged(Editable s) {
		
		}
	};
	
	//Creating the timer values
	private final Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
	private Runnable timeUpdateHandlerRunnable = new Runnable() {
		@Override
		public void run() {
			if(viewModel.stateLD.getValue() == ActivityViewModel.stateReady && conversationRecyclerAdapter != null) {
				conversationRecyclerAdapter.notifyItemRangeChanged(0, viewModel.conversationList.size(), conversationPayloadPreview);
			}
			
			//Running again
			timeUpdateHandler.postDelayed(this, timeUpdateHandlerDelay);
		}
	};
	
	//Creating the state values
	private int currentState = -1;
	
	public Conversations() {
		//Setting the plugins
		addPlugin(pluginMessageBar = new PluginMessageBar());
		addPlugin(pluginRXD = new PluginRXDisposable());
		addPlugin(pluginCS = new PluginConnectionService());
		addPlugin(new PluginQNavigation());
		addPlugin(new PluginThemeUpdater());
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Checking if the user needs to configure their server connection
		if(!SharedPreferencesManager.isConnectionConfigured(this)) {
			//Creating the intent
			Intent launchOnboarding = new Intent(this, Onboarding.class);
			
			//Launching the intent
			startActivity(launchOnboarding);
			
			//Finishing the current activity
			finish();
			
			//Returning
			return;
		}
		
		//Reading the archived state
		isViewArchived = getIntent().getBooleanExtra(intentExtraArchived, false) || (savedInstanceState != null && savedInstanceState.getBoolean(savedInstanceStateArchived));
		
		//Setting the content view
		setContentView(R.layout.activity_conversations);
		
		//Enabling the toolbar
		setSupportActionBar(findViewById(R.id.toolbar));
		
		//Getting the view model
		viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
			@NonNull
			@Override
			public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
				return (T) new ActivityViewModel(getApplication(), isViewArchived);
			}
		}).get(ActivityViewModel.class);
		
		//Getting the views
		viewAppBar = findViewById(R.id.appbar);
		viewToolbar = findViewById(R.id.toolbar);
		viewFAB = findViewById(R.id.fab);
		viewMainList = findViewById(R.id.list);
		viewLoading = findViewById(R.id.loading_text);
		viewGroupBlank = findViewById(R.id.blankview);
		
		viewGroupSearch = findViewById(R.id.viewgroup_search);
		viewSearchList = findViewById(R.id.list_search);
		viewGroupToolbarSearch = findViewById(R.id.layout_search);
		viewSearchField = findViewById(R.id.search_edittext);
		viewSearchClear = findViewById(R.id.search_buttonclear);
		
		viewGroupSync = findViewById(R.id.syncview);
		progressBarSync = findViewById(R.id.syncview_progress);
		
		viewGroupError = findViewById(R.id.errorview);
		Button buttonError = findViewById(R.id.button_error);
		
		pluginMessageBar.setParentView(findViewById(R.id.infobar_container));
		
		//Setting the list padding
		PluginQNavigation.setViewForInsets(viewMainList, viewMainList, viewSearchList);
		
		viewMainList.post(() -> {
			//Calculating the required padding
			int padding = WindowHelper.calculatePaddingContentWidth(getResources(), viewMainList);
			
			//Applying the padding to the main list
			{
				ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewMainList.getLayoutParams();
				layoutParams.leftMargin = padding;
				layoutParams.rightMargin = padding;
				viewMainList.setLayoutParams(layoutParams);
			}
			
			//Applying the padding to the search results list
			{
				ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewSearchList.getLayoutParams();
				layoutParams.leftMargin = padding;
				layoutParams.rightMargin = padding;
				viewSearchList.setLayoutParams(layoutParams);
			}
		});
		
		//Configuring the AMOLED theme
		if(ThemeHelper.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Setting the status bar color
		PlatformHelper.updateChromeOSStatusBar(this);
		
		//Configuring the list
		//conversationsBasePlugin.listView.setOnItemClickListener(onListItemClickListener);
		//conversationsBasePlugin.listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		//conversationsBasePlugin.listView.setMultiChoiceModeListener(listMultiChoiceModeListener);
		
		//Setting the listeners
		viewFAB.setOnClickListener(view -> startActivity(new Intent(Conversations.this, NewMessage.class)));
		viewSearchField.addTextChangedListener(searchTextWatcher);
		viewSearchClear.setOnClickListener(view -> viewSearchField.setText(""));
		buttonError.setOnClickListener(view -> viewModel.loadConversations());
		
		//Configuring the app bar
		viewAppBar.setLiftable(false);
		viewMainList.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				viewAppBar.setLifted(((LinearLayoutManager) viewMainList.getLayoutManager()).findFirstCompletelyVisibleItemPosition() > 0);
			}
		});
		
		//Creating the banners
		infoBarConnection = pluginMessageBar.create(R.drawable.disconnection, null);
		infoBarContacts = pluginMessageBar.create(R.drawable.contacts, getResources().getString(R.string.message_permissiondetails_contacts_listing));
		infoBarContacts.setButton(R.string.action_enable, view -> requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, permissionRequestContacts));
		infoBarServerUpdate = pluginMessageBar.create(R.drawable.update, getResources().getString(R.string.message_serverupdate));
		infoBarSecurityUpdate = pluginMessageBar.create(R.drawable.lock_alert, getResources().getString(R.string.message_securityupdate));
		infoBarSecurityUpdate.setButton(R.string.action_resolve, view -> GoogleApiAvailability.getInstance().showErrorDialogFragment(this, viewModel.playServicesErrorCode.getValue(), activityResultPlayServices));
		
		//Configuring the normal / archived view
		if(isViewArchived) {
			getSupportActionBar().setTitle(getArchivedSpannable());
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			
			//Setting the blank state text
			((TextView) findViewById(R.id.blankview_title)).setText(R.string.message_blankstate_conversations_archived);
			((TextView) findViewById(R.id.blankview_description)).setText(R.string.message_blankstate_conversations_archived_description);
			
			//Hiding the FAB
			viewFAB.setVisibility(View.GONE);
		} else {
			getSupportActionBar().setTitle(getTitleSpannable());
		}
		
		//Subscribing to load state updates
		viewModel.stateLD.observe(this, this::updateUI);
		
		//Subscribing to mass retrieval updates
		viewModel.massRetrievalProgressLD.observe(this, progress -> {
			progressBarSync.setProgressCompat(progress, true);
		});
		viewModel.massRetrievalTotalLD.observe(this, total -> {
			progressBarSync.setMax(total);
		});
		
		//Subscribing to security provider updates
		viewModel.playServicesErrorCode.observe(this, error -> {
			if(error == null) infoBarSecurityUpdate.hide();
			else infoBarSecurityUpdate.show();
		});
		
		pluginRXD.activity().addAll(
				ReduxEmitterNetwork.getMessageUpdateSubject().subscribe(this::updateConversationList), //Subscribing to messaging updates
				ReduxEmitterNetwork.getTextImportUpdateSubject().subscribe(this::updateStateTextImport) //Subscribing to message import updates
		);
	}
	
	@Override
	public void onStart() {
		//Calling the super method
		super.onStart();
		
		pluginRXD.ui().addAll(
				ReduxEmitterNetwork.getConnectionStateSubject().subscribe(this::updateStateConnection), //Subscribing to connection state updates
				ReduxEmitterNetwork.getMassRetrievalUpdateSubject().subscribe(this::updateStateMassRetrieval) //Subscribing to mass retrieval updates
		);
	}
	
	@Override
	public void onResume() {
		//Calling the super method
		super.onResume();
		
		//Adding the phantom reference
		foregroundActivities.add(new WeakReference<>(this));
		
		//Checking if a sync is due
		if(pluginCS.isServiceBound()) {
			ConnectionManager connectionManager = pluginCS.getConnectionManager();
			if(connectionManager.isConnected() && connectionManager.isPendingSync()) {
				//Showing the sync screen
				promptSync();
			}
		}
		
		//Updating the contacts info bar
		if(MainApplication.canUseContacts(this)) infoBarContacts.hide();
		else infoBarContacts.show();
		
		//Updating the "mark as read" control
		updateMarkAllRead();
		
		//Clearing all message notifications
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		for(StatusBarNotification notification : notificationManager.getActiveNotifications()) {
			//Cancelling notifications with the 'notification' tag
			if(NotificationHelper.notificationTagMessage.equals(notification.getTag()) || NotificationHelper.notificationIDMessageSummary == notification.getId()) {
				notificationManager.cancel(notification.getTag(), notification.getId());
			}
		}
		/* if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			for(StatusBarNotification notification : notificationManager.getActiveNotifications()) {
				if(!MainApplication.notificationChannelMessage.equals(notification.getNotification().getChannelId())) continue;
				notificationManager.cancel(notification.getId());
			}
		} */
		
		//Starting the time update handler
		timeUpdateHandlerRunnable.run();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		//Iterating over the foreground conversations
		for(Iterator<WeakReference<Conversations>> iterator = foregroundActivities.iterator(); iterator.hasNext();) {
			//Getting the referenced activity
			Conversations activity = iterator.next().get();
			
			//Removing the reference if it is invalid
			if(activity == null) {
				iterator.remove();
				continue;
			}
			//Skipping the remainder of the iteration if the activity isn't this one
			else if(activity != this) continue;
			
			//Removing the reference (to this activity)
			iterator.remove();
			
			//Breaking from the loop
			break;
		}
		
		//Stopping the time update handler
		timeUpdateHandler.removeCallbacks(timeUpdateHandlerRunnable);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(viewMainList != null) viewMainList.setAdapter(null);
		if(viewSearchList != null) viewSearchList.setAdapter(null);
	}
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		
		savedInstanceState.putBoolean(savedInstanceStateArchived, isViewArchived);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Don't create an options menu if we're on the archived screen
		if(isViewArchived) {
			return true;
		}
		
		//Inflating the menu resource
		getMenuInflater().inflate(R.menu.menu_conversations, menu);
		
		//Updating the "mark all as read" option
		menuItemMarkAllRead = menu.findItem(R.id.action_markallread);
		updateMarkAllRead();
		
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == activityResultPlayServices) {
			//Refreshing the security provider
			viewModel.updateSecurityProvider();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		//Returning if there are no results
		if(grantResults.length == 0) return;
		
		//Checking if the request code is contacts access
		if(requestCode == permissionRequestContacts) {
			//Checking if the result is a success
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				//Hiding the contact request info bar
				infoBarContacts.hide();
				
				//Updating the display
				if(viewModel.stateLD.getValue() == ActivityViewModel.stateReady) {
					conversationRecyclerAdapter.notifyDataSetChanged();
				}
				
				//Starting the update listener
				MainApplication.getInstance().registerContactsListener();
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
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemID = item.getItemId();
		if(itemID == android.R.id.home) { //Up button
			finish();
			
			return true;
		}if(itemID == R.id.action_search) { //Search
			if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady) return true;
			
			//Setting the state to search
			setSearchState(true, true);
			
			return true;
		} else if(itemID == R.id.action_archived) { //Archived conversations
			//Starting the archived conversations activity
			startActivity(new Intent(this, Conversations.class).putExtra(intentExtraArchived, true));
			
			return true;
		} else if(itemID == R.id.action_markallread) { //Mark all as read
			if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady) return true;
			
			//Marking all items as read
			List<ConversationInfo> unreadConversationList = viewModel.conversationList.stream().filter(conversation -> conversation.getUnreadMessageCount() > 0).collect(Collectors.toList());
			ConversationActionTask.unreadConversations(unreadConversationList, 0).subscribe();
		/* } else if(itemID == R.id.action_blocked) { //Blocked contacts
			return true; */
		} else if(itemID == R.id.action_settings) { //Settings
			//Starting the settings activity
			startActivity(new Intent(this, Preferences.class));
			
			//Returning true
			return true;
		} else if(itemID == R.id.action_feedback) { //Send feedback
			String currentCommunicationsVersion;
			String serverSystemVersion;
			String serverSoftwareVersion;
			String proxyType;
			
			if(pluginCS.isServiceBound()) {
				currentCommunicationsVersion = pluginCS.getConnectionManager().getCommunicationsVersion();
				serverSystemVersion = StringHelper.defaultEmptyString(pluginCS.getConnectionManager().getServerSystemVersion(), "(none)");
				serverSoftwareVersion = StringHelper.defaultEmptyString(pluginCS.getConnectionManager().getServerSoftwareVersion(), "(none)");
			} else {
				currentCommunicationsVersion = "(none)";
				serverSystemVersion = "(none)";
				serverSoftwareVersion = "(none)";
			}
			
			if(SharedPreferencesManager.isConnectionConfigured(this)) {
				if(SharedPreferencesManager.getProxyType(this) == ProxyType.direct) {
					proxyType = "Direct";
				} else {
					proxyType = "Connect";
				}
			} else {
				proxyType = "(none)";
			}
			
			//Showing a dialog
			new MaterialAlertDialogBuilder(this)
					.setTitle(R.string.action_sendfeedback)
					.setMessage(R.string.dialog_feedback_message)
					.setNeutralButton(R.string.dialog_feedback_email, (dialog, which) -> {
						//Creating the intent
						Intent intent = new Intent(Intent.ACTION_SENDTO);
						intent.setData(Uri.parse("mailto:"));
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ExternalLinkConstants.feedbackEmail});
						intent.putExtra(Intent.EXTRA_SUBJECT, "AirMessage feedback");
						intent.putExtra(Intent.EXTRA_TEXT, "\r\n\r\n" +
								"---------- DEVICE INFORMATION ----------" + "\r\n" +
								"Device model: " + Build.MODEL + "\r\n" +
								"Android version: " + Build.VERSION.RELEASE + "\r\n" +
								"Client version: " + BuildConfig.VERSION_NAME + "\r\n" +
								"Communications version: " + currentCommunicationsVersion + " (target " + VersionConstants.targetCommVer + ")" + "\r\n" +
								"Proxy type: " + proxyType + "\r\n" +
								"Server system version: " + serverSystemVersion + "\r\n" +
								"Server software version: " + serverSoftwareVersion);
						
						//Launching the intent
						try {
							startActivity(intent);
						} catch(ActivityNotFoundException exception) {
							Toast.makeText(this, R.string.message_intenterror_email, Toast.LENGTH_SHORT).show();
						}
					})
					.setPositiveButton(R.string.dialog_feedback_community, (dialog, which) -> {
						//Creating the intent
						Intent intent = new Intent(Intent.ACTION_VIEW, ExternalLinkConstants.communityAddress);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						
						//Launching the intent
						try {
							startActivity(intent);
						} catch(ActivityNotFoundException exception) {
							Toast.makeText(this, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show();
						}
					})
					.create().show();
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onBackPressed() {
		//Exiting out of a search
		if(viewModel.isSearching) {
			setSearchState(false, true);
		}
		//Default behavior
		else {
			super.onBackPressed();
		}
	}
	
	/**
	 * Updates the UI view in response to a state change
	 */
	private void updateUI(int state) {
		//Ignoring if the state hasn't changed
		if(currentState == state) return;
		
		if(state == ActivityViewModel.stateReady) {
			//Setting the list adapters
			viewMainList.setAdapter(conversationRecyclerAdapter = new ConversationRecyclerAdapter(viewModel.conversationList));
			viewSearchList.setAdapter(searchRecyclerAdapter = new SearchRecyclerAdapter(viewModel.conversationList));
			
			//Restoring the action mode
			restoreActionMode();
			
			//Prompting the user to update their messages
			promptSync();
		} else {
			//Closing search
			setSearchState(false, true);
			
			//Closing action mode
			if(actionMode != null) actionMode.finish();
		}
		
		//Disabling the old state
		switch(currentState) {
			case ActivityViewModel.stateLoading:
				viewLoading.animate()
						.alpha(0)
						.withEndAction(() -> viewLoading.setVisibility(View.GONE));
				break;
			case ActivityViewModel.stateSyncing:
				viewGroupSync.animate()
						.alpha(0)
						.withEndAction(() -> viewGroupSync.setVisibility(View.GONE));
				break;
			case ActivityViewModel.stateReady:
				viewMainList.animate()
						.alpha(0)
						.withEndAction(() -> viewMainList.setVisibility(View.GONE));
				
				if(viewModel.conversationList.isEmpty()) {
					viewGroupBlank.animate()
							.alpha(0)
							.withEndAction(() -> viewGroupBlank.setVisibility(View.GONE));
				}
				break;
			case ActivityViewModel.stateError:
				viewGroupError.animate()
						.alpha(0)
						.withEndAction(() -> viewGroupError.setVisibility(View.GONE));
				break;
		}
		
		//Setting the new state
		currentState = state;
		
		//Enabling the new state
		switch(state) {
			case ActivityViewModel.stateLoading:
				viewLoading.animate().withStartAction(() -> viewLoading.setVisibility(View.VISIBLE)).alpha(1);
				break;
			case ActivityViewModel.stateSyncing:
				viewGroupSync.animate().withStartAction(() -> viewGroupSync.setVisibility(View.VISIBLE)).alpha(1);
				progressBarSync.setIndeterminate(true);
				break;
			case ActivityViewModel.stateReady:
				viewMainList.animate().withStartAction(() -> viewMainList.setVisibility(View.VISIBLE)).alpha(1);
				if(viewModel.conversationList.isEmpty()) {
					viewGroupBlank.animate().withStartAction(() -> viewGroupBlank.setVisibility(View.VISIBLE)).alpha(1);
				}
				break;
			case ActivityViewModel.stateError:
				viewGroupError.animate().withStartAction(() -> viewGroupError.setVisibility(View.VISIBLE)).alpha(1);
				break;
		}
	}
	
	/**
	 * Updates the activity based on a new connection event
	 */
	private void updateStateConnection(ReduxEventConnection event) {
		//Prompting the user to sync their messages
		if(event.getState() == ConnectionState.connected) {
			promptSync();
		}
		
		//Updating the connection warning banner
		if(event.getState() == ConnectionState.disconnected) {
			showServerWarning(((ReduxEventConnection.Disconnected) event).getCode());
		} else {
			hideServerWarning();
		}
	}
	
	/**
	 * Updates the state from a mass retrieval event
	 */
	private void updateStateMassRetrieval(ReduxEventMassRetrieval event) {
		if(event instanceof ReduxEventMassRetrieval.Complete) {
			viewModel.finishStateSyncing();
		} else if(event instanceof ReduxEventMassRetrieval.Error) {
			viewModel.finishStateSyncing();
			
			//Showing a snackbar
			if(event.getRequestID() != viewModel.lastMassRetrievalErrorID) {
				Snackbar.make(findViewById(R.id.root), getResources().getString(R.string.message_syncerror, ((ReduxEventMassRetrieval.Error) event).getCode()), Snackbar.LENGTH_LONG).show();
				viewModel.lastMassRetrievalErrorID = event.getRequestID();
			}
		} else {
			viewModel.setStateSyncing();
			
			if(event instanceof ReduxEventMassRetrieval.Start) {
				ReduxEventMassRetrieval.Start startEvent = (ReduxEventMassRetrieval.Start) event;
				viewModel.massRetrievalTotalLD.setValue(startEvent.getMessageCount());
			} else if(event instanceof ReduxEventMassRetrieval.Progress) {
				ReduxEventMassRetrieval.Progress progressEvent = (ReduxEventMassRetrieval.Progress) event;
				viewModel.massRetrievalTotalLD.setValue(progressEvent.getTotalItems());
				viewModel.massRetrievalProgressLD.setValue(progressEvent.getReceivedItems());
			}
		}
	}
	
	/**
	 * Updates the state from a text message import event
	 */
	private void updateStateTextImport(ReduxEventTextImport event) {
		if(event instanceof ReduxEventTextImport.Complete) {
			//Ignoring if there is no change in state
			if(!viewModel.isStateTextImport()) return;
			
			//Updating the state
			viewModel.setStateTextImport(false);
			
			//Adding the imported conversations
			for(ConversationInfo conversationInfo : ((ReduxEventTextImport.Complete) event).getConversations()) {
				int insertIndex = ConversationHelper.findInsertionIndex(conversationInfo, viewModel.conversationList);
				viewModel.conversationList.add(insertIndex, conversationInfo);
				conversationRecyclerAdapter.notifyItemInserted(insertIndex);
			}
		} else {
			//Updating the state
			viewModel.setStateTextImport(true);
		}
	}
	
	/**
	 * Shows the connection warning banner with the provided {@link ConnectionErrorCode}
	 */
	private void showServerWarning(@ConnectionErrorCode int reason) {
		//Getting the error details
		ErrorDetailsHelper.ErrorDetails details = ErrorDetailsHelper.getErrorDetails(reason, false);
		ErrorDetailsHelper.ErrorDetails.Button button = details.getButton();
		
		//Applying the error details to the info bar
		infoBarConnection.setText(getResources().getString(R.string.message_serverstatus_limitedfunctionality, getResources().getString(details.getLabel())));
		if(button == null) {
			infoBarConnection.removeButton();
		} else {
			infoBarConnection.setButton(getResources().getString(button.getLabel()), view -> button.getClickListener().invoke(this, getSupportFragmentManager(), pluginCS.getConnectionManager()));
		}
		
		//Showing the info bar
		infoBarConnection.show();
	}
	
	/**
	 * Hides the connection warning banner
	 */
	private void hideServerWarning() {
		infoBarConnection.hide();
	}
	
	/**
	 * Updates the conversation list in response to a messaging event
	 */
	private void updateConversationList(ReduxEventMessaging event) {
		//Ignoring if messages aren't loaded
		if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady || conversationRecyclerAdapter == null) return;
		
		if(event instanceof ReduxEventMessaging.Message) {
			updateMessageListMessageUpdate((ReduxEventMessaging.Message) event);
		} else if(event instanceof ReduxEventMessaging.ConversationUpdate) {
			updateMessageListConversationUpdate((ReduxEventMessaging.ConversationUpdate) event);
		} else if(event instanceof ReduxEventMessaging.ConversationUnread) {
			ReduxEventMessaging.ConversationUnread unreadEvent = (ReduxEventMessaging.ConversationUnread) event;
			
			getConversationFromAction(unreadEvent, (conversation, i) -> {
				conversation.setUnreadMessageCount(unreadEvent.getUnreadCount());
				conversationRecyclerAdapter.notifyItemChanged(i, conversationPayloadUnread);
			});
			
			updateMarkAllRead();
		} else if(event instanceof ReduxEventMessaging.ConversationMember) {
			ReduxEventMessaging.ConversationMember memberEvent = (ReduxEventMessaging.ConversationMember) event;
			
			getConversationFromAction(memberEvent, (conversation, i) -> {
				if(memberEvent.isJoin()) conversation.getMembers().add(memberEvent.getMember().clone());
				else conversation.getMembers().removeIf(member -> member.getAddress().equals(memberEvent.getMember().getAddress()));
				conversationRecyclerAdapter.notifyItemChanged(i, conversationPayloadMember);
			});
		} else if(event instanceof ReduxEventMessaging.ConversationMute) {
			ReduxEventMessaging.ConversationMute muteEvent = (ReduxEventMessaging.ConversationMute) event;
			
			getConversationFromAction(muteEvent, (conversation, i) -> {
				conversation.setMuted(muteEvent.isMuted());
				conversationRecyclerAdapter.notifyItemChanged(i, conversationPayloadMuted);
			});
		} else if(event instanceof ReduxEventMessaging.ConversationArchive) {
			ReduxEventMessaging.ConversationArchive archiveEvent = (ReduxEventMessaging.ConversationArchive) event;
			
			//If this conversation is to be added to the list
			if(isViewArchived == archiveEvent.isArchived()) {
				//Hiding the blank state view
				if(viewModel.conversationList.isEmpty()) {
					viewGroupBlank.animate().alpha(0).withEndAction(() -> viewGroupBlank.setVisibility(View.GONE));
				}
				
				//Getting the conversation
				ConversationInfo conversationInfo = archiveEvent.getConversationInfo().clone();
				conversationInfo.setArchived(archiveEvent.isArchived());
				
				//Adding the conversation
				int index = ConversationHelper.findInsertionIndex(conversationInfo, viewModel.conversationList);
				viewModel.conversationList.add(index, conversationInfo);
				conversationRecyclerAdapter.notifyItemInserted(index);
			} else { //This conversation is to be removed from the list
				getConversationFromAction(archiveEvent, (conversation, i) -> {
							viewModel.conversationList.remove(i);
							conversationRecyclerAdapter.notifyItemRemoved(i);
						});
				
				//Showing the blank state view if there are no more conversations
				if(viewModel.conversationList.isEmpty()) {
					viewGroupBlank.animate().withStartAction(() -> viewGroupBlank.setVisibility(View.VISIBLE)).alpha(1);
				}
			}
		} else if(event instanceof ReduxEventMessaging.ConversationDelete) {
			ReduxEventMessaging.ConversationDelete deleteEvent = (ReduxEventMessaging.ConversationDelete) event;
			
			//Removing the conversation
			getConversationFromAction(deleteEvent, (conversation, i) -> {
				viewModel.conversationList.remove(i);
				conversationRecyclerAdapter.notifyItemRemoved(i);
			});
			
			//Showing the blank state view if there are no more conversations
			if(viewModel.conversationList.isEmpty()) {
				viewGroupBlank.animate().withStartAction(() -> viewGroupBlank.setVisibility(View.VISIBLE)).alpha(1);
			}
		} else if(event instanceof ReduxEventMessaging.ConversationServiceHandlerDelete) {
			ReduxEventMessaging.ConversationServiceHandlerDelete deleteEvent = (ReduxEventMessaging.ConversationServiceHandlerDelete) event;
			
			//Removing any matching conversations
			for(ListIterator<ConversationInfo> iterator = viewModel.conversationList.listIterator(); iterator.hasNext();) {
				int i = iterator.nextIndex();
				ConversationInfo conversationInfo = iterator.next();
				if(conversationInfo.getServiceHandler() != deleteEvent.getServiceHandler()) continue;
				
				iterator.remove();
				conversationRecyclerAdapter.notifyItemRemoved(i);
			}
			
			//Showing the blank state view if there are no more conversations
			if(viewModel.conversationList.isEmpty()) {
				viewGroupBlank.animate().withStartAction(() -> viewGroupBlank.setVisibility(View.VISIBLE)).alpha(1);
			}
		} else if(event instanceof ReduxEventMessaging.ConversationTitle) {
			ReduxEventMessaging.ConversationTitle titleEvent = (ReduxEventMessaging.ConversationTitle) event;
			
			getConversationFromAction(titleEvent, (conversation, i) -> {
						conversation.setTitle(titleEvent.getTitle());
						conversationRecyclerAdapter.notifyItemChanged(i, conversationPayloadTitle);
					});
		} else if(event instanceof ReduxEventMessaging.ConversationDraftMessageUpdate) {
			ReduxEventMessaging.ConversationDraftMessageUpdate draftMessageEvent = (ReduxEventMessaging.ConversationDraftMessageUpdate) event;
			
			getConversationFromAction(draftMessageEvent, (conversation, i) -> {
				//Updating the conversation
				conversation.setDraftMessage(draftMessageEvent.getDraftMessage());
				conversation.setDraftUpdateTime(draftMessageEvent.getUpdateTime());
				conversationRecyclerAdapter.notifyItemChanged(i, conversationPayloadPreview);
				
				//Re-sorting the conversation
				int conversationIndex = viewModel.conversationList.indexOf(conversation);
				int insertionIndex = ConversationHelper.findReinsertionIndex(conversation, viewModel.conversationList);
				if(conversationIndex != insertionIndex) {
					viewModel.conversationList.remove(conversation);
					viewModel.conversationList.add(insertionIndex, conversation);
					conversationRecyclerAdapter.notifyItemMoved(conversationIndex, insertionIndex);
				}
			});
		} else if(event instanceof ReduxEventMessaging.ConversationDraftFileUpdate) {
			ReduxEventMessaging.ConversationDraftFileUpdate draftFileEvent = (ReduxEventMessaging.ConversationDraftFileUpdate) event;
			
			getConversationFromAction(draftFileEvent, (conversation, i) -> {
				//Updating the conversation
				if(draftFileEvent.isAddition()) conversation.getDraftFiles().add(draftFileEvent.getDraft());
				else conversation.getDraftFiles().removeIf(draft -> draft.getLocalID() == draftFileEvent.getDraft().getLocalID());
				conversation.setDraftUpdateTime(draftFileEvent.getUpdateTime());
				conversationRecyclerAdapter.notifyItemChanged(i, conversationPayloadPreview);
				
				//Re-sorting the conversation
				int conversationIndex = viewModel.conversationList.indexOf(conversation);
				int insertionIndex = ConversationHelper.findReinsertionIndex(conversation, viewModel.conversationList);
				if(conversationIndex != insertionIndex) {
					viewModel.conversationList.remove(conversation);
					viewModel.conversationList.add(insertionIndex, conversation);
					conversationRecyclerAdapter.notifyItemMoved(conversationIndex, insertionIndex);
				}
			});
		} else if(event instanceof ReduxEventMessaging.ConversationDraftFileClear) {
			ReduxEventMessaging.ConversationDraftFileClear draftClearEvent = (ReduxEventMessaging.ConversationDraftFileClear) event;
			
			getConversationFromAction(draftClearEvent, (conversation, i) -> {
				//Updating the conversation
				conversation.getDraftFiles().clear();
				conversation.setDraftUpdateTime(System.currentTimeMillis());
				conversationRecyclerAdapter.notifyItemChanged(i, conversationPayloadPreview);
				
				//Re-sorting the conversation
				int conversationIndex = viewModel.conversationList.indexOf(conversation);
				int insertionIndex = ConversationHelper.findReinsertionIndex(conversation, viewModel.conversationList);
				if(conversationIndex != insertionIndex) {
					viewModel.conversationList.remove(conversation);
					viewModel.conversationList.add(insertionIndex, conversation);
					conversationRecyclerAdapter.notifyItemMoved(conversationIndex, insertionIndex);
				}
			});
		}
		
		//Updating the search list adapter
		if(viewModel.isSearching) searchRecyclerAdapter.updateAndFilter();
		
		//Updating the "mark all as read" menu option
		updateMarkAllRead();
	}
	
	/**
	 * Runs the provided consumer if the conversation of the provided action is present
	 */
	private void getConversationFromAction(ReduxEventMessaging.ReduxConversationAction action, ObjIntConsumer<ConversationInfo> consumer) {
		IntStream.range(0, viewModel.conversationList.size())
				.filter(i -> viewModel.conversationList.get(i).getLocalID() == action.getConversationInfo().getLocalID())
				.findAny()
				.ifPresent(i -> consumer.accept(viewModel.conversationList.get(i), i));
	}
	
	/**
	 * Updates a conversation's preview, position in the list, title, and members based on a collection of new items
	 * @param conversation The conversation to update
	 * @param newMessages The collection of new messages under this conversation
	 */
	private void updateConversationDetails(ConversationInfo conversation, Collection<ConversationItem> newMessages) {
		//Getting the conversation preview
		ConversationPreview conversationPreview = ConversationPreviewHelper.latestItemToPreview(newMessages);
		if(conversationPreview == null) return;
		
		//Checking if this new preview can replace the old one
		if((conversation.getMessagePreview() == null || conversation.getMessagePreview().getDate() < conversationPreview.getDate())) {
			//Updating the message preview
			conversation.setMessagePreview(conversationPreview);
			
			//Re-sorting the conversation
			int conversationIndex = viewModel.conversationList.indexOf(conversation);
			int insertionIndex = ConversationHelper.findReinsertionIndex(conversation, viewModel.conversationList);
			if(conversationIndex != insertionIndex) {
				viewModel.conversationList.remove(conversation);
				viewModel.conversationList.add(insertionIndex, conversation);
				conversationRecyclerAdapter.notifyItemMoved(conversationIndex, insertionIndex);
			}
			
			//Updating the conversation preview
			conversationRecyclerAdapter.notifyItemChanged(insertionIndex, conversationPayloadPreview);
		}
	}
	
	/**
	 * Updates the message list in response to a message update
	 */
	private void updateMessageListMessageUpdate(ReduxEventMessaging.Message event) {
		List<ConversationInfo> conversationList = viewModel.conversationList;
		
		for(Pair<ConversationInfo, List<ReplaceInsertResult>> entry : event.getConversationItems()) {
			//Finding the existing conversation index
			ConversationInfo conversationInfo = conversationList.stream().filter(conversation -> entry.getFirst().getLocalID() == conversation.getLocalID()).findAny().orElse(null);
			if(conversationInfo == null) continue;
			
			//Updating the conversation's preview
			updateConversationDetails(conversationInfo, entry.getSecond().stream().map(ReplaceInsertResult::getTargetItem).collect(Collectors.toList()));
		}
	}
	
	/**
	 * Updates the message list in response to a conversation update
	 */
	private void updateMessageListConversationUpdate(ReduxEventMessaging.ConversationUpdate event) {
		List<ConversationInfo> conversationList = viewModel.conversationList;
		
		//Copying the list of new conversations
		Map<ConversationInfo, Collection<ConversationItem>> newConversations = new HashMap<>(event.getNewConversations());
		
		//Handling transferred conversations
		for(TransferredConversation transferredConversation : event.getTransferredConversations()) {
			//Finding the existing conversation
			ConversationInfo conversationInfo = conversationList.stream().filter(conversation -> transferredConversation.getServerConversation().getGUID().equals(conversation.getGUID())).findAny().orElse(null);
			
			//Converting the merge result to just the target items
			List<ConversationItem> conversationItems = transferredConversation.getServerConversationItems().stream().map(ReplaceInsertResult::getTargetItem).collect(Collectors.toList());
			
			//If we couldn't find a matching conversation, add it as a new conversation
			if(conversationInfo == null) {
				newConversations.put(transferredConversation.getClientConversation(), conversationItems);
				continue;
			}
			
			//Updating the conversation's preview
			updateConversationDetails(conversationInfo, conversationItems);
		}
		
		//Hiding the blank state view
		if(!newConversations.isEmpty() && viewModel.conversationList.isEmpty()) {
			viewGroupBlank.animate().alpha(0).withEndAction(() -> viewGroupBlank.setVisibility(View.GONE));
		}
		
		//Handling new conversations
		for(Map.Entry<ConversationInfo, Collection<ConversationItem>> entry : newConversations.entrySet()) {
			//Getting the conversation
			ConversationInfo conversationInfo = entry.getKey().clone();
			
			//Getting and updating the conversation's preview
			ConversationPreview conversationPreview = ConversationPreviewHelper.latestItemToPreview(entry.getValue());
			if(conversationPreview != null) conversationInfo.setMessagePreview(conversationPreview);
			conversationInfo.setUnreadMessageCount(entry.getValue().size());
			
			//Finding the index to insert the conversation
			int insertionIndex = ConversationHelper.findInsertionIndex(conversationInfo, conversationList);
			
			//Inserting the conversation
			conversationList.add(insertionIndex, conversationInfo);
			conversationRecyclerAdapter.notifyItemInserted(insertionIndex);
		}
	}
	
	void startActionMode() {
		//Starting action mode
		actionMode = startSupportActionMode(actionModeCallbacks);
	}
	
	void restoreActionMode() {
		//Returning if the action mode is already set up or there are no items selected
		if(actionMode != null || viewModel.actionModeSelections.isEmpty()) return;
		
		//Calculating the values
		for(ConversationInfo conversationInfo : viewModel.conversationList) {
			//Skipping the remainder of the iteration if the conversation is not selected
			if(!viewModel.actionModeSelections.contains(conversationInfo.getLocalID())) continue;
			
			//Adding to the values
			actionModeCallbacks.selectedCount++;
			if(conversationInfo.isMuted()) actionModeCallbacks.mutedCount++;
			else actionModeCallbacks.unmutedCount++;
			if(conversationInfo.isArchived()) actionModeCallbacks.archivedCount++;
			else actionModeCallbacks.unarchivedCount++;
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
			if(updateContext) viewToolbar.animate().alpha(0).setDuration(duration).withEndAction(() -> viewToolbar.setVisibility(View.GONE));
			
			//Showing the search group
			viewGroupToolbarSearch.animate().alpha(1).setDuration(duration).withStartAction(() -> {
				viewGroupToolbarSearch.setVisibility(View.VISIBLE);
				
				//Clearing the text field
				if(viewSearchField.getText().length() > 0) viewSearchField.setText("");
				
				//Opening the keyboard
				viewSearchField.requestFocus();
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(viewSearchField, InputMethodManager.SHOW_IMPLICIT);
			}).withEndAction(() -> viewGroupToolbarSearch.setClickable(true));
			
			//Hiding the FAB
			if(updateContext) updateFAB();
		} else {
			//Showing the toolbar
			if(updateContext) viewToolbar.animate().alpha(1).setDuration(duration).withStartAction(() -> viewToolbar.setVisibility(View.VISIBLE));
			
			//Hiding the search group
			viewGroupToolbarSearch.animate().alpha(0).setDuration(duration).withStartAction(() -> {
				viewGroupToolbarSearch.setClickable(false);
				
				//Closing the keyboard
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(viewSearchField.getWindowToken(), 0);
			}).withEndAction(() -> viewGroupToolbarSearch.setVisibility(View.GONE));
			
			//Showing the FAB
			if(updateContext) updateFAB();
			
			//Clearing the search query
			updateSearchFilter("");
		}
	}
	
	@Deprecated
	void restoreSearchState() {
		//Ignoring if searching is not active
		if(!viewModel.isSearching) return;
		
		//Hiding the toolbar
		viewToolbar.setVisibility(View.GONE);
		viewToolbar.setAlpha(0);
		
		//Showing the views
		viewGroupToolbarSearch.setVisibility(View.VISIBLE);
		viewGroupToolbarSearch.setAlpha(1);
		viewSearchField.requestFocus();
		viewGroupSearch.setVisibility(View.VISIBLE);
		viewMainList.setVisibility(View.INVISIBLE);
		
		//Updating the close button
		viewSearchClear.setVisibility(viewSearchField.getText().length() > 0 ? View.VISIBLE : View.GONE);
		
		//Hiding the FAB
		updateFAB();
	}
	
	/**
	 * Updates the "mark all as read" menu option item, showing or hiding it based on if there are any conversations with unread messages
	 */
	private void updateMarkAllRead() {
		if(menuItemMarkAllRead == null || viewModel.stateLD.getValue() != ActivityViewModel.stateReady) return;
		
		menuItemMarkAllRead.setVisible(viewModel.conversationList.stream().anyMatch(conversation -> conversation.getUnreadMessageCount() > 0));
	}
	
	/**
	 * Updates the FAB display, based on if the user is searching or the activity is displaying archived conversations
	 */
	private void updateFAB() {
		if(viewModel.isSearching || isViewArchived) viewFAB.hide();
		else viewFAB.show();
	}
	
	/**
	 * Opens the sync dialog to prompt the user to update their messages from their server
	 */
	private void promptSync() {
		//Ignoring if we aren't connected to the service
		if(!pluginCS.isServiceBound()) return;
		
		//Returning if no sync is pending, conversations aren't loaded, or the fragment already exists
		if(!pluginCS.getConnectionManager().isPendingSync() || viewModel.stateLD.getValue() != ActivityViewModel.stateReady || getSupportFragmentManager().findFragmentByTag(keyFragmentSync) != null) return;
		
		//Creating and showing a new fragment
		FragmentSync fragmentSync = new FragmentSync(pluginCS.getConnectionManager().getServerDeviceName(), pluginCS.getConnectionManager().getServerInstallationID(), viewModel.conversationList.stream().anyMatch(chat -> chat.getServiceHandler() == ServiceHandler.appleBridge));
		fragmentSync.setCancelable(false);
		fragmentSync.show(getSupportFragmentManager(), keyFragmentSync);
	}
	
	public void onCloseSearchClicked(View view) {
		setSearchState(false, true);
	}
	
	private void updateSearchFilter(String query) {
		boolean queryAvailable = !query.isEmpty();
		
		//Returning if the search is not active
		if(queryAvailable && !viewModel.isSearching) return;
		
		//Updating the recycler adapter
		if(searchRecyclerAdapter != null) searchRecyclerAdapter.updateFilterText(query);
		
		//Setting the search group state
		viewGroupSearch.setVisibility(queryAvailable ? View.VISIBLE : View.GONE);
		viewMainList.setVisibility(queryAvailable ? View.INVISIBLE : View.VISIBLE);
	}
	
	/**
	 * Gets a spannable to use for the action bar title when displaying the conversation list
	 */
	private Spannable getTitleSpannable() {
		Spannable text = new SpannableString(getResources().getString(R.string.app_name));
		text.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary, null)), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		text.setSpan(new TypefaceSpan("sans-serif-medium"), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		
		return text;
	}
	
	/**
	 * Gets a spannable to use for the action bar title when displaying archived conversations
	 */
	private Spannable getArchivedSpannable() {
		Spannable text = new SpannableString(getResources().getString(R.string.screen_archived));
		//text.setSpan(new ForegroundColorSpan(getResources().getServiceColor(R.color.colorArchived, null)), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		return text;
	}
	
	void setDarkAMOLED() {
		ThemeHelper.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(ColorConstants.colorAMOLED);
		findViewById(R.id.viewgroup_search).setBackgroundColor(ColorConstants.colorAMOLED);
	}
	
	void setDarkAMOLEDSamsung() {
		ThemeHelper.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(ColorConstants.colorAMOLED);
		
		RecyclerView listMessages = findViewById(R.id.list);
		RecyclerView listSearch = findViewById(R.id.list_search);
		listMessages.setBackgroundResource(R.drawable.background_amoledsamsung);
		listMessages.setClipToOutline(true);
		listMessages.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
		findViewById(R.id.viewgroup_search).setBackgroundColor(ColorConstants.colorAMOLED);
		listSearch.setBackgroundResource(R.drawable.background_amoledsamsung);
		listSearch.setClipToOutline(true);
		listSearch.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
	}
	
	/**
	 * Returns true if there is a foreground instance of this activity running
	 */
	public static boolean isForeground() {
		return !foregroundActivities.isEmpty();
	}
	
	public static class ActivityViewModel extends AndroidViewModel {
		static final int stateIdle = 0;
		static final int stateLoading = 1;
		static final int stateSyncing = 2;
		static final int stateReady = 3;
		static final int stateError = 4;
		
		private final CompositeDisposable compositeDisposable = new CompositeDisposable();
		private Disposable conversationLoadDisposable;
		
		//Parameters
		final boolean isViewArchived;
		
		//Data
		List<ConversationInfo> conversationList;
		
		//State
		final MutableLiveData<Integer> stateLD = new MutableLiveData<>(stateIdle);
		final MutableLiveData<Integer> massRetrievalProgressLD = new MutableLiveData<>();
		final MutableLiveData<Integer> massRetrievalTotalLD = new MutableLiveData<>();
		final MutableLiveData<Boolean> textImportLD = new MutableLiveData<>(false);
		final MutableLiveData<Integer> playServicesErrorCode = new MutableLiveData<>(null);
		final List<Long> actionModeSelections = new ArrayList<>();
		boolean isSearching = false;
		short lastMassRetrievalErrorID = -1;
		
		public ActivityViewModel(@NonNull Application application, boolean isViewArchived) {
			super(application);
			
			//Setting the parameters
			this.isViewArchived = isViewArchived;
			
			//Loading conversations
			loadConversations();
			
			//Updating the security provider
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				updateSecurityProvider();
			}
		}
		
		@Override
		protected void onCleared() {
			//Clearing task subscriptions
			compositeDisposable.clear();
		}
		
		/**
		 * Loads conversations from the database
		 */
		private void loadConversations() {
			//Ignoring if the state is already loading
			if(stateLD.getValue() == stateLoading) return;
			
			//Updating the state
			stateLD.setValue(stateLoading);
			
			//Loading the conversations
			compositeDisposable.add(conversationLoadDisposable = Single.create((SingleEmitter<List<ConversationInfo>> emitter) -> {
				//Loading the conversations
				emitter.onSuccess(DatabaseManager.getInstance().fetchSummaryConversations(getApplication(), isViewArchived));
			}).subscribeOn(Schedulers.single())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe((conversations) -> {
						//Setting the conversation list
						conversationList = conversations;
						
						//Setting the state
						stateLD.setValue(stateReady);
					}));
		}
		
		/**
		 * Updates the state in response to a sync event
		 */
		public void setStateSyncing() {
			//Ignoring if the state is already syncing
			if(stateLD.getValue() == stateSyncing) return;
			
			//Cancelling the existing conversation load task if it's running
			if(conversationLoadDisposable != null && !conversationLoadDisposable.isDisposed()) {
				conversationLoadDisposable.dispose();
			}
			
			//Updating the state
			stateLD.setValue(stateSyncing);
		}
		
		/**
		 * Updates the state in response to the completion of a sync event
		 * @return Whether a sync was taking place
		 */
		public boolean finishStateSyncing() {
			//Ignoring if the state is not syncing
			if(stateLD.getValue() != stateSyncing) return false;
			
			//Reloading the conversations
			loadConversations();
			
			//Returning true
			return true;
		}
		
		/**
		 * Sets whether a text message import is in progress
		 */
		public void setStateTextImport(boolean inProgress) {
			//Updating the state value
			textImportLD.setValue(inProgress);
		}
		
		/**
		 * Gets whether a text message import is in progress
		 */
		public boolean isStateTextImport() {
			return textImportLD.getValue();
		}
		
		public void updateSecurityProvider() {
			playServicesErrorCode.setValue(null);
			
			ProviderInstaller.installIfNeededAsync(getApplication(), new ProviderInstaller.ProviderInstallListener() {
				@Override
				public void onProviderInstalled() {
				}
				
				@Override
				public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
					GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
					if(availability.isUserResolvableError(errorCode)) {
						playServicesErrorCode.setValue(errorCode);
					}
				}
			});
		}
	}
	
	private class ConversationRecyclerAdapter extends RecyclerView.Adapter<VHConversationDetailed> {
		//Creating the list values
		private final List<ConversationInfo> items;
		
		ConversationRecyclerAdapter(List<ConversationInfo> items) {
			//Setting the items
			this.items = items;
			
			//Enabling stable IDs
			setHasStableIds(true);
		}
		
		@Override
		@NonNull
		public VHConversationDetailed onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = getLayoutInflater().inflate(R.layout.listitem_conversation, parent, false);
			return new VHConversationDetailed(view,
					view.findViewById(R.id.conversationicon),
					view.findViewById(R.id.title),
					view.findViewById(R.id.selected),
					view.findViewById(R.id.selectionhighlight),
					view.findViewById(R.id.message),
					view.findViewById(R.id.time),
					view.findViewById(R.id.unread),
					view.findViewById(R.id.flag_muted),
					view.findViewById(R.id.flag_draft)
			);
		}
		
		@Override
		public void onBindViewHolder(@NonNull VHConversationDetailed holder, int position) {
			//Getting the conversation info
			ConversationInfo conversationInfo = items.get(position);
			
			//Binding the title and icon
			holder.getCompositeDisposable().addAll(
					VBConversation.bindTitle(Conversations.this, holder.getConversationTitle(), conversationInfo).subscribe(),
					VBConversation.bindUsers(Conversations.this, holder.getIconGroup(), conversationInfo).subscribe()
			);
			
			//Binding the conversation preview
			VBConversation.bindPreview(Conversations.this, holder.getLabelMessage(), holder.getLabelStatus(), conversationInfo.getDynamicPreview());
			
			//Binding the unread status
			VBConversation.bindUnreadStatus(Conversations.this, holder.getConversationTitle(), holder.getLabelMessage(), holder.getLabelUnread(), conversationInfo.getUnreadMessageCount());
			
			//Binding the flags
			holder.getFlagMuted().setVisibility(conversationInfo.isMuted() ? View.VISIBLE : View.GONE);
			holder.getFlagDraft().setVisibility(conversationInfo.getDraftMessage() != null || !conversationInfo.getDraftFiles().isEmpty() ? View.VISIBLE : View.GONE);
			
			//Binding the selection indicator
			VBConversation.bindSelectionIndicator(holder.itemView, holder.getIconGroup(), holder.getSelectionIndicator(), holder.getSelectionHighlight(), viewModel.actionModeSelections.contains(conversationInfo.getLocalID()), false);
			
			//Setting the view's click listeners
			holder.itemView.setOnClickListener(view -> {
				//Checking if action mode is active
				if(actionMode != null) {
					//Toggling the item's checked state
					actionModeCallbacks.onItemCheckedStateToggled(conversationInfo);
					
					return;
				}
				
				//Updating the conversation's shortcut usage
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
					ShortcutHelper.reportShortcutUsed(getApplication(), conversationInfo.getLocalID());
				}
				
				//Launching the conversation activity
				startActivity(new Intent(Conversations.this, Messaging.class).putExtra(Messaging.intentParamTargetID, conversationInfo.getLocalID()));
			});
			holder.itemView.setOnLongClickListener(view -> {
				//Starting action mode if it isn't running
				if(actionMode == null) startActionMode();
				
				//Toggling the item's checked state
				actionModeCallbacks.onItemCheckedStateToggled(conversationInfo);
				
				return true;
			});
		}
		
		@Override
		public void onBindViewHolder(@NonNull VHConversationDetailed holder, int position, @NonNull List<Object> payloads) {
			if(payloads.isEmpty()) {
				onBindViewHolder(holder, position);
			} else {
				//Getting the conversation info
				ConversationInfo conversationInfo = items.get(position);
				
				for(Object payload : payloads) {
					switch((int) payload) {
						case conversationPayloadPreview:
							VBConversation.bindPreview(Conversations.this, holder.getLabelMessage(), holder.getLabelStatus(), conversationInfo.getDynamicPreview());
							holder.getFlagDraft().setVisibility(conversationInfo.getDraftMessage() != null || !conversationInfo.getDraftFiles().isEmpty() ? View.VISIBLE : View.GONE);
							break;
						case conversationPayloadTitle:
							holder.getCompositeDisposable().add(VBConversation.bindTitle(Conversations.this, holder.getConversationTitle(), conversationInfo).subscribe());
							break;
						case conversationPayloadMember:
							holder.getCompositeDisposable().add(VBConversation.bindUsers(Conversations.this, holder.getIconGroup(), conversationInfo).subscribe());
							break;
						case conversationPayloadMuted:
							holder.getFlagMuted().setVisibility(conversationInfo.isMuted() ? View.VISIBLE : View.GONE);
							break;
						case conversationPayloadUnread:
							VBConversation.bindUnreadStatus(Conversations.this, holder.getConversationTitle(), holder.getLabelMessage(), holder.getLabelUnread(), conversationInfo.getUnreadMessageCount());
							break;
						case conversationPayloadSelection:
							VBConversation.bindSelectionIndicator(holder.itemView, holder.getIconGroup(), holder.getSelectionIndicator(), holder.getSelectionHighlight(), viewModel.actionModeSelections.contains(conversationInfo.getLocalID()), true);
							break;
					}
				}
			}
		}
		
		@Override
		public void onViewRecycled(@NonNull VHConversationDetailed holder) {
			holder.getCompositeDisposable().clear();
		}
		
		@Override
		public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
			//Cancelling all view holder tasks
			LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			int firstVisibleIndex = layoutManager.findFirstVisibleItemPosition();
			int lastVisibleIndex = layoutManager.findLastVisibleItemPosition();
			
			for(int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
				DisposableViewHolder holder = (DisposableViewHolder) recyclerView.findViewHolderForLayoutPosition(i);
				if(holder != null) holder.getCompositeDisposable().clear();
			}
		}
		
		@Override
		public int getItemCount() {
			return items.size();
		}
		
		@Override
		public long getItemId(int position) {
			return items.get(position).getLocalID();
		}
	}
	
	private class SearchRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the reference values
		private static final int itemTypeSubheader = -1;
		private static final int itemTypeConversation = 0;
		private static final int itemTypeMessage = 1;
		
		//Creating the list values
		private final List<ConversationInfo> conversationSourceList;
		
		private final List<ConversationInfo> conversationFilterList = new ArrayList<>();
		private final List<MessageInfo> messageFilterList = new ArrayList<>();
		
		private Disposable searchTask = null;
		
		SearchRecyclerAdapter(List<ConversationInfo> conversationList) {
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
		
		private String lastFilterText = "";
		void updateFilterText(String text) {
			//Cancelling the current subscription
			if(searchTask != null && !searchTask.isDisposed()) searchTask.dispose();
			
			//Setting the last filter text
			lastFilterText = text;
			
			//Clearing the filter lists
			conversationFilterList.clear();
			messageFilterList.clear();
			
			//Returning if there is no filter text
			if(text.isEmpty()) {
				notifyDataSetChanged();
				return;
			}
			
			//Iterating over the conversations
			List<ConversationInfo> asyncSearchList = new ArrayList<>();
			conversationLoop:
			for(ConversationInfo conversationInfo : conversationSourceList) {
				//Filtering the conversation based on its static name
				if(conversationInfo.getTitle() != null && searchString(conversationInfo.getTitle(), text)) {
					conversationFilterList.add(conversationInfo);
					continue conversationLoop;
				}
				
				//Filtering the conversation based on its members
				for(MemberInfo member : conversationInfo.getMembers()) {
					if(searchString(AddressHelper.normalizeAddress(member.getAddress()), text)) {
						conversationFilterList.add(conversationInfo);
						continue conversationLoop;
					}
					asyncSearchList.add(conversationInfo);
				}
			}
			
			//Updating the list
			notifyDataSetChanged();
			
			if(!asyncSearchList.isEmpty()) {
				//Starting a search for member names
				searchTask = Observable.fromIterable(asyncSearchList).flatMapMaybe(conversation ->
						Observable.fromIterable(conversation.getMembers())
								//Map each member to their user info
								.flatMapSingle(member -> MainApplication.getInstance().getUserCacheHelper().getUserInfo(Conversations.this, member.getAddress()))
								//Ignore errors
								.onErrorResumeNext(error -> Observable.empty())
								//Try to match any member to the search query
								.any(user -> searchString(user.getContactName(), lastFilterText))
								//Filter out non-matches
								.filter(isMatch -> isMatch)
								//Map back to return the conversation
								.map(isMatch -> conversation)
				).subscribe((conversation) -> {
					//Adding the conversation
					conversationFilterList.add(conversation);
					
					//Updating the list
					notifyItemInserted(conversationFilterList.size() - 1);
				});
			}
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
				case itemTypeConversation: {
					View view = getLayoutInflater().inflate(R.layout.listitem_conversation, parent, false);
					return new VHConversationDetailed(view,
							view.findViewById(R.id.conversationicon),
							view.findViewById(R.id.title),
							view.findViewById(R.id.selected),
							view.findViewById(R.id.selectionhighlight),
							view.findViewById(R.id.message),
							view.findViewById(R.id.time),
							view.findViewById(R.id.unread),
							view.findViewById(R.id.flag_muted),
							view.findViewById(R.id.flag_draft)
					);
				}
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
					//Getting the data
					VHConversationDetailed viewHolder = (VHConversationDetailed) holder;
					ConversationInfo conversation = conversationFilterList.get(position - 1);
					
					//Binding the title and icon
					viewHolder.getCompositeDisposable().addAll(
							VBConversation.bindTitle(Conversations.this, viewHolder.getConversationTitle(), conversation).subscribe(),
							VBConversation.bindUsers(Conversations.this, viewHolder.getIconGroup(), conversation).subscribe()
					);
					
					//Binding the conversation preview
					VBConversation.bindPreview(Conversations.this, viewHolder.getLabelMessage(), viewHolder.getLabelStatus(), conversation.getDynamicPreview());
					
					//Binding the unread status
					VBConversation.bindUnreadStatus(Conversations.this, viewHolder.getConversationTitle(), viewHolder.getLabelMessage(), viewHolder.getLabelUnread(), conversation.getUnreadMessageCount());
					
					//Binding the flags
					viewHolder.getFlagMuted().setVisibility(conversation.isMuted() ? View.VISIBLE : View.GONE);
					viewHolder.getFlagDraft().setVisibility(conversation.getDraftMessage() != null || !conversation.getDraftFiles().isEmpty() ? View.VISIBLE : View.GONE);
					
					//Setting the view's click listener
					holder.itemView.setOnClickListener(view -> {
						//Updating the conversation's shortcut usage
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
							ShortcutHelper.reportShortcutUsed(getApplication(), conversation.getLocalID());
						}
						
						//Opening the conversation
						startActivity(new Intent(Conversations.this, Messaging.class).putExtra(Messaging.intentParamTargetID, conversation.getLocalID()));
					});
					
					break;
				}
				case itemTypeMessage:
					break;
			}
		}
		
		@Override
		public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
			if(holder instanceof DisposableViewHolder) {
				((DisposableViewHolder) holder).getCompositeDisposable().clear();
			}
		}
		
		@Override
		public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
			//Cancelling all view holder tasks
			LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			int firstVisibleIndex = layoutManager.findFirstVisibleItemPosition();
			int lastVisibleIndex = layoutManager.findLastVisibleItemPosition();
			
			for(int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
				RecyclerView.ViewHolder holder = recyclerView.findViewHolderForLayoutPosition(i);
				if(holder instanceof DisposableViewHolder) {
					((DisposableViewHolder) holder).getCompositeDisposable().clear();
				}
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
	
	private class CountingActionModeCallback implements ActionMode.Callback {
		//Creating the selected conversations variable
		int selectedCount = 0;
		int mutedCount = 0;
		int unmutedCount = 0;
		int archivedCount = 0;
		int unarchivedCount = 0;
		
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			//Inflating the menu
			MenuInflater inflater = actionMode.getMenuInflater();
			inflater.inflate(R.menu.menu_conversation_actionmode, menu);
			
			//Closing the search
			setSearchState(false, false);
			
			//Hiding the toolbar
			viewToolbar.animate().alpha(0).withEndAction(() -> viewToolbar.setVisibility(View.INVISIBLE));
			
			//Animating the app bar
			/* int toolbarColor = getResources().getServiceColor(R.color.colorContextualAppBar, null);
			int statusBarColor = getResources().getServiceColor(R.color.colorContextualAppBarDark, null);
			animateAppBarColor(toolbarColor, statusBarColor, getResources().getInteger(android.R.integer.config_mediumAnimTime)); */
			
			//Hiding the FAB
			updateFAB();
			
			//Returning true
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			//Invalidating the action mode
			Conversations.this.actionMode = null;
			
			//Copying the selected items
			List<Long> selectedItems = new ArrayList<>(viewModel.actionModeSelections);
			
			//Deselecting all items
			viewModel.actionModeSelections.clear();
			
			//Updating the list
			for(long selectedItemID : selectedItems) {
				ConversationInfo selectedItem = viewModel.conversationList.stream().filter(item -> item.getLocalID() == selectedItemID).findAny().orElse(null);
				if(selectedItem != null) {
					conversationRecyclerAdapter.notifyItemChanged(viewModel.conversationList.indexOf(selectedItem), conversationPayloadSelection);
				}
			}
			
			//Resetting the selection counts
			selectedCount = mutedCount = unmutedCount = archivedCount = unarchivedCount = 0;
			
			//Showing the toolbar
			viewToolbar.setVisibility(View.VISIBLE);
			viewToolbar.animate().setStartDelay(50).alpha(1);
			
			//Showing the FAB
			updateFAB();
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			//Collecting the selected conversations
			Stream<ConversationInfo> selectionStream = viewModel.actionModeSelections.stream().map(conversationID ->
					viewModel.conversationList.stream().filter(conversation -> conversation.getLocalID() == conversationID).findAny())
					.filter(Optional::isPresent)
					.map(Optional::get);
			
			//Mute conversations
			if(menuItem.getItemId() == R.id.action_mute) {
				ConversationActionTask.muteConversations(selectionStream.filter(conversation -> !conversation.isMuted()).collect(Collectors.toList()), true).subscribe();
				
				actionMode.finish();
				return true;
			}
			//Unmute conversations
			else if(menuItem.getItemId() == R.id.action_unmute) {
				ConversationActionTask.muteConversations(selectionStream.filter(ConversationInfo::isMuted).collect(Collectors.toList()), false).subscribe();
				
				actionMode.finish();
				return true;
			}
			//Archive conversations
			else if(menuItem.getItemId() == R.id.action_archive) {
				//Getting the conversation list
				final List<ConversationInfo> selectedConversations = selectionStream.collect(Collectors.toList());
				
				//Archiving the conversations
				pluginRXD.activity().add(ConversationActionTask.archiveConversations(selectedConversations, true)
						.subscribe(() -> {
							//Creating a snackbar
							int affectedCount = selectedConversations.size();
							Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
								//Unarchiving the conversations
								ConversationActionTask.archiveConversations(selectedConversations, false).subscribe();
							}).show();
						}));
				
				//Finishing the action mode
				actionMode.finish();
				
				//Returning true
				return true;
			}
			//Unarchive conversations
			else if(menuItem.getItemId() == R.id.action_unarchive) {
				//Getting the conversation list
				final List<ConversationInfo> selectedConversations = selectionStream.collect(Collectors.toList());
				
				//Unarchiving the conversations
				pluginRXD.activity().add(ConversationActionTask.archiveConversations(selectedConversations, false)
						.subscribe(() -> {
							//Creating a snackbar
							int affectedCount = selectedConversations.size();
							Snackbar.make(findViewById(R.id.root), getResources().getQuantityString(R.plurals.message_conversationunarchived, affectedCount, affectedCount), Snackbar.LENGTH_LONG).setAction(R.string.action_undo, view -> {
								//Re-archiving the conversations
								ConversationActionTask.archiveConversations(selectedConversations, true).subscribe();
							}).show();
						}));
				
				//Finishing the action mode
				actionMode.finish();
				
				//Returning true
				return true;
			}
			//Delete conversations
			else if(menuItem.getItemId() == R.id.action_delete) {
				//Displaying a dialog
				new MaterialAlertDialogBuilder(Conversations.this)
						//Setting the message
						.setMessage(getResources().getQuantityString(R.plurals.message_confirm_deleteconversation, selectedCount))
						//Setting the button
						.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
						.setPositiveButton(R.string.action_delete, (dialog, which) -> {
							//Deleting the conversations
							ConversationActionTask.deleteConversations(Conversations.this, selectionStream.collect(Collectors.toList())).subscribe();
							
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
		
		void onItemCheckedStateToggled(ConversationInfo item) {
			boolean newSelectionState = !viewModel.actionModeSelections.contains(item.getLocalID());
			
			//Setting the item's checked state
			if(newSelectionState) viewModel.actionModeSelections.add(item.getLocalID());
			else viewModel.actionModeSelections.remove(item.getLocalID());
			
			//Updating the view
			conversationRecyclerAdapter.notifyItemChanged(viewModel.conversationList.indexOf(item), conversationPayloadSelection);
			
			//Updating the selected conversations
			int value = newSelectionState ? 1 : -1;
			selectedCount += value;
			if(item.isMuted()) mutedCount += value;
			else unmutedCount += value;
			if(item.isArchived()) archivedCount += value;
			else unarchivedCount += value;
			
			//Updating the context
			updateActionModeContext();
			
			//Finishing the action mode if there are no more items selected
			if(selectedCount == 0) actionMode.finish();
		}
		
		void updateActionModeContext() {
			//Updating the title
			actionMode.setTitle(getResources().getQuantityString(R.plurals.message_selectioncount, selectedCount, selectedCount));
			
			//Showing or hiding the mute / unmute buttons
			actionMode.getMenu().findItem(R.id.action_unmute).setVisible(mutedCount > 0);
			actionMode.getMenu().findItem(R.id.action_mute).setVisible(unmutedCount > 0);
			
			//Showing or hiding the archive / unarchive buttons
			actionMode.getMenu().findItem(R.id.action_unarchive).setVisible(archivedCount > 0);
			actionMode.getMenu().findItem(R.id.action_archive).setVisible(unarchivedCount > 0);
		}
	}
}