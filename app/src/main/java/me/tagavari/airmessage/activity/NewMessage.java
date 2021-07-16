package me.tagavari.airmessage.activity;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.*;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import kotlin.Pair;
import me.tagavari.airmessage.BuildConfig;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginConnectionService;
import me.tagavari.airmessage.compositeplugin.PluginQNavigation;
import me.tagavari.airmessage.compositeplugin.PluginRXDisposable;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.helper.*;
import me.tagavari.airmessage.messaging.ChatCreateAction;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.task.ContactsTask;
import me.tagavari.airmessage.util.AddressInfo;
import me.tagavari.airmessage.util.ContactInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

public class NewMessage extends AppCompatCompositeActivity {
	//Creating the constants
	private static final int menuIdentifierConfirmParticipants = 0;
	
	private static final int permissionRequestContacts = 0;
	
	private static final MessageServiceDescription[] availableServiceArray = BuildConfig.DEBUG ? new MessageServiceDescription[]{
			new MessageServiceDescription(R.drawable.message_push, R.string.title_imessage, R.color.colorPrimary, ServiceHandler.appleBridge, ServiceType.appleMessage, true),
			new MessageServiceDescription(R.drawable.message_bridge, R.string.title_textmessageforwarding, R.color.colorMessageTextMessageForwarding, ServiceHandler.appleBridge, ServiceType.appleSMS, false),
			new MessageServiceDescription(R.drawable.message_sms, R.string.title_textmessage, R.color.colorMessageTextMessage, ServiceHandler.systemMessaging, ServiceType.systemSMS, false),
			//new MessageServiceDescription(R.drawable.message_plus, R.string.title_rcs, false, -1, R.color.colorMessageRCS, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemRCS, false),
	} : new MessageServiceDescription[]{
			new MessageServiceDescription(R.drawable.message_push, R.string.title_imessage, R.color.colorPrimary, ServiceHandler.appleBridge, ServiceType.appleMessage, true),
			//new MessageServiceDescription(R.drawable.message_bridge, R.string.title_textmessageforwarding, false, -1, R.color.colorMessageTextMessageForwarding, ConversationInfo.serviceHandlerAMBridge, ConversationInfo.serviceTypeAppleTextMessageForwarding, false),
			new MessageServiceDescription(R.drawable.message_sms, R.string.title_textmessage, R.color.colorMessageTextMessage, ServiceHandler.systemMessaging, ServiceType.systemSMS, false),
			//new MessageServiceDescription(R.drawable.message_plus, R.string.title_rcs, false, -1, R.color.colorMessageRCS, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemRCS, false),
	};
	
	//Creating the view model and plugin values
	private ActivityViewModel viewModel;
	
	private PluginQNavigation pluginQNavigation;
	private PluginConnectionService pluginCS;
	private PluginRXDisposable pluginRXCD;
	
	//Creating the state values
	private boolean serviceSelectorAvailable;
	
	//Creating the view values
	private ViewGroup recipientListGroup;
	private MenuItem confirmMenuItem;
	private EditText recipientInput;
	private ImageButton recipientInputToggle;
	private RecyclerView contactListView;
	private RecyclerAdapter contactsListAdapter;
	
	private ViewGroup groupMessagePermission;
	private ViewGroup groupMessageError;
	//private ListView contactListView;
	//private ListAdapter contactsListAdapter;
	
	private ServiceChipData[] serviceChipDataArray = new ServiceChipData[availableServiceArray.length];
	private ServiceChipData currentActiveServiceChip = null;
	
	private final Observer<Integer> contactStateObserver = state -> {
		switch(state) {
			case ActivityViewModel.contactStateReady:
				contactListView.setVisibility(View.VISIBLE);
				groupMessagePermission.setVisibility(View.GONE);
				groupMessageError.setVisibility(View.GONE);
				break;
			case ActivityViewModel.contactStateNoAccess:
				contactListView.setVisibility(View.GONE);
				groupMessagePermission.setVisibility(View.VISIBLE);
				groupMessageError.setVisibility(View.GONE);
				break;
			case ActivityViewModel.contactStateFailed:
				contactListView.setVisibility(View.GONE);
				groupMessagePermission.setVisibility(View.GONE);
				groupMessageError.setVisibility(View.VISIBLE);
				break;
		}
	};
	
	//Creating the listener values
	private final TextWatcher recipientInputTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			/* //Getting the character sequence as a string
			String cleanString = s.toString();
			
			//Returning if the name is empty or does not contain a new line
			if(!cleanString.contains("\n")) return;
			
			//Removing the new line from the string
			cleanString = cleanString.replaceFirst("\n", "");
			int lastIndexSpace = cleanString.lastIndexOf(" ", recipientInput.getSelectionStart());
			String scopedString;
			if(lastIndexSpace == -1) scopedString = cleanString;
			else scopedString = cleanString.substring(lastIndexSpace);
			
			
			//Checking if the name does not pass validation
			if(!cleanString.matches(telephoneRegEx) && !cleanString.matches(emailRegEx)) {
				//Recording the selection
				int[] selection = {recipientInput.getSelectionStart() - 1, recipientInput.getSelectionEnd() - 1};
				
				//Setting the updated name
				recipientInput.setText(s.toString());
				
				//Restoring the selection
				recipientInput.setSelection(selection[0], selection[1]);
				
				//Returning
				return;
			}
			
			//Clearing the name
			recipientInput.setText(scopedString.substring(0, lastIndexSpace));
			
			//Adding a chip
			addChip(new Chip(cleanString)); */
			
