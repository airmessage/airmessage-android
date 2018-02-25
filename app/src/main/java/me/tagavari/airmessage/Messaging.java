package me.tagavari.airmessage;

import android.Manifest;
import android.animation.Animator;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import me.tagavari.airmessage.common.SharedValues;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class Messaging extends AppCompatActivity {
	//Creating the static values
	private static final List<WeakReference<Messaging>> loadedConversations = new ArrayList<>();
	
	//Creating the activity values
	private ConversationManager.ConversationInfo conversationInfo;
	
	private final Runnable conversationTitleChangeListener = new Runnable() {
		@Override
		public void run() {
			//Returning if the conversation info is invalid
			if(conversationInfo == null) return;
			
			//Building the conversation title
			conversationInfo.buildTitle(Messaging.this, (result, wasTasked) -> {
				//Setting the title in the app bar
				getSupportActionBar().setTitle(result);
				
				//Updating the task description
				lastTaskDescription = new ActivityManager.TaskDescription(result, lastTaskDescription.getIcon(), conversationInfo.getConversationColor());
				setTaskDescription(lastTaskDescription);
			});
		}
	};
	
	//Creating the send values
	private File currentSendFile;
	
	private boolean currentScreenEffectPlaying = false;
	private String currentScreenEffect = "";
	
	//Creating the state values
	private static final byte appBarStateDefault = 0;
	private static final byte appBarStateSearch = 1;
	private byte currentAppBarState = appBarStateDefault;
	
	private int searchCount = 0;
	private int searchIndex = 0;
	private final ArrayList<ConversationManager.MessageInfo> searchFilteredMessages = new ArrayList<>();
	private final List<Integer> searchListIndexes = new ArrayList<>();
	
	private static final String retainedFragmentTag = RetainedFragment.class.getName();
	private RetainedFragment retainedFragment;
	
	//Creating the broadcast values
	private final BroadcastReceiver clientConnectionResultBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Getting the result
			final byte result = intent.getByteExtra(Constants.intentParamResult, ConnectionService.intentResultValueConnection);
			
			//Showing the server warning
			serverWarningBar.post(() -> {
				//Hiding the server warning bar if the connection is successful
				if(result == ConnectionService.intentResultValueSuccess) hideServerWarning();
					//Otherwise showing the warning
				else showServerWarning(result);
			});
		}
	};
	
	//Creating the view values
	private RecyclerView messageList;
	private View inputBar;
	private View referenceBar;
	private ViewGroup messageBar;
	private View contentBar;
	private View recordingBar;
	private EditText messageInputField;
	private ImageButton messageSendButton;
	private ImageButton messageContentButton;
	private ImageButton contentCloseButton;
	private ImageButton contentRecordButton;
	private View recordingIndicator;
	private TextView recordingTimeLabel;
	private ViewGroup serverWarningBar;
	
	//Creating the menu values
	private boolean menuLoaded = false;
	private MenuItem archiveMenuItem;
	private MenuItem unarchiveMenuItem;
	
	//Creating the listeners
	private final View.OnTouchListener recordingTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			//Performing the click
			view.performClick();
			
			//Checking if the input state is content and the action is a down touch
			if(retainedFragment.inputState == InputState.CONTENT && motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				//Attempting to start recording
				boolean result = startRecording();
				
				//Moving the recording indicator if the recording could be started
				if(result)
					recordingIndicator.setX(motionEvent.getRawX() - (float) recordingIndicator.getWidth() / 2f);
				
				//Returning true
				return true;
			}
			//Returning false
			return false;
		}
	};
	
	//Creating the other values
	private RecyclerAdapter messageListAdapter = null;
	private boolean serverWarningVisible = false;
	private boolean messageBoxHasText = false;
	private ActivityManager.TaskDescription lastTaskDescription;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_messaging);
		
		//Getting the views
		messageList = findViewById(R.id.list_messages);
		inputBar = findViewById(R.id.inputbar);
		messageSendButton = inputBar.findViewById(R.id.button_send);
		referenceBar = inputBar.findViewById(R.id.referencebar);
		messageBar = inputBar.findViewById(R.id.messagebar);
		contentBar = inputBar.findViewById(R.id.contentbar);
		recordingBar = inputBar.findViewById(R.id.recordingbar);
		messageInputField = inputBar.findViewById(R.id.messagebox);
		messageContentButton = inputBar.findViewById(R.id.button_addcontent);
		contentCloseButton = inputBar.findViewById(R.id.button_closecontent);
		contentRecordButton = inputBar.findViewById(R.id.button_record);
		recordingIndicator = findViewById(R.id.recordingindicator);
		recordingTimeLabel = inputBar.findViewById(R.id.recordingtime);
		serverWarningBar = findViewById(R.id.serverwarning);
		
		//Enforcing the maximum content width
		Constants.enforceContentWidth(getResources(), messageList);
		
		//Setting up the retained fragment
		prepareRetainedFragment();
		
		//Restoring the input bar state
		restoreInputBarState();
		
		//Restoring the text states
		findViewById(R.id.loading_text).setVisibility(retainedFragment.messagesState == RetainedFragment.messagesStateLoading ? View.VISIBLE : View.GONE);
		
		//Enabling the toolbar's up navigation
		//setActionBar((Toolbar) findViewById(R.id.toolbar));
		//getActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Setting the listeners
		messageInputField.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//Getting if the message box has text
				messageBoxHasText = stringHasChar(s.toString());
				
				//Updating the send button
				updateSendButton();
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		messageSendButton.setOnClickListener(view -> {
			//Returning if the input state is not text
			if(retainedFragment.inputState != InputState.MESSAGE) return;
			
			//Checking if the message box has text
			if(messageBoxHasText) {
				//Getting the message
				String message = messageInputField.getText().toString();
				
				//Trimming the message
				message = message.trim();
				
				//Returning if the message is empty
				if(message.isEmpty()) return;
				
				//Creating a message
				ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(-1, null, conversationInfo, null, message, "", System.currentTimeMillis(), SharedValues.MessageInfo.stateCodeGhost, Constants.messageErrorCodeOK, -1);
				
				//Writing the message to the database
				new AddGhostMessageTask(getApplicationContext(), messageInfo).execute();
				
				//Adding the message to the conversation in memory
				conversationInfo.addGhostMessage(this, messageInfo);
				
				//Sending the message
				messageInfo.sendMessage(this);
				
				//Clearing the message box
				messageInputField.setText("");
				messageInputField.invalidate(); //Height of input field doesn't update otherwise
				messageBoxHasText = false;
				
				//Scrolling to the bottom of the chat
				if(messageListAdapter != null) messageListAdapter.scrollToBottom();
			}
		});
		messageContentButton.setOnClickListener(view -> showContentBar());
		contentCloseButton.setOnClickListener(view -> hideContentBar());
		inputBar.findViewById(R.id.button_camera).setOnClickListener(view -> requestTakePicture());
		inputBar.findViewById(R.id.button_gallery).setOnClickListener(view -> requestMediaFile());
		inputBar.findViewById(R.id.button_attach).setOnClickListener(view -> requestAnyFile());
		contentRecordButton.setOnTouchListener(recordingTouchListener);
		
		//Getting the conversation info
		retainedFragment.conversationID = getIntent().getLongExtra(Constants.intentParamTargetID, -1);
		ConversationManager.ConversationInfo conversationInfo = ConversationManager.findConversationInfo(retainedFragment.conversationID);
		
		//Checking if the conversation info is invalid
		if(conversationInfo == null) {
			//Disabling the message bar
			setMessageBarState(false);
			
			//Showing the loading text
			findViewById(R.id.loading_text).setVisibility(View.VISIBLE);
			
			//Loading the conversation
			retainedFragment.loadConversation(getApplicationContext());
		} else {
			//Applying the conversation
			applyConversation(conversationInfo);
		}
		
		//Setting the filler data
		if(getIntent().hasExtra(Constants.intentParamDataText))
			messageInputField.setText(getIntent().getStringExtra(Constants.intentParamDataText));
	}
	
	private void prepareRetainedFragment() {
		//Getting the send file task fragment
		FragmentManager fragmentManager = getFragmentManager();
		retainedFragment = (RetainedFragment) fragmentManager.findFragmentByTag(retainedFragmentTag);
		
		//Checking if the fragment is invalid
		if(retainedFragment == null) { //If the fragment is valid, it has been retrieved from across a config change
			//Creating and adding the fragment
			retainedFragment = new RetainedFragment();
			fragmentManager.beginTransaction().add(retainedFragment, retainedFragmentTag).commit();
		}
	}
	
	void applyConversation(ConversationManager.ConversationInfo conversationInfo) {
		//Checking if the conversation info is invalid
		if(conversationInfo == null) {
			//Finishing the activity
			finish();
			return;
		}
		
		//Setting the conversation info
		this.conversationInfo = conversationInfo;
		
		//Enabling the messaging bar
		setMessageBarState(true);
		
		//Setting the conversation title
		conversationInfo.buildTitle(Messaging.this, (result, wasTasked) -> {
			getSupportActionBar().setTitle(result);
			setTaskDescription(lastTaskDescription = new ActivityManager.TaskDescription(result, BitmapFactory.decodeResource(getResources(), R.drawable.app_icon), conversationInfo.getConversationColor()));
		});
		
		//Setting up the menu buttons
		if(menuLoaded) {
			if(conversationInfo.isArchived()) unarchiveMenuItem.setVisible(true);
			else archiveMenuItem.setVisible(true);
		}
		
		//Setting the list adapter
		//messageList.setLayoutManager(new SpeedyLinearLayoutManager(this));
		messageListAdapter = new RecyclerAdapter(retainedFragment.conversationItemList);
		conversationInfo.setAdapterUpdater(new AdapterUpdater(this));
		messageList.setAdapter(messageListAdapter);
		
		//Setting the title listener
		conversationInfo.addTitleChangeListener(conversationTitleChangeListener);
		
		//Setting the conversation effect callbacks
		conversationInfo.setEffectCallbacks(new EffectCallbacks(this));
		
		//Setting the message input field hint
		messageInputField.setHint(getInputBarMessage());
		
		//Updating the send button
		updateSendButton();
		
		//Loading the messages
		if(retainedFragment.messagesState == RetainedFragment.messagesStateLoaded) onMessagesLoaded();
		else loadMessages();
	}
	
	void onMessagesLoaded() {
		//Hiding the loading text
		findViewById(R.id.loading_text).setVisibility(View.GONE);
		
		//Finding the latest send effect
		for(int i = retainedFragment.conversationItemList.size() - 1; i >= 0; i--) {
			//Getting the conversation item
			ConversationManager.ConversationItem conversationItem = retainedFragment.conversationItemList.get(i);
			
			//Breaking from the loop if the item has already been viewed
			if(conversationItem.getDate() < conversationInfo.getTimeLastViewed()) break;
			
			//Skipping the remainder of the iteration if the item is not a message
			if(!(conversationItem instanceof ConversationManager.MessageInfo)) continue;
			
			//Getting the message
			ConversationManager.MessageInfo messageInfo = (ConversationManager.MessageInfo) conversationItem;
			
			//Skipping the remainder of the iteration if the message has no send effect
			if(messageInfo.getSendEffect().isEmpty()) continue;
			
			//Setting the send effect
			currentScreenEffect = messageInfo.getSendEffect();
			
			//Breaking from the loop
			break;
		}
		
		//Playing the send effect if there is one
		if(!currentScreenEffect.isEmpty()) playCurrentSendEffect();
	}
	
	/* void onMessagesFailed() {
		//Hiding the loading text
		findViewById(R.id.loading_text).setVisibility(View.GONE);
		
		//Showing the failed text
		findViewById(R.id.error_text).setVisibility(View.VISIBLE);
	} */
	
	public void onAudioRecordingTimeUpdate(String time) {
		recordingTimeLabel.setText(time);
	}
	
	@Override
	protected void onStart() {
		//Calling the super method
		super.onStart();
		
		//Adding the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(clientConnectionResultBroadcastReceiver, new IntentFilter(ConnectionService.localBCResult));
	}
	
	@Override
	protected void onResume() {
		//Calling the super method
		super.onResume();
		
		//Adding the phantom reference
		loadedConversations.add(new WeakReference<>(this));
		
		//Checking if the conversation is valid
		if(conversationInfo != null) {
			//Clearing the notifications
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int) conversationInfo.getLocalID());
			
			//Coloring the UI
			colorUI(findViewById(android.R.id.content));
			
			//Coloring the messages
			if(retainedFragment.conversationItemList != null) for(ConversationManager.ConversationItem conversationItem : retainedFragment.conversationItemList) conversationItem.updateViewColor(getResources());
			
			//Updating the recycler adapter's views
			//messageListAdapter.notifyDataSetChanged();
		}
		
		//Updating the server warning bar state
		ConnectionService connectionService = ConnectionService.getInstance();
		boolean showWarning = connectionService == null || (!connectionService.isConnected() && !connectionService.isConnecting() && ConnectionService.lastConnectionResult != -1);
		if(showWarning) showServerWarning(ConnectionService.lastConnectionResult);
		else hideServerWarning();
	}
	
	@Override
	protected void onPause() {
		//Calling the super method
		super.onPause();
		
		//Clearing the phantom reference
		for(Iterator<WeakReference<Messaging>> iterator = loadedConversations.iterator(); iterator.hasNext();) {
			//Getting the referenced activity
			Messaging activity = iterator.next().get();
			
			//Removing the reference if it is invalid
			if(activity == null) {
				iterator.remove();
				continue;
			}
			//Skipping the remainder of the iteration if the activity isn't this one
			else if(activity != this) continue;
			
			//Removing the reference
			iterator.remove();
			
			//Breaking from the loop
			break;
		}
	}
	
	@Override
	protected void onStop() {
		//Calling the super method
		super.onStop();
		
		//Checking if the activity is being destroyed
		if(isFinishing()) {
			/* //Checking the conversation if the list is empty and the messages have been loaded
			if(conversationInfo.getConversationItems().isEmpty()) {
				//Removing the conversation from memory
				ConversationManager.getConversations().remove(conversationInfo);
				
				//Deleting the conversation from the database
				DatabaseManager.deleteConversation(DatabaseManager.getWritableDatabase(this), conversationInfo);
				
				//Updating the conversation activity list
				LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Conversations.localBCConversationUpdate));
			} */
			
			//Cleaning up the media recorder
			invalidateMediaRecorder();
			
			//Cleaning up the media player
			getAudioMessageManager().release();
			
			//Checking if the conversation is valid
			if(conversationInfo != null) {
				//Clearing the messages
				conversationInfo.clearMessages();
				
				//Setting the conversation as unloaded
				loadedConversations.remove(conversationInfo.getLocalID());
				
				//Updating the conversation's last view time
				long lastViewTime = System.currentTimeMillis();
				conversationInfo.setTimeLastViewed(lastViewTime);
				conversationInfo.updateUnreadStatus();
				new UpdateLastViewTimeTask(getApplicationContext(), conversationInfo.getLocalID(), lastViewTime).execute();
			}
		} else {
			//Setting the restarting from config change variable to true
			retainedFragment.restartingFromConfigChange = true;
		}
		
		//Removing the conversation title change listener
		if(conversationInfo != null) conversationInfo.removeTitleChangeListener(conversationTitleChangeListener);
		
		//Removing the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.unregisterReceiver(clientConnectionResultBroadcastReceiver);
	}
	
	@Override
	public void finish() {
		//Calling the super method
		super.finish();
		
		//Overriding the transition
		//overridePendingTransition(R.anim.fade_in_light, R.anim.slide_out);
	}
	
	long getConversationID() {
		return retainedFragment.conversationID;
	}
	
	private void restoreInputBarState() {
		switch(retainedFragment.inputState) {
			case CONTENT:
				//Disabling the message bar
				messageContentButton.setEnabled(false);
				messageInputField.setEnabled(false);
				messageSendButton.setEnabled(false);
				
				//Showing the content bar
				contentCloseButton.setRotation(45);
				contentBar.setVisibility(View.VISIBLE);
				
				break;
			case RECORDING:
				//Disabling the message bar
				messageContentButton.setEnabled(false);
				messageInputField.setEnabled(false);
				messageSendButton.setEnabled(false);
				
				//Showing the recording bar
				recordingBar.setVisibility(View.VISIBLE);
				
				//Waiting until the view has been loaded
				messageList.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						//Removing the layout listener
						messageList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
						
						//Positioning and showing the recording indicator
						recordingIndicator.setY(inputBar.getTop() + inputBar.getHeight() / 2 - recordingIndicator.getHeight() / 2);
						recordingIndicator.setVisibility(View.VISIBLE);
					}
				});
				
				break;
		}
		
		//Reconnecting the media player to its attachment
		//getAudioMessageManager().reconnectAttachment(conversationInfo);
	}
	
	private void loadMessages() {
		//Returning if the messages don't need to be loaded
		if(retainedFragment.messagesState != RetainedFragment.messagesStateUnloaded) return;
		
		//Checking if the messages are already loaded (the GC hasn't removed them yet)
		/* if(retainedFragment.conversationItemList != null) {
			//Setting the messages as loaded
			retainedFragment.messagesState = RetainedFragment.messagesStateLoaded;
			
			//Applying the messages
			onMessagesLoaded();
			
			//Returning
			return;
		} */
		
		//Showing the loading text
		findViewById(R.id.loading_text).setVisibility(View.VISIBLE);
		
		//Telling the retained fragment to load the messages
		retainedFragment.loadMessages(conversationInfo);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Checking if the request code is taking a picture
		if(requestCode == Constants.intentTakePicture) {
			//Returning if the current input state is not the content bar
			if(retainedFragment.inputState != InputState.CONTENT) return;
			
			//Checking if the result was a success
			if(resultCode == RESULT_OK) {
				//Sending the file
				sendFile(currentSendFile);
			}
		}
		//Otherwise if the request code is the media picker
		else if(requestCode == Constants.intentPickMediaFile || requestCode == Constants.intentPickAnyFile) {
			//Returning if the current input state is not the content bar
			if(retainedFragment.inputState != InputState.CONTENT) return;
			
			//Checking if the result was a success
			if(resultCode == RESULT_OK) {
				//Getting the content
				Uri content = data.getData();
				if(content == null) return;
				
				//Sending the file
				sendFile(content);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Inflating the menu resource
		getMenuInflater().inflate(R.menu.menu_messaging, menu);
		
		//Getting the menu items
		archiveMenuItem = menu.findItem(R.id.action_archive);
		unarchiveMenuItem = menu.findItem(R.id.action_unarchive);
		
		//Showing the correct "archive" menu item
		if(conversationInfo != null) {
			if(conversationInfo.isArchived()) unarchiveMenuItem.setVisible(true);
			else archiveMenuItem.setVisible(true);
		}
		
		//Setting the menu as loaded
		menuLoaded = true;
		
		//Returning true
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				//Checking if the state is in a search
				if(currentAppBarState == appBarStateSearch) {
					//Setting the state to normal
					setAppBarState(appBarStateDefault);
				} else {
					//Finishing the activity
					finish();
				}
				return true;
			case R.id.action_details:
				//Checking if the conversation is valid
				if(conversationInfo != null) {
					//Launching the details activity
					startActivity(new Intent(this, MessagingInfo.class).putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID()));
				}
				
				return true;
			case R.id.action_search:
				//Checking if the conversation is valid
				if(conversationInfo != null) {
					//Setting the state to search
					setAppBarState(appBarStateSearch);
				}
				
				return true;
			case R.id.action_archive:
				//Checking if the conversation is valid
				if(conversationInfo != null) {
					//Archiving the conversation
					conversationInfo.setArchived(true);
					
					//Updating the conversation's database entry
					ContentValues contentValues = new ContentValues();
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, true);
					DatabaseManager.updateConversation(DatabaseManager.getWritableDatabase(Messaging.this), conversationInfo.getLocalID(), contentValues);
					
					//Showing a toast
					Toast.makeText(Messaging.this, R.string.conversation_archived, Toast.LENGTH_SHORT).show();
					
					//Swapping out the menu buttons
					archiveMenuItem.setVisible(false);
					unarchiveMenuItem.setVisible(true);
				}
				return true;
			case R.id.action_unarchive:
				//Checking if the conversation is valid
				if(conversationInfo != null) {
					//Unarchiving the conversation
					conversationInfo.setArchived(false);
					
					//Updating the conversation's database entry
					ContentValues contentValues = new ContentValues();
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, false);
					DatabaseManager.updateConversation(DatabaseManager.getWritableDatabase(Messaging.this), conversationInfo.getLocalID(), contentValues);
					
					//Showing a toast
					Toast.makeText(Messaging.this, R.string.conversation_unarchived, Toast.LENGTH_SHORT).show();
					
					//Swapping out the menu buttons
					archiveMenuItem.setVisible(true);
					unarchiveMenuItem.setVisible(false);
				}
				return true;
			case R.id.action_delete:
				//Checking if the conversation is valid
				if(conversationInfo != null) {
					//Creating a dialog
					new AlertDialog.Builder(this)
							.setMessage(R.string.dialog_deleteconversation_current)
							.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
								dialog.dismiss();
							})
							.setPositiveButton(R.string.action_delete, (dialog, which) -> {
								//Removing the conversation from memory
								ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
								if(conversations != null) conversations.remove(conversationInfo);
								
								//Deleting the conversation from the database
								DatabaseManager.deleteConversation(DatabaseManager.getReadableDatabase(Messaging.this), conversationInfo);
								
								//Showing a toast
								Toast.makeText(Messaging.this, R.string.conversation_deleted, Toast.LENGTH_SHORT).show();
								
								//Finishing the activity
								finish();
							})
							.create()
							.show();
				}
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
		} else {
			//Calling the super method
			super.onBackPressed();
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		//Returning false if the current state isn't recording
		if(retainedFragment.inputState != InputState.RECORDING) {
			super.dispatchTouchEvent(event);
			return false;
		}
		
		//Checking if the action is a move touch
		if(event.getAction() == MotionEvent.ACTION_MOVE) {
			//Moving the recording indicator
			recordingIndicator.setX(event.getRawX() - (float) recordingIndicator.getWidth() / 2f);
			
			//Returning true
			return true;
		}
		//Checking if the action is an up touch
		else if(event.getAction() == MotionEvent.ACTION_UP) {
			//Creating the force discard variable
			boolean forceDiscard = false;
			
			//Getting the display width
			DisplayMetrics displaymetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			int width = displaymetrics.widthPixels;
			
			//Checking if the layout is left-to-right
			if(getResources().getBoolean(R.bool.is_left_to_right)) {
				//Setting force discard to true if the touch is on the left 20% of the display
				if(event.getRawX() < (float) width / 5) forceDiscard = true;
			} else {
				//Setting force discard to true if the touch is on the right 20% of the display
				if(event.getRawX() > (float) width * 0.8) forceDiscard = true;
			}
			
			//Stopping recording
			stopRecording(forceDiscard, (int) event.getRawX());
			
			//Returning true
			return true;
		}
		
		//Returning false
		super.dispatchTouchEvent(event);
		return false;
	}
	
	private void colorUI(ViewGroup root) {
		//Getting the color
		int color = conversationInfo.getConversationColor();
		
		//Coloring the app and status bar
		getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
		getWindow().setStatusBarColor(ColorHelper.darkenColor(color));
		
		//Updating the task description
		if(lastTaskDescription != null) setTaskDescription(lastTaskDescription = new ActivityManager.TaskDescription(lastTaskDescription.getLabel(), lastTaskDescription.getIcon(), color));
		
		//Coloring parts of the UI
		for(View view : Constants.getViewsByTag(root, getResources().getString(R.string.tag_primarytint))) {
			if(view instanceof ImageView) ((ImageView) view).setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
			else if(view instanceof Button) ((Button) view).setTextColor(color);
			else if(view instanceof RelativeLayout) view.setBackground(new ColorDrawable(color));
			else if(view instanceof Switch) {
				Switch switchView = (Switch) view;
				switchView.setThumbTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0xFFFAFAFA, color}));
				switchView.setTrackTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0x61000000, color}));
			}
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
		}
		else actionBar.setDisplayShowCustomEnabled(true);
		
		//Cleaning up the old state
		switch(currentAppBarState) {
			case appBarStateSearch:
				//Hiding the search results bar
				findViewById(R.id.searchresults).setVisibility(View.GONE);
				
				break;
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
						//Returning if the conversation details are invalid
						if(conversationInfo == null || retainedFragment.conversationItemList == null) return;
						
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
						
						for(int i = 0; i < retainedFragment.conversationItemList.size(); i++) {
							ConversationManager.ConversationItem conversationItem = retainedFragment.conversationItemList.get(i);
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
						if(!searchListIndexes.isEmpty()) messageList.getLayoutManager().scrollToPosition(searchListIndexes.get(searchIndex));
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
	
	public void onClickSearchPrevious(View view) {
		//Returning if the index cannot be decreased
		if(searchIndex == 0) return;
		
		//Updating the search
		searchIndex--;
		((TextView) findViewById(R.id.searchresults_message)).setText(getResources().getString(R.string.search_results, searchIndex + 1, searchCount));
		if(!searchListIndexes.isEmpty()) messageList.getLayoutManager().scrollToPosition(searchListIndexes.get(searchIndex));
	}
	
	public void onClickSearchNext(View view) {
		//Returning if the index cannot be increased
		if(searchIndex >= searchCount - 1) return;
		
		//Updating the search
		searchIndex++;
		((TextView) findViewById(R.id.searchresults_message)).setText(getResources().getString(R.string.search_results, searchIndex + 1, searchCount));
		if(!searchListIndexes.isEmpty()) messageList.getLayoutManager().scrollToPosition(searchListIndexes.get(searchIndex));
	}
	
	void setMessageBarState(boolean enabled) {
		//Setting the message input field
		messageInputField.setEnabled(enabled);
		
		//Setting the send button
		messageSendButton.setEnabled(enabled);
		//messageSendButton.setClickable(messageBoxHasText);
		messageSendButton.setAlpha(enabled && messageBoxHasText ? 1 : 0.38f);
		
		//Setting the add content button
		messageContentButton.setEnabled(enabled);
		messageContentButton.setAlpha(enabled ? 1 : 0.38f);
		
		//Opening the soft keyboard if the text input is enabled and the soft keyboard should be used
		//if(enabled) ((InputMethodManager) activitySource.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(messageInputField, InputMethodManager.SHOW_FORCED);
	}
	
	private void updateSendButton() {
		//Setting the send button state
		messageSendButton.setClickable(messageBoxHasText);
		messageSendButton.setAlpha(messageBoxHasText ? 1 : 0.38f);
	}
	
	private void showContentBar() {
		//Setting the input state to the content bar
		retainedFragment.inputState = InputState.CONTENT;
		
		//Disabling the message bar
		TransitionManager.beginDelayedTransition((ViewGroup) messageBar, new ChangeBounds());
		messageBar.getLayoutParams().height = referenceBar.getHeight();
		
		messageContentButton.setEnabled(false);
		messageInputField.setEnabled(false);
		messageSendButton.setEnabled(false);
		
		//Rotating the add content button
		messageContentButton.animate().rotation(45).start();
		
		//Rotating the close content button
		contentCloseButton.animate().rotation(45).start();
		
		//Revealing the content bar
		contentBar.setVisibility(View.VISIBLE);
		Animator animator = ViewAnimationUtils.createCircularReveal(contentBar, referenceBar.getLeft() + (int) messageContentButton.getX() + messageContentButton.getWidth() / 2, referenceBar.getTop() + referenceBar.getHeight() / 2, 0, referenceBar.getWidth());
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.start();
		
		//Enabling the content bar
		contentCloseButton.setEnabled(true);
		contentBar.findViewById(R.id.button_camera).setEnabled(true);
		contentBar.findViewById(R.id.button_gallery).setEnabled(true);
		contentBar.findViewById(R.id.button_attach).setEnabled(true);
		contentRecordButton.setEnabled(true);
	}
	
	private void hideContentBar() {
		//Setting the input state to the message bar
		retainedFragment.inputState = InputState.MESSAGE;
		
		//Disabling the content bar
		contentCloseButton.setEnabled(false);
		contentBar.findViewById(R.id.button_camera).setEnabled(false);
		contentBar.findViewById(R.id.button_gallery).setEnabled(false);
		contentBar.findViewById(R.id.button_attach).setEnabled(false);
		contentRecordButton.setEnabled(false);
		
		//Rotating the add content button
		messageContentButton.animate().rotation(0).start();
		
		//Rotating the close content button
		contentCloseButton.animate().rotation(0).start();
		
		//Concealing the content bar
		Animator animator = ViewAnimationUtils.createCircularReveal(contentBar, referenceBar.getLeft() + (int) messageContentButton.getX() + messageContentButton.getWidth() / 2, referenceBar.getTop() + referenceBar.getHeight() / 2, referenceBar.getWidth(), 0);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
			
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				//Returning if the state doesn't match
				if(retainedFragment.inputState == InputState.CONTENT) return;
				
				//Hiding the content bar
				contentBar.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
			
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
			
			}
		});
		animator.start();
		
		//Enabling the message bar
		TransitionManager.beginDelayedTransition((ViewGroup) messageBar, new ChangeBounds());
		messageBar.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
		messageContentButton.setEnabled(true);
		messageInputField.setEnabled(true);
		messageSendButton.setEnabled(true);
	}
	
	private void showRecordingBar() {
		//Setting the input state to recording
		retainedFragment.inputState = InputState.RECORDING;
		
		//Disabling the content bar
		contentCloseButton.setEnabled(false);
		contentBar.findViewById(R.id.button_camera).setEnabled(false);
		contentBar.findViewById(R.id.button_gallery).setEnabled(false);
		contentBar.findViewById(R.id.button_attach).setEnabled(false);
		contentBar.findViewById(R.id.button_record).setEnabled(false);
		
		//Revealing the recording bar
		int[] recordingButtonLocation = {0, 0};
		contentRecordButton.getLocationOnScreen(recordingButtonLocation);
		
		recordingBar.setVisibility(View.VISIBLE);
		Animator animator = ViewAnimationUtils.createCircularReveal(recordingBar, referenceBar.getLeft() + recordingButtonLocation[0] + contentRecordButton.getWidth() / 2, referenceBar.getTop() + referenceBar.getHeight() / 2, 0, referenceBar.getWidth());
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
			
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				//Returning if the state doesn't match
				if(retainedFragment.inputState == InputState.CONTENT) return;
				
				//Hiding the content bar
				contentBar.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
			
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
			
			}
		});
		animator.start();
	}
	
	private void hideRecordingBar(boolean toMessageInput, int positionX) {
		//Setting the input state to the message input or content bar
		retainedFragment.inputState = toMessageInput ? InputState.MESSAGE : InputState.CONTENT;
		
		//Concealing the recording bar
		Animator animator = ViewAnimationUtils.createCircularReveal(recordingBar, referenceBar.getLeft() + positionX, referenceBar.getTop() + referenceBar.getHeight() / 2, referenceBar.getWidth(), 0);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
			
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				//Returning if the state doesn't match
				if(retainedFragment.inputState == InputState.RECORDING)
					return;
				
				//Hiding the recording bar
				recordingBar.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
			
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
			
			}
		});
		animator.start();
		
		//Checking if the target is the message input
		if(toMessageInput) {
			//Enabling the message bar
			messageContentButton.setRotation(0);
			messageContentButton.setEnabled(true);
			messageInputField.setEnabled(true);
			messageSendButton.setEnabled(true);
		}
		//Otherwise the target is the content bar
		else {
			//Enabling the content bar
			contentBar.setVisibility(View.VISIBLE);
			contentCloseButton.setRotation(45);
			contentCloseButton.setEnabled(true);
			contentBar.findViewById(R.id.button_camera).setEnabled(true);
			contentBar.findViewById(R.id.button_gallery).setEnabled(true);
			contentBar.findViewById(R.id.button_attach).setEnabled(true);
			contentRecordButton.setEnabled(true);
		}
	}
	
	private boolean startRecording() {
		//Setting up the media recorder
		boolean result = setupMediaRecorder();
		
		//Returning false if the media recorder couldn't be set up
		if(!result) return false;
		
		//Finding a target file
		retainedFragment.mediaRecorderFile = MainApplication.findUploadFileTarget(this, Constants.recordingName);
		
		try {
			//Creating the targets
			if(!retainedFragment.mediaRecorderFile.getParentFile().mkdir()) throw new IOException();
			//if(!targetFile.createNewFile()) throw new IOException();
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning
			return false;
		}
		
		//Setting the media recorder file
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) retainedFragment.mediaRecorder.setOutputFile(retainedFragment.mediaRecorderFile);
		else retainedFragment.mediaRecorder.setOutputFile(retainedFragment.mediaRecorderFile.getAbsolutePath());
		
		try {
			//Preparing the media recorder
			retainedFragment.mediaRecorder.prepare();
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning
			return false;
		}
		
		//Resetting the recording duration
		retainedFragment.recordingDuration = 0;
		
		//Setting the recording time text
		recordingTimeLabel.setText(RetainedFragment.getFormattedDuration(0));
		
		//Showing the cancel text
		//recordingBar.findViewById(R.id.recording_canceltext).setVisibility(View.VISIBLE);
		
		//Setting the recording indicator's Y
		recordingIndicator.setY(inputBar.getTop() + inputBar.getHeight() / 2 - recordingIndicator.getHeight() / 2);
		
		//Revealing the indicator
		recordingIndicator.setVisibility(View.VISIBLE);
		Animator animator = ViewAnimationUtils.createCircularReveal(recordingIndicator, recordingIndicator.getLeft() + recordingIndicator.getWidth() / 2, recordingIndicator.getTop() + recordingIndicator.getWidth() / 2, 0, recordingIndicator.getWidth());
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.start();
		
		//Starting the timer
		retainedFragment.recordingTimerHandler.postDelayed(retainedFragment.recordingTimerHandlerRunnable, 1000);
		
		//Showing the recording input
		showRecordingBar();
		
		//Starting the media recorder
		retainedFragment.mediaRecorder.start();
		
		//Returning true
		return true;
	}
	
	void stopRecording(boolean forceDiscard, int positionX) {
		//Returning if the input state is not recording
		if(retainedFragment.inputState != InputState.RECORDING) return;
		
		//Setting the input state to finished
		//retainedFragment.inputState = InputState.MESSAGE;
		
		//Concealing the recording indicator
		Animator animator = ViewAnimationUtils.createCircularReveal(recordingIndicator, recordingIndicator.getLeft() + recordingIndicator.getWidth() / 2, recordingIndicator.getTop() + recordingIndicator.getWidth() / 2, recordingIndicator.getWidth(), 0);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.setStartDelay(100);
		animator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				//Returning if the state doesn't match
				if(retainedFragment.inputState == InputState.RECORDING) return;
				
				//Hiding the recording indicator
				recordingIndicator.setVisibility(View.INVISIBLE);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
				
			}
		});
		animator.start();
		
		//Disabling the recording button
		//contentRecordButton.setEnabled(false);
		
		//Hiding the cancel text
		//inputBar.findViewById(R.id.recording_canceltext).setVisibility(View.GONE);
		
		//Stopping the timer
		retainedFragment.recordingTimerHandler.removeCallbacks(retainedFragment.recordingTimerHandlerRunnable);
		
		try {
			//Stopping the media recorder
			retainedFragment.mediaRecorder.stop();
		} catch(RuntimeException stopException) { //The media recorder couldn't capture any media
			//Showing a toast
			Toast.makeText(Messaging.this, R.string.recording_instructions, Toast.LENGTH_LONG).show();
			
			//Hiding the recording bar
			int[] recordingButtonLocation = {0, 0};
			contentRecordButton.getLocationOnScreen(recordingButtonLocation);
			hideRecordingBar(false, referenceBar.getLeft() + recordingButtonLocation[0]);
			
			//Returning
			return;
		}
		
		//Checking if there was not enough time
		if(retainedFragment.recordingDuration < 1) {
			//Showing a toast
			Toast.makeText(Messaging.this, R.string.recording_instructions, Toast.LENGTH_LONG).show();
			
			//Setting force discard to true
			forceDiscard = true;
		}
		
		//Resetting the media recorder
		retainedFragment.mediaRecorder.reset();
		
		//Checking if the recording should be discarded
		if(forceDiscard) {
			//Discarding the recording
			discardRecording(positionX);
			
			//Returning
			return;
		}
		
		//Hiding the recording bar
		int[] recordingButtonLocation = {0, 0};
		contentRecordButton.getLocationOnScreen(recordingButtonLocation);
		hideRecordingBar(true, recordingButtonLocation[0]);
		
		//Disabling the message bar
		//setMessageBarState(false);
		
		//Sending the file
		sendFile(retainedFragment.mediaRecorderFile);
	}
	
	void discardRecording(int positionX) {
		//Deleting the file if it exists
		if(retainedFragment.mediaRecorderFile.exists())
			retainedFragment.mediaRecorderFile.delete();
		
		//Hiding the recording bar
		hideRecordingBar(false, positionX);
	}
	
	private boolean setupMediaRecorder() {
		//Returning false if the required permissions have not been granted
		if(Constants.requestPermission(this, new String[]{Manifest.permission.RECORD_AUDIO}, Constants.permissionRecordAudio)) {
			//Notifying the user via a toast
			Toast.makeText(Messaging.this, R.string.failed_recording_permission, Toast.LENGTH_SHORT).show();
			
			//Returning false
			return false;
		}
		
		//Setting the media recorder
		if(retainedFragment.mediaRecorder == null) retainedFragment.mediaRecorder = new MediaRecorder();
		else retainedFragment.mediaRecorder.reset();
		
		//Configuring the media recorder
		retainedFragment.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		retainedFragment.mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
		retainedFragment.mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		retainedFragment.mediaRecorder.setMaxDuration(10 * 60 * 1000); //10 minutes
		
		/* //Checking if the media recorder is invalid
		if(retainedFragment.mediaRecorder == null) {
			//Notifying the user via a toast
			Toast.makeText(Messaging.this, R.string.failed_recording_error, Toast.LENGTH_SHORT).show();
			
			//Returning false
			return false;
		} */
		
		//Returning true
		return true;
	}
	
	private void invalidateMediaRecorder() {
		//Discarding the recording
		if(retainedFragment.inputState == InputState.RECORDING) {
			int[] recordingButtonLocation = {0, 0};
			contentRecordButton.getLocationOnScreen(recordingButtonLocation);
			
			discardRecording(referenceBar.getLeft() + recordingButtonLocation[0]);
		}
		
		//Releasing the media recorder
		if(retainedFragment.mediaRecorder != null) {
			retainedFragment.mediaRecorder.release();
			retainedFragment.mediaRecorder = null;
		}
	}
	
	private String getInputBarMessage() {
		//Returning a generic message if the service is invalid
		if(conversationInfo.getService() == null)
			return getResources().getString(R.string.type_a_message);
		
		switch(conversationInfo.getService()) {
			case Constants.serviceIDAppleMessage:
				return getResources().getString(R.string.imessage);
			case Constants.serviceIDSMS:
				return getResources().getString(R.string.sms);
			default:
				return conversationInfo.getService();
		}
	}
	
	void showServerWarning(byte reason) {
		//Returning if the server warning bar is already visible
		if(serverWarningVisible) return;
		
		//Setting the new state
		serverWarningVisible = true;
		
		//Setting the current state
		serverWarningVisible = true;
		
		//Getting the warning box components
		TextView message = serverWarningBar.findViewById(R.id.serverwarning_message);
		Button button = serverWarningBar.findViewById(R.id.serverwarning_button);
		
		switch(reason) {
			case ConnectionService.intentResultValueInternalException:
				message.setText(R.string.serverstatus_internalexception);
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Messaging.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
			case ConnectionService.intentResultValueBadRequest:
				message.setText(R.string.serverstatus_badrequest);
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Messaging.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
			case ConnectionService.intentResultValueClientOutdated:
				message.setText(R.string.serverstatus_clientoutdated);
				button.setText(R.string.button_update);
				button.setOnClickListener(view -> {
					//Launching the app's page in the market
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
					startActivity(intent);
				});
				break;
			case ConnectionService.intentResultValueServerOutdated:
				message.setText(R.string.serverstatus_serveroutdated);
				button.setText(R.string.button_help);
				button.setOnClickListener(view -> {
					//Launching the server update URL
					Intent intent = new Intent(Intent.ACTION_VIEW, Constants.serverUpdateAddress);
					startActivity(intent);
				});
				break;
			case ConnectionService.intentResultValueUnauthorized:
				message.setText(R.string.serverstatus_authfail);
				button.setText(R.string.button_reconfigure);
				button.setOnClickListener(view -> {
					//Launching the connection wizard activity
					startActivity(new Intent(Messaging.this, ServerSetup.class));
				});
				break;
			case ConnectionService.intentResultValueConnection:
				message.setText(R.string.serverstatus_noconnection);
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Messaging.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
			default:
				message.setText(R.string.serverstatus_unknown);
				button.setText(R.string.button_retry);
				button.setOnClickListener(view -> {
					ConnectionService connectionService = ConnectionService.getInstance();
					if(connectionService == null) {
						//Starting the service
						startService(new Intent(Messaging.this, ConnectionService.class));
					} else {
						//Reconnecting
						connectionService.reconnect();
						
						//Hiding the bar
						hideServerWarning();
					}
				});
				break;
		}
		
		//Showing the warning bar
		serverWarningBar.setVisibility(View.VISIBLE);
		
		//Enabling the button
		button.setEnabled(true);
		
		//Animating the server warning
		//serverWarningBar.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		//int wrapSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		//serverWarningBar.measure(wrapSpec, wrapSpec);
		//int barHeight = serverWarningBar.getMeasuredHeight();
		
		//Constants.ResizeAnimation resizeAnimation = new Constants.ResizeAnimation(serverWarningBar, serverWarningBar.getHeight(), barHeight);
		//resizeAnimation.setDuration(noticeBarAnimationDuration);
		//serverWarningBar.startAnimation(resizeAnimation);
		TransitionManager.beginDelayedTransition(serverWarningBar, new ChangeBounds());
		serverWarningBar.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
	}
	
	void hideServerWarning() {
		//Returning if the server warning bar is already invisible
		if(!serverWarningVisible) return;
		
		//Setting the new state
		serverWarningVisible = false;
		
		//Animating the server warning
		/* Constants.ResizeAnimation resizeAnimation = new Constants.ResizeAnimation(serverWarningBar, serverWarningBar.getHeight(), 0);
		resizeAnimation.setDuration(noticeBarAnimationDuration);
		resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				//Setting the visibility
				serverWarningBar.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			
			}
		});
		serverWarningBar.startAnimation(resizeAnimation); */
		TransitionManager.beginDelayedTransition(serverWarningBar, new ChangeBounds());
		ViewGroup.LayoutParams layoutParams = serverWarningBar.getLayoutParams();
		layoutParams.height = 0;
		serverWarningBar.setLayoutParams(layoutParams);
		
		//Disabling the action button
		serverWarningBar.findViewById(R.id.serverwarning_button).setEnabled(false);
	}
	
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		//Returning if there are no grant results
		if(grantResults.length == 0) return;
		
		//Checking if the request code is recording audio
		if(requestCode == Constants.permissionRecordAudio) {
			//Checking if the result is a denial
			if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
				//Creating a dialog
				AlertDialog dialog = new AlertDialog.Builder(Messaging.this)
						.setTitle(R.string.dialog_rejected)
						.setMessage(R.string.dialog_audio_message)
						.setPositiveButton(R.string.button_retry, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//Requesting microphone access
								Constants.requestPermission(Messaging.this, new String[]{Manifest.permission.RECORD_AUDIO}, Constants.permissionRecordAudio);
								
								//Dismissing the dialog
								dialog.dismiss();
							}
						})
						.setNeutralButton(R.string.settings, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//Showing the application settings
								Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
								intent.setData(Uri.parse("package:" + getPackageName()));
								startActivity(intent);
								
								//Dismissing the dialog
								dialog.dismiss();
							}
						})
						.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//Dismissing the dialog
								dialog.dismiss();
							}
						})
						.create();
				
				//Displaying the dialog
				dialog.show();
			}
		}
	}
	
	private void requestTakePicture() {
		//Creating the intent
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		//Checking if there are no apps that can take the intent
		if(takePictureIntent.resolveActivity(getPackageManager()) == null) {
			//Telling the user via a toast
			Toast.makeText(Messaging.this, R.string.intent_nocamera, Toast.LENGTH_SHORT).show();
			
			//Returning
			return;
		}
		
		//Finding a free file
		currentSendFile = MainApplication.findUploadFileTarget(this, Constants.pictureName);
		
		try {
			//Creating the targets
			if(!currentSendFile.getParentFile().mkdir()) throw new IOException();
			if(!currentSendFile.createNewFile()) throw new IOException();
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning
			return;
		}
		
		//Getting the content uri
		Uri imageUri = FileProvider.getUriForFile(this, MainApplication.fileAuthority, currentSendFile);
		
		//Setting the clip data
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		
		//Setting the file path (for future reference)
		takePictureIntent.putExtra(Constants.intentParamDataFile, currentSendFile);
		
		//Starting the activity
		startActivityForResult(takePictureIntent, Constants.intentTakePicture);
	}
	
	private void requestMediaFile() {
		//Launching the system's file picker
		Intent intent = new Intent();
		intent.setType("*/*");
		String[] mimeTypes = {"image/*", "video/*"};
		intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		//intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.file_selector)), Constants.intentPickMediaFile);
	}
	
	private void requestAnyFile() {
		//Launching the system's file picker
		Intent intent = new Intent();
		intent.setType("*/*");
		//String[] mimeTypes = {"image/*", "video/*"};
		//intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		//intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.file_selector)), Constants.intentPickAnyFile);
	}
	
	private static boolean stringHasChar(String string) {
		//Looping through the characters of the string
		for(char character : string.toCharArray()) {
			//Returning true if the character is not a control character
			if(!Character.isISOControl(character) && !Character.isSpaceChar(character))
				return true;
		}
		
		//Returning false
		return false;
	}
	
	public static ArrayList<Long> getLoadedConversations() {
		//Creating the list
		ArrayList<Long> list = new ArrayList<>();
		
		//Iterating over the loaded conversations
		for(Iterator<WeakReference<Messaging>> iterator = loadedConversations.iterator(); iterator.hasNext();) {
			//Getting the referenced activity
			Messaging activity = iterator.next().get();
			
			//Removing the reference if it is invalid
			if(activity == null) {
				iterator.remove();
				continue;
			}
			
			//Adding the entry to the list
			list.add(activity.getConversationID());
		}
		
		//Returning the list
		return list;
	}
	
	private void sendFile(File file) {
		//Creating a message
		ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(-1, null, conversationInfo, null, null, "", System.currentTimeMillis(), SharedValues.MessageInfo.stateCodeGhost, Constants.messageErrorCodeOK, -1);
		
		//Starting the task
		new SendFilePreparationTask(this, messageInfo, file).execute();
	}
	
	private void sendFile(Uri uri) {
		//Creating a message
		ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(-1, null, conversationInfo, null, null, "", System.currentTimeMillis(), SharedValues.MessageInfo.stateCodeGhost, Constants.messageErrorCodeOK, -1);
		
		//Starting the task
		new SendFilePreparationTask(this, messageInfo, uri).execute();
	}
	
	private static class SendFilePreparationTask extends AsyncTask<Void, Void, Void> {
		//Creating the reference values
		private final WeakReference<Messaging> activityReference;
		private final WeakReference<Context> contextReference;
		
		//Creating the request values
		private final ConversationManager.MessageInfo messageInfo;
		private File targetFile;
		private Uri targetUri;
		
		private SendFilePreparationTask(Messaging activity, ConversationManager.MessageInfo messageInfo) {
			//Setting the references
			activityReference = new WeakReference<>(activity);
			contextReference = new WeakReference<>(activity.getApplicationContext());
			
			//Setting the basic values
			this.messageInfo = messageInfo;
		}
		
		SendFilePreparationTask(Messaging activity, ConversationManager.MessageInfo messageInfo, File file) {
			//Calling the main constructor
			this(activity, messageInfo);
			
			//Setting the request values
			targetFile = file;
			targetUri = null;
		}
		
		SendFilePreparationTask(Messaging activity, ConversationManager.MessageInfo messageInfo, Uri uri) {
			//Calling the main constructor
			this(activity, messageInfo);
			
			//Setting the request values
			targetFile = null;
			targetUri = uri;
		}
		
		@Override
		protected Void doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Creating the attachment
			ConversationManager.AttachmentInfo attachment;
			if(targetFile != null) {
				switch(ConversationManager.ContentType.getType(Constants.getMimeType(targetFile))) {
					case IMAGE:
						attachment = new ConversationManager.ImageAttachmentInfo(-1, null, messageInfo, targetFile.getName(), targetFile);
						break;
					case VIDEO:
						attachment = new ConversationManager.VideoAttachmentInfo(-1, null, messageInfo, targetFile.getName(), targetFile);
						break;
					case AUDIO:
						attachment = new ConversationManager.AudioAttachmentInfo(-1, null, messageInfo, targetFile.getName(), targetFile);
						break;
					default:
						attachment = new ConversationManager.OtherAttachmentInfo(-1, null, messageInfo, targetFile.getName(), targetFile);
						break;
				}
			} else {
				String fileName = Constants.getFileName(context, targetUri);
				switch(ConversationManager.ContentType.getType(Constants.getMimeType(context, targetUri))) {
					case IMAGE:
						attachment = new ConversationManager.ImageAttachmentInfo(-1, null, messageInfo, fileName, targetUri);
						break;
					case VIDEO:
						attachment = new ConversationManager.VideoAttachmentInfo(-1, null, messageInfo, fileName, targetUri);
						break;
					case AUDIO:
						attachment = new ConversationManager.AudioAttachmentInfo(-1, null, messageInfo, fileName, targetUri);
						break;
					default:
						attachment = new ConversationManager.OtherAttachmentInfo(-1, null, messageInfo, fileName, targetUri);
						break;
				}
			}
			
			//Adding the item to the database
			messageInfo.addAttachment(attachment);
			DatabaseManager.addConversationItem(DatabaseManager.getWritableDatabase(context), messageInfo);
			
			//Returning
			return null;
		}
		
		@Override
		protected void onPostExecute(Void parameter) {
			//Getting the context
			Context context = contextReference.get();
			if(context != null) {
				//Checking if the conversation is loaded
				long currentConversationID = messageInfo.getConversationInfo().getLocalID();
				boolean conversationFound = false;
				for(long identifiers : Messaging.getLoadedConversations())
					if(currentConversationID == identifiers) {
						conversationFound = true;
						break;
					}
				
				//Checking if the conversation is still loaded
				if(conversationFound) {
					//Adding the message to the conversation in memory
					messageInfo.getConversationInfo().addGhostMessage(context, messageInfo);
					
					//Getting the activity
					Messaging activity = activityReference.get();
					if(activity != null) {
						//Scrolling to the bottom of the chat
						activity.messageListAdapter.scrollToBottom();
					}
				}
			}
			
			//Sending the message
			messageInfo.sendMessage(context);
		}
	}
	
	void setCurrentScreenEffect(String screenEffect) {
		currentScreenEffect = screenEffect;
	}
	
	private static final long confettiDuration = 1000;
	void playCurrentSendEffect() {
		//Returning if an effect is already playing
		if(currentScreenEffectPlaying) return;
		currentScreenEffectPlaying = true;
		
		switch(currentScreenEffect) {
			case Constants.appleSendStyleFireworks:
				currentScreenEffectPlaying = false;
				break;
			case Constants.appleSendStyleConfetti: {
				//Activating the Konfetti view
				KonfettiView konfettiView = findViewById(R.id.konfetti);
				konfettiView.build()
						.addColors(
								0xFCE18A, //Yellow
								0xFF726D, //Orange
								0xB48DEF, //Purple
								0xF4306D, //Pink
								0x42A5F5, //Blue
								0x7986CB //Indigo
						)
						.setDirection(0D, 359D)
						.setSpeed(4F, 8F)
						.setFadeOutEnabled(true)
						.setTimeToLive(5000L)
						.addShapes(Shape.RECT, Shape.CIRCLE)
						.addSizes(new Size(12, 5), new Size(16, 6))
						.setPosition(-50F, konfettiView.getWidth() + 50F, -50F, -50F)
						.stream(300, confettiDuration);
				
				//Setting the timer to mark the effect as finished
				new Handler().postDelayed(() -> currentScreenEffectPlaying = false, confettiDuration);
				
				break;
			}
			default:
				currentScreenEffectPlaying = false;
				break;
		}
	}
	
	public RetainedFragment.AudioMessageManager getAudioMessageManager() {
		return retainedFragment.audioMessageManager;
	}
	
	private enum InputState {
		MESSAGE,
		CONTENT,
		RECORDING
	}
	
	/* private class ListAdapter extends ArrayAdapter<ConversationManager.ConversationItem> {
		//Creating the values
		private final ListView listView;
		
		ListAdapter(Context context, int resource, List<ConversationManager.ConversationItem> items, ListView listView) {
			//Calling the super method
			super(context, resource, items);
			
			//Setting the list view
			this.listView = listView;
		}
		
		@Override
		@NonNull
		public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
			//Getting the view
			View view = convertView;
			
			//Getting the conversation item
			ConversationManager.ConversationItem conversationItem = getItem(position);
			
			//Returning if the message info is invalid
			if(conversationItem == null) return view;
			
			//Getting the view
			view = conversationItem.createView(Messaging.this, convertView, parent);
			
			//Setting the view source
			conversationItem.setViewSource(() -> listView.getChildAt(position - listView.getFirstVisiblePosition()));
			
			//Returning the view
			return view;
		}
	} */
	
	class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the values
		private final ArrayList<ConversationManager.ConversationItem> conversationItems;
		
		RecyclerAdapter(ArrayList<ConversationManager.ConversationItem> items) {
			//Setting the conversation items
			conversationItems = items;
		}
		
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Returning the correct view holder
			switch(viewType) {
				case ConversationManager.MessageInfo.itemType:
					return new ConversationManager.ConversationItem.MessageViewHolder((LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_conversationitem, parent, false));
				case ConversationManager.GroupActionInfo.itemType:
				case ConversationManager.ChatRenameActionInfo.itemType:
				case ConversationManager.ChatCreationMessage.itemType:
					return new ConversationManager.ActionLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_action, parent, false));
				default:
					return null;
			}
		}
		
		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			//Getting the item
			ConversationManager.ConversationItem conversationItem = conversationItems.get(position);
			
			//Creating the view
			conversationItem.bindView(Messaging.this, holder);
			
			//Setting the view source
			LinearLayoutManager layout = (LinearLayoutManager) messageList.getLayoutManager();
			conversationItem.setViewSource(() -> layout.findViewByPosition(conversationItems.indexOf(conversationItem)));
		}
		
		@Override
		public int getItemCount() {
			return conversationItems.size();
		}
		
		@Override
		public int getItemViewType(int position) {
			return conversationItems.get(position).getItemType();
		}
		
		void scrollNotifyItemInserted(int position) {
			//Returning if the list is empty
			if(conversationItems.isEmpty()) return;
			
			//Getting if the list is currently scrolled to the bottom
			boolean scrolledToBottom = ((LinearLayoutManager) messageList.getLayoutManager()).findLastCompletelyVisibleItemPosition() == getItemCount() - 2; //-2 because the item was already added to the list
			
			//Calling the original method
			notifyItemInserted(position);
			
			//Checking if the list is scrolled to the end and the new item is at the bottom
			if(scrolledToBottom && position == getItemCount() - 1) {
				//Scrolling to the bottom
				messageList.smoothScrollToPosition(getItemCount() - 1);
			}
		}
		
		boolean isScrolledToBottom() {
			return ((LinearLayoutManager) messageList.getLayoutManager()).findLastCompletelyVisibleItemPosition() == getItemCount() - 1;
		}
		
		void scrollToBottom() {
			//Returning if the list has already been scrolled to the bottom
			if(isScrolledToBottom()) return;
			
			//Scrolling to the bottom
			messageList.smoothScrollToPosition(getItemCount() - 1);
		}
	}
	
	public static class SpeedyLinearLayoutManager extends LinearLayoutManager {
		public SpeedyLinearLayoutManager(Context context) {
			super(context);
		}
		
		public SpeedyLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
			super(context, orientation, reverseLayout);
		}
		
		public SpeedyLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
			super(context, attrs, defStyleAttr, defStyleRes);
		}
		
		@Override
		public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
			//Calculating the speed
			float millisPerInch = 50 - Math.min(Math.abs(findFirstVisibleItemPosition() - position), Math.abs(findLastVisibleItemPosition() - position));
			if(millisPerInch < 0.01F) {
				scrollToPosition(position);
				return;
			}
			
			LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
				@Override
				public PointF computeScrollVectorForPosition(int targetPosition) {
					return SpeedyLinearLayoutManager.this.computeScrollVectorForPosition(targetPosition);
				}
				
				@Override
				protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
					return millisPerInch / displayMetrics.densityDpi;
				}
			};
			
			linearSmoothScroller.setTargetPosition(position);
			startSmoothScroll(linearSmoothScroller);
		}
	}
	
	public static class RetainedFragment extends Fragment {
		//Creating the task values
		private Messaging parentActivity;
		
		//Creating the general state values
		InputState inputState = InputState.MESSAGE;
		boolean restartingFromConfigChange = false;
		private static final byte messagesStateUnloaded = 0;
		private static final byte messagesStateLoading = 1;
		private static final byte messagesStateLoaded = 2;
		private static final byte messagesStateFailed = 3;
		byte messagesState = messagesStateUnloaded;
		
		private long conversationID;
		private ArrayList<ConversationManager.ConversationItem> conversationItemList = new ArrayList<>();
		private ArrayList<ConversationManager.MessageInfo> conversationGhostList = new ArrayList<>();
		
		//Creating the audio state values
		int recordingDuration = 0;
		MediaRecorder mediaRecorder = null;
		File mediaRecorderFile = null;
		AudioMessageManager audioMessageManager = new AudioMessageManager();
		final Handler recordingTimerHandler = new Handler(Looper.getMainLooper());
		final Runnable recordingTimerHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				//Adding a second
				recordingDuration++;
				
				//Updating the time
				if(parentActivity != null) parentActivity.onAudioRecordingTimeUpdate(Constants.getFormattedDuration(recordingDuration));
				
				//Running again
				recordingTimerHandler.postDelayed(this, 1000);
			}
		};
		
		/**
		 * Hold a reference to the parent Activity so we can report the
		 * task's current progress and results. The Android framework
		 * will pass us a reference to the newly created Activity after
		 * each configuration change.
		 */
		@Override
		public void onAttach(Context context) {
			//Calling the super method
			super.onAttach(context);
			
			//Getting the parent activity
			parentActivity = (Messaging) context;
		}
		
		/**
		 * This method will only be called once when the retained
		 * Fragment is first created.
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			//Calling the super method
			super.onCreate(savedInstanceState);
			
			//Retain this fragment across configuration changes
			setRetainInstance(true);
		}
		
		/**
		 * Set the callback to null so we don't accidentally leak the
		 * Activity instance.
		 */
		@Override
		public void onDetach() {
			super.onDetach();
			parentActivity = null;
		}
		
		void loadConversation(Context context) {
			//Starting the task
			new LoadConversationTask(context, this, conversationID).execute();
		}
		
		void loadMessages(ConversationManager.ConversationInfo conversationInfo) {
			//Starting the task
			new LoadMessagesTask(this, conversationInfo).execute();
		}
		
		private static class LoadConversationTask extends AsyncTask<Void, Void, ConversationManager.ConversationInfo> {
			//Creating the request values
			private final WeakReference<Context> contextReference;
			private final WeakReference<RetainedFragment> superclassReference;
			private final long identifier;
			
			LoadConversationTask(Context context, RetainedFragment superclass, long identifier) {
				//Setting the request values
				contextReference = new WeakReference<>(context);
				superclassReference = new WeakReference<>(superclass);
				this.identifier = identifier;
			}
			
			@Override
			protected ConversationManager.ConversationInfo doInBackground(Void... voids) {
				//Getting the context
				Context context = contextReference.get();
				if(context == null) return null;
				
				//Returning the conversation
				return DatabaseManager.fetchConversationInfo(context, DatabaseManager.getReadableDatabase(context), identifier);
			}
			
			@Override
			protected void onPostExecute(ConversationManager.ConversationInfo conversationInfo) {
				//Getting the superclass
				RetainedFragment superclass = superclassReference.get();
				if(superclass == null) return;
				
				//Applying the conversation
				superclass.parentActivity.applyConversation(conversationInfo);
			}
		}
		
		private static class LoadMessagesTask extends AsyncTask<Void, Void, ArrayList<ConversationManager.ConversationItem>> {
			//Creating the values
			private final WeakReference<RetainedFragment> fragmentReference;
			
			private final ConversationManager.ConversationInfo conversationInfo;
			
			LoadMessagesTask(RetainedFragment retainedFragment, ConversationManager.ConversationInfo conversationInfo) {
				//Setting the references
				fragmentReference = new WeakReference<>(retainedFragment);
				
				//Setting the values
				this.conversationInfo = conversationInfo;
			}
			
			@Override
			protected void onPreExecute() {
				//Setting the messages state
				fragmentReference.get().messagesState = messagesStateLoading;
			}
			
			@Override
			protected ArrayList<ConversationManager.ConversationItem> doInBackground(Void... params) {
				//Getting the fragment
				RetainedFragment retainedFragment = fragmentReference.get();
				if(retainedFragment == null) return null;
				
				//Loading the conversation items
				ArrayList<ConversationManager.ConversationItem> conversationItems = DatabaseManager.loadConversationItems(DatabaseManager.getReadableDatabase(retainedFragment.getContext()), conversationInfo);
				
				//Setting up the conversation item relations
				ConversationManager.setupConversationItemRelations(conversationItems, conversationInfo);
				
				//Returning the conversation items
				return conversationItems;
			}
			
			@Override
			protected void onPostExecute(ArrayList<ConversationManager.ConversationItem> messages) {
				//Returning if the conversation isn't loaded anymore
				if(!ConversationManager.getLoadedConversations().contains(conversationInfo)) return;
				
				//Getting the fragment
				RetainedFragment retainedFragment = fragmentReference.get();
				if(retainedFragment == null || retainedFragment.getContext() == null) return;
				
				//Checking if the messages are invalid
				if(messages == null) {
					//Setting the state
					retainedFragment.messagesState = messagesStateFailed;
					
					//Calling the failed method
					//retainedFragment.parentActivity.onMessagesFailed();
					
					//Returning
					return;
				}
				
				//Setting the state
				retainedFragment.messagesState = messagesStateLoaded;
				
				//Recording the lists in the conversation info
				conversationInfo.setConversationItems(retainedFragment.conversationItemList, retainedFragment.conversationGhostList);
				
				//Replacing the conversation items
				conversationInfo.replaceConversationItems(retainedFragment.getContext(), messages);
				
				//Telling the callbacks
				retainedFragment.parentActivity.onMessagesLoaded();
			}
		}
		
		static class AudioMessageManager {
			//Creating the values
			private MediaPlayer mediaPlayer = new MediaPlayer();
			private String currentFilePath = "";
			private WeakReference<ConversationManager.AudioAttachmentInfo> attachmentReference;
			private Handler mediaPlayerHandler = new Handler(Looper.getMainLooper());
			private Runnable mediaPlayerHandlerRunnable = new Runnable() {
				@Override
				public void run() {
					//Updating the attachment
					ConversationManager.AudioAttachmentInfo attachment = getAttachmentInfo();
					if(attachment != null) attachment.setMediaProgress(mediaPlayer.getCurrentPosition());
					
					//Running again
					mediaPlayerHandler.postDelayed(this, 10);
				}
			};
			
			AudioMessageManager() {
				//Setting the media player listeners
				mediaPlayer.setOnPreparedListener(player -> {
					//Playing the media
					player.start();
					
					//Starting the timer
					mediaPlayerHandlerRunnable.run();
					
					//Updating the attachment
					ConversationManager.AudioAttachmentInfo attachment = getAttachmentInfo();
					if(attachment != null) attachment.setMediaPlaying(true);
				});
				mediaPlayer.setOnCompletionListener(player -> {
					//Cancelling the timer
					mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
					
					//Updating the attachment
					ConversationManager.AudioAttachmentInfo attachment = getAttachmentInfo();
					if(attachment != null) attachment.resetPlaying();
				});
			}
			
			private ConversationManager.AudioAttachmentInfo getAttachmentInfo() {
				if(attachmentReference == null) return null;
				return attachmentReference.get();
			}
			
			void release() {
				//Cancelling the timer
				if(mediaPlayer.isPlaying()) mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
				
				//Releasing the media player
				mediaPlayer.release();
			}
			
			void prepareMediaPlayer(long messageID, File file, ConversationManager.AudioAttachmentInfo attachmentInfo) {
				//Returning if the attachment is already playing
				if(currentFilePath.equals(file.getPath())) return;
				
				//Cancelling the timer
				if(mediaPlayer.isPlaying()) mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
				
				//Resetting the media player
				mediaPlayer.reset();
				
				//Resetting the old view
				{
					ConversationManager.AudioAttachmentInfo oldAttachmentInfo = getAttachmentInfo();
					if(oldAttachmentInfo != null) oldAttachmentInfo.resetPlaying();
				}
				
				try {
					//Creating the media player
					mediaPlayer.setDataSource(file.getPath());
				} catch(IOException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Returning
					return;
				}
				
				//Setting the attachment reference
				attachmentReference = new WeakReference<>(attachmentInfo);
				
				//Setting the values
				currentFilePath = file.getPath();
				
				//Preparing the media player
				mediaPlayer.prepareAsync();
			}
			
			boolean isCurrentMessage(File file) {
				return currentFilePath.equals(file.getPath());
			}
			
			void togglePlaying() {
				//Checking if the media player is playing
				if(mediaPlayer.isPlaying()) {
					//Pausing the media player
					mediaPlayer.pause();
					
					//Cancelling the playback timer
					mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
				} else {
					//Playing the media player
					mediaPlayer.start();
					
					//Starting the playback timer
					mediaPlayerHandlerRunnable.run();
				}
				
				//Telling the attachment
				ConversationManager.AudioAttachmentInfo attachment = getAttachmentInfo();
				if(attachment != null) attachment.setMediaPlaying(mediaPlayer.isPlaying());
			}
		}
		
		private static String getFormattedDuration(int seconds) {
			//Getting the values
			int minutes = seconds / 60;
			seconds %= 60;
			int hours = minutes / 60;
			minutes %= 60;
			
			//Getting the values as string
			String hourString = Integer.toString(hours);
			String minuteString = Integer.toString(minutes);
			String secondString = Integer.toString(seconds);
			
			//Adding an extra 0 if the number is only 1 digit
			if(minuteString.length() <= 1) minuteString = "0" + minuteString;
			if(secondString.length() <= 1) secondString = "0" + secondString;
			
			//Checking if the duration is more than an hour
			if(hours >= 1f) {
				//Returning the time with hours
				return hourString + ":" + minuteString + ":" + secondString;
			} else {
				//Returning the time without hours
				return minuteString + ":" + secondString;
			}
		}
	}
	
	private static class UpdateLastViewTimeTask extends AsyncTask<Void, Void, Void> {
		private final WeakReference<Context> contextReference;
		private final long conversationID;
		private final long time;
		
		UpdateLastViewTimeTask(Context context, long conversationID, long time) {
			contextReference = new WeakReference<>(context);
			
			this.conversationID = conversationID;
			this.time = time;
		}
		
		@Override
		protected Void doInBackground(Void... voids) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Updating the time
			DatabaseManager.setConversationLastViewTime(DatabaseManager.getWritableDatabase(context), conversationID, time);
			
			//Returning
			return null;
		}
	}
	
	static class EffectCallbacks {
		WeakReference<Messaging> activityReference;
		
		EffectCallbacks(Messaging activity) {
			activityReference = new WeakReference<>(activity);
		}
		
		void setCurrentScreenEffect(String screenEffect) {
			Messaging activity = activityReference.get();
			if(activity != null) activity.setCurrentScreenEffect(screenEffect);
		}
		
		void playCurrentScreenEffect() {
			Messaging activity = activityReference.get();
			if(activity != null) activity.playCurrentSendEffect();
		}
	}
	
	private static class AddGhostMessageTask extends AsyncTask<Void, Void, Void> {
		private final WeakReference<Context> contextReference;
		
		private final ConversationManager.MessageInfo messageInfo;
		
		AddGhostMessageTask(Context context, ConversationManager.MessageInfo messageInfo) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			
			//Setting the other values
			this.messageInfo = messageInfo;
		}
		
		@Override
		protected Void doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Adding the item to the database
			DatabaseManager.addConversationItem(DatabaseManager.getWritableDatabase(context), messageInfo);
			
			//Returning
			return null;
		}
	}
	
	private static class AdapterUpdater extends ConversationManager.ConversationInfo.AdapterUpdater {
		//Creating the references
		private final WeakReference<Messaging> activityReference;
		
		AdapterUpdater(Messaging activity) {
			//Setting the references
			activityReference = new WeakReference<>(activity);
		}
		
		@Override
		public void updateFully() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageList.getRecycledViewPool().clear();
			activity.messageListAdapter.notifyDataSetChanged();
		}
		
		@Override
		public void updateScroll(int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.scrollNotifyItemInserted(index);
		}
		
		@Override
		public void updateMove(int from, int to) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.notifyItemChanged(from);
			activity.messageListAdapter.notifyItemMoved(from, to);
		}
	}
}