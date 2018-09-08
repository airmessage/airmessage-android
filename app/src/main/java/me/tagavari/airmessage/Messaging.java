package me.tagavari.airmessage;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
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
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Pools;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import java9.util.function.Consumer;
import me.tagavari.airmessage.common.SharedValues;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.view.AppleEffectView;
import me.tagavari.airmessage.view.VisualizerView;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class Messaging extends AppCompatCompositeActivity {
	//Creating the reference values
	private static final int quickScrollFABThreshold = 3;
	private static final float bottomSheetFillThreshold = 0.8F;
	private static final float contentPanelFillThreshold = 0.5F;
	static final int messageChunkSize = 50;
	static final int progressiveLoadThreshold = 10;
	private static final String[] documentMimeTypes = {"text/*", "application/*", "font/*"};
	private static final int draftCountLimit = 10;
	
	private static final long confettiDuration = 1000;
	//private static final float disabledAlpha = 0.38F
	
	private static final int permissionRequestStorage = 0;
	private static final int permissionRequestAudio = 1;
	private static final int permissionRequestAudioDirect = 2; //Used when requesting microphone usage directly form the input bar
	
	private static final int intentPickFile = 1;
	private static final int intentTakePicture = 2;
	
	//Creating the static values
	private static final List<WeakReference<Messaging>> foregroundConversations = new ArrayList<>();
	//private static final List<WeakReference<Messaging>> loadedConversations = new ArrayList<>();
	
	//Creating the view model and plugin values
	private ActivityViewModel viewModel;
	private PluginMessageBar pluginMessageBar;
	
	//Creating the info bar values
	private PluginMessageBar.InfoBar infoBarConnection;
	
	private boolean currentScreenEffectPlaying = false;
	
	//Creating the listener values
	private final BroadcastReceiver clientConnectionResultBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(Constants.intentParamState, -1);
			if(state == ConnectionService.stateDisconnected) {
				int code = intent.getIntExtra(Constants.intentParamCode, -1);
				showServerWarning(code);
			} else hideServerWarning();
		}
	};
	private final RecyclerView.OnScrollListener messageListScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			//Getting the layout manager
			LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			int itemsScrolledFromBottom = linearLayoutManager.getItemCount() - 1 - linearLayoutManager.findLastVisibleItemPosition();
			
			//Showing the FAB if the user has scrolled more than the threshold items
			if(itemsScrolledFromBottom > quickScrollFABThreshold) setFABVisibility(true);
			else setFABVisibility(false);
			
			//Marking viewed items as read
			if(itemsScrolledFromBottom < viewModel.conversationInfo.getUnreadMessageCount()) viewModel.conversationInfo.setUnreadMessageCount(itemsScrolledFromBottom);
			
			//Loading chunks if the user is scrolled to the top
			if(linearLayoutManager.findFirstVisibleItemPosition() < progressiveLoadThreshold && !viewModel.isProgressiveLoadInProgress() && !viewModel.progressiveLoadReachedLimit) recyclerView.post(viewModel::loadNextChunk);
		}
	};
	private final View.OnTouchListener recordingTouchListener = (view, motionEvent) -> {
		//Performing the click
		view.performClick();
		
		//Checking if the input state is content and the action is a down touch
		if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
			//Attempting to start recording
			startRecording(motionEvent.getX(), motionEvent.getY());
			
			//Returning true
			return true;
		}
		
		//Returning false
		return false;
	};
	private final ActivityViewModel.AttachmentsLoadCallbacks[] attachmentsLoadCallbacks = new ActivityViewModel.AttachmentsLoadCallbacks[2];
	
	//Creating the view values
	private View rootView;
	private AppBarLayout appBar;
	private TextView labelLoading;
	private ViewGroup groupLoadFail;
	private TextView labelLoadFail;
	private RecyclerView messageList;
	private View inputBar;
	private ImageButton buttonSendMessage;
	private ImageButton buttonAddContent;
	private InsertionEditText messageInputField;
	private ViewGroup recordingActiveGroup;
	private TextView recordingTimeLabel;
	private VisualizerView recordingVisualizer;
	private FloatingActionButton bottomFAB;
	private TextView bottomFABBadge;
	private AppleEffectView appleEffectView;
	private final HashMap<ConversationManager.MemberInfo, View> memberListViews = new HashMap<>();
	
	private View detailScrim;
	
	private RecyclerView listAttachmentQueue;
	
	//Creating the other values
	private final MessageListRecyclerAdapter messageListAdapter = new MessageListRecyclerAdapter();
	private ActivityManager.TaskDescription lastTaskDescription;
	
	private boolean currentSendButtonState = true;
	private boolean toolbarVisible = true;
	
	private DialogFragment currentColorPickerDialog = null;
	private ConversationManager.MemberInfo currentColorPickerDialogMember = null;
	
	//Creating the listeners
	private final ViewTreeObserver.OnGlobalLayoutListener rootLayoutListener = () -> {
		//Getting the height
		int height = messageList.getHeight();
		if(appBar.getVisibility() == View.VISIBLE) height += appBar.getHeight();
		
		//Checking if the window is smaller than the minimum height, the window isn't in multi-window mode
		if(height < getResources().getDimensionPixelSize(R.dimen.conversationwindow_minheight) && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())) {
			//Hiding the app bar
			hideToolbar();
		} else {
			//Showing the app bar
			showToolbar();
		}
	};
	private final TextWatcher inputFieldTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
		}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Getting if the message box has text
			//messageBoxHasText = stringHasChar(s.toString());
			
			//Updating the send button
			updateSendButton(false);
		}
		
		@Override
		public void afterTextChanged(Editable s) {
		}
	};
	private final View.OnClickListener sendButtonClickListener = view -> {
		//Creating the message list
		ArrayList<ConversationManager.MessageInfo> messageList = new ArrayList<>();
		
		//Checking if the message box has text
		if(messageInputField.getText().length() > 0) {
			//Getting the message text
			String message = messageInputField.getText().toString();
			
			//Trimming the message
			message = message.trim();
			
			//Returning if the message is empty
			if(message.isEmpty()) return;
			
			//Creating a message
			messageList.add(new ConversationManager.MessageInfo(-1, -1, null, viewModel.conversationInfo, null, message, null, false, System.currentTimeMillis(), SharedValues.MessageInfo.stateCodeGhost, Constants.messageErrorCodeOK, -1));
			
			//Clearing the message box
			messageInputField.setText("");
			messageInputField.requestLayout(); //Height of input field doesn't update otherwise
			//messageBoxHasText = false;
		}
		
		//Iterating over the drafts
		for(QueuedFileInfo queuedFile : new ArrayList<>(viewModel.draftQueueList)) {
			//Creating the message
			ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(-1, -1, null, viewModel.conversationInfo, null, null, null, false, System.currentTimeMillis(), SharedValues.MessageInfo.stateCodeGhost, Constants.messageErrorCodeOK, -1);
			
			//Creating the attachment
			SimpleAttachmentInfo attachmentFile = queuedFile.getItem();
			ConversationManager.AttachmentInfo attachment = ConversationManager.createAttachmentInfoFromType(-1, null, messageInfo, attachmentFile.getFileName(), attachmentFile.getFileType(), attachmentFile.getFileSize());
			attachment.setDraftingPushRequest(queuedFile.getFilePushRequest());
			
			//Adding the attachment to the message
			messageInfo.addAttachment(attachment);
			
			//Adding the message to the queue list
			messageList.add(messageInfo);
			
			//Dequeuing the item
			dequeueAttachment(queuedFile.getItem(), true, false);
			viewModel.conversationInfo.removeDraftFileUpdate(this, queuedFile.getDraftFile(), -1);
		}
		
		//Returning if there are no items to send
		if(messageList.isEmpty()) return;
		
		//Clearing the conversation's drafts
		viewModel.conversationInfo.clearDraftsUpdate(this);
		
		//Writing the messages to the database
		new AddGhostMessageTask(getApplicationContext(), new GhostMessageFinishHandler()).execute(messageList.toArray(new ConversationManager.MessageInfo[0]));
		
		//Scrolling to the bottom of the chat
		messageListAdapter.scrollToBottom();
	};
	private static class GhostMessageFinishHandler implements Consumer<ConversationManager.MessageInfo> {
		@Override
		public void accept(ConversationManager.MessageInfo messageInfo) {
			//Adding the message to the conversation in memory
			messageInfo.getConversationInfo().addGhostMessage(MainApplication.getInstance(), messageInfo);
			
			//Sending the message
			messageInfo.sendMessage(MainApplication.getInstance());
		}
	}
	/* private final View.OnTouchListener recordingTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			//Performing the click
			view.performClick();
			
			//Checking if the input state is content and the action is a down touch
			if(viewModel.inputState == ActivityViewModel.inputStateContent && motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				//Attempting to start recording
				boolean result = startRecording();
				
				//Moving the recording indicator if the recording could be started
				if(result) recordingIndicator.setX(motionEvent.getRawX() - (float) recordingIndicator.getWidth() / 2f);
				
				//Returning true
				return true;
			}
			//Returning false
			return false;
		}
	}; */
	private final Observer<Byte> messagesStateObserver = state -> {
		switch(state) {
			case ActivityViewModel.messagesStateLoadingConversation:
				labelLoading.setVisibility(View.VISIBLE);
				groupLoadFail.setVisibility(View.GONE);
				
				setMessageBarState(false);
				
				break;
			case ActivityViewModel.messagesStateLoadingMessages:
				labelLoading.setVisibility(View.VISIBLE);
				groupLoadFail.setVisibility(View.GONE);
				
				setMessageBarState(false);
				
				//Setting the conversation title
				viewModel.conversationInfo.buildTitle(Messaging.this, (result, wasTasked) -> {
					getSupportActionBar().setTitle(result);
					setTaskDescription(lastTaskDescription = new ActivityManager.TaskDescription(result, BitmapFactory.decodeResource(getResources(), R.mipmap.app_icon), viewModel.conversationInfo.getConversationColor()));
				});
				
				//Coloring the UI
				colorUI(findViewById(android.R.id.content));
				
				//Setting the activity callbacks
				viewModel.conversationInfo.setActivityCallbacks(new ActivityCallbacks(this));
				
				//Setting the message input field hint
				messageInputField.setHint(getInputBarMessage());
				
				//Updating the send button
				//updateSendButton();
				
				break;
			case ActivityViewModel.messagesStateReady:
				labelLoading.setVisibility(View.GONE);
				groupLoadFail.setVisibility(View.GONE);
				
				setMessageBarState(true);
				
				//Setting the conversation title
				viewModel.conversationInfo.buildTitle(Messaging.this, (result, wasTasked) -> {
					getSupportActionBar().setTitle(result);
					setTaskDescription(lastTaskDescription = new ActivityManager.TaskDescription(result, BitmapFactory.decodeResource(getResources(), R.mipmap.app_icon), viewModel.conversationInfo.getConversationColor()));
				});
				
				//Setting the activity callbacks
				viewModel.conversationInfo.setActivityCallbacks(new ActivityCallbacks(this));
				
				//Setting the list adapter
				messageListAdapter.setItemList(viewModel.conversationItemList);
				messageList.setAdapter(messageListAdapter);
				messageList.addOnScrollListener(messageListScrollListener);
				
				//Setting the message input field hint
				messageInputField.setHint(getInputBarMessage());
				
				//Checking if there are drafts files saved in the conversation
				if(!viewModel.conversationInfo.getDrafts().isEmpty()) {
					//Copying the drafts to the activity
					if(viewModel.draftQueueList.isEmpty()) for(ConversationManager.DraftFile draft : viewModel.conversationInfo.getDrafts()) {
						//Creating the queued item
						QueuedFileInfo queuedItem = new QueuedFileInfo(draft);
						
						//Searching for the item's push request
						ConnectionService.FilePushRequest currentRequest = null;
						ConnectionService service = ConnectionService.getInstance();
						if(service != null) {
							ConnectionService.FileProcessingRequest processingRequest = service.searchFileProcessingQueue(draft.getLocalID());
							if(processingRequest instanceof ConnectionService.FilePushRequest) currentRequest = (ConnectionService.FilePushRequest) processingRequest;
						}
						
						//Creating a new request if there was no current request in the queue
						if(currentRequest == null) currentRequest = new ConnectionService.FilePushRequest(draft.getFile(), draft.getFileType(), draft.getFileName(), draft.getModificationDate(), viewModel.conversationInfo, -1, draft.getLocalID(), ConnectionService.FilePushRequest.stateQueued, 0, false);
						
						//Assigning the request to the queued item
						queuedItem.setFilePushRequest(currentRequest);
						
						//Adding the queued item
						viewModel.draftQueueList.add(queuedItem);
					}
					
					//Showing the draft bar
					listAttachmentQueue.setVisibility(View.VISIBLE);
					
					//Updating the send button
					updateSendButton(false);
				}
				
				//Restoring the draft message
				if(messageInputField.getText().length() == 0) messageInputField.setText(viewModel.conversationInfo.getDraftMessage());
				
				/* //Finding the latest send effect
				for(int i = viewModel.conversationItemList.size() - 1; i >= 0 && i >= viewModel.conversationItemList.size() - viewModel.conversationInfo.getUnreadMessageCount(); i--) {
					//Getting the conversation item
					ConversationManager.ConversationItem conversationItem = viewModel.conversationItemList.get(i);
					
					//Skipping the remainder of the iteration if the item is not a message
					if(!(conversationItem instanceof ConversationManager.MessageInfo)) continue;
					
					//Getting the message
					ConversationManager.MessageInfo messageInfo = (ConversationManager.MessageInfo) conversationItem;
					
					//Skipping the remainder of the iteration if the message has no send effect
					if(messageInfo.getSendStyle().isEmpty()) continue;
					
					//Setting the send effect
					currentScreenEffect = messageInfo.getSendStyle();
					
					//Breaking from the loop
					break;
				}
				
				//Playing the send effect if there is one
				if(!currentScreenEffect.isEmpty()) playCurrentScreenEffect(); */
				
				//Setting the last message count
				viewModel.lastUnreadCount = viewModel.conversationInfo.getUnreadMessageCount();
				
				{
					//Getting the layout manager
					LinearLayoutManager linearLayoutManager = (LinearLayoutManager) messageList.getLayoutManager();
					int itemsScrolledFromBottom = linearLayoutManager.getItemCount() - linearLayoutManager.findLastVisibleItemPosition();
					
					//Showing the FAB if the user has scrolled more than 3 items
					if(itemsScrolledFromBottom > quickScrollFABThreshold) {
						bottomFAB.show();
						restoreUnreadIndicator();
					}
				}
				
				break;
			case ActivityViewModel.messagesStateFailedConversation:
				labelLoading.setVisibility(View.GONE);
				groupLoadFail.setVisibility(View.VISIBLE);
				labelLoadFail.setText(R.string.message_loaderror_conversation);
				
				setMessageBarState(false);
				
				break;
			case ActivityViewModel.messagesStateFailedMessages:
				labelLoading.setVisibility(View.GONE);
				groupLoadFail.setVisibility(View.VISIBLE);
				labelLoadFail.setText(R.string.message_loaderror_messages);
				
				setMessageBarState(false);
				
				break;
		}
	};
	
	public Messaging() {
		//Setting the plugins;
		addPlugin(pluginMessageBar = new PluginMessageBar());
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_messaging);
		
		//Setting the action bar
		setSupportActionBar(findViewById(R.id.toolbar));
		
		//Getting the views
		rootView = findViewById(android.R.id.content);
		appBar = findViewById(R.id.appbar);
		
		labelLoading = findViewById(R.id.loading_text);
		groupLoadFail = findViewById(R.id.group_error);
		labelLoadFail = findViewById(R.id.group_error_label);
		messageList = findViewById(R.id.list_messages);
		inputBar = findViewById(R.id.inputbar);
		buttonSendMessage = inputBar.findViewById(R.id.button_send);
		buttonAddContent = inputBar.findViewById(R.id.button_addcontent);
		messageInputField = inputBar.findViewById(R.id.messagebox);
		//recordingTimeLabel = inputBar.findViewById(R.id.recordingtime);
		bottomFAB = findViewById(R.id.fab_bottom);
		bottomFABBadge = findViewById(R.id.fab_bottom_badge);
		appleEffectView = findViewById(R.id.effect_foreground);
		
		detailScrim = findViewById(R.id.scrim);
		
		listAttachmentQueue = findViewById(R.id.inputbar_attachments);
		
		//Setting the plugin views
		pluginMessageBar.setParentView(findViewById(R.id.infobar_container));
		
		//Enforcing the maximum content width
		Constants.enforceContentWidth(getResources(), messageList);
		
		//Getting the conversation ID
		long conversationID = getIntent().getLongExtra(Constants.intentParamTargetID, -1);
		
		//Getting the view model
		viewModel = ViewModelProviders.of(this, new ViewModelProvider.Factory() {
			@NonNull
			@Override
			public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
				return (T) new ActivityViewModel(getApplication(), conversationID);
			}
		}).get(ActivityViewModel.class);
		
		//Restoring the input bar state
		restoreInputBarState();
		
		//Restoring the text states
		//findViewById(R.id.loading_text).setVisibility(viewModel.messagesState == viewModel.messagesStateLoading ? View.VISIBLE : View.GONE);
		
		//Enabling the toolbar's up navigation
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Setting the listeners
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(rootLayoutListener);
		messageInputField.addTextChangedListener(inputFieldTextWatcher);
		messageInputField.setOnClickListener(view -> closeAttachmentsPanel(false));
		buttonSendMessage.setOnClickListener(sendButtonClickListener);
		buttonAddContent.setOnClickListener(view -> {
			if(viewModel.isAttachmentsPanelOpen) closeAttachmentsPanel(true);
			else {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				boolean result = inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
				openAttachmentsPanel(false, !result);
			}
		});
		/* inputBar.findViewById(R.id.button_camera).setOnClickListener(view -> requestTakePicture());
		inputBar.findViewById(R.id.button_gallery).setOnClickListener(view -> requestGalleryFile());
		inputBar.findViewById(R.id.button_attach).setOnClickListener(view -> requestAnyFile());
		contentRecordButton.setOnTouchListener(recordingTouchListener); */
		bottomFAB.setOnClickListener(view -> messageListAdapter.scrollToBottom());
		appleEffectView.setFinishListener(() -> currentScreenEffectPlaying = false);
		detailScrim.setOnClickListener(view -> closeDetailsPanel());
		
		//Configuring the input field
		messageInputField.setContentProcessor((uri, type, name, size) -> queueAttachment(new SimpleAttachmentInfo(uri, type, name, size, -1), findAppropriateTileHelper(type), true));
		
		//Setting up the attachments
		{
			listAttachmentQueue.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
			listAttachmentQueue.setAdapter(new AttachmentsQueueRecyclerAdapter(viewModel.draftQueueList));
		}
		
		//Setting the filler data
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getIntent().hasExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT)) messageInputField.setText(getIntent().getStringExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT)); //Notification inline reply text (only supported on Android P and above)
		else if(getIntent().hasExtra(Constants.intentParamDataText)) messageInputField.setText(getIntent().getStringExtra(Constants.intentParamDataText)); //Shared text from activity
		if(getIntent().hasExtra(Constants.intentParamDataFile)) {
			Parcelable[] targetParcelables = getIntent().getParcelableArrayExtra(Constants.intentParamDataFile);
			Uri[] targetUris = new Uri[targetParcelables.length];
			for(int i = 0; i < targetParcelables.length; i++) targetUris[i] = (Uri) targetParcelables[i];
			new QueueUriAsyncTask(this).execute(targetUris);
		}
		
		/* //Iterating over the loaded conversations
		for(Iterator<WeakReference<Messaging>> iterator = loadedConversations.iterator(); iterator.hasNext(); ) {
			//Getting the referenced activity
			Messaging activity = iterator.next().get();
			
			//Removing the reference if it is invalid
			if(activity == null) {
				iterator.remove();
				continue;
			}
			
			//Checking if the conversation matches this one
			//if(activity.viewModel.conversationID != conversationID) continue;
			
			//Destroying the activity
			activity.finish();
			iterator.remove();
			
			//Breaking from the loop
			break;
		}
		
		//Adding the conversation as a loaded conversation
		loadedConversations.add(new WeakReference<>(this)); */
		
		//Creating the info bars
		infoBarConnection = pluginMessageBar.create(R.drawable.disconnection, null);
		
		//Restoring the panels
		getWindow().getDecorView().post(() -> {
			openDetailsPanel(true);
			openAttachmentsPanel(true, false);
		});
		
		//Updating the send button
		updateSendButton(true);
		
		//Registering the observers
		viewModel.messagesState.observe(this, messagesStateObserver);
		viewModel.isRecording.observe(this, value -> {
			//Returning if the value is recording (already handled, since the activity has to initiate it)
			if(value) return;
			
			//Concealing the recording view
			concealRecordingView();
			
			//Queuing the target file (if it is available)
			if(viewModel.targetFileRecording != null) new QueueFileAsyncTask(this).execute(viewModel.targetFileRecording);
		});
		viewModel.recordingDuration.observe(this, value -> {
			if(recordingTimeLabel != null) recordingTimeLabel.setText(Constants.getFormattedDuration(value));
		});
		viewModel.progressiveLoadInProgress.observe(this, value -> {
			if(value) onProgressiveLoadStart();
			else onProgressiveLoadFinish(viewModel.lastProgressiveLoadCount);
		});
	}
	
	/* void onMessagesFailed() {
		//Hiding the loading text
		findViewById(R.id.loading_text).setVisibility(View.GONE);
		
		//Showing the failed text
		findViewById(R.id.error_text).setVisibility(View.VISIBLE);
	} */
	
	@Override
	public void onStart() {
		//Calling the super method
		super.onStart();
		
		//Adding the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(clientConnectionResultBroadcastReceiver, new IntentFilter(ConnectionService.localBCStateUpdate));
	}
	
	@Override
	public void onResume() {
		//Calling the super method
		super.onResume();
		
		//Adding the phantom reference
		foregroundConversations.add(new WeakReference<>(this));
		
		//Clearing the notifications
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int) viewModel.conversationID);
		
		//Updating the server warning bar state
		ConnectionService connectionService = ConnectionService.getInstance();
		boolean showWarning = connectionService == null || (connectionService.getCurrentState() == ConnectionService.stateDisconnected && ConnectionService.getLastConnectionResult() != -1);
		if(showWarning) showServerWarning(ConnectionService.getLastConnectionResult());
		else hideServerWarning();
		
		//Notifying the views
		if(viewModel.messagesState.getValue() == ActivityViewModel.messagesStateReady) {
			for(ConversationManager.ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof ConversationManager.MessageInfo) {
					((ConversationManager.MessageInfo) conversationItem).notifyResume();
				}
			}
		}
		
		//Coloring the UI
		colorUI(findViewById(android.R.id.content));
	}
	
	@Override
	public void onPause() {
		//Calling the super method
		super.onPause();
		
		//Iterating over the foreground conversations
		for(Iterator<WeakReference<Messaging>> iterator = foregroundConversations.iterator(); iterator.hasNext(); ) {
			//Getting the referenced activity
			Messaging activity = iterator.next().get();
			
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
		
		//Notifying the views
		if(viewModel.messagesState.getValue() == ActivityViewModel.messagesStateReady) {
			for(ConversationManager.ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof ConversationManager.MessageInfo) {
					((ConversationManager.MessageInfo) conversationItem).notifyPause();
				}
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
		
		//Notifying the views
		if(viewModel.messagesState.getValue() == ActivityViewModel.messagesStateReady) {
			for(ConversationManager.ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof ConversationManager.MessageInfo) {
					((ConversationManager.MessageInfo) conversationItem).notifyPause();
				}
			}
		}
		
		
		//Checking if the activity is finishing
		if(isFinishing()) {
			//Saving the draft message
			viewModel.applyDraftMessage(messageInputField.getText().toString());
		}
	}
	
	@Override
	public void onDestroy() {
		//Calling the super method
		super.onDestroy();
		
		//Iterating over the loaded conversations
		/* for(Iterator<WeakReference<Messaging>> iterator = loadedConversations.iterator(); iterator.hasNext();) {
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
		} */
		
		//Notifying the views
		if(viewModel.messagesState.getValue() == ActivityViewModel.messagesStateReady) {
			for(ConversationManager.ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof ConversationManager.MessageInfo) {
					((ConversationManager.MessageInfo) conversationItem).notifyPause();
				}
			}
		}
	}
	
	long getConversationID() {
		return viewModel.conversationID;
	}
	
	private void restoreInputBarState() {
		/* switch(viewModel.inputState) {
			case ActivityViewModel.inputStateContent:
				//Disabling the message bar
				messageContentButton.setEnabled(false);
				messageInputField.setEnabled(false);
				messageSendButton.setEnabled(false);
				messageBar.getLayoutParams().height = referenceBar.getHeight();
				
				//Showing the content bar
				contentCloseButton.setRotation(45);
				contentBar.setVisibility(View.VISIBLE);
				
				break;
			case ActivityViewModel.inputStateRecording:
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
		//getAudioMessageManager().reconnectAttachment(conversationInfo); */
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		//Checking if the request code is taking a picture
		if(requestCode == intentTakePicture) {
			//Returning if the current input state is not the content bar
			//if(viewModel.inputState != inputStateContent) return;
			
			//Checking if the result was a success
			if(resultCode == RESULT_OK) {
				//Queuing the file
				new QueueFileAsyncTask(this).execute(viewModel.targetFileIntent);
			}
		}
		//Otherwise if the request code is the media picker
		else if(requestCode == intentPickFile) {
			//Returning if the current input state is not the content bar
			//if(viewModel.inputState != ActivityViewModel.inputStateContent) return;
			
			//Checking if the result was a success
			if(resultCode == RESULT_OK) {
				//Getting the content
				if(intent.getData() != null) {
					//Queuing the content
					new QueueUriAsyncTask(this).execute(intent.getData());
				} else if(intent.getClipData() != null) {
					Uri[] list = new Uri[intent.getClipData().getItemCount()];
					for(int i = 0; i < intent.getClipData().getItemCount(); i++) list[i] = intent.getClipData().getItemAt(i).getUri();
					new QueueUriAsyncTask(this).execute(list);
				}
			}
		}
	}
	
	private static class QueueFileAsyncTask extends AsyncTask<File, Void, ArrayList<SimpleAttachmentInfo>> {
		private final WeakReference<Messaging> activityReference;
		
		QueueFileAsyncTask(Messaging activity) {
			activityReference = new WeakReference<>(activity);
		}
		
		@Override
		protected ArrayList<SimpleAttachmentInfo> doInBackground(File... files) {
			//Creating the list
			ArrayList<SimpleAttachmentInfo> list = new ArrayList<>();
			
			//Getting the context
			Context context = activityReference.get();
			if(context == null) return null;
			
			//Adding the files
			for(File file : files) list.add(new SimpleAttachmentInfo(file, Constants.getMimeType(file), file.getName(), file.length(), -1));
			
			//Returning
			return list;
		}
		
		@Override
		protected void onPostExecute(ArrayList<SimpleAttachmentInfo> results) {
			//Returning if there are no results
			if(results == null || results.isEmpty()) return;
			
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Queuing the files
			for(SimpleAttachmentInfo attachmentInfo : results) activity.queueAttachment(attachmentInfo, activity.findAppropriateTileHelper(attachmentInfo.getFileType()), true);
		}
	}
	
	private static class QueueUriAsyncTask extends AsyncTask<Uri, Void, ArrayList<SimpleAttachmentInfo>> {
		private final WeakReference<Messaging> activityReference;
		
		QueueUriAsyncTask(Messaging activity) {
			activityReference = new WeakReference<>(activity);
		}
		
		@Override
		protected ArrayList<SimpleAttachmentInfo> doInBackground(Uri... uris) {
			//Creating the list
			ArrayList<SimpleAttachmentInfo> list = new ArrayList<>();
			
			//Getting the context
			Context context = activityReference.get();
			if(context == null) return null;
			
			//Adding the files
			for(Uri uri : uris) {
				list.add(new SimpleAttachmentInfo(uri, Constants.getMimeType(context, uri), Constants.getUriName(context, uri), Constants.getUriSize(context, uri), -1));
			}
			
			//Returning
			return list;
		}
		
		@Override
		protected void onPostExecute(ArrayList<SimpleAttachmentInfo> results) {
			//Returning if there are no results
			if(results == null || results.isEmpty()) return;
			
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Queuing the files
			for(SimpleAttachmentInfo attachmentInfo : results) activity.queueAttachment(attachmentInfo, activity.findAppropriateTileHelper(attachmentInfo.getFileType()), true);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Inflating the menu resource
		getMenuInflater().inflate(R.menu.menu_messaging, menu);
		
		//Returning true
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			/* case android.R.id.home:
				//Closing the details panel if it is open
				if(viewModel.isDetailsPanelOpen) closeDetailsPanel();
				//Otherwise finishing the activity
				else finish();
				
				return true; */
			case R.id.action_details:
				//Checking if the conversation is valid
				if(viewModel.conversationInfo != null) {
					//Launching the details activity
					//startActivity(new Intent(this, MessagingInfo.class).putExtra(Constants.intentParamTargetID, viewModel.conversationInfo.getLocalID()));
					
					//Opening the details panel
					openDetailsPanel(false);
				}
				
				return true;
		}
		
		//Returning false
		return false;
	}
	
	@Override
	public void onBackPressed() {
		//Closing the details panel if it is open
		if(viewModel.isDetailsPanelOpen) closeDetailsPanel();
		//Closing the attachments panel if it is open
		else if(viewModel.isAttachmentsPanelOpen) closeAttachmentsPanel(true);
		//Otherwise passing the event to the superclass
		else super.onBackPressed();
	}
	
	private void openAttachmentsPanel(boolean restore, boolean animate) {
		//Returning if the conversation is not ready or the panel is already open
		if(viewModel.messagesState.getValue() != ActivityViewModel.messagesStateReady || restore != viewModel.isAttachmentsPanelOpen) return;
		
		//Setting the panel as open
		viewModel.isAttachmentsPanelOpen = true;
		
		//Inflating the view
		ViewStub viewStub = findViewById(R.id.viewstub_attachments);
		if(viewStub != null) {
			//Inflating the view stub
			ViewGroup inflated = (ViewGroup) viewStub.inflate();
			
			/* LinearLayout.LayoutParams inflatedParams = (LinearLayout.LayoutParams) inflated.getLayoutParams();
			inflatedParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
			inflatedParams.height = getResources().getDimensionPixelSize(R.dimen.contentpanel_height);
			inflated.setLayoutParams(inflatedParams); */
			
			//Coloring the UI
			colorUI(inflated);
			
			//Setting up the sections
			setupAttachmentsGallerySection();
			setupAttachmentsAudioSection();
			setupAttachmentsDocumentsSection();
			
			//Setting the listeners
			//inflated.findViewById(R.id.pane_attachments).setOnTouchListener(attachmentListTouchListener);
			
			//Checking if the request is a restore
			if(restore) {
				//Setting the recording panel
				restoreRecordingView();
			}
		}
		
		//Resolving the heights
		int requestedPanelHeight = getResources().getDimensionPixelSize(R.dimen.contentpanel_height);
		int windowThreshold = (int) (getAvailableWindowHeight() * contentPanelFillThreshold);
		
		//Limiting the panel height
		int targetHeight = Math.min(requestedPanelHeight, windowThreshold);
		
		//Getting the panel
		View panel = findViewById(R.id.panel_attachments);
		
		if(animate) {
			//Animating in the panel
			ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
			animator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
			animator.setInterpolator(new AccelerateDecelerateInterpolator());
			animator.addUpdateListener(animation -> {
				int value = (int) animation.getAnimatedValue();
				panel.getLayoutParams().height = value;
				panel.requestLayout();
			});
			animator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					panel.setVisibility(View.VISIBLE);
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					panel.getLayoutParams().height = targetHeight;
					panel.requestLayout();
				}
			});
			animator.start();
		} else {
			//Setting the panel height
			panel.getLayoutParams().height = targetHeight;
			
			//Showing the panel
			findViewById(R.id.panel_attachments).setVisibility(View.VISIBLE);
		}
	}
	
	private void closeAttachmentsPanel(boolean animate) {
		//Returning if the conversation is not ready or the panel is already closed
		if(viewModel.conversationInfo == null || !viewModel.isAttachmentsPanelOpen) return;
		
		//Setting the panel as closed
		viewModel.isAttachmentsPanelOpen = false;
		
		//Getting the panel
		View panel = findViewById(R.id.panel_attachments);
		
		//Closing the panel
		if(animate) {
			ValueAnimator animator = ValueAnimator.ofInt(panel.getHeight(), 0);
			animator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
			animator.setInterpolator(new AccelerateDecelerateInterpolator());
			animator.addUpdateListener(animation -> {
				int value = (int) animation.getAnimatedValue();
				panel.getLayoutParams().height = value;
				panel.requestLayout();
			});
			animator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					panel.getLayoutParams().height = 0;
					panel.setVisibility(View.GONE);
				}
			});
			animator.start();
		} else {
			panel.getLayoutParams().height = 0;
			panel.setVisibility(View.GONE);
		}
	}
	
	private void setupAttachmentsGallerySection() {
		//Getting the view
		ViewGroup viewGroup = findViewById(R.id.viewgroup_attachment_gallery);
		if(viewGroup == null) return;
		
		//Setting the click listeners
		viewGroup.findViewById(R.id.button_attachment_gallery_systempicker).setOnClickListener(view -> requestGalleryFile());
		
		//Checking if the state is failed
		if(viewModel.getAttachmentState(ActivityViewModel.attachmentTypeGallery) == ActivityViewModel.attachmentsStateFailed) {
			//Showing the failed text
			viewGroup.findViewById(R.id.label_attachment_gallery_failed).setVisibility(View.VISIBLE);
			
			//Hiding the permission request button and the list
			viewGroup.findViewById(R.id.button_attachment_gallery_permission).setVisibility(View.GONE);
			viewGroup.findViewById(R.id.list_attachment_gallery).setVisibility(View.GONE);
		}
		//Checking if the permission has not been granted
		else if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			//Hiding the list and failed text
			viewGroup.findViewById(R.id.list_attachment_gallery).setVisibility(View.GONE);
			viewGroup.findViewById(R.id.label_attachment_gallery_failed).setVisibility(View.GONE);
			
			//Setting up the permission request button
			View permissionButton = viewGroup.findViewById(R.id.button_attachment_gallery_permission);
			permissionButton.setVisibility(View.VISIBLE);
			permissionButton.setOnClickListener(view -> Constants.requestPermission(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, permissionRequestStorage));
		} else {
			//Hiding the permission request button and the failed text
			viewGroup.findViewById(R.id.button_attachment_gallery_permission).setVisibility(View.GONE);
			viewGroup.findViewById(R.id.label_attachment_gallery_failed).setVisibility(View.GONE);
			
			//Setting up the list
			RecyclerView list = viewGroup.findViewById(R.id.list_attachment_gallery);
			list.setVisibility(View.VISIBLE);
			list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
			list.addOnScrollListener(new AttachmentListScrollListener(viewGroup.findViewById(R.id.button_attachment_gallery_systempicker)));
			
			//Checking if the files are loaded
			if(viewModel.getAttachmentState(ActivityViewModel.attachmentTypeGallery) == ActivityViewModel.attachmentsStateLoaded) {
				//Setting the list adapter
				list.setAdapter(new AttachmentsGalleryRecyclerAdapter(viewModel.getAttachmentFileList(ActivityViewModel.attachmentTypeGallery)));
			} else {
				//Setting the list adapter
				ArrayList<SimpleAttachmentInfo> itemList = new ArrayList<>();
				for(int i = 0; i < ActivityViewModel.attachmentsTileCount; i++) itemList.add(null);
				AttachmentsRecyclerAdapter<?> adapter = new AttachmentsGalleryRecyclerAdapter(itemList);
				list.setAdapter(adapter);
				
				//Creating the listener
				ActivityViewModel.AttachmentsLoadCallbacks callbacks = attachmentsLoadCallbacks[ActivityViewModel.attachmentTypeGallery];
				if(callbacks == null) {
					callbacks = attachmentsLoadCallbacks[ActivityViewModel.attachmentTypeGallery] = result -> {
						if(result) {
							//Setting the list adapter's list
							((AttachmentsGalleryRecyclerAdapter) list.getAdapter()).setList(viewModel.getAttachmentFileList(ActivityViewModel.attachmentTypeGallery));
						} else {
							//Replacing the list view with the failed text
							viewGroup.findViewById(R.id.list_attachment_gallery).setVisibility(View.GONE);
							viewGroup.findViewById(R.id.label_attachment_gallery_failed).setVisibility(View.VISIBLE);
						}
					};
				}
				
				//Loading the media
				viewModel.indexAttachmentsGallery(callbacks, adapter);
			}
		}
	}
	
	/**
	 * Gets the available window height: the height of the window, without the app bar
	 * @return the available window height
	 */
	private int getAvailableWindowHeight() {
		return getWindow().getDecorView().getHeight() - getSupportActionBar().getHeight();
	}
	
	private void setupAttachmentsAudioSection() {
		//Getting the view
		ViewGroup viewGroup = findViewById(R.id.viewgroup_attachment_audio);
		if(viewGroup == null) return;
		
		//Setting the click listeners
		viewGroup.findViewById(R.id.button_attachment_audio_systempicker).setOnClickListener(view -> requestAudioFile());
		
		//Checking if the permission has not been granted
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			//Setting up the permission request button
			View permissionButton = viewGroup.findViewById(R.id.button_attachment_audio_permission);
			permissionButton.setVisibility(View.VISIBLE);
			permissionButton.setOnClickListener(view -> Constants.requestPermission(this, new String[]{Manifest.permission.RECORD_AUDIO}, permissionRequestAudio));
			
			//Hiding the recording view
			viewGroup.findViewById(R.id.frame_attachment_audio_content).setVisibility(View.GONE);
		} else {
			//Swapping to the content view
			viewGroup.findViewById(R.id.button_attachment_audio_permission).setVisibility(View.GONE);
			viewGroup.findViewById(R.id.frame_attachment_audio_content).setVisibility(View.VISIBLE);
			
			//Getting the views
			recordingActiveGroup = viewGroup.findViewById(R.id.frame_attachment_audio_recording);
			recordingTimeLabel = viewGroup.findViewById(R.id.label_attachment_audio_recording);
			recordingVisualizer = viewGroup.findViewById(R.id.visualizer_attachment_audio_recording);
			
			//Setting the listeners
			viewGroup.findViewById(R.id.frame_attachment_audio_gate).setOnTouchListener(recordingTouchListener);
			//recordingTouchListener
		}
	}
	
	private void setupAttachmentsDocumentsSection() {
		//Getting the view
		ViewGroup viewGroup = findViewById(R.id.viewgroup_attachment_documents);
		if(viewGroup == null) return;
		
		//Setting the click listeners
		viewGroup.findViewById(R.id.button_attachment_documents_systempicker).setOnClickListener(view -> requestDocumentFile());
		
		//Checking if the state is failed
		if(viewModel.getAttachmentState(ActivityViewModel.attachmentTypeDocument) == ActivityViewModel.attachmentsStateFailed) {
			//Showing the failed text
			viewGroup.findViewById(R.id.label_attachment_documents_failed).setVisibility(View.VISIBLE);
			
			//Hiding the permission request button and the list
			viewGroup.findViewById(R.id.button_attachment_documents_permission).setVisibility(View.GONE);
			viewGroup.findViewById(R.id.list_attachment_documents).setVisibility(View.GONE);
		}
		//Checking if the permission has not been granted
		else if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			//Hiding the list and failed text
			viewGroup.findViewById(R.id.list_attachment_documents).setVisibility(View.GONE);
			viewGroup.findViewById(R.id.label_attachment_documents_failed).setVisibility(View.GONE);
			
			//Setting up the permission request button
			View permissionButton = viewGroup.findViewById(R.id.button_attachment_documents_permission);
			permissionButton.setVisibility(View.VISIBLE);
			permissionButton.setOnClickListener(view -> Constants.requestPermission(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, permissionRequestStorage));
		} else {
			//Hiding the permission request button and the failed text
			viewGroup.findViewById(R.id.button_attachment_documents_permission).setVisibility(View.GONE);
			viewGroup.findViewById(R.id.label_attachment_documents_failed).setVisibility(View.GONE);
			
			//Setting up the list
			RecyclerView list = viewGroup.findViewById(R.id.list_attachment_documents);
			list.setVisibility(View.VISIBLE);
			list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
			list.addOnScrollListener(new AttachmentListScrollListener(viewGroup.findViewById(R.id.button_attachment_documents_systempicker)));
			
			//Checking if the files are loaded
			if(viewModel.getAttachmentState(ActivityViewModel.attachmentTypeDocument) == ActivityViewModel.attachmentsStateLoaded) {
				//Setting the list adapter
				list.setAdapter(new AttachmentsDocumentRecyclerAdapter(viewModel.getAttachmentFileList(ActivityViewModel.attachmentTypeDocument)));
			} else {
				//Setting the list adapter
				List<SimpleAttachmentInfo> itemList = new ArrayList<>();
				for(int i = 0; i < ActivityViewModel.attachmentsTileCount; i++) itemList.add(null);
				AttachmentsRecyclerAdapter<?> adapter = new AttachmentsDocumentRecyclerAdapter(itemList);
				list.setAdapter(adapter);
				
				//Creating the listener
				ActivityViewModel.AttachmentsLoadCallbacks callbacks = attachmentsLoadCallbacks[ActivityViewModel.attachmentTypeDocument];
				if(callbacks == null) {
					callbacks = attachmentsLoadCallbacks[ActivityViewModel.attachmentTypeDocument] = result -> {
						if(result) {
							//Setting the list adapter's list
							((AttachmentsDocumentRecyclerAdapter) list.getAdapter()).setList(viewModel.getAttachmentFileList(ActivityViewModel.attachmentTypeDocument));
						} else {
							//Replacing the list view with the failed text
							viewGroup.findViewById(R.id.list_attachment_documents).setVisibility(View.GONE);
							viewGroup.findViewById(R.id.label_attachment_documents_failed).setVisibility(View.VISIBLE);
						}
					};
				}
				
				//Loading the media
				viewModel.indexAttachmentsDocument(callbacks, adapter);
			}
		}
	}
	
	private class AttachmentListScrollListener extends RecyclerView.OnScrollListener {
		boolean buttonIsBubble = false;
		private final CardView pickerView;
		
		AttachmentListScrollListener(CardView pickerView) {
			this.pickerView = pickerView;
		}
		
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			boolean isAtStart = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition() == 0;
			if(buttonIsBubble == isAtStart) {
				buttonIsBubble = !isAtStart;
				setSystemPickerBubbleState(pickerView, buttonIsBubble);
			}
		}
	}
	
	private float systemPickerBubbleStateSizeTile = -1;
	private float systemPickerBubbleStateRadiusTile = -1;
	private float systemPickerBubbleStateElevationBubble = -1;
	void setSystemPickerBubbleState(CardView view, boolean bubble) {
		//Fetching the reference target values if needed
		if(systemPickerBubbleStateSizeTile == -1) {
			systemPickerBubbleStateSizeTile = getResources().getDimensionPixelSize(R.dimen.contenttile_size);
			systemPickerBubbleStateRadiusTile = getResources().getDimensionPixelSize(R.dimen.contenttile_radius);
			systemPickerBubbleStateElevationBubble = Constants.dpToPx(4);
		}
		
		//Establishing the target values
		float sizeStart = view.getHeight();
		float radiusStart = view.getRadius();
		float elevationStart = view.getCardElevation();
		float sizeTarget, radiusTarget, elevationTarget;
		if(bubble) {
			sizeTarget = view.getWidth();
			radiusTarget = sizeTarget / 2F;
			elevationTarget = systemPickerBubbleStateElevationBubble;
		} else {
			sizeTarget = systemPickerBubbleStateSizeTile;
			radiusTarget = systemPickerBubbleStateRadiusTile;
			elevationTarget = 0;
		}
		
		//Setting the value animator
		ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
		valueAnimator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
		valueAnimator.addUpdateListener(animation -> {
			float value = (float) animation.getAnimatedValue();
			view.getLayoutParams().height = (int) Constants.lerp(value, sizeStart, sizeTarget);
			view.setRadius(Constants.lerp(value, radiusStart, radiusTarget));
			view.setCardElevation(Constants.lerp(value, elevationStart, elevationTarget));
			view.requestLayout();
		});
		valueAnimator.start();
	}
	
	private void openDetailsPanel(boolean restore) {
		//Returning if the conversation is not ready or the panel is already open
		if(viewModel.messagesState.getValue() != ActivityViewModel.messagesStateReady || restore != viewModel.isDetailsPanelOpen) return;
		
		//Setting the panel as open
		viewModel.isDetailsPanelOpen = true;
		
		//Inflating the view
		ViewStub viewStub = findViewById(R.id.viewstub_messaginginfo);
		if(viewStub != null) {
			//Inflating the view stub
			ViewGroup inflated = (ViewGroup) viewStub.inflate();
			
			//Coloring the UI
			colorUI(inflated);
			
			//Getting the views
			Switch notificationsSwitch = inflated.findViewById(R.id.switch_getnotifications);
			//Switch pinnedSwitch = inflated.findViewById(R.id.switch_pinconversation);
			
			//Restoring the elements' states
			notificationsSwitch.setChecked(!viewModel.conversationInfo.isMuted());
			//pinnedSwitch.setChecked(viewModel.conversationInfo.isPinned());
			
			//Setting the listeners
			inflated.findViewById(R.id.group_getnotifications).setOnClickListener(view -> notificationsSwitch.setChecked(!notificationsSwitch.isChecked()));
			notificationsSwitch.setOnCheckedChangeListener((view, isChecked) -> {
				//Updating the conversation
				boolean isMuted = !isChecked;
				viewModel.conversationInfo.setMuted(isMuted);
				DatabaseManager.getInstance().updateConversationMuted(viewModel.conversationInfo.getLocalID(), isMuted);
				
			});
			//inflated.findViewById(R.id.group_pinconversation).setOnClickListener(view -> pinnedSwitch.setChecked(!pinnedSwitch.isChecked()));
			inflated.findViewById(R.id.button_changecolor).setOnClickListener(view -> showColorDialog(null, viewModel.conversationInfo.getConversationColor()));
			//pinnedSwitch.setOnCheckedChangeListener((view, isChecked) -> viewModel.conversationInfo.setPinned(isChecked));
			
			findViewById(R.id.button_archive).setOnClickListener(view -> {
				//Toggling the archive state
				boolean newState = !viewModel.conversationInfo.isArchived();
				viewModel.conversationInfo.setArchived(newState);
				
				//Sending an update
				LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
				
				//Updating the conversation's database entry
				DatabaseManager.getInstance().updateConversationArchived(viewModel.conversationInfo.getLocalID(), newState);
				
				//Updating the button
				MaterialButton buttonView = (MaterialButton) view;
				buttonView.setText(newState ? R.string.action_unarchive : R.string.action_archive);
				buttonView.setIconResource(newState ? R.drawable.unarchive : R.drawable.archive);
			});
			
			findViewById(R.id.button_delete).setOnClickListener(view -> {
				//Creating a dialog
				AlertDialog dialog = new AlertDialog.Builder(this)
						.setMessage(R.string.message_confirm_deleteconversation_current)
						.setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
						.setPositiveButton(R.string.action_delete, (dialogInterface, which) -> {
							//Removing the conversation from memory
							ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
							if(conversations != null) conversations.remove(viewModel.conversationInfo);
							
							//Deleting the conversation from the database
							DatabaseManager.getInstance().deleteConversation(viewModel.conversationInfo);
							
							//Sending an update
							LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
							
							//Finishing the activity
							finish();
						})
						.create();
				
				//Configuring the dialog's listener
				dialog.setOnShowListener(dialogInterface -> {
					//Setting the button's colors
					int color = getResources().getColor(R.color.colorActionDelete, null);
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
					dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
				});
				
				//Showing the dialog
				dialog.show();
			});
			
			//Adding the conversation members
			detailsBuildConversationMembers(new ArrayList<>(viewModel.conversationInfo.getConversationMembers()));
		} else {
			((ScrollView) findViewById(R.id.group_messaginginfo_scroll)).fullScroll(ScrollView.FOCUS_UP);
		}
		
		//Finding the views
		FrameLayout panel = findViewById(R.id.panel_messaginginfo);
		ViewGroup content = findViewById(R.id.group_messaginginfo_content);
		
		//Measuring the content
		content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		
		//Checking if the view occupies most of the window
		if(content.getMeasuredHeight() > getAvailableWindowHeight() * bottomSheetFillThreshold) {
			//Instructing the panel to fill the entire view
			ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) panel.getLayoutParams();
			params.topToBottom = R.id.appbar;
			params.height = 0;
		} else {
			//Instructing the panel to align to the bottom and wrap its content
			ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) panel.getLayoutParams();
			params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
			params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
		}
		
		if(restore) {
			//Configuring the views
			detailScrim.setVisibility(View.VISIBLE);
			detailScrim.setAlpha(1);
		} else {
			//Animating in the panel
			int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.messagedetails_slide_in_bottom);
			animation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					panel.setVisibility(View.VISIBLE);
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			panel.startAnimation(animation);
			detailScrim.animate().alpha(1).withStartAction(() -> detailScrim.setVisibility(View.VISIBLE)).setDuration(duration).start();
		}
	}
	
	private void closeDetailsPanel() {
		//Returning if the conversation is not ready or the panel is already closed
		if(viewModel.conversationInfo == null || !viewModel.isDetailsPanelOpen) return;
		
		//Setting the panel as closed
		viewModel.isDetailsPanelOpen = false;
		
		//Animating out the panel
		FrameLayout panel = findViewById(R.id.panel_messaginginfo);
		Animation animation = AnimationUtils.loadAnimation(this, R.anim.messagedetails_slide_out_bottom);
		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				panel.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
		});
		panel.startAnimation(animation);
		
		int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
		detailScrim.animate().alpha(0).withEndAction(() -> detailScrim.setVisibility(View.GONE)).setDuration(duration).start();
	}
	
	private void detailsBuildConversationMembers(List<ConversationManager.MemberInfo> members) {
		//Getting the members layout
		//ViewGroup membersLayout = findViewById(R.id.list_conversationmembers);
		
		//Sorting the members
		Collections.sort(members, ConversationManager.memberInfoComparator);
		
		//Adding the member views
		boolean showMemberColors = members.size() > 1;
		for(int i = 0; i < members.size(); i++) addMemberView(members.get(i), i, showMemberColors);
		/* for(final ConversationManager.MemberInfo member : members) {
			//Creating the view
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View memberEntry = inflater.inflate(R.layout.listitem_member, membersLayout, false);
			
			//Setting the default information
			((TextView) memberEntry.findViewById(R.id.label_member)).setText(member.getName());
			((ImageView) memberEntry.findViewById(R.id.profile_default)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			
			//Filling in the information
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(this, member.getName(), new UserCacheHelper.UserFetchResult(memberEntry) {
				@Override
				void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Returning if the user info is invalid
					if(userInfo == null) return;
					
					//Getting the view
					View memberEntry = viewReference.get();
					if(memberEntry == null) return;
					
					//Setting the tag
					memberEntry.setTag(userInfo.getContactLookupUri());
					
					//Setting the member's name
					((TextView) memberEntry.findViewById(R.id.label_member)).setText(userInfo.getContactName());
					TextView addressView = memberEntry.findViewById(R.id.label_address);
					addressView.setText(member.getName());
					addressView.setVisibility(View.VISIBLE);
				}
			});
			MainApplication.getInstance().getBitmapCacheHelper().assignContactImage(getApplicationContext(), member.getName(), (View) memberEntry.findViewById(R.id.profile_image));
			
			//Configuring the color editor
			ImageView changeColorButton = memberEntry.findViewById(R.id.button_change_color);
			if(members.size() == 1) {
				changeColorButton.setVisibility(View.GONE);
			} else {
				changeColorButton.setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
				changeColorButton.setOnClickListener(view -> showColorDialog(member, member.getColor()));
				changeColorButton.setVisibility(View.VISIBLE);
			}
			
			//Setting the click listener
			memberEntry.setOnClickListener(view -> {
				//Returning if the view has no tag
				if(view.getTag() == null) return;
				
				//Opening the user's contact profile
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData((Uri) view.getTag());
				//intent.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(view.getTag())));
				view.getContext().startActivity(intent);
			});
			
			//Adding the view
			membersLayout.addView(memberEntry);
			memberListViews.put(member.getName(), memberEntry);
		} */
	}
	
	private void addMemberView(ConversationManager.MemberInfo member, int index, boolean showColor) {
		//Getting the members layout
		ViewGroup membersLayout = findViewById(R.id.list_conversationmembers);
		if(membersLayout == null) return;
		
		//Creating the view
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View memberEntry = inflater.inflate(R.layout.listitem_member, membersLayout, false);
		
		//Setting the default information
		((TextView) memberEntry.findViewById(R.id.label_member)).setText(member.getName());
		((ImageView) memberEntry.findViewById(R.id.profile_default)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		
		//Filling in the information
		MainApplication.getInstance().getUserCacheHelper().getUserInfo(this, member.getName(), new UserCacheHelper.UserFetchResult(memberEntry) {
			@Override
			void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
				//Returning if the user info is invalid
				if(userInfo == null) return;
				
				//Getting the view
				View memberEntry = viewReference.get();
				if(memberEntry == null) return;
				
				//Setting the tag
				memberEntry.setTag(userInfo.getContactLookupUri());
				
				//Setting the member's name
				((TextView) memberEntry.findViewById(R.id.label_member)).setText(userInfo.getContactName());
				TextView addressView = memberEntry.findViewById(R.id.label_address);
				addressView.setText(member.getName());
				addressView.setVisibility(View.VISIBLE);
			}
		});
		MainApplication.getInstance().getBitmapCacheHelper().assignContactImage(getApplicationContext(), member.getName(), (View) memberEntry.findViewById(R.id.profile_image));
		
		//Configuring the color editor
		ImageView changeColorButton = memberEntry.findViewById(R.id.button_change_color);
		if(showColor) {
			changeColorButton.setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			changeColorButton.setOnClickListener(view -> showColorDialog(member, member.getColor()));
			changeColorButton.setVisibility(View.VISIBLE);
		} else {
			changeColorButton.setVisibility(View.GONE);
		}
		
		//Setting the click listener
		memberEntry.setOnClickListener(view -> {
			//Returning if the view has no tag
			if(view.getTag() == null) return;
			
			//Opening the user's contact profile
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData((Uri) view.getTag());
			//intent.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(view.getTag())));
			view.getContext().startActivity(intent);
		});
		
		//Adding the view
		membersLayout.addView(memberEntry, index);
		memberListViews.put(member, memberEntry);
	}
	
	private void removeMemberView(ConversationManager.MemberInfo member) {
		//Getting the members layout
		ViewGroup membersLayout = findViewById(R.id.list_conversationmembers);
		if(membersLayout == null) return;
		
		//Removing the view
		membersLayout.removeView(memberListViews.get(member));
		memberListViews.remove(member);
		
		//Closing the dialog
		if(currentColorPickerDialog != null && currentColorPickerDialogMember == member) currentColorPickerDialog.dismiss();
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		//Calling the super method if there is no recording taking place
		if(!viewModel.isRecording()) return super.dispatchTouchEvent(event);
		
		//Consuming the move event (as to not affect the elements below)
		if(event.getAction() == MotionEvent.ACTION_MOVE) return true;
		else if(event.getAction() == MotionEvent.ACTION_UP) {
			//Stopping the recording session
			stopRecording();
			
			//Returning true to consume the event
			return true;
		}
		
		//Calling the super method
		return super.dispatchTouchEvent(event);
	}
	
	private void colorUI(ViewGroup root) {
		//Returning if the conversation is invalid
		if(viewModel.conversationInfo == null) return;
		
		//Getting the color
		int color = viewModel.conversationInfo.getConversationColor();
		int darkerColor = ColorHelper.darkenColor(color);
		int lighterColor = ColorHelper.lightenColor(color);
		
		//Coloring the app and status bar
		getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
		getWindow().setStatusBarColor(darkerColor);
		
		//Updating the task description
		if(lastTaskDescription != null) setTaskDescription(lastTaskDescription = new ActivityManager.TaskDescription(lastTaskDescription.getLabel(), lastTaskDescription.getIcon(), color));
		
		//Coloring tagged parts of the UI
		for(View view : Constants.getViewsByTag(root, getResources().getString(R.string.tag_primarytint))) {
			if(view instanceof ImageView) ((ImageView) view).setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
			else if(view instanceof Switch) {
				Switch switchView = (Switch) view;
				switchView.setThumbTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0xFFFAFAFA, color}));
				switchView.setTrackTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0x61000000, color}));
			}
			else if(view instanceof MaterialButton) {
				MaterialButton buttonView = (MaterialButton) view;
				buttonView.setTextColor(color);
				buttonView.setIconTint(ColorStateList.valueOf(color));
				buttonView.setRippleColor(ColorStateList.valueOf(color));
			}
			else if(view instanceof TextView) ((TextView) view).setTextColor(color);
			else if(view instanceof RelativeLayout) view.setBackground(new ColorDrawable(color));
			else if(view instanceof FrameLayout) view.setBackgroundTintList(ColorStateList.valueOf(color));
		}
		
		//Coloring the unique UI components
		if(viewModel.lastUnreadCount == 0) bottomFAB.setImageTintList(ColorStateList.valueOf(color));
		else bottomFAB.setBackgroundTintList(ColorStateList.valueOf(color));
		bottomFABBadge.setBackgroundTintList(ColorStateList.valueOf(darkerColor));
		findViewById(R.id.fab_bottom_splash).setBackgroundTintList(ColorStateList.valueOf(lighterColor));
		
		//Coloring the info bars
		infoBarConnection.setColor(color);
	}
	
	public void onClickRetryLoad(View view) {
		byte state = viewModel.messagesState.getValue();
		if(state == ActivityViewModel.messagesStateFailedConversation) viewModel.loadConversation();
		else if(state == ActivityViewModel.messagesStateFailedMessages) viewModel.loadMessages();
	}
	
	void setMessageBarState(boolean enabled) {
		//Setting the message input field
		messageInputField.setEnabled(enabled);
		
		//Setting the send button
		buttonSendMessage.setEnabled(enabled);
		//messageSendButton.setClickable(messageBoxHasText);
		//buttonSendMessage.setAlpha(enabled && messageBoxHasText ? 1 : 0.38f);
		
		//Setting the add content button
		buttonAddContent.setEnabled(enabled);
		//buttonAddContent.setAlpha(enabled ? 1 : 0.38f);
		
		//Opening the soft keyboard if the text input is enabled and the soft keyboard should be used
		//if(enabled) ((InputMethodManager) activitySource.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(messageInputField, InputMethodManager.SHOW_FORCED);
	}
	
	private void updateSendButton(boolean restore) {
		//Getting the send button state
		boolean state = messageInputField.getText().length() > 0 || !viewModel.draftQueueList.isEmpty();
		if(currentSendButtonState == state) return;
		currentSendButtonState = state;
		
		//Updating the button
		buttonSendMessage.setClickable(state);
		float targetAlpha = state ? 1 : Constants.disabledAlpha;
		if(restore) buttonSendMessage.setAlpha(targetAlpha);
		else buttonSendMessage.animate().setDuration(100).alpha(targetAlpha);
	}
	
	/* private boolean startRecording() {
		//Starting the recording
		if(!viewModel.startRecording(this)) return false;
		
		//Resetting the recording bar's color
		//recordingBar.setBackgroundColor(Constants.resolveColorAttr(this, android.R.attr.colorBackgroundFloating));
		
		//Setting the recording indicator's Y
		recordingIndicator.setY(inputBar.getTop() + inputBar.getHeight() / 2 - recordingIndicator.getHeight() / 2);
		
		//Revealing the indicator
		recordingIndicator.setVisibility(View.VISIBLE);
		Animator animator = ViewAnimationUtils.createCircularReveal(recordingIndicator, recordingIndicator.getLeft() + recordingIndicator.getWidth() / 2, recordingIndicator.getTop() + recordingIndicator.getWidth() / 2, 0, recordingIndicator.getWidth());
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.start();
		
		//Showing the recording input
		//showRecordingBar();
		
		//Returning true
		return true;
	}
	
	void stopRecording(boolean forceDiscard, int positionX) {
		//Returning if the input state is not recording
		if(viewModel.inputState != ActivityViewModel.inputStateRecording) return;
		
		//Stopping the recording session
		boolean fileAvailable = viewModel.stopRecording(forceDiscard);
		
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
				if(viewModel.inputState == ActivityViewModel.inputStateRecording) return;
				
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
		
		//Hiding the recording bar
		//int[] recordingButtonLocation = {0, 0};
		//contentRecordButton.getLocationOnScreen(recordingButtonLocation);
		//hideRecordingBar(fileAvailable, recordingButtonLocation[0]);
		hideRecordingBar(fileAvailable, positionX);
		
		//Sending the file
		if(fileAvailable) sendFile(viewModel.targetFile);
	} */
	
	private String getInputBarMessage() {
		//Returning a generic message if the service is invalid
		if(viewModel.conversationInfo.getService() == null)
			return getResources().getString(R.string.imperative_messageinput);
		
		switch(viewModel.conversationInfo.getService()) {
			case Constants.serviceIDAppleMessage:
				return getResources().getString(R.string.proper_imessage);
			case Constants.serviceIDSMS:
				return getResources().getString(R.string.proper_sms);
			default:
				return viewModel.conversationInfo.getService();
		}
	}
	
	void showServerWarning(int reason) {
		switch(reason) {
			case ConnectionService.intentResultCodeInternalException:
				infoBarConnection.setText(R.string.message_serverstatus_internalexception);
				infoBarConnection.setButton(R.string.action_retry, view -> {
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
			case ConnectionService.intentResultCodeBadRequest:
				infoBarConnection.setText(R.string.message_serverstatus_badrequest);
				infoBarConnection.setButton(R.string.action_retry, view -> {
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
			case ConnectionService.intentResultCodeClientOutdated:
				infoBarConnection.setText(R.string.message_serverstatus_clientoutdated);
				infoBarConnection.setButton(R.string.action_update, view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()))));
				break;
			case ConnectionService.intentResultCodeServerOutdated:
				infoBarConnection.setText(R.string.message_serverstatus_serveroutdated);
				infoBarConnection.setButton(R.string.screen_help, view -> startActivity(new Intent(Intent.ACTION_VIEW, Constants.serverUpdateAddress)));
				break;
			case ConnectionService.intentResultCodeUnauthorized:
				infoBarConnection.setText(R.string.message_serverstatus_authfail);
				infoBarConnection.setButton(R.string.action_reconfigure, view -> startActivity(new Intent(Messaging.this, ServerSetup.class)));
				break;
			case ConnectionService.intentResultCodeConnection:
				infoBarConnection.setText(R.string.message_serverstatus_noconnection);
				infoBarConnection.setButton(R.string.action_retry, view -> {
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
				infoBarConnection.setText(R.string.message_serverstatus_unknown);
				infoBarConnection.setButton(R.string.action_retry, view -> {
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
		
		//Showing the info bar
		infoBarConnection.show();
	}
	
	void hideServerWarning() {
		infoBarConnection.hide();
	}
	
	void hideToolbar() {
		//Returning if the toolbar is already invisible
		if(!toolbarVisible) return;
		
		//Setting the toolbar as invisible
		toolbarVisible = false;
		
		//Hiding the app bar
		appBar.setVisibility(View.GONE);
	}
	
	void showToolbar() {
		//Returning if the toolbar is already visible
		if(toolbarVisible) return;
		
		//Setting the toolbar as visible
		toolbarVisible = true;
		
		//Showing the app bar
		appBar.setVisibility(View.VISIBLE);
	}
	
	void setFABVisibility(boolean visible) {
		//Returning if the current state matches the requested state
		if(bottomFAB.isShown() == visible) return;
		
		if(visible) {
			//Showing the FAB
			bottomFAB.show();
			
			//Checking if there are unread messages
			if(viewModel.conversationInfo.getUnreadMessageCount() > 0) {
				//Animating the badge (to go with the FAB appearance)
				bottomFABBadge.setVisibility(View.VISIBLE);
				bottomFABBadge.animate()
						.scaleX(1)
						.scaleY(1)
						.setInterpolator(new OvershootInterpolator())
						.start();
			}
		} else {
			//Hiding the FAB
			bottomFAB.hide();
			
			//Checking if there are unread messages
			if(viewModel.conversationInfo.getUnreadMessageCount() > 0) {
				//Hiding the badge (to go with the FAB disappearance)
				bottomFABBadge.animate()
						.scaleX(0)
						.scaleY(0)
						.withEndAction(() -> bottomFABBadge.setVisibility(View.GONE))
						.start();
			}
		}
	}
	
	void restoreUnreadIndicator() {
		//Returning if the indicator shouldn't be visible
		if(viewModel.conversationInfo.getUnreadMessageCount() == 0) return;
		
		//Coloring the FAB
		bottomFAB.setBackgroundTintList(ColorStateList.valueOf(viewModel.conversationInfo.getConversationColor()));
		bottomFAB.setImageTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.white, null)));
		
		//Updating the badge
		bottomFABBadge.setText(String.format(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? getResources().getConfiguration().getLocales().get(0) : getResources().getConfiguration().locale, "%d", viewModel.conversationInfo.getUnreadMessageCount()));
		bottomFABBadge.setVisibility(View.VISIBLE);
		bottomFABBadge.setScaleX(1);
		bottomFABBadge.setScaleY(1);
	}
	
	void updateUnreadIndicator() {
		//Returning if the value has not changed
		if(viewModel.lastUnreadCount == viewModel.conversationInfo.getUnreadMessageCount()) return;
		
		//Getting the color
		int colorTint = viewModel.conversationInfo.getConversationColor();
		
		//Checking if there are any unread messages
		if(viewModel.conversationInfo.getUnreadMessageCount() > 0) {
			//Coloring the FAB
			bottomFAB.setBackgroundTintList(ColorStateList.valueOf(viewModel.conversationInfo.getConversationColor()));
			bottomFAB.setImageTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.white, null)));
			
			//Updating the badge text
			bottomFABBadge.setText(Constants.intToFormattedString(getResources(), viewModel.conversationInfo.getUnreadMessageCount()));
			
			//Animating the badge
			if(bottomFAB.isShown()) {
				bottomFABBadge.setVisibility(View.VISIBLE);
				bottomFABBadge.setScaleX(0);
				bottomFABBadge.setScaleY(0);
				bottomFABBadge.animate()
						.scaleX(1)
						.scaleY(1)
						.setInterpolator(new OvershootInterpolator())
						.start();
			}
			
			//Checking if there were previously no unread messages and the FAB is visible
			if(viewModel.lastUnreadCount == 0 && bottomFAB.isShown()) {
				//Animating the splash
				View bottomSplash = findViewById(R.id.fab_bottom_splash);
				bottomSplash.setScaleX(1);
				bottomSplash.setScaleY(1);
				bottomSplash.setAlpha(1);
				bottomSplash.setVisibility(View.VISIBLE);
				bottomSplash.animate()
						.scaleX(2.5F)
						.scaleY(2.5F)
						.alpha(0)
						.setDuration(1000)
						.setInterpolator(new DecelerateInterpolator())
						.withEndAction(() -> bottomSplash.setVisibility(View.GONE))
						.start();
			}
		} else {
			//Restoring the FAB color
			bottomFAB.setBackgroundTintList(ColorStateList.valueOf(Constants.resolveColorAttr(this, android.R.attr.colorBackgroundFloating)));
			bottomFAB.setImageTintList(ColorStateList.valueOf(colorTint));
			
			//Hiding the badge
			if(bottomFAB.isShown()) bottomFABBadge.animate()
					.scaleX(0)
					.scaleY(0)
					.withEndAction(() -> bottomFABBadge.setVisibility(View.GONE))
					.start();
		}
		
		//Setting the last unread message count
		viewModel.lastUnreadCount = viewModel.conversationInfo.getUnreadMessageCount();
	}
	
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		//Returning if there are no grant results
		if(grantResults.length == 0) return;
		
		switch(requestCode) {
			case permissionRequestStorage:
				//Checking if the request was granted
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//Updating the attachment sections
					setupAttachmentsGallerySection();
					setupAttachmentsDocumentsSection();
				}
				break;
			case permissionRequestAudio:
				//Checking if the request was granted
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//Updating the attachment section
					setupAttachmentsAudioSection();
				}
				break;
			case permissionRequestAudioDirect: {
				//Checking if the request was denied
				if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
					//Creating a dialog
					AlertDialog dialog = new AlertDialog.Builder(Messaging.this)
							.setTitle(R.string.message_permissionrejected)
							.setMessage(R.string.message_permissiondetails_microphone_failedrequest)
							.setPositiveButton(R.string.action_retry, (dialogInterface, which) -> {
								//Requesting microphone access again
								Constants.requestPermission(Messaging.this, new String[]{Manifest.permission.RECORD_AUDIO}, permissionRequestAudio);
								
								//Dismissing the dialog
								dialogInterface.dismiss();
							})
							.setNeutralButton(R.string.screen_settings, (dialogInterface, which) -> {
								//Showing the application settings
								Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
								intent.setData(Uri.parse("package:" + getPackageName()));
								startActivity(intent);
								
								//Dismissing the dialog
								dialogInterface.dismiss();
							})
							.setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
							.create();
					
					//Configuring the dialog's listener
					dialog.setOnShowListener(dialogInterface -> {
						//Setting the button's colors
						int color = viewModel.conversationInfo.getConversationColor();
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
						dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(color);
						dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
					});
					
					//Displaying the dialog
					dialog.show();
				} else {
					//Updating the attachment sections
					setupAttachmentsAudioSection();
				}
			}
		}
	}
	
	private void requestTakePicture() {
		//Creating the intent
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		//Checking if there are no apps that can take the intent
		if(takePictureIntent.resolveActivity(getPackageManager()) == null) {
			//Telling the user via a toast
			Toast.makeText(Messaging.this, R.string.message_intenterror_camera, Toast.LENGTH_SHORT).show();
			
			//Returning
			return;
		}
		
		//Finding a free file
		viewModel.targetFileIntent = MainApplication.getDraftTarget(this, viewModel.conversationID, Constants.pictureName);
		
		/* try {
			//Creating the targets
			if(!viewModel.targetFile.getParentFile().mkdir()) throw new IOException();
			//if(!viewModel.targetFile.createNewFile()) throw new IOException();
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning
			return;
		} */
		
		//Getting the content uri
		Uri imageUri = FileProvider.getUriForFile(this, MainApplication.fileAuthority, viewModel.targetFileIntent);
		
		//Setting the clip data
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		
		//Setting the file path (for future reference)
		//takePictureIntent.putExtra(Constants.intentParamDataFile, viewModel.targetFile);
		
		//Starting the activity
		startActivityForResult(takePictureIntent, intentTakePicture);
	}
	
	private void launchPickerIntent(String[] mimeTypes) {
		Intent intent = new Intent();
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.imperative_selectfile)), intentPickFile);
	}
	
	private void requestGalleryFile() {
		launchPickerIntent(new String[]{"image/*", "video/*"});
	}
	
	private void requestAudioFile() {
		launchPickerIntent(new String[]{"audio/*"});
	}
	
	private void requestDocumentFile() {
		launchPickerIntent(documentMimeTypes);
	}
	
	public static ArrayList<Long> getForegroundConversations() {
		//Creating the list
		ArrayList<Long> list = new ArrayList<>();
		
		//Iterating over the loaded conversations
		for(Iterator<WeakReference<Messaging>> iterator = foregroundConversations.iterator(); iterator.hasNext(); ) {
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
	
	/* public static ArrayList<Long> getActivityLoadedConversations() {
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
	} */
	
	void playScreenEffect(String effect, View target) {
		//Returning if an effect is already playing
		if(currentScreenEffectPlaying) return;
		currentScreenEffectPlaying = true;
		
		switch(effect) {
			case Constants.appleSendStyleScrnEcho:
				//Activating the effect view
				appleEffectView.playEcho(target);
				break;
			case Constants.appleSendStyleScrnSpotlight:
				currentScreenEffectPlaying = false;
				break;
			case Constants.appleSendStyleScrnBalloons:
				appleEffectView.playBalloons();
				break;
			case Constants.appleSendStyleScrnConfetti: {
				//Activating the Konfetti view
				KonfettiView konfettiView = findViewById(R.id.konfetti);
				konfettiView.build()
						.addColors(Constants.effectColors)
						.setDirection(0D, 359D)
						.setSpeed(4F, 8F)
						.setFadeOutEnabled(true)
						.setTimeToLive(5000L)
						.addShapes(Shape.RECT, Shape.CIRCLE)
						.addSizes(new Size(12, 5), new Size(16, 6))
						.setPosition(-50F, konfettiView.getWidth() + 50F, -50F, -50F)
						.streamFor(300, confettiDuration);
				
				//Setting the timer to mark the effect as finished
				new Handler().postDelayed(() -> currentScreenEffectPlaying = false, confettiDuration * 5);
				
				break;
			}
			case Constants.appleSendStyleScrnLove:
				currentScreenEffectPlaying = false;
				break;
			case Constants.appleSendStyleScrnLasers:
				currentScreenEffectPlaying = false;
				break;
			case Constants.appleSendStyleScrnFireworks:
				currentScreenEffectPlaying = false;
				break;
			case Constants.appleSendStyleScrnShootingStar:
				currentScreenEffectPlaying = false;
				break;
			case Constants.appleSendStyleScrnCelebration:
				currentScreenEffectPlaying = false;
				break;
			default:
				currentScreenEffectPlaying = false;
				break;
		}
	}
	
	void onProgressiveLoadStart() {
		//Updating the recycler adapter (to show the loading spinner)
		messageListAdapter.notifyItemInserted(0);
	}
	
	void onProgressiveLoadFinish(int itemCount) {
		messageListAdapter.notifyItemRemoved(0); //Removing the loading spinner
		messageListAdapter.notifyItemRangeInserted(0, itemCount); //Inserting the new items
	}
	
	void startRecording(float touchX, float touchY) {
		//Telling the view model to start recording
		boolean result = viewModel.startRecording(this);
		if(!result) return;
		
		//Revealing the recording view
		revealRecordingView(touchX, touchY);
	}
	
	void stopRecording() {
		//Telling the view model to stop recording
		viewModel.stopRecording(true, false);
	}
	
	private void restoreRecordingView() {
		if(recordingActiveGroup == null || !viewModel.isRecording()) return;
		recordingActiveGroup.setVisibility(View.VISIBLE);
	}
	
	private void revealRecordingView(float touchX, float touchY) {
		//Returning if the view is invalid
		if(recordingActiveGroup == null) return;
		
		//Calculating the radius
		float greaterX = Math.max(touchX, recordingActiveGroup.getWidth() - touchX);
		float greaterY = Math.max(touchY, recordingActiveGroup.getHeight() - touchY);
		float endRadius = (float) Math.hypot(greaterX, greaterY);
		
		//Revealing the recording view
		Animator animator = ViewAnimationUtils.createCircularReveal(recordingActiveGroup, (int) touchX, (int) touchY, 0, endRadius);
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				recordingActiveGroup.setAlpha(1);
				recordingActiveGroup.setVisibility(View.VISIBLE);
				recordingVisualizer.clear();
				recordingVisualizer.attachMediaRecorder(viewModel.mediaRecorder);
			}
		});
		animator.start();
	}
	
	private void concealRecordingView() {
		//Returning if the view is invalid
		if(recordingActiveGroup == null) return;
		
		//Fading the view
		recordingActiveGroup.animate().alpha(0).withEndAction(() -> {
			if(viewModel.isRecording()) return;
			recordingActiveGroup.setVisibility(View.GONE);
			recordingVisualizer.detachMediaRecorder();
		});
	}
	
	class MessageListRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the reference values
		private static final int itemTypeLoadingBar = -1;
		
		//Creating the values
		private ArrayList<ConversationManager.ConversationItem> conversationItems = null;
		private RecyclerView recyclerView;
		
		//Creating the pools
		private final SparseArray<Pools.SimplePool<? extends RecyclerView.ViewHolder>> componentPoolList = new SparseArray<>();
		private final PoolSource poolSource = new PoolSource();
		
		MessageListRecyclerAdapter() {
			//Enabling stable IDs
			setHasStableIds(true);
		}
		
		void setItemList(ArrayList<ConversationManager.ConversationItem> items) {
			//Setting the conversation items
			conversationItems = items;
			
			//Updating the view
			messageList.getRecycledViewPool().clear();
			notifyDataSetChanged();
		}
		
		@Override
		public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
			super.onAttachedToRecyclerView(recyclerView);
			
			this.recyclerView = recyclerView;
		}
		
		@Override
		@NonNull
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			//Returning the correct view holder
			switch(viewType) {
				case ConversationManager.ConversationItem.viewTypeMessage:
					return new ConversationManager.MessageInfo.ViewHolder(LayoutInflater.from(Messaging.this).inflate(R.layout.listitem_message, parent, false));
				case ConversationManager.ConversationItem.viewTypeAction:
					return new ConversationManager.ActionLineViewHolder(LayoutInflater.from(Messaging.this).inflate(R.layout.listitem_action, parent, false));
				case itemTypeLoadingBar: {
					View loadingView = LayoutInflater.from(Messaging.this).inflate(R.layout.listitem_loading, parent, false);
					((ProgressBar) loadingView.findViewById(R.id.progressbar)).setIndeterminateTintList(ColorStateList.valueOf(viewModel.conversationInfo.getConversationColor()));
					return new LoadingViewHolder(loadingView);
				}
				default:
					throw new IllegalArgumentException("Invalid view type received: " + viewType);
			}
		}
		
		@Override
		public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
			if(holder instanceof ConversationManager.MessageInfo.ViewHolder) {
				for(ConversationManager.MessageComponent.ViewHolder attachmentHolder : ((ConversationManager.MessageInfo.ViewHolder) holder).messageComponents) {
					attachmentHolder.cleanupState();
				}
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			//Returning if there are no items or the item is the loading spinner
			if(conversationItems == null || getItemViewType(position) == itemTypeLoadingBar) return;
			
			//Getting the item
			ConversationManager.ConversationItem conversationItem;
			if(viewModel.isProgressiveLoadInProgress()) conversationItem = conversationItems.get(position - 1);
			else conversationItem = conversationItems.get(position);
			
			//Checking if the item is a message
			boolean isMessage = false;
			if(conversationItem instanceof ConversationManager.MessageInfo) {
				((ConversationManager.MessageInfo.ViewHolder) holder).setPoolSource(poolSource);
				isMessage = true;
			}
			
			//Creating the view
			conversationItem.bindView(holder, Messaging.this);
			
			//Setting the view source
			conversationItem.setViewHolderSource(new Constants.ViewHolderSourceImpl<RecyclerView.ViewHolder>(recyclerView, conversationItem.getLocalID()));
			
			//Checking if the item is a message
			if(isMessage) {
				ConversationManager.MessageInfo messageInfo = (ConversationManager.MessageInfo) conversationItem;
				//Playing the message's effect if it hasn't been viewed yet
				if(messageInfo.getSendStyle() != null && !messageInfo.getSendStyleViewed()) {
					messageInfo.setSendStyleViewed(true);
					messageInfo.playEffect((ConversationManager.MessageInfo.ViewHolder) holder);
					/* if(Constants.validateScreenEffect(messageInfo.getSendStyle())) playScreenEffect(messageInfo.getSendStyle());
					else messageInfo.playEffect(); */
					new TaskMarkMessageSendStyleViewed().execute(messageInfo);
				}
			}
		}
		
		@Override
		public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
			//Clearing the view's animation
			holder.itemView.clearAnimation();
		}
		
		@Override
		public int getItemCount() {
			if(conversationItems == null) return 0;
			int size = conversationItems.size();
			if(viewModel.isProgressiveLoadInProgress()) size += 1;
			return size;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(conversationItems == null) return 0;
			if(isViewLoadingSpinner(position)) return itemTypeLoadingBar;
			return conversationItems.get(position - (viewModel.isProgressiveLoadInProgress() ? 1 : 0)).getItemViewType();
		}
		
		@Override
		public long getItemId(int position) {
			if(conversationItems == null) return 0;
			if(isViewLoadingSpinner(position)) return -1;
			return conversationItems.get(position - (viewModel.isProgressiveLoadInProgress() ? 1 : 0)).getLocalID();
		}
		
		private boolean isViewLoadingSpinner(int position) {
			return viewModel.isProgressiveLoadInProgress() && position == 0;
		}
		
		final class PoolSource {
			static final int poolSize = 12;
			
			ConversationManager.MessageComponent.ViewHolder getComponent(ConversationManager.MessageComponent<ConversationManager.MessageComponent.ViewHolder> component, ViewGroup parent, Context context) {
				Pools.SimplePool<ConversationManager.MessageComponent.ViewHolder> pool = (Pools.SimplePool<ConversationManager.MessageComponent.ViewHolder>) componentPoolList.get(component.getItemViewType());
				if(pool == null) return component.createViewHolder(context, parent);
				else {
					ConversationManager.MessageComponent.ViewHolder viewHolder = pool.acquire();
					return viewHolder == null ? component.createViewHolder(context, parent) : viewHolder;
				}
			}
			
			void releaseComponent(int componentViewType, ConversationManager.MessageComponent.ViewHolder viewHolder) {
				Pools.SimplePool<ConversationManager.MessageComponent.ViewHolder> pool = (Pools.SimplePool<ConversationManager.MessageComponent.ViewHolder>) componentPoolList.get(componentViewType);
				if(pool == null) {
					pool = new Pools.SimplePool<>(poolSize);
					componentPoolList.put(componentViewType, pool);
				}
				pool.release(viewHolder);
			}
		}
		
		/* void scrollNotifyItemInserted(int position) {
			//Returning if the list is empty
			if(conversationItems.isEmpty()) return;
			
			//Getting if the list is currently scrolled to the bottom
			boolean scrolledToBottom = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition() == getItemCount() - 2; //-2 because the item was already added to the list
			
			//Calling the original method
			notifyItemInserted(position);
			
			//Checking if the list is scrolled to the end and the new item is at the bottom
			if(scrolledToBottom && position == getItemCount() - 1) {
				//Scrolling to the bottom
				recyclerView.smoothScrollToPosition(getItemCount() - 1);
			}
		} */
		
		boolean isScrolledToBottom() {
			return ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition() == getItemCount() - 1;
		}
		
		boolean isDirectlyBelowFrame(int index) {
			return ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition() + 1 == index;
		}
		
		void scrollToBottom() {
			//Returning if the list has already been scrolled to the bottom
			if(isScrolledToBottom()) return;
			
			//Scrolling to the bottom
			recyclerView.smoothScrollToPosition(getItemCount() - 1);
		}
	}
	
	private abstract class AttachmentsRecyclerAdapter<VH extends AttachmentTileViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the reference values
		private static final int itemTypeActionButton = 0;
		private static final int itemTypeContent = 1;
		private static final int itemTypeOverflowButton = 2;
		
		static final int payloadUpdateIndex = 0;
		static final int payloadUpdateSelection = 1;
		
		//Creating the list values
		private List<SimpleAttachmentInfo> fileList;
		
		AttachmentsRecyclerAdapter(List<SimpleAttachmentInfo> list) {
			fileList = list;
		}
		
		abstract boolean usesActionButton();
		int getActionButtonText() {
			return -1;
		}
		int getActionButtonDrawable() {
			return -1;
		}
		
		void onActionButtonClick() {}
		
		void onOverflowButtonClick() {}
		
		@Override
		public int getItemCount() {
			int customButtonCount = usesActionButton() ? 2 : 1;
			return fileList == null ? customButtonCount : fileList.size() + customButtonCount;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(usesActionButton() && position == 0) return itemTypeActionButton;
			else {
				if(position + 1 == getItemCount()) return itemTypeOverflowButton;
				else return itemTypeContent;
			}
		}
		
		SimpleAttachmentInfo getItemAt(int index) {
			if(usesActionButton()) index--;
			if(index < 0 || index >= fileList.size()) return null;
			return fileList.get(index);
		}
		
		void setList(List<SimpleAttachmentInfo> list) {
			fileList = list;
			notifyDataSetChanged();
		}
		
		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			switch(viewType) {
				case itemTypeActionButton: {
					View actionButton = getLayoutInflater().inflate(R.layout.listitem_attachment_actiontile, parent, false);
					TextView label = actionButton.findViewById(R.id.label);
					label.setText(getActionButtonText());
					label.setCompoundDrawablesWithIntrinsicBounds(0, getActionButtonDrawable(), 0, 0);
					actionButton.setOnClickListener(view -> onActionButtonClick());
					return new ViewHolderImpl(actionButton);
				}
				case itemTypeOverflowButton: {
					View overflowButton = getLayoutInflater().inflate(R.layout.listitem_attachment_overflow, parent, false);
					overflowButton.setOnClickListener(view -> onOverflowButtonClick());
					return new ViewHolderImpl(overflowButton);
				}
				case itemTypeContent: {
					return createContentViewHolder(parent);
				}
				default:
					throw new IllegalArgumentException("Invalid view type received: " + viewType);
			}
		}
		
		abstract VH createContentViewHolder(@NonNull ViewGroup parent);
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
			//Filtering out non-content items
			if(getItemViewType(position) != itemTypeContent) return;
			
			//Getting the item
			SimpleAttachmentInfo item = getItemAt(position);
			
			//Checking if the item is invalid
			if(item == null) {
				//Removing the click listener
				viewHolder.itemView.setOnClickListener(null);
				
				//Returning
				return;
			}
			
			//Setting the adapter information
			item.setAdapterInformation(this, position);
			
			//Binding the content view
			int draftIndex = getDraftItemIndex(item);
			bindContentViewHolder((VH) viewHolder, item, draftIndex);
			
			//Assigning the click listener
			assignItemClickListener((AttachmentTileViewHolder) viewHolder, item, draftIndex);
		}
		
		abstract void bindContentViewHolder(VH viewHolder, SimpleAttachmentInfo item, int draftIndex);
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
			//Filtering out non-content items
			if(getItemViewType(position) != itemTypeContent) return;
			
			//Ignoring the request if the payloads are empty
			if(payloads.isEmpty()) {
				super.onBindViewHolder(holder, position, payloads);
				return;
			}
			
			for(Object objectPayload : payloads) {
				int payload = (int) objectPayload;
				
				switch(payload) {
					case payloadUpdateIndex: {
						if(!(holder instanceof AttachmentTileViewHolder)) break;
						
						AttachmentTileViewHolder tileHolder = (AttachmentTileViewHolder) holder;
						if(tileHolder.labelSelection == null) break;
						
						//Getting the item information
						SimpleAttachmentInfo item = getItemAt(position);
						int itemIndex;
						if(item != null && (itemIndex = getDraftItemIndex(item)) != -1) {
							//Setting the index
							tileHolder.labelSelection.setText(Constants.intToFormattedString(getResources(), itemIndex + 1));
						}
						
						break;
					}
					case payloadUpdateSelection: {
						if(!(holder instanceof AttachmentTileViewHolder)) break;
						
						AttachmentTileViewHolder tileHolder = (AttachmentTileViewHolder) holder;
						if(tileHolder.labelSelection == null) break;
						
						//Getting the item information
						SimpleAttachmentInfo item = getItemAt(position);
						if(item == null) break;
						
						//Setting the selection
						int draftIndex = getDraftItemIndex(item);
						if(draftIndex != -1) tileHolder.setSelected(true, draftIndex + 1);
						else tileHolder.setDeselected(true);
						
						//Updating the click listener
						assignItemClickListener((AttachmentTileViewHolder) holder, item, draftIndex);
						
						break;
					}
				}
			}
		}
		
		private void assignItemClickListener(AttachmentTileViewHolder viewHolder, SimpleAttachmentInfo item, int draftIndex) {
			viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
				private int newDraftIndex;
				{
					newDraftIndex = draftIndex;
				}
				
				@Override
				public void onClick(View view) {
					//Checking if the item is not selected
					if(newDraftIndex == -1) {
						//Adding the item
						newDraftIndex = queueAttachment(item, getTileHelper(), false);
						
						//Returning if the item was not added successfully
						if(newDraftIndex == -1) return;
						
						//Showing the item's selection indicator
						viewHolder.setSelected(true, newDraftIndex + 1);
					} else {
						//Removing the item
						dequeueAttachment(item, false, true);
						newDraftIndex = -1;
						
						//Setting the selection
						viewHolder.setDeselected(true);
						
						//Updating the items
						recalculateIndices();
					}
				}
			});
		}
		
		void recalculateIndices() {
			notifyItemRangeChanged(0, fileList.size(), payloadUpdateIndex);
		}
		
		void linkToQueue(List<QueuedFileInfo> queueList) {
			//Iterating over the queue and item lists
			for(QueuedFileInfo queuedItem : queueList) for(ListIterator<SimpleAttachmentInfo> loadedIterator = fileList.listIterator(); loadedIterator.hasNext();) {
				//Getting the item information
				int loadedIndex = loadedIterator.nextIndex();
				if(usesActionButton()) loadedIndex += 1; //The action button takes up the first slot at the start
				SimpleAttachmentInfo loadedItem = loadedIterator.next();
				
				//Skipping the remainder of the iteration if the items don't match
				if(!loadedItem.compare(queuedItem.getItem())) continue;
				
				//Updating the items' adapter information
				queuedItem.getItem().setAdapterInformation(this, loadedIndex);
				loadedItem.setAdapterInformation(this, loadedIndex);
			}
		}
		
		/* private int getItemIndex(SimpleAttachmentInfo item) {
			int index = fileList.indexOf(item);
			if(usesActionButton()) index -= 1;
			return index;
		} */
		
		abstract AttachmentTileHelper<?> getTileHelper();
		
		private class ViewHolderImpl extends RecyclerView.ViewHolder {
			ViewHolderImpl(View itemView) {
				super(itemView);
			}
		}
	}
	
	private ValueAnimator currentListAttachmentQueueValueAnimator = null;
	int queueAttachment(SimpleAttachmentInfo item, AttachmentTileHelper<?> tileHelper, boolean updateListing) {
		//Checking if there is no more room for new attachments
		if(viewModel.draftQueueList.size() >= draftCountLimit) {
			//Displaying a toast message
			Toast.makeText(this, R.string.message_draft_limitreached, Toast.LENGTH_SHORT).show();
			
			//Returning
			return -1;
		}
		
		//Getting the connection service
		ConnectionService service = ConnectionService.getInstance();
		if(service == null) {
			//Starting the service
			((MainApplication) getApplication()).startConnectionService();
			
			return -1;
		}
		
		//Adding the item
		boolean listStartedEmpty = viewModel.draftQueueList.isEmpty();
		
		QueuedFileInfo draft = new QueuedFileInfo(tileHelper, item);
		viewModel.draftQueueList.add(draft);
		int draftIndex = viewModel.draftQueueList.size() - 1;
		listAttachmentQueue.getAdapter().notifyItemInserted(draftIndex);
		
		//Updating the listing
		if(updateListing && item.file != null && item.getListAdapter() != null)
			item.getListAdapter().notifyItemChanged(item.getListIndex(), AttachmentsRecyclerAdapter.payloadUpdateSelection);
		
		//Recording the current update time
		long updateTime = System.currentTimeMillis();
		
		//Creating the processing request
		ConnectionService.FilePushRequest request = item.getFile() != null ?
				new ConnectionService.FilePushRequest(item.getFile(), item.getFileType(), item.getFileName(), item.getModificationDate(), viewModel.conversationInfo, -1, -1, ConnectionService.FilePushRequest.stateLinked, updateTime, false) :
				new ConnectionService.FilePushRequest(item.getUri(), item.getFileType(), item.getFileName(), viewModel.conversationInfo, -1, -1, ConnectionService.FilePushRequest.stateLinked, updateTime, false);
		request.getCallbacks().onFail = result -> {
			//Dequeuing the attachment
			dequeueAttachment(item, true, false);
			
			//TODO notifying the user
		};
		request.getCallbacks().onDraftPreparationFinished = (file, draftFile) -> {
			//Setting the draft file
			draft.setDraftFile(draftFile);
			
			//Adding the draft file to the conversation in memory
			if(viewModel.conversationInfo != null) viewModel.conversationInfo.addDraftFileUpdate(Messaging.this, draftFile, updateTime);
			
			//Updating the attachment
			listAttachmentQueue.getAdapter().notifyItemChanged(viewModel.draftQueueList.indexOf(draft), AttachmentsQueueRecyclerAdapter.payloadUpdateState);
		};
		draft.setFilePushRequest(request);
		
		//Adding the processing request
		service.addFileProcessingRequest(request);
		
		//Animating the list
		if(listStartedEmpty) {
			if(currentListAttachmentQueueValueAnimator != null) currentListAttachmentQueueValueAnimator.cancel();
			
			listAttachmentQueue.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			ValueAnimator anim = ValueAnimator.ofInt(0, listAttachmentQueue.getMeasuredHeight());
			anim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					listAttachmentQueue.setVisibility(View.VISIBLE);
					listAttachmentQueue.getLayoutParams().height = 0;
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					listAttachmentQueue.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
					listAttachmentQueue.requestLayout();
					//for(int iChild = 0; iChild < listAttachmentQueue.getChildCount(); iChild++) listAttachmentQueue.getChildAt(iChild).invalidate();
					//Constants.recursiveInvalidate(listAttachmentQueue);
				}
			});
			anim.addUpdateListener(valueAnimator -> {
				listAttachmentQueue.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
				listAttachmentQueue.requestLayout();
				//listAttachmentQueue.invalidate();
				//for(int iChild = 0; iChild < listAttachmentQueue.getChildCount(); iChild++) listAttachmentQueue.getChildAt(iChild).invalidate();
				//Constants.recursiveInvalidate(listAttachmentQueue);
			});
			anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
			anim.start();
			currentListAttachmentQueueValueAnimator = anim;
		}
		
		//Updating the send button
		updateSendButton(false);
		
		//Returning the index
		return draftIndex;
	}
	
	int dequeueAttachment(SimpleAttachmentInfo item, boolean updateListing, boolean updateElsewhere) {
		int draftIndex = -1;
		
		//Removing the item
		QueuedFileInfo queuedItem = null;
		for(ListIterator<QueuedFileInfo> iterator = viewModel.draftQueueList.listIterator(); iterator.hasNext();) {
			draftIndex = iterator.nextIndex();
			queuedItem = iterator.next();
			if(queuedItem.getItem().compare(item)) {
				iterator.remove();
				listAttachmentQueue.getAdapter().notifyItemRemoved(draftIndex);
				break;
			}
		}
		
		//Returning if no item was found
		if(draftIndex == -1) return -1;
		
		//Removing the draft from the conversation and from disk
		if(updateElsewhere && queuedItem.getDraftFile() != null) {
			//Getting the connection service
			ConnectionService service = ConnectionService.getInstance();
			if(service == null) {
				//Starting the service
				((MainApplication) getApplication()).startConnectionService();
				
				return -1;
			}
			
			//Adding the request
			long updateTime = System.currentTimeMillis();
			ConnectionService.FileProcessingRequest request = new ConnectionService.FileRemovalRequest(queuedItem.getDraftFile(), updateTime);
			final QueuedFileInfo finalQueuedItem = queuedItem;
			request.getCallbacks().onRemovalFinish = () -> {
				//Removing the draft from the conversation in memory
				if(viewModel.conversationInfo != null) viewModel.conversationInfo.removeDraftFileUpdate(Messaging.this, finalQueuedItem.getDraftFile(), updateTime);
			};
			service.addFileProcessingRequest(request);
		}
		
		//Updating the listings
		if(updateListing) {
			//Updating the relevant adapters
			List<AttachmentsRecyclerAdapter<?>> adapterList = new ArrayList<>();
			for(int i = draftIndex; i < viewModel.draftQueueList.size(); i++) {
				QueuedFileInfo listedItem = viewModel.draftQueueList.get(i);
				if(listedItem.getItem().getListAdapter() == null || adapterList.contains(listedItem.getItem().getListAdapter())) continue;
				adapterList.add(listedItem.getItem().getListAdapter());
			}
			for(AttachmentsRecyclerAdapter<?> adapter : adapterList) adapter.recalculateIndices();
			
			//Updating the item selection
			if(item.getFile() != null && item.getListAdapter() != null) item.getListAdapter().notifyItemChanged(item.getListIndex(), AttachmentsRecyclerAdapter.payloadUpdateSelection);
		}
		
		//Animating the list
		if(viewModel.draftQueueList.isEmpty()) {
			if(currentListAttachmentQueueValueAnimator != null) currentListAttachmentQueueValueAnimator.cancel();
			//listAttachmentQueue.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			
			ValueAnimator anim = ValueAnimator.ofInt(listAttachmentQueue.getHeight(), 0);
			anim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					listAttachmentQueue.setVisibility(View.GONE);
					//listAttachmentQueue.requestLayout();
				}
			});
			anim.addUpdateListener(valueAnimator -> {
				listAttachmentQueue.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
				listAttachmentQueue.requestLayout();
				//listAttachmentQueue.invalidate();
			});
			anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
			anim.start();
			currentListAttachmentQueueValueAnimator = anim;
		}
		
		//Updating the send button
		updateSendButton(false);
		
		//Returning the removed item index
		return draftIndex;
	}
	
	private class AttachmentsQueueRecyclerAdapter extends RecyclerView.Adapter<AttachmentsQueueRecyclerAdapter.QueueTileViewHolder> {
		//Creating the reference values
		static final int payloadUpdateState = 0;
		
		//Creating the list value
		private List<QueuedFileInfo> itemList;
		
		AttachmentsQueueRecyclerAdapter(List<QueuedFileInfo> list) {
			itemList = list;
		}
		
		@NonNull
		@Override
		public QueueTileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			//Inflating the layout
			View layout = getLayoutInflater().inflate(R.layout.listitem_attachment_queuetile, parent, false);
			FrameLayout container = layout.findViewById(R.id.container);
			
			//Creating the tile view
			RecyclerView.ViewHolder tileViewHolder;
			switch(viewType) {
				case AttachmentTileHelper.viewTypeMedia:
					tileViewHolder = attachmentsMediaTileHelper.createViewHolder(container);
					break;
				case AttachmentTileHelper.viewTypeDocument:
					tileViewHolder = attachmentsDocumentTileHelper.createViewHolder(container);
					break;
				case AttachmentTileHelper.viewTypeAudio:
					tileViewHolder = attachmentsAudioTileHelper.createViewHolder(container);
					break;
				default:
					throw new IllegalArgumentException("Invalid view type received: " + viewType);
			}
			
			//Creating the queue tile
			return new QueueTileViewHolder(layout, tileViewHolder);
		}
		
		@Override
		public void onBindViewHolder(@NonNull QueueTileViewHolder holder, int position) {
			//Binding the tile view
			QueuedFileInfo fileInfo = itemList.get(position);
			fileInfo.getTileHelper().bindView(holder.contentViewHolder, fileInfo.item);
			
			//Hooking up the remove button
			holder.buttonRemove.setOnClickListener(view -> dequeueAttachment(fileInfo.item, true, true));
			
			//Setting the view state
			holder.setAppearenceState(fileInfo.getFilePushRequest() == null || !fileInfo.getFilePushRequest().isInProcessing(), false);
		}
		
		@Override
		public void onBindViewHolder(@NonNull QueueTileViewHolder holder, int position, @NonNull List<Object> payloads) {
			super.onBindViewHolder(holder, position, payloads);
			
			//Ignoring the request if the payloads are empty
			if(payloads.isEmpty()) {
				super.onBindViewHolder(holder, position, payloads);
				return;
			}
			
			for(Object objectPayload : payloads) {
				int payload = (int) objectPayload;
				
				switch(payload) {
					case payloadUpdateState: {
						//Getting the item information
						QueuedFileInfo fileInfo = itemList.get(position);
						
						//Updating the view
						holder.setAppearenceState(fileInfo.getFilePushRequest() == null || !fileInfo.getFilePushRequest().isInProcessing(), true);
						
						break;
					}
				}
			}
		}
		
		@Override
		public int getItemCount() {
			return itemList.size();
		}
		
		@Override
		public int getItemViewType(int position) {
			return itemList.get(position).getTileHelper().getViewType();
		}
		
		class QueueTileViewHolder extends RecyclerView.ViewHolder {
			private final ImageButton buttonRemove;
			private final FrameLayout container;
			private final RecyclerView.ViewHolder contentViewHolder;
			
			QueueTileViewHolder(View itemView, RecyclerView.ViewHolder contentViewHolder) {
				super(itemView);
				
				//Setting the content view holder
				this.contentViewHolder = contentViewHolder;
				
				//Getting the views
				buttonRemove = itemView.findViewById(R.id.button_remove);
				container = itemView.findViewById(R.id.container);
				
				//Adding the child view
				container.addView(contentViewHolder.itemView);
				
				//Scaling the child view
				float scale = getResources().getDimension(R.dimen.queuetile_size) / getResources().getDimension(R.dimen.contenttile_size);
				contentViewHolder.itemView.setPivotX(0.5F);
				contentViewHolder.itemView.setPivotY(0.5F);
				contentViewHolder.itemView.setScaleX(scale);
				contentViewHolder.itemView.setScaleY(scale);
			}
			
			void setAppearenceState(boolean state, boolean animate) {
				if(animate) TransitionManager.beginDelayedTransition((ViewGroup) itemView);
				buttonRemove.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
				container.setAlpha(state ? 1 : 0.5F);
			}
		}
	}
	
	private class QueuedFileInfo {
		private final AttachmentTileHelper tileHelper;
		private final SimpleAttachmentInfo item;
		private ConnectionService.FilePushRequest filePushRequest;
		private ConversationManager.DraftFile draftFile;
		
		QueuedFileInfo(AttachmentTileHelper tileHelper, SimpleAttachmentInfo item) {
			this.tileHelper = tileHelper;
			this.item = item;
		}
		
		QueuedFileInfo(ConversationManager.DraftFile draft) {
			tileHelper = findAppropriateTileHelper(draft.getFileType());
			item = new SimpleAttachmentInfo(draft);
			draftFile = draft;
		}
		
		AttachmentTileHelper getTileHelper() {
			return tileHelper;
		}
		
		SimpleAttachmentInfo getItem() {
			return item;
		}
		
		ConnectionService.FilePushRequest getFilePushRequest() {
			return filePushRequest;
		}
		
		void setFilePushRequest(ConnectionService.FilePushRequest request) {
			filePushRequest = request;
		}
		
		ConversationManager.DraftFile getDraftFile() {
			return draftFile;
		}
		
		void setDraftFile(ConversationManager.DraftFile draftFile) {
			this.draftFile = draftFile;
		}
	}
	
	private abstract class AttachmentTileHelper<VH extends RecyclerView.ViewHolder> {
		//Creating the reference values
		static final int viewTypeMedia = 0;
		static final int viewTypeDocument = 1;
		static final int viewTypeAudio = 2;
		
		abstract VH createViewHolder(ViewGroup parent);
		abstract void bindView(VH viewHolder, SimpleAttachmentInfo item);
		
		abstract int getViewType();
	}
	
	AttachmentTileHelper<?> findAppropriateTileHelper(String mimeType) {
		if(mimeType == null) return attachmentsDocumentTileHelper;
		
		{
			String mimeTypeGeneral = mimeType.split("/")[0];
			if(mimeTypeGeneral.equals("image") || mimeTypeGeneral.equals("video")) return attachmentsMediaTileHelper;
			else if(mimeTypeGeneral.equals("audio")) return attachmentsAudioTileHelper;
		}
		
		return attachmentsDocumentTileHelper;
	}
	
	private abstract class AttachmentTileViewHolder extends RecyclerView.ViewHolder {
		//Creating the reference values
		private static final float selectedScale = 0.85F;
		
		//Creating the view values
		private ViewGroup groupSelection;
		private TextView labelSelection;
		
		AttachmentTileViewHolder(View itemView) {
			super(itemView);
		}
		
		void setSelected(boolean animate, int index) {
			//Returning if the view state is already selected
			if(groupSelection != null && groupSelection.getVisibility() == View.VISIBLE) return;
			
			//Inflating the view if it hasn't yet been
			if(groupSelection == null) {
				groupSelection = (ViewGroup) ((ViewStub) itemView.findViewById(R.id.viewstub_selection)).inflate();
				labelSelection = groupSelection.findViewById(R.id.label_selectionindex);
			}
			
			//Showing the view
			if(animate) {
				int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
				groupSelection.animate().withStartAction(() -> groupSelection.setVisibility(View.VISIBLE)).alpha(1).setDuration(duration).start();
				{
					ValueAnimator animator = ValueAnimator.ofFloat(itemView.getScaleX(), selectedScale);
					animator.setDuration(duration);
					animator.addUpdateListener(animation -> {
						float value = (float) animation.getAnimatedValue();
						itemView.setScaleX(value);
						itemView.setScaleY(value);
					});
					animator.start();
				}
				/* {
					ScaleAnimation animation = new ScaleAnimation(itemView.getScaleX(), selectedScale, itemView.getScaleY(), selectedScale, 0.5F, 0.5F);
					animation.setDuration(duration);
					animation.setFillAfter(true);
					animation.setFillEnabled(true);
					animation.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							itemView.setScaleX(0.5F);
							itemView.setScaleY(0.5F);
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {}
					});
					itemView.startAnimation(animation);
				} */
				/* itemView.animate().scaleX(selectedScale).scaleY(selectedScale).withEndAction(() -> {
					itemView.setScaleX(selectedScale);
					itemView.setScaleY(selectedScale);
				}).setDuration(duration).start(); */
			} else {
				groupSelection.setVisibility(View.VISIBLE);
				groupSelection.setAlpha(1);
				itemView.setScaleX(selectedScale);
				itemView.setScaleY(selectedScale);
			}
			
			labelSelection.setText(Constants.intToFormattedString(getResources(), index));
		}
		
		void setDeselected(boolean animate) {
			//Returning if the view state is already deselected
			if(groupSelection == null || groupSelection.getVisibility() == View.GONE) return;
			
			//Hiding the view
			if(animate) {
				int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
				groupSelection.animate().withEndAction(() -> groupSelection.setVisibility(View.GONE)).alpha(0).setDuration(duration).start();
				{
					ValueAnimator animator = ValueAnimator.ofFloat(itemView.getScaleX(), 1);
					animator.setDuration(duration);
					animator.addUpdateListener(animation -> {
						float value = (float) animation.getAnimatedValue();
						itemView.setScaleX(value);
						itemView.setScaleY(value);
					});
					animator.start();
				}
				/* {
					ScaleAnimation animation = new ScaleAnimation(itemView.getScaleX(), 1, itemView.getScaleY(), 1, 0.5F, 0.5F);
					animation.setDuration(duration);
					animation.setFillAfter(true);
					animation.setFillEnabled(true);
					itemView.startAnimation(animation);
				} */
				/* itemView.animate().scaleX(selectedScale * 2F).scaleY(selectedScale * 2F).withEndAction(() -> {
					itemView.setScaleX(selectedScale * 2F);
					itemView.setScaleY(selectedScale * 2F);
				}).setDuration(duration).start(); */
			} else {
				groupSelection.setVisibility(View.GONE);
				groupSelection.setAlpha(1);
				itemView.setScaleX(1);
				itemView.setScaleY(1);
			}
		}
	}
	
	private class AttachmentsGalleryRecyclerAdapter extends AttachmentsRecyclerAdapter<AttachmentsMediaTileViewHolder> {
		//Creating the reference values
		private final AttachmentTileHelper<AttachmentsMediaTileViewHolder> tileHelper = attachmentsMediaTileHelper;
		
		AttachmentsGalleryRecyclerAdapter(List<SimpleAttachmentInfo> list) {
			super(list);
		}
		
		@Override
		boolean usesActionButton() {
			return true;
		}
		
		@Override
		int getActionButtonText() {
			return R.string.part_camera;
		}
		
		@Override
		int getActionButtonDrawable() {
			return R.drawable.camera;
		}
		
		@Override
		void onActionButtonClick() {
			requestTakePicture();
		}
		
		@Override
		void onOverflowButtonClick() {
			requestGalleryFile();
		}
		
		@Override
		AttachmentsMediaTileViewHolder createContentViewHolder(@NonNull ViewGroup parent) {
			return tileHelper.createViewHolder(parent);
		}
		
		@Override
		void bindContentViewHolder(AttachmentsMediaTileViewHolder viewHolder, SimpleAttachmentInfo file, int draftIndex) {
			//Binding the view through the tile helper
			tileHelper.bindView(viewHolder, file);
			
			//Setting the selection state
			if(draftIndex == -1) viewHolder.setDeselected(false);
			else viewHolder.setSelected(false, draftIndex + 1);
		}
		
		@Override
		AttachmentTileHelper<?> getTileHelper() {
			return tileHelper;
		}
	}
	
	/**
	 * Retrieves the index of the selected attachment item
	 * @param item the selected item
	 * @return the index of the selected
	 */
	private int getDraftItemIndex(SimpleAttachmentInfo item) {
		for(int i = 0; i < viewModel.draftQueueList.size(); i++) {
			if(viewModel.draftQueueList.get(i).getItem().compare(item)) return i;
		}
		return -1;
	}
	
	private class AttachmentsMediaTileViewHolder extends AttachmentTileViewHolder {
		//Creating the view values
		private final ImageView imageThumbnail;
		private final ImageView imageFlagGIF;
		
		AttachmentsMediaTileViewHolder(View itemView) {
			super(itemView);
			imageThumbnail = itemView.findViewById(R.id.image);
			imageFlagGIF = itemView.findViewById(R.id.image_flag_gif);
		}
	}
	
	private final AttachmentTileHelper<AttachmentsMediaTileViewHolder> attachmentsMediaTileHelper = new AttachmentTileHelper<AttachmentsMediaTileViewHolder>() {
		@Override
		AttachmentsMediaTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsMediaTileViewHolder(getLayoutInflater().inflate(R.layout.listitem_attachment_mediatile, parent, false));
		}
		
		@Override
		void bindView(AttachmentsMediaTileViewHolder viewHolder, SimpleAttachmentInfo item) {
			//Returning if the item is invalid
			if(item == null) return;
			
			//Returning if the activity is finishing
			//if(isFinishing() || isDestroyed()) return;
			
			//Setting the image thumbnail
			Glide.with(Messaging.this)
					.load(item.getFile() != null ? item.getFile() : item.getUri())
					.apply(RequestOptions.centerCropTransform())
					.transition(DrawableTransitionOptions.withCrossFade())
					.into(viewHolder.imageThumbnail);
		}
		
		@Override
		int getViewType() {
			return viewTypeMedia;
		}
	};
	
	private class AttachmentsDocumentRecyclerAdapter extends AttachmentsRecyclerAdapter<AttachmentsDocumentTileViewHolder> {
		//Creating the reference values
		private final AttachmentTileHelper<AttachmentsDocumentTileViewHolder> tileHelper = attachmentsDocumentTileHelper;
		AttachmentsDocumentRecyclerAdapter(List<SimpleAttachmentInfo> list) {
			super(list);
		}
		
		@Override
		boolean usesActionButton() {
			return false;
		}
		
		@Override
		void onOverflowButtonClick() {
			requestDocumentFile();
		}
		
		@Override
		AttachmentsDocumentTileViewHolder createContentViewHolder(@NonNull ViewGroup parent) {
			return attachmentsDocumentTileHelper.createViewHolder(parent);
		}
		
		@Override
		void bindContentViewHolder(AttachmentsDocumentTileViewHolder viewHolder, SimpleAttachmentInfo document, int draftIndex) {
			tileHelper.bindView(viewHolder, document);
		}
		
		@Override
		AttachmentTileHelper<?> getTileHelper() {
			return tileHelper;
		}
	}
	
	private class AttachmentsDocumentTileViewHolder extends AttachmentTileViewHolder {
		//Creating the view values
		private TextView documentName;
		private ImageView documentIcon;
		private TextView documentSize;
		
		AttachmentsDocumentTileViewHolder(View itemView) {
			super(itemView);
			documentName = itemView.findViewById(R.id.label);
			documentIcon = itemView.findViewById(R.id.icon);
			documentSize = itemView.findViewById(R.id.label_size);
		}
	}
	
	private final AttachmentTileHelper<AttachmentsDocumentTileViewHolder> attachmentsDocumentTileHelper = new AttachmentTileHelper<AttachmentsDocumentTileViewHolder>() {
		@Override
		AttachmentsDocumentTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsDocumentTileViewHolder(getLayoutInflater().inflate(R.layout.listitem_attachment_documenttile, parent, false));
		}
		
		@Override
		void bindView(AttachmentsDocumentTileViewHolder viewHolder, SimpleAttachmentInfo item) {
			//Returning if the file is invalid
			if(item == null) return;
			
			//Getting the type-based details
			int iconResource = R.drawable.file;
			int viewColorBG = R.color.tile_grey_bg;
			int viewColorFG = R.color.tile_grey_fg;
			if(item.getFileType() != null) {
				switch(item.getFileType()) {
					default:
						if(item.getFileType().split("/")[0].startsWith("text")) {
							iconResource = R.drawable.file_document;
							viewColorBG = R.color.tile_indigo_bg;
							viewColorFG = R.color.tile_indigo_fg;
						}
						break;
					case "application/zip":
					case "application/x-tar":
					case "application/x-rar-compressed":
					case "application/x-7z-compressed":
					case "application/x-bzip":
					case "application/x-bzip2":
						iconResource = R.drawable.file_zip;
						viewColorBG = R.color.tile_brown_bg;
						viewColorFG = R.color.tile_brown_fg;
						break;
					case "application/pdf":
						iconResource = R.drawable.file_pdf;
						viewColorBG = R.color.tile_red_bg;
						viewColorFG = R.color.tile_red_fg;
						break;
					case "text/xml":
					case "application/xml":
					case "text/html":
						iconResource = R.drawable.file_xml;
						viewColorBG = R.color.tile_orange_bg;
						viewColorFG = R.color.tile_orange_fg;
						break;
					case "text/vcard":
						iconResource = R.drawable.file_user;
						viewColorBG = R.color.tile_cyan_bg;
						viewColorFG = R.color.tile_cyan_fg;
						break;
					case "application/msword":
					case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
						iconResource = R.drawable.file_msword;
						viewColorBG = R.color.tile_blue_bg;
						viewColorFG = R.color.tile_blue_fg;
						break;
					case "application/vnd.ms-excel":
					case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
						iconResource = R.drawable.file_msexcel;
						viewColorBG = R.color.tile_green_bg;
						viewColorFG = R.color.tile_green_fg;
						break;
					case "application/vnd.ms-powerpoint":
					case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
						iconResource = R.drawable.file_mspowerpoint;
						viewColorBG = R.color.tile_yellow_bg;
						viewColorFG = R.color.tile_yellow_fg;
						break;
				}
			}
			
			//Resolving the color resources
			viewColorBG = getResources().getColor(viewColorBG, null);
			viewColorFG = getResources().getColor(viewColorFG, null);
			if(Constants.isNightMode(getResources())) {
				int temp = viewColorBG;
				viewColorBG = viewColorFG;
				viewColorFG = temp;
			}
			
			//Filling in the view data
			viewHolder.documentName.setText(item.getFileName());
			viewHolder.documentName.setTextColor(viewColorFG);
			
			viewHolder.documentIcon.setImageResource(iconResource);
			viewHolder.documentIcon.setImageTintList(ColorStateList.valueOf(viewColorFG));
			
			viewHolder.documentSize.setText(Constants.humanReadableByteCount(item.getFileSize(), true));
			viewHolder.documentSize.setTextColor(viewColorFG);
			
			viewHolder.itemView.setBackgroundTintList(ColorStateList.valueOf(viewColorBG));
		}
		
		@Override
		int getViewType() {
			return viewTypeDocument;
		}
	};
	
	private class AttachmentsAudioTileViewHolder extends AttachmentTileViewHolder {
		//Creating the reference values
		static final int resourceDrawablePlay = R.drawable.play;
		static final int resourceDrawablePause = R.drawable.pause;
		
		//Creating the view values
		private ProgressBar progressBar;
		private ImageView imagePlay;
		//private TextView labelDuration;
		
		AttachmentsAudioTileViewHolder(View itemView) {
			super(itemView);
			progressBar = itemView.findViewById(R.id.progress);
			imagePlay = itemView.findViewById(R.id.image_play);
			//labelDuration = itemView.findViewById(R.id.label_duration);
		}
	}
	
	private final AttachmentTileHelper<AttachmentsAudioTileViewHolder> attachmentsAudioTileHelper = new AttachmentTileHelper<AttachmentsAudioTileViewHolder>() {
		@Override
		AttachmentsAudioTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsAudioTileViewHolder(getLayoutInflater().inflate(R.layout.listitem_attachment_audiotile, parent, false));
		}
		
		@Override
		void bindView(AttachmentsAudioTileViewHolder viewHolder, SimpleAttachmentInfo item) {
			//Returning if the item is invalid
			if(item == null) return;
			
			//Getting the extension
			SimpleAttachmentInfo.AudioExtension extension = (SimpleAttachmentInfo.AudioExtension) item.getExtension();
			
			//Binding the view
			extension.updateViewPlaying(viewHolder);
			extension.updateViewProgress(viewHolder);
			
			//Creating the view holder source
			Constants.ViewHolderSource<AttachmentsAudioTileViewHolder> viewHolderSource = () -> {
				if(listAttachmentQueue == null) return null;
				int itemIndex = findAttachmentInQueue(item);
				if(itemIndex == -1) return null;
				return (AttachmentsAudioTileViewHolder) ((AttachmentsQueueRecyclerAdapter.QueueTileViewHolder) listAttachmentQueue.findViewHolderForAdapterPosition(itemIndex)).contentViewHolder;
			};
			
			//Finding the queued file info
			QueuedFileInfo queuedInfo = null;
			for(QueuedFileInfo allQueued : viewModel.draftQueueList) {
				if(allQueued.getItem() != item) continue;
				queuedInfo = allQueued;
				break;
			}
			if(queuedInfo == null) return;
			
			//Setting the click listener
			final QueuedFileInfo finalQueuedFileInfo = queuedInfo;
			viewHolder.itemView.setOnClickListener(view -> extension.play(viewModel.audioPlaybackManager, finalQueuedFileInfo, viewHolderSource));
		}
		
		private int findAttachmentInQueue(SimpleAttachmentInfo item) {
			int queuedIndex;
			QueuedFileInfo queuedItem;
			for(ListIterator<QueuedFileInfo> iterator = viewModel.draftQueueList.listIterator(); iterator.hasNext();) {
				queuedIndex = iterator.nextIndex();
				queuedItem = iterator.next();
				if(queuedItem.getItem() == item) return queuedIndex;
			}
			
			return -1;
		}
		
		@Override
		int getViewType() {
			return viewTypeAudio;
		}
	};
	
	private static class SimpleAttachmentInfo {
		private final File file;
		private final Uri uri;
		private final String fileType;
		private final String fileName;
		private final long fileSize;
		private final long modificationDate;
		
		private Extension extension;
		
		private AttachmentsRecyclerAdapter<?> listAdapter;
		private int listIndex;
		
		/* SimpleDraftInfo(File file, String fileType, String fileName, long fileSize) {
			this.file = file;
			this.fileType = fileType;
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.modificationDate = 0;
		} */
		
		SimpleAttachmentInfo(File file, String fileType, String fileName, long fileSize, long modificationDate) {
			this.file = file;
			this.fileType = fileType;
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.modificationDate = modificationDate;
			
			this.uri = null;
			
			assignExtension();
		}
		
		SimpleAttachmentInfo(Uri uri, String fileType, String fileName, long fileSize, long modificationDate) {
			this.uri = uri;
			this.fileType = fileType;
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.modificationDate = modificationDate;
			
			this.file = null;
			
			assignExtension();
		}
		
		SimpleAttachmentInfo(ConversationManager.DraftFile draft) {
			this(draft.getOriginalFile() != null ? draft.getOriginalFile() : draft.getFile(), draft.getFileType(), draft.getFileName(), draft.getFileSize(), draft.getModificationDate());
		}
		
		private void assignExtension() {
			if(fileType.split("/")[0].equals("audio")) extension = new AudioExtension();
			else extension = null;
			
			if(extension != null) extension.initialize();
		}
		
		File getFile() {
			return file;
		}
		
		String getFileType() {
			return fileType;
		}
		
		String getFileName() {
			return fileName;
		}
		
		long getFileSize() {
			return fileSize;
		}
		
		long getModificationDate() {
			return modificationDate;
		}
		
		Uri getUri() {
			return uri;
		}
		
		void setAdapterInformation(AttachmentsRecyclerAdapter<?> adapter, int index) {
			listAdapter = adapter;
			listIndex = index;
		}
		
		AttachmentsRecyclerAdapter<?> getListAdapter() {
			return listAdapter;
		}
		
		int getListIndex() {
			return listIndex;
		}
		
		public boolean compare(SimpleAttachmentInfo item) {
			return this.getModificationDate() == item.getModificationDate() && Objects.equals(this.getFile(), item.getFile());
		}
		
		abstract class Extension {
			void initialize() {}
		}
		
		Extension getExtension() {
			return extension;
		}
		
		class AudioExtension extends Extension {
			//Creating the attachment info values
			private long mediaDuration = -1;
			
			//Creating the playback values
			private boolean isSelected = false;
			private boolean isPlaying = false;
			private long mediaProgress = 0;
			
			@Override
			void initialize() {
				if(file != null) mediaDuration = Constants.getMediaDuration(file);
				else if(uri != null) mediaDuration = Constants.getMediaDuration(MainApplication.getInstance(), uri);
			}
			
			long getMediaDuration() {
				return mediaDuration;
			}
			
			boolean isPlaying() {
				return isPlaying;
			}
			
			long getMediaProgress() {
				return mediaProgress;
			}
			
			void play(AudioPlaybackManager playbackManager, QueuedFileInfo queuedInfo, Constants.ViewHolderSource<AttachmentsAudioTileViewHolder> viewSource) {
				//Returning if the file is invalid
				File targetFile = queuedInfo.getDraftFile().getFile();
				if(targetFile == null) return;
				
				if(isSelected) {
					//Toggling play
					playbackManager.togglePlaying();
					
					//Returning
					return;
				}
				
				//Playing the file
				playbackManager.play(targetFile, new AudioPlaybackManager.Callbacks() {
					@Override
					public void onPlay() {
						isPlaying = true;
						
						AttachmentsAudioTileViewHolder viewHolder = viewSource.get();
						if(viewHolder != null) updateViewPlaying(viewHolder);
					}
					
					@Override
					public void onProgress(long time) {
						mediaProgress = time;
						
						AttachmentsAudioTileViewHolder viewHolder = viewSource.get();
						if(viewHolder != null) updateViewProgress(viewHolder);
					}
					
					@Override
					public void onPause() {
						isPlaying = false;
						
						AttachmentsAudioTileViewHolder viewHolder = viewSource.get();
						if(viewHolder != null) updateViewPlaying(viewHolder);
					}
					
					@Override
					public void onStop() {
						isPlaying = false;
						isSelected = false;
						mediaProgress = 0;
						
						AttachmentsAudioTileViewHolder viewHolder = viewSource.get();
						if(viewHolder != null) {
							updateViewPlaying(viewHolder);
							updateViewProgress(viewHolder);
						}
					}
				});
				
				isSelected = true;
			}
			
			void updateViewPlaying(AttachmentsAudioTileViewHolder viewHolder) {
				viewHolder.imagePlay.setImageResource(isPlaying ? AttachmentsAudioTileViewHolder.resourceDrawablePause : AttachmentsAudioTileViewHolder.resourceDrawablePlay);
			}
			
			void updateViewProgress(AttachmentsAudioTileViewHolder viewHolder) {
				viewHolder.progressBar.setProgress((int) ((float) mediaProgress / (float) mediaDuration * 100F));
				//viewHolder.labelDuration.setText(Constants.getFormattedDuration((int) Math.floor(mediaProgress <= 0 ? mediaDuration / 1000L : mediaProgress / 1000L)));
			}
		}
	}
	
	private static class TaskMarkMessageSendStyleViewed extends AsyncTask<ConversationManager.MessageInfo, Void, Void> {
		@Override
		protected Void doInBackground(ConversationManager.MessageInfo... items) {
			for(ConversationManager.MessageInfo item : items) DatabaseManager.getInstance().markSendStyleViewed(item.getLocalID());
			return null;
		}
	}
	
	static class LoadingViewHolder extends RecyclerView.ViewHolder {
		LoadingViewHolder(View itemView) {
			super(itemView);
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
	
	private static class ActivityViewModel extends AndroidViewModel {
		static final byte messagesStateIdle = 0;
		static final byte messagesStateLoadingConversation = 1;
		static final byte messagesStateLoadingMessages = 2;
		static final byte messagesStateFailedConversation = 3;
		static final byte messagesStateFailedMessages = 4;
		static final byte messagesStateReady = 5;
		
		static final byte attachmentsStateIdle = 0;
		static final byte attachmentsStateLoading = 1;
		static final byte attachmentsStateLoaded = 2;
		static final byte attachmentsStateFailed = 3;
		
		static final int attachmentTypeGallery = 0;
		static final int attachmentTypeDocument = 1;
		private static final int attachmentsTileCount = 12;
		
		//Creating the state values
		private final MutableLiveData<Byte> messagesState = new MutableLiveData<>();
		
		final List<QueuedFileInfo> draftQueueList = new ArrayList<>(3);
		private static final int attachmentTypesCount = 2;
		private final byte[] attachmentStates = new byte[attachmentTypesCount];
		private final ArrayList<SimpleAttachmentInfo>[] attachmentLists = new ArrayList[attachmentTypesCount];
		private final WeakReference<AttachmentsLoadCallbacks>[] attachmentCallbacks = new WeakReference[attachmentTypesCount];
		
		boolean isAttachmentsPanelOpen = false;
		boolean isDetailsPanelOpen = false;
		
		private final MutableLiveData<Boolean> progressiveLoadInProgress = new MutableLiveData<>();
		boolean progressiveLoadReachedLimit = false;
		int lastProgressiveLoadCount = -1;
		
		int lastUnreadCount = 0;
		
		//Creating the conversation values
		private long conversationID;
		private ConversationManager.ConversationInfo conversationInfo;
		private ArrayList<ConversationManager.ConversationItem> conversationItemList;
		private ArrayList<ConversationManager.MessageInfo> conversationGhostList;
		
		//Creating the attachment values
		File targetFileIntent = null;
		File targetFileRecording = null;
		
		final AudioPlaybackManager audioPlaybackManager = new AudioPlaybackManager();
		
		MediaRecorder mediaRecorder = null;
		private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>();
		private final MutableLiveData<Integer> recordingDuration = new MutableLiveData<>();
		private final Handler recordingTimerHandler = new Handler(Looper.getMainLooper());
		private final Runnable recordingTimerHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				//Adding a second
				recordingDuration.setValue(recordingDuration.getValue() + 1);
				
				//Scheduling the next run
				recordingTimerHandler.postDelayed(this, 1000);
			}
		};
		
		ActivityViewModel(Application application, long conversationID) {
			super(application);
			
			//Setting the values
			this.conversationID = conversationID;
			
			//Loading the data
			loadConversation();
			
			//Filling the attachment lists
			Arrays.fill(attachmentStates, attachmentsStateIdle);
		}
		
		@Override
		protected void onCleared() {
			//Cleaning up the media recorder
			if(isRecording()) stopRecording(true, true);
			if(mediaRecorder != null) mediaRecorder.release();
			
			//Releasing the audio player
			audioPlaybackManager.release();
			
			//Checking if the conversation is valid
			if(conversationInfo != null) {
				//Clearing the messages
				//conversationInfo.clearMessages();
				
				//Updating the conversation's unread message count
				conversationInfo.updateUnreadStatus(MainApplication.getInstance());
				new UpdateUnreadMessageCount(MainApplication.getInstance(), conversationID, conversationInfo.getUnreadMessageCount()).execute();
			}
		}
		
		/**
		 * Loads the conversation and its messages
		 */
		@SuppressLint("StaticFieldLeak")
		void loadConversation() {
			//Updating the state
			messagesState.setValue(messagesStateLoadingConversation);
			
			//Loading the conversation
			conversationInfo = ConversationManager.findConversationInfo(conversationID);
			if(conversationInfo != null) loadMessages();
			else new AsyncTask<Void, Void, ConversationManager.ConversationInfo>() {
				@Override
				protected ConversationManager.ConversationInfo doInBackground(Void... args) {
					return DatabaseManager.getInstance().fetchConversationInfo(getApplication(), conversationID);
				}
				
				@Override
				protected void onPostExecute(ConversationManager.ConversationInfo result) {
					//Setting the state to failed if the conversation info couldn't be fetched
					if(result == null) messagesState.setValue(messagesStateFailedConversation);
					else {
						conversationInfo = result;
						loadMessages();
					}
				}
			}.execute();
		}
		
		@SuppressLint("StaticFieldLeak")
		void loadMessages() {
			//Updating the state
			messagesState.setValue(messagesStateLoadingMessages);
			
			//Checking if the conversation already has lists
			ArrayList<ConversationManager.ConversationItem> existingConversationItems = conversationInfo.getConversationItems();
			ArrayList<ConversationManager.MessageInfo> existingGhostMessages = conversationInfo.getGhostMessages();
			if(existingConversationItems != null && existingGhostMessages != null) {
				//Setting the lists
				conversationItemList = existingConversationItems;
				conversationGhostList = existingGhostMessages;
				
				//Marking all messages as read (list will always be scrolled to the bottom)
				conversationInfo.setUnreadMessageCount(0);
				
				//Setting the state
				messagesState.setValue(messagesStateReady);
			} else {
				//Loading the messages
				new AsyncTask<Void, Void, List<ConversationManager.ConversationItem>>() {
					@Override
					protected List<ConversationManager.ConversationItem> doInBackground(Void... params) {
						//Loading the conversation items
						List<ConversationManager.ConversationItem> conversationItems = DatabaseManager.getInstance().loadConversationChunk(conversationInfo, false, 0);
						
						//Setting up the conversation item relations
						ConversationManager.setupConversationItemRelations(conversationItems, conversationInfo);
						
						//Returning the conversation items
						return conversationItems;
					}
					
					@Override
					protected void onPostExecute(List<ConversationManager.ConversationItem> messages) {
						//Checking if the messages are invalid
						if(messages == null) {
							//Setting the state
							messagesState.setValue(messagesStateFailedMessages);
							
							//Returning
							return;
						}
						
						//Creating the lists
						conversationItemList = new ArrayList<>();
						conversationGhostList = new ArrayList<>();
						
						//Recording the lists in the conversation info
						conversationInfo.setConversationLists(conversationItemList, conversationGhostList);
						
						//Replacing the conversation items
						conversationInfo.replaceConversationItems(MainApplication.getInstance(), messages);
						
						//Marking all messages as read (list will always be scrolled to the bottom)
						conversationInfo.setUnreadMessageCount(0);
						
						//Setting the state
						messagesState.setValue(messagesStateReady);
					}
				}.execute();
			}
		}
		
		@SuppressLint("StaticFieldLeak")
		void loadNextChunk() {
			//Returning if the conversation isn't ready, a load is already in progress or there are no conversation items
			if(messagesState.getValue() != messagesStateReady || isProgressiveLoadInProgress() || progressiveLoadReachedLimit || conversationInfo.getConversationItems().isEmpty()) return;
			
			//Setting the flags
			progressiveLoadInProgress.setValue(true);
			
			//Loading a chunk
			long lastMessageDate = conversationInfo.getConversationItems().get(0).getDate();
			new AsyncTask<Void, Void, List<ConversationManager.ConversationItem>>() {
				@Override
				protected List<ConversationManager.ConversationItem> doInBackground(Void... params) {
					//Loading the conversation items
					return DatabaseManager.getInstance().loadConversationChunk(conversationInfo, true, lastMessageDate);
				}
				
				@Override
				protected void onPostExecute(List<ConversationManager.ConversationItem> conversationItems) {
					//Setting the progressive load count
					lastProgressiveLoadCount = conversationItems.size();
					
					//Checking if there are no new conversation items
					if(conversationItems.isEmpty()) {
						//Disabling the progressive load (there are no more items to load)
						progressiveLoadReachedLimit = true;
					} else {
						//Loading the items
						List<ConversationManager.ConversationItem> allItems = conversationInfo.getConversationItems();
						if(allItems == null) return;
						
						//Adding the items
						conversationInfo.addChunk(conversationItems);
						
						//Updating the items' relations
						ConversationManager.addConversationItemRelations(conversationInfo, allItems, conversationItems, MainApplication.getInstance(), true);
					}
					
					//Finishing the progressive load
					progressiveLoadInProgress.setValue(false);
				}
			}.execute();
		}
		
		@SuppressLint("StaticFieldLeak")
		void applyDraftMessage(String message) {
			//Invalidating the message if it is empty
			if(message != null && message.isEmpty()) message = null;
			final String finalMessage = message;
			
			//Recording the update time
			long updateTime = System.currentTimeMillis();
			
			//Checking if the conversation is valid
			if(conversationInfo != null) {
				//Returning if the draft message hasn't changed
				if(Objects.equals(conversationInfo.getDraftMessage(), finalMessage)) return;
				
				//Assigning the message to the conversation in memory
				conversationInfo.setDraftMessageUpdate(getApplication(), finalMessage, updateTime);
			}
			
			//Writing the message to disk
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					DatabaseManager.getInstance().updateConversationDraftMessage(conversationID, finalMessage, updateTime);
					return null;
				}
			}.execute();
		}
		
		boolean isProgressiveLoadInProgress() {
			Boolean value = progressiveLoadInProgress.getValue();
			return value == null ? false : value;
		}
		
		/* int getRecordingDuration() {
			Integer value = recordingDuration.getValue();
			if(value == null) return 0;
			return value;
		} */
		
		boolean startRecording(Activity activity) {
			//Setting up the media recorder
			boolean result = setupMediaRecorder(activity);
			
			//Returning false if the media recorder couldn't be set up
			if(!result) return false;
			
			//Finding a target file
			targetFileRecording = MainApplication.getDraftTarget(getApplication(), conversationID, Constants.recordingName);
			
			/* try {
				//Creating the targets
				if(!targetFileRecording.getParentFile().mkdir()) throw new IOException();
				//if(!targetFile.createNewFile()) throw new IOException();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning
				return false;
			} */
			
			//Setting the media recorder file
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) mediaRecorder.setOutputFile(targetFileRecording);
			else mediaRecorder.setOutputFile(targetFileRecording.getAbsolutePath());
			
			try {
				//Preparing the media recorder
				mediaRecorder.prepare();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning
				return false;
			}
			
			//Starting the recording timer
			startRecordingTimer();
			
			//Starting the media recorder
			mediaRecorder.start();
			
			//Updating the state
			isRecording.setValue(true);
			
			//Returning true
			return true;
		}
		
		/**
		 * Stops the current recording session
		 * @param cleanup whether or not to clean up the media recorder (usually wanted, unless the media recorder encountered an error)
		 * @param discard whether or not to discard the recorded file
		 * @return the file's availability (to be able to use or send)
		 */
		private boolean stopRecording(boolean cleanup, boolean discard) {
			//Returning if the input state is not recording
			if(!isRecording()) return true;
			
			try {
				//Stopping the timer
				stopRecordingTimer();
				
				if(cleanup) {
					try {
						//Stopping the media recorder
						mediaRecorder.stop();
					} catch(RuntimeException stopException) { //The media recorder couldn't capture any media
						//Showing a toast
						Toast.makeText(MainApplication.getInstance(), R.string.imperative_recording_instructions, Toast.LENGTH_LONG).show();
						
						//Invalidating the recording file reference
						targetFileRecording = null;
						
						//Returning false
						return false;
					}
				}
				
				//Checking if the recording was under a second
				if(recordingDuration.getValue() < 1) {
					//Showing a toast
					Toast.makeText(MainApplication.getInstance(), R.string.imperative_recording_instructions, Toast.LENGTH_LONG).show();
					
					//Discarding the file
					discard = true;
				}
				
				//Checking if the recording should be discarded
				if(discard) {
					//Deleting the file and invalidating its reference
					targetFileRecording.delete();
					targetFileRecording = null;
					
					//Returning false
					return false;
				}
				
				//Returning true
				return true;
			} finally {
				//Updating the state
				isRecording.setValue(false);
			}
		}
		
		boolean isRecording() {
			return Boolean.TRUE.equals(isRecording.getValue());
		}
		
		private boolean setupMediaRecorder(Activity activity) {
			//Returning false if the required permissions have not been granted
			if(Constants.requestPermission(activity, new String[]{Manifest.permission.RECORD_AUDIO}, permissionRequestAudio)) {
				//Notifying the user via a toast
				Toast.makeText(activity, R.string.message_permissiondetails_microphone_missing, Toast.LENGTH_SHORT).show();
				
				//Returning false
				return false;
			}
			
			//Setting the media recorder
			if(mediaRecorder == null) mediaRecorder = new MediaRecorder();
			else mediaRecorder.reset();
			
			//Configuring the media recorder
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mediaRecorder.setMaxDuration(10 * 60 * 1000); //10 minutes
			
			mediaRecorder.setOnInfoListener((recorder, what, extra) -> {
				if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
						what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					stopRecording(false, false);
				}
			});
			mediaRecorder.setOnErrorListener((recorder, what, extra) -> {
				stopRecording(false, true);
				mediaRecorder.release();
				mediaRecorder = null;
			});
			
			//Returning true
			return true;
		}
		
		private void startRecordingTimer() {
			recordingDuration.setValue(0);
			recordingTimerHandler.postDelayed(recordingTimerHandlerRunnable, 1000);
		}
		
		private void stopRecordingTimer() {
			recordingTimerHandler.removeCallbacks(recordingTimerHandlerRunnable);
		}
		
		void indexAttachmentsGallery(AttachmentsLoadCallbacks listener, AttachmentsRecyclerAdapter<?> adapter) {
			indexAttachmentsFromMediaStore(listener, attachmentTypeGallery, MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, adapter);
		}
		
		void indexAttachmentsDocument(AttachmentsLoadCallbacks listener, AttachmentsRecyclerAdapter<?> adapter) {
			StringBuilder selectionQuery = new StringBuilder(MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE + " AND " + MediaStore.Files.FileColumns.SIZE + " <= " + ConnectionService.largestFileSize);
			for(int i = 0; i < documentMimeTypes.length; i++) {
				String mimeType = documentMimeTypes[i];
				if(mimeType.endsWith("*")) mimeType = mimeType.substring(0, mimeType.length() - 1);
				if(i == 0) selectionQuery.append(" AND (");
				else selectionQuery.append(" OR ");
				selectionQuery.append(MediaStore.Files.FileColumns.MIME_TYPE).append(" LIKE ").append('"').append(mimeType).append('%').append('"');
				if(i + 1 == documentMimeTypes.length) selectionQuery.append(")");
			}
			
			indexAttachmentsFromMediaStore(listener, attachmentTypeDocument, selectionQuery.toString(), adapter);
		}
		
		byte getAttachmentState(int itemType) {
			return attachmentStates[itemType];
		}
		
		ArrayList<SimpleAttachmentInfo> getAttachmentFileList(int itemType) {
			return attachmentLists[itemType];
		}
		
		@SuppressLint("StaticFieldLeak")
		private void indexAttachmentsFromMediaStore(AttachmentsLoadCallbacks listener, int itemType, String msQuerySelection, AttachmentsRecyclerAdapter<?> adapter) {
			//Updating the listener
			attachmentCallbacks[itemType] = new WeakReference<>(listener);
			
			//Returning if the state is incapable of handling the request
			int currentState = attachmentStates[itemType];
			if(currentState == attachmentsStateLoading || currentState == attachmentsStateLoaded) return;
			
			//Setting the state
			attachmentStates[itemType] = attachmentsStateLoading;
			
			//Starting the asynchronous task
			new AsyncTask<Void, Void, ArrayList<SimpleAttachmentInfo>>() {
				@Override
				protected ArrayList<SimpleAttachmentInfo> doInBackground(Void... params) {
					try {
						//Creating the list
						ArrayList<SimpleAttachmentInfo> list = new ArrayList<>();
						
						//Querying the media files
						try(Cursor cursor = getApplication().getContentResolver().query(
								MediaStore.Files.getContentUri("external"),
								new String[]{MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED},
								msQuerySelection,
								null,
								MediaStore.Files.FileColumns.DATE_ADDED + " DESC" + ' ' + "LIMIT " + attachmentsTileCount)) {
							if(cursor == null) return null;
							
							int indexData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
							int indexType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
							int indexSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
							int indexModificationDate = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
							
							while(cursor.moveToNext()) {
								//Getting the file information
								File file = new File(cursor.getString(indexData));
								String fileType = cursor.getString(indexType);
								String fileName = file.getName();
								long fileSize = cursor.getLong(indexSize);
								long modificationDate = cursor.getLong(indexModificationDate);
								list.add(new SimpleAttachmentInfo(file, fileType, fileName, fileSize, modificationDate));
							}
						}
						
						//Returning the list
						return list;
					} catch(SQLiteException exception) {
						//Logging the exception
						exception.printStackTrace();
						Crashlytics.logException(exception);
						
						//Returning null
						return null;
					}
				}
				
				@Override
				protected void onPostExecute(ArrayList<SimpleAttachmentInfo> files) {
					//Getting the callback listener
					AttachmentsLoadCallbacks listener = attachmentCallbacks[itemType].get();
					
					//Checking if the data is invalid
					if(files == null) {
						//Setting the state
						attachmentStates[itemType] = attachmentsStateFailed;
						
						//Telling the listener
						if(listener != null) listener.onLoadFinished(false);
					} else {
						//Setting the state
						attachmentStates[itemType] = attachmentsStateLoaded;
						
						//Setting the items
						attachmentLists[itemType] = files;
						
						//Telling the listener
						if(listener != null) listener.onLoadFinished(true);
						
						//Linking the queued items with the newly loaded items
						adapter.linkToQueue(draftQueueList);
					}
				}
			}.execute();
		}
		
		interface AttachmentsLoadCallbacks {
			void onLoadFinished(boolean successful);
		}
	}
	
	static class AudioPlaybackManager {
		//Creating the constants
		static final String requestTypeAttachment = "attachment-";
		static final String requestTypeDraft = "draft-";
		
		//Creating the values
		private String requestID = "";
		private MediaPlayer mediaPlayer = new MediaPlayer();
		private Callbacks callbacks = null;
		private final Handler mediaPlayerHandler = new Handler(Looper.getMainLooper());
		private final Runnable mediaPlayerHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				//Notifying the listener
				callbacks.onProgress(mediaPlayer.getCurrentPosition());
				
				//Running again
				mediaPlayerHandler.postDelayed(this, 10);
			}
		};
		
		AudioPlaybackManager() {
			//Setting the media player listeners
			mediaPlayer.setOnPreparedListener(player -> {
				//Playing the media
				player.start();
				
				//Starting the timer
				startTimer();
				
				//Notifying the listener
				callbacks.onPlay();
			});
			mediaPlayer.setOnCompletionListener(player -> {
				//Cancelling the timer
				stopTimer();
				
				//Notifying the listener
				callbacks.onStop();
			});
		}
		
		void release() {
			//Cancelling the timer
			if(mediaPlayer.isPlaying()) mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
			
			//Releasing the media player
			mediaPlayer.release();
		}
		
		boolean play(File file, Callbacks callbacks) {
			return play(null, file, callbacks);
		}
		
		boolean play(String requestID, File file, Callbacks callbacks) {
			//Returning true if the request ID matches
			if(requestID != null && this.requestID.equals(requestID)) return true;
			
			//Stopping the current media player
			stop();
			
			//Resetting the media player
			mediaPlayer.reset();
			
			try {
				//Creating the media player
				mediaPlayer.setDataSource(file.getPath());
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning false
				return false;
			}
			
			//Setting the request information
			this.requestID = requestID;
			this.callbacks = callbacks;
			
			//Preparing the media player
			mediaPlayer.prepareAsync();
			
			//Returning true
			return true;
		}
		
		void togglePlaying() {
			//Checking if the media player is playing
			if(mediaPlayer.isPlaying()) {
				//Pausing the media player
				mediaPlayer.pause();
				
				//Cancelling the playback timer
				stopTimer();
				
				//Notifying the listener
				callbacks.onPause();
			} else {
				//Playing the media player
				mediaPlayer.start();
				
				//Starting the playback timer
				startTimer();
				
				//Notifying the listener
				callbacks.onPlay();
			}
		}
		
		void stop() {
			//if(!mediaPlayer.isPlaying()) return;
			
			mediaPlayer.stop();
			if(callbacks != null) callbacks.onStop();
			stopTimer();
		}
		
		boolean compareRequestID(String requestID) {
			return this.requestID == requestID;
		}
		
		interface Callbacks {
			void onPlay();
			void onProgress(long time);
			void onPause();
			void onStop();
		}
		
		private void startTimer() {
			mediaPlayerHandlerRunnable.run();
		}
		
		private void stopTimer() {
			mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
		}
	}
	
	/* static class AudioMessageManager {
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
	} */
	
	private static class UpdateUnreadMessageCount extends AsyncTask<Void, Void, Void> {
		private final WeakReference<Context> contextReference;
		private final long conversationID;
		private final int count;
		
		UpdateUnreadMessageCount(Context context, long conversationID, int count) {
			contextReference = new WeakReference<>(context);
			
			this.conversationID = conversationID;
			this.count = count;
		}
		
		@Override
		protected Void doInBackground(Void... voids) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Updating the time
			DatabaseManager.getInstance().setUnreadMessageCount(conversationID, count);
			
			//Returning
			return null;
		}
	}
	
	private static class AddGhostMessageTask extends AsyncTask<ConversationManager.MessageInfo, ConversationManager.MessageInfo, Void> {
		private final WeakReference<Context> contextReference;
		private final Consumer<ConversationManager.MessageInfo> onFinishListener;
		
		AddGhostMessageTask(Context context, Consumer<ConversationManager.MessageInfo> onFinishListener) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			
			//Setting the other values
			this.onFinishListener = onFinishListener;
		}
		
		@Override
		protected Void doInBackground(ConversationManager.MessageInfo... messages) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Adding the items to the database
			for(ConversationManager.MessageInfo message : messages) {
				DatabaseManager.getInstance().addConversationItem(message);
				publishProgress(message);
			}
			
			//Returning
			return null;
		}
		
		@Override
		protected void onProgressUpdate(ConversationManager.MessageInfo... messages) {
			for(ConversationManager.MessageInfo message : messages) onFinishListener.accept(message);
		}
	}
	
	private static class ActivityCallbacks extends ConversationManager.ConversationInfo.ActivityCallbacks {
		//Creating the references
		private final WeakReference<Messaging> activityReference;
		
		ActivityCallbacks(Messaging activity) {
			//Setting the references
			activityReference = new WeakReference<>(activity);
		}
		
		@Override
		public void listUpdateFully() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageList.getRecycledViewPool().clear();
			activity.messageListAdapter.notifyDataSetChanged();
			//activity.messageList.scheduleLayoutAnimation();
		}
		
		@Override
		void listUpdateInserted(int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.notifyItemInserted(index);
		}
		
		@Override
		void listUpdateRemoved(int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.notifyItemRemoved(index);
		}
		
		@Override
		public void listUpdateMove(int from, int to) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.notifyItemChanged(from);
			activity.messageListAdapter.notifyItemMoved(from, to);
		}
		
		@Override
		void listUpdateUnread() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Notifying the scroll listener
			activity.messageListScrollListener.onScrolled(activity.messageList, 0, 0);
		}
		
		@Override
		void listScrollToBottom() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.scrollToBottom();
		}
		
		@Override
		void listAttemptScrollToBottom(int... newIndices) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			boolean newMessageAdded = false;
			for(int index : newIndices) {
				if(activity.messageListAdapter.isDirectlyBelowFrame(index)) {
					newMessageAdded = true;
					break;
				}
			}
			
			//Scrolling to the bottom of the list
			if(newMessageAdded) activity.messageListAdapter.scrollToBottom();
		}
		
		@Override
		void chatUpdateTitle() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Building the conversation title
			activity.viewModel.conversationInfo.buildTitle(activity, (result, wasTasked) -> {
				//Setting the title in the app bar
				activity.getSupportActionBar().setTitle(result);
				
				//Updating the task description
				activity.lastTaskDescription = new ActivityManager.TaskDescription(result, activity.lastTaskDescription.getIcon(), activity.viewModel.conversationInfo.getConversationColor());
				activity.setTaskDescription(activity.lastTaskDescription);
			});
		}
		
		@Override
		void chatUpdateUnreadCount() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the unread indicator
			activity.updateUnreadIndicator();
		}
		
		@Override
		void chatUpdateMemberAdded(ConversationManager.MemberInfo member, int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Adding the member
			activity.addMemberView(member, index, true);
		}
		
		@Override
		void chatUpdateMemberRemoved(ConversationManager.MemberInfo member, int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Removing the member
			activity.removeMemberView(member);
		}
		
		@Override
		AudioPlaybackManager getAudioPlaybackManager() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return null;
			
			//Returning the view model's audio message manager
			return activity.viewModel.audioPlaybackManager;
		}
		
		@Override
		void playScreenEffect(String screenEffect, View target) {
			Messaging activity = activityReference.get();
			if(activity != null) activity.playScreenEffect(screenEffect, target);
		}
	}
	
	void detailSwitchConversationColor(int newColor) {
		//Updating the conversation color
		viewModel.conversationInfo.setConversationColor(newColor);
		if(!viewModel.conversationInfo.isGroupChat()) viewModel.conversationInfo.updateViewUser(this);
		
		//Coloring the UI
		colorUI(findViewById(android.R.id.content));
		
		//Updating the conversation color on disk
		DatabaseManager.getInstance().updateConversationColor(viewModel.conversationInfo.getLocalID(), newColor);
		
		//Updating the member if there is only one member as well
		if(viewModel.conversationInfo.getConversationMembers().size() == 1) detailSwitchMemberColor(viewModel.conversationInfo.getConversationMembers().get(0), newColor);
	}
	
	void detailSwitchMemberColor(ConversationManager.MemberInfo member, int newColor) {
		//Updating the user's color
		member.setColor(newColor);
		
		//Updating the view
		View memberView = memberListViews.get(member);
		((ImageView) memberView.findViewById(R.id.button_change_color)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		((ImageView) memberView.findViewById(R.id.profile_default)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		
		//Updating the message colors
		if(viewModel.conversationItemList != null) for(ConversationManager.ConversationItem conversationItem : viewModel.conversationItemList) conversationItem.updateViewColor(this);
		
		//Updating the listing color
		viewModel.conversationInfo.updateViewUser(this);
		
		//Updating the member color on disk
		DatabaseManager.getInstance().updateMemberColor(viewModel.conversationInfo.getLocalID(), member.getName(), newColor);
	}
	
	private static final String colorDialogTag = "colorPickerDialog";
	void showColorDialog(ConversationManager.MemberInfo member, int currentColor) {
		//Starting a fragment transaction
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		
		//Removing the previous fragment if it already exists
		Fragment previousFragment = getSupportFragmentManager().findFragmentByTag(colorDialogTag);
		if(previousFragment != null) fragmentTransaction.remove(previousFragment);
		fragmentTransaction.addToBackStack(null);
		
		//Creating and showing the dialog fragment
		ColorPickerDialog newFragment = ColorPickerDialog.newInstance(member, currentColor);
		newFragment.show(fragmentTransaction, colorDialogTag);
		currentColorPickerDialog = newFragment;
		currentColorPickerDialogMember = member;
	}
	
	public static class ColorPickerDialog extends DialogFragment {
		//Creating the instantiation values
		private ConversationManager.MemberInfo member = null;
		private int selectedColor;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			//Getting the arguments
			if(getArguments().containsKey(Constants.intentParamData))
				member = (ConversationManager.MemberInfo) getArguments().getSerializable(Constants.intentParamData);
			selectedColor = getArguments().getInt(Constants.intentParamCurrent);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			//Configuring the dialog
			getDialog().setTitle(member == null ? R.string.action_editconversationcolor : R.string.action_editcontactcolor);
			
			//Inflating the view
			View dialogView = inflater.inflate(R.layout.dialog_colorpicker, container, false);
			ViewGroup contentViewGroup = dialogView.findViewById(R.id.colorpicker_itemview);
			
			int padding = Constants.dpToPx(24);
			contentViewGroup.setPadding(padding, padding, padding, padding);
			
			//Adding the elements
			for(int i = 0; i < ConversationManager.ConversationInfo.standardUserColors.length; i++) {
				//Getting the color
				final int standardColor = ConversationManager.ConversationInfo.standardUserColors[i];
				
				//Inflating the layout
				View item = inflater.inflate(R.layout.dialog_colorpicker_item, contentViewGroup, false);
				
				//Configuring the layout
				ImageView colorView = item.findViewById(R.id.colorpickeritem_color);
				colorView.setColorFilter(standardColor);
				
				final boolean isSelectedColor = selectedColor == standardColor;
				if(isSelectedColor) item.findViewById(R.id.colorpickeritem_selection).setVisibility(View.VISIBLE);
				
				//Setting the click listener
				colorView.setOnClickListener(view -> {
					//Telling the activity
					if(!isSelectedColor) {
						if(member == null) ((Messaging) getActivity()).detailSwitchConversationColor(standardColor);
						else ((Messaging) getActivity()).detailSwitchMemberColor(member, standardColor);
					}
					
					getDialog().dismiss();
				});
				
				//Adding the view to the layout
				contentViewGroup.addView(item);
			}
			
			//Returning the view
			return dialogView;
		}
		
		static ColorPickerDialog newInstance(ConversationManager.MemberInfo memberInfo, int selectedColor) {
			//Creating the instance
			ColorPickerDialog instance = new ColorPickerDialog();
			
			//Adding the member info
			Bundle bundle = new Bundle();
			bundle.putSerializable(Constants.intentParamData, memberInfo);
			bundle.putSerializable(Constants.intentParamCurrent, selectedColor);
			instance.setArguments(bundle);
			
			//Returning the instance
			return instance;
		}
	}
	
	public static class InsertionEditText extends AppCompatEditText {
		private ContentProcessor contentProcessor = null;
		
		public InsertionEditText(Context context) {
			super(context);
		}
		
		public InsertionEditText(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		public InsertionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
			super(context, attrs, defStyleAttr);
		}
		
		void setContentProcessor(ContentProcessor value) {
			contentProcessor = value;
		}
		
		@Override
		public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
			//Configuring the input connection
			InputConnection inputConnection = super.onCreateInputConnection(editorInfo);
			if(inputConnection == null) return null;
			EditorInfoCompat.setContentMimeTypes(editorInfo, new String[]{"image/*"});
			
			//Creating the callback
			InputConnectionCompat.OnCommitContentListener callback = (inputContentInfo, flags, opts) -> {
				//Returning false if the content processor is not ready
				if(contentProcessor == null) return false;
				
				//Requesting permission to use image keyboard
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && (flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
					try {
						inputContentInfo.requestPermission();
					} catch(Exception exception) {
						exception.printStackTrace();
						return false;
					}
				}
				
				//Getting the data
				Uri uri = inputContentInfo.getContentUri();
				ClipDescription itemDesc = inputContentInfo.getDescription();
				String type = itemDesc.getMimeTypeCount() > 0 ? itemDesc.getMimeType(0) : Constants.defaultMIMEType;
				
				//Determining the correct file extension
				String extension = null;
				switch(type) {
					case "image/bmp":
						extension = "bmp";
						break;
					case "image/gif":
						extension = "gif";
						break;
					case "image/x-icon":
						extension = "ico";
						break;
					case "image/jpeg":
						extension = "jpeg";
						break;
					case "image/png":
						extension = "png";
						break;
					case "image/svg+html":
						extension = "svg";
						break;
					case "image/tiff":
						extension = "tiff";
						break;
					case "image/webp":
						extension = "webp";
						break;
				}
				
				//Getting the name
				String name = itemDesc.getLabel().toString() + (extension == null ? "" : '.' + extension);
				
				//Sending the data to the content processor
				contentProcessor.process(uri, type, name, -1);
				new Handler().postDelayed(inputContentInfo::releasePermission, 1000);
				
				//Returning true
				return true;
			};
			
			//Returning the created data wrapper
			return InputConnectionCompat.createWrapper(inputConnection, editorInfo, callback);
		}
	}
	
	private interface ContentProcessor {
		void process(Uri content, String type, String name, long size);
	}
}