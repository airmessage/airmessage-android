package me.tagavari.airmessage.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.ClipDescription;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.ConversationAction;
import android.view.textclassifier.ConversationActions;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.cardview.widget.CardView;
import androidx.collection.LongSparseArray;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Consumer;
import androidx.core.util.Pools;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.util.BiConsumer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Objects;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Url;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.FileProcessingRequest;
import me.tagavari.airmessage.connection.request.FilePushRequest;
import me.tagavari.airmessage.connection.request.FileRemovalRequest;
import me.tagavari.airmessage.messaging.AMConversationAction;
import me.tagavari.airmessage.messaging.ContactAttachmentInfo;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.DraftFile;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageComponent;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.messaging.VLocationAttachmentInfo;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.util.ColorHelper;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.extension.MediaSharedElementCallback;
import me.tagavari.airmessage.compositeplugin.PluginMessageBar;
import me.tagavari.airmessage.view.AppleEffectView;
import me.tagavari.airmessage.view.OverScrollScrollView;
import me.tagavari.airmessage.view.VisualizerView;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

import static android.provider.Settings.System.canWrite;
import static android.provider.Settings.System.getInt;

public class Messaging extends AppCompatCompositeActivity {
	//Creating the reference values
	public static final int messageChunkSize = 20;
	public static final int progressiveLoadThreshold = 10;
	
	private static final int quickScrollFABThreshold = 3;
	private static final float bottomSheetFillThreshold = 0.8F;
	private static final float contentPanelMinAllowanceDP = 275;
	private static final int draftCountLimit = 10;
	
	private static final String mimeTypeImage = "image/*";
	private static final String mimeTypeVideo = "video/*";
	private static final String mimeTypeAudio = "audio/*";
	private static final String mimeTypeGIF = "image/gif";
	private static final String mimeTypeVCard = "text/vcard";
	private static final String mimeTypeVLocation = "text/x-vlocation";
	
	private static final long confettiDuration = 1000;
	//private static final float disabledAlpha = 0.38F
	
	private static final int permissionRequestStorage = 0;
	private static final int permissionRequestAudio = 1;
	private static final int permissionRequestAudioDirect = 2; //Used when requesting microphone usage directly form the input bar
	private static final int permissionRequestLocation = 3;
	private static final int permissionRequestMessageCustomOffset = 100; //Used to offset custom message permission requests, to prevent collisions with activity requests
	
	private static final int intentPickFile = 0;
	private static final int intentTakePicture = 1;
	private static final int intentLocationResolution = 2;
	private static final int intentPickLocation = 3;
	private static final int intentSaveFileSAF = 4;
	
	//Creating the static values
	private static final List<WeakReference<Messaging>> foregroundConversations = new ArrayList<>();
	//private static final List<WeakReference<Messaging>> loadedConversations = new ArrayList<>();
	
	//Creating the view model and plugin values
	private ActivityViewModel viewModel;
	private PluginMessageBar pluginMessageBar;
	
	//Creating the info bar values
	private PluginMessageBar.InfoBar infoBarConnection;
	
	//Creating the state values
	private boolean currentScreenEffectPlaying = false;
	
	private boolean bottomDetailsWindowIntercept = false;
	private float bottomDetailsWindowDragY = -1;
	//private float bottomDetailsWindowStartY;
	private float bottomDetailsWindowEndY;
	
	//Creating the view values
	private View rootView;
	private AppBarLayout appBar;
	private TextView labelLoading;
	private ViewGroup groupLoadFail;
	private TextView labelLoadFail;
	private RecyclerView messageList;
	private View inputBar;
	private View inputBarShadow;
	private View attachmentsPanel;
	private ImageButton buttonSendMessage;
	private FrameLayout buttonAddContent;
	private InsertionEditText messageInputField;
	private ViewGroup recordingActiveGroup;
	private TextView recordingTimeLabel;
	private VisualizerView recordingVisualizer;
	private FloatingActionButton bottomFAB;
	private TextView bottomFABBadge;
	private AppleEffectView appleEffectView;
	private FrameLayout bottomDetailsPanel;
	
	private final HashMap<MemberInfo, View> memberListViews = new HashMap<>();
	
	private View detailScrim;
	
	private RecyclerView listAttachmentQueue;
	
	private final MessageListRecyclerAdapter messageListAdapter = new MessageListRecyclerAdapter();
	
	//Creating the listener values
	private final BroadcastReceiver clientConnectionResultBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Getting the connection manager
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			
			//Returning if this is not the latest launch
			if(connectionManager == null || ConnectionManager.getCurrentLaunchID() != intent.getByteExtra(Constants.intentParamLaunchID, (byte) -1)) return;
			
