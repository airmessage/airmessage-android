package me.tagavari.airmessage.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Pools;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.wasabeef.glide.transformations.BlurTransformation;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginConnectionService;
import me.tagavari.airmessage.compositeplugin.PluginMessageBar;
import me.tagavari.airmessage.compositeplugin.PluginRXDisposable;
import me.tagavari.airmessage.connection.ConnectionTaskManager;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.constants.TimingConstants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.AttachmentType;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionState;
import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.enums.MessagePreviewState;
import me.tagavari.airmessage.enums.MessagePreviewType;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.enums.MessageViewType;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.fragment.FragmentMessagingAttachments;
import me.tagavari.airmessage.fragment.FragmentMessagingDetails;
import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.AttachmentStorageHelper;
import me.tagavari.airmessage.helper.CollectionHelper;
import me.tagavari.airmessage.helper.ColorHelper;
import me.tagavari.airmessage.helper.ColorMathHelper;
import me.tagavari.airmessage.helper.ContactHelper;
import me.tagavari.airmessage.helper.ConversationBuildHelper;
import me.tagavari.airmessage.helper.ConversationColorHelper;
import me.tagavari.airmessage.helper.DataCompressionHelper;
import me.tagavari.airmessage.helper.DataStreamHelper;
import me.tagavari.airmessage.helper.ErrorDetailsHelper;
import me.tagavari.airmessage.helper.ErrorLanguageHelper;
import me.tagavari.airmessage.helper.ExternalStorageHelper;
import me.tagavari.airmessage.helper.FileHelper;
import me.tagavari.airmessage.helper.IntentHelper;
import me.tagavari.airmessage.helper.LanguageHelper;
import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.helper.MessageSendHelper;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.helper.PlatformHelper;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.helper.SendStyleHelper;
import me.tagavari.airmessage.helper.ShortcutHelper;
import me.tagavari.airmessage.helper.SmartReplyHelper;
import me.tagavari.airmessage.helper.SoundHelper;
import me.tagavari.airmessage.helper.StringHelper;
import me.tagavari.airmessage.helper.ThemeHelper;
import me.tagavari.airmessage.helper.ViewHelper;
import me.tagavari.airmessage.helper.WindowHelper;
import me.tagavari.airmessage.messaging.AMConversationAction;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationAction;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.FileDisplayMetadata;
import me.tagavari.airmessage.messaging.FileDraft;
import me.tagavari.airmessage.messaging.FileLinked;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageComponent;
import me.tagavari.airmessage.messaging.MessageComponentText;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.MessagePreviewInfo;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.messaging.viewbinder.VBMessageComponent;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentQueued;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContent;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContentAudio;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContentContact;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContentDocument;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContentLocation;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContentMedia;
import me.tagavari.airmessage.messaging.viewholder.VHConversationActions;
import me.tagavari.airmessage.messaging.viewholder.VHMessageAction;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponent;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentAttachment;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentAudio;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentContact;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentDocument;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentLocation;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentText;
import me.tagavari.airmessage.messaging.viewholder.VHMessageComponentVisual;
import me.tagavari.airmessage.messaging.viewholder.VHMessagePreviewLink;
import me.tagavari.airmessage.messaging.viewholder.VHMessageStructure;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventAttachmentDownload;
import me.tagavari.airmessage.redux.ReduxEventConnection;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.task.ConversationActionTask;
import me.tagavari.airmessage.task.DraftActionTask;
import me.tagavari.airmessage.task.FileQueueTask;
import me.tagavari.airmessage.task.MessageActionTask;
import me.tagavari.airmessage.task.RichPreviewTask;
import me.tagavari.airmessage.util.AnimatingInsetsCallback;
import me.tagavari.airmessage.util.AudioPlaybackManager;
import me.tagavari.airmessage.util.CustomTabsLinkTransformationMethod;
import me.tagavari.airmessage.util.DisposableViewHolder;
import me.tagavari.airmessage.util.ReplaceInsertResult;
import me.tagavari.airmessage.util.TapbackDisplayData;
import me.tagavari.airmessage.util.TaskManager;
import me.tagavari.airmessage.util.TaskManagerLong;
import me.tagavari.airmessage.util.Union;
import me.tagavari.airmessage.view.AppleEffectView;
import me.tagavari.airmessage.view.InvisibleInkView;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class Messaging extends AppCompatCompositeActivity {
	private static final String TAG = Messaging.class.getSimpleName();
	
	public static final String intentParamTargetID = "targetID";
	public static final String intentParamDataText = "dataText";
	public static final String intentParamDataFile = "dataFile";
	public static final String intentParamBubble = "bubble";
	
	public static final int messageChunkSize = 20;
	public static final int progressiveLoadThreshold = 10;
	
	private static final int quickScrollFABThreshold = 3;
	private static final float contentPanelMinAllowanceDP = 275;
	private static final int draftCountLimit = 10;
	
	private static final long confettiDuration = 1000;
	//private static final float disabledAlpha = 0.38F
	
	private static final int intentSaveFileSAF = 1;
	
	private static final int previewImageMaxSize = 128 * 1024; //128 kB
	
	private static final String keyFragmentAttachments = "fragmentAttachments";
	
	//Creating the static values
	private static final List<WeakReference<Messaging>> foregroundConversations = new ArrayList<>();
	
	//Creating the view model and plugin values
	private ActivityViewModel viewModel;
	private final PluginMessageBar pluginMessageBar;
	private final PluginConnectionService pluginCS;
	private final PluginRXDisposable pluginRXD;
	
	//Creating the info bar values
	private PluginMessageBar.InfoBar infoBarConnection;
	
	//Creating the state values
	private boolean currentScreenEffectPlaying = false;
	private boolean attachmentQueueVisible = false;
	private ValueAnimator attachmentQueueValueAnimator = null;
	
	//Creating the view values
	private View rootView;
	private AppBarLayout appBar;
	private View scrimStatusBar;
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
	private FloatingActionButton bottomFAB;
	private TextView bottomFABBadge;
	private View bottomFABSplash;
	private AppleEffectView appleEffectView;
	private FrameLayout bottomDetailsPanel;
	
	private RecyclerView listAttachmentQueue;
	
	private MessageListRecyclerAdapter messageListAdapter;
	
	//Creating the fragment values
	private FragmentMessagingAttachments fragmentAttachments;
	
	//Creating the listener values
	private final RecyclerView.OnScrollListener messageListScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			//Getting the layout manager
			LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			int itemsScrolledFromBottom = linearLayoutManager.getItemCount() - 1 - linearLayoutManager.findLastVisibleItemPosition();
			
			//Showing the FAB if the user has scrolled more than the threshold items
			setFABVisibility(itemsScrolledFromBottom > quickScrollFABThreshold);
			
			//Marking viewed items as read
			if(itemsScrolledFromBottom < viewModel.conversationInfo.getUnreadMessageCount()) {
				viewModel.conversationInfo.setUnreadMessageCount(itemsScrolledFromBottom);
				updateUnreadIndicator();
			}
			
			//Loading chunks if the user is scrolled to the top
			if(linearLayoutManager.findFirstVisibleItemPosition() < progressiveLoadThreshold && !viewModel.isProgressiveLoadInProgress() && !viewModel.progressiveLoadReachedLimit) {
				recyclerView.post(viewModel::loadNextChunk);
			}
		}
	};
	private final BroadcastReceiver contactsUpdateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			rebuildContactViews();
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
			//messageInputField.setMaxHeight(height - ResourceHelper.dpToPx(200));
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
	private final TextView.OnEditorActionListener inputFieldEditorActionListener = (textView, actionID, event) -> {
		/*
		IME_ACTION_DONE is triggered on the Google Pixelbook
		IME_NULL is triggered with external wireless keyboards
		 */
		if(actionID == EditorInfo.IME_ACTION_DONE ||
		   (actionID == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getSource() != InputDevice.SOURCE_UNKNOWN)) {
			//Sending the message
			submitInput();
			
			//Returning true
			return true;
		}
		
		//Returning false
		return false;
	};
	
	//Creating the other values
	private boolean currentSendButtonState = true;
	private boolean toolbarVisible = true;
	
	private File currentTargetSAFFile = null;
	
	public Messaging() {
		//Setting the plugins;
		addPlugin(pluginMessageBar = new PluginMessageBar());
		addPlugin(pluginCS = new PluginConnectionService());
		addPlugin(pluginRXD = new PluginRXDisposable());
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
		
		scrimStatusBar = findViewById(R.id.scrim_statusbar);
		labelLoading = findViewById(R.id.loading_text);
		groupLoadFail = findViewById(R.id.group_error);
		labelLoadFail = findViewById(R.id.group_error_label);
		messageList = findViewById(R.id.list_messages);
		inputBar = findViewById(R.id.inputbar);
		inputBarShadow = ThemeHelper.shouldUseAMOLED(this) ? findViewById(R.id.bottomshadow_amoled) : findViewById(R.id.bottomshadow);
		attachmentsPanel = findViewById(R.id.panel_attachments);
		inputBarShadow.setVisibility(View.VISIBLE);
		buttonSendMessage = inputBar.findViewById(R.id.button_send);
		buttonAddContent = inputBar.findViewById(R.id.button_addcontent);
		messageInputField = inputBar.findViewById(R.id.messagebox);
		bottomFAB = findViewById(R.id.fab_bottom);
		bottomFABBadge = findViewById(R.id.fab_bottom_badge);
		bottomFABSplash = findViewById(R.id.fab_bottom_splash);
		appleEffectView = findViewById(R.id.effect_foreground);
		bottomDetailsPanel = findViewById(R.id.panel_messaginginfo);
		
		listAttachmentQueue = findViewById(R.id.inputbar_attachments);
		
		//Setting the window layout
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(false);
		} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		} else {
			getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
		
		getWindow().setStatusBarColor(Color.TRANSPARENT);
		
		//Listening for window inset changes
		ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
			appBar.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), appBar.getPaddingBottom());
			appBar.post(() -> {
				messageList.setPadding(insets.getSystemWindowInsetLeft(), appBar.getHeight(), insets.getSystemWindowInsetRight(), messageList.getPaddingBottom());
			});
			scrimStatusBar.getLayoutParams().height = insets.getSystemWindowInsetTop();
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				//Adding padding to the details menu
				View detailsMenu = findViewById(R.id.group_messaginginfo_content);
				if(detailsMenu != null) detailsMenu.setPadding(insets.getSystemWindowInsetLeft(), detailsMenu.getPaddingTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
				
				//Adding side margins to the details panel
				ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) bottomDetailsPanel.getLayoutParams();
				layoutParams.leftMargin = insets.getSystemWindowInsetLeft();
				layoutParams.rightMargin = insets.getSystemWindowInsetRight();
			} else {
				//Simply applying padding to the entire root view
				rootView.setPadding(rootView.getPaddingLeft(), rootView.getPaddingTop(), rootView.getPaddingRight(), insets.getSystemWindowInsetBottom());
			}
			
			//currentInsetPaddingBottom = insets.getSystemWindowInsetBottom();
			
			//if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return WindowInsets.CONSUMED;
			//else return windowInsets.consumeSystemWindowInsets();
			return insets;
		});
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			//Listening for window insets and animation callbacks for the input bar
			AnimatingInsetsCallback callback = new AnimatingInsetsCallback(inputBar);
			inputBar.setOnApplyWindowInsetsListener(callback);
			inputBar.setWindowInsetsAnimationCallback(callback);
		} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			//Listening for window insets for the input bar
			inputBar.setOnApplyWindowInsetsListener((view, windowInsets) -> {
				inputBar.setPadding(windowInsets.getSystemWindowInsetLeft(), inputBar.getPaddingTop(), windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
				return windowInsets.consumeSystemWindowInsets();
			});
		}
		
		//Setting the plugin views
		pluginMessageBar.setParentView(findViewById(R.id.infobar_container));
		
		if(savedInstanceState != null) {
			//Restoring the fragments
			fragmentAttachments = (FragmentMessagingAttachments) getSupportFragmentManager().getFragment(savedInstanceState, keyFragmentAttachments);
		}
		
		//Enforcing the maximum content width
		WindowHelper.enforceContentWidthView(getResources(), messageList);
		
		//Configuring the AMOLED theme
		if(ThemeHelper.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Setting the status bar color
		PlatformHelper.updateChromeOSStatusBar(this);
		
		//Creating the filler values
		String fillerText = null;
		Uri[] fillerFiles = null;
		
		//Checking if the request is a send intent
		if(Intent.ACTION_SEND.equals(getIntent().getAction()) || Intent.ACTION_SENDTO.equals(getIntent().getAction())) {
			long conversationID = -1;
			String[] recipients = null;
			
			//Checking if the request came from direct share
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && getIntent().hasExtra(Intent.EXTRA_SHORTCUT_ID)) {
				conversationID = ShortcutHelper.shortcutIDToConversationID(getIntent().getStringExtra(Intent.EXTRA_SHORTCUT_ID));
			}
			//Checking if the request came from an SMS link
			else if(getIntent().getDataString() != null) {
				//Getting the recipients
				recipients = Uri.decode(getIntent().getDataString())
						.replaceAll("sms:", "")
						.replaceAll("smsto:", "")
						.replaceAll("mms:", "")
						.replaceAll("mmsto:", "")
						.split(",");
				
				//Normalizing the recipients
				AddressHelper.normalizeAddresses(recipients);
			}
			
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
			final long finalConversationID = conversationID;
			final String[] finalRecipients = recipients;
			viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
				@NonNull
				@Override
				public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
					if(finalConversationID != -1) return (T) new ActivityViewModel(getApplication(), finalConversationID);
					else if(finalRecipients != null) return (T) new ActivityViewModel(getApplication(), finalRecipients);
					else return (T) new ActivityViewModel(getApplication(), -1);
				}
			}).get(ActivityViewModel.class);
		} else {
			//Getting the conversation ID
			long conversationID = getIntent().getLongExtra(intentParamTargetID, -1);
			
			//Getting the view model
			viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
				@NonNull
				@Override
				public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
					return (T) new ActivityViewModel(getApplication(), conversationID);
				}
			}).get(ActivityViewModel.class);
			
			//Getting the filler data
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getIntent().hasExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT)) fillerText = getIntent().getStringExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT); //Notification inline reply text (only supported on Android P and above)
			else if(getIntent().hasExtra(intentParamDataText)) fillerText = getIntent().getStringExtra(intentParamDataText); //Shared text from activity
			
			if(getIntent().getBooleanExtra(intentParamDataFile, false)) {
				ClipData clipData = getIntent().getClipData();
				fillerFiles = new Uri[clipData.getItemCount()];
				for(int i = 0; i < clipData.getItemCount(); i++) fillerFiles[i] = clipData.getItemAt(i).getUri();
			}
		}
		
		//Enabling the toolbar's up navigation
		if(!isInBubble()) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
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
				messageInputField.setMaxHeight(height - ResourceHelper.dpToPx(400));
			}
		});
		
		//Setting the listeners
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(rootLayoutListener);
		messageInputField.addTextChangedListener(inputFieldTextWatcher);
		messageInputField.setOnEditorActionListener(inputFieldEditorActionListener);
		buttonSendMessage.setOnClickListener(view -> submitInput());
		buttonAddContent.setOnClickListener(view -> {
			if(viewModel.isAttachmentsPanelOpen) {
				closeAttachmentsPanel(true);
			} else {
				openAttachmentsPanel(false, true);
			}
		});
		bottomFAB.setOnClickListener(view -> messageListAdapter.scrollToBottom());
		appleEffectView.setFinishListener(() -> currentScreenEffectPlaying = false);
		
		viewModel.titleLD.observe(this, (title) -> getSupportActionBar().setTitle(title));
		
		LocalBroadcastManager.getInstance(this).registerReceiver(contactsUpdateBroadcastReceiver, new IntentFilter(MainApplication.localBCContactUpdate));
		
		//Configuring the input field
		messageInputField.setContentProcessor((uri, type, name, size) -> {
			if(!checkQueueCapacity(1)) return;
			
			viewModel.queueFile(new FileLinked(Union.ofB(uri), FileHelper.cleanFileName(name), size, type));
		});
		
		//Setting up the attachments
		{
			listAttachmentQueue.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
			listAttachmentQueue.setAdapter(new AttachmentsQueueRecyclerAdapter(viewModel.queueList));
			
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
		if(fillerFiles != null) queueURIs(fillerFiles);
		
		//Creating the info bars
		infoBarConnection = pluginMessageBar.create(R.drawable.disconnection, null);
		
		getWindow().getDecorView().post(() -> {
			//Restoring the panels
			openAttachmentsPanel(true, false);
		});
		
		//Updating the send button
		updateSendButton(true);
		
		//Registering the observers
		viewModel.stateLD.observe(this, this::updateUI);
		viewModel.progressiveLoadInProgress.observe(this, value -> messageListAdapter.setShowTopProgressBar(value));
		pluginRXD.activity().add(viewModel.subjectProgressiveLoadUpdate.subscribe(count -> messageListAdapter.notifyItemRangeInserted(messageListAdapter.mapRecyclerIndex(0), count)));
		
		pluginRXD.activity().add(viewModel.subjectQueueListAdd.subscribe(this::updateQueueAdded));
		pluginRXD.activity().add(viewModel.subjectQueueListRemove.subscribe(this::updateQueueRemoved));
		pluginRXD.activity().add(viewModel.subjectQueueListUpdate.subscribe(this::updateQueueUpdated));
		
		messageInputField.setOnTouchListener((View view, MotionEvent event) -> {
			closeAttachmentsPanel(true);
			return false;
		});
		
		//Subscribing to messaging updates
		pluginRXD.activity().add(ReduxEmitterNetwork.getMessageUpdateSubject().subscribe(this::updateMessageList));
	}
	
	@Override
	public void onStart() {
		//Calling the super method
		super.onStart();
		
		//Subscribing to connection state updates
		pluginRXD.ui().add(ReduxEmitterNetwork.getConnectionStateSubject().subscribe(this::updateServerWarning));
	}
	
	@Override
	public void onResume() {
		//Calling the super method
		super.onResume();
		
		//Adding the phantom reference
		foregroundConversations.add(new WeakReference<>(this));
		
		//Clearing the notifications (unless we're running in a bubble, as clearing the notification closes the window immediately)
		if(!isInBubble()) {
			NotificationManager notificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
			int notificationID = (int) viewModel.conversationIDTarget;
			NotificationHelper.cancelMessageNotification(notificationManager, notificationID);
			notificationManager.cancel(NotificationHelper.notificationTagMessageError, notificationID);
		}
	}
	
	@Override
	public void onPause() {
		//Calling the super method
		super.onPause();
		
		//Iterating over the foreground conversations
		for(Iterator<WeakReference<Messaging>> iterator = foregroundConversations.iterator(); iterator.hasNext();) {
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
		
		//Saving the draft message
		viewModel.applyDraftMessage(messageInputField.getText().toString());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//Detatching the recycler view adapter
		messageList.setAdapter(null);
	}
	
	/**
	 * Updates the connection warning banner based on a connection event
	 */
	private void updateServerWarning(ReduxEventConnection event) {
		if(event.getState() == ConnectionState.disconnected) {
			showServerWarning(((ReduxEventConnection.Disconnected) event).getCode());
		} else {
			hideServerWarning();
		}
	}
	
	/**
	 * Updates the activity's UI in response to a change in state
	 */
	private void updateUI(int state) {
		switch(state) {
			case ActivityViewModel.stateLoadingConversation:
				labelLoading.setVisibility(View.VISIBLE);
				groupLoadFail.setVisibility(View.GONE);
				
				setMessageInputBarState(false);
				
				break;
			case ActivityViewModel.stateLoadingMessages:
				labelLoading.setVisibility(View.VISIBLE);
				groupLoadFail.setVisibility(View.GONE);
				
				setMessageInputBarState(false);
				
				//Coloring the UI
				updateUIColor();
				
				//Setting the message input field hint
				messageInputField.setHint(getMessageFieldPlaceholder());
				
				//Updating the send button
				//updateSendButton();
				
				break;
			case ActivityViewModel.stateReady: {
				labelLoading.setVisibility(View.GONE);
				groupLoadFail.setVisibility(View.GONE);
				
				setMessageInputBarState(true);
				
				//Setting the list adapter
				messageListAdapter = new MessageListRecyclerAdapter(viewModel.conversationItemList);
				messageList.setAdapter(messageListAdapter);
				messageList.addOnScrollListener(messageListScrollListener);
				viewModel.conversationActionsLD.observe(this, messageListAdapter::setConversationActions);
				
				//Setting the message input field hint
				messageInputField.setHint(getMessageFieldPlaceholder());
				
				//Checking if there are drafts files saved in the conversation
				if(!viewModel.conversationInfo.getDraftFiles().isEmpty()) {
					//Showing the file queue
					showFileQueue(false);
					
					//Updating the send button
					updateSendButton(false);
				}
				
				//Restoring the draft message
				if(messageInputField.getText().length() == 0) {
					messageInputField.setText(viewModel.conversationInfo.getDraftMessage());
				}
				
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
				viewModel.updateConversationActions();
				
				break;
			}
			case ActivityViewModel.stateFailedMatching:
			case ActivityViewModel.stateFailedConversation:
				labelLoading.setVisibility(View.GONE);
				groupLoadFail.setVisibility(View.VISIBLE);
				labelLoadFail.setText(R.string.message_loaderror_conversation);
				
				setMessageInputBarState(false);
				
				break;
			case ActivityViewModel.stateFailedMessages:
				labelLoading.setVisibility(View.GONE);
				groupLoadFail.setVisibility(View.VISIBLE);
				labelLoadFail.setText(R.string.message_loaderror_messages);
				
				setMessageInputBarState(false);
				
				break;
		}
	}
	
	/**
	 * Updates the message list in response to a messaging event
	 */
	private void updateMessageList(ReduxEventMessaging event) {
		//Ignoring the event if we aren't loaded yet
		if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady || messageListAdapter == null) return;
		
		if(event instanceof ReduxEventMessaging.Message) {
			ReduxEventMessaging.Message messageEvent = (ReduxEventMessaging.Message) event;
			
			//Getting the conversation items for this conversation
			List<ReplaceInsertResult> resultList = messageEvent.getConversationItems().stream()
					.filter(pair -> pair.first.getLocalID() == viewModel.conversationInfo.getLocalID())
					.findAny().map(pair -> new ArrayList<>(pair.second)).orElse(null);
			if(resultList == null) return;
			
			applyMessageUpdate(resultList);
		} else if(event instanceof ReduxEventMessaging.ConversationUpdate) {
			ReduxEventMessaging.ConversationUpdate conversationEvent = (ReduxEventMessaging.ConversationUpdate) event;
			
			//Searching for this activity's conversation
			conversationEvent.getTransferredConversations().stream()
					.filter(transferredConversation -> transferredConversation.getClientConversation().getLocalID() == viewModel.conversationInfo.getLocalID())
					.findFirst().ifPresent(transferredConversation -> {
				//Updating the conversation details
				ConversationInfo serverConversation = transferredConversation.getServerConversation();
				viewModel.conversationInfo.setGUID(serverConversation.getGUID());
				viewModel.conversationInfo.setState(serverConversation.getState());
				viewModel.conversationInfo.setTitle(serverConversation.getTitle());
				viewModel.updateConversationTitle();
				
				//Adding the transferred messages
				applyMessageUpdate(transferredConversation.getServerConversationItems());
			});
		} else if(event instanceof ReduxEventMessaging.MessageState) {
			ReduxEventMessaging.MessageState eventState = (ReduxEventMessaging.MessageState) event;
			IntStream.range(0, viewModel.conversationItemList.size())
					.filter(i -> viewModel.conversationItemList.get(i).getLocalID() == eventState.getMessageID())
					.findAny()
					.ifPresent(i -> {
						//Updating the message
						MessageInfo messageInfo = (MessageInfo) viewModel.conversationItemList.get(i);
						messageInfo.setMessageState(eventState.getStateCode());
						messageInfo.setDateRead(eventState.getDateRead());
						
						//Trying to set conversation targets
						MessageTargetUpdate update = null;
						if(messageInfo.getMessageState() == MessageState.delivered) {
							update = viewModel.tryApplyDeliveredTarget(messageInfo);
						} else if(messageInfo.getMessageState() == MessageState.read) {
							update = viewModel.tryApplyReadTarget(messageInfo);
						}
						
						//Updating the adapter
						messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(i), MessageListPayload.state);
						if(update != null) {
							messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(viewModel.conversationItemList.indexOf(update.getNewMessage())), MessageListPayload.status);
							for(MessageInfo changedMessage : update.getOldMessages()) {
								messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(viewModel.conversationItemList.indexOf(changedMessage)), MessageListPayload.status);
							}
						}
					});
		} else if(event instanceof ReduxEventMessaging.MessageError) {
			ReduxEventMessaging.MessageError eventError = (ReduxEventMessaging.MessageError) event;
			IntStream.range(0, viewModel.conversationItemList.size())
					.filter(i -> viewModel.conversationItemList.get(i).getLocalID() == eventError.getMessageInfo().getLocalID())
					.findAny()
					.ifPresent(i -> {
						//Updating the message
						MessageInfo messageInfo = (MessageInfo) viewModel.conversationItemList.get(i);
						messageInfo.setErrorCode(eventError.getErrorCode());
						messageInfo.setErrorDetailsAvailable(eventError.getErrorDetails() != null);
						messageInfo.setErrorDetails(eventError.getErrorDetails());
						
						//Updating the adapter
						messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(i), MessageListPayload.state);
					});
		} else if(event instanceof ReduxEventMessaging.MessageDelete) {
			IntStream.range(0, viewModel.conversationItemList.size())
					.filter(i -> viewModel.conversationItemList.get(i).getLocalID() == ((ReduxEventMessaging.MessageDelete) event).getMessageInfo().getLocalID())
					.findAny()
					.ifPresent(i -> {
						//Removing the message
						viewModel.conversationItemList.remove(i);
						
						//Updating the adapter
						messageListAdapter.notifyItemRemoved(messageListAdapter.mapRecyclerIndex(i));
					});
		} else if(event instanceof ReduxEventMessaging.AttachmentFile) {
			ReduxEventMessaging.AttachmentFile attachmentEvent = (ReduxEventMessaging.AttachmentFile) event;
			
			//Finding the message
			int messageIndex = IntStream.range(0, viewModel.conversationItemList.size())
					.filter(i -> viewModel.conversationItemList.get(i).getLocalID() == attachmentEvent.getMessageID())
					.findAny().orElse(-1);
			if(messageIndex == -1) return;
			MessageInfo messageInfo = (MessageInfo) viewModel.conversationItemList.get(messageIndex);
			
			//Finding the attachment
			int attachmentIndex = IntStream.range(0, messageInfo.getAttachments().size())
					.filter(i -> messageInfo.getAttachments().get(i).getLocalID() == attachmentEvent.getAttachmentID())
					.findAny().orElse(-1);
			if(attachmentIndex == -1) return;
			
			//Updating the attachment
			messageInfo.getAttachments().get(attachmentIndex).setFile(attachmentEvent.getFile());
			
			//Updating the adapter
			messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(messageIndex), new MessageListPayload.Attachment(attachmentIndex));
		} else if(event instanceof ReduxEventMessaging.TapbackUpdate) {
			ReduxEventMessaging.TapbackUpdate tapbackEvent = (ReduxEventMessaging.TapbackUpdate) event;
			
			//Finding the message
			int messageIndex = IntStream.range(0, viewModel.conversationItemList.size())
					.filter(i -> viewModel.conversationItemList.get(i).getLocalID() == tapbackEvent.getMetadata().getMessageID())
					.findAny().orElse(-1);
			if(messageIndex == -1) return;
			MessageInfo messageInfo = (MessageInfo) viewModel.conversationItemList.get(messageIndex);
			
			//Finding the component
			List<MessageComponent> componentList = messageInfo.getComponents();
			if(tapbackEvent.getMetadata().getComponentIndex() >= componentList.size()) return;
			MessageComponent component = componentList.get(tapbackEvent.getMetadata().getComponentIndex());
			
			//Updating the component's tapbacks
			if(tapbackEvent.isAddition()) component.getTapbacks().add(tapbackEvent.getTapbackInfo());
			else component.getTapbacks().removeIf(tapback -> tapback.getLocalID() == tapbackEvent.getTapbackInfo().getLocalID());
			
			//Updating the adapter
			messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(messageIndex), new MessageListPayload.Tapback(tapbackEvent.getMetadata().getComponentIndex()));
		} else if(event instanceof ReduxEventMessaging.StickerAdd) {
			ReduxEventMessaging.StickerAdd stickerEvent = (ReduxEventMessaging.StickerAdd) event;
			
			//Finding the message
			int messageIndex = IntStream.range(0, viewModel.conversationItemList.size())
					.filter(i -> viewModel.conversationItemList.get(i).getLocalID() == stickerEvent.getMetadata().getMessageID())
					.findAny().orElse(-1);
			if(messageIndex == -1) return;
			MessageInfo messageInfo = (MessageInfo) viewModel.conversationItemList.get(messageIndex);
			
			//Finding the component
			List<MessageComponent> componentList = messageInfo.getComponents();
			if(stickerEvent.getMetadata().getComponentIndex() >= componentList.size()) return;
			MessageComponent component = componentList.get(stickerEvent.getMetadata().getComponentIndex());
			
			//Adding the sticker
			component.getStickers().add(stickerEvent.getStickerInfo());
			
			//Updating the adapter
			messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(messageIndex), new MessageListPayload.Sticker(stickerEvent.getMetadata().getComponentIndex(), stickerEvent.getStickerInfo()));
		} else if(event instanceof ReduxEventMessaging.ReduxConversationAction) {
			//Ignoring if the event is not relevant to this conversation
			if(((ReduxEventMessaging.ReduxConversationAction) event).getConversationInfo().getLocalID() != viewModel.conversationInfo.getLocalID()) return;
			
			if(event instanceof ReduxEventMessaging.ConversationMember) {
				ReduxEventMessaging.ConversationMember memberEvent = (ReduxEventMessaging.ConversationMember) event;
				
				//Updating the members
				if(memberEvent.isJoin()) viewModel.conversationInfo.getMembers().add(memberEvent.getMember().clone());
				else viewModel.conversationInfo.getMembers().removeIf(member -> member.getAddress().equals(memberEvent.getMember().getAddress()));
				
				//Rebuilding the conversation title
				if(viewModel.conversationInfo.getTitle() == null) viewModel.updateConversationTitle();
			} else if(event instanceof ReduxEventMessaging.ConversationDelete) {
				//Close this activity
				finish();
			} else if(event instanceof ReduxEventMessaging.ConversationTitle) {
				ReduxEventMessaging.ConversationTitle titleEvent = (ReduxEventMessaging.ConversationTitle) event;
				
				//Updating the title
				viewModel.conversationInfo.setTitle(titleEvent.getTitle());
				viewModel.updateConversationTitle();
			} else if(event instanceof ReduxEventMessaging.ConversationColor) {
				ReduxEventMessaging.ConversationColor colorEvent = (ReduxEventMessaging.ConversationColor) event;
				
				//Updating the color
				viewModel.conversationInfo.setConversationColor(colorEvent.getColor());
				updateUIColor();
				if(!Preferences.getPreferenceAdvancedColor(Messaging.this)) {
					messageListAdapter.notifyItemRangeChanged(messageListAdapter.mapRecyclerIndex(0), viewModel.conversationItemList.size(), MessageListPayload.color);
				}
			} else if(event instanceof ReduxEventMessaging.ConversationMemberColor) {
				ReduxEventMessaging.ConversationMemberColor memberColorEvent = (ReduxEventMessaging.ConversationMemberColor) event;
				
				//Updating the color
				viewModel.conversationInfo.getMembers().stream()
						.filter(member -> member.getAddress().equals(memberColorEvent.getMemberInfo().getAddress()))
						.findAny().ifPresent(member -> {
					member.setColor(memberColorEvent.getColor());
					for(ListIterator<ConversationItem> iterator = viewModel.conversationItemList.listIterator(); iterator.hasNext();) {
						int i = iterator.nextIndex();
						ConversationItem item = iterator.next();
						if(item.getItemType() == ConversationItemType.message && memberColorEvent.getMemberInfo().getAddress().equals(((MessageInfo) item).getSender())) {
							messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(i), MessageListPayload.color);
						}
					}
				});
			}
		}
	}
	
	/**
	 * Applies a list of {@link ReplaceInsertResult}s to the conversation
	 */
	private void applyMessageUpdate(List<ReplaceInsertResult> replaceInsertResults) {
		//A newest-to-oldest list of messages that may become the next delivered or read target
		List<MessageInfo> messageTargetCandidates = new ArrayList<>();
		
		//Whether any of the new messages are incoming / outgoing
		int incomingMessageCount = (int) replaceInsertResults.stream().filter(result ->
				result.getTargetItem().getItemType() == ConversationItemType.message &&
						!((MessageInfo) result.getTargetItem()).isOutgoing()).count();
		boolean messageOutgoing = false;
		
		boolean wasScrolledToBottom = messageListAdapter.isScrolledToBottom();
		
		for(ReplaceInsertResult result : replaceInsertResults) {
			//Adding new items
			int insertIndex = viewModel.conversationItemList.size();
			viewModel.conversationItemList.addAll(result.getNewItems());
			messageListAdapter.notifyItemRangeInserted(messageListAdapter.mapRecyclerIndex(insertIndex), result.getNewItems().size());
			if(insertIndex > 0 && viewModel.conversationItemList.get(insertIndex - 1).getItemType() == ConversationItemType.message) messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(insertIndex - 1), MessageListPayload.flow);
			messageTargetCandidates.addAll(result.getNewItems().stream().filter(item -> item.getItemType() == ConversationItemType.message).map(item -> (MessageInfo) item).collect(Collectors.toList()));
			if(!messageOutgoing) messageOutgoing = result.getNewItems().stream().anyMatch(item -> item.getItemType() == ConversationItemType.message && ((MessageInfo) item).isOutgoing());
			
			//Applying updated items
			for(MessageInfo updatedItem : result.getUpdatedItems()) {
				//Finding a matching local item
				MessageInfo localItem = viewModel.conversationGhostList.stream()
						.filter(ghostMessage -> ghostMessage.getLocalID() == updatedItem.getLocalID())
						.findAny().orElse(null);
				if(localItem == null) continue;
				viewModel.conversationGhostList.remove(localItem);
				messageTargetCandidates.add(localItem);
				
				//Updating the local item
				if(!updatedItem.getAttachments().isEmpty() &&
						(localItem.getAttachments().size() != updatedItem.getAttachments().size() ||
								IntStream.range(0, localItem.getAttachments().size()).anyMatch(i -> localItem.getAttachments().get(i).getLocalID() != updatedItem.getAttachments().get(i).getLocalID()))) {
					localItem.getAttachments().clear();
					localItem.getAttachments().addAll(updatedItem.getAttachments());
					
					messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(viewModel.conversationItemList.indexOf(localItem)), MessageListPayload.attachmentRebuild);
				}
				
				localItem.setDate(updatedItem.getDate());
				localItem.setMessageState(updatedItem.getMessageState());
				localItem.setGuid(updatedItem.getGuid());
				localItem.setMessageState(updatedItem.getMessageState());
				localItem.setErrorDetailsAvailable(updatedItem.isErrorDetailsAvailable());
				localItem.setDateRead(updatedItem.getDateRead());
				messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(viewModel.conversationItemList.indexOf(localItem)), MessageListPayload.state);
			}
			
			//Adding new ghost items
			viewModel.conversationGhostList.addAll(result.getNewItems().stream().filter(conversationItem ->
					conversationItem instanceof MessageInfo && ((MessageInfo) conversationItem).getMessageState() == MessageState.ghost)
					.map(conversationItem -> (MessageInfo) conversationItem).collect(Collectors.toList()));
		}
		
		Collections.sort(messageTargetCandidates, (message1, message2) -> -Long.compare(message1.getDate(), message2.getDate())); //Sort newest to oldest
		
		//Trying to update conversation targets
		boolean deliveredTargetMatched = false;
		List<MessageTargetUpdate> messageTargetUpdates = new ArrayList<>();
		for(MessageInfo messageInfo : messageTargetCandidates) {
			if(!deliveredTargetMatched && messageInfo.getMessageState() == MessageState.delivered) {
				MessageTargetUpdate update = viewModel.tryApplyDeliveredTarget(messageInfo);
				if(update != null) {
					messageTargetUpdates.add(update);
					deliveredTargetMatched = true;
				}
			} else if(messageInfo.getMessageState() == MessageState.read) {
				MessageTargetUpdate update = viewModel.tryApplyReadTarget(messageInfo);
				if(update != null) {
					messageTargetUpdates.add(update);
					break;
				}
			}
		}
		
		//Updating the adapter
		for(MessageTargetUpdate update : messageTargetUpdates) {
			messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(viewModel.conversationItemList.indexOf(update.getNewMessage())), MessageListPayload.status);
			for(MessageInfo changedMessage : update.getOldMessages()) {
				messageListAdapter.notifyItemChanged(messageListAdapter.mapRecyclerIndex(viewModel.conversationItemList.indexOf(changedMessage)), MessageListPayload.status);
			}
		}
		
		//Checking if we have any new incoming messages
		if(incomingMessageCount > 0) {
			//Playing a sound
			if(Preferences.getPreferenceMessageSounds(this)) {
				SoundHelper.playSound(viewModel.soundPool, viewModel.soundIDMessageIncoming);
			}
			
			//Updating the unread count
			if(!wasScrolledToBottom) {
				viewModel.conversationInfo.setUnreadMessageCount(viewModel.conversationInfo.getUnreadMessageCount() + incomingMessageCount);
				updateUnreadIndicator();
			}
		}
		
		//Scrolling to the bottom of the list
		if(messageOutgoing || wasScrolledToBottom) {
			messageListAdapter.scrollToBottom();
		}
		
		//Updating the reply suggestions
		viewModel.updateConversationActions();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		
		if(requestCode == intentSaveFileSAF) {
			if(resultCode == RESULT_OK) {
				//Saving the file
				ExternalStorageHelper.exportFile(this, currentTargetSAFFile, intent.getData());
			}
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
		if(item.getItemId() == android.R.id.home) {
			//Going back
			finish();
			return true;
		} else if(item.getItemId() == R.id.action_details) {
			if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady) return true;
			
			//Opening the details panel
			FragmentMessagingDetails fragment = new FragmentMessagingDetails(viewModel.conversationInfo);
			fragment.show(getSupportFragmentManager(), null);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onBackPressed() {
		//Closing the attachments panel if it is open
		if(viewModel.isAttachmentsPanelOpen) closeAttachmentsPanel(true);
		//Otherwise passing the event to the superclass
		else super.onBackPressed();
	}
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//Saving the fragment
		if(fragmentAttachments != null && fragmentAttachments.isAdded()) getSupportFragmentManager().putFragment(outState, keyFragmentAttachments, fragmentAttachments);
	}
	
	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		super.onAttachFragment(fragment);
		
		if(fragment instanceof FragmentMessagingAttachments) {
			((FragmentMessagingAttachments) fragment).setCommunicationsCallback(new FragmentMessagingAttachments.FragmentCommunicationQueue() {
				@Override
				public long[] getMediaStoreIDs() {
					return viewModel.queueList.stream().mapToLong(FileQueued::getMediaStoreID).filter(id -> id != -1).toArray();
				}
				
				@Override
				public int getQueueIndex(long mediaStoreID) {
					return IntStream.range(0, viewModel.queueList.size()).filter(i -> viewModel.queueList.get(i).getMediaStoreID() == mediaStoreID).findAny().orElse(-1);
				}
				
				@Override
				public void queueFile(FileLinked file) {
					if(!checkQueueCapacity(1)) return;
					
					//Queuing the file
					viewModel.queueFile(file);
				}
				
				@Override
				public void dequeueFile(FileLinked file) {
					if(file.getMediaStoreData() == null) return;
					
					//Finding and dequeuing the file
					viewModel.queueList.stream().filter(fileQueued -> fileQueued.getMediaStoreID() == file.getMediaStoreData().getMediaStoreID()).findAny().ifPresent(viewModel::dequeueFile);
				}
				
				@Override
				public void queueText(String text) {
					String currentInputText = messageInputField.getText().toString();
					if(currentInputText.isEmpty()) {
						messageInputField.setText(text);
					} else {
						//Adding a whitespace character if there is none
						if(!Character.isWhitespace(currentInputText.charAt(currentInputText.length() - 1))) currentInputText += " ";
						messageInputField.setText(currentInputText + text);
					}
				}
			});
		}
	}
	
	/**
	 * Gets the ID of the currently loaded conversation of this activity, or -1 if unavailable
	 */
	public long getConversationID() {
		if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady) return -1;
		return viewModel.conversationInfo.getLocalID();
	}
	
	/**
	 * Gets if this messaging activity is launched as a floating bubble
	 */
	private boolean isInBubble() {
		return getIntent().getBooleanExtra(intentParamBubble, false);
	}
	
	/**
	 * Adapts the view for an AMOLED display
	 */
	private void setDarkAMOLED() {
		ThemeHelper.setActivityAMOLEDBase(this);
		appBar.setBackgroundColor(ColorConstants.colorAMOLED);
		
		//Setting the input bar background color
		inputBar.setBackgroundColor(ColorConstants.colorAMOLED);
		
		//Setting the attachments view
		View attachmentsView = findViewById(R.id.pane_attachments);
		if(attachmentsView != null) {
			attachmentsView.setBackgroundColor(ColorConstants.colorAMOLED);
		}
	}
	
	/**
	 * Updates the view when a queue file is added
	 * @param pair The index and the added item
	 */
	private void updateQueueAdded(Pair<Integer, FileQueued> pair) {
		//Updating the adapter
		listAttachmentQueue.getAdapter().notifyItemInserted(pair.first);
		
		//Showing the attachment queue
		if(!attachmentQueueVisible) {
			showFileQueue(true);
		}
		
		//Updating the fragment
		if(fragmentAttachments != null) {
			FileQueued fileQueued = pair.second;
			if(fileQueued.getMediaStoreID() != -1) fragmentAttachments.onFileQueued(fileQueued.getMediaStoreID());
		}
		
		//Scrolling to the end of the list
		listAttachmentQueue.smoothScrollToPosition(viewModel.queueList.size() - 1);
		
		//Updating the send button
		updateSendButton(false);
	}
	
	/**
	 * Updates the view when a queue file is removed
	 * @param pair The index and the removed item
	 */
	private void updateQueueRemoved(Pair<Integer, FileQueued> pair) {
		//Updating the adapter
		listAttachmentQueue.getAdapter().notifyItemRemoved(pair.first);
		
		//Updating the fragment
		if(fragmentAttachments != null) {
			FileQueued fileQueued = pair.second;
			if(fileQueued.getMediaStoreID() != -1) fragmentAttachments.onFileDequeued(fileQueued.getMediaStoreID());
		}
		
		//Checking if there are no more queued files
		if(viewModel.queueList.isEmpty()) {
			//Updating the send button
			updateSendButton(false);
			
			//Hiding the attachment queue
			if(attachmentQueueVisible) {
				hideFileQueue(true);
			}
		}
	}
	
	/**
	 * Updates the view when a queue file is updated
	 * @param pair The index and the updated item
	 */
	private void updateQueueUpdated(Pair<Integer, FileQueued> pair) {
		//Updating the adapter
		listAttachmentQueue.getAdapter().notifyItemChanged(pair.first, AttachmentsQueueRecyclerAdapter.payloadUpdateState);
	}
	
	/**
	 * Updates the view when all queued files are removed
	 * @param files The queued files that have been removed
	 */
	private void updateQueueCleared(List<FileQueued> files) {
		//Updating the adapter
		listAttachmentQueue.getAdapter().notifyItemRangeRemoved(0, files.size());
		
		//Updating the fragment
		if(fragmentAttachments != null) {
			for(FileQueued fileQueued : files) {
				if(fileQueued.getMediaStoreID() != -1) fragmentAttachments.onFileDequeued(fileQueued.getMediaStoreID());
			}
		}
		
		//Updating the send button
		updateSendButton(false);
		
		//Hiding the attachment queue
		hideFileQueue(true);
	}
	
	private void showFileQueue(boolean animate) {
		//Ignoring if there is no change in state
		if(attachmentQueueVisible) return;
		attachmentQueueVisible = true;
		
		if(!animate) {
			listAttachmentQueue.setVisibility(View.VISIBLE);
			
			return;
		}
		
		listAttachmentQueue.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		
		//Cancelling the current animation
		if(attachmentQueueValueAnimator != null) {
			attachmentQueueValueAnimator.cancel();
		}
		
		//Animating the attachment queue height
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
			}
		});
		anim.addUpdateListener(valueAnimator -> {
			listAttachmentQueue.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
			listAttachmentQueue.requestLayout();
		});
		anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
		anim.start();
		attachmentQueueValueAnimator = anim;
	}
	
	private void hideFileQueue(boolean animate) {
		//Ignoring if there is no change in state
		if(!attachmentQueueVisible) return;
		attachmentQueueVisible = false;
		
		if(!animate) {
			listAttachmentQueue.setVisibility(View.GONE);
			
			return;
		}
		
		//Cancelling the current animation
		if(attachmentQueueValueAnimator != null) {
			attachmentQueueValueAnimator.cancel();
		}
		
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
		attachmentQueueValueAnimator = anim;
	}
	
	/**
	 * Converts URIs to draft files and adds them to the queue
	 */
	private void queueURIs(Uri[] uris) {
		if(!checkQueueCapacity(uris.length)) return;
		
		//Adding the URIs as queued files
		pluginRXD.activity().add(Observable.fromArray(uris)
				.observeOn(Schedulers.io()).map(uri -> FileQueueTask.uriToFileLinkedSync(this, uri))
				.observeOn(AndroidSchedulers.mainThread()).subscribe(file -> viewModel.queueFile(file)));
	}
	
	/**
	 * Checks if there is enough room in the queue to add more files. Displays a warning to the user if the files cannot be added.
	 * @param additionCount The amount of files to check for space for in the queue
	 * @return Whether these files can be added to the queue
	 */
	private boolean checkQueueCapacity(int additionCount) {
		if(viewModel.queueList.size() + additionCount >= draftCountLimit) {
			//Displaying a toast message
			Toast.makeText(this, R.string.message_draft_limitreached, Toast.LENGTH_SHORT).show();
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Opens the attachments panel
	 * @param restore Whether to treat this action as a restore after activity start
	 * @param animate Whether to animate this change
	 */
	private void openAttachmentsPanel(boolean restore, boolean animate) {
		//Returning if the conversation is not ready or the panel is already open
		if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady || restore != viewModel.isAttachmentsPanelOpen) return;
		
		//Setting the panel as open
		viewModel.isAttachmentsPanelOpen = true;
		
		//Closing the keyboard
		hideKeyboard();
		
		if(fragmentAttachments == null) {
			//Initializing the fragment
			fragmentAttachments = new FragmentMessagingAttachments();
			fragmentAttachments.setSupportsAppleContent(viewModel.conversationInfo.getServiceHandler() == ServiceHandler.appleBridge && ServiceType.appleMessage.equals(viewModel.conversationInfo.getServiceType()));
			fragmentAttachments.setLowResContent(
					viewModel.conversationInfo.getServiceHandler() == ServiceHandler.appleBridge && ServiceType.appleSMS.equals(viewModel.conversationInfo.getServiceType()) ||
							viewModel.conversationInfo.getServiceHandler() == ServiceHandler.systemMessaging && ServiceType.systemSMS.equals(viewModel.conversationInfo.getServiceType())
			);
			fragmentAttachments.setPrimaryColor(getUIColor());
			
			//Adding the fragment
			FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
			fragmentTransaction.add(R.id.fragmentcontainer_attachments, fragmentAttachments);
			fragmentTransaction.commit();
		}
		
		//Resolving the heights
		int requestedPanelHeight = getResources().getDimensionPixelSize(R.dimen.contentpanel_height);
		int windowThreshold = WindowHelper.getWindowHeight(this) - ResourceHelper.dpToPx(contentPanelMinAllowanceDP);
		
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
	
	/**
	 * Closes the attachments panel
	 * @param animate Whether to animate this change
	 */
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
	
	/**
	 * Sends all of the messages and files in the input bar
	 */
	private void submitInput() {
		//Getting the message details
		String cleanMessageText = messageInputField.getText().toString().trim();
		if(cleanMessageText.isEmpty()) cleanMessageText = null;
		
		//Ignoring if there is no data to send, or if there is an attachment that hasn't been prepared
		if(cleanMessageText == null && viewModel.queueList.isEmpty() || viewModel.queueList.stream().anyMatch(fileQueued -> fileQueued.getFile().isA())) return;
		
		//Preparing and sending the messages
		MessageSendHelper.prepareSendMessages(this, viewModel.conversationInfo, cleanMessageText, viewModel.queueList.stream().map(fileQueued -> fileQueued.getFile().getB()).collect(Collectors.toList()), pluginCS.getConnectionManager()).subscribe();
		/* MessageSendHelper.prepareMessages(this, viewModel.conversationInfo, cleanMessageText, viewModel.queueList.stream().map(fileQueued -> fileQueued.getFile().getB()).collect(Collectors.toList()))
				.flatMapCompletable(message -> MessageSendHelper.sendMessage(this, viewModel.conversationInfo, message, pluginCS.getConnectionManager())).subscribe(); */
		
		//Clearing the message
		messageInputField.setText(null);
		List<FileQueued> queueList = new ArrayList<>(viewModel.queueList);
		viewModel.clearDrafts();
		updateQueueCleared(queueList);
		
		//Updating the activity
		handleMessageSent();
	}
	
	/**
	 * Updates the conversation and the activity in response to a message sent by the user
	 */
	private void handleMessageSent() {
		//Unarchiving the conversation
		if(viewModel.conversationInfo.isArchived()) {
			ConversationActionTask.archiveConversations(Collections.singleton(viewModel.conversationInfo), false).subscribe();
		}
		
		//Clearing the conversation actions
		viewModel.clearConversationActions();
		
		//Playing a sound
		if(Preferences.getPreferenceMessageSounds(this)) {
			SoundHelper.playSound(viewModel.soundPool, viewModel.soundIDMessageOutgoing);
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
	
	/**
	 * Colors accented UI components with the service or conversation color
	 */
	private void updateUIColor() {
		//Ignoring if the conversation is invalid
		if(viewModel.conversationInfo == null) return;
		
		//Getting the color
		int color = getUIColor();
		int darkerColor = ColorMathHelper.darkenColor(color);
		int lighterColor = ColorMathHelper.lightenColor(color);
		
		//Coloring tagged parts of the UI
		ViewHelper.colorTaggedUI(getResources(), findViewById(android.R.id.content), color);
		
		//Coloring the unique UI components
		updateSendButton(true);
		if(viewModel.lastUnreadCount == 0) bottomFAB.setImageTintList(ColorStateList.valueOf(color));
		else bottomFAB.setBackgroundTintList(ColorStateList.valueOf(color));
		bottomFABBadge.setBackgroundTintList(ColorStateList.valueOf(darkerColor));
		bottomFABSplash.setBackgroundTintList(ColorStateList.valueOf(lighterColor));
		
		//Coloring the info bars
		infoBarConnection.setColor(color);
	}
	
	/**
	 * Gets the UI color, based off of the conversation color and dark theme
	 */
	private int getUIColor() {
		//If the user has enabled rainbow chats, get the color based on the conversation's setting
		if(Preferences.getPreferenceAdvancedColor(this)) {
			int color = viewModel.conversationInfo.getConversationColor();
			
			//Lighten the color for night mode
			if(ThemeHelper.isNightMode(getResources())) {
				color = ColorMathHelper.darkModeLightenColor(color);
			}
			
			return color;
		} else {
			//Otherwise, grab the color from the service type
			//These colors are automatically adapted for night mode based on their resource overrides
			return ColorHelper.getServiceColor(getResources(), viewModel.conversationInfo.getServiceHandler(), viewModel.conversationInfo.getServiceType());
		}
	}
	
	public void onClickRetryLoad(View view) {
		int state = viewModel.stateLD.getValue();
		if(state == ActivityViewModel.stateFailedMatching) viewModel.findCreateConversationMMSSMS();
		if(state == ActivityViewModel.stateFailedConversation) viewModel.loadConversation();
		else if(state == ActivityViewModel.stateFailedMessages) viewModel.loadMessages();
	}
	
	/**
	 * Sets whether the message input bar can be interacted with
	 */
	void setMessageInputBarState(boolean enabled) {
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
	
	/**
	 * Updates the state of the send button
	 */
	private void updateSendButton(boolean restore) {
		//Returning if the conversation isn't ready
		if(viewModel.conversationInfo == null) return;
		
		//Getting the send button state
		boolean state = !messageInputField.getText().toString().trim().isEmpty() || !viewModel.queueList.isEmpty();
		if(currentSendButtonState == state && !restore) return;
		currentSendButtonState = state;
		
		//Updating the button
		buttonSendMessage.setClickable(state);
		int targetColor = state ? getUIColor() : ResourceHelper.resolveColorAttr(this, android.R.attr.colorControlNormal);
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
		if(viewModel.stateLD.getValue() != ActivityViewModel.stateReady || messageListAdapter == null) return;
		
		//Updating the title
		if(viewModel.conversationInfo.getTitle() == null) viewModel.updateConversationTitle();
		
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
	
	/**
	 * Gets the placeholder message to display in the input field
	 */
	private String getMessageFieldPlaceholder() {
		//AirMessage bridge
		if(viewModel.conversationInfo.getServiceHandler() == ServiceHandler.appleBridge) {
			//Returning a generic message if the service is invalid
			if(viewModel.conversationInfo.getServiceType() == null) return getResources().getString(R.string.imperative_messageinput);
			
			switch(viewModel.conversationInfo.getServiceType()) {
				case ServiceType.appleMessage:
					//iMessage
					return getResources().getString(R.string.title_imessage);
					//SMS bridge
				case ServiceType.appleSMS:
					return getResources().getString(R.string.title_textmessageforwarding);
				default:
					return viewModel.conversationInfo.getServiceType();
			}
		}
		//System messaging
		else if(viewModel.conversationInfo.getServiceHandler() == ServiceHandler.systemMessaging) {
			switch(viewModel.conversationInfo.getServiceType()) {
				case ServiceType.systemSMS:
					return getResources().getString(R.string.title_textmessage);
				case ServiceType.systemRCS:
					return getResources().getString(R.string.title_rcs);
				default:
					return viewModel.conversationInfo.getServiceType();
			}
		}
		
		//Returning a generic message
		return getResources().getString(R.string.imperative_messageinput);
	}
	
	/**
	 * Shows the server warning banner
	 * @param reason The connection error code
	 */
	private void showServerWarning(@ConnectionErrorCode int reason) {
		//Getting the error details
		ErrorDetailsHelper.ErrorDetails details = ErrorDetailsHelper.getErrorDetails(reason, false);
		ErrorDetailsHelper.ErrorDetails.Button button = details.getButton();
		
		//Applying the error details to the info bar
		infoBarConnection.setText(getResources().getString(details.getLabel()));
		if(button == null) {
			infoBarConnection.removeButton();
		} else {
			infoBarConnection.setButton(getResources().getString(button.getLabel()), view -> button.getClickListener().accept(this, pluginCS.getConnectionManager()));
		}
		
		//Showing the info bar
		infoBarConnection.show();
	}
	
	/**
	 * Hides the server warning banner
	 */
	private void hideServerWarning() {
		infoBarConnection.hide();
	}
	
	/**
	 * Hides the toolbar at the top of the screen
	 */
	void hideToolbar() {
		//Returning if the toolbar is already invisible
		if(!toolbarVisible) return;
		
		//Setting the toolbar as invisible
		toolbarVisible = false;
		
		//Hiding the app bar
		appBar.setVisibility(View.GONE);
		scrimStatusBar.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Shows the toolbar at the top of the screen
	 */
	void showToolbar() {
		//Returning if the toolbar is already visible
		if(toolbarVisible) return;
		
		//Setting the toolbar as visible
		toolbarVisible = true;
		
		//Showing the app bar
		appBar.setVisibility(View.VISIBLE);
		scrimStatusBar.setVisibility(View.GONE);
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
		bottomFAB.setBackgroundTintList(ColorStateList.valueOf(getUIColor()));
		bottomFAB.setImageTintList(ColorStateList.valueOf(ResourceHelper.resolveColorAttr(this, R.attr.colorOnPrimary)));
		
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
		int colorTint = getUIColor();
		
		//Checking if there are any unread messages
		if(viewModel.conversationInfo.getUnreadMessageCount() > 0) {
			//Coloring the FAB
			bottomFAB.setBackgroundTintList(ColorStateList.valueOf(colorTint));
			bottomFAB.setImageTintList(ColorStateList.valueOf(ResourceHelper.resolveColorAttr(this, R.attr.colorOnPrimary)));
			
			//Updating the badge text
			bottomFABBadge.setText(LanguageHelper.intToFormattedString(getResources(), viewModel.conversationInfo.getUnreadMessageCount()));
			
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
				bottomFABSplash.setScaleX(1);
				bottomFABSplash.setScaleY(1);
				bottomFABSplash.setAlpha(1);
				bottomFABSplash.setVisibility(View.VISIBLE);
				bottomFABSplash.animate()
						.scaleX(2.5F)
						.scaleY(2.5F)
						.alpha(0)
						.setDuration(1000)
						.setInterpolator(new DecelerateInterpolator())
						.withEndAction(() -> bottomFABSplash.setVisibility(View.GONE))
						.start();
			}
		} else {
			//Restoring the FAB color
			bottomFAB.setBackgroundTintList(ColorStateList.valueOf(ResourceHelper.resolveColorAttr(this, android.R.attr.colorBackgroundFloating)));
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
	
	private void playScreenEffect(String effect, View target) {
		//Returning if an effect is already playing
		if(currentScreenEffectPlaying) return;
		currentScreenEffectPlaying = true;
		
		//TODO finish implementation of effects
		switch(effect) {
			case SendStyleHelper.appleSendStyleScrnEcho:
				//Activating the effect view
				appleEffectView.playEcho(target);
				break;
			case SendStyleHelper.appleSendStyleScrnSpotlight:
				currentScreenEffectPlaying = false;
				break;
			case SendStyleHelper.appleSendStyleScrnBalloons:
				appleEffectView.playBalloons();
				break;
			case SendStyleHelper.appleSendStyleScrnConfetti: {
				//Activating the Konfetti view
				KonfettiView konfettiView = findViewById(R.id.konfetti);
				konfettiView.build()
						.addColors(ColorConstants.effectColors)
						.setDirection(0D, 359D)
						.setSpeed(4F, 8F)
						.setFadeOutEnabled(true)
						.setTimeToLive(5000L)
						.addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
						.addSizes(new Size(12, 5), new Size(16, 6))
						.setPosition(-50F, konfettiView.getWidth() + 50F, -50F, -50F)
						.streamFor(300, confettiDuration);
				
				//Setting the timer to mark the effect as finished
				new Handler().postDelayed(() -> currentScreenEffectPlaying = false, confettiDuration * 5);
				
				break;
			}
			case SendStyleHelper.appleSendStyleScrnLove:
				currentScreenEffectPlaying = false;
				break;
			case SendStyleHelper.appleSendStyleScrnLasers:
				currentScreenEffectPlaying = false;
				break;
			case SendStyleHelper.appleSendStyleScrnFireworks:
				currentScreenEffectPlaying = false;
				break;
			case SendStyleHelper.appleSendStyleScrnShootingStar:
				currentScreenEffectPlaying = false;
				break;
			case SendStyleHelper.appleSendStyleScrnCelebration:
				currentScreenEffectPlaying = false;
				break;
			default:
				currentScreenEffectPlaying = false;
				break;
		}
	}
	
	public static boolean isConversationInForeground(long conversationID) {
		return getForegroundConversations().contains(conversationID);
	}
	
	public static List<Long> getForegroundConversations() {
		//Creating the list
		List<Long> list = new ArrayList<>();
		
		//Iterating over the loaded conversations
		for(Iterator<WeakReference<Messaging>> iterator = foregroundConversations.iterator(); iterator.hasNext();) {
			//Getting the referenced activity
			Messaging activity = iterator.next().get();
			
			//Removing the reference if it is invalid
			if(activity == null) {
				iterator.remove();
				continue;
			}
			
			//Adding the entry to the list
			long conversationID = activity.getConversationID();
			if(conversationID != -1) list.add(conversationID);
		}
		
		//Returning the list
		return list;
	}
	
	public class MessageListRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the reference values
		private static final int poolSize = 12;
		private static final int itemTypeTopProgressBar = -1;
		private static final int itemTypeConversationActions = -2;
		
		//Creating the content values
		private final List<ConversationItem> conversationItems;
		private RecyclerView recyclerView;
		
		private AMConversationAction[] conversationActions = null; //Suggested actions at the bottom of the message list
		private boolean showTopProgressBar = false; //A loading spinner displayed at the top of the message list when loading previous message history
		
		//Creating the pools
		private final SparseArray<Pools.SimplePool<? extends VHMessageComponent>> componentPoolList = new SparseArray<>();
		private final Pools.SimplePool<VHMessagePreviewLink> previewPool = new Pools.SimplePool<>(poolSize);
		
		public MessageListRecyclerAdapter(List<ConversationItem> items) {
			conversationItems = items;
			
			//Enabling stable IDs
			setHasStableIds(true);
		}
		
		/**
		 * Updates the reply suggestions at the bottom of the list.
		 * Pass NULL to hide suggestions.
		 */
		public void setConversationActions(@Nullable AMConversationAction[] conversationActions) {
			boolean wasScrolledToBottom = isScrolledToBottom();
			
			boolean oldAvailable = this.conversationActions != null;
			boolean newAvailable = conversationActions != null;
			
			//Setting the suggestions
			this.conversationActions = conversationActions;
			
			//If conversation actions are being added
			if(!oldAvailable && newAvailable) {
				notifyItemInserted(getItemCount() - 1);
				
				if(wasScrolledToBottom) scrollToBottom();
			}
			//If conversation actions are being removed
			else if(oldAvailable && !newAvailable) {
				notifyItemRemoved(getItemCount());
			}
			//If conversation actions are being updated
			else if(oldAvailable && newAvailable) {
				notifyItemChanged(getItemCount() - 1);
			}
		}
		
		/**
		 * Updates the display of the progress bar at the top of the list
		 */
		public void setShowTopProgressBar(boolean show) {
			//Ignore if there is no change
			if(showTopProgressBar == show) return;
			
			boolean oldShow = showTopProgressBar;
			showTopProgressBar = show;
			
			//The loading spinner is added
			if(!oldShow && show) {
				notifyItemInserted(0);
			}
			//The loading spinner is removed
			else {
				notifyItemRemoved(0);
			}
		}
		
		/**
		 * Maps a source array index to an array adapter index (for use with adapter.notify, for example)
		 */
		public int mapRecyclerIndex(int index) {
			if(conversationActions != null) return index + 1;
			else return index;
		}
		
		/**
		 * Maps an array adapter index to a source array index (for retrieving source items based on their adapter position, for example)
		 */
		public int mapSourceIndex(int index) {
			if(showTopProgressBar) return index - 1;
			else return index;
		}
		
		/**
		 * Gets the matching conversation item from its adapter index
		 */
		private ConversationItem getItemAt(int index) {
			return conversationItems.get(mapSourceIndex(index));
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
				case ConversationItem.viewTypeMessage: {
					View view = getLayoutInflater().inflate(R.layout.listitem_message, parent, false);
					return new VHMessageStructure(view,
							view.findViewById(R.id.timedivider),
							view.findViewById(R.id.sender),
							view.findViewById(R.id.stub_profile),
							view.findViewById(R.id.messagepart_container),
							R.id.profile_default,
							R.id.profile_image,
							view.findViewById(R.id.activitystatus),
							view.findViewById(R.id.sendeffect_replay),
							view.findViewById(R.id.send_error)
					);
				}
				case ConversationItem.viewTypeAction: {
					View view = getLayoutInflater().inflate(R.layout.listitem_action, parent, false);
					return new VHMessageAction(view, view.findViewById(R.id.message));
				}
				case itemTypeTopProgressBar: {
					View view = getLayoutInflater().inflate(R.layout.listitem_loading, parent, false);
					CircularProgressIndicator progressIndicator = view.findViewById(R.id.progressbar);
					progressIndicator.setIndicatorColor(getUIColor());
					progressIndicator.setTrackColor(getUIColor());
					return new LoadingViewHolder(view);
				}
				case itemTypeConversationActions: {
					View view = getLayoutInflater().inflate(R.layout.listitem_replysuggestions, parent, false);
					return new VHConversationActions(view, (HorizontalScrollView) view, view.findViewById(R.id.container));
				}
				default:
					throw new IllegalArgumentException("Invalid view type received: " + viewType);
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
					((DisposableViewHolder) holder).getCompositeDisposable().dispose();
				}
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			//Returning if there are no items or the item is the loading spinner
			int itemType = getItemViewType(position);
			if(itemType == itemTypeTopProgressBar) return;
			
			//Checking if the item is the suggestions
			if(itemType == itemTypeConversationActions) {
				VHConversationActions viewHolder = (VHConversationActions) holder;
				viewHolder.setActions(Messaging.this, conversationActions, getUIColor(), messageText -> {
					MessageSendHelper.prepareSendMessages(Messaging.this, viewModel.conversationInfo, messageText, Collections.emptyList(), pluginCS.getConnectionManager()).subscribe();
					/* MessageSendHelper.prepareMessages(Messaging.this, viewModel.conversationInfo, messageText, Collections.emptyList())
							.flatMapCompletable(message -> MessageSendHelper.sendMessage(Messaging.this, viewModel.conversationInfo, message, pluginCS.getConnectionManager())).subscribe(); */
					handleMessageSent();
				});
				viewHolder.resetScroll();
			}
			//Otherwise checking if the item is a message
			else if(itemType == MessageViewType.message) {
				//Getting the item
				MessageInfo messageInfo = (MessageInfo) getItemAt(position);
				
				//Getting the adjacent messages
				Pair<MessageInfo, MessageInfo> adjacentMessages = getAdjacentMessages(mapSourceIndex(position));
				
				//Binding the message view
				bindMessage((VHMessageStructure) holder, viewModel.conversationInfo, messageInfo, adjacentMessages.first, adjacentMessages.second);
				
				//Playing the message's effect if it hasn't been viewed yet
				if(messageInfo.getSendStyle() != null && !messageInfo.isSendStyleViewed()) {
					messageInfo.setSendStyleViewed(true);
					playScreenEffect(messageInfo.getSendStyle(), holder.itemView);
					Completable.fromAction(() -> DatabaseManager.getInstance().markSendStyleViewed(messageInfo.getLocalID()))
							.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).subscribe();
				}
			}
			//Otherwise checking if the item is an action
			else if(itemType == MessageViewType.action) {
				//Getting the data
				VHMessageAction viewHolder = (VHMessageAction) holder;
				ConversationAction conversationAction = (ConversationAction) getItemAt(position);
				
				//Setting the immediate text
				viewHolder.label.setText(conversationAction.getMessageDirect(Messaging.this));
				
				//Building and applying the complete action
				if(conversationAction.supportsBuildMessageAsync()) {
					viewHolder.getCompositeDisposable().add(
							conversationAction.buildMessageAsync(Messaging.this)
									.subscribe((Consumer<String>) viewHolder.label::setText)
					);
				}
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
			if(payloads.isEmpty()) {
				onBindViewHolder(holder, position);
			} else {
				//Ignoring if the item isn't a message
				int itemType = getItemViewType(position);
				if(itemType == itemTypeTopProgressBar || itemType == itemTypeConversationActions) return;
				
				//Getting the conversation info
				ConversationItem conversationItem = getItemAt(position);
				
				for(Object objectPayload : payloads) {
					MessageListPayload payload = (MessageListPayload) objectPayload;
					switch(payload.getType()) {
						case MessageListPayloadType.state: {
							//Updating the message state
							bindMessageState((VHMessageStructure) holder, (MessageInfo) conversationItem, true);
							
							break;
						}
						case MessageListPayloadType.status: {
							//Animating the change
							//TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);
							
							//Updating the message status
							updateMessageActivityStatus((VHMessageStructure) holder, (MessageInfo) conversationItem);
							
							break;
						}
						case MessageListPayloadType.attachment: {
							//Rebuilding the attachment view
							int attachmentIndex = ((MessageListPayload.Attachment) payload).getAttachmentIndex();
							MessageInfo messageInfo = (MessageInfo) conversationItem;
							VHMessageStructure viewHolderStructure = (VHMessageStructure) holder;
							int componentIndex = attachmentIndex + (messageInfo.getMessageTextInfo() != null ? 1 : 0);
							if(componentIndex < viewHolderStructure.messageComponents.size()) {
								bindMessageComponent(viewHolderStructure, viewHolderStructure.messageComponents.get(componentIndex), viewModel.conversationInfo, messageInfo, messageInfo.getAttachments().get(attachmentIndex));
							}
							
							break;
						}
						case MessageListPayloadType.attachmentRebuild: {
							//Rebuilding the message's components
							bindMessageComponentList((VHMessageStructure) holder, viewModel.conversationInfo, (MessageInfo) conversationItem);
							updateMessageViewColoring(viewModel.conversationInfo, (VHMessageStructure) holder, (MessageInfo) conversationItem);
							
							Pair<MessageInfo, MessageInfo> adjacentMessages = getAdjacentMessages(mapSourceIndex(position));
							updateMessageViewEdges((VHMessageStructure) holder, viewModel.conversationInfo, (MessageInfo) conversationItem, adjacentMessages.first, adjacentMessages.second);
							
							break;
						}
						case MessageListPayloadType.flow: {
							Pair<MessageInfo, MessageInfo> adjacentMessages = getAdjacentMessages(mapSourceIndex(position));
							
							//Updating the view edges
							updateMessageViewEdges((VHMessageStructure) holder, viewModel.conversationInfo, (MessageInfo) conversationItem, adjacentMessages.first, adjacentMessages.second);
							
							break;
						}
						case MessageListPayloadType.color: {
							//Updating the message color
							updateMessageViewColoring(viewModel.conversationInfo, (VHMessageStructure) holder, (MessageInfo) conversationItem);
							
							break;
						}
						case MessageListPayloadType.tapback: {
							//Rebuilding the tapback view
							int componentIndex = ((MessageListPayload.Tapback) payload).getComponentIndex();
							MessageComponent messageComponent = ((MessageInfo) conversationItem).getComponents().get(componentIndex);
							VHMessageComponent componentViewHolder = ((VHMessageStructure) holder).messageComponents.get(componentIndex);
							
							VBMessageComponent.buildTapbackView(Messaging.this, messageComponent.getTapbacks(), componentViewHolder.tapbackContainer);
							
							break;
						}
						case MessageListPayloadType.sticker: {
							MessageListPayload.Sticker stickerPayload = (MessageListPayload.Sticker) payload;
							
							//Adding the sticker
							int componentIndex = stickerPayload.getComponentIndex();
							VHMessageComponent componentViewHolder = ((VHMessageStructure) holder).messageComponents.get(componentIndex);
							
							VBMessageComponent.addStickerView(Messaging.this, stickerPayload.getStickerInfo(), componentViewHolder.stickerContainer);
							
							break;
						}
					}
				}
			}
		}
		
		/**
		 * Gets the {@link MessageInfo} directly before and after a certain position. May be NULL if not present.
		 * @param position The source array index of the origin item
		 * @return A pair of the message before this one and the message after this one
		 */
		private Pair<MessageInfo, MessageInfo> getAdjacentMessages(int position) {
			MessageInfo messageBefore = null, messageAfter = null;
			
			if(position - 1 >= 0) {
				ConversationItem conversationItemBefore = conversationItems.get(position - 1);
				if(conversationItemBefore.getItemType() == ConversationItemType.message) messageBefore = (MessageInfo) conversationItemBefore;
			}
			if(position + 1 < conversationItems.size()) {
				ConversationItem conversationItemAfter = conversationItems.get(position + 1);
				if(conversationItemAfter.getItemType() == ConversationItemType.message) messageAfter = (MessageInfo) conversationItemAfter;
			}
			
			return new Pair<>(messageBefore, messageAfter);
		}
		
		private void bindMessage(VHMessageStructure viewHolder, ConversationInfo conversationInfo, MessageInfo messageInfo, @Nullable MessageInfo messageAbove, @Nullable MessageInfo messageBelow) {
			//Setting the alignment
			{
				ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) viewHolder.containerMessagePart.getLayoutParams();
				if(messageInfo.isOutgoing()) {
					params.startToStart = ConstraintLayout.LayoutParams.UNSET;
					params.endToEnd = R.id.barrier_alert;
				} else {
					params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
					params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
				}
			}
			
			//Binding the message components
			bindMessageComponentList(viewHolder, conversationInfo, messageInfo);
			
			//Checking if the message is outgoing
			if(messageInfo.isOutgoing()) {
				//Hiding the user info
				if(viewHolder.profileGroup != null) viewHolder.profileGroup.setVisibility(View.GONE);
				
				//Hiding the sender
				viewHolder.labelSender.setVisibility(View.GONE);
			} else {
				//Inflating the profile stub and getting the profile view
				viewHolder.inflateProfile();
				
				//Showing the profile view
				viewHolder.profileGroup.setVisibility(View.VISIBLE);
				
				//Clearing the profile image
				viewHolder.profileImage.setImageBitmap(null);
				viewHolder.profileDefault.setVisibility(View.VISIBLE);
				
				//Checking if the chat is a group chat
				if(conversationInfo.isGroupChat()) {
					//Setting the sender's name (temporarily)
					viewHolder.labelSender.setText(messageInfo.getSender());
					
					//Showing the sender
					viewHolder.labelSender.setVisibility(View.VISIBLE);
				} else {
					//Hiding the sender
					viewHolder.labelSender.setVisibility(View.GONE);
				}
				
				viewHolder.getCompositeDisposable().add(
						MainApplication.getInstance().getUserCacheHelper().getUserInfo(Messaging.this, messageInfo.getSender()).onErrorComplete().subscribe(userInfo -> {
							//Setting the sender's name
							viewHolder.labelSender.setText(userInfo.getContactName());
							
							//Loading the profile thumbnail
							viewHolder.inflateProfile();
							Glide.with(Messaging.this)
									.load(ContactHelper.getContactImageURI(userInfo.getContactID()))
									.listener(new RequestListener<Drawable>() {
										@Override
										public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
											return false;
										}
										
										@Override
										public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
											//Swapping to the profile view
											viewHolder.profileDefault.setVisibility(View.GONE);
											viewHolder.profileImage.setVisibility(View.VISIBLE);
											
											return false;
										}
									})
									.into(viewHolder.profileImage);
						})
				);
			}
			
			//Checking if the message has no send effect
			if(messageInfo.getSendStyle() == null || (!SendStyleHelper.validateAnimatedBubbleEffect(messageInfo.getSendStyle()) && !SendStyleHelper.validateScreenEffect(messageInfo.getSendStyle()))) {
				//Hiding the "replay" button
				viewHolder.buttonSendEffectReplay.setVisibility(View.GONE);
			} else {
				//Showing and configuring the "replay" button
				viewHolder.buttonSendEffectReplay.setVisibility(View.VISIBLE);
				viewHolder.buttonSendEffectReplay.setOnClickListener(clickedView -> playScreenEffect(messageInfo.getSendStyle(), viewHolder.itemView));
			}
			
			//Setting the text switcher's animations
			viewHolder.labelActivityStatus.setInAnimation(AnimationUtils.loadAnimation(Messaging.this, R.anim.fade_in_delayed));
			viewHolder.labelActivityStatus.setOutAnimation(AnimationUtils.loadAnimation(Messaging.this, R.anim.fade_out));
			
			//Updating the message state
			bindMessageState(viewHolder, messageInfo, false);
			
			//Updating the view edges
			updateMessageViewEdges(viewHolder, conversationInfo, messageInfo, messageAbove, messageBelow);
			
			//Updating the view color
			updateMessageViewColoring(conversationInfo, viewHolder, messageInfo);
			
			//Updating the view state display
			updateMessageActivityStatus(viewHolder, messageInfo);
			
			//Updating the time divider
			updateMessageTimeDivider(viewHolder, messageInfo, messageAbove);
		}
		
		/**
		 * Updates the ghost and error display state of a message view
		 * @param viewHolder The view holder of the message
		 * @param messageInfo The message to bind
		 * @param animate Whether to animate the view changes
		 */
		private void bindMessageState(VHMessageStructure viewHolder, MessageInfo messageInfo, boolean animate) {
			if(animate) {
				//Animating the message part container's alpha
				if(messageInfo.getMessageState() == MessageState.ghost) viewHolder.containerMessagePart.animate().alpha(0.5F);
				else viewHolder.containerMessagePart.animate().alpha(1);
			} else {
				//Setting the message part container's alpha
				if(messageInfo.getMessageState() == MessageState.ghost) viewHolder.containerMessagePart.setAlpha(0.5F);
				else viewHolder.containerMessagePart.setAlpha(1);
			}
			
			//Hiding the error and returning if there wasn't any problem
			if(!messageInfo.hasError()) {
				viewHolder.buttonSendError.setVisibility(View.GONE);
			} else {
				//Showing the error
				viewHolder.buttonSendError.setVisibility(View.VISIBLE);
				
				//Showing the dialog when the button is clicked
				viewHolder.buttonSendError.setOnClickListener(view -> {
					//Configuring the dialog
					MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(Messaging.this)
							.setTitle(R.string.message_messageerror_title)
							.setNeutralButton(R.string.action_deletemessage, (dialog, which) ->
									MessageActionTask.deleteMessages(Messaging.this, viewModel.conversationInfo, Collections.singletonList(messageInfo)).subscribe())
							.setNegativeButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss());
					
					//Getting the error display
					Pair<String, Boolean> errorDisplay = ErrorLanguageHelper.getErrorDisplay(Messaging.this, viewModel.conversationInfo, messageInfo.getErrorCode());
					
					//Setting the message
					dialogBuilder.setMessage(errorDisplay.first);
					
					//Showing the retry button (if requested)
					if(errorDisplay.second) {
						dialogBuilder.setPositiveButton(R.string.action_retry, (dialog, which) -> {
							//Re-sending the message
							MessageActionTask.updateMessageErrorCode(viewModel.conversationInfo, messageInfo, MessageSendErrorCode.none, null)
									.andThen(MessageSendHelper.sendMessage(Messaging.this, viewModel.conversationInfo, messageInfo, pluginCS.getConnectionManager()))
									.subscribe();
						});
					}
					
					//Showing the dialog
					dialogBuilder.create().show();
				});
				
				viewHolder.buttonSendError.setOnLongClickListener(view -> {
					if(messageInfo.isErrorDetailsAvailable()) {
						//Fetching the error details from the database
						pluginRXD.ui().add(Single.create((SingleEmitter<String> emitter) -> {
							String errorDetails = DatabaseManager.getInstance().getMessageErrorDetails(messageInfo.getLocalID());
							if(errorDetails != null) emitter.onSuccess(errorDetails);
							else emitter.onError(new Exception("No error details available"));
						}).subscribeOn(Schedulers.single())
								.observeOn(AndroidSchedulers.mainThread()).subscribe(
										details -> {
											//Showing the error details
											View dialogView = getLayoutInflater().inflate(R.layout.dialog_simplescroll, null);
											TextView textView = dialogView.findViewById(R.id.text);
											textView.setTypeface(Typeface.MONOSPACE);
											textView.setText(details);
											
											//Showing the dialog
											new MaterialAlertDialogBuilder(Messaging.this)
													.setTitle(R.string.message_messageerror_details_title)
													.setView(dialogView)
													.setNeutralButton(R.string.action_copy, (dialog, which) -> {
														ClipboardManager clipboard = (ClipboardManager) MainApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
														clipboard.setPrimaryClip(ClipData.newPlainText("Error details", details));
														Toast.makeText(MainApplication.getInstance(), R.string.message_textcopied, Toast.LENGTH_SHORT).show();
														dialog.dismiss();
													})
													.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
													.create().show();
										},
										error -> {
											//Notifying the user via a toast
											Toast.makeText(Messaging.this, R.string.message_messageerror_details_unavailable, Toast.LENGTH_SHORT).show();
										}
								));
					} else {
						//Notifying the user via a toast
						Toast.makeText(Messaging.this, R.string.message_messageerror_details_unavailable, Toast.LENGTH_SHORT).show();
					}
					
					return true;
				});
			}
		}
		
		/**
		 * Updates the activity status label of a message
		 * @param viewHolder The view holder of the message
		 * @param messageInfo The message
		 */
		private void updateMessageActivityStatus(VHMessageStructure viewHolder, MessageInfo messageInfo) {
			//Setting up the label
			if((viewModel.latestMessageDelivered != null && messageInfo.getLocalID() == viewModel.latestMessageDelivered.getLocalID()) ||
					(viewModel.latestMessageRead != null && messageInfo.getLocalID() == viewModel.latestMessageRead.getLocalID())) {
				viewHolder.labelActivityStatus.setVisibility(View.VISIBLE);
				
				String message;
				if(messageInfo.getMessageState() == MessageState.delivered) {
					message = getResources().getString(R.string.state_delivered);
				} else if(messageInfo.getMessageState() == MessageState.read) {
					message = getResources().getString(R.string.state_read) + LanguageHelper.bulletSeparator + LanguageHelper.getDeliveryStatusTime(Messaging.this, messageInfo.getDateRead());
				} else {
					message = getResources().getString(R.string.part_unknown);
				}
				viewHolder.labelActivityStatus.setCurrentText(message);
			} else {
				viewHolder.labelActivityStatus.setVisibility(View.GONE);
			}
		}
		
		/**
		 * Updates the time divider of a message
		 * @param viewHolder The view holder of the message
		 * @param messageInfo The message
		 * @param messageAbove The message directly above this message, or NULL if none is present
		 */
		private void updateMessageTimeDivider(VHMessageStructure viewHolder, MessageInfo messageInfo, @Nullable MessageInfo messageAbove) {
			//Checking if the time divider should be visible
			if(messageAbove == null || messageInfo.getDate() - messageAbove.getDate() >= TimingConstants.conversationSessionTimeMillis) {
				//Showing the time divider
				viewHolder.labelTimeDivider.setText(LanguageHelper.generateTimeDividerString(Messaging.this, messageInfo.getDate()));
				viewHolder.labelTimeDivider.setVisibility(View.VISIBLE);
			} else {
				//Hiding the time divider
				viewHolder.labelTimeDivider.setVisibility(View.GONE);
			}
		}
		
		private void updateMessageViewEdges(VHMessageStructure viewHolder, ConversationInfo conversationInfo, MessageInfo messageInfo, @Nullable MessageInfo messageAbove, @Nullable MessageInfo messageBelow) {
			boolean isLTR = getResources().getBoolean(R.bool.is_left_to_right);
			/*
			 * true + true = true
			 * true + false = false
			 * false + true = false
			 * false + false = true
		 	 */
			boolean alignToRight = messageInfo.isOutgoing() == isLTR;
			boolean isAnchoredTop = messageAbove != null && Objects.equals(messageInfo.getSender(), messageAbove.getSender()) && messageInfo.getDate() - messageAbove.getDate() < TimingConstants.conversationBurstTimeMillis;
			boolean isAnchoredBottom = messageBelow != null && Objects.equals(messageInfo.getSender(), messageBelow.getSender()) && messageBelow.getDate() - messageInfo.getDate() < TimingConstants.conversationBurstTimeMillis;
			
			//Getting the dimension values
			int pxPaddingAnchored = getResources().getDimensionPixelSize(R.dimen.messagebubble_padding_anchored);
			int pxPaddingUnanchored = getResources().getDimensionPixelSize(R.dimen.messagebubble_padding);
			
			//Updating the padding
			viewHolder.itemView.setPadding(viewHolder.itemView.getPaddingLeft(), isAnchoredTop ? pxPaddingAnchored : pxPaddingUnanchored, viewHolder.itemView.getPaddingRight(), isAnchoredBottom ? pxPaddingAnchored : pxPaddingUnanchored);
			
			//Checking if the message is incoming
			if(!messageInfo.isOutgoing()) {
				//Setting the user information
				boolean showUserInfo = !isAnchoredTop; //If the message isn't anchored to the top
				if(conversationInfo.isGroupChat()) viewHolder.labelSender.setVisibility(showUserInfo ? View.VISIBLE : View.GONE);
				if(viewHolder.profileGroup != null) viewHolder.profileGroup.setVisibility(showUserInfo ? View.VISIBLE : View.GONE);
			}
			
			//Updating the view edges between components
			for(ListIterator<VHMessageComponent> iterator = viewHolder.messageComponents.listIterator(); iterator.hasNext();) {
				int i = iterator.nextIndex();
				VHMessageComponent componentViewHolder = iterator.next();
				componentViewHolder.updateViewEdges(Messaging.this, i > 0 || isAnchoredTop, iterator.hasNext() || isAnchoredBottom, alignToRight);
				componentViewHolder.itemView.setPadding(0, i > 0 ? pxPaddingAnchored : 0, 0, 0);
			}
		}
		
		private void updateMessageViewColoring(ConversationInfo conversationInfo, VHMessageStructure viewHolder, MessageInfo messageInfo) {
			//Getting the colors
			int textColor;
			int textColorSecondary;
			int backgroundColor;
			
			MemberInfo memberInfo;
			
			if(messageInfo.isOutgoing()) {
				memberInfo = null;
				if(Preferences.getPreferenceAdvancedColor(Messaging.this)) {
					textColor = ResourceHelper.resolveColorAttr(Messaging.this, android.R.attr.textColorPrimary);
					textColorSecondary = ResourceHelper.resolveColorAttr(Messaging.this, android.R.attr.textColorSecondary);
					backgroundColor = getResources().getColor(R.color.colorMessageOutgoing, null);
				} else {
					textColor = ResourceHelper.resolveColorAttr(Messaging.this, R.attr.colorOnPrimary);
					textColorSecondary = ColorUtils.setAlphaComponent(textColor, ColorConstants.secondaryAlphaInt);
					backgroundColor = ColorHelper.getServiceColor(getResources(), conversationInfo.getServiceHandler(), conversationInfo.getServiceType());
				}
			} else {
				//Finding the member
				memberInfo = conversationInfo.getMembers().stream().filter(member -> member.getAddress().equals(messageInfo.getSender())).findAny().orElse(null);
				
				if(Preferences.getPreferenceAdvancedColor(Messaging.this)) {
					int targetColor = memberInfo == null ? ConversationColorHelper.backupUserColor : memberInfo.getColor();
					textColor = ColorMathHelper.multiplyColorLightness(targetColor, ThemeHelper.isNightMode(getResources()) ? 1.5F : 0.7F);
					textColorSecondary = ColorUtils.setAlphaComponent(textColor, 179);
					backgroundColor = ColorUtils.setAlphaComponent(targetColor, 50);
				} else {
					textColor = ResourceHelper.resolveColorAttr(Messaging.this, android.R.attr.textColorPrimary);
					textColorSecondary = ResourceHelper.resolveColorAttr(Messaging.this, android.R.attr.textColorSecondary);
					backgroundColor = getResources().getColor(R.color.colorMessageOutgoing, null);
				}
			}
			
			//Setting the user tint
			if(!messageInfo.isOutgoing() && viewHolder.profileGroup != null) {
				int memberColor = memberInfo == null ? ConversationColorHelper.backupUserColor : memberInfo.getColor();
				viewHolder.profileDefault.setColorFilter(memberColor, android.graphics.PorterDuff.Mode.MULTIPLY);
			}
			
			//Updating the components
			for(VHMessageComponent viewHolderComponent : viewHolder.messageComponents) {
				viewHolderComponent.updateViewColoring(Messaging.this, textColor, textColorSecondary, backgroundColor);
			}
		}
		
		private void bindMessageComponentList(@NonNull VHMessageStructure viewHolder, ConversationInfo conversationInfo, MessageInfo messageInfo) {
			/*
			 * This function behaves as the following:
			 * 1. Index the existing components from the view holder
			 * 2. Clear all components from the view holder
			 * 3. Build the component view for the new message first using our indexed components, and then pulling from the component pool
			 * 4. Release any excess indexed components back into the pool
			 */
			
			//Pulling views
			SparseArray<List<VHMessageComponent>> componentViewHolderList = new SparseArray<>();
			if(!viewHolder.messageComponents.isEmpty()) {
				//Sorting the components by type into the map
				for(VHMessageComponent componentViewHolder : viewHolder.messageComponents) {
					List<VHMessageComponent> list = componentViewHolderList.get(componentViewHolder.getComponentType());
					if(list == null) {
						list = new ArrayList<>();
						componentViewHolderList.put(componentViewHolder.getComponentType(), list);
					}
					
					list.add(componentViewHolder);
				}
				
				//Clearing and removing the components
				viewHolder.messageComponents.clear();
				viewHolder.containerMessagePart.removeAllViews();
			}
			
			//Building the component views
			List<MessageComponent> messageComponentList = new ArrayList<>();
			if(messageInfo.getMessageTextInfo() != null) messageComponentList.add(messageInfo.getMessageTextInfo());
			messageComponentList.addAll(messageInfo.getAttachments());
			
			for(MessageComponent component : messageComponentList) {
				int componentType = getMessageComponentType(component);
				
				List<VHMessageComponent> list = componentViewHolderList.get(componentType);
				VHMessageComponent componentViewHolder;
				if(list != null && list.isEmpty()) {
					//If we can re-use one of our existing view holders, do that
					componentViewHolder = list.get(0);
					list.remove(0);
				} else {
					//Otherwise get a view holder from the pool
					componentViewHolder = getPoolComponent(componentType, viewHolder.containerMessagePart);
				}
				
				//Adding the component view holder to the message view holder
				viewHolder.messageComponents.add(componentViewHolder);
				viewHolder.containerMessagePart.addView(componentViewHolder.itemView);
				
				//Binding the component view
				bindMessageComponent(viewHolder, componentViewHolder, conversationInfo, messageInfo, component);
			}
			
			//Sending any excess component views back to the pool
			for(int i = 0; i < componentViewHolderList.size(); i++) {
				int itemViewType = componentViewHolderList.keyAt(i);
				List<VHMessageComponent> list = componentViewHolderList.valueAt(i);
				for(VHMessageComponent componentViewHolder : list) {
					releasePoolComponent(itemViewType, componentViewHolder);
				}
			}
		}
		
		/**
		 * Binds a message component to its view holder
		 * @param viewHolderStructure The view holder of this message's structure
		 * @param viewHolder The view holder of this message component
		 * @param messageComponent The message component to bind
		 */
		private void bindMessageComponent(VHMessageStructure viewHolderStructure, VHMessageComponent viewHolder, ConversationInfo conversationInfo, MessageInfo messageInfo, MessageComponent messageComponent) {
			//if(viewHolder.getComponentType() != messageComponent.getComponentType()) throw new IllegalArgumentException("Trying to bind message component to view holder of different type: " + viewHolder.getComponentType() + " != " + messageComponent.getComponentType() + "!");
			
			//Binding the common data
			bindMessageComponentCommon(viewHolder, messageInfo, messageComponent);
			
			if(viewHolder.getComponentType() == MessageComponentType.text) {
				bindMessageComponentText(viewHolderStructure, (VHMessageComponentText) viewHolder, conversationInfo, messageInfo, (MessageComponentText) messageComponent);
			} else {
				bindMessageAttachmentCommon(viewHolderStructure, (VHMessageComponentAttachment) viewHolder, messageInfo, (AttachmentInfo) messageComponent);
			}
		}
		
		/**
		 * Binds the views common across all components
		 * @param viewHolder The view holder for the component
		 * @param messageInfo The message info of the component
		 * @param component The component to bind
		 */
		private void bindMessageComponentCommon(VHMessageComponent viewHolder, MessageInfo messageInfo, MessageComponent component) {
			//Setting the message alignment
			((LinearLayout.LayoutParams) viewHolder.itemView.getLayoutParams()).gravity = (messageInfo.isOutgoing() ? Gravity.END : Gravity.START);
			
			//Building the sticker and tapback views
			VBMessageComponent.buildStickerView(Messaging.this, component.getStickers(), viewHolder.stickerContainer);
			VBMessageComponent.buildTapbackView(Messaging.this, component.getTapbacks(), viewHolder.tapbackContainer);
			
			//Resetting the click listener (can be set later on by different content types)
			viewHolder.itemView.setOnClickListener(null);
			
			//Open the popup menu on long click
			viewHolder.itemView.setOnLongClickListener(view -> {
				openPopupMenu(viewHolder.itemView, messageInfo, component);
				return true;
			});
		}
		
		private void bindMessageComponentText(VHMessageStructure viewHolderStructure, VHMessageComponentText viewHolder, ConversationInfo conversationInfo, MessageInfo messageInfo, MessageComponentText component) {
			//Resetting the click listener
			viewHolder.labelBody.setOnLongClickListener(null);
			
			//Checking if there is body text
			if(component.getText() != null) {
				//Showing the body label
				viewHolder.labelBody.setVisibility(View.VISIBLE);
				
				//Checking if the string consists exclusively of emoji characters
				if(StringHelper.stringContainsOnlyEmoji(component.getText())) {
					//Increasing the text size
					viewHolder.labelBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
					
					//Setting the message text
					viewHolder.labelBody.setText(component.getText());
				} else {
					//Resetting the text size
					viewHolder.labelBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					
					//Setting the message text
					viewHolder.labelBody.setText(component.getText());
					viewHolderStructure.getCompositeDisposable().add(
							viewModel.taskLinkify(component.getLocalID(), Messaging.this, component.getText())
									.onErrorComplete().subscribe(spannable -> {
								viewHolder.labelBody.setText(spannable);
								viewHolder.labelBody.setTransformationMethod(new CustomTabsLinkTransformationMethod());
								viewHolder.labelBody.setMovementMethod(LinkMovementMethod.getInstance());
								viewHolder.labelBody.setOnLongClickListener(view -> {
									openPopupMenu(viewHolder.itemView, messageInfo, component);
									return true;
								});
							})
					);
				}
			} else {
				//Hiding the body label
				viewHolder.labelBody.setVisibility(View.GONE);
			}
			
			//Checking if there is subject text
			if(component.getSubject() != null) {
				//Showing the subject label
				viewHolder.labelSubject.setVisibility(View.VISIBLE);
				
				//Setting the subject text
				viewHolder.labelSubject.setText(component.getSubject());
			} else {
				//Hiding the subject label
				viewHolder.labelSubject.setVisibility(View.GONE);
			}
			
			//Removing the preview view
			if(viewHolder.messagePreviewViewHolder != null) {
				viewHolder.messagePreviewContainer.removeView(viewHolder.messagePreviewViewHolder.itemView);
				previewPool.release(viewHolder.messagePreviewViewHolder);
				viewHolder.messagePreviewViewHolder = null;
				viewHolder.messagePreviewContainer.setVisibility(View.GONE);
			}
			
			//Setting the invisible ink view touch listener
			viewHolder.inkView.setOnTouchListener((View view, MotionEvent event) -> {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					((InvisibleInkView) view).reveal();
				}
				
				return view.onTouchEvent(event);
			});
			
			//Setting the message alignment
			((LinearLayout.LayoutParams) viewHolder.itemView.getLayoutParams()).gravity = (messageInfo.isOutgoing() ? Gravity.END : Gravity.START);
			
			//Enforcing the maximum content width
			{
				int maxWidth = WindowHelper.getMaxMessageWidth(getResources());
				viewHolder.labelBody.setMaxWidth(maxWidth);
				viewHolder.labelSubject.setMaxWidth(maxWidth);
				viewHolder.messagePreviewContainer.getLayoutParams().width = maxWidth;
			}
			
			//Resetting the message text bubble's width to its default
			viewHolder.groupMessage.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
			
			//Checking if previews are enabled
			if(Preferences.getPreferenceMessagePreviews(Messaging.this)) {
				int messagePreviewState = component.getMessagePreviewState();
				
				//Checking if a message preview is available
				if(messagePreviewState == MessagePreviewState.available) {
					//Requesting the message preview information
					viewHolderStructure.getCompositeDisposable().add(
							viewModel.taskMessagePreview(component.getMessagePreviewID())
									.doOnError(error -> Log.w(TAG, "Failed to load message preview ID " + component.getMessagePreviewID() + " from disk", error))
									.onErrorComplete()
									.subscribe(preview -> bindMessagePreview(viewHolderStructure, conversationInfo, messageInfo, viewHolder, preview))
					);
				}
				//Checking if a message preview should be fetched
				else if(messagePreviewState == MessagePreviewState.notTried && component.getText() != null) {
					//Finding any URL spans
					Matcher matcher = Patterns.WEB_URL.matcher(component.getText());
					int matchOffset = 0;
					String targetURL = null;
					
					while(matcher.find(matchOffset)) {
						//Getting the URL
						String urlString = matcher.group();
						
						//Updating the offset
						matchOffset = matcher.end();
						
						//Ignoring email addresses
						int matchStart = matcher.start();
						if(matchStart > 0 && component.getText().charAt(matchStart - 1) == '@') continue;
						
						//Skipping the URL if it has a custom scheme (the WEB_URL matcher will not include the scheme in the URL if it is unknown)
						String schemeOutside = component.getText().substring(0, matcher.start());
						if(schemeOutside.matches("\\w(?:\\w|\\d|\\+|-|\\.)*://$")) continue; //https://regex101.com/r/hW5bOW/1
						
						if(urlString.contains("…")) continue; //Crashes okhttp for some reason
						
						if(!urlString.contains("://")) urlString = "https://" + urlString; //Adding the scheme if it doesn't have one
						else if(urlString.startsWith("http://")) urlString = urlString.replaceFirst("http://", "https://"); //Replacing HTTP schemes with HTTPS schemes
						else if(!urlString.startsWith("https://")) continue; //Ignoring URLs of other schemes
						
						//Setting the url
						targetURL = urlString;
						break;
					}
					
					//Checking if a URL was found
					if(targetURL != null) {
						//Fetching the data
						String finalTargetURL = targetURL;
						viewHolder.getCompositeDisposable().add(
								viewModel.taskLinkPreview(component.getLocalID(), targetURL)
										.observeOn(Schedulers.single())
										.map(metadata -> {
											//Downloading the preview image
											byte[] imageBytes = null;
											if(metadata.getImageURL() != null) {
												try(BufferedInputStream in = new BufferedInputStream(new URL(metadata.getImageURL()).openStream());
													ByteArrayOutputStream out = new ByteArrayOutputStream()) {
													DataStreamHelper.copyStream(in, out);
													imageBytes = out.toByteArray();
													if(imageBytes.length > previewImageMaxSize) imageBytes = DataCompressionHelper.compressBitmap(imageBytes, "image/webp", previewImageMaxSize);
												} catch(IOException exception) {
													exception.printStackTrace();
													//Not returning, as the preview should simply be displayed without an image if the image failed to download
												}
											}
											
											//Creating the message preview
											String caption;
											if(metadata.getSiteName() != null && !metadata.getSiteName().isEmpty()) caption = metadata.getSiteName();
											else {
												try {
													caption = LanguageHelper.getDomainName(finalTargetURL);
													if(caption == null) throw new IllegalStateException("Cannot find domain name of " + caption);
												} catch(URISyntaxException exception) {
													//Logging the error
													exception.printStackTrace();
													
													//Updating the message state
													DatabaseManager.getInstance().setMessagePreviewState(component.getLocalID(), MessagePreviewState.unavailable);
													
													//Returning
													throw exception;
												}
											}
											
											//Creating the message preview
											MessagePreviewInfo messagePreview = new MessagePreviewInfo(MessagePreviewType.link, component.getLocalID(), imageBytes, finalTargetURL, metadata.getTitle(), metadata.getDescription(), caption);
											
											//Writing the metadata to disk
											DatabaseManager.getInstance().setMessagePreviewData(component.getLocalID(), messagePreview);
											
											return messagePreview;
										})
										.observeOn(AndroidSchedulers.mainThread())
										//Updating the state
										.subscribe(preview -> {
											component.setMessagePreviewState(MessagePreviewState.available);
											component.setMessagePreviewID(preview.getLocalID());
											viewModel.taskManagerMessagePreview.add(preview.getLocalID(), preview);
											bindMessagePreview(viewHolderStructure, conversationInfo, messageInfo, viewHolder, preview);
										}, error -> {
											component.setMessagePreviewState(MessagePreviewState.unavailable);
										})
						);
					} else {
						//Updating the preview
						component.setMessagePreviewState(MessagePreviewState.unavailable);
						
						//Updating the state on disk
						Completable.create(emitter -> {
							DatabaseManager.getInstance().setMessagePreviewState(messageInfo.getLocalID(), MessagePreviewState.unavailable);
						}).subscribeOn(Schedulers.single()).subscribe();
					}
				}
			}
			
			//Setting up the message effects
			if(SendStyleHelper.appleSendStyleBubbleInvisibleInk.equals(messageInfo.getSendStyle())) {
				viewHolder.inkView.setVisibility(View.VISIBLE);
				viewHolder.inkView.setState(true);
			} else {
				viewHolder.inkView.setVisibility(View.GONE);
			}
		}
		
		/**
		 * Sets the visible view on an attachment component, and hides all other views
		 * @param viewHolder The attachment component's view holder
		 * @param shownView The view to show
		 */
		private void setAttachmentView(VHMessageComponentAttachment viewHolder, View shownView) {
			viewHolder.groupPrompt.setVisibility(viewHolder.groupPrompt == shownView ? View.VISIBLE : View.GONE);
			viewHolder.groupProgress.setVisibility(viewHolder.groupProgress == shownView ? View.VISIBLE : View.GONE);
			viewHolder.groupOpen.setVisibility(viewHolder.groupOpen == shownView ? View.VISIBLE : View.GONE);
			viewHolder.groupContentFrame.setVisibility(viewHolder.groupContentFrame == shownView ? View.VISIBLE : View.GONE);
		}
		
		private void bindMessageAttachmentCommon(VHMessageStructure viewHolderStructure, VHMessageComponentAttachment viewHolder, MessageInfo messageInfo, AttachmentInfo component) {
			//Checking if we already have content
			if(component.getFile() != null) {
				//Showing the open view
				setAttachmentView(viewHolder, viewHolder.groupOpen);
				viewHolder.labelOpen.setText(component.getFileName());
				viewHolder.itemView.setOnClickListener(view -> IntentHelper.openAttachmentFile(Messaging.this, component.getFile(), component.getContentType()));
				
				//Setting up the content view
				bindMessageComponentContent(viewHolderStructure, viewHolder, messageInfo, component);
			} else {
				//Getting the current download state
				BehaviorSubject<ReduxEventAttachmentDownload> downloadObservable = ConnectionTaskManager.getDownload(component.getLocalID());
				
				//Checking if there is a download in progress
				if(downloadObservable != null && !(downloadObservable.getValue() instanceof ReduxEventAttachmentDownload.Complete)) {
					//Showing the progress view
					setAttachmentView(viewHolder, viewHolder.groupProgress);
					viewHolder.progressProgress.setIndeterminate(true);
					
					//Subscribing to download updates
					attachmentSubscribeDownload(viewHolderStructure, viewHolder, messageInfo, component, downloadObservable);
				} else {
					//Showing the download prompt view
					setAttachmentView(viewHolder, viewHolder.groupPrompt);
					
					viewHolder.labelPromptType.setText(LanguageHelper.getHumanReadableContentType(getResources(), component.getContentType()));
					if(component.getFileSize() == -1) viewHolder.labelPromptSize.setVisibility(View.GONE);
					else {
						viewHolder.labelPromptSize.setVisibility(View.VISIBLE);
						viewHolder.labelPromptSize.setText(Formatter.formatShortFileSize(Messaging.this, component.getFileSize()));
					}
					
					//Setting the download click listener
					viewHolder.itemView.setOnClickListener(view -> downloadAttachmentContent(viewHolderStructure, viewHolder, messageInfo, component));
				}
			}
		}
		
		/**
		 * Initiates a download request for an attachment's content and updates the attachment's view to reflect the new state
		 * @param viewHolderStructure The view holder structure
		 * @param viewHolder The view holder of the component
		 * @param messageInfo The message of the attachment
		 * @param component The attachment to download
		 */
		private void downloadAttachmentContent(VHMessageStructure viewHolderStructure, VHMessageComponentAttachment viewHolder, MessageInfo messageInfo, AttachmentInfo component) {
			if(component.getGUID() != null) {
				if(pluginCS.isServiceBound()) {
					//Switching to the download view
					setAttachmentView(viewHolder, viewHolder.groupProgress);
					viewHolder.progressProgress.setIndeterminate(true);
					
					//Starting the download
					attachmentSubscribeDownload(viewHolderStructure, viewHolder, messageInfo, component, ConnectionTaskManager.downloadAttachment(pluginCS.getConnectionManager(), messageInfo.getLocalID(), component.getLocalID(), component.getGUID(), component.getFileName()));
				} else {
					Toast.makeText(Messaging.this, R.string.message_connectionerror, Toast.LENGTH_SHORT).show();
				}
			}
		}
		
		/**
		 * Subscribes a message component to its download progress
		 * @param viewHolderStructure The view holder structure
		 * @param viewHolder The view holder of the component
		 * @param attachmentInfo The attachment to subscribe
		 * @param observable The download task observable
		 */
		private void attachmentSubscribeDownload(VHMessageStructure viewHolderStructure, VHMessageComponentAttachment viewHolder, MessageInfo messageInfo, AttachmentInfo attachmentInfo, Observable<ReduxEventAttachmentDownload> observable) {
			viewHolderStructure.getCompositeDisposable().add(
					observable.onErrorComplete().subscribe(event -> {
						viewHolder.progressProgress.setIndeterminate(false);
						if(event instanceof ReduxEventAttachmentDownload.Progress) {
							ReduxEventAttachmentDownload.Progress progressEvent = (ReduxEventAttachmentDownload.Progress) event;
							
							//Updating the progress bar
							viewHolder.progressProgress.setProgress((int) ((float) progressEvent.getBytesProgress() / progressEvent.getBytesTotal() * viewHolder.progressProgress.getMax()));
						}
					})
			);
		}
		
		private void bindMessageComponentContent(VHMessageStructure viewHolderStructure, VHMessageComponentAttachment viewHolder, MessageInfo messageInfo, AttachmentInfo component) {
			if(FileHelper.compareMimeTypes(component.getContentType(), MIMEConstants.mimeTypeImage) || FileHelper.compareMimeTypes(component.getContentType(), MIMEConstants.mimeTypeVideo)) {
				bindMessageComponentVisual((VHMessageComponentVisual) viewHolder, messageInfo, component);
			} else if(FileHelper.compareMimeTypes(component.getContentType(), MIMEConstants.mimeTypeAudio)) {
				bindMessageComponentAudio(viewModel.audioPlaybackManager, viewHolderStructure, (VHMessageComponentAudio) viewHolder, component);
			} else if(FileHelper.compareMimeTypes(component.getContentType(), MIMEConstants.mimeTypeVCard)) {
				bindMessageComponentContact(viewHolderStructure, (VHMessageComponentContact) viewHolder, component);
			} else if(FileHelper.compareMimeTypes(component.getContentType(), MIMEConstants.mimeTypeVLocation)) {
				bindMessageComponentLocation(viewHolderStructure, (VHMessageComponentLocation) viewHolder, component);
			}
		}
		
		private void bindMessageComponentVisual(VHMessageComponentVisual viewHolder, MessageInfo messageInfo, AttachmentInfo component) {
			//Resetting the image view
			viewHolder.imageView.layout(0, 0, 0, 0);
			
			//Loading the image
			RequestBuilder<Drawable> requestBuilder = Glide.with(Messaging.this)
					.load(component.getFile())
					.signature(new ObjectKey(component.getGUID() != null ? component.getGUID() : component.getLocalID()))
					.transition(DrawableTransitionOptions.withCrossFade());
			if(SendStyleHelper.appleSendStyleBubbleInvisibleInk.equals(messageInfo.getSendStyle())) requestBuilder.apply(RequestOptions.bitmapTransform(new BlurTransformation(SendStyleHelper.invisibleInkBlurRadius, SendStyleHelper.invisibleInkBlurSampling)));
			requestBuilder
					.override(getResources().getDimensionPixelSize(R.dimen.image_width_preferred))
					.listener(new RequestListener<Drawable>() {
						@Override
						public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
							return false;
						}
						
						@Override
						public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
							//Switching to the content view
							setAttachmentView(viewHolder, viewHolder.groupContentFrame);
							
							//Setting the click listener
							viewHolder.itemView.setOnClickListener(view -> openAttachmentFileMediaViewer(component, viewHolder.imageView, new float[]{0, 0, 0, 0, 0, 0, 0, 0}));
							
							//Updating the image view layout
							viewHolder.imageView.requestLayout();
							//viewHolder.imageView.post(viewHolder.imageView::requestLayout);
							
							if(FileHelper.compareMimeTypes(component.getContentType(), MIMEConstants.mimeTypeVideo)) {
								//Showing the play indicator
								viewHolder.playIndicator.setVisibility(View.VISIBLE);
								
								//Computing the image brightness
								Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
								boolean isLight = ColorMathHelper.calculateBrightness(bitmap, bitmap.getWidth() / 16) > 200;
								
								//Updating the play icon color
								viewHolder.playIndicator.setImageTintList(isLight ? ColorStateList.valueOf(0xFF212121) : ColorStateList.valueOf(0xFFFFFFFF));
							} else {
								//Hiding the play indicator
								viewHolder.playIndicator.setVisibility(View.GONE);
							}
							
							//Updating the ink view
							if(SendStyleHelper.appleSendStyleBubbleInvisibleInk.equals(messageInfo.getSendStyle())) {
								viewHolder.inkView.setVisibility(View.VISIBLE);
								viewHolder.inkView.setState(true);
							} else {
								viewHolder.inkView.setVisibility(View.GONE);
							}
							
							return false;
						}
					})
					.into(viewHolder.imageView);
		}
		
		private void bindMessageComponentAudio(AudioPlaybackManager playbackManager, VHMessageStructure viewHolderStructure, VHMessageComponentAudio viewHolder, AttachmentInfo component) {
			//Loading the metadata
			viewHolderStructure.getCompositeDisposable().add(
					component.<FileDisplayMetadata.Media>getDisplayMetadata(() -> Single.create(emitter -> emitter.onSuccess(new FileDisplayMetadata.Media(Messaging.this, Union.ofA(component.getFile())))))
					.subscribe(metadata -> {
						//Switching to the content view
						setAttachmentView(viewHolder, viewHolder.groupContentFrame);
						
						//Checking if we are currently playing this message
						if(playbackManager.compareRequest(component)) {
							//Subscribing to playback updates
							attachAudioPlayback(playbackManager.emitter(), viewHolderStructure, viewHolder, metadata.getMediaDuration());
						} else {
							//Showing the idle state
							viewHolder.setPlaybackIdle(metadata.getMediaDuration());
						}
						
						//Setting the click listener
						viewHolder.itemView.setOnClickListener(view -> {
							if(playbackManager.compareRequest(component)) {
								//Toggling playback if we're already playing this audio file
								playbackManager.togglePlaying();
							} else {
								//Otherwise, starting a new playback session
								Observable<AudioPlaybackManager.Progress> playbackObservable = playbackManager.play(Messaging.this, component, component.getFile());
								attachAudioPlayback(playbackObservable, viewHolderStructure, viewHolder, metadata.getMediaDuration());
							}
						});
					})
			);
		}
		
		/**
		 * Subscribes an audio component view holder to the current playback of an audio playback manager
		 * @param playbackObservable The audio playback observable to subscribe to
		 * @param viewHolderStructure The view holder of this message's structure
		 * @param viewHolder The view holder of the audio component
		 * @param audioDuration The total length of the audio file
		 */
		private void attachAudioPlayback(Observable<AudioPlaybackManager.Progress> playbackObservable, VHMessageStructure viewHolderStructure, VHMessageComponentAudio viewHolder, long audioDuration) {
			viewHolderStructure.getCompositeDisposable().add(playbackObservable.subscribe(update -> {
				viewHolder.setPlaybackProgress(update.getPlaybackProgress(), audioDuration, update.isPlaying());
			}, error -> {}, () -> {
				//Returning to idle when playback is finished
				viewHolder.setPlaybackIdle(audioDuration);
			}));
		}
		
		private void bindMessageComponentContact(VHMessageStructure viewHolderStructure, VHMessageComponentContact viewHolder, AttachmentInfo component) {
			//Loading the metadata
			viewHolderStructure.getCompositeDisposable().add(
					component.<FileDisplayMetadata.Contact>getDisplayMetadata(() -> Single.create(emitter -> emitter.onSuccess(new FileDisplayMetadata.Contact(Messaging.this, Union.ofA(component.getFile())))))
					.subscribe(metadata -> {
						//Switching to the content view
						setAttachmentView(viewHolder, viewHolder.groupContentFrame);
						
						//Setting the contact name label
						if(metadata.getContactName() == null) viewHolder.labelName.setText(R.string.part_content_contact);
						else viewHolder.labelName.setText(metadata.getContactName());
						
						//Setting the contact's picture
						if(metadata.getContactIcon() == null) {
							viewHolder.iconPlaceholder.setVisibility(View.VISIBLE);
							viewHolder.iconProfile.setVisibility(View.GONE);
						} else {
							viewHolder.iconPlaceholder.setVisibility(View.GONE);
							viewHolder.iconProfile.setVisibility(View.VISIBLE);
							viewHolder.iconProfile.setImageBitmap(metadata.getContactIcon());
						}
						
						//Setting the click listener
						viewHolder.itemView.setOnClickListener(view -> IntentHelper.openAttachmentFile(Messaging.this, component.getFile(), component.getContentType()));
					})
			);
		}
		
		private void bindMessageComponentLocation(VHMessageStructure viewHolderStructure, VHMessageComponentLocation viewHolder, AttachmentInfo component) {
			//Setting the width
			viewHolder.groupContent.getLayoutParams().width = WindowHelper.getMaxMessageWidth(getResources());
			
			//Loading the metadata and waiting for the map view to be initialized
			viewHolderStructure.getCompositeDisposable().add(
					Single.zip(
							component.<FileDisplayMetadata.LocationDetailed>getDisplayMetadata(() -> Single.fromCallable(() -> new FileDisplayMetadata.LocationDetailed(Messaging.this, Union.ofA(component.getFile())))),
							viewHolder.getGoogleMap(),
							Pair::new
					).subscribe(results -> {
						//Getting the results
						FileDisplayMetadata.LocationDetailed metadata = results.first;
						GoogleMap googleMap = results.second;
						
						//Switching to the content view
						setAttachmentView(viewHolder, viewHolder.groupContentFrame);
						
						//Setting the location title
						if(metadata.getLocationName() != null) viewHolder.labelTitle.setText(metadata.getLocationName());
						else viewHolder.labelTitle.setText(R.string.message_locationtitle_unknown);
						
						//Setting the address
						if(metadata.getLocationAddress() != null) viewHolder.labelAddress.setText(metadata.getLocationAddress());
						else {
							if(metadata.getLocationCoords() != null) viewHolder.labelAddress.setText(LanguageHelper.coordinatesToString(metadata.getLocationCoords()));
							else viewHolder.labelAddress.setText(R.string.message_locationaddress_unknown);
						}
						
						//Setting the map preview
						if(metadata.getLocationCoords() == null || !Preferences.getPreferenceMessagePreviews(viewHolder.itemView.getContext())) {
							viewHolder.mapContainer.setVisibility(View.GONE);
						} else {
							//Showing the map view and setting it as non-clickable (so that the card view parent will handle clicks instead)
							viewHolder.mapContainer.setVisibility(View.VISIBLE);
							viewHolder.mapView.setClickable(false);
							
							//Setting the map location
							LatLng targetLocation = metadata.getLocationCoords();
							googleMap.clear();
							googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 15));
							MarkerOptions markerOptions = new MarkerOptions().position(targetLocation);
							googleMap.addMarker(markerOptions);
							
							//Setting the map theme
							googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(Messaging.this, ThemeHelper.isNightMode(getResources()) ? R.raw.map_dark : R.raw.map_light));
							googleMap.getUiSettings().setMapToolbarEnabled(false);
						}
						
						//Setting the click listener
						viewHolder.itemView.setOnClickListener(view -> IntentHelper.launchUri(viewHolder.groupContent.getContext(), metadata.getMapLink()));
					})
			);
		}
		
		private void bindMessagePreview(VHMessageStructure structureViewHolder, ConversationInfo conversationInfo, MessageInfo messageInfo, VHMessageComponentText componentViewHolder, MessagePreviewInfo preview) {
			//Expanding the message text bubble to match the view
			componentViewHolder.groupMessage.getLayoutParams().width = 0;
			
			//Showing the preview container
			componentViewHolder.messagePreviewContainer.setVisibility(View.VISIBLE);
			
			//There will be a switch statement or something here if we add more preview types
			bindMessagePreviewLink(componentViewHolder, preview);
			
			//Updating the view edges
			Pair<MessageInfo, MessageInfo> adjacentMessages = getAdjacentMessages(conversationItems.indexOf(messageInfo));
			updateMessageViewEdges(structureViewHolder, conversationInfo, messageInfo, adjacentMessages.first, adjacentMessages.second);
		}
		
		private void bindMessagePreviewLink(VHMessageComponentText componentViewHolder, MessagePreviewInfo preview) {
			//Getting a preview view holder from the pool
			VHMessagePreviewLink viewHolder = previewPool.acquire();
			
			//If we don't have a view holder, make a new one
			if(viewHolder == null) {
				View view = getLayoutInflater().inflate(R.layout.layout_messagepreview_linklarge, componentViewHolder.messagePreviewContainer, false);
				viewHolder = new VHMessagePreviewLink(view,
						view.findViewById(R.id.view_border),
						view.findViewById(R.id.image_header),
						view.findViewById(R.id.label_title),
						view.findViewById(R.id.label_description),
						view.findViewById(R.id.label_address)
				);
			}
			
			//Applying the view holder
			componentViewHolder.messagePreviewContainer.addView(viewHolder.itemView);
			componentViewHolder.messagePreviewViewHolder = viewHolder;
			
			//Loading the image (or disabling it if there is none)
			byte[] data = preview.getData();
			if(data == null) {
				viewHolder.imageHeader.setVisibility(View.GONE);
			} else {
				viewHolder.imageHeader.setVisibility(View.VISIBLE);
				Glide.with(Messaging.this).load(data).into(viewHolder.imageHeader);
			}
			
			//Setting the title
			viewHolder.labelTitle.setText(preview.getTitle());
			
			//Setting the description (or disabling it if there is none)
			String subtitle = preview.getSubtitle();
			if(subtitle == null || subtitle.isEmpty()) {
				viewHolder.labelDescription.setVisibility(View.GONE);
			} else {
				viewHolder.labelDescription.setVisibility(View.VISIBLE);
				viewHolder.labelDescription.setText(subtitle);
			}
			
			//Setting the address (as the site name, or otherwise the host)
			viewHolder.labelAddress.setText(preview.getCaption());
			
			//Setting the click listener
			viewHolder.itemView.setOnClickListener(view -> {
				Uri targetUri = Uri.parse(preview.getTarget());
				//To keep consistent with standard Linkify
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) IntentHelper.launchUri(Messaging.this, targetUri);
				else IntentHelper.launchCustomTabs(Messaging.this, targetUri);
			});
		}
		
		private void openAttachmentFileMediaViewer(AttachmentInfo attachmentInfo, View transitionView, float[] radiiRaw) {
			//Assembling a list of all attachment files
			ArrayList<AttachmentInfo> attachments = viewModel.conversationItemList.stream()
					.filter(item -> item.getItemType() == ConversationItemType.message)
					.flatMap(item -> ((MessageInfo) item).getAttachments().stream())
					.filter(attachment ->
							attachment.getFile() != null &&
									(FileHelper.compareMimeTypes(attachment.getContentType(), MIMEConstants.mimeTypeImage) || FileHelper.compareMimeTypes(attachment.getContentType(), MIMEConstants.mimeTypeVideo))
					).collect(Collectors.toCollection(ArrayList::new));
			int itemIndex = attachments.indexOf(attachmentInfo);
			
			//Launching the media viewer
			if(itemIndex == -1) return;
			startActivity(new Intent(Messaging.this, MediaViewer.class)
					.putParcelableArrayListExtra(MediaViewer.intentParamDataList, attachments)
					.putExtra(MediaViewer.intentParamIndex, itemIndex)
			);
		}
		
		/**
		 * Displays a popup menu for a message component
		 * @param targetView The view to display the popup menu on
		 * @param messageInfo The message component's message info
		 * @param messageComponent The message component to display the popup menu for
		 */
		private void openPopupMenu(View targetView, MessageInfo messageInfo, MessageComponent messageComponent) {
			//Creating a new popup menu
			PopupMenu popupMenu = new PopupMenu(Messaging.this, targetView);
			
			//Inflating the menu
			popupMenu.inflate(R.menu.menu_conversationitem_contextual);
			
			//Getting the component type
			int componentType = getMessageComponentType(messageComponent);
			
			Menu menu = popupMenu.getMenu();
			if(componentType == MessageComponentType.text) {
				//Removing attachment-specific options
				menu.removeItem(R.id.action_save);
				menu.removeItem(R.id.action_deletedata);
			} else {
				AttachmentInfo attachmentInfo = (AttachmentInfo) messageComponent;
				
				//Removing text-specific options
				menu.removeItem(R.id.action_copytext);
				
				//Disabling the share, save, and delete option if there is no data
				if(attachmentInfo.getFile() == null) {
					menu.findItem(R.id.action_share).setEnabled(false);
					menu.findItem(R.id.action_save).setEnabled(false);
					menu.findItem(R.id.action_deletedata).setEnabled(false);
				}
				//Remove the delete option if the file was not sent over AirMessage Bridge
				if(viewModel.conversationInfo.getServiceHandler() != ServiceHandler.appleBridge) {
					menu.removeItem(R.id.action_deletedata);
				}
			}
			
			//Removing the tapback info option if there are no tapbacks
			if(messageComponent.getTapbacks().isEmpty()) menu.removeItem(R.id.action_tapbackdetails);
			
			//Setting the click listener
			popupMenu.setOnMenuItemClickListener(menuItem -> {
				int itemId = menuItem.getItemId();
				if(itemId == R.id.action_tapbackdetails) {
					//Displaying the tapback list
					openTapbackDialog(viewModel.conversationInfo, messageComponent.getTapbacks());
					
					return true;
				} else if(itemId == R.id.action_details) {
					Date sentDate = new Date(messageInfo.getDate());
					
					//Building the message
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(getResources().getString(R.string.message_messagedetails_sender, messageInfo.getSender() != null ? messageInfo.getSender() : getResources().getString(R.string.part_you))).append('\n'); //Sender
					stringBuilder.append(getResources().getString(R.string.message_messagedetails_datesent, DateFormat.getTimeFormat(Messaging.this).format(sentDate) + LanguageHelper.bulletSeparator + DateFormat.getLongDateFormat(Messaging.this).format(sentDate))).append('\n'); //Time sent
					if(messageComponent instanceof AttachmentInfo) {
						long fileSize = ((AttachmentInfo) messageComponent).getFileSize();
						stringBuilder.append(getResources().getString(R.string.message_messagedetails_size, fileSize != -1 ? Formatter.formatFileSize(Messaging.this, fileSize) : getResources().getString(R.string.part_nodata))).append('\n'); //Attachment file size
					}
					stringBuilder.append(getResources().getString(R.string.message_messagedetails_sendeffect, messageInfo.getSendStyle() == null ? getResources().getString(R.string.part_none) :messageInfo.getSendStyle())); //Send effect
					
					//Showing a dialog
					new MaterialAlertDialogBuilder(Messaging.this)
							.setTitle(R.string.message_messagedetails_title)
							.setMessage(stringBuilder.toString())
							.create()
							.show();
					
					return true;
				} else if(itemId == R.id.action_copytext) {
					//Copying the message text to the clipboard
					getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, LanguageHelper.textComponentToString(getResources(), (MessageComponentText) messageComponent)));
					
					//Showing a confirmation toast
					Toast.makeText(Messaging.this, R.string.message_textcopied, Toast.LENGTH_SHORT).show();
					
					//Returning true
					return true;
				} else if(itemId == R.id.action_share) {
					//Checking if this is a text component
					if(componentType == MessageComponentType.text) {
						String shareText = LanguageHelper.textComponentToString(getResources(), (MessageComponentText) messageComponent);
						
						//Starting the intent immediately if the user is "you"
						if(messageInfo.getSender() == null) {
							shareMessageText(null, shareText, messageInfo.getDate());
						} else {
							//Requesting the user info
							pluginRXD.activity().add(
									MainApplication.getInstance().getUserCacheHelper().getUserInfo(Messaging.this, messageInfo.getSender()).subscribe(userInfo -> {
										shareMessageText(userInfo.getContactName(), shareText, messageInfo.getDate());
									}, error -> {
										//Defaulting to the user's address
										shareMessageText(messageInfo.getSender(), shareText, messageInfo.getDate());
									})
							);
						}
					} else {
						AttachmentInfo attachmentInfo = (AttachmentInfo) messageComponent;
						
						//Sharing the attachment file
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_SEND);
						intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(Messaging.this, AttachmentStorageHelper.getFileAuthority(Messaging.this), attachmentInfo.getFile()));
						intent.setType(attachmentInfo.getContentType());
						startActivity(Intent.createChooser(intent, getResources().getText(R.string.action_sharemessage)));
					}
					
					return true;
				} else if(itemId == R.id.action_save) {
					AttachmentInfo attachmentInfo = (AttachmentInfo) messageComponent;
					
					//Opening the file picker to save the file
					currentTargetSAFFile = attachmentInfo.getFile();
					ExternalStorageHelper.createFileSAF(Messaging.this, intentSaveFileSAF, attachmentInfo.getContentType(), attachmentInfo.getFileName());
					
					return true;
				} else if(itemId == R.id.action_deletedata) {
					AttachmentInfo attachmentInfo = (AttachmentInfo) messageComponent;
					
					//Deleting the attachment file
					MessageActionTask.deleteAttachmentFile(messageInfo.getLocalID(), attachmentInfo).subscribe();
					
					//Removing the download from the cache
					ConnectionTaskManager.removeDownload(attachmentInfo.getLocalID());
					
					return true;
				}
				
				//Returning false
				return false;
			});
			
			//Setting the context menu as closed when the menu closes
			/* popupMenu.setOnDismissListener(closedMenu -> {
				contextMenuOpen = false;
				updateStickerVisibility();
				composite
			}); */
			
			//Showing the menu
			popupMenu.show();
		}
		
		/**
		 * Shares the contents of a text message
		 * @param sender The name of the user who sent this message, or NULL if the local user sent the message
		 * @param message The message body
		 * @param date The date the message was sent
		 */
		private void shareMessageText(@Nullable String sender, @NonNull String message, long date) {
			//Creating the intent
			Intent intent = new Intent();
			
			//Setting the action
			intent.setAction(Intent.ACTION_SEND);
			
			//Getting the text
			String text = sender == null ?
					getResources().getString(R.string.message_shareable_text_you, DateFormat.getLongDateFormat(Messaging.this).format(date), DateFormat.getTimeFormat(Messaging.this).format(date), message) :
					getResources().getString(R.string.message_shareable_text, DateFormat.getLongDateFormat(Messaging.this).format(date), DateFormat.getTimeFormat(Messaging.this).format(date), sender, message);
			
			//Setting the text
			intent.putExtra(Intent.EXTRA_TEXT, text);
			
			//Setting the intent type
			intent.setType("text/plain");
			
			//Starting the intent
			startActivity(Intent.createChooser(intent, getResources().getString(R.string.action_sharemessage)));
		}
		
		/**
		 * Creates and displays a dialog that shows information for a list of tapbacks
		 */
		private void openTapbackDialog(ConversationInfo conversationInfo, List<TapbackInfo> tapbacks) {
			//Creating the composite disposable for the dialog lifecycle
			CompositeDisposable compositeDisposable = new CompositeDisposable();
			
			//Creating the view
			View dialogView = getLayoutInflater().inflate(R.layout.dialog_simplelist, null);
			LinearLayout viewGroup = dialogView.findViewById(R.id.list);
			
			//Sorting the tapback list by kind
			Map<Integer, Integer> tapbackCounts = new HashMap<>();
			for(TapbackInfo tapback : tapbacks) {
				if(tapbackCounts.containsKey(tapback.getCode())) tapbackCounts.put(tapback.getCode(), tapbackCounts.get(tapback.getCode()) + 1);
				else tapbackCounts.put(tapback.getCode(), 1);
			}
			
			//Grouping matching tapback types
			Map<Integer, List<TapbackInfo>> tapbackResponses = new HashMap<>();
			for(TapbackInfo tapback : tapbacks) {
				if(tapbackResponses.containsKey(tapback.getCode())) tapbackResponses.get(tapback.getCode()).add(tapback);
				else tapbackResponses.put(tapback.getCode(), new ArrayList<>(Collections.singletonList(tapback)));
			}
			
			//Sorting the tapback counts by value (descending)
			tapbackCounts = CollectionHelper.sortMapByValueDesc(tapbackCounts);
			
			//Iterating over the tapback groups
			for(Map.Entry<Integer, Integer> entry : tapbackCounts.entrySet()) {
				//Getting the display info
				TapbackDisplayData displayInfo = LanguageHelper.getTapbackDisplay(entry.getKey());
				
				//Adding the header
				TextView headerLabel = (TextView) getLayoutInflater().inflate(R.layout.listitem_tapbackinfo_header, viewGroup, false);
				headerLabel.setText(getResources().getString(displayInfo.getLabel()));
				headerLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(displayInfo.getIconResource(), 0, 0, 0);
				headerLabel.setCompoundDrawableTintList(ColorStateList.valueOf(getColor(displayInfo.getColor())));
				viewGroup.addView(headerLabel);
				
				//Getting all tapbacks of this type
				for(TapbackInfo tapback : tapbackResponses.get(entry.getKey())) {
					//Getting the tapback information
					String sender = tapback.getSender();
					
					//Creating the user view
					ViewGroup userView = (ViewGroup) getLayoutInflater().inflate(R.layout.listitem_tapbackinfo_user, viewGroup, false);
					viewGroup.addView(userView);
					TextView nameLabel = userView.findViewById(R.id.label_name);
					
					//Checking if the sender is the user
					if(sender == null) {
						//Setting the name to "you"
						nameLabel.setText(R.string.part_you);
						
						//Setting the default user tint
						((ImageView) userView.findViewById(R.id.profile_default)).setColorFilter(getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
					} else {
						//Setting the sender's name (temporarily)
						nameLabel.setText(sender);
						
						//Setting the default user tint
						MemberInfo memberInfo = conversationInfo.getMembers().stream().filter(member -> member.getAddress().equals(sender)).findAny().orElse(null);
						int userTint = memberInfo == null ? ConversationColorHelper.backupUserColor : memberInfo.getColor();
						((ImageView) userView.findViewById(R.id.profile_default)).setColorFilter(userTint, android.graphics.PorterDuff.Mode.MULTIPLY);
						
						//Setting the sender's name
						compositeDisposable.add(MainApplication.getInstance().getUserCacheHelper().getUserInfo(Messaging.this, sender).onErrorComplete().subscribe(userInfo -> {
							//Setting the contact name
							nameLabel.setText(userInfo.getContactName());
							
							//Setting the user's profile image
							ImageView profileDefault = userView.findViewById(R.id.profile_default);
							ImageView profileImage = userView.findViewById(R.id.profile_image);
							Glide.with(Messaging.this)
									.load(ContactHelper.getContactImageURI(userInfo.getContactID()))
									.listener(new RequestListener<Drawable>() {
										@Override
										public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
											return false;
										}
										
										@Override
										public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
											//Swapping to the profile view
											profileDefault.setVisibility(View.GONE);
											profileImage.setVisibility(View.VISIBLE);
											
											return false;
										}
									})
									.into(profileImage);
						}));
					}
				}
			}
			
			//Showing the dialog
			new MaterialAlertDialogBuilder(Messaging.this)
					.setView(dialogView)
					.setOnDismissListener(dialog -> compositeDisposable.clear())
					.create().show();
		}
		
		@Override
		public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
			//Clearing the view's animation
			holder.itemView.clearAnimation();
		}
		
		@Override
		public int getItemCount() {
			int size = conversationItems.size();
			if(conversationActions != null) size += 1;
			if(showTopProgressBar) size += 1;
			return size;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(isViewTopProgressBar(position)) return itemTypeTopProgressBar;
			if(isViewConversationActions(position)) return itemTypeConversationActions;
			return getItemAt(position).getItemViewType();
		}
		
		@Override
		public long getItemId(int position) {
			if(isViewTopProgressBar(position)) return itemTypeTopProgressBar;
			if(isViewConversationActions(position)) return itemTypeConversationActions;
			return getItemAt(position).getLocalID();
		}
		
		/**
		 * Gets if the view at the specified position is the top progress bar
		 */
		private boolean isViewTopProgressBar(int position) {
			return showTopProgressBar && position == 0;
		}
		
		/**
		 * Gets if the view at the specified position is the conversation action selector
		 */
		private boolean isViewConversationActions(int position) {
			return conversationActions != null && position + 1 == getItemCount();
		}
		
		/**
		 * Gets a component from the view pool
		 * @param componentType The type of this component
		 * @param parent The parent view to inflate the component under
		 * @param <T> The view holder for this component
		 * @return The view holder for this component
		 */
		public <T extends VHMessageComponent> T getPoolComponent(@MessageComponentType int componentType, ViewGroup parent) {
			Pools.SimplePool<T> pool = (Pools.SimplePool<T>) componentPoolList.get(componentType);
			if(pool == null) return createComponentViewHolder(componentType, parent);
			else {
				T viewHolder = pool.acquire();
				return viewHolder == null ? createComponentViewHolder(componentType, parent) : viewHolder;
			}
		}
		
		/**
		 * Releases a component back into the pool
		 * @param componentType The type of this component
		 * @param viewHolder The view holder to release back into the pool
		 * @param <T> The view holder for this component
		 */
		public <T extends VHMessageComponent> void releasePoolComponent(@MessageComponentType int componentType, T viewHolder) {
			Pools.SimplePool<T> pool = (Pools.SimplePool<T>) componentPoolList.get(componentType);
			if(pool == null) {
				pool = new Pools.SimplePool<>(poolSize);
				componentPoolList.put(componentType, pool);
			}
			pool.release(viewHolder);
		}
		
		/**
		 * Inflates a view and creates a new view holder for the given component type
		 * @param componentType The component type to inflate the view for
		 * @param parent The parent view to inflate the component under
		 * @param <T> The view holder for this component
		 * @return A newly created view holder of this component type
		 */
		@SuppressWarnings("unchecked")
		private <T extends VHMessageComponent> T createComponentViewHolder(@MessageComponentType int componentType, ViewGroup parent) {
			switch(componentType) {
				case MessageComponentType.text: {
					View view = getLayoutInflater().inflate(R.layout.listitem_contenttext, parent, false);
					return (T) new VHMessageComponentText(view,
							view.findViewById(R.id.container),
							view.findViewById(R.id.sticker_container),
							view.findViewById(R.id.tapback_container),
							view.findViewById(R.id.content),
							view.findViewById(R.id.group_message),
							view.findViewById(R.id.label_body),
							view.findViewById(R.id.label_subject),
							view.findViewById(R.id.content_ink),
							view.findViewById(R.id.group_messagepreview)
					);
				}
				case MessageComponentType.attachmentDocument: {
					View view = getEmptyAttachmentView(getLayoutInflater(), parent);
					return (T) new VHMessageComponentDocument(view,
							view.findViewById(R.id.container),
							view.findViewById(R.id.sticker_container),
							view.findViewById(R.id.tapback_container),
							view.findViewById(R.id.downloadprompt),
							view.findViewById(R.id.label_size),
							view.findViewById(R.id.label_type),
							view.findViewById(R.id.prompt_icon),
							view.findViewById(R.id.downloadprogress),
							view.findViewById(R.id.download_progress),
							view.findViewById(R.id.progress_icon),
							view.findViewById(R.id.opencontent),
							view.findViewById(R.id.open_label),
							view.findViewById(R.id.frame_content)
					);
				}
				case MessageComponentType.attachmentVisual: {
					View view = buildAttachmentView(getLayoutInflater(), parent, R.layout.listitem_contentvisual);
					return (T) new VHMessageComponentVisual(view,
							view.findViewById(R.id.container),
							view.findViewById(R.id.sticker_container),
							view.findViewById(R.id.tapback_container),
							view.findViewById(R.id.downloadprompt),
							view.findViewById(R.id.label_size),
							view.findViewById(R.id.label_type),
							view.findViewById(R.id.prompt_icon),
							view.findViewById(R.id.downloadprogress),
							view.findViewById(R.id.download_progress),
							view.findViewById(R.id.progress_icon),
							view.findViewById(R.id.opencontent),
							view.findViewById(R.id.open_label),
							view.findViewById(R.id.frame_content),
							view.findViewById(R.id.content),
							view.findViewById(R.id.content_view),
							view.findViewById(R.id.content_ink),
							view.findViewById(R.id.icon_play)
					);
				}
				case MessageComponentType.attachmentAudio: {
					View view = buildAttachmentView(getLayoutInflater(), parent, R.layout.listitem_contentaudio);
					return (T) new VHMessageComponentAudio(view,
							view.findViewById(R.id.container),
							view.findViewById(R.id.sticker_container),
							view.findViewById(R.id.tapback_container),
							view.findViewById(R.id.downloadprompt),
							view.findViewById(R.id.label_size),
							view.findViewById(R.id.label_type),
							view.findViewById(R.id.prompt_icon),
							view.findViewById(R.id.downloadprogress),
							view.findViewById(R.id.download_progress),
							view.findViewById(R.id.progress_icon),
							view.findViewById(R.id.opencontent),
							view.findViewById(R.id.open_label),
							view.findViewById(R.id.frame_content),
							view.findViewById(R.id.content),
							view.findViewById(R.id.content_icon),
							view.findViewById(R.id.content_duration),
							view.findViewById(R.id.content_progress)
					);
				}
				case MessageComponentType.attachmentContact: {
					View view = buildAttachmentView(getLayoutInflater(), parent, R.layout.listitem_contentcontact);
					return (T) new VHMessageComponentContact(view,
							view.findViewById(R.id.container),
							view.findViewById(R.id.sticker_container),
							view.findViewById(R.id.tapback_container),
							view.findViewById(R.id.downloadprompt),
							view.findViewById(R.id.label_size),
							view.findViewById(R.id.label_type),
							view.findViewById(R.id.prompt_icon),
							view.findViewById(R.id.downloadprogress),
							view.findViewById(R.id.download_progress),
							view.findViewById(R.id.progress_icon),
							view.findViewById(R.id.opencontent),
							view.findViewById(R.id.open_label),
							view.findViewById(R.id.frame_content),
							view.findViewById(R.id.content),
							view.findViewById(R.id.image_profile),
							view.findViewById(R.id.icon_placeholder),
							view.findViewById(R.id.label_name)
					);
				}
				case MessageComponentType.attachmentLocation: {
					View view = buildAttachmentView(getLayoutInflater(), parent, R.layout.listitem_contentlocation);
					return (T) new VHMessageComponentLocation(view,
							view.findViewById(R.id.container),
							view.findViewById(R.id.sticker_container),
							view.findViewById(R.id.tapback_container),
							view.findViewById(R.id.downloadprompt),
							view.findViewById(R.id.label_size),
							view.findViewById(R.id.label_type),
							view.findViewById(R.id.prompt_icon),
							view.findViewById(R.id.downloadprogress),
							view.findViewById(R.id.download_progress),
							view.findViewById(R.id.progress_icon),
							view.findViewById(R.id.opencontent),
							view.findViewById(R.id.open_label),
							view.findViewById(R.id.frame_content),
							view.findViewById(R.id.content),
							view.findViewById(R.id.view_border),
							view.findViewById(R.id.map_container),
							view.findViewById(R.id.map_header),
							view.findViewById(R.id.label_title),
							view.findViewById(R.id.label_address)
					);
				}
				default:
					throw new IllegalArgumentException("Illegal component type " + componentType);
			}
		}
		
		private View getEmptyAttachmentView(LayoutInflater layoutInflater, ViewGroup parent) {
			return layoutInflater.inflate(R.layout.listitem_contentstructure, parent, false);
		}
		
		private View buildAttachmentView(LayoutInflater layoutInflater, ViewGroup parent, @LayoutRes int contentLayout) {
			//Inflating the structure view
			View structureView = getEmptyAttachmentView(layoutInflater, parent);
			
			//Adding the content layout
			layoutInflater.inflate(contentLayout, structureView.findViewById(R.id.frame_content), true);
			
			//Returning the view
			return structureView;
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
		
		@MessageComponentType
		private int getMessageComponentType(MessageComponent component) {
			if(component instanceof MessageComponentText) {
				return MessageComponentType.text;
			} else if(component instanceof AttachmentInfo) {
				AttachmentInfo attachmentInfo = (AttachmentInfo) component;
				if(FileHelper.compareMimeTypes(attachmentInfo.getContentType(), MIMEConstants.mimeTypeImage) || FileHelper.compareMimeTypes(attachmentInfo.getContentType(), MIMEConstants.mimeTypeVideo)) return MessageComponentType.attachmentVisual;
				else if(FileHelper.compareMimeTypes(attachmentInfo.getContentType(), MIMEConstants.mimeTypeAudio)) return MessageComponentType.attachmentAudio;
				else if(FileHelper.compareMimeTypes(attachmentInfo.getContentType(), MIMEConstants.mimeTypeVCard)) return MessageComponentType.attachmentContact;
				else if(FileHelper.compareMimeTypes(attachmentInfo.getContentType(), MIMEConstants.mimeTypeVLocation)) return MessageComponentType.attachmentLocation;
				else return MessageComponentType.attachmentDocument;
			} else {
				throw new IllegalArgumentException("Unknown component type");
			}
		}
	}
	
	private static class MessageTargetUpdate {
		private final MessageInfo newMessage;
		private final List<MessageInfo> oldMessages;
		
		/**
		 * Creates a new container class for message target updates
		 * @param newMessage The new message that is the receiver of the target update
		 * @param oldMessages Any old messages that have lost their status as receiver of the target update
		 */
		public MessageTargetUpdate(MessageInfo newMessage, List<MessageInfo> oldMessages) {
			this.newMessage = newMessage;
			this.oldMessages = oldMessages;
		}
		
		public MessageInfo getNewMessage() {
			return newMessage;
		}
		
		public List<MessageInfo> getOldMessages() {
			return oldMessages;
		}
	}
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({MessageListPayloadType.state, MessageListPayloadType.status, MessageListPayloadType.attachment,MessageListPayloadType.attachmentRebuild, MessageListPayloadType.flow, MessageListPayloadType.color, MessageListPayloadType.tapback, MessageListPayloadType.sticker})
	private @interface MessageListPayloadType {
		int state = 0; //Change in a message's send or error state
		int status = 1; //Change in a message's activity status
		int attachment = 2; //When an attachment's file is updated
		int attachmentRebuild = 3; //When a message's attachments must be rebuilt
		int flow = 4; //Change in shape (when the messages adjacent to this message are changed)
		int color = 5; //Change in color
		int tapback = 6; //Update of a tapback
		int sticker = 7; //Addition of a sticker
	}
	
	private static abstract class MessageListPayload {
		@MessageListPayloadType
		abstract int getType();
		
		private static class BasicImpl extends MessageListPayload {
			@MessageListPayloadType
			private final int type;
			
			public BasicImpl(int type) {
				this.type = type;
			}
			
			@Override
			int getType() {
				return type;
			}
		}
		
		private static final MessageListPayload state = new BasicImpl(MessageListPayloadType.state);
		private static final MessageListPayload status = new BasicImpl(MessageListPayloadType.status);
		private static final MessageListPayload attachmentRebuild = new BasicImpl(MessageListPayloadType.attachmentRebuild);
		private static final MessageListPayload flow = new BasicImpl(MessageListPayloadType.flow);
		private static final MessageListPayload color = new BasicImpl(MessageListPayloadType.color);
		
		private static class Attachment extends MessageListPayload {
			private final int attachmentIndex;
			
			public Attachment(int attachmentIndex) {
				this.attachmentIndex = attachmentIndex;
			}
			
			@Override
			int getType() {
				return MessageListPayloadType.attachment;
			}
			
			public int getAttachmentIndex() {
				return attachmentIndex;
			}
		}
		
		private static class Tapback extends MessageListPayload {
			private final int componentIndex;
			
			public Tapback(int componentIndex) {
				this.componentIndex = componentIndex;
			}
			
			public int getComponentIndex() {
				return componentIndex;
			}
			
			@Override
			int getType() {
				return MessageListPayloadType.tapback;
			}
		}
		
		private static class Sticker extends MessageListPayload {
			private final int componentIndex;
			private final StickerInfo stickerInfo;
			
			public Sticker(int componentIndex, StickerInfo stickerInfo) {
				this.componentIndex = componentIndex;
				this.stickerInfo = stickerInfo;
			}
			
			public int getComponentIndex() {
				return componentIndex;
			}
			
			public StickerInfo getStickerInfo() {
				return stickerInfo;
			}
			
			@Override
			int getType() {
				return MessageListPayloadType.sticker;
			}
		}
	}
	
	private class AttachmentsQueueRecyclerAdapter extends RecyclerView.Adapter<VHAttachmentQueued> {
		//Creating the reference values
		static final int payloadUpdateState = 1;
		
		//Creating the list value
		private final List<FileQueued> itemList;
		
		AttachmentsQueueRecyclerAdapter(List<FileQueued> list) {
			itemList = list;
		}
		
		@NonNull
		@Override
		public VHAttachmentQueued onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			//Inflating the layout
			View layout = getLayoutInflater().inflate(R.layout.listitem_attachment_queuetile, parent, false);
			ViewGroup container = layout.findViewById(R.id.container);
			
			//Creating the tile view
			View contentView;
			VHAttachmentTileContent content;
			switch(viewType) {
				case AttachmentType.document: {
					contentView = getLayoutInflater().inflate(R.layout.listitem_attachment_documenttile, container, false);
					content = new VHAttachmentTileContentDocument(contentView, contentView.findViewById(R.id.label_name), contentView.findViewById(R.id.icon), contentView.findViewById(R.id.label_size));
					break;
				}
				case AttachmentType.media: {
					contentView = getLayoutInflater().inflate(R.layout.listitem_attachment_mediatile, container, false);
					content = new VHAttachmentTileContentMedia(contentView.findViewById(R.id.image), contentView.findViewById(R.id.image_flag_gif), contentView.findViewById(R.id.group_flag_video), contentView.findViewById(R.id.label_flag_video));
					break;
				}
				case AttachmentType.audio: {
					contentView = getLayoutInflater().inflate(R.layout.listitem_attachment_audiotile, container, false);
					content = new VHAttachmentTileContentAudio(contentView, contentView.findViewById(R.id.progress), contentView.findViewById(R.id.image_play));
					break;
				}
				case AttachmentType.contact: {
					contentView = getLayoutInflater().inflate(R.layout.listitem_attachment_contacttile, container, false);
					content = new VHAttachmentTileContentContact(contentView.findViewById(R.id.icon_placeholder), contentView.findViewById(R.id.image_profile), contentView.findViewById(R.id.label_name));
					break;
				}
				case AttachmentType.location: {
					contentView = getLayoutInflater().inflate(R.layout.listitem_attachment_locationtile, container, false);
					content = new VHAttachmentTileContentLocation(contentView.findViewById(R.id.label_name));
					break;
				}
				default:
					throw new IllegalArgumentException("Invalid view type received: " + viewType);
			}
			
			container.addView(contentView);
			return new VHAttachmentQueued(layout, layout.findViewById(R.id.button_remove), container, content);
		}
		
		@Override
		public void onBindViewHolder(@NonNull VHAttachmentQueued holder, int position) {
			//Getting the item
			FileQueued fileInfo = itemList.get(position);
			Union<File, Uri> fileSource = fileInfo.getFile().map(FileLinked::getFile, file -> Union.ofA(file.getFile()));
			
			//Binding the content view
			holder.content.bind(Messaging.this,
					holder.getCompositeDisposable(),
					fileSource,
					fileInfo.getFile().map(FileLinked::getFileName, FileDraft::getFileName),
					fileInfo.getFile().map(FileLinked::getFileType, FileDraft::getFileType),
					fileInfo.getFile().map(FileLinked::getFileSize, FileDraft::getFileSize),
					fileInfo.getFile().map(file -> -1L, FileDraft::getLocalID),
					fileInfo.getMediaStoreID()
			);
			
			//Loading the metadata
			int itemViewType = getItemViewType(position);
			if(itemViewType == AttachmentType.media) {
				if(FileHelper.compareMimeTypes(fileInfo.getFile().map(FileLinked::getFileType, FileDraft::getFileType), MIMEConstants.mimeTypeVideo)) {
					holder.getCompositeDisposable().add(
							viewModel.taskManagerMetadata.run(fileInfo.getReferenceID(), () ->
									Single.create((SingleEmitter<FileDisplayMetadata> emitter) -> emitter.onSuccess(new FileDisplayMetadata.Media(Messaging.this, fileSource)))
											.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()))
									.subscribe(metadata -> ((VHAttachmentTileContentMedia) holder.content).applyMetadata((FileDisplayMetadata.Media) metadata))
					);
				}
			} else if(itemViewType == AttachmentType.audio) {
				holder.getCompositeDisposable().add(
						viewModel.taskManagerMetadata.run(fileInfo.getReferenceID(), () ->
								Single.create((SingleEmitter<FileDisplayMetadata> emitter) -> emitter.onSuccess(new FileDisplayMetadata.Media(Messaging.this, fileSource)))
										.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()))
								.subscribe(metadata ->
										((VHAttachmentTileContentAudio) holder.content).onLoaded(Messaging.this, holder.getCompositeDisposable(), viewModel.audioPlaybackManager, fileSource, (FileDisplayMetadata.Media) metadata, fileInfo.getReferenceID()))
				);
			} else if(itemViewType == AttachmentType.contact) {
				holder.getCompositeDisposable().add(
						viewModel.taskManagerMetadata.run(fileInfo.getReferenceID(), () ->
								Single.create((SingleEmitter<FileDisplayMetadata> emitter) -> emitter.onSuccess(new FileDisplayMetadata.Contact(Messaging.this, fileSource)))
										.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()))
								.subscribe(metadata -> ((VHAttachmentTileContentContact) holder.content).applyMetadata((FileDisplayMetadata.Contact) metadata))
				);
			} else if(itemViewType == AttachmentType.location) {
				holder.getCompositeDisposable().add(
						viewModel.taskManagerMetadata.run(fileInfo.getReferenceID(), () ->
								Single.create((SingleEmitter<FileDisplayMetadata> emitter) -> emitter.onSuccess(new FileDisplayMetadata.LocationSimple(Messaging.this, fileSource)))
										.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()))
								.subscribe(metadata -> ((VHAttachmentTileContentLocation) holder.content).applyMetadata((FileDisplayMetadata.LocationSimple) metadata))
				);
			}
			
			//Hooking up the remove button
			holder.buttonRemove.setOnClickListener(view -> viewModel.dequeueFile(fileInfo));
			
			//Setting the view state
			holder.setAppearanceState(!fileInfo.getFile().isA(), false);
		}
		
		@Override
		public void onBindViewHolder(@NonNull VHAttachmentQueued holder, int position, @NonNull List<Object> payloads) {
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
						FileQueued fileInfo = itemList.get(position);
						
						//Setting the view state
						holder.setAppearanceState(!fileInfo.getFile().isA(), false);
						
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
			return itemList.get(position).getAttachmentType();
		}
	}
	
	private static class LoadingViewHolder extends RecyclerView.ViewHolder {
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
		static final int stateLoadingConversation = 1;
		static final int stateLoadingMessages = 2;
		static final int stateFailedMatching = 3; //When given the recipients of the conversation to find the conversation ID
		static final int stateFailedConversation = 4;
		static final int stateFailedMessages = 5;
		static final int stateReady = 6;
		
		//Creating the state values
		private final MutableLiveData<String> titleLD = new MutableLiveData<>();
		private final MutableLiveData<Integer> stateLD = new MutableLiveData<>();
		
		final List<FileQueued> queueList = new ArrayList<>(3);
		final PublishSubject<Pair<Integer, FileQueued>> subjectQueueListAdd = PublishSubject.create(); //When a queued file is added
		final PublishSubject<Pair<Integer, FileQueued>> subjectQueueListRemove = PublishSubject.create(); //When a queued file is removed
		final PublishSubject<Pair<Integer, FileQueued>> subjectQueueListUpdate = PublishSubject.create(); //When a queued file is prepared
		
		boolean isAttachmentsPanelOpen = false;
		
		final PublishSubject<Integer> subjectProgressiveLoadUpdate = PublishSubject.create(); //When a progressive load is completed
		private final MutableLiveData<Boolean> progressiveLoadInProgress = new MutableLiveData<>();
		private boolean progressiveLoadReachedLimit = false;
		
		private final MutableLiveData<AMConversationAction[]> conversationActionsLD = new MutableLiveData<>();
		
		private int lastUnreadCount = 0;
		
		private int nextDraftFileReferenceID = 0;
		
		//Creating the conversation values
		private String[] conversationParticipantsTarget; //Used for fetching conversation
		private final long conversationIDTarget; //Used for fetching conversation
		ConversationInfo conversationInfo; //The actual loaded conversation
		List<ConversationItem> conversationItemList; //The conversation's messages
		List<MessageInfo> conversationGhostList; //The conversation's ghost messages
		DatabaseManager.ConversationLazyLoader conversationLazyLoader; //The lazy loader utility for conversation items
		//The latest read message and latest delivered message
		MessageInfo latestMessageRead, latestMessageDelivered;
		private long lastConversationActionTarget = -1;
		
		//Creating the sound values
		private final SoundPool soundPool = SoundHelper.getSoundPool();
		private final int soundIDMessageIncoming = soundPool.load(getApplication(), R.raw.message_in, 1);
		private final int soundIDMessageOutgoing = soundPool.load(getApplication(), R.raw.message_out, 1);
		//private final int soundIDMessageError = soundPool.load(getApplication(), R.raw.message_error, 1);
		
		final AudioPlaybackManager audioPlaybackManager = new AudioPlaybackManager(getApplication());
		
		//Creating the task values
		private final TaskManagerLong<Spannable> taskManagerLinkify = new TaskManagerLong<>();
		Single<Spannable> taskLinkify(long id, Context context, String text) {
			return taskManagerLinkify.run(id, () -> {
				//Smart Linkify is preferred on Android 9+
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					return Single.fromCallable(() -> {
						Spannable spannable = new SpannableString(text);
						
						TextClassifier textClassifier = context.getSystemService(TextClassificationManager.class).getTextClassifier();
						TextLinks textLinks = textClassifier.generateLinks(new TextLinks.Request.Builder(text).build());
						textLinks.apply(spannable, TextLinks.APPLY_STRATEGY_REPLACE, null);
						
						return spannable;
					}).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
				} else {
					//Use regular ol' Linkify
					return Single.fromCallable(() -> {
						Spannable spannable = new SpannableString(text);
						Linkify.addLinks(spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
						return spannable;
					});
				}
			});
		}
		final TaskManagerLong<RichPreviewTask.Metadata> taskManagerLinkPreview = new TaskManagerLong<>();
		Single<RichPreviewTask.Metadata> taskLinkPreview(long id, String url) {
			return taskManagerLinkPreview.run(id, () -> RichPreviewTask.fetchMetadata(url));
		}
		final TaskManagerLong<MessagePreviewInfo> taskManagerMessagePreview = new TaskManagerLong<>();
		Single<MessagePreviewInfo> taskMessagePreview(long id) {
			return taskManagerMessagePreview.run(id, () -> Single.fromCallable(() -> {
				MessagePreviewInfo messagePreview = DatabaseManager.getInstance().loadMessagePreview(id);
				if(messagePreview != null) return messagePreview;
				else throw new RuntimeException("No preview with ID " + id + " found");
			}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()));
		}
		final TaskManager<FileDisplayMetadata> taskManagerMetadata = new TaskManager<>();
		
		//Creating the task subscription values
		private final CompositeDisposable compositeDisposable = new CompositeDisposable();
		private Disposable conversationTitleDisposable;
		private Disposable conversationActionsDisposable;
		
		ActivityViewModel(Application application, long conversationIDTarget) {
			super(application);
			
			//Setting the values
			this.conversationIDTarget = conversationIDTarget;
			
			//Loading the data
			loadConversation();
		}
		
		ActivityViewModel(@NonNull Application application, String[] conversationParticipantsTarget) {
			super(application);
			
			//Setting the values
			this.conversationIDTarget = -1;
			this.conversationParticipantsTarget = conversationParticipantsTarget;
			
			//Loading the conversation
			findCreateConversationMMSSMS();
		}
		
		@Override
		protected void onCleared() {
			//Releasing the sounds
			soundPool.release();
			
			//Releasing the audio player
			audioPlaybackManager.release();
			
			//Updating the conversation's unread message count
			if(conversationInfo != null) {
				ConversationActionTask.unreadConversations(Collections.singleton(conversationInfo), conversationInfo.getUnreadMessageCount()).subscribe();
			}
			
			//Clearing task subscriptions
			compositeDisposable.clear();
		}
		
		/**
		 * Loads the conversation from its ID
		 */
		void loadConversation() {
			//Updating the state
			stateLD.setValue(stateLoadingConversation);
			
			//Finalize the conversation ID for reliable access
			final long conversationID = ActivityViewModel.this.conversationIDTarget;
			compositeDisposable.add(Single.create((SingleEmitter<Pair<ConversationInfo, DatabaseManager.ConversationLazyLoader>> emitter) -> {
				//Getting the conversation
				ConversationInfo conversationInfo = DatabaseManager.getInstance().fetchConversationInfo(getApplication(), conversationID);
				if(conversationInfo == null) {
					emitter.onError(new Throwable("Conversation not found"));
					return;
				}
				
				//Creating the lazy loader
				DatabaseManager.ConversationLazyLoader lazyLoader = new DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), conversationInfo);
				
				emitter.onSuccess(new Pair<>(conversationInfo, lazyLoader));
			}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).subscribe(result -> applyConversation(result.first, result.second), (error) -> {
				//Setting the state to failed if the conversation info couldn't be fetched
				stateLD.setValue(stateFailedConversation);
			}));
		}
		
		/**
		 * Find or create a conversation, based on the data provided
		 */
		void findCreateConversationMMSSMS() {
			//Updating the state
			stateLD.setValue(stateLoadingConversation);
			
			compositeDisposable.add(Single.create((SingleEmitter<Long> emitter) -> {
				//Finding or creating a matching conversation in Android's SMS / MMS database
				emitter.onSuccess(Telephony.Threads.getOrCreateThreadId(getApplication(), new HashSet<>(Arrays.asList(conversationParticipantsTarget))));
			}).subscribeOn(Schedulers.io())
					.observeOn(Schedulers.single()).map((threadID) -> {
						//Finding the conversation in the database
						ConversationInfo conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(getApplication(), threadID, ServiceHandler.systemMessaging, ServiceType.systemSMS);
						
						//Checking if a conversation wasn't found in the database
						if(conversationInfo == null) {
							//Creating a new conversation
							int conversationColor = ConversationColorHelper.getDefaultConversationColor(threadID);
							List<MemberInfo> coloredMembers = ConversationColorHelper.getColoredMembers(conversationParticipantsTarget, conversationColor, threadID);
							conversationInfo = new ConversationInfo(-1, null, threadID, ConversationState.ready, ServiceHandler.systemMessaging, ServiceType.systemSMS, conversationColor, coloredMembers, null);
							
							//Writing the conversation to disk
							boolean result = DatabaseManager.getInstance().addConversationInfo(conversationInfo);
							if(!result) return null;
							
							//Adding the conversation created message
							DatabaseManager.getInstance().addConversationCreatedMessage(conversationInfo.getLocalID());
						}
						
						//Creating the lazy loader
						DatabaseManager.ConversationLazyLoader lazyLoader = new DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), conversationInfo);
						
						//Returning the data
						return new Pair<>(conversationInfo, lazyLoader);
					}).observeOn(AndroidSchedulers.mainThread()).subscribe(result -> applyConversation(result.first, result.second)));
		}
		
		/**
		 * Applies conversation details fetched from disk
		 * @param conversationInfo The conversation
		 * @param conversationLazyLoader The conversation's lazy loader for fetching past messages
		 */
		private void applyConversation(ConversationInfo conversationInfo, DatabaseManager.ConversationLazyLoader conversationLazyLoader) {
			//Setting the values
			this.conversationInfo = conversationInfo;
			this.conversationLazyLoader = conversationLazyLoader;
			
			//Loading the conversation's title
			titleLD.setValue(ConversationBuildHelper.buildConversationTitleDirect(getApplication(), conversationInfo));
			updateConversationTitle();
			
			//Loading the conversation's drafts
			queueList.addAll(conversationInfo.getDraftFiles().stream().map(fileDraft -> new FileQueued(fileDraft, nextDraftFileReferenceID++)).collect(Collectors.toList()));
			
			//Loading the conversation's messages
			loadMessages();
		}
		
		/**
		 * Loads the first group of messages from a conversation
		 */
		private void loadMessages() {
			//Updating the state
			stateLD.setValue(stateLoadingMessages);
			
			//Loading the messages
			compositeDisposable.add(
					Single.fromCallable(() -> conversationLazyLoader.loadNextChunk(getApplication()))
							.subscribeOn(Schedulers.single())
							.observeOn(AndroidSchedulers.mainThread()).subscribe((result) -> {
						//Checking if the result is invalid
						if(result == null) {
							//Setting the state
							stateLD.setValue(stateFailedMessages);
							
							//Returning
							return;
						}
						
						//Creating the lists
						conversationItemList = result;
						conversationGhostList = conversationItemList.stream().filter(conversationItem ->
								conversationItem instanceof MessageInfo && ((MessageInfo) conversationItem).getMessageState() == MessageState.ghost)
								.map(conversationItem -> (MessageInfo) conversationItem).collect(Collectors.toList());
						
						//Setting the read and delivered targets
						for(ListIterator<ConversationItem> iterator = conversationItemList.listIterator(conversationItemList.size()); iterator.hasPrevious();) {
							ConversationItem conversationItem = iterator.previous();
							
							if(conversationItem.getItemType() != ConversationItemType.message) continue;
							MessageInfo messageInfo = (MessageInfo) conversationItem;
							if(!messageInfo.isOutgoing()) continue;
							
							//Find latest delivered
							if(latestMessageDelivered == null) {
								if(messageInfo.getMessageState() == MessageState.read) {
									latestMessageDelivered = messageInfo;
									latestMessageRead = messageInfo;
									break;
								} else if(messageInfo.getMessageState() == MessageState.delivered) {
									latestMessageDelivered = messageInfo;
									continue;
								}
							}
							
							//Find latest read
							if(messageInfo.getMessageState() == MessageState.read) {
								latestMessageRead = messageInfo;
								break;
							}
						}
						
						//Marking all messages as read (list will always be scrolled to the bottom)
						conversationInfo.setUnreadMessageCount(0);
						
						//Setting the state
						stateLD.setValue(stateReady);
					})
			);
		}
		
		/**
		 * Loads the next group of messages from the database
		 */
		void loadNextChunk() {
			//Returning if the conversation isn't ready, a load is already in progress or there are no conversation items
			if(stateLD.getValue() != stateReady || isProgressiveLoadInProgress() || progressiveLoadReachedLimit || conversationItemList.isEmpty()) return;
			
			//Setting the flags
			progressiveLoadInProgress.setValue(true);
			
			compositeDisposable.add(Single.fromCallable(() -> conversationLazyLoader.loadNextChunk(getApplication()))
					.subscribeOn(Schedulers.single())
					.observeOn(AndroidSchedulers.mainThread()).subscribe((result) -> {
						//Checking if there are no new conversation items
						if(result.isEmpty()) {
							//Disabling the progressive load (there are no more items to load)
							progressiveLoadReachedLimit = true;
						} else {
							//Adding the items
							conversationItemList.addAll(0, result);
						}
						
						//Finishing the progressive load
						progressiveLoadInProgress.setValue(false);
						
						//Setting the progressive load count
						subjectProgressiveLoadUpdate.onNext(result.size());
					}));
		}
		
		/**
		 * Gets the {@link ConversationInfo} loaded in this view model
		 */
		ConversationInfo getConversationInfo() {
			return conversationInfo;
		}
		
		/**
		 * Sets the new draft message of this conversation
		 */
		void applyDraftMessage(String message) {
			//Ignoring if the state is not loaded
			if(stateLD.getValue() != stateReady) return;
			
			//Invalidating the message if it is empty
			final String finalMessage = StringHelper.nullifyEmptyString(message);
			
			//Recording the update time
			long updateTime = System.currentTimeMillis();
			
			//Writing the message to disk
			ConversationActionTask.setConversationDraft(conversationInfo, finalMessage, updateTime).subscribe();
		}
		
		@Nullable
		MessageTargetUpdate tryApplyDeliveredTarget(MessageInfo messageInfo) {
			//Ignoring if the message is incoming or isn't delivered
			if(!messageInfo.isOutgoing() || messageInfo.getMessageState() != MessageState.delivered) return null;
			
			//Checking if the provided message can replace the current message
			if(latestMessageDelivered == null || messageInfo.getDate() > latestMessageDelivered.getDate()) {
				List<MessageInfo> oldMessages = new ArrayList<>();
				
				//Setting the delivered target
				if(latestMessageDelivered != null) oldMessages.add(latestMessageDelivered);
				latestMessageDelivered = messageInfo;
				
				return new MessageTargetUpdate(messageInfo, oldMessages);
			} else {
				return null;
			}
		}
		
		@Nullable
		MessageTargetUpdate tryApplyReadTarget(MessageInfo messageInfo) {
			//Ignoring if the message is incoming or isn't delivered
			if(!messageInfo.isOutgoing() || messageInfo.getMessageState() != MessageState.read) return null;
			
			//Checking if the provided message can replace the current message
			if(latestMessageRead == null || messageInfo.getDate() > latestMessageRead.getDate()) {
				List<MessageInfo> oldMessages = new ArrayList<>();
				
				//Setting the read target
				if(latestMessageRead != null) oldMessages.add(latestMessageRead);
				latestMessageRead = messageInfo;
				
				//Clearing the delivered target if this read target is surpassing it
				if(latestMessageDelivered != null && latestMessageDelivered.getDate() <= messageInfo.getDate()) {
					oldMessages.add(latestMessageDelivered);
					latestMessageDelivered = null;
				}
				
				return new MessageTargetUpdate(messageInfo, oldMessages);
			} else {
				return null;
			}
		}
		
		/**
		 * Clears the conversation's draft message and draft files
		 */
		void clearDrafts() {
			//Clearing the queued list
			queueList.clear();
			
			//Clearing the conversation's drafts in memory
			if(conversationInfo != null) {
				conversationInfo.setDraftMessage(null);
				conversationInfo.getDraftFiles().clear();
				conversationInfo.setDraftUpdateTime(-1);
			}
			
			//Clearing the conversation's drafts on disk
			Completable.fromAction(() -> {
				DatabaseManager.getInstance().clearDraftReferences(conversationIDTarget);
				DatabaseManager.getInstance().updateConversationDraftMessage(conversationIDTarget, null, -1);
			}).subscribeOn(Schedulers.single())
					.observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationDraftFileClear(conversationInfo)))
					.subscribe();
		}
		
		/**
		 * Gets if we are currently loading past messages in the background
		 */
		boolean isProgressiveLoadInProgress() {
			Boolean value = progressiveLoadInProgress.getValue();
			return value == null ? false : value;
		}
		
		/**
		 * Builds and updates the title of the conversation
		 */
		void updateConversationTitle() {
			//Cancelling the title task
			if(conversationTitleDisposable != null && !conversationTitleDisposable.isDisposed()) conversationTitleDisposable.dispose();
			
			//Building and setting the conversation title
			compositeDisposable.add(conversationTitleDisposable = ConversationBuildHelper.buildConversationTitle(getApplication(), conversationInfo).subscribe(titleLD::setValue, (error) -> {}));
		}
		
		/**
		 * Updates the conversation actions based on the current conversation state
		 */
		void updateConversationActions() {
			//Ignoring if smart reply is disabled or the conversation has no messages
			if(!Preferences.getPreferenceReplySuggestions(getApplication()) || conversationItemList == null || conversationItemList.isEmpty()) return;
			
			//Finding the last message
			MessageInfo lastMessage = null;
			for(int i  = conversationItemList.size() - 1; i >= 0; i--) {
				ConversationItem item = conversationItemList.get(i);
				if(!(item instanceof MessageInfo)) continue;
				lastMessage = (MessageInfo) item;
				break;
			}
			
			//Ignoring if the last message hasn't changed
			if(lastMessage != null && lastConversationActionTarget == lastMessage.getLocalID()) return;
			
			//Cancelling the last smart reply task
			if(conversationActionsDisposable != null && !conversationActionsDisposable.isDisposed()) conversationActionsDisposable.dispose();
			
			//Checking if the last message isn't valid
			if(lastMessage == null || lastMessage.getMessageText() == null ||
					lastMessage.isOutgoing() || SendStyleHelper.appleSendStyleBubbleInvisibleInk.equals(lastMessage.getSendStyle())) {
				//Hiding the conversation actions
				conversationActionsLD.setValue(null);
				
				return;
			}
			
			//Collecting the last 10 messages
			List<MessageInfo> messageHistory = new ArrayList<>(SmartReplyHelper.smartReplyHistoryLength);
			for(int i  = conversationItemList.size() - 1; i >= 0; i--) {
				ConversationItem item = conversationItemList.get(i);
				if(!(item instanceof MessageInfo)) continue;
				MessageInfo message = (MessageInfo) item;
				if(message.getMessageText() == null) continue;
				messageHistory.add(message);
				
				if(messageHistory.size() == SmartReplyHelper.smartReplyHistoryLength) break;
			}
			
			//Requesting smart reply
			compositeDisposable.add(
					conversationActionsDisposable = SmartReplyHelper.generateResponses(getApplication(), messageHistory)
							.subscribe(responses -> {
								if(responses.length > 0) conversationActionsLD.setValue(responses);
								else conversationActionsLD.setValue(null);
							})
			);
			
			lastConversationActionTarget = lastMessage.getLocalID();
		}
		
		/**
		 * Clears the current conversation actions, for use after a mutating action has been used
		 */
		void clearConversationActions() {
			//Cancelling the smart reply task
			if(conversationActionsDisposable != null && !conversationActionsDisposable.isDisposed()) conversationActionsDisposable.dispose();
			
			//Clearing the current actions
			conversationActionsLD.setValue(null);
		}
		
		/**
		 * Gets the maximum file size for attachments for the current messaging service
		 * @return The maximum file size in bytes, or -1 if unlimited
		 */
		private int getFileCompressionTarget() {
			//Apple continuity MMS messaging
			if(conversationInfo.getServiceHandler() == ServiceHandler.appleBridge && ServiceType.appleSMS.equals(conversationInfo.getServiceType())) {
				//Defaulting to 300 KB
				return 300 * 1024;
			}
			
			//Android system MMS messaging
			if(conversationInfo.getServiceHandler() == ServiceHandler.systemMessaging && ServiceType.systemSMS.equals(conversationInfo.getServiceType())) {
				//Returning the carrier-specific information
				return MMSSMSHelper.getMaxMessageSize(getApplication());
			}
			
			//No compression required
			return -1;
		}
		
		/**
		 * Adds a file to the queue and prepares it as a draft
		 */
		void queueFile(FileLinked file) {
			//Creating the queued data
			FileQueued fileQueued = new FileQueued(file, nextDraftFileReferenceID++);
			int additionIndex = queueList.size();
			queueList.add(fileQueued);
			subjectQueueListAdd.onNext(new Pair<>(additionIndex, fileQueued));
			
			long updateTime = System.currentTimeMillis();
			
			//Preparing the attachment
			fileQueued.setPrepareDisposable(
					DraftActionTask.prepareLinkedToDraft(getApplication(), file, conversationInfo.getLocalID(), getFileCompressionTarget(), file.getFile().isA(), updateTime)
							.flatMapMaybe(draft -> {
								//Checking if this file is to be deleted
								if(fileQueued.shouldRemove()) {
									return deleteDraftFile(draft, System.currentTimeMillis()).andThen(Maybe.empty()); //Don't do anything after; removing the file from the queue should have already been taken care of for us
								} else {
									//Continue and add the draft file
									return Maybe.just(draft);
								}
							})
							.doOnSuccess(draft -> {
								//Sending an update
								ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationDraftFileUpdate(conversationInfo, draft, true, updateTime));
							})
							.subscribe(draft -> {
								//Queuing the file
								int currentIndex = queueList.indexOf(fileQueued);
								fileQueued.setDraft(draft);
								subjectQueueListUpdate.onNext(new Pair<>(currentIndex, fileQueued));
							}, error -> {
								//Logging the error
								Log.w(TAG, "Failed to queue draft", error);
								FirebaseCrashlytics.getInstance().recordException(error);
								
								//Dequeuing the file
								int currentIndex = queueList.indexOf(fileQueued);
								queueList.remove(fileQueued);
								subjectQueueListRemove.onNext(new Pair<>(currentIndex, fileQueued));
							})
			);
		}
		
		/**
		 * Cancels any running tasks and removes the file from the queue
		 */
		void dequeueFile(FileQueued file) {
			//Disposing of the queued file's tasks
			file.dispose();
			
			if(file.getFile().isA()) {
				//If this file is still linked, mark it to be removed when it completes
				file.setShouldRemove(true);
			} else {
				long updateTime = System.currentTimeMillis();
				
				//If this file is queued, delete just delete it
				deleteDraftFile(file.getFile().getB(), updateTime).subscribe();
				
				//Sending an update
				ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationDraftFileUpdate(conversationInfo, file.getFile().getB(), false, updateTime));
			}
			
			//Removing the file from the queue
			int currentIndex = queueList.indexOf(file);
			queueList.remove(file);
			subjectQueueListRemove.onNext(new Pair<>(currentIndex, file));
		}
		
		/**
		 * Deletes a draft file from disk
		 */
		private static Completable deleteDraftFile(FileDraft draft, long updateTime) {
			return Completable.fromAction(() -> {
				//Deleting the draft file
				AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraft, draft.getFile());
			}).subscribeOn(Schedulers.io())
					.observeOn(Schedulers.single())
					.doOnComplete(() -> {
						//Removing the item from the database
						DatabaseManager.getInstance().removeDraftReference(draft.getLocalID(), updateTime);
					}).observeOn(AndroidSchedulers.mainThread());
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
				String type = itemDesc.getMimeTypeCount() > 0 ? itemDesc.getMimeType(0) : MIMEConstants.defaultMIMEType;
				
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
	
	/**
	 * Represents a file that's either being processed into a {@link FileDraft} or already is one
	 */
	private static class FileQueued {
		private Union<FileLinked, FileDraft> file;
		private Disposable disposablePrepare;
		private boolean shouldRemove = false;
		@AttachmentType private final int attachmentType;
		private final int referenceID;
		
		public FileQueued(FileLinked fileLinked, int referenceID) {
			this.file = Union.ofA(fileLinked);
			this.attachmentType = FileHelper.getAttachmentType(fileLinked.getFileType());
			this.referenceID = referenceID;
		}
		
		public FileQueued(FileDraft fileDraft, int referenceID) {
			this.file = Union.ofB(fileDraft);
			this.attachmentType = FileHelper.getAttachmentType(fileDraft.getFileType());
			this.referenceID = referenceID;
		}
		
		/**
		 * Gets the {@link FileLinked} or {@link FileDraft} of this queued entry
		 */
		public Union<FileLinked, FileDraft> getFile() {
			return file;
		}
		
		/**
		 * Gets the media store ID of this queued file, or -1 if unavailable
		 */
		public long getMediaStoreID() {
			return file.map(file -> file.getMediaStoreData() != null ? file.getMediaStoreData().getMediaStoreID() : -1, FileDraft::getMediaStoreID);
		}
		
		/**
		 * Gets an integer ID that is guaranteed to be unique for the lifecycle of this draft
		 */
		public int getReferenceID() {
			return referenceID;
		}
		
		/**
		 * Gets the {@link AttachmentType} of this queued entry
		 */
		@AttachmentType
		public int getAttachmentType() {
			return attachmentType;
		}
		
		/**
		 * Cancels any tasks associated with this item
		 */
		public void dispose() {
			if(disposablePrepare != null && !disposablePrepare.isDisposed()) disposablePrepare.dispose();
		}
		
		public void setShouldRemove(boolean shouldRemove) {
			this.shouldRemove = shouldRemove;
		}
		
		/**
		 * Gets whether this queued file should be cleaned up
		 */
		public boolean shouldRemove() {
			return shouldRemove;
		}
		
		/**
		 * Sets the prepare disposable for this file
		 */
		public void setPrepareDisposable(Disposable disposable) {
			if(disposablePrepare != null) throw new IllegalStateException("Trying to assign a disposable, but one already exists!");
			disposablePrepare = disposable;
		}
		
		/**
		 * Replaces the linked file with a draft file
		 */
		public void setDraft(FileDraft fileDraft) {
			file = Union.ofB(fileDraft);
		}
	}
}