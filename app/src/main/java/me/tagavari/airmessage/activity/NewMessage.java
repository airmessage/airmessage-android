package me.tagavari.airmessage.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

import me.tagavari.airmessage.BuildConfig;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginQNavigation;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.ChatCreationResponseManager;
import me.tagavari.airmessage.data.BitmapCacheHelper;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.MMSSMSHelper;

public class NewMessage extends AppCompatCompositeActivity {
	//Creating the constants
	private static final int menuIdentifierConfirmParticipants = 0;
	
	private static final int permissionRequestContacts = 0;
	
	private static final MessageServiceDescription[] availableServiceArray = BuildConfig.DEBUG ? new MessageServiceDescription[]{
			new MessageServiceDescription(R.drawable.message_push, R.string.title_imessage, false, -1, R.color.colorPrimary, ConversationInfo.serviceHandlerAMBridge, ConversationInfo.serviceTypeAppleMessage, true),
			new MessageServiceDescription(R.drawable.message_bridge, R.string.title_textmessageforwarding, false, -1, R.color.colorMessageTextMessageForwarding, ConversationInfo.serviceHandlerAMBridge, ConversationInfo.serviceTypeAppleTextMessageForwarding, false),
			new MessageServiceDescription(R.drawable.message_sms, R.string.title_textmessage, false, -1, R.color.colorMessageTextMessage, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, false),
			//new MessageServiceDescription(R.drawable.message_plus, R.string.title_rcs, false, -1, R.color.colorMessageRCS, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemRCS, false),
	} : new MessageServiceDescription[]{
			new MessageServiceDescription(R.drawable.message_push, R.string.title_imessage, false, -1, R.color.colorPrimary, ConversationInfo.serviceHandlerAMBridge, ConversationInfo.serviceTypeAppleMessage, true),
			//new MessageServiceDescription(R.drawable.message_bridge, R.string.title_textmessageforwarding, false, -1, R.color.colorMessageTextMessageForwarding, ConversationInfo.serviceHandlerAMBridge, ConversationInfo.serviceTypeAppleTextMessageForwarding, false),
			new MessageServiceDescription(R.drawable.message_sms, R.string.title_textmessage, false, -1, R.color.colorMessageTextMessage, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, false),
			//new MessageServiceDescription(R.drawable.message_plus, R.string.title_rcs, false, -1, R.color.colorMessageRCS, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemRCS, false),
	};
	
	//Creating the view model and plugin values
	private ActivityViewModel viewModel;
	