			int state = intent.getIntExtra(Constants.intentParamState, -1);
			if(state == ConnectionManager.stateDisconnected) {
				int code = intent.getIntExtra(Constants.intentParamCode, -1);
				showServerWarning(code);
			} else hideServerWarning();
		}
	};
	private final RecyclerView.OnScrollListener messageListScrollListener = new RecyclerView.OnScrollListener() {
		int lastRevealedItem = 0;
		
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			//Getting the layout manager
			LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			int itemsScrolledFromBottom = linearLayoutManager.getItemCount() - 1 - linearLayoutManager.findLastVisibleItemPosition();
			
			//Showing the FAB if the user has scrolled more than the threshold items
			if(itemsScrolledFromBottom > quickScrollFABThreshold) setFABVisibility(true);
			else setFABVisibility(false);
			
			//Marking viewed items as read
			if(itemsScrolledFromBottom < viewModel.conversationInfo.getUnreadMessageCount())
				viewModel.conversationInfo.setUnreadMessageCount(itemsScrolledFromBottom);
			
			//Loading chunks if the user is scrolled to the top
			if(linearLayoutManager.findFirstVisibleItemPosition() < progressiveLoadThreshold && !viewModel.isProgressiveLoadInProgress() && !viewModel.progressiveLoadReachedLimit)
				recyclerView.post(viewModel::loadNextChunk);
			
			//Checking if the user is scrolling upwards
			int newRevealedItem = dy < 0 ? linearLayoutManager.findFirstVisibleItemPosition() : linearLayoutManager.findLastVisibleItemPosition();
			if(lastRevealedItem != newRevealedItem) {
				lastRevealedItem = newRevealedItem;
				/* RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForLayoutPosition(newRevealedItem);
				if(viewHolder instanceof MessageInfo.ViewHolder) {
					((MessageInfo.ViewHolder) viewHolder).onBind();
				} */
				if(lastRevealedItem > 0 && lastRevealedItem < viewModel.conversationItemList.size()) {
					ConversationItem item = viewModel.conversationItemList.get(lastRevealedItem);
					if(item instanceof MessageInfo)
						((MessageInfo) item).onScrollShow();
				}
			}
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
	private final BroadcastReceiver contactsUpdateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			rebuildContactViews();
		}
	};
	private final OverScrollScrollView.OverScrollListener detailsPanelOverScrollListener = new OverScrollScrollView.OverScrollListener() {
		@Override
		public void onOverScroll(float scrollY) {
			//Only accepting when the user overscrolls from the top of the window
			if(scrollY > 0) bottomDetailsWindowIntercept = true;
		}
	};
	private final ViewTreeObserver.OnGlobalLayoutListener rootLayoutListener = () -> {
		{
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
		}
		
		{
			//Setting a height limit on the message input field
			//int height = rootView.getHeight() - attachmentsPanel.getHeight();
			//messageInputField.setMaxHeight(height - Constants.dpToPx(200));
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
	private final View.OnKeyListener inputFieldKeyListener = (view, keyCode, event) -> {
		//Returning if the event is not a key down
		if(event.getAction() != KeyEvent.ACTION_DOWN) return false;
		
		/*
		Google default keyboards are the only keyboards that seem to behave properly here.
		Third-party keyboards like SwiftKey and Samsung Keyboard will trigger this key event when pressing the enter key, even though they're still software keyboards.
		If a software keyboard provided the event, the event source is UNKNOWN. If it's a physical bluetooth keyboard, it is assigned a source.
		On Chrome OS, since the keyboard is attached to the device, it provides the same event data as a soft keyboard.
		However, on Chrome OS, the default keyboard does not trigger this. (Let's just hope the user doesn't install a third-party keyboard!)
		 */
		if(keyCode == KeyEvent.KEYCODE_ENTER && !event.isShiftPressed() &&
		   (event.getSource() != InputDevice.SOURCE_UNKNOWN || Constants.isChromeOS(this))) {
			//Sending the message
			sendMessage();
			
			//Returning true
			return true;
		}
		
		//Returning false
		return false;
	};
	private final TextView.OnEditorActionListener inputFieldEditorActionListener = (textView, actionID, event) -> {
		/*
		IME_ACTION_DONE is triggered on the Google Pixelbook
		IME_NULL is triggered with external wireless keyboards
		 */
		if(actionID == EditorInfo.IME_ACTION_DONE ||
		   (actionID == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getSource() != InputDevice.SOURCE_UNKNOWN)) {
			//Sending the message
			sendMessage();
			
			//Returning true
			return true;
		}
		
		//Returning false
		return false;
	};
	private final View.OnClickListener sendButtonClickListener = view -> sendMessage();
	private final Observer<Integer> messagesStateObserver = state -> {
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
				viewModel.conversationInfo.buildTitle(this, new ConversationTitleResultCallback(this));
				
				//Coloring the UI
				colorUI(findViewById(android.R.id.content));
				
				//Setting the activity callbacks
				viewModel.conversationInfo.setActivityCallbacks(new ActivityCallbacks(this));
				
				//Setting the message input field hint
				messageInputField.setHint(getInputBarMessage());
				
				//Updating the send button
				//updateSendButton();
				
				break;
			case ActivityViewModel.messagesStateReady: {
				labelLoading.setVisibility(View.GONE);
				groupLoadFail.setVisibility(View.GONE);
				
				setMessageBarState(true);
				
				//Setting the conversation title
				viewModel.conversationInfo.buildTitle(this, new ConversationTitleResultCallback(this));
				
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
					if(viewModel.draftQueueList.isEmpty())
						for(DraftFile draft : viewModel.conversationInfo.getDrafts()) {
							//Creating the queued item
							QueuedFileInfo queuedItem = new QueuedFileInfo(draft);
							
							//Searching for the item's push request
							FilePushRequest currentRequest = null;
							ConnectionManager connectionManager = ConnectionService.getConnectionManager();
							if(connectionManager != null) {
								FileProcessingRequest processingRequest = connectionManager.searchFileProcessingQueue(draft.getLocalID());
								if(processingRequest instanceof FilePushRequest) currentRequest = (FilePushRequest) processingRequest;
							}
							
							//Creating a new request if there was no current request in the queue
							if(currentRequest == null) currentRequest = new FilePushRequest(draft.getFile(), draft.getFileType(), draft.getFileName(), draft.getModificationDate(), viewModel.conversationInfo, -1, draft.getLocalID(), FilePushRequest.stateQueued, 0, false, viewModel.doFilesRequireCompression());
							
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
					ConversationItem conversationItem = viewModel.conversationItemList.get(i);
					
					//Skipping the remainder of the iteration if the item is not a message
					if(!(conversationItem instanceof MessageInfo)) continue;
					
					//Getting the message
					MessageInfo messageInfo = (MessageInfo) conversationItem;
					
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
				
				//Updating the reply suggestions
				viewModel.updateSmartReply();
			}
			break;
			case ActivityViewModel.messagesStateFailedMatching:
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
	
	//Creating the other values
	private boolean currentSendButtonState = true;
	private boolean toolbarVisible = true;
	
	private DialogFragment currentColorPickerDialog = null;
	private MemberInfo currentColorPickerDialogMember = null;
	
	private File currentTargetSAFFile = null;
	
	private int currentInsetPaddingBottom = 0;
	
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
		inputBarShadow = Constants.shouldUseAMOLED(this) ? findViewById(R.id.bottomshadow_amoled) : findViewById(R.id.bottomshadow);
		attachmentsPanel = findViewById(R.id.panel_attachments);
		inputBarShadow.setVisibility(View.VISIBLE);
		buttonSendMessage = inputBar.findViewById(R.id.button_send);
		buttonAddContent = inputBar.findViewById(R.id.button_addcontent);
		messageInputField = inputBar.findViewById(R.id.messagebox);
		//recordingTimeLabel = inputBar.findViewById(R.id.recordingtime);
		bottomFAB = findViewById(R.id.fab_bottom);
		bottomFABBadge = findViewById(R.id.fab_bottom_badge);
		appleEffectView = findViewById(R.id.effect_foreground);
		bottomDetailsPanel = findViewById(R.id.panel_messaginginfo);
		
		detailScrim = findViewById(R.id.scrim);
		
		listAttachmentQueue = findViewById(R.id.inputbar_attachments);
		
		//Setting the window layout
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		} else {
			getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
		
		getWindow().setStatusBarColor(Color.TRANSPARENT);
		
		//Listening for window inset changes
		ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
			appBar.setPadding(appBar.getPaddingLeft(), insets.getSystemWindowInsetTop(), appBar.getPaddingRight(), appBar.getPaddingBottom());
			appBar.post(() -> {
				messageList.setPadding(messageList.getPaddingLeft(), appBar.getHeight(), messageList.getPaddingRight(), messageList.getPaddingBottom());
			});
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				//Adding padding to the bottom input bar
				//View inputBarText = inputBar.findViewById(R.id.inputbar_text);
				//inputBarText.setPadding(inputBarText.getPaddingLeft(), inputBarText.getPaddingTop(), inputBarText.getPaddingRight(), insets.getSystemWindowInsetBottom());
				inputBar.setPadding(inputBar.getPaddingLeft(), inputBar.getPaddingTop(), inputBar.getPaddingRight(), insets.getSystemWindowInsetBottom());
				
				//Adding padding to the details menu
				View detailsMenu = findViewById(R.id.group_messaginginfo_content);
				if(detailsMenu != null) detailsMenu.setPadding(detailsMenu.getPaddingLeft(), detailsMenu.getPaddingTop(), detailsMenu.getPaddingRight(), insets.getSystemWindowInsetBottom());
			} else {
				//Simply applying padding to the entire root view
				rootView.setPadding(rootView.getPaddingLeft(), rootView.getPaddingTop(), rootView.getPaddingRight(), insets.getSystemWindowInsetBottom());
			}
			
			currentInsetPaddingBottom = insets.getSystemWindowInsetBottom();
			
			return insets.consumeSystemWindowInsets();
		});
		
		//Setting the plugin views
		pluginMessageBar.setParentView(findViewById(R.id.infobar_container));
		
		//Enforcing the maximum content width
		Constants.enforceContentWidthView(getResources(), messageList);
		
		//Configuring the AMOLED theme
		if(Constants.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Setting the status bar color
		Constants.updateChromeOSStatusBar(this);
		
		//Creating the filler values
		String fillerText = null;
		Uri[] fillerFiles = null;
		
		//Checking if the request is a send intent
		if(Intent.ACTION_SEND.equals(getIntent().getAction()) || Intent.ACTION_SENDTO.equals(getIntent().getAction())) {
			//Getting the recipient
			String recipientData = getIntent().getDataString();
			String recipient;
			if(recipientData.startsWith("sms:")) recipient = recipientData.replaceFirst("sms:", "");
			else if(recipientData.startsWith("smsto:")) recipient = recipientData.replaceFirst("smsto:", "");
			else if(recipientData.startsWith("mms:")) recipient = recipientData.replaceFirst("mms:", "");
			else if(recipientData.startsWith("mmsto:")) recipient = recipientData.replaceFirst("mmsto:", "");
			else recipient = null;
			//if(recipient.contains("%")) recipient = URLDecoder.decode(recipient, "UTF-8");
			
			//Getting the message
			String message;
			if(getIntent().hasExtra(Intent.EXTRA_TEXT)) message = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			else if(getIntent().hasExtra("sms_body")) message = getIntent().getStringExtra("sms_body");
			else message = null;
			fillerText = message;
			
			//Getting the extra files
			List<Uri> sendFiles = new ArrayList<>();
			{
				Uri data = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
				if(data != null) sendFiles.add(data);
				else {
					List<Uri> dataList = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
					if(dataList != null) sendFiles.addAll(dataList);
				}
			}
			fillerFiles = sendFiles.toArray(new Uri[0]);
			
			//Getting the view model
			viewModel = ViewModelProviders.of(this, new ViewModelProvider.Factory() {
				@NonNull
				@Override
				public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
					return (T) new ActivityViewModel(getApplication(), new String[]{recipient});
				}
			}).get(ActivityViewModel.class);
		} else {
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
			
			//Getting the filler data
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getIntent().hasExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT)) fillerText = getIntent().getStringExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT); //Notification inline reply text (only supported on Android P and above)
			else if(getIntent().hasExtra(Constants.intentParamDataText)) fillerText = getIntent().getStringExtra(Constants.intentParamDataText); //Shared text from activity
			
			if(getIntent().hasExtra(Constants.intentParamDataFile)) {
				Parcelable[] parcelableArray = getIntent().getParcelableArrayExtra(Constants.intentParamDataFile);
				fillerFiles = Arrays.copyOf(parcelableArray, parcelableArray.length, Uri[].class);
			}
		}
		
		//Restoring the input bar state
		restoreInputBarState();
		
		//Restoring the text states
		//findViewById(R.id.loading_text).setVisibility(viewModel.messagesState == viewModel.messagesStateLoading ? View.VISIBLE : View.GONE);
		
		//Enabling the toolbar's up navigation
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Setting the input bar elevation animation
		messageList.setOnScrollChangeListener(new View.OnScrollChangeListener() {
			boolean isShadowVisible = false;
			
			@Override
			public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				boolean visibility = messageList.canScrollVertically(1);
				if(isShadowVisible == visibility) return;
				isShadowVisible = visibility;
				
				inputBarShadow.animate().alpha(visibility ? 1 : 0);
			}
		});
		
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				int height = rootView.getHeight() - attachmentsPanel.getHeight();
				messageInputField.setMaxHeight(height - Constants.dpToPx(400));
			}
		});
		
		//Setting the listeners
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(rootLayoutListener);
		messageInputField.addTextChangedListener(inputFieldTextWatcher);
		//messageInputField.setOnKeyListener(inputFieldKeyListener);
		messageInputField.setOnEditorActionListener(inputFieldEditorActionListener);
		//messageInputField.setOnClickListener(view -> closeAttachmentsPanel(false));
		buttonSendMessage.setOnClickListener(sendButtonClickListener);
		buttonAddContent.setOnClickListener(view -> {
			if(viewModel.isAttachmentsPanelOpen) {
				/* if(KeyboardVisibilityEvent.isKeyboardVisible(Messaging.this)) UIUtil.showKeyboard(Messaging.this, messageInputField); //The attachment drawer is automatically hidden when the keyboard is opened
				else closeAttachmentsPanel(true); */
				closeAttachmentsPanel(true);
			} else {
				/*InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				boolean result = inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
				openAttachmentsPanel(false, !result); */
				
				/* if(KeyboardVisibilityEvent.isKeyboardVisible(Messaging.this)) {
					//Waiting for the keyboard to close itself before opening the attachment drawer
					attachmentsWaitingForKeyboard = true;
					UIUtil.hideKeyboard(Messaging.this);
				} else openAttachmentsPanel(false, true); */
				
				openAttachmentsPanel(false, true);
			}
		});
		/* inputBar.findViewById(R.id.button_camera).setOnClickListener(view -> requestTakePicture());
		inputBar.findViewById(R.id.button_gallery).setOnClickListener(view -> requestGalleryFile());
		inputBar.findViewById(R.id.button_attach).setOnClickListener(view -> requestAnyFile());
		contentRecordButton.setOnTouchListener(recordingTouchListener); */
		bottomFAB.setOnClickListener(view -> messageListAdapter.scrollToBottom());
		appleEffectView.setFinishListener(() -> currentScreenEffectPlaying = false);
		detailScrim.setOnClickListener(view -> closeDetailsPanel(true));
		
		LocalBroadcastManager.getInstance(this).registerReceiver(contactsUpdateBroadcastReceiver, new IntentFilter(MainApplication.localBCContactUpdate));
		
		//Configuring the input field
		messageInputField.setContentProcessor((uri, type, name, size) -> queueAttachment(new SimpleAttachmentInfo(uri, type, Constants.cleanFileName(name), size, -1), findAppropriateTileHelper(type), true));
		
		//Setting up the attachments
		{
			listAttachmentQueue.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
			listAttachmentQueue.setAdapter(new AttachmentsQueueRecyclerAdapter(viewModel.draftQueueList));
			
			listAttachmentQueue.setOutlineProvider(new ViewOutlineProvider() {
				@Override
				public void getOutline(View view, Outline outline) {
					int radius = getResources().getDimensionPixelSize(R.dimen.inputbar_radius);
					outline.setRoundRect(0, 0, view.getWidth(), view.getHeight() + radius, radius);
				}
			});
			listAttachmentQueue.setClipToOutline(true);
		}
		
		//Setting the filler data
		if(fillerText != null) messageInputField.setText(fillerText);
		if(fillerFiles != null) new QueueUriAsyncTask(this).execute(fillerFiles);
		
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
		
		getWindow().getDecorView().post(() -> {
			//Restoring the panels
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
			if(viewModel.targetFileRecording != null) {
				new QueueFileAsyncTask(this).execute(viewModel.targetFileRecording);
				viewModel.targetFileRecording = null;
			}
		});
		viewModel.recordingDuration.observe(this, value -> {
			if(recordingTimeLabel != null) recordingTimeLabel.setText(DateUtils.formatElapsedTime(value));
		});
		viewModel.progressiveLoadInProgress.observe(this, value -> {
			if(value) onProgressiveLoadStart();
			else onProgressiveLoadFinish(viewModel.lastProgressiveLoadCount);
		});
		viewModel.smartReplyAvailable.observe(this, value -> {
			//Updating the recycler adapter (to show the reply suggestions)
			if(value) {
				if(messageListAdapter.replySuggestionsAvailable) {
					messageListAdapter.notifyItemChanged(messageListAdapter.getItemCount() - 1);
				} else {
					messageListAdapter.replySuggestionsAvailable = true;
					messageListAdapter.notifyItemInserted(messageListAdapter.getItemCount());
					getWindow().getDecorView().post(messageListAdapter::scrollToBottom);
				}
			} else {
				if(messageListAdapter.replySuggestionsAvailable) {
					messageListAdapter.replySuggestionsAvailable = false;
					messageListAdapter.notifyItemRemoved(messageListAdapter.getItemCount() - 1);
				}
			}
		});
		
		/* KeyboardVisibilityEvent.setEventListener(this, isOpen -> {
			//Closing the attachment drawer if it is open
			if(isOpen && viewModel.isAttachmentsPanelOpen) {
				closeAttachmentsPanel(false);
			}
			//Checking if the activity is waiting for the keyboard and the drawer is closed
			else if(!isOpen && !viewModel.isAttachmentsPanelOpen && attachmentsWaitingForKeyboard) {
				//Updating the attachment drawer
				attachmentsWaitingForKeyboard = false;
			}
		}); */
		
		/* inputBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
			boolean movingUp = top < oldTop;
			if(top > oldTop) { //Layout is expanding downwards
				closeAttachmentsPanel(false);
				inputBar.post(() -> inputBar.requestLayout());
			}
		}); */
		
		messageInputField.setOnTouchListener((View view, MotionEvent event) -> {
			closeAttachmentsPanel(true);
			return false;
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
		localBroadcastManager.registerReceiver(clientConnectionResultBroadcastReceiver, new IntentFilter(ConnectionManager.localBCStateUpdate));
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
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		boolean showWarning = connectionManager == null || (connectionManager.getCurrentState() == ConnectionManager.stateDisconnected && connectionManager.getLastConnectionResult() != -1);
		if(showWarning) showServerWarning(connectionManager == null ? -1 : connectionManager.getLastConnectionResult());
		else hideServerWarning();
		
		//Notifying the views
		if(viewModel.messagesState.getValue() == ActivityViewModel.messagesStateReady) {
			for(ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof MessageInfo) {
					((MessageInfo) conversationItem).notifyResume();
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
			for(ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof MessageInfo) {
					((MessageInfo) conversationItem).notifyPause();
				}
			}
		}
		
		//Saving the draft message
		viewModel.applyDraftMessage(messageInputField.getText().toString());
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
			for(ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof MessageInfo) {
					((MessageInfo) conversationItem).notifyPause();
				}
			}
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
			for(ConversationItem conversationItem : viewModel.conversationItemList) {
				if(conversationItem instanceof MessageInfo) {
					((MessageInfo) conversationItem).notifyPause();
				}
			}
		}
		
		//Unregistering the broadcast listeners
		LocalBroadcastManager.getInstance(this).unregisterReceiver(contactsUpdateBroadcastReceiver);
	}
	
	@Override
	public void onActivityReenter(int resultCode, Intent data) {
		//Returning if there is no data
		if(viewModel.conversationInfo == null || viewModel.conversationItemList == null) return;
		
		//Getting the associated message information
		long selectedID = data.getLongExtra(MediaViewer.PARAM_SELECTEDID, -1);
		if(selectedID == -1) return;
		
		LongSparseArray<AttachmentInfo> localIDConversationMap = viewModel.conversationInfo.getLocalIDAttachmentMap();
		AttachmentInfo attachmentInfo = localIDConversationMap.get(selectedID);
		if(attachmentInfo == null) return;
		
		MessageInfo messageInfo = attachmentInfo.getMessageInfo();
		
		//Scrolling the list to the message
		int position = viewModel.conversationItemList.indexOf(messageInfo);
		if(position != RecyclerView.NO_POSITION) messageList.scrollToPosition(position);
		
		//Setting the shared element transitions callback
		MediaSharedElementCallback sharedElementCallback = new MediaSharedElementCallback();
		setExitSharedElementCallback(sharedElementCallback);
		/* setExitSharedElementCallback(new SharedElementCallback() {
			@Override
			public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
				if(viewModel.conversationInfo == null) return;
				LongSparseArray<ConversationUtils.AttachmentInfo> localIDConversationMap = viewModel.conversationInfo.getLocalIDAttachmentMap();
				ConversationUtils.AttachmentInfo attachmentInfo = localIDConversationMap.get(MediaViewer.selectedID);
				if(attachmentInfo == null) return;
				View view = attachmentInfo.getSharedElementView();
				if(view == null) return;
				
				sharedElements.put(names.get(0), view);
			}
		}); */
		supportPostponeEnterTransition();
		messageList.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				messageList.getViewTreeObserver().removeOnPreDrawListener(this);
				sharedElementCallback.setSharedElementViews(attachmentInfo.getSharedElementView());
				supportStartPostponedEnterTransition();
				return true;
			}
		});
		
		getWindow().getSharedElementExitTransition().addListener(new Transition.TransitionListener() {
			@Override
			public void onTransitionStart(Transition transition) {}
			
			@Override
			public void onTransitionEnd(Transition transition) {
				getWindow().getSharedElementExitTransition().removeListener(this);
				setExitSharedElementCallback((SharedElementCallback) null);
			}
			
			@Override
			public void onTransitionCancel(Transition transition) {}
			
			@Override
			public void onTransitionPause(Transition transition) {}
			
			@Override
			public void onTransitionResume(Transition transition) {}
		});
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
		super.onActivityResult(requestCode, resultCode, intent);
		
		switch(requestCode) {
			case intentTakePicture: //Taking a picture
				//Returning if the current input state is not the content bar
				//if(viewModel.inputState != inputStateContent) return;
				
				//Queuing the file
				if(resultCode == RESULT_OK && viewModel.targetFileIntent != null) new QueueFileAsyncTask(this).execute(viewModel.targetFileIntent);
				break;
			case intentPickFile: //Media picker
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
						for(int i = 0; i < intent.getClipData().getItemCount(); i++)
							list[i] = intent.getClipData().getItemAt(i).getUri();
						new QueueUriAsyncTask(this).execute(list);
					}
				}
				break;
			case intentLocationResolution:
				//Updating the attachment section
				if(resultCode == RESULT_OK) viewModel.updateAttachmentsLocationState();
				break;
			case intentPickLocation: {
				if(resultCode != RESULT_OK) break;
				
				//Getting the data
				LatLng mapPosition = intent.getParcelableExtra(Constants.intentParamData);
				String mapPositionAddress = intent.getStringExtra(Constants.intentParamAddress);
				String mapPositionName = intent.getStringExtra(Constants.intentParamName);
				
				//Selecting a file to write to
				File file = MainApplication.getDraftTarget(this, viewModel.conversationID, mapPositionName != null ? Constants.cleanFileName(mapPositionName) + ".loc.vcf" : Constants.locationName);
				
				//Writing the file and creating the attachment data
				new WriteVLocationTask(file, mapPosition, mapPositionAddress, mapPositionName, this).execute();
				
				break;
			}
			case intentSaveFileSAF:
				if(resultCode == RESULT_OK) Constants.exportUri(this, currentTargetSAFFile, intent.getData());
				break;
		}
	}
	
	void setDarkAMOLED() {
		Constants.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(Constants.colorAMOLED);
		
		//Setting the input bar background color
		inputBar.setBackgroundColor(Constants.colorAMOLED);
		
		//Setting the attachments view
		View attachmentsView = findViewById(R.id.pane_attachments);
		if(attachmentsView != null) {
			attachmentsView.setBackgroundColor(Constants.colorAMOLED);
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
			for(File file : files) list.add(new SimpleAttachmentInfo(file, Constants.getMimeType(file), file.getName(), file.length(), file.lastModified()));
			
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
				if(uri == null) continue;
				
				//Querying the file data
				try(Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED}, null, null, null)) {
					if(cursor == null) continue;
					int iType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
					int iDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
					int iSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
					int iModificationDate = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
					
					if(!cursor.moveToFirst()) continue;
					
					//Getting the file information
					String fileType = cursor.getString(iType);
					if(fileType == null) fileType = "application/octet-stream";
					String fileName = Constants.cleanFileName(cursor.getString(iDisplayName));
					long fileSize = cursor.getLong(iSize);
					long modificationDate = cursor.getLong(iModificationDate);
					list.add(new SimpleAttachmentInfo(uri, fileType, fileName, fileSize, modificationDate));
				}
				
				//list.add(new SimpleAttachmentInfo(uri, Constants.getMimeType(context, uri), Constants.getUriName(context, uri), Constants.getUriSize(context, uri), -1));
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
			for(SimpleAttachmentInfo attachmentInfo : results)
				activity.queueAttachment(attachmentInfo, activity.findAppropriateTileHelper(attachmentInfo.getFileType()), true);
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
		if(viewModel.isDetailsPanelOpen) closeDetailsPanel(true);
			//Closing the attachments panel if it is open
		else if(viewModel.isAttachmentsPanelOpen) closeAttachmentsPanel(true);
			//Otherwise passing the event to the superclass
		else super.onBackPressed();
	}
	
	private void sendMessage() {
		/* if(!canWrite(this)) {
			Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
			intent.setData(Uri.parse("package:" + getPackageName()));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			try {
				startActivity(intent);
			} catch (Exception e) {
				Log.e("MainActivity", "error starting permission intent", e);
			}
			return;
		} */
		
		//Creating the message list
		ArrayList<MessageInfo> messageList = new ArrayList<>();
		
		//Getting the message details
		String cleanMessageText = messageInputField.getText().toString().trim();
		
		//Returning if there is no data to send
		if(cleanMessageText.isEmpty() && viewModel.draftQueueList.isEmpty()) return;
		
		//Checking if the current service handler is AirMessage bridge
		if(viewModel.conversationInfo.getServiceHandler() == ConversationInfo.serviceHandlerAMBridge) {
			//Checking if the message box has text
			if(!cleanMessageText.isEmpty()) {
				//Creating a message
				messageList.add(new MessageInfo(-1, -1, null, viewModel.conversationInfo, null, cleanMessageText, null, false, System.currentTimeMillis(), Constants.messageStateCodeGhost, Constants.messageErrorCodeOK, false, -1));
				
				//Clearing the message box
				messageInputField.setText("");
				messageInputField.requestLayout(); //Height of input field doesn't update otherwise
				//messageBoxHasText = false;
				
				//Saving the draft message
				//viewModel.applyDraftMessage("");
			}
			
			//Iterating over the drafts
			for(QueuedFileInfo queuedFile : new ArrayList<>(viewModel.draftQueueList)) {
				//Creating the message
				MessageInfo messageInfo = new MessageInfo(-1, -1, null, viewModel.conversationInfo, null, null, null, false, System.currentTimeMillis(), Constants.messageStateCodeGhost, Constants.messageErrorCodeOK, false, -1);
				
				//Creating the attachment
				SimpleAttachmentInfo attachmentFile = queuedFile.getItem();
				AttachmentInfo attachment = ConversationUtils.createAttachmentInfoFromType(-1, null, messageInfo, attachmentFile.getFileName(), attachmentFile.getFileType(), attachmentFile.getFileSize());
				attachment.setDraftingPushRequest(queuedFile.getFilePushRequest());
				
				//Adding the attachment to the message
				messageInfo.addAttachment(attachment);
				
				//Adding the message to the list
				messageList.add(messageInfo);
				
				//Dequeuing the item
				dequeueAttachment(queuedFile, true, false);
				viewModel.conversationInfo.removeDraftFileUpdate(this, queuedFile.getDraftFile(), -1);
			}
		} else {
			//Creating the message
			MessageInfo messageInfo = new MessageInfo(-1, -1, null, viewModel.conversationInfo, null, cleanMessageText.isEmpty() ? null : cleanMessageText, null, false, System.currentTimeMillis(), Constants.messageStateCodeGhost, Constants.messageErrorCodeOK, false, -1);
			
			//Clearing the message box
			if(!cleanMessageText.isEmpty()) {
				messageInputField.setText("");
				messageInputField.requestLayout(); //Height of input field doesn't update otherwise
			}
			
			//Iterating over the drafts
			for(QueuedFileInfo queuedFile : new ArrayList<>(viewModel.draftQueueList)) {
				//Creating the attachment
				SimpleAttachmentInfo attachmentFile = queuedFile.getItem();
				AttachmentInfo attachment = ConversationUtils.createAttachmentInfoFromType(-1, null, messageInfo, attachmentFile.getFileName(), attachmentFile.getFileType(), attachmentFile.getFileSize());
				attachment.setDraftingPushRequest(queuedFile.getFilePushRequest());
				
				//Adding the attachment to the message
				messageInfo.addAttachment(attachment);
				
				//Dequeuing the item
				dequeueAttachment(queuedFile, true, false);
				viewModel.conversationInfo.removeDraftFileUpdate(this, queuedFile.getDraftFile(), -1);
			}
			
			//Adding the message to the list
			messageList.add(messageInfo);
		}
		
		//Returning if there are no items to send
		if(messageList.isEmpty()) return;
		
		//Clearing the conversation's drafts
		viewModel.conversationInfo.clearDraftsUpdate(this);
		
		//Writing the messages to the database
		new AddGhostMessageTask(getApplicationContext(), new GhostMessageFinishHandler()).execute(messageList.toArray(new MessageInfo[0]));
		
		//Scrolling to the bottom of the chat
		messageListAdapter.scrollToBottom();
	}
	
	private void openAttachmentsPanel(boolean restore, boolean animate) {
		//Returning if the conversation is not ready or the panel is already open
		if(viewModel.messagesState.getValue() != ActivityViewModel.messagesStateReady || restore != viewModel.isAttachmentsPanelOpen) return;
		
		//Setting the panel as open
		viewModel.isAttachmentsPanelOpen = true;
		
		//Closing the keyboard
		hideKeyboard();
		
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
			if(Constants.shouldUseAMOLED(this)) findViewById(R.id.pane_attachments).setBackgroundColor(Constants.colorAMOLED);
			
			//Setting up the sections
			setupAttachmentsGallerySection();
			setupAttachmentsAudioSection();
			
			viewModel.attachmentsLocationLoading.observe(this, value -> {
				if(value) setupAttachmentsLocationSection();
			});
			viewModel.attachmentsLocationState.observe(this, value -> {
				if(!viewModel.isAttachmentsLocationLoading()) setupAttachmentsLocationSection();
			});
			viewModel.updateAttachmentsLocationState();
			//setupAttachmentsLocationSection();
			
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
		int windowThreshold = Constants.getWindowHeight(this) - Constants.dpToPx(contentPanelMinAllowanceDP);
		
		//Limiting the panel height
		int targetHeight = Math.min(requestedPanelHeight, windowThreshold);
		
		if(animate) {
			//Animating in the panel
			ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
			animator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
			animator.setInterpolator(new FastOutSlowInInterpolator());
			animator.addUpdateListener(animation -> {
				int value = (int) animation.getAnimatedValue();
				attachmentsPanel.getLayoutParams().height = value;
				attachmentsPanel.requestLayout();
			});
			animator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					attachmentsPanel.setVisibility(View.VISIBLE);
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					attachmentsPanel.getLayoutParams().height = targetHeight;
					attachmentsPanel.requestLayout();
				}
			});
			animator.start();
		} else {
			//Setting the panel height
			attachmentsPanel.getLayoutParams().height = targetHeight;
			
			//Showing the panel
			attachmentsPanel.setVisibility(View.VISIBLE);
		}
	}
	
	private void closeAttachmentsPanel(boolean animate) {
		//Returning if the conversation is not ready or the panel is already closed
		if(viewModel.conversationInfo == null || !viewModel.isAttachmentsPanelOpen) return;
		
		//Setting the panel as closed
		viewModel.isAttachmentsPanelOpen = false;
		
		//Closing the panel
		if(animate) {
			ValueAnimator animator = ValueAnimator.ofInt(attachmentsPanel.getHeight(), 0);
			animator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
			animator.setInterpolator(new FastOutSlowInInterpolator());
			animator.addUpdateListener(animation -> {
				int value = (int) animation.getAnimatedValue();
				attachmentsPanel.getLayoutParams().height = value;
				attachmentsPanel.requestLayout();
			});
			animator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					attachmentsPanel.getLayoutParams().height = 0;
					attachmentsPanel.setVisibility(View.GONE);
				}
			});
			animator.start();
		} else {
			attachmentsPanel.getLayoutParams().height = 0;
			attachmentsPanel.setVisibility(View.GONE);
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
			GridLayoutManager layoutManager = new GridLayoutManager(this, 2, LinearLayoutManager.HORIZONTAL, false);
			layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
				@Override
				public int getSpanSize(int position) {
					List<SimpleAttachmentInfo> items = viewModel.getAttachmentFileList(ActivityViewModel.attachmentTypeGallery);
					int itemCount = items == null ? ActivityViewModel.attachmentsTileCount : items.size();
					return (position == 0 || position == itemCount + 1) ? 2 : 1;
				}
			});
			list.setLayoutManager(layoutManager);
			list.addOnScrollListener(new AttachmentListScrollListener(viewGroup.findViewById(R.id.button_attachment_gallery_systempicker), getResources().getDimensionPixelSize(R.dimen.contenttile_size_double)));
			list.addItemDecoration(new AttachmentsDoubleSpacingDecoration());
			
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
	
	private void setupAttachmentsLocationSection() {
		//Getting the views
		ViewGroup viewGroup = findViewById(R.id.viewgroup_attachment_location);
		if(viewGroup == null) return;
		ViewGroup groupAction = viewGroup.findViewById(R.id.button_attachment_location_action);
		ViewGroup groupContent = viewGroup.findViewById(R.id.frame_attachment_location_content);
		
		//Checking if the process is loading
		if(viewModel.isAttachmentsLocationLoading()) {
			//Switching to the loading view
			groupAction.setVisibility(View.VISIBLE);
			groupContent.setVisibility(View.GONE);
			
			((TextView) groupAction.findViewById(R.id.button_attachment_location_action_label)).setText(R.string.message_generalloading);
			groupAction.setOnClickListener(null);
			
			return;
		}
		
		int state = viewModel.getAttachmentsLocationState();
		if(state == ActivityViewModel.attachmentsLocationStateOK) {
			//Swapping to the content view
			groupAction.setVisibility(View.GONE);
			groupContent.setVisibility(View.VISIBLE);
			
			//Configuring the map
			groupContent.findViewById(R.id.frame_attachment_location_map).setClickable(false);
			MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.frame_attachment_location_map);
			mapFragment.getMapAsync(googleMap -> {
				googleMap.setBuildingsEnabled(true);
				googleMap.getUiSettings().setMapToolbarEnabled(false);
				googleMap.getUiSettings().setAllGesturesEnabled(false);
				googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(viewModel.attachmentsLocationResult.getLatitude(), viewModel.attachmentsLocationResult.getLongitude()), 15));
				googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, Constants.isNightMode(getResources()) ? R.raw.map_plaindark : R.raw.map_plainlight));
			});
			
			groupContent.setOnClickListener(view -> {
				startActivityForResult(new Intent(Messaging.this, LocationPicker.class).putExtra(Constants.intentParamData, viewModel.attachmentsLocationResult), intentPickLocation);
			});
			
			return;
		}
		
		//Swapping to the action view
		groupAction.setVisibility(View.VISIBLE);
		groupContent.setVisibility(View.GONE);
		
		String buttonText;
		View.OnClickListener buttonClickListener;
		switch(state) {
			case ActivityViewModel.attachmentsLocationStatePermission:
				buttonText = getResources().getString(R.string.imperative_permission_location);
				buttonClickListener = view -> Constants.requestPermission(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, permissionRequestLocation);
				break;
			case ActivityViewModel.attachmentsLocationStatePrompt:
				buttonText = getResources().getString(R.string.imperative_enablelocationservices);
				buttonClickListener = view -> viewModel.attachmentsLocationLaunchPrompt(this, intentLocationResolution);
				break;
			case ActivityViewModel.attachmentsLocationStateUnavailable:
				buttonText = getResources().getString(R.string.message_notsupported);
				buttonClickListener = null;
				break;
			case ActivityViewModel.attachmentsLocationStateFetching:
				buttonText = getResources().getString(R.string.message_generalloading);
				buttonClickListener = null;
				break;
			default:
				throw new IllegalArgumentException("Invalid attachment location state " + state + " provided");
		}
		
		//Setting the details
		((TextView) groupAction.findViewById(R.id.button_attachment_location_action_label)).setText(buttonText);
		groupAction.setOnClickListener(buttonClickListener);
	}
	
	private class AttachmentsDoubleSpacingDecoration extends RecyclerView.ItemDecoration {
		@Override
		public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			//Getting the item count
			List<SimpleAttachmentInfo> items = viewModel.getAttachmentFileList(ActivityViewModel.attachmentTypeGallery);
			int itemCount = items == null ? ActivityViewModel.attachmentsTileCount : items.size();
			
			//Adding top margin for items on the bottom row
			int position = parent.getChildLayoutPosition(view);
			if(position != 0 && position != itemCount + 1 && parent.getChildLayoutPosition(view) % 2 == 0) {
				outRect.top = getResources().getDimensionPixelSize(R.dimen.contenttile_margin) / 2;
			}
		}
	}
	
	/**
	 * Gets the available window height: the height of the window, without the app bar
	 *
	 * @return the available window height
	 */
	private int getAvailableWindowHeight() {
		return getWindow().getDecorView().getHeight() - getSupportActionBar().getHeight();
	}
	
	private class AttachmentListScrollListener extends RecyclerView.OnScrollListener {
		boolean buttonIsBubble = false;
		private final CardView pickerView;
		private final int fullHeight;
		
		AttachmentListScrollListener(CardView pickerView, int fullHeight) {
			this.pickerView = pickerView;
			this.fullHeight = fullHeight;
		}
		
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			//boolean isAtStart = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition() == 0;
			boolean isAtStart = !recyclerView.canScrollHorizontally(-1);
			if(buttonIsBubble == isAtStart) {
				buttonIsBubble = !isAtStart;
				setSystemPickerBubbleState(pickerView, buttonIsBubble, fullHeight);
			}
		}
	}
	
	//private float systemPickerBubbleStateSizeTile = -1;
	private float systemPickerBubbleStateRadiusTile = -1;
	private float systemPickerBubbleStateElevationBubble = -1;
	
	void setSystemPickerBubbleState(CardView view, boolean bubble, int tileSize) {
		//Fetching the reference target values if needed
		if(systemPickerBubbleStateRadiusTile == -1) {
			//systemPickerBubbleStateSizeTile = getResources().getDimensionPixelSize(R.dimen.contenttile_size);
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
			sizeTarget = tileSize;
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
		
		//Closing the keyboard
		hideKeyboard();
		
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
			Button buttonChangeColor = inflated.findViewById(R.id.button_changecolor);
			if(Preferences.getPreferenceAdvancedColor(this)) buttonChangeColor.setOnClickListener(view -> showColorDialog(null, viewModel.conversationInfo.getConversationColor()));
			else buttonChangeColor.setVisibility(View.GONE);
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
				buttonView.setIconResource(newState ? R.drawable.unarchive_outlined : R.drawable.archive_outlined);
			});
			
			findViewById(R.id.button_delete).setOnClickListener(view -> {
				//Creating a dialog
				AlertDialog dialog = new MaterialAlertDialogBuilder(this)
						.setMessage(R.string.message_confirm_deleteconversation_current)
						.setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
						.setPositiveButton(R.string.action_delete, (dialogInterface, which) -> {
							//Removing the conversation from memory
							ArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
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
					ColorStateList colorRipple = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, Constants.rippleAlphaInt));
					
					MaterialButton buttonPositive = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
					buttonPositive.setTextColor(color);
					buttonPositive.setRippleColor(colorRipple);
					
					MaterialButton buttonNegative = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
					buttonNegative.setTextColor(color);
					buttonNegative.setRippleColor(colorRipple);
				});
				
				//Showing the dialog
				dialog.show();
			});
			
			//Adding the conversation members
			detailsBuildConversationMembers(new ArrayList<>(viewModel.conversationInfo.getConversationMembers()));
			
			//Setting the panel position
			bottomDetailsPanel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			bottomDetailsPanel.setTranslationY(bottomDetailsPanel.getMeasuredHeight());
		} else {
			((ScrollView) findViewById(R.id.group_messaginginfo_scroll)).fullScroll(ScrollView.FOCUS_UP);
		}
		
		//Finding the views
		FrameLayout panel = bottomDetailsPanel;
		ViewGroup content = findViewById(R.id.group_messaginginfo_content);
		
		//Adding padding on Android Q
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(), currentInsetPaddingBottom);
		}
		
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
			//Showing the details panel
			bottomDetailsPanel.setVisibility(View.VISIBLE);
			bottomDetailsPanel.setTranslationY(0);
			
			//Showing the scrim
			detailScrim.setVisibility(View.VISIBLE);
			detailScrim.setAlpha(1);
			
			//ADding the overscroll listener
			OverScrollScrollView scrollView = panel.findViewById(R.id.group_messaginginfo_scroll);
			scrollView.setOverScrollListener(detailsPanelOverScrollListener);
		} else {
			animateDetailsPanelOpen();
		}
	}
	
	private void animateDetailsPanelOpen() {
		//Getting the animation duration
		long duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
		
		//Animating in the panel
		FrameLayout panel = bottomDetailsPanel;
		panel.animate()
				.translationY(0)
				.setDuration(duration)
				.setInterpolator(new DecelerateInterpolator())
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationStart(Animator animation) {
						panel.setVisibility(View.VISIBLE);
					}
					
					@Override
					public void onAnimationEnd(Animator animation) {
						//Setting the overscroll listener
						OverScrollScrollView scrollView = panel.findViewById(R.id.group_messaginginfo_scroll);
						scrollView.setOverScrollListener(detailsPanelOverScrollListener);
					}
				}).start();
		
		//Animating in the scrim
		detailScrim.animate().alpha(1).withStartAction(() -> detailScrim.setVisibility(View.VISIBLE)).setDuration(duration).start();
	}
	
	private void closeDetailsPanel(boolean animateAccelerate) {
		//Returning if the conversation is not ready or the panel is already closed
		if(viewModel.conversationInfo == null || !viewModel.isDetailsPanelOpen) return;
		
		//Setting the panel as closed
		viewModel.isDetailsPanelOpen = false;
		
		//Starting the animation
		animateDetailsPanelClose(animateAccelerate);
	}
	
	private void animateDetailsPanelClose(boolean animateAccelerate) {
		//Getting the animation duration
		long duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
		
		//Animating out the panel
		FrameLayout panel = bottomDetailsPanel;
		panel.animate()
				.translationY(panel.getHeight())
				.setDuration(duration)
				.setInterpolator(animateAccelerate ? new AccelerateInterpolator() : new DecelerateInterpolator())
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationStart(Animator animation) {
						//Disabling the scroll listener
						OverScrollScrollView scrollView = panel.findViewById(R.id.group_messaginginfo_scroll);
						scrollView.setOverScrollListener(null);
					}
					
					@Override
					public void onAnimationEnd(Animator animation) {
						//Hiding the panel
						panel.setVisibility(View.GONE);
					}
				}).start();
		
		//Animating out the scrim
		detailScrim.animate().alpha(0).withEndAction(() -> detailScrim.setVisibility(View.GONE)).setDuration(duration).start();
	}
	
	void releaseDragDetailsPanel() {
		//Checking if the panel should collapse (more than 100dp dragged down)
		if(bottomDetailsPanel.getTranslationY() > Constants.dpToPx(100)) {
			closeDetailsPanel(false);
		} else {
			animateDetailsPanelOpen();
		}
	}
	
	private boolean checkInputVisibility() {
		return messageInputField.isFocused();
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		//Find the currently focused view, so we can grab the correct window token from it.
		View view = getCurrentFocus();
		//If no view currently has focus, create a new one, just so we can grab a window token from it
		if (view == null) {
			view = new View(this);
		}
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
	
	private void detailsBuildConversationMembers(List<MemberInfo> members) {
		//Getting the members layout
		ViewGroup membersLayout = findViewById(R.id.list_conversationmembers);
		if(membersLayout == null) return;
		membersLayout.removeAllViews();
		
		//Sorting the members
		Collections.sort(members, ConversationUtils.memberInfoComparator);
		
		//Adding the member views
		boolean showMemberColors = members.size() > 1;
		for(int i = 0; i < members.size(); i++) addMemberView(members.get(i), i, showMemberColors);
		/* for(final MemberInfo member : members) {
			//Creating the view
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View memberEntry = inflater.inflate(R.layout.listitem_member, membersLayout, false);
			
			//Setting the default information
			((TextView) memberEntry.findViewById(R.id.label_member)).setText(member.getName());
			((ImageView) memberEntry.findViewById(R.id.profile_default)).setColorFilter(member.getServiceColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			
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
				changeColorButton.setColorFilter(member.getServiceColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
				changeColorButton.setOnClickListener(view -> showColorDialog(member, member.getServiceColor()));
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
	
	private void addMemberView(MemberInfo member, int index, boolean showColor) {
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
			public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
				//Getting the view
				View memberEntry = viewReference.get();
				if(memberEntry == null) return;
				
				//Checking if the user info is invalid
				if(userInfo == null) {
					//Setting the tag (for a new contact request)
					memberEntry.setTag(new ContactAccessInfo(member.getName()));
					return;
				}
				
				//Setting the tag (for a contact view link)
				memberEntry.setTag(new ContactAccessInfo(userInfo.getContactLookupUri()));
				
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
		if(showColor && Preferences.getPreferenceAdvancedColor(this)) {
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
			/* Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData((Uri) view.getTag());
			//intent.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(view.getTag())));
			view.getContext().startActivity(intent); */
			((ContactAccessInfo) view.getTag()).openContact(view.getContext());
		});
		
		//Adding the view
		membersLayout.addView(memberEntry, index);
		memberListViews.put(member, memberEntry);
	}
	
	private void removeMemberView(MemberInfo member) {
		//Getting the members layout
		ViewGroup membersLayout = findViewById(R.id.list_conversationmembers);
		if(membersLayout == null) return;
		
		//Removing the view
		membersLayout.removeView(memberListViews.get(member));
		memberListViews.remove(member);
		
		//Closing the dialog
		if(currentColorPickerDialog != null && currentColorPickerDialogMember == member)
			currentColorPickerDialog.dismiss();
	}
	
	private static class ContactAccessInfo {
		private final Uri accessUri;
		private final String address;
		
		ContactAccessInfo(Uri accessUri) {
			this.accessUri = accessUri;
			address = null;
		}
		
		ContactAccessInfo(String address) {
			accessUri = null;
			this.address = address;
		}
		
		boolean openContact(Context context) {
			if(accessUri != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(accessUri);
				context.startActivity(intent);
				return true;
			} else if(address != null) {
				Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
				intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
				
				if(Constants.validateEmail(address)) intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
				else if(Constants.validatePhoneNumber(address)) intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
				else return false;
				
				//Launching the intent
				if(intent.resolveActivity(context.getPackageManager()) != null) context.startActivity(intent);
				else Toast.makeText(context, R.string.message_intenterror_email, Toast.LENGTH_SHORT).show();
			}
			
			return false;
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent motionEvent) {
		//Checking if there is a recording taking place
		if(viewModel.isRecording()) {
			//Consuming the move event (as to not affect the elements below)
			if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) return true;
			else if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
				//Stopping the recording session
				stopRecording();
				
				//Returning true to consume the event
				return true;
			}
		}
		
		if(bottomDetailsWindowIntercept) {
			if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
				//Assigning the start data
				if(bottomDetailsWindowDragY == -1) {
					bottomDetailsWindowDragY = motionEvent.getY();
					bottomDetailsWindowEndY = bottomDetailsPanel.getHeight();
				} else {
					//Calculating the Y movement delta
					float deltaY = motionEvent.getRawY() - bottomDetailsWindowDragY;
					bottomDetailsWindowDragY = motionEvent.getY();
					
					//Checking if the panel is going to be placed back at its original location
					if(bottomDetailsPanel.getTranslationY() + deltaY <= 0) {
						//Setting the translation and scrim opacity
						bottomDetailsPanel.setTranslationY(0);
						detailScrim.setAlpha(1);
						
						//Cancelling the motion
						bottomDetailsWindowIntercept = false;
						bottomDetailsWindowDragY = -1;
						releaseDragDetailsPanel();
					} else {
						//Setting the bottom panel translation
						bottomDetailsPanel.setTranslationY(Math.min(bottomDetailsPanel.getTranslationY() + deltaY, bottomDetailsWindowEndY));
						
						//Setting the scrim opacity
						detailScrim.setAlpha(1 - (bottomDetailsPanel.getTranslationY() / bottomDetailsPanel.getHeight()));
						
						return true;
					}
				}
			} else if(motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
				bottomDetailsWindowIntercept = false;
				bottomDetailsWindowDragY = -1;
				releaseDragDetailsPanel();
			}
		}
		
		//Calling the super method
		return super.dispatchTouchEvent(motionEvent);
	}
	
	private void colorUI(ViewGroup root) {
		//Returning if the conversation is invalid
		if(viewModel.conversationInfo == null) return;
		
		//Getting the color
		int color = getConversationUIColor();
		int darkerColor = ColorHelper.darkenColor(color);
		int lighterColor = ColorHelper.lightenColor(color);
		
		//Coloring tagged parts of the UI
		for(View view : Constants.getViewsByTag(root, getResources().getString(R.string.tag_primarytint))) {
			if(view instanceof ImageView) ((ImageView) view).setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
			else if(view instanceof Switch) {
				Switch switchView = (Switch) view;
				switchView.setThumbTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0xFFFAFAFA, color}));
				switchView.setTrackTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0x61000000, color}));
			} else if(view instanceof MaterialButton) {
				MaterialButton buttonView = (MaterialButton) view;
				buttonView.setTextColor(color);
				buttonView.setIconTint(ColorStateList.valueOf(color));
				buttonView.setRippleColor(ColorStateList.valueOf(color));
			} else if(view instanceof TextView) ((TextView) view).setTextColor(color);
			else if(view instanceof RelativeLayout) view.setBackground(new ColorDrawable(color));
			else if(view instanceof FrameLayout) view.setBackgroundTintList(ColorStateList.valueOf(color));
		}
		
		//Coloring the unique UI components
		updateSendButton(true);
		if(viewModel.lastUnreadCount == 0) bottomFAB.setImageTintList(ColorStateList.valueOf(color));
		else bottomFAB.setBackgroundTintList(ColorStateList.valueOf(color));
		bottomFABBadge.setBackgroundTintList(ColorStateList.valueOf(darkerColor));
		findViewById(R.id.fab_bottom_splash).setBackgroundTintList(ColorStateList.valueOf(lighterColor));
		
		//Coloring the info bars
		infoBarConnection.setColor(color);
	}
	
	int getConversationUIColor() {
		int color = viewModel.conversationInfo.getDisplayConversationColor(this);
		if(Constants.isNightMode(getResources()) && Preferences.getPreferenceAdvancedColor(this)) color = ColorHelper.darkModeLightenColor(color); //Standard colors have night mode overrides by default
		return color;
	}
	
	private void setActionBarTitle(String title) {
		/* Spannable text = new SpannableString(title);
		text.setSpan(new ForegroundColorSpan(viewModel.conversationInfo.getConversationColor()), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		getSupportActionBar().setTitle(text); */
		getSupportActionBar().setTitle(title);
	}
	
	public void onClickRetryLoad(View view) {
		int state = viewModel.messagesState.getValue();
		if(state == ActivityViewModel.messagesStateFailedMatching) viewModel.findCreateConversationMMSSMS();
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
		boolean state = !messageInputField.getText().toString().trim().isEmpty() || !viewModel.draftQueueList.isEmpty();
		if(currentSendButtonState == state && !restore) return;
		currentSendButtonState = state;
		
		//Updating the button
		buttonSendMessage.setClickable(state);
		int targetColor = state ? getConversationUIColor() : Constants.resolveColorAttr(this, android.R.attr.colorControlNormal);
		/* if(restore) buttonSendMessage.setColorFilter(targetColor);
		else {
			int startColor = state ? Constants.resolveColorAttr(this, android.R.attr.colorControlNormal) : viewModel.conversationInfo.getConversationColor();
			ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, targetColor);
			animator.setDuration(100);
			animator.addUpdateListener(animatorStage -> buttonSendMessage.setColorFilter((int) animatorStage.getAnimatedValue()));
			animator.start();
		} */
		buttonSendMessage.setColorFilter(targetColor);
		buttonSendMessage.setAlpha(state ? 1 : 0.5F);
		
		//buttonSendMessage.setImageTintList(ColorStateList.valueOf(state ? getResources().getServiceColor(R.color.colorPrimary, null) : Constants.resolveColorAttr(this, android.R.attr.colorControlNormal)));
		//if(restore) buttonSendMessage.setAlpha(targetAlpha);
		//else buttonSendMessage.animate().setDuration(100).alpha(targetAlpha);
	}
	
	private void rebuildContactViews() {
		//Returning if the messages are not ready
		if(viewModel.conversationItemList == null) return;
		
		//Updating the title
		viewModel.conversationInfo.buildTitle(this, new ConversationTitleResultCallback(this));
		
		//Rebuilding the conversation members
		detailsBuildConversationMembers(new ArrayList<>(viewModel.conversationInfo.getConversationMembers()));
		
		//Updating the views
		messageListAdapter.notifyDataSetChanged();
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
		//AirMessage bridge
		if(viewModel.conversationInfo.getServiceHandler() == ConversationInfo.serviceHandlerAMBridge) {
			//Returning a generic message if the service is invalid
			if(viewModel.conversationInfo.getService() == null) return getResources().getString(R.string.imperative_messageinput);
			
			switch(viewModel.conversationInfo.getService()) {
				case ConversationInfo.serviceTypeAppleMessage:
					//iMessage
					return getResources().getString(R.string.title_imessage);
					//SMS bridge
				case ConversationInfo.serviceTypeAppleTextMessageForwarding:
					return getResources().getString(R.string.title_textmessageforwarding);
				default:
					return viewModel.conversationInfo.getService();
			}
		}
		//System messaging
		else if(viewModel.conversationInfo.getServiceHandler() == ConversationInfo.serviceHandlerSystemMessaging) {
			switch(viewModel.conversationInfo.getService()) {
				case ConversationInfo.serviceTypeSystemMMSSMS:
					return getResources().getString(R.string.title_textmessage);
				case ConversationInfo.serviceTypeSystemRCS:
					return getResources().getString(R.string.title_rcs);
				default:
					return viewModel.conversationInfo.getService();
			}
		}
		
		//Returning a generic message
		return getResources().getString(R.string.imperative_messageinput);
	}
	
	void showServerWarning(int reason) {
		switch(reason) {
			case ConnectionManager.intentResultCodeInternalException:
				infoBarConnection.setText(R.string.message_serverstatus_internalexception);
				infoBarConnection.setButton(R.string.action_retry, view -> reconnectService());
				break;
			case ConnectionManager.intentResultCodeBadRequest:
				infoBarConnection.setText(R.string.message_serverstatus_badrequest);
				infoBarConnection.setButton(R.string.action_retry, view -> reconnectService());
				break;
			case ConnectionManager.intentResultCodeClientOutdated:
				infoBarConnection.setText(R.string.message_serverstatus_clientoutdated);
				infoBarConnection.setButton(R.string.action_update, view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()))));
				break;
			case ConnectionManager.intentResultCodeServerOutdated:
				infoBarConnection.setText(R.string.message_serverstatus_serveroutdated);
				infoBarConnection.setButton(R.string.screen_help, view -> startActivity(new Intent(Intent.ACTION_VIEW, Constants.serverUpdateAddress)));
				break;
			case ConnectionManager.intentResultCodeUnauthorized:
				infoBarConnection.setText(R.string.message_serverstatus_authfail);
				infoBarConnection.setButton(R.string.action_reconfigure, view -> startActivity(new Intent(Messaging.this, ServerSetup.class)));
				break;
			case ConnectionManager.intentResultCodeConnection:
				infoBarConnection.setText(R.string.message_serverstatus_noconnection);
				infoBarConnection.setButton(R.string.action_retry, view -> reconnectService());
				break;
			default:
				infoBarConnection.setText(R.string.message_serverstatus_unknown);
				infoBarConnection.setButton(R.string.action_retry, view -> reconnectService());
				break;
		}
		
		//Showing the info bar
		infoBarConnection.show();
	}
	
	private void reconnectService() {
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager == null) {
			//Starting the service
			startService(new Intent(Messaging.this, ConnectionService.class));
		} else {
			//Reconnecting
			connectionManager.reconnect(this);
			
			//Hiding the bar
			hideServerWarning();
		}
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
		bottomFAB.setBackgroundTintList(ColorStateList.valueOf(getConversationUIColor()));
		bottomFAB.setImageTintList(ColorStateList.valueOf(Constants.resolveColorAttr(this, R.attr.colorOnPrimary)));
		
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
		int colorTint = getConversationUIColor();
		
		//Checking if there are any unread messages
		if(viewModel.conversationInfo.getUnreadMessageCount() > 0) {
			//Coloring the FAB
			bottomFAB.setBackgroundTintList(ColorStateList.valueOf(colorTint));
			bottomFAB.setImageTintList(ColorStateList.valueOf(Constants.resolveColorAttr(this, R.attr.colorOnPrimary)));
			
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
					AlertDialog dialog = new MaterialAlertDialogBuilder(Messaging.this)
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
						int color = getConversationUIColor();
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
			case permissionRequestLocation: {
				//Checking if the request was granted
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//Updating the attachment section
					viewModel.updateAttachmentsLocationState();
					//setupAttachmentsLocationSection();
				}
			}
			default:
				viewModel.callPermissionsRequestListener(requestCode - permissionRequestMessageCustomOffset, grantResults[0] == PackageManager.PERMISSION_GRANTED);
		}
	}
	
	private void requestCamera(boolean video) {
		//Creating the intent
		Intent cameraCaptureIntent = new Intent(video ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
		
		//Checking if there are no apps that can take the intent
		if(cameraCaptureIntent.resolveActivity(getPackageManager()) == null) {
			//Telling the user via a toast
			Toast.makeText(Messaging.this, R.string.message_intenterror_camera, Toast.LENGTH_SHORT).show();
			
			//Returning
			return;
		}
		
		//Finding a free file
		viewModel.targetFileIntent = MainApplication.getDraftTarget(this, viewModel.conversationID, video ? Constants.videoName : Constants.pictureName);
		
		//Setting the output file target
		Uri targetUri = FileProvider.getUriForFile(this, MainApplication.fileAuthority, viewModel.targetFileIntent);
		cameraCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, targetUri);
		
		//Starting the activity
		startActivityForResult(cameraCaptureIntent, intentTakePicture);
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
		//Playing a sound
		viewModel.playSound(ActivityViewModel.soundRecordingStart);
		
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
		
		//Resetting the time label
		recordingTimeLabel.setText(DateUtils.formatElapsedTime(0));
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
	
	public class MessageListRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the reference values
		private static final int itemTypeLoadingBar = -1;
		private static final int itemTypeReplySuggestions = -2;
		
		//Creating the values
		private ArrayList<ConversationItem> conversationItems = null;
		private RecyclerView recyclerView;
		
		//Creating the pools
		private final SparseArray<Pools.SimplePool<? extends RecyclerView.ViewHolder>> componentPoolList = new SparseArray<>();
		private final PoolSource poolSource = new PoolSource();
		
		//Creating the states
		private boolean replySuggestionsAvailable;
		
		public MessageListRecyclerAdapter() {
			//Enabling stable IDs
			setHasStableIds(true);
		}
		
		public void setItemList(ArrayList<ConversationItem> items) {
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
			
			//Getting the reply suggestions available state
			replySuggestionsAvailable = viewModel.isSmartReplyAvailable();
		}
		
		@Override
		@NonNull
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			//Returning the correct view holder
			switch(viewType) {
				case ConversationItem.viewTypeMessage:
					return new MessageInfo.ViewHolder(LayoutInflater.from(Messaging.this).inflate(R.layout.listitem_message, parent, false));
				case ConversationItem.viewTypeAction:
					return new ConversationUtils.ActionLineViewHolder(LayoutInflater.from(Messaging.this).inflate(R.layout.listitem_action, parent, false));
				case itemTypeLoadingBar: {
					View loadingView = LayoutInflater.from(Messaging.this).inflate(R.layout.listitem_loading, parent, false);
					((ProgressBar) loadingView.findViewById(R.id.progressbar)).setIndeterminateTintList(ColorStateList.valueOf(getConversationUIColor()));
					return new LoadingViewHolder(loadingView);
				}
				case itemTypeReplySuggestions:
					return new ReplySuggestionsViewHolder(LayoutInflater.from(Messaging.this).inflate(R.layout.listitem_replysuggestions, parent, false));
				default:
					throw new IllegalArgumentException("Invalid view type received: " + viewType);
			}
		}
		
		@Override
		public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
			if(holder instanceof MessageInfo.ViewHolder) {
				((MessageInfo.ViewHolder) holder).cleanupState();
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			//Returning if there are no items or the item is the loading spinner
			int itemType = getItemViewType(position);
			if(conversationItems == null || itemType == itemTypeLoadingBar) return;
			
			//Checking if the item is the suggestions
			if(itemType == itemTypeReplySuggestions) {
				ReplySuggestionsViewHolder viewHolder = (ReplySuggestionsViewHolder) holder;
				viewHolder.setSuggestions(Messaging.this, viewModel, viewModel.lastSmartReplyResult);
				viewHolder.resetScroll();
			} else {
				//Getting the item
				ConversationItem conversationItem;
				if(viewModel.isProgressiveLoadInProgress()) conversationItem = conversationItems.get(position - 1);
				else conversationItem = conversationItems.get(position);
				
				//Checking if the item is a message
				boolean isMessage = false;
				if(conversationItem instanceof MessageInfo) {
					((MessageInfo.ViewHolder) holder).setPoolSource(poolSource);
					isMessage = true;
				}
				
				//Creating the view
				conversationItem.bindView(holder, Messaging.this);
				
				//Setting the view source
				conversationItem.setViewHolderSource(new Constants.ViewHolderSourceImpl<RecyclerView.ViewHolder>(recyclerView, conversationItem.getLocalID()));
				
				//Checking if the item is a message
				if(isMessage) {
					MessageInfo messageInfo = (MessageInfo) conversationItem;
					//Playing the message's effect if it hasn't been viewed yet
					if(messageInfo.getSendStyle() != null && !messageInfo.getSendStyleViewed()) {
						messageInfo.setSendStyleViewed(true);
						messageInfo.playEffect((MessageInfo.ViewHolder) holder);
					/* if(Constants.validateScreenEffect(messageInfo.getSendStyle())) playScreenEffect(messageInfo.getSendStyle());
					else messageInfo.playEffect(); */
						new TaskMarkMessageSendStyleViewed().execute(messageInfo);
					}
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
			if(viewModel.isSmartReplyAvailable()) size += 1;
			return size;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(conversationItems == null) return 0;
			if(isViewLoadingSpinner(position)) return itemTypeLoadingBar;
			if(isViewReplySuggestions(position)) return itemTypeReplySuggestions;
			return conversationItems.get(position - (viewModel.isProgressiveLoadInProgress() ? 1 : 0)).getItemViewType();
		}
		
		@Override
		public long getItemId(int position) {
			if(conversationItems == null) return 0;
			if(isViewLoadingSpinner(position)) return itemTypeLoadingBar;
			if(isViewReplySuggestions(position)) return itemTypeReplySuggestions;
			return conversationItems.get(position - (viewModel.isProgressiveLoadInProgress() ? 1 : 0)).getLocalID();
		}
		
		private boolean isViewLoadingSpinner(int position) {
			return viewModel.isProgressiveLoadInProgress() && position == 0;
		}
		
		private boolean isViewReplySuggestions(int position) {
			return viewModel.isSmartReplyAvailable() && position + 1 == getItemCount();
		}
		
		public final class PoolSource {
			static final int poolSize = 12;
			
			public MessageComponent.ViewHolder getComponent(MessageComponent<MessageComponent.ViewHolder> component, ViewGroup parent, Context context) {
				Pools.SimplePool<MessageComponent.ViewHolder> pool = (Pools.SimplePool<MessageComponent.ViewHolder>) componentPoolList.get(component.getItemViewType());
				if(pool == null) return component.createViewHolder(context, parent);
				else {
					MessageComponent.ViewHolder viewHolder = pool.acquire();
					return viewHolder == null ? component.createViewHolder(context, parent) : viewHolder;
				}
			}
			
			public void releaseComponent(int componentViewType, MessageComponent.ViewHolder viewHolder) {
				Pools.SimplePool<MessageComponent.ViewHolder> pool = (Pools.SimplePool<MessageComponent.ViewHolder>) componentPoolList.get(componentViewType);
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
			return recyclerView != null && recyclerView.getLayoutManager() != null && ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition() == getItemCount() - 1;
		}
		
		boolean isDirectlyBelowFrame(int index) {
			return recyclerView != null && recyclerView.getLayoutManager() != null && ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition() + 1 == index;
		}
		
		void scrollToBottom() {
			//Returning if the list has already been scrolled to the bottom
			//if(isScrolledToBottom()) return;
			
			//Returning if the list cannot be scrolled
			if(recyclerView == null || getItemCount() == 0) return;
			
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
		
		abstract void onOverflowButtonClick();
		
		abstract View inflateActionButton(@NonNull ViewGroup parent);
		
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
					return new ViewHolderImpl(inflateActionButton(parent));
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
						if(draftIndex != -1) tileHolder.setSelected(getResources(), true, draftIndex + 1);
						else tileHolder.setDeselected(getResources(), true);
						
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
						viewHolder.setSelected(getResources(), true, newDraftIndex + 1);
					} else {
						//Removing the item
						dequeueAttachment(item, false, true);
						newDraftIndex = -1;
						
						//Setting the selection
						viewHolder.setDeselected(getResources(), true);
						
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
			for(QueuedFileInfo queuedItem : queueList)
				for(ListIterator<SimpleAttachmentInfo> loadedIterator = fileList.listIterator(); loadedIterator.hasNext(); ) {
					//Getting the item information
					int loadedIndex = loadedIterator.nextIndex();
					if(usesActionButton()) loadedIndex += 1; //The action button takes up the first slot at the start
					SimpleAttachmentInfo loadedItem = loadedIterator.next();
					
					//Skipping the remainder of the iteration if the items don't match
					if(!loadedItem.compare(queuedItem)) continue;
					
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
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager == null) {
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
		FilePushRequest request = item.getFile() != null ?
													new FilePushRequest(item.getFile(), item.getFileType(), item.getFileName(), item.getModificationDate(), viewModel.conversationInfo, -1, -1, FilePushRequest.stateLinked, updateTime, false, viewModel.doFilesRequireCompression()) :
													new FilePushRequest(item.getUri(), item.getFileType(), item.getFileName(), item.getModificationDate(), viewModel.conversationInfo, -1, -1, FilePushRequest.stateLinked, updateTime, false, viewModel.doFilesRequireCompression());
		request.getCallbacks().onFail = (result, details) -> {
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
		connectionManager.addFileProcessingRequest(request);
		
		//Animating the list
		if(listStartedEmpty) {
			if(currentListAttachmentQueueValueAnimator != null)
				currentListAttachmentQueueValueAnimator.cancel();
			
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
	
	int dequeueAttachment(SimpleAttachmentInfo attachmentInfo, boolean updateListing, boolean updateElsewhere) {
		//Removing the item
		int queuedItemIndex = -1;
		QueuedFileInfo queuedItem = null;
		for(ListIterator<QueuedFileInfo> iterator = viewModel.draftQueueList.listIterator(); iterator.hasNext();) {
			int index = iterator.nextIndex();
			QueuedFileInfo item = iterator.next();
			
			if(attachmentInfo.compare(item)) {
				iterator.remove();
				listAttachmentQueue.getAdapter().notifyItemRemoved(index);
				
				queuedItemIndex = index;
				queuedItem = item;
				break;
			}
		}
		
		//Returning if no item was found
		if(queuedItemIndex == -1) return -1;
		
		//Dequeuing the item
		return dequeueAttachment(queuedItem, queuedItemIndex, updateListing, updateElsewhere);
	}
	
	int dequeueAttachment(QueuedFileInfo queuedItem, boolean updateListing, boolean updateElsewhere) {
		//Getting the item index
		int queuedItemIndex = viewModel.draftQueueList.indexOf(queuedItem);
		
		//Returning if no item was found
		if(queuedItemIndex == -1) return -1;
		
		//Removing the item
		viewModel.draftQueueList.remove(queuedItemIndex);
		listAttachmentQueue.getAdapter().notifyItemRemoved(queuedItemIndex);
		
		//Dequeuing the item
		return dequeueAttachment(queuedItem, queuedItemIndex, updateListing, updateElsewhere);
	}
	
	private int dequeueAttachment(QueuedFileInfo queuedItem, int queuedItemIndex, boolean updateListing, boolean updateElsewhere) {
		//Removing the draft from the conversation and from disk
		if(updateElsewhere && queuedItem.getDraftFile() != null) {
			//Getting the connection manager
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			if(connectionManager == null) {
				//Starting the service
				((MainApplication) getApplication()).startConnectionService();
				
				return -1;
			}
			
			//Adding the request
			long updateTime = System.currentTimeMillis();
			FileProcessingRequest request = new FileRemovalRequest(queuedItem.getDraftFile(), updateTime);
			final QueuedFileInfo finalQueuedItem = queuedItem;
			request.getCallbacks().onRemovalFinish = () -> {
				//Removing the draft from the conversation in memory
				if(viewModel.conversationInfo != null) viewModel.conversationInfo.removeDraftFileUpdate(Messaging.this, finalQueuedItem.getDraftFile(), updateTime);
			};
			connectionManager.addFileProcessingRequest(request);
		}
		
		//Updating the listings
		if(updateListing) {
			//Updating the relevant adapters
			List<AttachmentsRecyclerAdapter<?>> adapterList = new ArrayList<>();
			for(int i = queuedItemIndex; i < viewModel.draftQueueList.size(); i++) {
				QueuedFileInfo listedItem = viewModel.draftQueueList.get(i);
				if(listedItem.getItem().getListAdapter() == null || adapterList.contains(listedItem.getItem().getListAdapter())) continue;
				adapterList.add(listedItem.getItem().getListAdapter());
			}
			for(AttachmentsRecyclerAdapter<?> adapter : adapterList) adapter.recalculateIndices();
			
			//Updating the item selection
			SimpleAttachmentInfo attachmentInfo = queuedItem.getItem();
			if(attachmentInfo.getListAdapter() != null) attachmentInfo.getListAdapter().notifyItemChanged(attachmentInfo.getListIndex(), AttachmentsRecyclerAdapter.payloadUpdateSelection);
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
		return queuedItemIndex;
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
				case AttachmentTileHelper.viewTypeContact:
					tileViewHolder = attachmentsContactTileHelper.createViewHolder(container);
					break;
				case AttachmentTileHelper.viewTypeLocation:
					tileViewHolder = attachmentsLocationTileHelper.createViewHolder(container);
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
			fileInfo.getTileHelper().bindView(Messaging.this, holder.contentViewHolder, fileInfo.item);
			
			//Hooking up the remove button
			holder.buttonRemove.setOnClickListener(view -> dequeueAttachment(fileInfo, true, true));
			
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
				
				int contentWidth = (int) (contentViewHolder.itemView.getLayoutParams().width * scale);
				container.getLayoutParams().width = contentWidth;
				itemView.getLayoutParams().width = contentWidth + Constants.dpToPx(30);
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
		private FilePushRequest filePushRequest;
		private DraftFile draftFile;
		
		QueuedFileInfo(AttachmentTileHelper tileHelper, SimpleAttachmentInfo item) {
			this.tileHelper = tileHelper;
			this.item = item;
		}
		
		QueuedFileInfo(DraftFile draft) {
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
		
		FilePushRequest getFilePushRequest() {
			return filePushRequest;
		}
		
		void setFilePushRequest(FilePushRequest request) {
			filePushRequest = request;
		}
		
		DraftFile getDraftFile() {
			return draftFile;
		}
		
		void setDraftFile(DraftFile draftFile) {
			this.draftFile = draftFile;
		}
	}
	
	private static abstract class AttachmentTileHelper<VH extends RecyclerView.ViewHolder> {
		//Creating the reference values
		static final int viewTypeMedia = 0;
		static final int viewTypeDocument = 1;
		static final int viewTypeAudio = 2;
		static final int viewTypeContact = 3;
		static final int viewTypeLocation = 4;
		
		abstract VH createViewHolder(ViewGroup parent);
		
		abstract void bindView(Context context, VH viewHolder, SimpleAttachmentInfo item);
		
		abstract int getViewType();
	}
	
	AttachmentTileHelper<?> findAppropriateTileHelper(String mimeType) {
		if(mimeType == null) return attachmentsDocumentTileHelper;
		
		if(Constants.compareMimeTypes(mimeType, mimeTypeImage) || Constants.compareMimeTypes(mimeType, mimeTypeVideo)) return attachmentsMediaTileHelper;
		else if(Constants.compareMimeTypes(mimeType, mimeTypeAudio)) return attachmentsAudioTileHelper;
		else if(ContactAttachmentInfo.checkFileApplicability(mimeType, null)) return attachmentsContactTileHelper;
		else if(Constants.compareMimeTypes(mimeType, mimeTypeVLocation)) return attachmentsLocationTileHelper;
		
		return attachmentsDocumentTileHelper;
	}
	
	private static abstract class AttachmentTileViewHolder extends RecyclerView.ViewHolder {
		//Creating the reference values
		private static final float selectedScale = 0.85F;
		
		//Creating the view values
		private ViewGroup groupSelection;
		private TextView labelSelection;
		
		AttachmentTileViewHolder(View itemView) {
			super(itemView);
		}
		
		void setSelected(Resources resources, boolean animate, int index) {
			//Returning if the view state is already selected
			if(groupSelection != null && groupSelection.getVisibility() == View.VISIBLE) return;
			
			//Inflating the view if it hasn't yet been
			if(groupSelection == null) {
				groupSelection = (ViewGroup) ((ViewStub) itemView.findViewById(R.id.viewstub_selection)).inflate();
				labelSelection = groupSelection.findViewById(R.id.label_selectionindex);
			}
			
			//Showing the view
			if(animate) {
				int duration = resources.getInteger(android.R.integer.config_shortAnimTime);
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
			
			labelSelection.setText(Constants.intToFormattedString(resources, index));
		}
		
		void setDeselected(Resources resources, boolean animate) {
			//Returning if the view state is already deselected
			if(groupSelection == null || groupSelection.getVisibility() == View.GONE) return;
			
			//Hiding the view
			if(animate) {
				int duration = resources.getInteger(android.R.integer.config_shortAnimTime);
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
		
		/* @Override
		View inflateActionButton(@NonNull ViewGroup parent) {
			View actionButton = getLayoutInflater().inflate(R.layout.listitem_attachment_actiontile, parent, false);
			TextView label = actionButton.findViewById(R.id.label);
			label.setText(R.string.part_camera);
			label.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.camera, 0, 0);
			actionButton.setOnClickListener(view -> requestTakePicture());
			return actionButton;
		} */
		
		View inflateActionButton(@NonNull ViewGroup parent) {
			View actionButton = getLayoutInflater().inflate(R.layout.listitem_attachment_actiontile_double, parent, false);
			{
				ViewGroup group = actionButton.findViewById(R.id.group_1);
				TextView label = group.findViewById(R.id.label_1);
				label.setText(R.string.action_short_picture);
				label.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.camera_outlined, 0, 0);
				group.setOnClickListener(view -> requestCamera(false));
			}
			{
				ViewGroup group = actionButton.findViewById(R.id.group_2);
				TextView label = group.findViewById(R.id.label_2);
				label.setText(R.string.action_short_video);
				label.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.videocam_outlined, 0, 0);
				group.setOnClickListener(view -> requestCamera(true));
			}
			return actionButton;
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
			tileHelper.bindView(Messaging.this, viewHolder, file);
			
			//Setting the selection state
			if(draftIndex == -1) viewHolder.setDeselected(getResources(), false);
			else viewHolder.setSelected(getResources(), false, draftIndex + 1);
		}
		
		@Override
		AttachmentTileHelper<?> getTileHelper() {
			return tileHelper;
		}
	}
	
	/**
	 * Retrieves the index of the selected attachment item
	 *
	 * @param item the selected item
	 * @return the index of the selected
	 */
	private int getDraftItemIndex(SimpleAttachmentInfo item) {
		for(int i = 0; i < viewModel.draftQueueList.size(); i++) {
			if(item.compare(viewModel.draftQueueList.get(i))) return i;
		}
		return -1;
	}
	
	private static class AttachmentsMediaTileViewHolder extends AttachmentTileViewHolder {
		//Creating the view values
		private final ImageView imageThumbnail;
		private final ImageView imageFlagGIF;
		private final ViewGroup groupVideo;
		private final TextView labelVideo;
		
		AttachmentsMediaTileViewHolder(View itemView) {
			super(itemView);
			imageThumbnail = itemView.findViewById(R.id.image);
			imageFlagGIF = itemView.findViewById(R.id.image_flag_gif);
			
			groupVideo = itemView.findViewById(R.id.group_flag_video);
			labelVideo = groupVideo.findViewById(R.id.label_flag_video);
		}
	}
	
	private static final AttachmentTileHelper<AttachmentsMediaTileViewHolder> attachmentsMediaTileHelper = new AttachmentTileHelper<AttachmentsMediaTileViewHolder>() {
		@Override
		AttachmentsMediaTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsMediaTileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_attachment_mediatile, parent, false));
		}
		
		@Override
		void bindView(Context context, AttachmentsMediaTileViewHolder viewHolder, SimpleAttachmentInfo item) {
			//Returning if the item is invalid
			if(item == null) return;
			
			//Returning if the context is invalid
			//if(isFinishing() || isDestroyed()) return;
			if(!Constants.validateContext(context)) return;
			
			//Setting the image thumbnail
			Glide.with(context)
					.load(item.getFile() != null ? item.getFile() : item.getUri())
					.apply(RequestOptions.centerCropTransform())
					.transition(DrawableTransitionOptions.withCrossFade())
					.into(viewHolder.imageThumbnail);
			
			//Setting the image flags
			if(Constants.compareMimeTypes(item.getFileType(), mimeTypeGIF)) {
				viewHolder.imageFlagGIF.setVisibility(View.VISIBLE);
				viewHolder.groupVideo.setVisibility(View.GONE);
			} else if(Constants.compareMimeTypes(item.getFileType(), mimeTypeVideo)) {
				viewHolder.imageFlagGIF.setVisibility(View.GONE);
				viewHolder.groupVideo.setVisibility(View.VISIBLE);
				viewHolder.labelVideo.setText(DateUtils.formatElapsedTime(((SimpleAttachmentInfo.VideoExtension) item.getExtension()).mediaDuration / 1000));
			} else {
				viewHolder.imageFlagGIF.setVisibility(View.GONE);
				viewHolder.groupVideo.setVisibility(View.GONE);
			}
		}
		
		@Override
		int getViewType() {
			return viewTypeMedia;
		}
	};
	
	private static class AttachmentsDocumentTileViewHolder extends AttachmentTileViewHolder {
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
	
	private static final AttachmentTileHelper<AttachmentsDocumentTileViewHolder> attachmentsDocumentTileHelper = new AttachmentTileHelper<AttachmentsDocumentTileViewHolder>() {
		@Override
		AttachmentsDocumentTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsDocumentTileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_attachment_documenttile, parent, false));
		}
		
		@Override
		void bindView(Context context, AttachmentsDocumentTileViewHolder viewHolder, SimpleAttachmentInfo item) {
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
					case "application/vnd.openxmlformats-officedocument.wordprocessingml.template":
					case "application/vnd.ms-word.document.macroEnabled.12":
					case "application/vnd.ms-word.template.macroEnabled.12":
						iconResource = R.drawable.file_msword;
						viewColorBG = R.color.tile_blue_bg;
						viewColorFG = R.color.tile_blue_fg;
						break;
					case "application/vnd.ms-excel":
					case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
					case "application/vnd.openxmlformats-officedocument.spreadsheetml.template":
					case "application/vnd.ms-excel.sheet.macroEnabled.12":
					case "application/vnd.ms-excel.sheet.binary.macroEnabled.12":
					case "application/vnd.ms-excel.template.macroEnabled.12":
					case "application/vnd.ms-excel.addin.macroEnabled.12":
						iconResource = R.drawable.file_msexcel;
						viewColorBG = R.color.tile_green_bg;
						viewColorFG = R.color.tile_green_fg;
						break;
					case "application/vnd.ms-powerpoint":
					case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
					case "application/vnd.openxmlformats-officedocument.presentationml.template":
					case "application/vnd.openxmlformats-officedocument.presentationml.slideshow":
					case "application/vnd.ms-powerpoint.addin.macroEnabled.12":
					case "application/vnd.ms-powerpoint.presentation.macroEnabled.12":
					case "application/vnd.ms-powerpoint.template.macroEnabled.12":
					case "application/vnd.ms-powerpoint.slideshow.macroEnabled.12":
						iconResource = R.drawable.file_mspowerpoint;
						viewColorBG = R.color.tile_yellow_bg;
						viewColorFG = R.color.tile_yellow_fg;
						break;
				}
			}
			
			//Resolving the color resources
			viewColorBG = context.getResources().getColor(viewColorBG, null);
			viewColorFG = context.getResources().getColor(viewColorFG, null);
			if(Constants.isNightMode(context.getResources())) {
				int temp = viewColorBG;
				viewColorBG = viewColorFG;
				viewColorFG = temp;
			}
			
			//Filling in the view data
			viewHolder.documentName.setText(item.getFileName());
			viewHolder.documentName.setTextColor(viewColorFG);
			
			viewHolder.documentIcon.setImageResource(iconResource);
			viewHolder.documentIcon.setImageTintList(ColorStateList.valueOf(viewColorFG));
			
			viewHolder.documentSize.setText(Formatter.formatFileSize(context, item.getFileSize()));
			viewHolder.documentSize.setTextColor(viewColorFG);
			
			viewHolder.itemView.setBackgroundTintList(ColorStateList.valueOf(viewColorBG));
		}
		
		@Override
		int getViewType() {
			return viewTypeDocument;
		}
	};
	
	private static class AttachmentsAudioTileViewHolder extends AttachmentTileViewHolder {
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
	
	private static final AttachmentTileHelper<AttachmentsAudioTileViewHolder> attachmentsAudioTileHelper = new AttachmentTileHelper<AttachmentsAudioTileViewHolder>() {
		@Override
		AttachmentsAudioTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsAudioTileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_attachment_audiotile, parent, false));
		}
		
		@Override
		void bindView(Context context, AttachmentsAudioTileViewHolder viewHolder, SimpleAttachmentInfo item) {
			//Returning if the item is invalid
			if(item == null) return;
			
			Messaging activity = (Messaging) context;
			
			//Getting the extension
			SimpleAttachmentInfo.AudioExtension extension = (SimpleAttachmentInfo.AudioExtension) item.getExtension();
			
			//Binding the view
			extension.updateViewPlaying(viewHolder);
			extension.updateViewProgress(viewHolder);
			
			//Creating the view holder source
			Constants.ViewHolderSource<AttachmentsAudioTileViewHolder> viewHolderSource = () -> {
				if(activity.listAttachmentQueue == null) return null;
				int itemIndex = findAttachmentInQueue(activity, item);
				if(itemIndex == -1) return null;
				return (AttachmentsAudioTileViewHolder) ((AttachmentsQueueRecyclerAdapter.QueueTileViewHolder) activity.listAttachmentQueue.findViewHolderForAdapterPosition(itemIndex)).contentViewHolder;
			};
			
			//Finding the queued file info
			QueuedFileInfo queuedInfo = null;
			for(QueuedFileInfo allQueued : activity.viewModel.draftQueueList) {
				if(allQueued.getItem() != item) continue;
				queuedInfo = allQueued;
				break;
			}
			if(queuedInfo == null) return;
			
			//Setting the click listener
			final QueuedFileInfo finalQueuedFileInfo = queuedInfo;
			viewHolder.itemView.setOnClickListener(view -> extension.play(activity.viewModel.audioPlaybackManager, finalQueuedFileInfo, viewHolderSource));
		}
		
		private int findAttachmentInQueue(Messaging activity, SimpleAttachmentInfo item) {
			int queuedIndex;
			QueuedFileInfo queuedItem;
			for(ListIterator<QueuedFileInfo> iterator = activity.viewModel.draftQueueList.listIterator(); iterator.hasNext(); ) {
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
	
	private static class AttachmentsLocationTileViewHolder extends AttachmentTileViewHolder {
		//Creating the view values
		final TextView labelName;
		
		AttachmentsLocationTileViewHolder(View itemView) {
			super(itemView);
			
			labelName = itemView.findViewById(R.id.label_name);
		}
	}
	
	private static final AttachmentTileHelper<AttachmentsLocationTileViewHolder> attachmentsLocationTileHelper = new AttachmentTileHelper<AttachmentsLocationTileViewHolder>() {
		@Override
		AttachmentsLocationTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsLocationTileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_attachment_locationtile, parent, false));
		}
		
		@Override
		void bindView(Context context, AttachmentsLocationTileViewHolder viewHolder, SimpleAttachmentInfo item) {
			//Returning if the file is invalid
			if(item == null) return;
			
			//Getting the extension data
			SimpleAttachmentInfo.LocationExtension extension = (SimpleAttachmentInfo.LocationExtension) item.getExtension();
			
			//Setting the location name label
			if(extension.locationName == null) viewHolder.labelName.setText(R.string.part_content_location);
			else viewHolder.labelName.setText(extension.locationName);
		}
		
		@Override
		int getViewType() {
			return viewTypeLocation;
		}
	};
	
	private static class AttachmentsContactTileViewHolder extends AttachmentTileViewHolder {
		//Creating the view values
		final ImageView iconProfile;
		final ImageView iconPlaceholder;
		final TextView labelName;
		
		AttachmentsContactTileViewHolder(View itemView) {
			super(itemView);
			
			iconProfile = itemView.findViewById(R.id.image_profile);
			iconPlaceholder = itemView.findViewById(R.id.icon_placeholder);
			labelName = itemView.findViewById(R.id.label_name);
		}
	}
	
	private static final AttachmentTileHelper<AttachmentsContactTileViewHolder> attachmentsContactTileHelper = new AttachmentTileHelper<AttachmentsContactTileViewHolder>() {
		@Override
		AttachmentsContactTileViewHolder createViewHolder(ViewGroup parent) {
			return new AttachmentsContactTileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_attachment_contacttile, parent, false));
		}
		
		@Override
		void bindView(Context context, AttachmentsContactTileViewHolder viewHolder, SimpleAttachmentInfo item) {
			//Returning if the file is invalid
			if(item == null) return;
			
			//Getting the extension data
			SimpleAttachmentInfo.ContactExtension extension = (SimpleAttachmentInfo.ContactExtension) item.getExtension();
			
			//Setting the contact name label
			if(extension.contactName == null) viewHolder.labelName.setText(R.string.part_content_contact);
			else viewHolder.labelName.setText(extension.contactName);
			
			//Setting the contact's picture
			if(extension.contactIcon == null) {
				viewHolder.iconPlaceholder.setVisibility(View.VISIBLE);
				viewHolder.iconProfile.setVisibility(View.GONE);
			} else {
				viewHolder.iconPlaceholder.setVisibility(View.GONE);
				viewHolder.iconProfile.setVisibility(View.VISIBLE);
				viewHolder.iconProfile.setImageBitmap(extension.contactIcon);
			}
		}
		
		@Override
		int getViewType() {
			return viewTypeContact;
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
		
		SimpleAttachmentInfo(DraftFile draft) {
			this(draft.getFile() != null ? draft.getFile() : draft.getOriginalFile(), draft.getFileType(), draft.getFileName(), draft.getFileSize(), draft.getModificationDate());
		}
		
		private void assignExtension() {
			if(Constants.compareMimeTypes(fileType, mimeTypeAudio)) extension = new AudioExtension();
			else if(Constants.compareMimeTypes(fileType, mimeTypeVideo)) extension = new VideoExtension();
			else if(Constants.compareMimeTypes(fileType, mimeTypeVLocation)) extension = new LocationExtension();
			else if(ContactAttachmentInfo.checkFileApplicability(fileType, null)) extension = new ContactExtension();
			//else if(Constants.compareMimeTypes(fileType, mimeTypeVLocation)) extension = new LocationExtension();
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
		
		public boolean compare(QueuedFileInfo item) {
			if(item.getDraftFile() == null) return compare(item.item);
			return this.getModificationDate() == item.getItem().getModificationDate() &&
				   ((this.getUri() != null && this.getUri().equals(item.getDraftFile().getOriginalUri())) ||
					(this.getFile() != null && this.getFile().equals(item.getDraftFile().getOriginalFile())));
		}
		
		public boolean compare(SimpleAttachmentInfo item) {
			return this.getModificationDate() == item.getModificationDate() &&
				   ((this.getUri() != null && this.getUri().equals(item.getUri())) ||
					(this.getFile() != null && this.getFile().equals(item.getFile())));
		}
		
		abstract class Extension {
			abstract void initialize();
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
		
		class VideoExtension extends Extension {
			//Creating the attachment info values
			private long mediaDuration = -1;
			
			@Override
			void initialize() {
				//Getting the media duration
				if(file != null) mediaDuration = Constants.getMediaDuration(file);
				else if(uri != null) mediaDuration = Constants.getMediaDuration(MainApplication.getInstance(), uri);
			}
			
			long getMediaDuration() {
				return mediaDuration;
			}
		}
		
		class LocationExtension extends Extension {
			private String locationName = null;
			
			@Override
			void initialize() {
				try {
					//Getting the input stream
					InputStream fileStream;
					if(file != null) fileStream = new FileInputStream(file);
					else if(uri != null) fileStream = MainApplication.getInstance().getContentResolver().openInputStream(uri);
					else return;
					if(fileStream == null) return;
					
					//Parsing the file
					VCard vcard = Ezvcard.parse(fileStream).first();
					
					//Getting the name
					if(vcard.getFormattedName() != null) locationName = vcard.getFormattedName().getValue();
				} catch(IOException exception) {
					exception.printStackTrace();
				}
			}
		}
		
		class ContactExtension extends Extension {
			private String contactName = null;
			private Bitmap contactIcon = null;
			
			@Override
			void initialize() {
				try {
					//Getting the input stream
					InputStream fileStream;
					if(file != null) fileStream = new FileInputStream(file);
					else if(uri != null) fileStream = MainApplication.getInstance().getContentResolver().openInputStream(uri);
					else return;
					if(fileStream == null) return;
					
					//Parsing the file
					VCard vcard = Ezvcard.parse(fileStream).first();
					String name = null;
					Bitmap bitmap = null;
					
					//Getting the name
					if(vcard.getFormattedName() != null) name = vcard.getFormattedName().getValue();
					
					//Getting the bitmap
					if(!vcard.getPhotos().isEmpty()) {
						//Reading the profile picture
						Photo photo = vcard.getPhotos().get(0);
						byte[] photoData = photo.getData();
						bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length);
					}
					
					//Setting the information
					contactName = name;
					contactIcon = bitmap;
				} catch(IOException exception) {
					exception.printStackTrace();
				}
			}
		}
	}
	
	private static class TaskMarkMessageSendStyleViewed extends AsyncTask<MessageInfo, Void, Void> {
		@Override
		protected Void doInBackground(MessageInfo... items) {
			for(MessageInfo item : items)
				DatabaseManager.getInstance().markSendStyleViewed(item.getLocalID());
			return null;
		}
	}
	
	static class LoadingViewHolder extends RecyclerView.ViewHolder {
		LoadingViewHolder(View itemView) {
			super(itemView);
		}
	}
	
	static class ReplySuggestionsViewHolder extends RecyclerView.ViewHolder {
		HorizontalScrollView scrollView;
		LinearLayout container;
		
		ReplySuggestionsViewHolder(View itemView) {
			super(itemView);
			scrollView = (HorizontalScrollView) itemView;
			container = itemView.findViewById(R.id.container);
		}
		
		void resetScroll() {
			scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_RIGHT));
		}
		
		void setSuggestions(Context context, ActivityViewModel viewModel, AMConversationAction[] suggestions) {
			//Gathering active views
			List<TextView> childViewList = new ArrayList<>(suggestions.length);
			for(int i = 0; i < container.getChildCount(); i++) {
				TextView childView = (TextView) container.getChildAt(i);
				if(i < suggestions.length) {
					childView.setVisibility(View.VISIBLE);
					childViewList.add(childView);
				} else childView.setVisibility(View.GONE);
			}
			
			//Adding more views if necessary
			while(childViewList.size() < suggestions.length) {
				TextView item = (TextView) LayoutInflater.from(context).inflate(R.layout.listitem_replysuggestions_item, container, false);
				item.setTextColor(viewModel.conversationInfo.getDisplayConversationColor(context));
				container.addView(item);
				childViewList.add(item);
			}
			
			//Creating a reference to the view model
			WeakReference<ActivityViewModel> viewModelReference = new WeakReference<>(viewModel);
			
			//Configuring the suggestion
			for(int i = 0; i < suggestions.length; i++) {
				TextView childView = childViewList.get(i);
				AMConversationAction conversationAction = suggestions[i];
				
				if(conversationAction.isReplyAction()) {
					childView.setCompoundDrawables(null, null, null, null);
					childView.setText(conversationAction.getReplyString());
					childView.setOnClickListener(view -> {
						//Getting the view model
						ActivityViewModel newViewModel = viewModelReference.get();
						if(newViewModel == null) return;
						
						//Clearing the suggestions
						//newViewModel.smartReplyAvailable.setValue(false);
						
						//Creating a message
						MessageInfo message = new MessageInfo(-1, -1, null, newViewModel.conversationInfo, null, conversationAction.getReplyString().toString(), null, false, System.currentTimeMillis(), Constants.messageStateCodeGhost, Constants.messageErrorCodeOK, false, -1);
						
						//Writing the messages to the database
						new AddGhostMessageTask(newViewModel.getApplication(), new GhostMessageFinishHandler()).execute(message);
					});
				} else {
					//Getting the remote action
					AMConversationAction.AMRemoteAction remoteAction = conversationAction.getRemoteAction();
					
					//Configuring the remote action
					childView.setCompoundDrawablesRelative(remoteAction.getIcon().loadDrawable(context), null, null, null);
					childView.setText(remoteAction.getTitle());
					childView.setOnClickListener(view -> {
						//Launching the intent
						try {
							remoteAction.getActionIntent().send();
						} catch(PendingIntent.CanceledException exception) {
							exception.printStackTrace();
							return;
						}
					});
				}
			}
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
		public boolean supportsPredictiveItemAnimations() {
			return false;
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
		static final int messagesStateIdle = 0;
		static final int messagesStateLoadingConversation = 1;
		static final int messagesStateLoadingMessages = 2;
		static final int messagesStateFailedMatching = 3; //When given the recipients of the conversation to find the conversation ID
		static final int messagesStateFailedConversation = 4;
		static final int messagesStateFailedMessages = 5;
		static final int messagesStateReady = 6;
		
		/* static final int smartReplyStateDisabled = 0;
		static final int smartReplyStateLoading = 1;
		static final int smartReplyStateAvailable = 2; */
		
		static final int attachmentsLocationStatePermission = 0; //Permission not granted
		static final int attachmentsLocationStatePrompt = 1; //User action required
		static final int attachmentsLocationStateUnavailable = 2; //Functionality not available
		static final int attachmentsLocationStateFetching = 3; //Fetching the current location
		static final int attachmentsLocationStateOK = 4; //Location loaded
		
		static final int attachmentsStateIdle = 0;
		static final int attachmentsStateLoading = 1;
		static final int attachmentsStateLoaded = 2;
		static final int attachmentsStateFailed = 3;
		
		static final int soundMessageIncoming = 0;
		static final int soundMessageOutgoing = 1;
		static final int soundMessageError = 2;
		static final int soundRecordingStart = 3;
		static final int soundRecordingEnd = 4;
		
		static final int attachmentTypeGallery = 0;
		//static final int attachmentTypeDocument = 1;
		private static final int attachmentsTileCount = 24;
		
		//Creating the state values
		private final MutableLiveData<Integer> messagesState = new MutableLiveData<>();
		
		private final List<QueuedFileInfo> draftQueueList = new ArrayList<>(3);
		private static final int attachmentTypesCount = 2;
		private final int[] attachmentStates = new int[attachmentTypesCount];
		private final ArrayList<SimpleAttachmentInfo>[] attachmentLists = new ArrayList[attachmentTypesCount];
		private final WeakReference<AttachmentsLoadCallbacks>[] attachmentCallbacks = new WeakReference[attachmentTypesCount];
		
		private boolean isAttachmentsPanelOpen = false;
		private boolean isDetailsPanelOpen = false;
		
		private final MutableLiveData<Boolean> progressiveLoadInProgress = new MutableLiveData<>();
		private boolean progressiveLoadReachedLimit = false;
		private int lastProgressiveLoadCount = -1;
		
		private final MutableLiveData<Boolean> smartReplyAvailable = new MutableLiveData<>();
		private byte smartReplyRequestID = -1;
		private boolean smartReplyAwaiting = false;
		private AMConversationAction[] lastSmartReplyResult = null;
		
		private final MutableLiveData<Boolean> attachmentsLocationLoading = new MutableLiveData<>();
		private final MutableLiveData<Integer> attachmentsLocationState = new MutableLiveData<>();
		private Location attachmentsLocationResult = null;
		private ResolvableApiException attachmentsLocationResolvable = null;
		
		private int lastUnreadCount = 0;
		
		//Creating the conversation values
		private String[] conversationParticipantsTarget;
		private long conversationID;
		private ConversationInfo conversationInfo;
		private ArrayList<ConversationItem> conversationItemList;
		private ArrayList<MessageInfo> conversationGhostList;
		private DatabaseManager.ConversationLazyLoader conversationLazyLoader;
		
		//Creating the attachment values
		File targetFileIntent = null;
		File targetFileRecording = null;
		
		//Creating the sound values
		private static final float soundVolume = 0.15F;
		private SoundPool soundPool = new SoundPool.Builder().setAudioAttributes(new AudioAttributes.Builder()
				.setLegacyStreamType(AudioManager.STREAM_SYSTEM)
				.build())
				.setMaxStreams(2)
				.build();
		private int soundIDMessageIncoming = soundPool.load(getApplication(), R.raw.message_in, 1);
		private int soundIDMessageOutgoing = soundPool.load(getApplication(), R.raw.message_out, 1);
		private int soundIDMessageError = soundPool.load(getApplication(), R.raw.message_error, 1);
		private int soundIDRecordingStart = soundPool.load(getApplication(), R.raw.recording_start, 1);
		private int soundIDRecordingEnd = soundPool.load(getApplication(), R.raw.recording_end, 1);
		
		final AudioPlaybackManager audioPlaybackManager = new AudioPlaybackManager();
		
		private MediaRecorder mediaRecorder = null;
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
		private final Handler mediaRecorderHandler = new Handler();
		private final Runnable mediaRecorderRunnable = () -> {
			//Starting the recording timer
			startRecordingTimer();
			
			//Starting the media recorder
			mediaRecorder.start();
		};
		private final SparseArray<BiConsumer<Context, Boolean>> permissionRequestResultListenerList = new SparseArray<>();
		
		private ActivityViewModel(@NonNull Application application) {
			super(application);
			
			//Filling the attachment lists
			Arrays.fill(attachmentStates, attachmentsStateIdle);
			
			//Initializing the states
			attachmentsLocationLoading.setValue(false);
		}
		
		ActivityViewModel(Application application, long conversationID) {
			this(application);
			
			//Setting the values
			this.conversationID = conversationID;
			
			//Loading the data
			loadConversation();
		}
		
		public ActivityViewModel(@NonNull Application application, String[] conversationParticipantsTarget) {
			this(application);
			
			//Setting the values
			this.conversationID = -1;
			this.conversationParticipantsTarget = conversationParticipantsTarget;
			
			//Loading the conversation
			findCreateConversationMMSSMS();
		}
		
		@Override
		protected void onCleared() {
			//Releasing the sounds
			soundPool.release();
			
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
				//new UpdateUnreadMessageCount(MainApplication.getInstance(), conversationID, conversationInfo.getUnreadMessageCount()).execute();
				DatabaseManager.getInstance().setUnreadMessageCount(conversationID, conversationInfo.getUnreadMessageCount()); //Maybe the task gets killed before it is completed?
			}
		}
		
		/**
		 * Find or create a conversation, based on the data provided
		 */
		@SuppressLint("StaticFieldLeak")
		void findCreateConversationMMSSMS() {
			//Updating the state
			messagesState.setValue(messagesStateLoadingConversation);
			
			//Searching for the conversation in memory
			new AsyncTask<Void, Void, Long>() {
				@Override
				protected Long doInBackground(Void... voids) {
					return Telephony.Threads.getOrCreateThreadId(getApplication(), new HashSet<>(Arrays.asList(conversationParticipantsTarget)));
				}
				
				@Override
				protected void onPostExecute(Long threadID) {
					//Getting the conversation
					conversationInfo = ConversationUtils.findConversationInfoExternalID(threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS);
					
					if(conversationInfo != null) {
						new AsyncTask<Void, Void, DatabaseManager.ConversationLazyLoader>() {
							@Override
							protected DatabaseManager.ConversationLazyLoader doInBackground(Void... voids) {
								return new DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), conversationInfo);
							}
							
							@Override
							protected void onPostExecute(DatabaseManager.ConversationLazyLoader result) {
								//Setting the lazy loader
								conversationLazyLoader = result;
								
								//Loading the conversation's messages
								loadMessages();
							}
						}.execute();
					} else {
						//Fetching or creating the conversation on disk
						new AsyncTask<Void, Void, Constants.Tuple3<ConversationInfo, Boolean, DatabaseManager.ConversationLazyLoader>>() {
							@Override
							protected Constants.Tuple3<ConversationInfo, Boolean, DatabaseManager.ConversationLazyLoader> doInBackground(Void... voids) {
								ConversationInfo conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(getApplication(), threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS);
								boolean conversationNew = false;
								
								//Creating a new conversation if no existing conversation was found
								if(conversationInfo == null) {
									//Creating the conversation
									int conversationColor = ConversationInfo.getDefaultConversationColor(System.currentTimeMillis());
									conversationInfo = new ConversationInfo(-1, null, ConversationInfo.ConversationState.READY, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, new ArrayList<>(), null, 0, conversationColor, null, new ArrayList<>(), -1);
									conversationInfo.setExternalID(threadID);
									conversationInfo.setConversationMembersCreateColors(conversationParticipantsTarget);
									
									//Writing the conversation to disk
									boolean result = DatabaseManager.getInstance().addReadyConversationInfo(conversationInfo);
									if(!result) return null;
									
									//Adding the conversation created message
									DatabaseManager.getInstance().addConversationCreatedMessage(conversationInfo, getApplication());
									
									conversationNew = true;
								}
								
								//Creating the lazy loader
								DatabaseManager.ConversationLazyLoader lazyLoader = new DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), conversationInfo);
								
								//Returning the data
								return new Constants.Tuple3<>(conversationInfo, conversationNew, lazyLoader);
							}
							
							@Override
							protected void onPostExecute(Constants.Tuple3<ConversationInfo, Boolean, DatabaseManager.ConversationLazyLoader> result) {
								//Setting the state to failed if the conversation info couldn't be fetched
								if(result == null) messagesState.setValue(messagesStateFailedConversation);
								else {
									//Setting the conversation details
									conversationInfo = result.item1;
									boolean conversationNew = result.item2;
									conversationLazyLoader = result.item3;
									
									//Adding the conversation if it is new
									if(conversationNew) {
										ConversationUtils.addConversation(conversationInfo);
										
										//Updating the conversation activity list
										LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
									}
									
									//Loading the conversation's messages
									loadMessages();
								}
							}
						}.execute();
					}
				}
			}.execute();
		}
		
		/**
		 * Loads the conversation and its messages
		 */
		@SuppressLint("StaticFieldLeak")
		void loadConversation() {
			//Updating the state
			messagesState.setValue(messagesStateLoadingConversation);
			
			//Loading the conversation
			conversationInfo = ConversationUtils.findConversationInfo(conversationID);
			if(conversationInfo != null) {
				new AsyncTask<Void, Void, DatabaseManager.ConversationLazyLoader>() {
					@Override
					protected DatabaseManager.ConversationLazyLoader doInBackground(Void... voids) {
						return new DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), conversationInfo);
					}
					
					@Override
					protected void onPostExecute(DatabaseManager.ConversationLazyLoader result) {
						//Setting the lazy loader
						conversationLazyLoader = result;
						
						//Loading the conversation's messages
						loadMessages();
					}
				}.execute();
			} else {
				new AsyncTask<Void, Void, Constants.Tuple2<ConversationInfo, DatabaseManager.ConversationLazyLoader>>() {
					@Override
					protected Constants.Tuple2<ConversationInfo, DatabaseManager.ConversationLazyLoader> doInBackground(Void... args) {
						//Getting the conversation
						ConversationInfo conversation = DatabaseManager.getInstance().fetchConversationInfo(getApplication(), conversationID);
						if(conversation == null) return null;
						
						//Creating the lazy loader
						DatabaseManager.ConversationLazyLoader lazyLoader = new DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), conversation);
						
						//Returning the data
						return new Constants.Tuple2<>(conversation, lazyLoader);
					}
					
					@Override
					protected void onPostExecute(Constants.Tuple2<ConversationInfo, DatabaseManager.ConversationLazyLoader> result) {
						//Setting the state to failed if the conversation info couldn't be fetched
						if(result == null) messagesState.setValue(messagesStateFailedConversation);
						else {
							//Setting the conversation details
							conversationInfo = result.item1;
							conversationLazyLoader = result.item2;
							
							//Loading the conversation's messages
							loadMessages();
						}
					}
				}.execute();
			}
		}
		
		@SuppressLint("StaticFieldLeak")
		void loadMessages() {
			//Updating the state
			messagesState.setValue(messagesStateLoadingMessages);
			
			//Checking if the conversation already has lists
			ArrayList<ConversationItem> existingConversationItems = conversationInfo.getConversationItems();
			ArrayList<MessageInfo> existingGhostMessages = conversationInfo.getGhostMessages();
			if(existingConversationItems != null && existingGhostMessages != null) {
				//Setting the lists
				conversationItemList = existingConversationItems;
				conversationGhostList = existingGhostMessages;
				
				//Marking all messages as read (list will always be scrolled to the bottom)
				conversationInfo.setUnreadMessageCount(0);
				
				//Updating the lazy loader position
				conversationLazyLoader.setCursorPosition(existingConversationItems.size());
				
				//Setting the state
				messagesState.setValue(messagesStateReady);
				
				//Updating the conversation's shortcut usage
				ConversationUtils.reportShortcutUsed(getApplication(), conversationInfo.getGuid());
			} else {
				//Loading the messages
				new AsyncTask<Void, Void, List<ConversationItem>>() {
					@Override
					protected List<ConversationItem> doInBackground(Void... params) {
						//Loading the conversation items
						//Constants.Tuple3<List<ConversationItem>, Long, Integer> result = DatabaseManager.getInstance().loadConversationChunk(conversationInfo, false, -1, -1);
						List<ConversationItem> conversationItems = conversationLazyLoader.loadNextChunk();
						
						//Setting up the conversation item relations
						ConversationUtils.setupConversationItemRelations(conversationItems, conversationInfo);
						
						//Returning the conversation items
						return conversationItems;
					}
					
					@Override
					protected void onPostExecute(List<ConversationItem> result) {
						//Checking if the result is invalid
						if(result == null) {
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
						conversationInfo.replaceConversationItems(MainApplication.getInstance(), result);
						
						//Marking all messages as read (list will always be scrolled to the bottom)
						conversationInfo.setUnreadMessageCount(0);
						
						//Setting the state
						messagesState.setValue(messagesStateReady);
						
						//Updating the conversation's shortcut usage
						ConversationUtils.reportShortcutUsed(getApplication(), conversationInfo.getGuid());
					}
				}.execute();
			}
		}
		
		@SuppressLint("StaticFieldLeak")
		void loadNextChunk() {
			//Returning if the conversation isn't ready, a load is already in progress or there are no conversation items
			if(messagesState.getValue() != messagesStateReady || isProgressiveLoadInProgress() || progressiveLoadReachedLimit || conversationInfo.getConversationItems().isEmpty())
				return;
			
			//Setting the flags
			progressiveLoadInProgress.setValue(true);
			
			new AsyncTask<Void, Void, List<ConversationItem>>() {
				@Override
				protected List<ConversationItem> doInBackground(Void... params) {
					//Loading the conversation items
					//return DatabaseManager.getInstance().loadConversationChunk(conversationInfo, true, firstSortIDFinal, firstSortIDOffsetFinal);
					return conversationLazyLoader.loadNextChunk();
				}
				
				@Override
				protected void onPostExecute(List<ConversationItem> result) {
					//Setting the progressive load count
					lastProgressiveLoadCount = result.size();
					
					//Checking if there are no new conversation items
					if(result.isEmpty()) {
						//Disabling the progressive load (there are no more items to load)
						progressiveLoadReachedLimit = true;
					} else {
						//Loading the items
						List<ConversationItem> allItems = conversationInfo.getConversationItems();
						if(allItems == null) return;
						
						//Adding the items
						conversationInfo.addChunk(result);
						
						//Updating the items' relations
						ConversationUtils.addConversationItemRelations(conversationInfo, allItems, result, MainApplication.getInstance(), true);
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
		
		boolean isSmartReplyAvailable() {
			Boolean value = smartReplyAvailable.getValue();
			return value == null ? false : value;
		}
		
		void playSound(int id) {
			switch(id) {
				case soundMessageIncoming:
					soundPool.play(soundIDMessageIncoming, soundVolume, soundVolume, 0, 0, 1);
					break;
				case soundMessageOutgoing:
					soundPool.play(soundIDMessageOutgoing, soundVolume, soundVolume, 0, 0, 1);
					break;
				case soundMessageError:
					soundPool.play(soundIDMessageError, soundVolume, soundVolume, 1, 0, 1);
					break;
				case soundRecordingStart:
					soundPool.play(soundIDRecordingStart, soundVolume, soundVolume, 1, 0, 1);
					break;
				case soundRecordingEnd:
					soundPool.play(soundIDRecordingEnd, soundVolume, soundVolume, 1, 0, 1);
					break;
				default:
					throw new IllegalArgumentException("Unknown sound ID " + id);
			}
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
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				mediaRecorder.setOutputFile(targetFileRecording);
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
			
			//Updating the state
			isRecording.setValue(true);
			
			//Queueing a delay for the audio recorder
			mediaRecorderHandler.postDelayed(mediaRecorderRunnable, 70);
			
			//Returning true
			return true;
		}
		
		/**
		 * Stops the current recording session
		 *
		 * @param cleanup whether or not to clean up the media recorder (usually wanted, unless the media recorder encountered an error)
		 * @param discard whether or not to discard the recorded file
		 * @return the file's availability (to be able to use or send)
		 */
		private boolean stopRecording(boolean cleanup, boolean discard) {
			//Returning if the input state is not recording
			if(!isRecording()) return true;
			
			//Removing the timer callback
			mediaRecorderHandler.removeCallbacks(mediaRecorderRunnable);
			
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
				
				//Playing a sound
				playSound(soundRecordingEnd);
				
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
					//Stopping recording
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
		
		@SuppressLint("StaticFieldLeak")
		private void updateSmartReply() {
			//Returning if smart reply is disabled or the conversation has no messages
			if(!Preferences.getPreferenceReplySuggestions(getApplication()) || conversationItemList == null || conversationItemList.isEmpty()) return;
			
			//Finding the last message
			MessageInfo lastMessage = null;
			for(int i  = conversationItemList.size() - 1; i >= 0; i--) {
				ConversationItem item = conversationItemList.get(i);
				if(!(item instanceof MessageInfo)) continue;
				lastMessage = (MessageInfo) item;
				break;
			}
			
			//Checking if the last message isn't valid
			if(lastMessage == null || lastMessage.getMessageText() == null ||
			   lastMessage.isOutgoing() || Constants.appleSendStyleBubbleInvisibleInk.equals(lastMessage.getSendStyle())) {
				//Cancelling the smart reply
				smartReplyAvailable.setValue(false);
				smartReplyAwaiting = false;
				
				return;
			}
			
			//Collecting the last 10 messages
			List<MessageInfo> messageHistory = new ArrayList<>(Constants.smartReplyHistoryLength);
			for(int i  = conversationItemList.size() - 1; i >= 0; i--) {
				ConversationItem item = conversationItemList.get(i);
				if(!(item instanceof MessageInfo)) continue;
				MessageInfo message = (MessageInfo) item;
				if(message.getMessageText() == null) continue;
				messageHistory.add(message);
				
				if(messageHistory.size() == Constants.smartReplyHistoryLength) break;
			}
			
			//Sorting the list by date
			Collections.sort(messageHistory, (item1, item2) -> Long.compare(item1.getDate(), item2.getDate()));
			
			//Starting the smart reply request
			smartReplyAwaiting = true;
			short requestID = ++smartReplyRequestID;
			
			if(false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				TextClassifier textClassifier = ((TextClassificationManager) getApplication().getSystemService(Context.TEXT_CLASSIFICATION_SERVICE)).getTextClassifier();
				new AsyncTask<Void, Void, ConversationActions>() {
					@Override
					protected ConversationActions doInBackground(Void... args) {
						//Requesting conversation actions
						return textClassifier.suggestConversationActions(new ConversationActions.Request.Builder(Constants.messageToTextClassifierMessageList(messageHistory))
								//.setMaxSuggestions(3)
								//.setHints(Collections.singletonList(ConversationActions.Request.HINT_FOR_IN_APP))
								.build());
					}
					
					@Override
					protected void onPostExecute(ConversationActions conversationActions) {
						//Ignoring if the request ID doesn't line up
						if(smartReplyRequestID != requestID) return;
						
						List<ConversationAction> actionList = conversationActions.getConversationActions();
						if(actionList.isEmpty()) {
							//Cancelling the smart reply request
							smartReplyAvailable.setValue(false);
							smartReplyAwaiting = false;
						} else {
							//Mapping the suggestions to an array
							AMConversationAction[] suggestions = new AMConversationAction[actionList.size()];
							for(int i = 0; i < actionList.size(); i++) {
								ConversationAction action = actionList.get(i);
								
								//Text replies
								if(action.getType().equals(ConversationAction.TYPE_TEXT_REPLY)) {
									suggestions[i] = AMConversationAction.createReplyAction(action.getTextReply());
								}
								//Action replies
								else {
									RemoteAction remoteAction = action.getAction();
									suggestions[i] = AMConversationAction.createRemoteAction(new AMConversationAction.AMRemoteAction(remoteAction.shouldShowIcon() ? remoteAction.getIcon() : null, remoteAction.getTitle(), remoteAction.getActionIntent()));
								}
							}
							
							//Finishing the smart reply
							lastSmartReplyResult = suggestions;
							smartReplyAvailable.setValue(true);
							smartReplyAwaiting = false;
						}
					}
				}.execute();
			} else {
				FirebaseNaturalLanguage.getInstance().getSmartReply().suggestReplies(Constants.messageToFirebaseMessageList(messageHistory)).addOnSuccessListener(result -> {
					//Ignoring if the request ID doesn't line up
					if(smartReplyRequestID != requestID) return;
					
					if(result.getStatus() != SmartReplySuggestionResult.STATUS_SUCCESS) {
						//Cancelling the smart reply request
						smartReplyAvailable.setValue(false);
						smartReplyAwaiting = false;
					} else {
						//Mapping the suggestions to an array
						AMConversationAction[] suggestions = new AMConversationAction[result.getSuggestions().size()];
						for(int i = 0; i < suggestions.length; i++) suggestions[i] = AMConversationAction.createReplyAction(result.getSuggestions().get(i).getText());
						
						//Finishing the smart reply
						lastSmartReplyResult = suggestions;
						smartReplyAvailable.setValue(true);
						smartReplyAwaiting = false;
					}
					
				}).addOnFailureListener(exception -> {
					//Ignoring if the request ID doesn't line up
					if(smartReplyRequestID != requestID) return;
					
					//Cancelling the smart reply request
					smartReplyAvailable.setValue(false);
					smartReplyAwaiting = false;
				});
			}
		}
		
		void indexAttachmentsGallery(AttachmentsLoadCallbacks listener, AttachmentsRecyclerAdapter<?> adapter) {
			indexAttachmentsFromMediaStore(listener, attachmentTypeGallery, MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, adapter);
		}
		
		int getAttachmentState(int itemType) {
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
			if(currentState == attachmentsStateLoading || currentState == attachmentsStateLoaded)
				return;
			
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
								new String[]{MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED},
								msQuerySelection,
								null,
								MediaStore.Files.FileColumns.DATE_ADDED + " DESC" + ' ' + "LIMIT " + attachmentsTileCount)) {
							if(cursor == null) return null;
							
							//int indexData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
							int iLocalID = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
							int iType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
							int iDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
							int iSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
							int iModificationDate = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
							
							while(cursor.moveToNext()) {
								Uri uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), cursor.getInt(iLocalID));
								//Getting the file information
								//File file = new File(cursor.getString(iData));
								String fileType = cursor.getString(iType);
								if(fileType == null) fileType = "application/octet-stream";
								String fileName = Constants.cleanFileName(cursor.getString(iDisplayName));
								long fileSize = cursor.getLong(iSize);
								long modificationDate = cursor.getLong(iModificationDate);
								//list.add(new SimpleAttachmentInfo(file, fileType, fileName, fileSize, modificationDate));
								list.add(new SimpleAttachmentInfo(uri, fileType, fileName, fileSize, modificationDate));
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
		
		void addPermissionsRequestListener(int requestCode, BiConsumer<Context, Boolean> callback) {
			permissionRequestResultListenerList.put(requestCode, callback);
		}
		
		void callPermissionsRequestListener(int requestCode, boolean result) {
			BiConsumer<Context, Boolean> listener = permissionRequestResultListenerList.get(requestCode);
			if(listener == null) return;
			listener.accept(getApplication(), result);
			permissionRequestResultListenerList.remove(requestCode);
		}
		
		void updateAttachmentsLocationState() {
			//Checking if the permission has not been granted
			if(ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				attachmentsLocationState.setValue(attachmentsLocationStatePermission);
				return;
			}
			
			//Setting the state as loading
			attachmentsLocationLoading.setValue(true);
			
			FusedLocationProviderClient locationProvider = LocationServices.getFusedLocationProviderClient(getApplication());
			LocationRequest locationRequest = LocationRequest.create();
			//Requesting location services status
			LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
			Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(getApplication()).checkLocationSettings(builder.build());
			task.addOnCompleteListener(taskResult -> {
				//Restoring the loading state
				attachmentsLocationLoading.setValue(false);
				try {
					//Getting the result
					taskResult.getResult(ApiException.class); //Forces exception to be thrown if needed
					
					//Updating the state
					attachmentsLocationState.setValue(attachmentsLocationStateFetching);
					
					//Getting user's location
					locationProvider.getLastLocation().addOnSuccessListener(location -> {
						if(location == null) {
							//Pulling an update from location services
							locationProvider.requestLocationUpdates(locationRequest, new LocationCallback() {
								@Override
								public void onLocationResult(LocationResult locationResult) {
									//Returning if there is no result (and waiting for another update)
									if(locationResult == null) return;
									
									//Removing the updater
									locationProvider.removeLocationUpdates(this);
									
									//Setting the location
									attachmentsLocationResult = locationResult.getLastLocation();
									attachmentsLocationState.setValue(attachmentsLocationStateOK);
								}
							}, null);
						} else {
							//Setting the location
							attachmentsLocationResult = location;
							attachmentsLocationState.setValue(attachmentsLocationStateOK);
						}
					});
				} catch (ApiException exception) {
					switch (exception.getStatusCode()) {
						case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
							//Setting the resolvable
							attachmentsLocationResolvable = (ResolvableApiException) exception;
							attachmentsLocationState.setValue(attachmentsLocationStatePrompt);
							break;
						case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
							attachmentsLocationState.setValue(attachmentsLocationStateUnavailable);
							break;
					}
				}
			});
		}
		
		boolean isAttachmentsLocationLoading() {
			return attachmentsLocationLoading.getValue();
		}
		
		int getAttachmentsLocationState() {
			return attachmentsLocationState.getValue();
		}
		
		void attachmentsLocationLaunchPrompt(Activity activity, int requestCode) {
			try {
				attachmentsLocationResolvable.startResolutionForResult(activity, requestCode);
			} catch(IntentSender.SendIntentException exception) {
				exception.printStackTrace();
			}
		}
		
		boolean doFilesRequireCompression() {
			return conversationInfo.getServiceHandler() != ConversationInfo.serviceHandlerAMBridge || !ConversationInfo.serviceTypeAppleMessage.equals(conversationInfo.getService());
		}
	}
	
	public static class AudioPlaybackManager {
		//Creating the constants
		public static final String requestTypeAttachment = "attachment-";
		public static final String requestTypeDraft = "draft-";
		
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
		
		public AudioPlaybackManager() {
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
		
		public void release() {
			//Cancelling the timer
			if(mediaPlayer.isPlaying())
				mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
			
			//Releasing the media player
			mediaPlayer.release();
		}
		
		public boolean play(File file, Callbacks callbacks) {
			return play(null, file, callbacks);
		}
		
		public boolean play(String requestID, File file, Callbacks callbacks) {
			//Returning true if the request ID matches (or the request ID is null)
			if(this.requestID != null && this.requestID.equals(requestID)) return true;
			
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
		
		public void togglePlaying() {
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
		
		public void stop() {
			//if(!mediaPlayer.isPlaying()) return;
			
			mediaPlayer.stop();
			if(callbacks != null) callbacks.onStop();
			stopTimer();
		}
		
		public boolean compareRequestID(String requestID) {
			if(this.requestID == null) return false;
			return this.requestID.equals(requestID);
		}
		
		public interface Callbacks {
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
		private WeakReference<ConversationUtils.AudioAttachmentInfo> attachmentReference;
		private Handler mediaPlayerHandler = new Handler(Looper.getMainLooper());
		private Runnable mediaPlayerHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				//Updating the attachment
				ConversationUtils.AudioAttachmentInfo attachment = getAttachmentInfo();
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
				ConversationUtils.AudioAttachmentInfo attachment = getAttachmentInfo();
				if(attachment != null) attachment.setMediaPlaying(true);
			});
			mediaPlayer.setOnCompletionListener(player -> {
				//Cancelling the timer
				mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
				
				//Updating the attachment
				ConversationUtils.AudioAttachmentInfo attachment = getAttachmentInfo();
				if(attachment != null) attachment.resetPlaying();
			});
		}
		
		private ConversationUtils.AudioAttachmentInfo getAttachmentInfo() {
			if(attachmentReference == null) return null;
			return attachmentReference.get();
		}
		
		void release() {
			//Cancelling the timer
			if(mediaPlayer.isPlaying()) mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
			
			//Releasing the media player
			mediaPlayer.release();
		}
		
		void prepareMediaPlayer(long messageID, File file, ConversationUtils.AudioAttachmentInfo attachmentInfo) {
			//Returning if the attachment is already playing
			if(currentFilePath.equals(file.getPath())) return;
			
			//Cancelling the timer
			if(mediaPlayer.isPlaying()) mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
			
			//Resetting the media player
			mediaPlayer.reset();
			
			//Resetting the old view
			{
				ConversationUtils.AudioAttachmentInfo oldAttachmentInfo = getAttachmentInfo();
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
			ConversationUtils.AudioAttachmentInfo attachment = getAttachmentInfo();
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
	
	private static class AddGhostMessageTask extends AsyncTask<MessageInfo, MessageInfo, Void> {
		private final WeakReference<Context> contextReference;
		private final Consumer<MessageInfo> onFinishListener;
		
		AddGhostMessageTask(Context context, Consumer<MessageInfo> onFinishListener) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			
			//Setting the other values
			this.onFinishListener = onFinishListener;
		}
		
		@Override
		protected Void doInBackground(MessageInfo... messages) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Adding the items to the database
			for(MessageInfo message : messages) {
				DatabaseManager.getInstance().addConversationItem(message, message.getConversationInfo().getServiceHandler() == ConversationInfo.serviceHandlerAMBridge);
				publishProgress(message);
			}
			
			//Returning
			return null;
		}
		
		@Override
		protected void onProgressUpdate(MessageInfo... messages) {
			for(MessageInfo message : messages) onFinishListener.accept(message);
		}
	}
	
	private static class WriteVLocationTask extends AsyncTask<Void, Void, SimpleAttachmentInfo> {
		private final File targetFile;
		private final LatLng targetLocation;
		private final String locationAddress;
		private final String locationName;
		private final WeakReference<Messaging> activityReference;
		
		WriteVLocationTask(File targetFile, LatLng targetLocation, String locationAddress, String locationName, Messaging activity) {
			this.targetFile = targetFile;
			this.targetLocation = targetLocation;
			this.locationAddress = locationAddress;
			this.locationName = locationName;
			
			activityReference = new WeakReference<>(activity);
		}
		
		@Override
		protected SimpleAttachmentInfo doInBackground(Void... voids) {
			//Creating the vCard
			VCard vcard = new VCard();
			vcard.setProductId("-//" + Build.MANUFACTURER + "//" + "Android" + " " + Build.VERSION.RELEASE + "//" + Locale.getDefault().getLanguage().toUpperCase());
			vcard.setFormattedName(locationName);
			
			//Constructing the URL
			String stringLoc = targetLocation.latitude + "," + targetLocation.longitude;
			Uri.Builder uriBuilder = new Uri.Builder()
					.scheme("https")
					.authority("maps.apple.com")
					.appendQueryParameter("ll", stringLoc)
					.appendQueryParameter("q", locationAddress != null ? locationName : stringLoc);
			if(locationAddress != null) uriBuilder.appendQueryParameter("address", locationAddress);
			Url url = new Url(uriBuilder.build().toString());
			
			//Adding the URL
			url.setGroup("item1");
			vcard.addUrl(url);
			
			//Adding the type identifier
			RawProperty typeProperty = vcard.addExtendedProperty("X-ABLabel", "map url");
			typeProperty.setGroup("item1");
			
			//Writing the vCard
			try(VCardWriter writer = new VCardWriter(targetFile, VCardVersion.V3_0)) {
				writer.write(vcard);
			} catch(IOException exception) {
				exception.printStackTrace();
				return null;
			}
			
			//Returning the attachment data
			return new SimpleAttachmentInfo(targetFile, VLocationAttachmentInfo.MIME_TYPE, targetFile.getName(), targetFile.length(), -1);
		}
		
		@Override
		protected void onPostExecute(SimpleAttachmentInfo attachmentInfo) {
			//Ignoring the result if the file couldn't be written
			if(attachmentInfo == null) return;
			
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Queuing the file
			activity.queueAttachment(attachmentInfo, activity.findAppropriateTileHelper(attachmentInfo.getFileType()), true);
		}
	}
	
	private static class ActivityCallbacks extends ConversationInfo.ActivityCallbacks {
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
		public void listUpdateInserted(int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.notifyItemInserted(index);
		}
		
		@Override
		public void listUpdateRemoved(int index) {
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
		public void listUpdateUnread() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Notifying the scroll listener
			activity.messageListScrollListener.onScrolled(activity.messageList, 0, 0);
		}
		
		@Override
		public void listScrollToBottom() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the adapter
			activity.messageListAdapter.scrollToBottom();
		}
		
		@Override
		public void listAttemptScrollToBottom(int... newIndices) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Checking if the new message will cause the list to scroll
			boolean newMessageAdded = false;
			for(int index : newIndices) {
				if(activity.messageListAdapter.isDirectlyBelowFrame(index)) {
					newMessageAdded = true;
					break;
				}
			}
			
			//Scrolling down to the item (or the bottom of the list, same thing)
			if(newMessageAdded) activity.messageListAdapter.scrollToBottom();
		}
		
		@Override
		public void chatUpdateTitle() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Building the conversation title
			activity.viewModel.conversationInfo.buildTitle(activity, new ConversationTitleResultCallback(activity));
		}
		
		@Override
		public void chatUpdateUnreadCount() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Updating the unread indicator
			activity.updateUnreadIndicator();
		}
		
		@Override
		public void chatUpdateMemberAdded(MemberInfo member, int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Adding the member
			activity.addMemberView(member, index, true);
		}
		
		@Override
		public void chatUpdateMemberRemoved(MemberInfo member, int index) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Removing the member
			activity.removeMemberView(member);
		}
		
		@Override
		public void itemsAdded(List<ConversationItem> list) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Returning if the activity is not in the foreground
			if(!activity.hasWindowFocus()) return;
			
			boolean messageIncoming = false;
			boolean messageOutgoing = false;
			
			for(ConversationItem item : list) {
				//Ignoring items other than messages
				if(!(item instanceof MessageInfo)) continue;
				
				//Tracking the message types
				MessageInfo messageInfo = (MessageInfo) item;
				if(messageInfo.isOutgoing()) messageOutgoing = true;
				else messageIncoming = true;
				if(messageIncoming && messageOutgoing) break;
			}
			
			//Playing sounds
			if(messageIncoming) activity.viewModel.playSound(ActivityViewModel.soundMessageIncoming);
			if(messageOutgoing) activity.viewModel.playSound(ActivityViewModel.soundMessageOutgoing);
			
			//Updating the reply suggestions
			activity.viewModel.updateSmartReply();
		}
		
		@Override
		public void tapbackAdded(TapbackInfo item) {
		
		}
		
		@Override
		public void stickerAdded(StickerInfo item) {
		
		}
		
		@Override
		public void messageSendFailed(MessageInfo message) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Returning if the activity is not in the foreground
			if(!activity.hasWindowFocus()) return;
			
			//Playing a sound
			activity.viewModel.playSound(ActivityViewModel.soundMessageError);
		}
		
		@Override
		public AudioPlaybackManager getAudioPlaybackManager() {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return null;
			
			//Returning the view model's audio message manager
			return activity.viewModel.audioPlaybackManager;
		}
		
		@Override
		public void playScreenEffect(String screenEffect, View target) {
			Messaging activity = activityReference.get();
			if(activity != null) activity.playScreenEffect(screenEffect, target);
		}
		
		@Override
		public void requestPermission(String permission, int requestCode, BiConsumer<Context, Boolean> resultListener) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Requesting the permission
			activity.requestPermissions(new String[]{permission}, requestCode + permissionRequestMessageCustomOffset);
			activity.viewModel.addPermissionsRequestListener(requestCode, resultListener);
		}
		
		@Override
		public void saveFile(File file) {
			//Getting the activity
			Messaging activity = activityReference.get();
			if(activity == null) return;
			
			//Opening the file picker
			activity.currentTargetSAFFile = file;
			Constants.createFileSAF(activity, intentSaveFileSAF, Constants.getMimeType(file), file.getName());
		}
	}
	
	private static class ConversationTitleResultCallback implements Constants.TaskedResultCallback<String> {
		private final WeakReference<Messaging> activityReference;
		
		public ConversationTitleResultCallback(Messaging activity) {
			activityReference = new WeakReference<>(activity);
		}
		
		@Override
		public void onResult(String result, boolean wasTasked) {
			Messaging activity = activityReference.get();
			if(activity == null) return;
			activity.setActionBarTitle(result);
		}
	}
	
	void detailSwitchConversationColor(int newColor) {
		//Updating the conversation color
		viewModel.conversationInfo.setConversationColor(newColor);
		if(!viewModel.conversationInfo.isGroupChat())
			viewModel.conversationInfo.updateViewUser(this);
		
		//Coloring the UI
		colorUI(findViewById(android.R.id.content));
		
		//Updating the conversation color on disk
		DatabaseManager.getInstance().updateConversationColor(viewModel.conversationInfo.getLocalID(), newColor);
		
		//Updating the member if there is only one member as well
		if(viewModel.conversationInfo.getConversationMembers().size() == 1)
			detailSwitchMemberColor(viewModel.conversationInfo.getConversationMembers().get(0), newColor);
	}
	
	void detailSwitchMemberColor(MemberInfo member, int newColor) {
		//Updating the user's color
		member.setColor(newColor);
		
		//Updating the view
		View memberView = memberListViews.get(member);
		((ImageView) memberView.findViewById(R.id.button_change_color)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		((ImageView) memberView.findViewById(R.id.profile_default)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		
		//Updating the message colors
		if(viewModel.conversationItemList != null)
			for(ConversationItem conversationItem : viewModel.conversationItemList)
				conversationItem.updateViewColor(this);
		
		//Updating the listing color
		viewModel.conversationInfo.updateViewUser(this);
		
		//Updating the member color on disk
		DatabaseManager.getInstance().updateMemberColor(viewModel.conversationInfo.getLocalID(), member.getName(), newColor);
	}
	
	private static final String colorDialogTag = "colorPickerDialog";
	
	void showColorDialog(MemberInfo member, int currentColor) {
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
		private MemberInfo member = null;
		private int selectedColor;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			//Getting the arguments
			if(getArguments().containsKey(Constants.intentParamData))
				member = (MemberInfo) getArguments().getSerializable(Constants.intentParamData);
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
			for(int i = 0; i < ConversationInfo.standardUserColors.length; i++) {
				//Getting the color
				final int standardColor = ConversationInfo.standardUserColors[i];
				
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
						if(member == null)
							((Messaging) getActivity()).detailSwitchConversationColor(standardColor);
						else
							((Messaging) getActivity()).detailSwitchMemberColor(member, standardColor);
					}
					
					getDialog().dismiss();
				});
				
				//Adding the view to the layout
				contentViewGroup.addView(item);
			}
			
			//Returning the view
			return dialogView;
		}
		
		static ColorPickerDialog newInstance(MemberInfo memberInfo, int selectedColor) {
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
	
	private static class GhostMessageFinishHandler implements Consumer<MessageInfo> {
		@Override
		public void accept(MessageInfo messageInfo) {
			//Adding the message to the conversation in memory
			messageInfo.getConversationInfo().addGhostMessage(MainApplication.getInstance(), messageInfo);
			
			//Sending the message
			messageInfo.sendMessage(MainApplication.getInstance());
		}
	}
}