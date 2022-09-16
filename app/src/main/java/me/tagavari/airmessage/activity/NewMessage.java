package me.tagavari.airmessage.activity;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import kotlin.Pair;
import me.tagavari.airmessage.BuildConfig;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.component.ContactChip;
import me.tagavari.airmessage.component.ContactListReactiveUpdate;
import me.tagavari.airmessage.component.ContactsRecyclerAdapter;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginConnectionService;
import me.tagavari.airmessage.compositeplugin.PluginRXDisposable;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.ConversationColorHelper;
import me.tagavari.airmessage.helper.WindowHelper;
import me.tagavari.airmessage.messaging.ChatCreateAction;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.task.ContactsTask;
import me.tagavari.airmessage.util.ContactInfo;

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
	private ContactsRecyclerAdapter contactsListAdapter;
	
	private ViewGroup groupMessagePermission;
	private ViewGroup groupMessageError;
	//private ListView contactListView;
	//private ListAdapter contactsListAdapter;
	
	private Map<String, View> recipientListGroupMap = new HashMap<>();
	
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
					addChip(new ContactChip(cleanString, AddressHelper.normalizeAddress(cleanString)));
					
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
		
		//Enabling edge-to-edge rendering
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		
		ViewCompat.setOnApplyWindowInsetsListener(contactListView, (view, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
 			view.setPadding(insets.left, 0, insets.right, insets.bottom);
			return WindowInsetsCompat.CONSUMED;
		});
		
		//Enforcing the maximum content width
		WindowHelper.enforceContentWidthView(getResources(), contactListView);
		
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
		contactsListAdapter = new ContactsRecyclerAdapter(this, viewModel.contactList, (address) -> {
			//Adding the chip
			addChip(new ContactChip(address, AddressHelper.normalizeAddress(address)));

			//Clearing the text
			recipientInput.setText("");
		});
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
			for(ContactChip chip : viewModel.userChips) {
				addChip(chip);
				View chipView = chip.getView(this, this::removeChip);
				recipientListGroupMap.put(chip.getAddress(), chipView);
				recipientListGroup.addView(chipView, chipIndex);
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
			Chip chipView = new Chip(this);
			
			//Creating the service chip data
			ServiceChipData serviceChipData = new ServiceChipData(serviceDescription, chipView);
			serviceChipDataArray[i] = serviceChipData;
			
			//Setting the label text and icon
			chipView.setText(serviceDescription.title);
			chipView.setCheckable(true);
			chipView.setCheckedIconResource(R.drawable.check);
			chipView.setCheckedIconVisible(true);
			
			//Checking if this is the currently selected service
			if(serviceDescription == viewModel.currentService) {
				//Set the chip as selected
				chipView.setChecked(true);
				
				//Setting the view as the current active view
				currentActiveServiceChip = serviceChipData;
			} else {
				//Set the chip as non-selected
				chipView.setChecked(false);
			}
			
			//Setting the click listener
			chipView.setOnClickListener(view -> {
				//Ignoring if this is the currently active service
				if(currentActiveServiceChip == serviceChipData) return;
				
				//Disabling the old view
				currentActiveServiceChip.getView().setChecked(false);
				
				//Enabling the new view
				chipView.setCheckable(true);
				
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
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
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
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
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

	/**
	 * Returns a sorted list of addresses from the selected chips
	 */
	private List<String> getRecipientAddressList() {
		return viewModel.userChips.stream()
				.map(ContactChip::getAddress)
				.sorted()
				.collect(Collectors.toList());
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
	
	private void addChip(ContactChip chip) {
		//Making sure we aren't adding the same chip twice
		if(viewModel.userChips.stream()
				.anyMatch(existingChip -> existingChip.getAddress().equals(chip.getAddress()))) {
			return;
		}

		//Removing the hint from the recipient input if this is the first chip
		if(viewModel.userChips.isEmpty()) {
			recipientInput.setHint("");
		}
		
		//Adding the chip to the list
		viewModel.userChips.add(chip);
		
		//Adding the view
		View chipView = chip.getView(this, this::removeChip);
		recipientListGroupMap.put(chip.getAddress(), chipView);
		recipientListGroup.addView(chipView, viewModel.userChips.size() - 1);
		
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
	
	private void removeChip(ContactChip chip) {
		//Ignore if we're loading
		if(viewModel.loadingState.getValue() == Boolean.TRUE) {
			return;
		}

		//Removing the chip from the list
		viewModel.userChips.remove(chip);
		
		//Removing the view
		View chipView = recipientListGroupMap.get(chip.getAddress());
		if(chipView != null) {
			recipientListGroup.removeView(chipView);
			recipientListGroupMap.remove(chip.getAddress());
		}
		
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
		private final List<ContactChip> userChips = new ArrayList<>();
		private int participantsEmailCount = 0;
		
		//Creating the fill values
		private final String targetText;
		private final ClipData targetClipData;
		
		//Creating the other values
		MessageServiceDescription currentService = availableServiceArray[0];
		final List<ContactInfo> contactList = new ArrayList<>();
		
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
						.filter(i -> contactList.get(i).getContactID() == contactPart.getID()).findAny().orElse(-1);
				
				if(matchingContactIndex == -1) {
					//Add a new contact
					contactList.add(new ContactInfo(contactPart.getID(), contactPart.getName(), contactPart.getThumbnailURI(), new ArrayList<>(Collections.singleton(contactPart.getAddress()))));
					
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
		private final Chip view;
		
		public ServiceChipData(MessageServiceDescription serviceDescription, Chip view) {
			this.serviceDescription = serviceDescription;
			this.view = view;
		}
		
		public MessageServiceDescription getServiceDescription() {
			return serviceDescription;
		}
		
		public Chip getView() {
			return view;
		}
	}
}