	private PluginQNavigation pluginQNavigation;
	
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
				if(Constants.validateAddress(cleanString)) {
					//Adding a chip
					addChip(new Chip(cleanString, Constants.normalizeAddress(cleanString)));
					
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
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Reading the launch arguments
		final String targetText;
		if(getIntent().hasExtra(Constants.intentParamDataText)) targetText = getIntent().getStringExtra(Constants.intentParamDataText);
		else targetText = null;
		final ClipData targetClipData;
		if(getIntent().getBooleanExtra(Constants.intentParamDataFile, false)) targetClipData = getIntent().getClipData();
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
		Constants.enforceContentWidthView(getResources(), contactListView);
		
		//Setting the list padding
		//pluginQNavigation.setViewForInsets(new View[]{contactListView});
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			ViewCompat.setOnApplyWindowInsetsListener(contactListView, new OnApplyWindowInsetsListener() {
				@Override
				public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
					//((ViewGroup.MarginLayoutParams) reyclerView.getLayoutParams()).bottomMargin = -insets.getSystemWindowInsetBottom();
					contactListView.setPadding(contactListView.getPaddingLeft(), contactListView.getPaddingTop(), contactListView.getPaddingRight(), insets.getSystemWindowInsetBottom());
					return insets.consumeSystemWindowInsets();
				}
			});
		}
		
		//Configuring the AMOLED theme
		if(Constants.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Setting the status bar color
		Constants.updateChromeOSStatusBar(this);
		
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
		viewModel.contactListLD.observe(this, value -> contactsListAdapter.onListUpdated());
		
		//Restoring the input bar
		restoreInputBar();
		
		//Configuring the list
		contactsListAdapter = new RecyclerAdapter(viewModel.contactList, contactListView);
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
		Constants.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(Constants.colorAMOLED);
	}
	
	void setDarkAMOLEDSamsung() {
		Constants.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(Constants.colorAMOLED);
		
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
				icon.setImageTintList(ColorStateList.valueOf(Constants.resolveColorAttr(this, android.R.attr.colorControlNormal)));
			}
			
			//Setting the click listener
			chipView.setOnClickListener(view -> {
				//Ignoring if this is the currently active service
				if(currentActiveServiceChip == serviceChipData) return;
				
				//Starting a transition
				TransitionManager.beginDelayedTransition(findViewById(R.id.list_service));
				
				//Disabling the old view
				currentActiveServiceChip.getLabel().setVisibility(View.GONE);
				currentActiveServiceChip.getLabel().setTextColor(ColorStateList.valueOf(Constants.resolveColorAttr(this, android.R.attr.textColorSecondary)));
				currentActiveServiceChip.getIcon().setImageTintList(ColorStateList.valueOf(Constants.resolveColorAttr(this, android.R.attr.colorControlNormal)));
				
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
				service.getView().animate().alpha(Constants.disabledAlpha);
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
	public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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
		viewModel.confirmParticipants(getRecipientAddressList());
	}
	
	public void onClickRequestContacts(View view) {
		//Requesting the permission
		Constants.requestPermission(this, new String[]{android.Manifest.permission.READ_CONTACTS}, permissionRequestContacts);
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
		ProgressBar progressBar  = findViewById(R.id.progressbar_content);
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
		String chipName = Constants.normalizeAddress(chip.display);
		for(Chip existingChips : viewModel.userChips) if(Constants.normalizeAddress(existingChips.getDisplay()).equals(chipName)) return;
		
		//Removing the hint from the recipient input if this is the first chip
		if(viewModel.userChips.isEmpty()) recipientInput.setHint("");
		
		//Adding the chip to the list
		viewModel.userChips.add(chip);
		
		//Adding the view
		recipientListGroup.addView(chip.getView(), viewModel.userChips.size() - 1);
		
		//Setting the confirm button as visible
		confirmMenuItem.setVisible(true);
		
		//Checking if the service selector is available, and a phone number is being added
		if(serviceSelectorAvailable && Constants.validateEmail(chipName)) {
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
		if(serviceSelectorAvailable && Constants.validateEmail(chip.display)) {
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
				
				//Setting the default information
				TextView labelView = popupView.findViewById(R.id.label_member);
				labelView.setText(display);
				((ImageView) popupView.findViewById(R.id.profile_default)).setColorFilter(getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
				
				//Filling in the information
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(NewMessage.this, display, new UserCacheHelper.UserFetchResult() {
					@Override
					public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
						//Returning if the user info is invalid
						if(userInfo == null) return;
						
						//Updating the text
						labelView.setText(userInfo.getContactName());
						TextView addressView = popupView.findViewById(R.id.label_address);
						addressView.setText(display);
						addressView.setVisibility(View.VISIBLE);
						
					}
				});
				MainApplication.getInstance().getUserCacheHelper().assignUserInfo(getApplicationContext(), display, labelView);
				MainApplication.getInstance().getBitmapCacheHelper().assignContactImage(getApplicationContext(), display, (View) popupView.findViewById(R.id.profile_image));
				
				//Creating the window
				final PopupWindow popupWindow = new PopupWindow(popupView, Constants.dpToPx(300), Constants.dpToPx(56));
				
				//popupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getServiceColor(R.color.colorForegroundLight, null)));
				popupWindow.setOutsideTouchable(true);
				popupWindow.setElevation(Constants.dpToPx(2));
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
		private ArrayList<ContactInfo> originalItems;
		private final ArrayList<ContactInfo> filteredItems = new ArrayList<>();
		
		//Creating the recycler values
		private RecyclerView recyclerView;
		
		//Creating the task values
		private ContactsSearchTask contactsSearchTask = null;
		
		//Creating the other values
		/* private final boolean serviceSelectorHeaderEnabled;
		private boolean serviceSelectorHeaderVisible; */
		private boolean directAddHeaderVisible = false;
		private String lastFilterText = "";
		//Filter out phone numbers, for the sake of non-iMessage services
		private boolean filterPhoneOnly = false;
		
		RecyclerAdapter(ArrayList<ContactInfo> items, RecyclerView recyclerView) {
			//Setting the items
			originalItems = items;
			filteredItems.addAll(items);
			
			//Adding the recycler values
			this.recyclerView = recyclerView;
			
			//Setting the prefs
			//this.serviceSelectorHeaderEnabled = serviceSelectorHeaderEnabled;
			//serviceSelectorHeaderVisible = serviceSelectorHeaderEnabled;
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
						addChip(new Chip(lastFilterText, Constants.normalizeAddress(cleanString)));
						
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
					itemVH.contactName.setText(contactInfo.name);
					
					int addressCount = contactInfo.getAddresses().size();
					String firstAddress = contactInfo.getAddresses().get(0).getAddress();
					if(addressCount == 1) itemVH.contactAddress.setText(firstAddress);
					else itemVH.contactAddress.setText(getResources().getQuantityString(R.plurals.message_multipledestinations, addressCount, firstAddress, addressCount - 1));
					
					//Showing / hiding the section header
					boolean showHeader;
					//if(!lastFilterText.isEmpty()) showHeader = false;
					if(position > getHeaderCount()) {
						ContactInfo contactInfoAbove = filteredItems.get(position - getHeaderCount() - 1);
						showHeader = contactInfoAbove == null || !stringsHeaderEqual(contactInfo.name, contactInfoAbove.name);
					} else showHeader = true;
					
					if(showHeader) {
						itemVH.header.setVisibility(View.VISIBLE);
						itemVH.headerLabel.setText(Character.toString(getNameHeader(contactInfo.name)));
					} else itemVH.header.setVisibility(View.GONE);
					
					//Resetting the image view
					itemVH.profileDefault.setVisibility(View.VISIBLE);
					itemVH.profileDefault.setColorFilter(getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
					itemVH.profileImage.setImageBitmap(null);
					
					//Assigning the contact's image
					MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(getApplicationContext(), Long.toString(contactInfo.identifier), contactInfo.identifier, new BitmapCacheHelper.ImageDecodeResult() {
						@Override
						public void onImageMeasured(int width, int height) {
						
						}
						
						@Override
						public void onImageDecoded(Bitmap result, boolean wasTasked) {
							//Returning if the result is invalid
							if(result == null) return;
							
							//Returning if the item doesn't exist anymore
							if(!filteredItems.contains(contactInfo)) return;
							
							//Getting the view holder
							ItemViewHolder currentViewHolder = wasTasked ? (ItemViewHolder) recyclerView.findViewHolderForAdapterPosition(getIndexOfItem(contactInfo)) : itemVH;
							if(currentViewHolder == null) return;
							
							//Hiding the default view
							currentViewHolder.profileDefault.setVisibility(View.INVISIBLE);
							
							//Setting the bitmap
							currentViewHolder.profileImage.setImageBitmap(result);
							
							//Fading in the view
							if(wasTasked) {
								currentViewHolder.profileImage.setAlpha(0F);
								currentViewHolder.profileImage.animate().alpha(1).setDuration(300).start();
							}
						}
					});
					
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
									.setItems(contactInfo.getAddressDisplayArray(getResources()), ((dialogInterface, index) -> {
										//Adding the selected chip
										AddressInfo address = contactInfo.getAddresses().get(index);
										addChip(new Chip(address.getAddress(), address.getNormalizedAddress()));
										
										//Clearing the text
										recipientInput.setText("");
									}))
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
										boolean enabled = Constants.validatePhoneNumber(addressInfo.getNormalizedAddress());
										
										//Updating the child's status
										if(!enabled) {
											child.setEnabled(false);
											child.setAlpha(Constants.disabledAlpha);
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
			if(position < getHeaderCount()) {
				return TYPE_HEADER_DIRECT;
				/* if(position == 0) {
					if(serviceSelectorHeaderVisible) return TYPE_HEADER_SERVICE;
					else if(directAddHeaderVisible) return TYPE_HEADER_DIRECT;
					else throw new IllegalStateException("Unknown header position " + position);
				}
				else if(position == 1) return TYPE_HEADER_DIRECT; //The direct send header always comes second
				else throw new IllegalStateException("Unknown header position " + position); */
			}
			
			//Returning the item
			return TYPE_ITEM;
		}
		
		private int getHeaderCount() {
			int offset = 0;
			if(directAddHeaderVisible) offset++;
			//if(serviceSelectorHeaderEnabled && serviceSelectorHeaderVisible) offset++;
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
		
		void updateOriginalItems(ArrayList<ContactInfo> list) {
			//Setting the original items
			originalItems = list;
			
			//Re-filtering the list
			filterList(lastFilterText);
		}
		
		void filterList(String filter) {
			//Setting the last filter text
			lastFilterText = filter;
			
			//Cleaning the filter
			filter = filter.trim();
			
			//Cancelling the current task
			if(contactsSearchTask != null) contactsSearchTask.cancel(true);
			
			boolean filterEmpty = filter.isEmpty();
			
			//Showing the service header if there is no search query
			//serviceSelectorHeaderVisible = filterEmpty;
			
			//Removing the header view if there is no filter
			if(filterEmpty) directAddHeaderVisible = false;
			
			//Checking if the filter is empty
			if(filterEmpty && !filterPhoneOnly) {
				//Adding all of the items
				filteredItems.clear();
				filteredItems.addAll(originalItems);
				
				//Notifying the adapter
				notifyDataSetChanged();
				
				//Invalidating the task
				contactsSearchTask = null;
			} else {
				//Starting the task
				contactsSearchTask = new ContactsSearchTask(new ArrayList<>(originalItems), filter, filterPhoneOnly, new ContactsSearchTaskListener(this));
				contactsSearchTask.execute();
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
		
		void handleFilterResult(String query, List<ContactInfo> newFilteredItems, boolean queryValidAddress) {
			//Returning if the queries no longer match
			if(!lastFilterText.equals(query)) return;
			
			//Updating the list
			filteredItems.clear();
			filteredItems.addAll(newFilteredItems);
			directAddHeaderVisible = queryValidAddress;
			
			//Notifying the adapter
			notifyDataSetChanged();
		}
	}
	
	private static class ContactsSearchTask extends AsyncTask<Void, Void, Constants.Tuple2<List<ContactInfo>, Boolean>> {
		private final List<ContactInfo> contactList;
		private final String query;
		private final boolean filterPhoneOnly;
		private final Constants.TriConsumer<String, List<ContactInfo>, Boolean> resultListener;
		
		ContactsSearchTask(List<ContactInfo> contactList, String query, boolean filterPhoneOnly, Constants.TriConsumer<String, List<ContactInfo>, Boolean> resultListener) {
			this.contactList = contactList;
			this.query = query;
			this.filterPhoneOnly = filterPhoneOnly;
			this.resultListener = resultListener;
		}
		
		//Returns list of filtered contacts and the validity of the filter query (as a contact address, to decide whether or not to show the "new address" header)
		@Override
		protected Constants.Tuple2<List<ContactInfo>, Boolean> doInBackground(Void... voids) {
			//Returning if the request has been cancelled
			if(isCancelled()) return null;
			
			//Normalizing the filter
			String normalizedFilter = Constants.normalizeAddress(query);
			
			String strippedFilter = null;
			if(!query.matches("[^\\+\\(\\)\\-\\d]*")) { //Checking for any non-phone number characters
				strippedFilter = Constants.stripPhoneNumber(normalizedFilter);
				if(!strippedFilter.startsWith("1")) strippedFilter = "1" + strippedFilter; //All normalized items start with "1"
			}
			
			//Creating the list
			List<ContactInfo> filteredItems = new ArrayList<>();
			
			//Filtering the list
			contactLoop:
			for(ContactInfo contactInfo : contactList) {
				//Returning if the request has been cancelled
				if(isCancelled()) return null;
				
				//Filtering out contacts without phone numbers (if required)
				if(filterPhoneOnly) {
					boolean addressFound = false;
					for(AddressInfo address : contactInfo.getAddresses()) {
						if(Constants.validatePhoneNumber(address.getNormalizedAddress())) {
							addressFound = true;
							break;
						}
					}
					if(!addressFound) continue;
				}
				
				if(query.isEmpty()) {
					//Adding the item, as there is no filter
					filteredItems.add(contactInfo);
				} else {
					//Adding the item if the name matches the filter
					if(contactInfo.name != null && contactInfo.name.toLowerCase().contains(query.toLowerCase())) {
						filteredItems.add(contactInfo);
					} else {
						//Adding the item if any of the contact's addresses match the filter
						for(AddressInfo address : contactInfo.getAddresses()) {
							if(address.getNormalizedAddress().startsWith(normalizedFilter)) {
								filteredItems.add(contactInfo);
								break;
							} else {
								//Checking if the address is a phone number
								if(strippedFilter != null && Constants.validatePhoneNumber(address.getNormalizedAddress()) && Constants.stripPhoneNumber(address.getNormalizedAddress()).startsWith(strippedFilter)) {
									filteredItems.add(contactInfo);
									break;
								}
							}
						}
					}
				}
			}
			
			/* //Checking if the filter text is a valid label
			if(Constants.validateAddress(query)) {
				//Showing the header view
				setHeaderState(true);
			} else {
				//Hiding the header view
				setHeaderState(false);
			} */
			
			//Returning if the request has been cancelled
			if(isCancelled()) return null;
			
			//Checking the validity of the query as a contact address
			boolean queryAddressValid = !query.isEmpty() && Constants.validateAddress(query);
			
			//Returning the data
			return new Constants.Tuple2<>(filteredItems, queryAddressValid);
		}
		
		@Override
		protected void onPostExecute(Constants.Tuple2<List<ContactInfo>, Boolean> result) {
			//Ignoring cancelled requests
			if(result == null) return;
			
			//Telling the listener
			resultListener.accept(query, result.item1, result.item2);
		}
	}
	
	private static class ContactsSearchTaskListener implements Constants.TriConsumer<String, List<ContactInfo>, Boolean> {
		private final WeakReference<RecyclerAdapter> adapterReference;
		
		ContactsSearchTaskListener(RecyclerAdapter adapter) {
			adapterReference = new WeakReference<>(adapter);
		}
		
		@Override
		public void accept(String query, List<ContactInfo> filteredItems, Boolean queryValidAddress) {
			//Getting the adapter
			RecyclerAdapter adapter = adapterReference.get();
			if(adapter != null) adapter.handleFilterResult(query, filteredItems, queryValidAddress);
		}
	}
	
	private static class ContactInfo {
		private final long identifier;
		private final String name;
		private final ArrayList<AddressInfo> addresses;
		
		public ContactInfo(long identifier, String name, ArrayList<AddressInfo> addresses) {
			this.identifier = identifier;
			this.name = name;
			this.addresses = addresses;
		}
		
		public void addAddress(AddressInfo address) {
			addresses.add(address);
		}
		
		public List<AddressInfo> getAddresses() {
			return addresses;
		}
		
		public String[] getAddressDisplayArray(Resources resources) {
			String[] displayArray = new String[addresses.size()];
			for(ListIterator<AddressInfo> iterator = addresses.listIterator(); iterator.hasNext();) {
				displayArray[iterator.nextIndex()] = iterator.next().getDisplay(resources);
			}
			return displayArray;
		}
		
		@Override
		protected ContactInfo clone() {
			return new ContactInfo(identifier, name, new ArrayList<>(addresses));
		}
	}
	
	private static class AddressInfo {
		private final String address, normalizedAddress, addressLabel;
		
		public AddressInfo(String address, String addressLabel) {
			this.address = address;
			this.normalizedAddress = Constants.normalizeAddress(address);
			this.addressLabel = addressLabel;
		}
		
		public String getAddress() {
			return address;
		}
		
		public String getNormalizedAddress() {
			return normalizedAddress;
		}
		
		public String getAddressLabel() {
			return addressLabel;
		}
		
		public String getDisplay(Resources resources) {
			if(addressLabel == null) return address;
			else return resources.getString(R.string.label_addressdetails, addressLabel, address);
		}
	}
	
	public static class ActivityViewModel extends AndroidViewModel {
		//Creating the reference values
		static final int contactStateIdle = 0;
		static final int contactStateReady = 1;
		static final int contactStateNoAccess = 2;
		static final int contactStateFailed = 3;
		
		//Creating the state values
		final MutableLiveData<Integer> contactState = new MutableLiveData<>();
		final MutableLiveData<Boolean> loadingState = new MutableLiveData<>();
		boolean recipientInputAlphabetical = true;
		
		//Creating the input values
		private ArrayList<Chip> userChips = new ArrayList<>();
		private int participantsEmailCount = 0;
		
		//Creating the fill values
		private final String targetText;
		private final ClipData targetClipData;
		
		//Creating the other values
		MessageServiceDescription currentService = availableServiceArray[0];
		final MutableLiveData<Object> contactListLD = new MutableLiveData<>();
		final ArrayList<ContactInfo> contactList = new ArrayList<>();
		
		private WeakReference<NewMessage> activityReference = null;
		
		public ActivityViewModel(@NonNull Application application, String targetText, ClipData targetClipData) {
			super(application);
			
			//Setting the fill values
			this.targetText = targetText;
			this.targetClipData = targetClipData;
			
			//Loading the data
			loadContacts();
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
			new AsyncTask<Void, ContactInfo, ArrayList<ContactInfo>>() {
				@Override
				protected ArrayList<ContactInfo> doInBackground(Void... parameters) {
					//Getting the content resolver
					ContentResolver contentResolver = getApplication().getContentResolver();
					
					//Querying the database
					Cursor cursor = contentResolver.query(
							ContactsContract.Data.CONTENT_URI,
							new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Data.MIMETYPE, ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data.DATA1, ContactsContract.Data.DATA2, ContactsContract.Data.DATA3},
							ContactsContract.Data.MIMETYPE + " = ? OR (" + ContactsContract.Data.HAS_PHONE_NUMBER + "!= 0 AND " + ContactsContract.Data.MIMETYPE + " = ?)",
							new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
							ContactsContract.Data.DISPLAY_NAME + " ASC");
					
					//Returning null if the cursor is invalid
					if(cursor == null) return null;
					
					//Reading the data
					ArrayList<ContactInfo> contactList = new ArrayList<>();
					int indexContactID = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID);
					int indexMimeType = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE);
					int indexDisplayName = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME);
					int indexAddress = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1); //The address itself (email or phone number)
					int indexAddressType = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2); //The label ID for this address
					int indexAddressLabel = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3); //The custom user-assigned label for this address
					
					userIterator:
					while(cursor.moveToNext()) {
						//Retrieving and validating the entry's label
						String address = cursor.getString(indexAddress);
						if(address == null || address.isEmpty()) continue;
						
						//Getting the general info
						long contactID = cursor.getLong(indexContactID);
						String contactName = cursor.getString(indexDisplayName);
						if(contactName != null && contactName.isEmpty()) contactName = null;
						String addressLabel = null;
						if(!cursor.isNull(indexAddressType)) {
							int addressType = cursor.getInt(indexAddressType);
							if(addressType == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) addressLabel = cursor.getString(indexAddressLabel);
							else addressLabel = MMSSMSHelper.getAddressLabel(getApplication().getResources(), cursor.getString(indexMimeType), addressType);
						}
						AddressInfo addressInfo = new AddressInfo(address, addressLabel);
						
						//Checking if there is a user with a matching contact ID
						for(ContactInfo contactInfo : contactList) {
							if(contactInfo.identifier == contactID) {
								for(AddressInfo allAddressInfo : contactInfo.getAddresses()) {
									if(allAddressInfo.getNormalizedAddress().equals(addressInfo.getNormalizedAddress())) continue userIterator;
								}
								contactInfo.addAddress(addressInfo);
								continue userIterator;
							}
						}
						
						//Adding the user to the list
						ArrayList<AddressInfo> contactAddresses = new ArrayList<>();
						contactAddresses.add(addressInfo);
						ContactInfo contactInfo = new ContactInfo(contactID, contactName, contactAddresses);
						contactList.add(contactInfo);
						
						//Calling the progress update
						publishProgress(contactInfo.clone()); //Cloning for thread safety (alternate addresses won't get updated, though)
					}
					
					//Closing the cursor
					cursor.close();
					
					//Returning the contact list
					return contactList;
				}
				
				@Override
				protected void onProgressUpdate(ContactInfo... newContacts) {
					//Adding the contacts
					Collections.addAll(contactList, newContacts);
					contactListLD.setValue(null);
				}
				
				@Override
				protected void onPostExecute(ArrayList<ContactInfo> contacts) {
					//Clearing the contacts list
					contactList.clear();
					
					//Checking if the result is invalid
					if(contacts == null) {
						//Updating the state
						contactState.setValue(contactStateFailed);
					} else {
						//Setting the contacts
						contactList.addAll(contacts);
					}
					
					//Updating the contacts list
					contactListLD.setValue(null);
				}
			}.execute();
		}
		
		void confirmParticipants(ArrayList<String> participants) {
			//Setting the state
			loadingState.setValue(true);
			
			//Checking if if the current service handler is AirMessage bridge
			if(currentService.serviceHandler == ConversationInfo.serviceHandlerAMBridge) {
				//Getting the connection manager
				ConnectionManager connectionManager = ConnectionService.getConnectionManager();
				
				//Creating the response listener
				ChatCreationResponseListener listener = new ChatCreationResponseListener(participants, currentService.serviceName, connectionManager);
				
				//Checking if the connection manager is available
				if(connectionManager == null) {
					//Assuming a fail
					listener.onFail();
				} else {
					//Asking the server to create a chat
					connectionManager.createChat(participants.toArray(new String[0]), currentService.serviceName, listener);
				}
			} else {
				if(currentService.serviceName.equals(ConversationInfo.serviceTypeSystemMMSSMS)) {
					//Finding or creating and launching a new text message conversation
					new FindCreateMMSSMSConversation().execute(participants.toArray(new String[0]));
				}
			}
		}
		
		private class ChatCreationResponseListener extends ChatCreationResponseManager {
			private final ArrayList<String> participants;
			private final String serviceName;
			
			public ChatCreationResponseListener(ArrayList<String> participants, String serviceName, ConnectionManager connectionManager) {
				super(new ConnectionManager.ChatCreationDeregistrationListener(connectionManager));
				this.participants = participants;
				this.serviceName = serviceName;
			}
			
			@SuppressLint("StaticFieldLeak")
			@Override
			public void onSuccess(String chatGUID) {
				//Checking if the conversations are available in memory
				List<ConversationInfo> conversations = ConversationUtils.getConversations();
				if(conversations != null) {
					//Scanning the loaded conversations for a matching one
					for(ConversationInfo conversationInfo : conversations) {
						//Skipping the conversation if its members do not match
						if(!chatGUID.equals(conversationInfo.getGuid())) continue;
						
						//Launching the activity
						launchConversation(conversationInfo.getLocalID());
						
						//Returning
						return;
					}
				}
				
				//Creating the conversation
				new AsyncTask<Void, Void, ConversationInfo>() {
					@Override
					protected ConversationInfo doInBackground(Void... parameters) {
						/* //Cloning and normalizing the members' addresses
						List<String> normalizedMembers = new ArrayList<>(participants);
						for(ListIterator<String> iterator = normalizedMembers.listIterator(); iterator.hasNext();) iterator.set(Constants.normalizeAddress(iterator.next())); */
						
						//Adding the conversation
						return DatabaseManager.getInstance().addRetrieveMixedConversationInfoAMBridge(getApplication(), chatGUID, participants.toArray(new String[0]), serviceName);
					}
					
					@Override
					protected void onPostExecute(ConversationInfo result) {
						handleConversationConfirmation(result);
					}
				}.execute();
			}
			
			@SuppressLint("StaticFieldLeak")
			@Override
			public void onFail() {
				//Checking if the conversations are available in memory
				List<ConversationInfo> conversations = ConversationUtils.getConversations();
				if(conversations != null) {
					//Scanning the loaded conversations for a matching one
					for(ConversationInfo conversationInfo : conversations) {
						//Getting the conversation members
						//List<String> members = Constants.normalizeAddresses(conversationInfo.getConversationMembersAsCollection());
						List<String> members = conversationInfo.getConversationMembersAsCollection();
						
						//Skipping the conversation if its members do not match
						if(participants.size() != members.size() || !participants.containsAll(members)) continue;
						
						//Launching the activity
						launchConversation(conversationInfo.getLocalID());
						
						//Returning
						return;
					}
				}
				
				new AsyncTask<Void, Void, ConversationInfo>() {
					@Override
					protected ConversationInfo doInBackground(Void... parameters) {
						/* //Cloning and normalizing the members' addresses
						List<String> normalizedMembers = new ArrayList<>(participants);
						for(ListIterator<String> iterator = normalizedMembers.listIterator(); iterator.hasNext();) iterator.set(Constants.normalizeAddress(iterator.next())); */
						
						//Adding the conversation
						return DatabaseManager.getInstance().addRetrieveClientCreatedConversationInfo(getApplication(), participants, ConversationInfo.serviceHandlerAMBridge, serviceName);
					}
					
					@Override
					protected void onPostExecute(ConversationInfo result) {
						handleConversationConfirmation(result);
					}
				}.execute();
			}
		}
		
		@SuppressLint("StaticFieldLeak")
		private class FindCreateMMSSMSConversation extends AsyncTask<String, Void, ConversationInfo> {
			@Override
			protected ConversationInfo doInBackground(String... participants) {
				//Finding or creating a matching conversation in Android's message database
				long threadID = Telephony.Threads.getOrCreateThreadId(getApplication(), new HashSet<>(Arrays.asList(participants)));
				
				//Finding or creating a matching conversation in AirMessage's database
				ConversationInfo conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(getApplication(), threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS);
				
				//Creating a new conversation if no existing conversation was found
				if(conversationInfo == null) {
					//Creating the conversation
					int conversationColor = ConversationInfo.getDefaultConversationColor(threadID);
					conversationInfo = new ConversationInfo(-1, null, ConversationInfo.ConversationState.READY, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, new ArrayList<>(), null, 0, conversationColor, null, new ArrayList<>(), -1);
					conversationInfo.setExternalID(threadID);
					conversationInfo.setConversationMembersCreateColors(participants);
					
					//Writing the conversation to disk
					boolean result = DatabaseManager.getInstance().addReadyConversationInfo(conversationInfo);
					if(!result) return null;
					DatabaseManager.getInstance().addConversationCreatedMessage(conversationInfo, getApplication());
				}
				
				//Returning the conversation
				return conversationInfo;
			}
			
			@Override
			protected void onPostExecute(ConversationInfo result) {
				handleConversationConfirmation(result);
			}
		}
		
		private void handleConversationConfirmation(ConversationInfo result) {
			//Checking if the result is a failure
			if(result == null) {
				//Enabling the UI
				loadingState.setValue(false);
				
				//Showing an error toast
				Toast.makeText(getApplication(), R.string.message_serverstatus_internalexception, Toast.LENGTH_SHORT).show();
			} else {
				//Checking if the conversations exist
				ArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
				if(conversations != null && ConversationUtils.findConversationInfo(result.getLocalID()) == null) {
					//Adding the conversation in memory
					ConversationUtils.addConversation(result);
					
					//Updating the shortcut
					List<ConversationInfo> shortcutUpdateList = Collections.singletonList(result);
					ConversationUtils.updateShortcuts(getApplication(), shortcutUpdateList);
					ConversationUtils.enableShortcuts(getApplication(), shortcutUpdateList);
					
					//Updating the conversation activity list
					LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
				}
				
				//Launching the activity
				launchConversation(result.getLocalID());
			}
		}
		
		private boolean launchConversation(long identifier) {
			//Getting the activity
			Activity activity = activityReference.get();
			if(activity == null) return false;
			
			//Creating the intent
			Intent intent = new Intent(activity, Messaging.class);
			intent.putExtra(Constants.intentParamTargetID, identifier);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			
			//Setting the fill data
			if(targetText != null) intent.putExtra(Constants.intentParamDataText, targetText);
			if(targetClipData != null) {
				intent.putExtra(Constants.intentParamDataFile, true);
				intent.setClipData(targetClipData);
			}
			
			//Launching the activity
			activity.startActivity(intent);
			
			//Finishing this activity
			activity.finish();
			
			return true;
		}
	}
	
	private static class MessageServiceDescription {
		//Display values
		@DrawableRes
		private final int icon;
		@StringRes
		private final int title;
		private final boolean subtitleAvailable;
		@StringRes
		private final int subtitle;
		@ColorRes
		private final int color;
		
		//Service values
		private final int serviceHandler;
		private final String serviceName;
		private final boolean serviceSupportsEmail; //Whether or not this service supports email addresses
		
		MessageServiceDescription(int icon, int title, boolean subtitleAvailable, int subtitle, int color, int serviceHandler, String serviceName, boolean serviceSupportsEmail) {
			this.icon = icon;
			this.title = title;
			this.subtitleAvailable = subtitleAvailable;
			this.subtitle = subtitle;
			this.color = color;
			this.serviceHandler = serviceHandler;
			this.serviceName = serviceName;
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