			//Filtering the list
			contactsListAdapter.filterList(s.toString());
		}
		
		@Override
		public void afterTextChanged(Editable s) {
		
		}
	};
	//Creating the retained fragment values
	private final View.OnKeyListener recipientInputOnKeyListener = new View.OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			//Returning if the event is not a key down
			if(event.getAction() != KeyEvent.ACTION_DOWN) return false;
			
			//Checking if the key is the delete key
			if(keyCode == KeyEvent.KEYCODE_DEL) {
				//Checking if the cursor is at the start and there are chips
				if(recipientInput.getSelectionStart() == 0 && recipientInput.getSelectionEnd() == 0 && !viewModel.userChips.isEmpty()) {
					//Removing a chip
					removeChip(viewModel.userChips.get(viewModel.userChips.size() - 1));
					
					//Returning true
					return true;
				}
			}
			
			//Returning false
			return false;
		}
	};
	private final TextView.OnEditorActionListener recipientInputOnActionListener = new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			//Checking if the action is the "done" button
			if(actionId == EditorInfo.IME_ACTION_DONE) {
				//Getting the string
				String cleanString = recipientInput.getText().toString().trim();
				
				//Checking if the string passes validation
				if(AddressHelper.validateAddress(cleanString)) {
					//Adding a chip
					addChip(new Chip(cleanString, AddressHelper.normalizeAddress(cleanString)));
					
					//Clearing the text input
					recipientInput.setText("");
				}
				
				//Returning true
				return true;
			}
			
			//Returning false
			return false;
		}
	};
	
	public NewMessage() {
		addPlugin(pluginQNavigation = new PluginQNavigation());
		addPlugin(pluginCS = new PluginConnectionService());
		addPlugin(pluginRXCD = new PluginRXDisposable());
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Reading the launch arguments
		final String targetText = getIntent().getStringExtra(Messaging.intentParamDataText);
		final ClipData targetClipData;
		if(getIntent().getBooleanExtra(Messaging.intentParamDataFile, false)) targetClipData = getIntent().getClipData();
		else targetClipData = null;
		
		//Setting the content view
		setContentView(R.layout.activity_newmessage);
		
		//Configuring the toolbar
		setSupportActionBar(findViewById(R.id.toolbar));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Getting the views
		recipientListGroup = findViewById(R.id.group_recipientlist);
		recipientInput = findViewById(R.id.input_recipients);
		recipientInputToggle = findViewById(R.id.button_recipients);
		contactListView = findViewById(R.id.list_contacts);
		
		groupMessagePermission = findViewById(R.id.group_permission);
		groupMessageError = findViewById(R.id.group_error);
		
		//Enforcing the maximum content width
		WindowHelper.enforceContentWidthView(getResources(), contactListView);
		
		//Setting the list padding
		PluginQNavigation.setViewForInsets(findViewById(android.R.id.content), contactListView);
		/* ViewCompat.setOnApplyWindowInsetsListener(contactListView, (view, insets) -> {
			view.setPadding(insets.getSystemWindowInsetLeft(), view.getPaddingTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
			return insets.consumeSystemWindowInsets();
		}); */
		
		//Configuring the AMOLED theme
		if(ThemeHelper.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Setting the status bar color
		PlatformHelper.updateChromeOSStatusBar(this);
		
		//Adding the input listeners
		recipientInput.addTextChangedListener(recipientInputTextWatcher);
		recipientInput.setOnKeyListener(recipientInputOnKeyListener);
		recipientInput.setOnEditorActionListener(recipientInputOnActionListener);
		recipientInput.requestFocus();
		
		//Getting the view model
		viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
			@NonNull
			@Override
			public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
				return (T) new ActivityViewModel(getApplication(), targetText, targetClipData);
			}
		}).get(ActivityViewModel.class);
		viewModel.setActivityReference(this);
		
		//Registering the observers
		viewModel.contactState.observe(this, contactStateObserver);
		viewModel.loadingState.observe(this, value -> setActivityState(!value, true));
		viewModel.completionLaunchIntent.observe(this, intent -> {
			startActivity(intent);
			finish();
		});
		pluginRXCD.activity().add(viewModel.contactListSubject.subscribe(update -> update.updateAdapter(contactsListAdapter)));
		
		//Restoring the input bar
		restoreInputBar();
		
		//Configuring the list
		contactsListAdapter = new RecyclerAdapter(viewModel.contactList);
		contactListView.setItemAnimator(null);
		contactListView.setAdapter(contactsListAdapter);
		
		if(serviceSelectorAvailable = Preferences.getPreferenceTextMessageIntegration(this)) {
			//Building the service selector
			buildServiceSelector();
			
			//Updating the list filter
			contactsListAdapter.setFilterPhoneOnly(!viewModel.currentService.serviceSupportsEmail);
		} else {
			//Hiding the service selector input
			findViewById(R.id.group_service).setVisibility(View.GONE);
		}
	}
	
	void setDarkAMOLED() {
		ThemeHelper.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(ColorConstants.colorAMOLED);
	}
	
	void setDarkAMOLEDSamsung() {
		ThemeHelper.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(ColorConstants.colorAMOLED);
		
		contactListView.setBackgroundResource(R.drawable.background_amoledsamsung);
		contactListView.setClipToOutline(true);
		contactListView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
	}
	
	private void restoreInputBar() {
		//Restoring the input type
		if(viewModel.recipientInputAlphabetical) {
			recipientInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			recipientInputToggle.setImageResource(R.drawable.dialpad);
		} else {
			recipientInput.setInputType(InputType.TYPE_CLASS_PHONE);
			recipientInputToggle.setImageResource(R.drawable.keyboard_outlined);
		}
		
		//Restoring the chips
		if(viewModel.userChips.isEmpty()) {
			//Setting the hint
			recipientInput.setHint(R.string.imperative_userinput);
		} else {
			//Removing the hint
			recipientInput.setHint("");
			
			//Adding the views
			int chipIndex = 0;
			for(Chip chip : viewModel.userChips) {
				((ViewGroup) chip.getView().getParent()).removeView(chip.getView());
				recipientListGroup.addView(chip.getView(), chipIndex);
				chipIndex++;
			}
		}
	}
	
	private void buildServiceSelector() {
		//Getting the view group
		ViewGroup viewGroup = findViewById(R.id.list_service);
		
		for(int i = 0; i < availableServiceArray.length; i++) {
			//Getting the service description
			MessageServiceDescription serviceDescription = availableServiceArray[i];
			
			//Creating the service chip view
			ViewGroup chipView = (ViewGroup) getLayoutInflater().inflate(R.layout.listitem_servicechip, viewGroup, false);
			TextView label = chipView.findViewById(R.id.label);
			ImageView icon = chipView.findViewById(R.id.icon);
			
			//Creating the service chip data
			ServiceChipData serviceChipData = new ServiceChipData(serviceDescription, chipView, label, icon);
			serviceChipDataArray[i] = serviceChipData;
			
			//Setting the label text and icon
			label.setText(serviceDescription.title);
			icon.setImageResource(serviceDescription.icon);
			
			//Checking if this is the currently selected service
			if(serviceDescription == viewModel.currentService) {
				//Showing the label
				label.setVisibility(View.VISIBLE);
				
				//Coloring the label
				label.setTextColor(getResources().getColor(serviceDescription.color, null));
				
				//Coloring the icon
				icon.setImageTintList(ColorStateList.valueOf(getResources().getColor(serviceDescription.color, null)));
				
				//Setting the view as the current active view
				currentActiveServiceChip = serviceChipData;
			} else {
				//Hiding the label
				label.setVisibility(View.GONE);
				
				//Coloring the icon
				icon.setImageTintList(ColorStateList.valueOf(ResourceHelper.resolveColorAttr(this, android.R.attr.colorControlNormal)));
			}
			
			//Setting the click listener
			chipView.setOnClickListener(view -> {
				//Ignoring if this is the currently active service
				if(currentActiveServiceChip == serviceChipData) return;
				
				//Starting a transition
				TransitionManager.beginDelayedTransition(findViewById(R.id.list_service));
				
				//Disabling the old view
				currentActiveServiceChip.getLabel().setVisibility(View.GONE);
				currentActiveServiceChip.getLabel().setTextColor(ColorStateList.valueOf(ResourceHelper.resolveColorAttr(this, android.R.attr.textColorSecondary)));
				currentActiveServiceChip.getIcon().setImageTintList(ColorStateList.valueOf(ResourceHelper.resolveColorAttr(this, android.R.attr.colorControlNormal)));
				
				//Enabling the new view
				label.setVisibility(View.VISIBLE);
				label.setTextColor(getResources().getColor(serviceDescription.color, null));
				icon.setImageTintList(ColorStateList.valueOf(getResources().getColor(serviceDescription.color, null)));
				
				//Setting the new selected service view
				currentActiveServiceChip = serviceChipData;
				
				//Setting the selected service
				viewModel.currentService = currentActiveServiceChip.getServiceDescription();
				
				//Updating the list
				contactsListAdapter.setFilterPhoneOnly(!serviceDescription.serviceSupportsEmail);
			});
			
			//textView.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(serviceDescription.color, null)));
			//textView.setCompoundDrawableTintList(ColorStateList.valueOf(Constants.resolveColorAttr(this, android.R.attr.textColorSecondary)));
			
			//Adding the view
			viewGroup.addView(chipView);
		}
	}
	
	private void setPhoneServiceState(boolean enabled) {
		for(ServiceChipData service : serviceChipDataArray) {
			//Ignoring services that support email
			if(service.getServiceDescription().serviceSupportsEmail) continue;
			
			//Updating the service state
			if(enabled) {
				service.getView().setClickable(true);
				service.getView().animate().alpha(1);
			} else {
				service.getView().setClickable(false);
				service.getView().animate().alpha(ColorConstants.disabledAlpha);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Creating the "confirm participants" menu button
		confirmMenuItem = menu.add(Menu.NONE, menuIdentifierConfirmParticipants, Menu.NONE, R.string.action_confirmparticipants);
		confirmMenuItem.setIcon(R.drawable.next);
		//confirmMenuItem.setIconTintList(ColorStateList.valueOf(Constants.resolveColorAttr(this, android.R.attr.colorControlNormal)));
		confirmMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		//Hiding the menu button
		confirmMenuItem.setVisible(!viewModel.userChips.isEmpty());
		if(viewModel.loadingState.getValue() == Boolean.TRUE) confirmMenuItem.setEnabled(false);
		
		//Returning true
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home: //Up button
				//Finishing the activity
				finish();
				
				//Returning true
				return true;
			case menuIdentifierConfirmParticipants: //Confirm participants button
				//Confirming the participants
				confirmParticipants();
				
				//Returning true
				return true;
		}
		
		//Returning false
		return false;
	}
	
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		//Returning if there were no grant results
		if(grantResults.length == 0) return;
		
		//Checking if the request code is contacts access
		if(requestCode == permissionRequestContacts) {
			//Checking if the result is a success
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				//Loading the contacts
				viewModel.loadContacts();
				
				//Starting the update listener
				MainApplication.getInstance().registerContactsListener();
			}
			//Otherwise checking if the result is a denial
			else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
				//Showing a snackbar
				Snackbar.make(findViewById(android.R.id.content), R.string.message_permissionrejected, Snackbar.LENGTH_LONG)
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
	
	public void toggleInputType(View view) {
		//Saving the selection
		int selectionStart = recipientInput.getSelectionStart();
		int selectionEnd = recipientInput.getSelectionEnd();
		
		//Checking if the input is alphabetical
		if(viewModel.recipientInputAlphabetical) {
			//Setting the input type
			recipientInput.setInputType(InputType.TYPE_CLASS_PHONE);
			//recipientInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
			
			//Setting the toggle input type icon
			((ImageButton) view).setImageResource(R.drawable.keyboard_outlined);
			
			//Setting the alphabetical input variable
			viewModel.recipientInputAlphabetical = false;
		} else {
			//Setting the input type
			recipientInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			//recipientInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
			
			//Setting the toggle input type icon
			((ImageButton) view).setImageResource(R.drawable.dialpad);
			
			//Setting the alphabetical input variable
			viewModel.recipientInputAlphabetical = true;
		}
		
		//Restoring the selection
		recipientInput.setSelection(selectionStart, selectionEnd);
	}
	
	private void confirmParticipants() {
		//Disabling the UI
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		
		//Passing the event to the view model
		viewModel.confirmParticipants(getRecipientAddressList(), pluginCS.getConnectionManager());
	}
	
	public void onClickRequestContacts(View view) {
		//Requesting the permission
		requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, permissionRequestContacts);
	}
	
	public void onClickRetryLoad(View view) {
		if(viewModel.contactState.getValue() == ActivityViewModel.contactStateFailed) viewModel.loadContacts();
	}
	
	private ArrayList<String> getRecipientAddressList() {
		//Converting the user chips to a string list
		ArrayList<String> recipients = new ArrayList<>();
		for(Chip chip : viewModel.userChips) recipients.add(chip.getAddress());
		
		//Sorting the list
		Collections.sort(recipients);
		
		//Normalizing the list
		//Constants.normalizeAddresses(recipients);
		
		//Returning the recipient list
		return recipients;
	}
	
	private void setActivityState(boolean enabled, boolean animate) {
		//Disabling the button
		if(confirmMenuItem != null) confirmMenuItem.setEnabled(enabled);
		
		//Disabling the inputs
		recipientInput.setEnabled(enabled);
		recipientInputToggle.setEnabled(enabled);
		
		//Disabling the list
		contactListView.setEnabled(enabled);
		
		View scrim = findViewById(R.id.scrim_content);
		LinearProgressIndicator progressBar  = findViewById(R.id.progressbar_content);
		if(animate) {
			if(enabled) {
				scrim.animate().alpha(0).withEndAction(() -> scrim.setVisibility(View.GONE)).start();
				progressBar.animate().alpha(0).withEndAction(() -> progressBar.setVisibility(View.GONE)).start();
			} else {
				scrim.animate().alpha(1).withStartAction(() -> scrim.setVisibility(View.VISIBLE)).start();
				progressBar.animate().alpha(1).withStartAction(() -> progressBar.setVisibility(View.VISIBLE)).setStartDelay(1500).start();
			}
		} else {
			if(enabled) {
				scrim.setVisibility(View.GONE);
				scrim.setAlpha(0);
				progressBar.setVisibility(View.GONE);
			} else {
				scrim.setVisibility(View.VISIBLE);
				scrim.setAlpha(1);
				progressBar.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void addChip(Chip chip) {
		//Validating the chip
		for(Chip existingChips : viewModel.userChips) if(existingChips.getAddress().equals(chip.getAddress())) return;
		
		//Removing the hint from the recipient input if this is the first chip
		if(viewModel.userChips.isEmpty()) recipientInput.setHint("");
		
		//Adding the chip to the list
		viewModel.userChips.add(chip);
		
		//Adding the view
		recipientListGroup.addView(chip.getView(), viewModel.userChips.size() - 1);
		
		//Setting the confirm button as visible
		confirmMenuItem.setVisible(true);
		
		//Checking if the service selector is available, and an email address is being added
		if(serviceSelectorAvailable && AddressHelper.validateEmail(chip.getAddress())) {
			//Disabling relevant services
			if(viewModel.participantsEmailCount == 0) setPhoneServiceState(false);
			
			//Adding to the email count
			viewModel.participantsEmailCount++;
		}
	}
	
	private void removeChip(Chip chip) {
		//Removing the chip from the list
		viewModel.userChips.remove(chip);
		
		//Removing the view
		recipientListGroup.removeView(chip.getView());
		
		//Checking if there are no more chips
		if(viewModel.userChips.isEmpty()) {
			//Setting the hint
			recipientInput.setHint(R.string.imperative_userinput);
			
			//Setting the confirm button as invisible
			confirmMenuItem.setVisible(false);
		}
		
		//Checking if the service selector is available. and an email address is being removed
		if(serviceSelectorAvailable && AddressHelper.validateEmail(chip.getAddress())) {
			//Taking from the email count
			viewModel.participantsEmailCount--;
			
			//Re-enabling disabled services if there are no more emails
			if(viewModel.participantsEmailCount == 0) setPhoneServiceState(true);
		}
	}
	
	private class Chip {
		private final String display;
		private final String address;
		private final View view;
		
		Chip(String display, String address) {
			//Setting the data
			this.display = display;
			this.address = address;
			
			//Setting the view
			view = getLayoutInflater().inflate(R.layout.chip_user, null);
			
			//Setting the name
			((TextView) view.findViewById(R.id.text)).setText(display);
			
			//Setting the view's click listener
			view.setOnClickListener(click -> {
				//Inflating the view
				View popupView = getLayoutInflater().inflate(R.layout.popup_userchip, null);
				TextView labelView = popupView.findViewById(R.id.label_member);
				ImageView profileDefault = popupView.findViewById(R.id.profile_default);
				ImageView profileImage = popupView.findViewById(R.id.profile_image);
				
				//Setting the default information
				labelView.setText(display);
				profileDefault.setColorFilter(getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
				
				//Filling in the information
				final CompositeDisposable compositeDisposable = new CompositeDisposable();
				compositeDisposable.add(MainApplication.getInstance().getUserCacheHelper().getUserInfo(NewMessage.this, display)
						.onErrorComplete()
						.subscribe((userInfo) -> {
							//Setting the label to the user's display name
							labelView.setText(userInfo.getContactName());
							
							//Adding a sub-label with the user's address
							TextView addressView = popupView.findViewById(R.id.label_address);
							addressView.setText(display);
							addressView.setVisibility(View.VISIBLE);
							
							//Loading the user's icon
							Glide.with(NewMessage.this)
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
				
				//Creating the window
				final PopupWindow popupWindow = new PopupWindow(popupView, ResourceHelper.dpToPx(300), ResourceHelper.dpToPx(56));
				
				//popupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getServiceColor(R.color.colorForegroundLight, null)));
				popupWindow.setOutsideTouchable(true);
				popupWindow.setElevation(ResourceHelper.dpToPx(2));
				popupWindow.setEnterTransition(new ChangeBounds());
				popupWindow.setExitTransition(new Fade());
				
				//Setting the remove listener
				if(viewModel.loadingState.getValue() == Boolean.TRUE) {
					popupView.findViewById(R.id.button_remove).setEnabled(false);
				} else {
					popupView.findViewById(R.id.button_remove).setOnClickListener(view -> {
						//Removing this chip
						removeChip(Chip.this);
						
						//Dismissing the popup
						popupWindow.dismiss();
					});
				}
				
				//Showing the popup
				popupWindow.showAsDropDown(view);
				
				//Cancel running background tasks on dismiss
				popupWindow.setOnDismissListener(compositeDisposable::clear);
			});
		}
		
		String getDisplay() {
			return display;
		}
		
		String getAddress() {
			return address;
		}
		
		View getView() {
			return view;
		}
	}
	
	private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the type constants
		//private static final int TYPE_HEADER_SERVICE = 0;
		private static final int TYPE_HEADER_DIRECT = 0;
		private static final int TYPE_ITEM = 1;
		
		//Creating the list values
		private final List<ContactInfo> originalItems;
		private final List<ContactInfo> filteredItems = new ArrayList<>();
		
		//Creating the task values
		private Disposable searchDisposable;
		
		//Creating the other values
		/* private final boolean serviceSelectorHeaderEnabled;
		private boolean serviceSelectorHeaderVisible; */
		private boolean directAddHeaderVisible = false;
		private String lastFilterText = "";
		//Filter out phone numbers, for the sake of non-iMessage services
		private boolean filterPhoneOnly = false;
		
		RecyclerAdapter(List<ContactInfo> items) {
			//Setting the items
			originalItems = items;
			filteredItems.addAll(items);
		}
		
		/**
		 * Maps an index from the source list to its recycler view index
		 */
		public int mapSourceListIndex(int index) {
			if(directAddHeaderVisible) return index + 1;
			else return index;
		}
		
		public void onItemAdded(int additionIndex) {
			//If we're currently searching, ignore the item for now
			if(lastFilterText.isEmpty()) {
				filteredItems.add(originalItems.get(additionIndex));
				notifyItemInserted(mapSourceListIndex(additionIndex));
			}
		}
		
		public void onItemUpdated(int updateIndex) {
			if(lastFilterText.isEmpty()) {
				//Updating the item in the standard list view
				notifyItemChanged(mapSourceListIndex(updateIndex));
			} else {
				//Updating the item in the search view
				int searchIndex = filteredItems.indexOf(originalItems.get(updateIndex));
				if(searchIndex != -1) notifyItemChanged(mapSourceListIndex(searchIndex));
			}
		}
		
		/* class HeaderServiceViewHolder extends RecyclerView.ViewHolder {
			//Creating the view values
			private final ViewGroup groupContent;
			private final ImageView icon;
			private final TextView labelTitle;
			private final TextView labelSubtitle;
			
			HeaderServiceViewHolder(View view) {
				//Calling the super method
				super(view);
				
				//Setting the views
				groupContent = (ViewGroup) view;
				icon = view.findViewById(R.id.icon);
				labelTitle = view.findViewById(R.id.label_title);
				labelSubtitle = view.findViewById(R.id.label_subtitle);
			}
		} */
		
		class HeaderDirectViewHolder extends RecyclerView.ViewHolder {
			//Creating the view values
			private final TextView label;
			
			HeaderDirectViewHolder(View view) {
				//Calling the super method
				super(view);
				
				//Setting the views
				label = view.findViewById(R.id.label);
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
		@NonNull
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			switch(viewType) {
				/* case TYPE_HEADER_SERVICE:
					return new HeaderServiceViewHolder(LayoutInflater.from(NewMessage.this).inflate(R.layout.listitem_serviceselection, parent, false)); */
				case TYPE_HEADER_DIRECT:
					return new HeaderDirectViewHolder(LayoutInflater.from(NewMessage.this).inflate(R.layout.listitem_contact_sendheader, parent, false));
				case TYPE_ITEM:
					return new ItemViewHolder(LayoutInflater.from(NewMessage.this).inflate(R.layout.listitem_contact, parent, false));
				default:
					throw new IllegalArgumentException("Invalid view type received, got " + viewType);
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
			switch(getItemViewType(position)) {
				/* case TYPE_HEADER_SERVICE: {
					//Casting the view holder
					HeaderServiceViewHolder itemVH = (HeaderServiceViewHolder) viewHolder;
					
					//Getting the service details
					MessageServiceDescription service = viewModel.currentService;
					
					//Setting the service details
					itemVH.icon.setImageResource(service.icon);
					itemVH.icon.setImageTintList(ColorStateList.valueOf(getResources().getColor(service.color, null)));
					itemVH.labelTitle.setText(service.title);
					itemVH.labelSubtitle.setText(R.string.message_selectedservice);
					
					//Breaking
					break;
				} */
				case TYPE_HEADER_DIRECT: {
					//Casting the view holder
					HeaderDirectViewHolder itemVH = (HeaderDirectViewHolder) viewHolder;
					
					//Setting the label
					itemVH.label.setText(getResources().getString(R.string.action_sendto, lastFilterText));
					
					//Setting the click listener
					itemVH.itemView.setOnClickListener(view -> {
						String cleanString = lastFilterText.trim();
						//Adding the chip
						addChip(new Chip(lastFilterText, AddressHelper.normalizeAddress(cleanString)));
						
						//Clearing the text
						recipientInput.setText("");
					});
					
					//Breaking
					break;
				}
				case TYPE_ITEM: {
					//Casting the view holder
					ItemViewHolder itemVH = (ItemViewHolder) viewHolder;
					
					//Getting the item
					ContactInfo contactInfo = getItemAtIndex(position);
					
					//Populating the view
					itemVH.contactName.setText(contactInfo.getName());
					
					int addressCount = contactInfo.getAddresses().size();
					String firstAddress = contactInfo.getAddresses().get(0).getAddress();
					if(addressCount == 1) itemVH.contactAddress.setText(firstAddress);
					else itemVH.contactAddress.setText(getResources().getQuantityString(R.plurals.message_multipledestinations, addressCount, firstAddress, addressCount - 1));
					
					//Showing / hiding the section header
					boolean showHeader;
					//if(!lastFilterText.isEmpty()) showHeader = false;
					if(position > getHeaderCount()) {
						ContactInfo contactInfoAbove = filteredItems.get(position - getHeaderCount() - 1);
						showHeader = contactInfoAbove == null || !stringsHeaderEqual(contactInfo.getName(), contactInfoAbove.getName());
					} else showHeader = true;
					
					if(showHeader) {
						itemVH.header.setVisibility(View.VISIBLE);
						itemVH.headerLabel.setText(Character.toString(getNameHeader(contactInfo.getName())));
					} else itemVH.header.setVisibility(View.GONE);
					
					//Resetting the image view
					itemVH.profileDefault.setVisibility(View.VISIBLE);
					itemVH.profileDefault.setColorFilter(getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
					itemVH.profileImage.setImageBitmap(null);
					
					//Assigning the contact's image
					Glide.with(NewMessage.this)
							.load(ContactHelper.getContactImageURI(contactInfo.getIdentifier()))
							.listener(new RequestListener<Drawable>() {
								@Override
								public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
									return false;
								}
								
								@Override
								public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
									//Hiding the default view
									itemVH.profileDefault.setVisibility(View.INVISIBLE);
									
									//Setting the bitmap
									itemVH.profileImage.setImageDrawable(resource);
									
									return true;
								}
							})
							.into(itemVH.profileImage);
					
					//Setting the click listener
					itemVH.contentArea.setOnClickListener(clickView -> {
						//Checking if there is only one label
						if(contactInfo.getAddresses().size() == 1) {
							//Adding the chip
							AddressInfo address = contactInfo.getAddresses().get(0);
							addChip(new Chip(address.getAddress(), address.getNormalizedAddress()));
							
							//Clearing the text
							recipientInput.setText("");
						} else {
							//Creating the dialog
							AlertDialog dialog = new MaterialAlertDialogBuilder(NewMessage.this)
									.setTitle(R.string.imperative_selectdestination)
									.setItems(contactInfo.getAddressDisplayList(getResources()).toArray(new String[0]), (dialogInterface, index) -> {
										//Adding the selected chip
										AddressInfo address = contactInfo.getAddresses().get(index);
										addChip(new Chip(address.getAddress(), address.getNormalizedAddress()));
										
										//Clearing the text
										recipientInput.setText("");
									})
									.create();
							
							//Disabling any unavailable items (email addresses, when only phone numbers can be used)
							if(filterPhoneOnly) {
								dialog.getListView().setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
									@Override
									public void onChildViewAdded(View parent, View child) {
										//Getting the item
										int index = ((ViewGroup) parent).indexOfChild(child);
										AddressInfo addressInfo = contactInfo.getAddresses().get(index);
										
										//Validating the address
										boolean enabled = AddressHelper.validatePhoneNumber(addressInfo.getNormalizedAddress());
										
										//Updating the child's status
										if(!enabled) {
											child.setEnabled(false);
											child.setAlpha(ColorConstants.disabledAlpha);
											child.setOnClickListener(null);
										}
									}
									
									@Override
									public void onChildViewRemoved(View parent, View child) {
									
									}
								});
							}
							
							//Showing the dialog
							dialog.show();
						}
					});
					
					//Breaking
					break;
				}
			}
		}
		
		private ContactInfo getItemAtIndex(int index) {
			return filteredItems.get(index - getHeaderCount());
		}
		
		private int getIndexOfItem(ContactInfo contactInfo) {
			return filteredItems.indexOf(contactInfo) + getHeaderCount();
		}
		
		@Override
		public int getItemCount() {
			return filteredItems.size() + getHeaderCount();
		}
		
		@Override
		public int getItemViewType(int position) {
			//Checking if the item is a header
			if(position < getHeaderCount()) return TYPE_HEADER_DIRECT;
			
			//Returning the item
			return TYPE_ITEM;
		}
		
		@Override
		public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
			//Cancelling the background task
			if(searchDisposable != null && !searchDisposable.isDisposed()) searchDisposable.dispose();
		}
		
		private int getHeaderCount() {
			int offset = 0;
			if(directAddHeaderVisible) offset++;
			return offset;
		}
		
		private char getNameHeader(String name) {
			if(name == null || name.isEmpty()) return '?';
			char firstChar = name.charAt(0);
			if(Character.isDigit(firstChar) || firstChar == '(') return '#';
			return firstChar;
		}
		
		private boolean stringsHeaderEqual(String string1, String string2) {
			if(string1 == null || string1.isEmpty()) return string2 == null || string2.isEmpty();
			return getNameHeader(string1) == getNameHeader(string2);
		}
		
		void onListUpdated() {
			filterList(lastFilterText);
		}
		
		void filterList(String filter) {
			//Setting the last filter text
			lastFilterText = filter;
			
			//Cleaning the filter
			filter = filter.trim();
			
			//Cancelling the current task
			if(searchDisposable != null && !searchDisposable.isDisposed()) searchDisposable.dispose();
			
			boolean filterEmpty = filter.isEmpty();
			
			//Checking if the filter is empty
			if(filterEmpty && !filterPhoneOnly) {
				//Hiding the direct add header
				directAddHeaderVisible = false;
				
				//Adding all of the items
				filteredItems.clear();
				filteredItems.addAll(originalItems);
				
				//Notifying the adapter
				notifyDataSetChanged();
			} else {
				//Updating the direct add header's visibility
				directAddHeaderVisible = !filterEmpty && AddressHelper.validateAddress(filter);
				
				//Filtering and updating
				filteredItems.clear();
				notifyDataSetChanged();
				searchDisposable = ContactsTask.searchContacts(originalItems, filter, filterPhoneOnly).subscribe(item -> {
					int insertionIndex = filteredItems.size();
					filteredItems.add(item);
					notifyItemInserted(mapSourceListIndex(insertionIndex));
				});
			}
		}
		
		void setFilterPhoneOnly(boolean value) {
			//Returning if the requested value is already the current value
			if(filterPhoneOnly == value) return;
			
			//Setting the value
			filterPhoneOnly = value;
			
			//Updating the list
			onListUpdated();
		}
	}
	
	public static class ActivityViewModel extends AndroidViewModel {
		//Creating the reference values
		static final int contactStateIdle = 0;
		static final int contactStateReady = 1;
		static final int contactStateNoAccess = 2;
		static final int contactStateFailed = 3;
		
		//Creating the state values
		final MutableLiveData<Integer> contactState = new MutableLiveData<>(); //The current state of the activity
		final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(); //Whether the activity is loading due to a participant confirmation
		final MutableLiveData<Intent> completionLaunchIntent = new MutableLiveData<>(); //An intent to launch to complete this activity
		final PublishSubject<ContactListReactiveUpdate> contactListSubject = PublishSubject.create(); //A subject to emit updates to the contacts list
		boolean recipientInputAlphabetical = true; //Whether the recipient input field is in alphabetical or numeric mode
		
		//Creating the input values
		private final List<Chip> userChips = new ArrayList<>();
		private int participantsEmailCount = 0;
		
		//Creating the fill values
		private final String targetText;
		private final ClipData targetClipData;
		
		//Creating the other values
		MessageServiceDescription currentService = availableServiceArray[0];
		final List<ContactInfo> contactList = new ArrayList<>();
		
		private WeakReference<NewMessage> activityReference = null;
		
		//Creating the task values
		private final CompositeDisposable compositeDisposable = new CompositeDisposable();
		
		public ActivityViewModel(@NonNull Application application, String targetText, ClipData targetClipData) {
			super(application);
			
			//Setting the fill values
			this.targetText = targetText;
			this.targetClipData = targetClipData;
			
			//Loading the data
			loadContacts();
		}
		
		@Override
		protected void onCleared() {
			compositeDisposable.clear();
		}
		
		void setActivityReference(NewMessage activity) {
			activityReference = new WeakReference<>(activity);
		}
		
		@SuppressLint("StaticFieldLeak")
		void loadContacts() {
			//Aborting if contacts cannot be used
			if(!MainApplication.canUseContacts(getApplication())) {
				contactState.setValue(contactStateNoAccess);
				return;
			}
			
			//Updating the state
			contactState.setValue(contactStateReady);
			
			//Loading the contacts
			ContactsTask.loadContacts(getApplication()).map(contactPart -> {
				//Trying to match a contact in the list
				int contactListSize = contactList.size();
				int matchingContactIndex = IntStream.range(0, contactListSize)
						.map(i -> contactListSize - i - 1)
						.filter(i -> contactList.get(i).getIdentifier() == contactPart.getID()).findAny().orElse(-1);
				
				if(matchingContactIndex == -1) {
					//Add a new contact
					contactList.add(new ContactInfo(contactPart.getID(), contactPart.getName(), new ArrayList<>(Collections.singleton(contactPart.getAddress()))));
					
					return new ContactListReactiveUpdate.Addition(contactListSize);
				} else {
					//Updating the contact
					contactList.get(matchingContactIndex).addAddress(contactPart.getAddress());
					
					return new ContactListReactiveUpdate.Change(matchingContactIndex);
				}
			}).subscribe(contactListSubject);
		}
		
		void confirmParticipants(List<String> participants, @Nullable ConnectionManager connectionManager) {
			//Setting the state
			loadingState.setValue(true);
			
			//Pair: ConversationInfo - the conversation to launch, Boolean - whether to emit an update to notify listeners that this conversation is newly created
			Single<Pair<ConversationInfo, Boolean>> conversationSingle;
			
			//Checking if if the current service handler is AirMessage bridge
			if(currentService.serviceHandler == ServiceHandler.appleBridge) {
				@ServiceType final String serviceType = currentService.serviceType;
				
				//Creates the unlinked conversation locally in the event that the server cannot be reached
				Single<Pair<ConversationInfo, Boolean>> errorConversationSingle = Single.fromCallable(
						() -> DatabaseManager.getInstance().addRetrieveClientCreatedConversationInfo(getApplication(), participants, ServiceHandler.appleBridge, serviceType)).
						subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread());
				
				if(connectionManager == null) {
					//Create an unlinked conversation locally
					conversationSingle = errorConversationSingle;
				} else {
					conversationSingle = connectionManager.createChat(participants.toArray(new String[0]), serviceType)
							.observeOn(Schedulers.single())
							//If the server returned a valid result, try to find a matching conversation in the database, or create a new one
							.map(chatGUID -> DatabaseManager.getInstance().addRetrieveMixedConversationInfoAMBridge(getApplication(), chatGUID, participants, serviceType))
							//Otherwise, make our own client-side conversation to be linked later
							.onErrorResumeWith(errorConversationSingle)
							.observeOn(AndroidSchedulers.mainThread());
				}
			} else {
				if(currentService.serviceType.equals(ServiceType.systemSMS)) {
					conversationSingle = Single.fromCallable(() -> {
						//Finding or creating a matching conversation in Android's message database
						long threadID = Telephony.Threads.getOrCreateThreadId(getApplication(), new HashSet<>(participants));
						
						//Finding or creating a matching conversation in AirMessage's database
						ConversationInfo conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(getApplication(), threadID, ServiceHandler.systemMessaging, ServiceType.systemSMS);
						
						//Creating a new conversation if no existing conversation was found
						if(conversationInfo == null) {
							//Creating the conversation
							int conversationColor = ConversationColorHelper.getDefaultConversationColor();
							List<MemberInfo> coloredMembers = ConversationColorHelper.getColoredMembers(participants, conversationColor);
							conversationInfo = new ConversationInfo(-1, null, threadID, ConversationState.ready, ServiceHandler.systemMessaging, ServiceType.systemSMS, conversationColor, coloredMembers, null);
							
							//Writing the conversation to disk
							DatabaseManager.getInstance().addConversationInfo(conversationInfo);
							ChatCreateAction chatCreateAction = DatabaseManager.getInstance().addConversationCreatedMessage(conversationInfo.getLocalID());
							
							//Setting the conversation preview
							conversationInfo.setMessagePreview(new ConversationPreview.ChatCreation(chatCreateAction.getDate()));
							
							return new Pair<>(conversationInfo, true);
						} else {
							//Completing with the conversation
							return new Pair<>(conversationInfo, false);
						}
					}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread());
				} else {
					throw new UnsupportedOperationException("Service type " + currentService.serviceType + " is not supported");
				}
			}
			
			compositeDisposable.add(
					conversationSingle.doOnSuccess(pair -> {
						//Notifying listeners of the new conversation
						if(pair.getSecond()) {
							ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationUpdate(Collections.singletonMap(pair.getFirst(), Collections.emptyList()), Collections.emptyList()));
						}
					}).subscribe(pair -> {
						//Launching the conversation
						launchConversation(getApplication(), pair.getFirst().getLocalID());
					}, error -> {
						//Enabling the UI
						loadingState.setValue(false);
						
						//Showing an error toast
						Toast.makeText(getApplication(), R.string.message_serverstatus_internalexception, Toast.LENGTH_SHORT).show();
					})
			);
		}
		
		/**
		 * Launches the messaging activity for specified conversation ID
		 * @param context The context to use
		 * @param conversationID The ID of the conversation
		 */
		private void launchConversation(Context context, long conversationID) {
			//Creating the intent
			Intent intent = new Intent(context, Messaging.class);
			intent.putExtra(Messaging.intentParamTargetID, conversationID);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			
			//Setting the fill data
			if(targetText != null) intent.putExtra(Messaging.intentParamDataText, targetText);
			if(targetClipData != null) {
				intent.putExtra(Messaging.intentParamDataFile, true);
				intent.setClipData(targetClipData);
			}
			
			//Launching the activity
			completionLaunchIntent.setValue(intent);
		}
	}
	
	/**
	 * Represents an update payload for the contacts list
	 */
	private static abstract class ContactListReactiveUpdate {
		/**
		 * Updates the adapter with this reactive change
		 */
		abstract void updateAdapter(RecyclerAdapter adapter);
		
		/**
		 * Represents the addition of a new item
		 */
		static class Addition extends ContactListReactiveUpdate {
			private final int position;
			
			public Addition(int position) {
				this.position = position;
			}
			
			@Override
			void updateAdapter(RecyclerAdapter adapter) {
				adapter.onItemAdded(position);
			}
		}
		
		/**
		 * Represents the update of an existing item
		 */
		static class Change extends ContactListReactiveUpdate {
			private final int position;
			
			public Change(int position) {
				this.position = position;
			}
			
			@Override
			void updateAdapter(RecyclerAdapter adapter) {
				adapter.onItemUpdated(position);
			}
		}
	}
	
	private static class MessageServiceDescription {
		//Display values
		@DrawableRes final int icon;
		@StringRes final int title;
		@ColorRes final int color;
		
		//Service values
		@ServiceHandler final int serviceHandler;
		@ServiceType final String serviceType;
		final boolean serviceSupportsEmail; //Whether this service supports email addresses
		
		MessageServiceDescription(@DrawableRes int icon, @StringRes int title, @ColorRes int color, @ServiceHandler int serviceHandler, @ServiceType String serviceType, boolean serviceSupportsEmail) {
			this.icon = icon;
			this.title = title;
			this.color = color;
			this.serviceHandler = serviceHandler;
			this.serviceType = serviceType;
			this.serviceSupportsEmail = serviceSupportsEmail;
		}
	}
	
	private static class ServiceChipData {
		private final MessageServiceDescription serviceDescription;
		private final ViewGroup view;
		private final TextView label;
		private final ImageView icon;
		
		public ServiceChipData(MessageServiceDescription serviceDescription, ViewGroup view, TextView label, ImageView icon) {
			this.serviceDescription = serviceDescription;
			this.view = view;
			this.label = label;
			this.icon = icon;
		}
		
		public MessageServiceDescription getServiceDescription() {
			return serviceDescription;
		}
		
		public ViewGroup getView() {
			return view;
		}
		
		public TextView getLabel() {
			return label;
		}
		
		public ImageView getIcon() {
			return icon;
		}
	}
}