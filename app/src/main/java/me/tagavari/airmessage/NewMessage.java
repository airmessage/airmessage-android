package me.tagavari.airmessage;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class NewMessage extends AppCompatActivity {
	private static final int menuIdentifierConfirmParticipants = 0;
	
	//Creating the view values
	private ViewGroup recipientViewGroup;
	
	private MenuItem confirmMenuItem;
	private EditText recipientInput;
	private RecyclerView contactsList;
	private RecyclerAdapter contactsListAdapter;
	//private ListView contactsList;
	//private ListAdapter contactsListAdapter;
	
	//Creating the listener values
	private final TextWatcher recipientInputTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
		}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Trimming the string
			String trimmedString = s.toString().trim();
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
			contactsListAdapter.filterList(trimmedString);
		}
		
		@Override
		public void afterTextChanged(Editable s) {
		
		}
	};
	private final View.OnKeyListener recipientInputOnKeyListener = new View.OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			//Returning if the event is not a key down
			if(event.getAction() != KeyEvent.ACTION_DOWN) return false;
			
			//Checking if the key is the delete key
			if(keyCode == KeyEvent.KEYCODE_DEL) {
				//Checking if the cursor is at the start and there are chips
				if(recipientInput.getSelectionStart() == 0 && recipientInput.getSelectionEnd() == 0 && !retainedFragment.userChips.isEmpty()) {
					//Removing a chip
					removeChip(retainedFragment.userChips.get(retainedFragment.userChips.size() - 1));
					
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
					addChip(new Chip(cleanString));
					
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
	
	//Creating the retained fragment values
	private static final String retainedFragmentTag = RetainedFragment.class.getName();
	private RetainedFragment retainedFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_newmessage);
		
		//Enabling the up button
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Getting the views
		recipientViewGroup = findViewById(R.id.viewgroup_recipients);
		recipientInput = findViewById(R.id.recipients_input);
		contactsList = findViewById(R.id.list_contacts);
		
		//Setting the name change listener
		recipientInput.addTextChangedListener(recipientInputTextWatcher);
		recipientInput.setOnKeyListener(recipientInputOnKeyListener);
		recipientInput.setOnEditorActionListener(recipientInputOnActionListener);
		
		//Preparing the retained fragment
		prepareRetainedFragment();
		
		//Restoring the state
		restoreInputBar();
		
		//Checking if the state is idle
		if(retainedFragment.contactState == RetainedFragment.contactStateIdle) {
			//Checking if contacts can be used
			if(MainApplication.canUseContacts(this)) {
				//Loading the contacts
				retainedFragment.loadContacts(getApplicationContext());
			} else {
				//Setting the state to no access
				retainedFragment.updateState(RetainedFragment.contactStateNoAccess);
				updateContactState((byte) -1, RetainedFragment.contactStateNoAccess);
			}
		} else {
			//Updating the state
			updateContactState((byte) -1, retainedFragment.contactState);
		}
		
		//Configuring the input
		//recipientInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		//recipientInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
		
		//Configuring the list
		contactsListAdapter = new RecyclerAdapter(retainedFragment.contactList, contactsList);
		contactsList.setAdapter(contactsListAdapter);
	}
	
	private void prepareRetainedFragment() {
		//Getting the retained fragment
		FragmentManager fragmentManager = getFragmentManager();
		retainedFragment = (RetainedFragment) fragmentManager.findFragmentByTag(retainedFragmentTag);
		
		//Checking if the fragment is invalid
		if(retainedFragment == null) { //If the fragment is valid, it has been retrieved from across a config change
			//Creating and adding the fragment
			retainedFragment = new RetainedFragment();
			fragmentManager.beginTransaction().add(retainedFragment, retainedFragmentTag).commit();
		}
	}
	
	private void restoreInputBar() {
		//Restoring the input type
		if(retainedFragment.recipientInputAlphabetical) {
			recipientInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			((ImageButton) findViewById(R.id.recipients_inputtoggle)).setImageResource(R.drawable.dialpad);
		} else {
			recipientInput.setInputType(InputType.TYPE_CLASS_PHONE);
			((ImageButton) findViewById(R.id.recipients_inputtoggle)).setImageResource(R.drawable.keyboard);
		}
		
		//Restoring the chips
		if(retainedFragment.userChips.isEmpty()) {
			//Setting the hint
			recipientInput.setHint(R.string.imperative_userinput);
		} else {
			//Removing the hint
			recipientInput.setHint("");
			
			//Adding the views
			int chipIndex = 0;
			for(Chip chip : retainedFragment.userChips) {
				((ViewGroup) chip.getView().getParent()).removeView(chip.getView());
				recipientViewGroup.addView(chip.getView(), chipIndex);
				chipIndex++;
			}
		}
	}
	
	void updateContactState(byte oldState, byte newState) {
		switch(oldState) {
			case -1:
				break;
			case RetainedFragment.contactStateLoading:
				findViewById(R.id.list_contacts).setVisibility(View.GONE);
				break;
			case RetainedFragment.contactStateLoaded:
				findViewById(R.id.list_contacts).setVisibility(View.GONE);
				break;
			case RetainedFragment.contactStateNoAccess:
				findViewById(R.id.message_permission).setVisibility(View.GONE);
				break;
			case RetainedFragment.contactStateFailed:
				findViewById(R.id.message_error).setVisibility(View.GONE);
				break;
		}
		
		switch(newState) {
			case RetainedFragment.contactStateLoading:
				findViewById(R.id.list_contacts).setVisibility(View.VISIBLE);
				break;
			case RetainedFragment.contactStateLoaded:
				findViewById(R.id.list_contacts).setVisibility(View.VISIBLE);
				break;
			case RetainedFragment.contactStateNoAccess:
				findViewById(R.id.message_permission).setVisibility(View.VISIBLE);
				findViewById(R.id.list_contacts).setVisibility(View.GONE);
				break;
			case RetainedFragment.contactStateFailed:
				findViewById(R.id.message_error).setVisibility(View.VISIBLE);
				findViewById(R.id.list_contacts).setVisibility(View.GONE);
				break;
		}
	}
	
	void updateContactList() {
		contactsListAdapter.onListUpdated();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Creating the "confirm participants" menu button
		confirmMenuItem = menu.add(Menu.NONE, menuIdentifierConfirmParticipants, Menu.NONE, R.string.action_confirmparticipants);
		confirmMenuItem.setIcon(R.drawable.next);
		confirmMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		//Hiding the menu button
		confirmMenuItem.setVisible(!retainedFragment.userChips.isEmpty());
		
		//Returning true
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home: //Home button
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
		if(requestCode == Constants.permissionReadContacts) {
			//Checking if the result is a success
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				//Requesting contacts
				retainedFragment.loadContacts(getApplicationContext());
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
						.setActionTextColor(getResources().getColor(R.color.colorAccent, null))
						.show();
				/* //Showing a dialog
				new AlertDialog.Builder(this)
						.setMessage(R.string.permission_rejected)
						.setNegativeButton(R.string.button_dismiss, (dialog, which) -> {
							dialog.dismiss();
						})
						.setPositiveButton(R.string.settings, (dialog, which) -> {
							//Opening the application settings
							Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							intent.setData(Uri.parse("package:" + getPackageName()));
							startActivity(intent);
						})
						.create()
						.show(); */
			}
		}
	}
	
	public void toggleInputType(View view) {
		//Saving the selection
		int selectionStart = recipientInput.getSelectionStart();
		int selectionEnd = recipientInput.getSelectionEnd();
		
		//Checking if the input is alphabetical
		if(retainedFragment.recipientInputAlphabetical) {
			//Setting the input type
			recipientInput.setInputType(InputType.TYPE_CLASS_PHONE);
			//recipientInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
			
			//Setting the toggle input type icon
			((ImageButton) view).setImageResource(R.drawable.keyboard);
			
			//Setting the alphabetical input variable
			retainedFragment.recipientInputAlphabetical = false;
		} else {
			//Setting the input type
			recipientInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			//recipientInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
			
			//Setting the toggle input type icon
			((ImageButton) view).setImageResource(R.drawable.dialpad);
			
			//Setting the alphabetical input variable
			retainedFragment.recipientInputAlphabetical = true;
		}
		
		//Restoring the selection
		recipientInput.setSelection(selectionStart, selectionEnd);
	}
	
	private void confirmParticipants() {
		//Disabling the UI
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		
		//Getting the recipients
		ArrayList<String> recipients = getRecipientList();
		
		//Checking if the conversations are available
		ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
		if(conversations != null) {
			//Scanning the loaded conversations for a matching one
			for(ConversationManager.ConversationInfo conversationInfo : conversations) {
				//Getting the conversation members
				ArrayList<String> members = Constants.normalizeAddresses(conversationInfo.getConversationMembersAsCollection());
				
				//Returning if the conversation members are not the same
				if(recipients.size() != members.size() || !recipients.containsAll(members)) continue;
				
				//Launching the activity
				launchConversation(conversationInfo.getLocalID());
				
				//Returning
				return;
			}
		}
		
		//Confirming the participants
		new ConfirmParticipantsTask(getApplicationContext(), this, recipients, retainedFragment.service).execute();
		
		//Creating a new asynchronous task
		/* new AsyncTask<Object, Void, ConversationManager.ConversationInfo>() {
			@Override
			protected ConversationManager.ConversationInfo doInBackground(Object... args) {
				//Adding the conversation
				return DatabaseManager.addRetrieveClientCreatedConversationInfo(DatabaseManager.getWritableDatabase(NewMessage.this), NewMessage.this, (ArrayList<String>) args[0], retainedFragment.service);
			}
			
			@Override
			protected void onPostExecute(ConversationManager.ConversationInfo result) {
				//Checking if the result is a failure
				if(result == null) {
					//Enabling the UI
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
					
					//Showing an error toast
					Toast.makeText(NewMessage.this, R.string.internal_error, Toast.LENGTH_SHORT).show();
				} else {
					if(!ConversationManager.getConversations().contains(result)) {
						//Adding the conversation in memory
						ConversationManager.addConversation(result);
						
						//Updating the conversation activity list
						LocalBroadcastManager.getInstance(NewMessage.this).sendBroadcast(new Intent(Conversations.localBCConversationUpdate));
						//for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
						//	callbacks.updateList(true);
					}
					
					//Launching the activity
					launchConversation(result.getLocalID());
				}
			}
		}.execute(recipients); */
	}
	
	public void onClickRequestContacts(View view) {
		//Requesting the permission
		requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, Constants.permissionReadContacts);
	}
	
	private static class ConfirmParticipantsTask extends AsyncTask<Void, Void, ConversationManager.ConversationInfo> {
		//Creating the references
		private final WeakReference<Context> contextReference;
		private final WeakReference<NewMessage> activityReference;
		
		//Creating the values
		private final ArrayList<String> members;
		private final String service;
		
		ConfirmParticipantsTask(Context context, NewMessage activity, ArrayList<String> members, String service) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			activityReference = new WeakReference<>(activity);
			
			//Setting the values
			this.members = members;
			this.service = service;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected ConversationManager.ConversationInfo doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Cloning and normalizing the members' addresses
			List<String> normalizedMembers = (ArrayList<String>) members.clone();
			for(ListIterator<String> iterator = normalizedMembers.listIterator(); iterator.hasNext();) iterator.set(Constants.normalizeAddress(iterator.next()));
			
			//Adding the conversation
			return DatabaseManager.addRetrieveClientCreatedConversationInfo(DatabaseManager.getWritableDatabase(context), context, members, service);
		}
		
		@Override
		protected void onPostExecute(ConversationManager.ConversationInfo result) {
			//Getting the activity
			NewMessage activity = activityReference.get();
			if(activity == null) return;
			
			//Checking if the result is a failure
			if(result == null) {
				//Enabling the UI
				activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
				
				//Showing an error toast
				Toast.makeText(activity, R.string.message_serverstatus_internalexception, Toast.LENGTH_SHORT).show();
			} else {
				//Checking if the conversations exist
				ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
				if(conversations != null && !conversations.contains(result)) {
					//Adding the conversation in memory
					ConversationManager.addConversation(result);
					
					//Updating the conversation activity list
					LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(Conversations.localBCConversationUpdate));
						/* for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
							callbacks.updateList(true); */
				}
				
				//Launching the activity
				activity.launchConversation(result.getLocalID());
			}
		}
	}
	
	private ArrayList<String> getRecipientList() {
		//Converting the user chips to a string list
		ArrayList<String> recipients = new ArrayList<>();
		for(Chip chip : retainedFragment.userChips) recipients.add(chip.getName());
		
		//Sorting the list
		Collections.sort(recipients);
		
		//Normalizing the list
		Constants.normalizeAddresses(recipients);
		
		//Returning the recipient list
		return recipients;
	}
	
	private void launchConversation(long identifier) {
		//Launching the activity
		startActivity(new Intent(this, Messaging.class)
				.putExtra(Constants.intentParamTargetID, identifier));
		
		//Finishing this activity
		finish();
	}
	
	private void addChip(Chip chip) {
		//Validating the chip
		String chipName = Constants.normalizeAddress(chip.name);
		for(Chip existingChips : retainedFragment.userChips) if(Constants.normalizeAddress(existingChips.getName()).equals(chipName)) return;
		
		//Removing the hint from the recipient input if this is the first chip
		if(retainedFragment.userChips.isEmpty()) recipientInput.setHint("");
		
		//Adding the chip to the list
		retainedFragment.userChips.add(chip);
		
		//Adding the view
		recipientViewGroup.addView(chip.getView(), retainedFragment.userChips.size() - 1);
		
		//Setting the confirm button as visible
		confirmMenuItem.setVisible(true);
	}
	
	private void removeChip(Chip chip) {
		//Removing the chip from the list
		retainedFragment.userChips.remove(chip);
		
		//Removing the view
		recipientViewGroup.removeView(chip.getView());
		
		//Checking if there are no more chips
		if(retainedFragment.userChips.isEmpty()) {
			//Setting the hint
			recipientInput.setHint(R.string.imperative_userinput);
			
			//Setting the confirm button as invisible
			confirmMenuItem.setVisible(false);
		}
	}
	
	private class Chip {
		private final String name;
		private final View view;
		
		Chip(final String name) {
			//Setting the name
			this.name = name;
			
			//Setting the view
			view = getLayoutInflater().inflate(R.layout.chip_user, null);
			
			//Setting the name
			((TextView) view.findViewById(R.id.text)).setText(name);
			
			//Setting the view's click listener
			view.setOnClickListener(click -> {
				//Inflating the view
				View popupView = getLayoutInflater().inflate(R.layout.popup_userchip, null);
				
				//Setting the default information
				TextView labelView = popupView.findViewById(R.id.label_member);
				labelView.setText(name);
				((ImageView) popupView.findViewById(R.id.profile_default)).setColorFilter(getResources().getColor(R.color.colorPrimary), android.graphics.PorterDuff.Mode.MULTIPLY);
				
				//Filling in the information
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(NewMessage.this, name, new UserCacheHelper.UserFetchResult() {
					@Override
					void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
						//Returning if the user info is invalid
						if(userInfo == null) return;
						
						//Updating the text
						labelView.setText(userInfo.getContactName());
						TextView addressView = popupView.findViewById(R.id.label_address);
						addressView.setText(name);
						addressView.setVisibility(View.VISIBLE);
						
					}
				});
				MainApplication.getInstance().getUserCacheHelper().assignUserInfo(NewMessage.this, name, labelView);
				MainApplication.getInstance().getBitmapCacheHelper().assignContactImage(NewMessage.this, name, (View) popupView.findViewById(R.id.profile_image));
				
				//Creating the window
				final PopupWindow popupWindow = new PopupWindow(
						popupView,
						Constants.dpToPx(300),
						Constants.dpToPx(56));
				
				popupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorForegroundLight, null)));
				popupWindow.setOutsideTouchable(true);
				popupWindow.setElevation(Constants.dpToPx(2));
				popupWindow.setEnterTransition(new ChangeBounds());
				popupWindow.setExitTransition(new Fade());
				
				//Setting the remove listener
				popupView.findViewById(R.id.button_remove).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						//Removing this chip
						removeChip(Chip.this);
						
						//Dismissing the popup
						popupWindow.dismiss();
					}
				});
				
				//Showing the popup
				popupWindow.showAsDropDown(view);
			});
		}
		
		String getName() {
			return name;
		}
		
		View getView() {
			return view;
		}
	}
	
	/* private class ListAdapter extends BaseAdapter {
		//Creating the list values
		private ArrayList<ContactInfo> originalItems;
		private final ArrayList<ContactInfo> filteredItems = new ArrayList<>();
		
		//Creating the other values
		private View sendHeaderView;
		private boolean sendHeaderShown = false;
		private String lastFilterText = "";
		
		ListAdapter(ArrayList<ContactInfo> items) {
			//Setting the original items
			originalItems = items;
			
			//Setting the filtered items
			filteredItems.addAll(items);
			
			//Inflating the header view
			sendHeaderView = getLayoutInflater().inflate(R.layout.listitem_contact_sendheader, contactsList, false);
		}
		
		@Override
		public int getCount() {
			return filteredItems.size();
		}
		
		@Override
		public ContactInfo getItem(int position) {
			return filteredItems.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		@NonNull
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			//Inflating the view if it is invalid
			View view = convertView == null ? getLayoutInflater().inflate(R.layout.listitem_contact, null) : convertView;
			
			//Getting the item
			ContactInfo contactInfo = getItem(position);
			
			//Returning the view if the info is invalid
			if(contactInfo == null) return view;
			
			//Populating the view
			((TextView) view.findViewById(R.id.label_name)).setText(contactInfo.name);
			
			int addressCount = contactInfo.addresses.size();
			String firstAddress = contactInfo.addresses.get(0);
			if(addressCount == 1) ((TextView) view.findViewById(R.id.label_address)).setText(firstAddress);
			else ((TextView) view.findViewById(R.id.label_address)).setText(getResources().getQuantityString(R.plurals.contact_address_multiple, addressCount, firstAddress, addressCount - 1));
			
			//Showing / hiding the section header
			boolean showHeader;
			//if(!lastFilterText.isEmpty()) showHeader = false;
			if(position > 0) {
				ContactInfo contactInfoAbove = getItem(position - 1);
				showHeader = contactInfoAbove == null || !stringsHeaderEqual(contactInfo.name, contactInfoAbove.name);
			} else showHeader = true;
			if(showHeader) {
				view.findViewById(R.id.header).setVisibility(View.VISIBLE);
				((TextView) view.findViewById(R.id.header_label)).setText(Character.toString(getNameHeader(contactInfo.name)));
			} else view.findViewById(R.id.header).setVisibility(View.GONE);
			
			//Resetting the image view
			View iconView = view.findViewById(R.id.profile);
			ImageView profileDefault = iconView.findViewById(R.id.profile_default);
			profileDefault.setVisibility(View.VISIBLE);
			profileDefault.setColorFilter(getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
			((ImageView) iconView.findViewById(R.id.profile_image)).setImageBitmap(null);
			
			//Assigning the contact's image
			MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(NewMessage.this, Long.toString(contactInfo.identifier), contactInfo.identifier, new BitmapCacheHelper.ImageDecodeResult() {
				@Override
				void onImageMeasured(int width, int height) {
				
				}
				
				@Override
				void onImageDecoded(Bitmap result, boolean wasTasked) {
					//Returning if the result is invalid
					if(result == null) return;
					
					//Getting the view
					View currentView = wasTasked ? contactsList.getChildAt(position - contactsList.getFirstVisiblePosition()) : view;
					if(currentView == null) return;
					
					//Getting the icon view
					View iconView = currentView.findViewById(R.id.profile);
					if(iconView == null) return; //TODO find out why this is necessary
					
					//Hiding the default view
					iconView.findViewById(R.id.profile_default).setVisibility(View.INVISIBLE);
					
					//Getting the profile image view
					ImageView imageView = iconView.findViewById(R.id.profile_image);
					
					//Setting the bitmap
					imageView.setImageBitmap(result);
					
					//Fading in the view
					if(wasTasked) {
						imageView.setAlpha(0F);
						imageView.animate().alpha(1).setDuration(300).start();
					}
				}
			});
			
			//Setting the click listener
			view.findViewById(R.id.area_content).setOnClickListener(clickView -> {
				//Checking if there is only one label
				if(contactInfo.addresses.size() == 1) {
					//Adding the chip
					addChip(new Chip(contactInfo.addresses.get(0)));
					
					//Clearing the text
					recipientInput.setText("");
				} else {
					//Showing a dialog
					new AlertDialog.Builder(NewMessage.this)
							.setTitle(R.string.contact_address_select)
							.setItems(contactInfo.addresses.toArray(new String[0]), ((dialogInterface, index) -> {
								//Adding the selected chip
								addChip(new Chip(contactInfo.addresses.get(index)));
								
								//Clearing the text
								recipientInput.setText("");
							}))
							.create().show();
				}
			});
			
			//Returning the view
			return view;
		}
		
		private char getNameHeader(String name) {
			if(name.isEmpty()) return '?';
			return name.charAt(0);
		}
		
		private boolean stringsHeaderEqual(String string1, String string2) {
			if(string1.isEmpty()) return string2.isEmpty();
			return string1.charAt(0) == string2.charAt(0);
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
			
			//Copying the original items
			filteredItems.clear();
			
			//Checking if the filter isn't empty
			if(!filter.isEmpty()) {
				//Normalizing the filter
				String normalizedFilter = Constants.normalizeAddress(filter);
				
				//Filtering the list
				contactLoop:
				for(ContactInfo contactInfo : originalItems) {
					//Adding the item if the name matches the filter
					if(contactInfo.name != null && contactInfo.name.toLowerCase().contains(filter.toLowerCase())) {
						filteredItems.add(contactInfo);
						continue contactLoop;
					}
					
					//Adding the item if any of the contact's addresses match the filter
					for(String address : contactInfo.normalizedAddresses) if(address.startsWith(normalizedFilter)) {
						filteredItems.add(contactInfo);
						continue contactLoop;
					}
				}
				
				//Checking if the filter text is a valid label
				if(Constants.validateAddress(filter)) {
					//Showing the header view
					setHeaderState(true);
					
					//Updating the header view
					((TextView) sendHeaderView.findViewById(R.id.label_address)).setText(getResources().getString(R.string.contact_address_fill, filter));
				} else {
					//Hiding the header view
					setHeaderState(false);
				}
			} else {
				//Adding all of the items
				filteredItems.addAll(originalItems);
				
				//Removing the header view
				setHeaderState(false);
			}
			
			//Notifying the adapter
			notifyDataSetChanged();
		}
		
		private void setHeaderState(boolean state) {
			//Returning if the requested state matches the current state
			if(sendHeaderShown == state) return;
			
			//Updating the state
			sendHeaderShown = state;
			
			//Adding / removing the header and click listener
			if(state) {
				contactsList.addHeaderView(sendHeaderView);
				sendHeaderView.setOnClickListener(view -> {
					//Adding the chip
					addChip(new Chip(lastFilterText.trim()));
					
					//Clearing the text
					recipientInput.setText("");
				});
			}
			else contactsList.removeHeaderView(sendHeaderView);
		}
	} */
	
	private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the type constants
		private static final int TYPE_HEADER = 0;
		private static final int TYPE_ITEM = 1;
		
		//Creating the list values
		private ArrayList<ContactInfo> originalItems;
		private final ArrayList<ContactInfo> filteredItems = new ArrayList<>();
		
		//Creating the recycler values
		private RecyclerView recyclerView;
		
		//Creating the other values
		private boolean directAddHeaderVisible = false;
		private String lastFilterText = "";
		
		RecyclerAdapter(ArrayList<ContactInfo> items, RecyclerView recyclerView) {
			//Setting the items
			originalItems = items;
			filteredItems.addAll(items);
			
			//Adding the recycler values
			this.recyclerView = recyclerView;
		}
		
		class HeaderViewHolder extends RecyclerView.ViewHolder {
			//Creating the view values
			private final TextView label;
			
			HeaderViewHolder(View view) {
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
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			switch(viewType) {
				case TYPE_HEADER:
					return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_contact_sendheader, parent, false));
				case TYPE_ITEM:
					return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_contact, parent, false));
				default:
					return null;
			}
		}
		
		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
			switch(getItemViewType(position)) {
				case TYPE_HEADER: {
					//Casting the view holder
					HeaderViewHolder headerViewHolder = (HeaderViewHolder) viewHolder;
					
					//Setting the label
					headerViewHolder.label.setText(getResources().getString(R.string.action_sendto, lastFilterText));
					
					//Setting the click listener
					headerViewHolder.itemView.setOnClickListener(view -> {
						//Adding the chip
						addChip(new Chip(lastFilterText.trim()));
						
						//Clearing the text
						recipientInput.setText("");
					});
					
					//Breaking
					break;
				}
				case TYPE_ITEM: {
					//Casting the view holder
					ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
					
					//Getting the item
					ContactInfo contactInfo = getItemAtIndex(position);
					
					//Populating the view
					itemViewHolder.contactName.setText(contactInfo.name);
					
					int addressCount = contactInfo.addresses.size();
					String firstAddress = contactInfo.addresses.get(0);
					if(addressCount == 1) itemViewHolder.contactAddress.setText(firstAddress);
					else itemViewHolder.contactAddress.setText(getResources().getQuantityString(R.plurals.message_multipledestinations, addressCount, firstAddress, addressCount - 1));
					
					//Showing / hiding the section header
					boolean showHeader;
					//if(!lastFilterText.isEmpty()) showHeader = false;
					if(position > 0) {
						ContactInfo contactInfoAbove = filteredItems.get(position - 1);
						showHeader = contactInfoAbove == null || !stringsHeaderEqual(contactInfo.name, contactInfoAbove.name);
					} else showHeader = true;
					
					if(showHeader) {
						itemViewHolder.header.setVisibility(View.VISIBLE);
						itemViewHolder.headerLabel.setText(Character.toString(getNameHeader(contactInfo.name)));
					} else itemViewHolder.header.setVisibility(View.GONE);
					
					//Resetting the image view
					itemViewHolder.profileDefault.setVisibility(View.VISIBLE);
					itemViewHolder.profileDefault.setColorFilter(getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
					itemViewHolder.profileImage.setImageBitmap(null);
					
					//Assigning the contact's image
					MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(NewMessage.this, Long.toString(contactInfo.identifier), contactInfo.identifier, new BitmapCacheHelper.ImageDecodeResult() {
						@Override
						void onImageMeasured(int width, int height) {
						
						}
						
						@Override
						void onImageDecoded(Bitmap result, boolean wasTasked) {
							//Returning if the result is invalid
							if(result == null) return;
							
							//Returning if the item doesn't exist anymore
							if(!filteredItems.contains(contactInfo)) return;
							
							//Getting the view holder
							ItemViewHolder currentViewHolder = wasTasked ? (ItemViewHolder) recyclerView.findViewHolderForAdapterPosition(filteredItems.indexOf(contactInfo)) : itemViewHolder;
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
					itemViewHolder.contentArea.setOnClickListener(clickView -> {
						//Checking if there is only one label
						if(contactInfo.addresses.size() == 1) {
							//Adding the chip
							addChip(new Chip(contactInfo.addresses.get(0)));
							
							//Clearing the text
							recipientInput.setText("");
						} else {
							//Showing a dialog
							new AlertDialog.Builder(NewMessage.this)
									.setTitle(R.string.imperative_selectdestination)
									.setItems(contactInfo.addresses.toArray(new String[0]), ((dialogInterface, index) -> {
										//Adding the selected chip
										addChip(new Chip(contactInfo.addresses.get(index)));
										
										//Clearing the text
										recipientInput.setText("");
									}))
									.create().show();
						}
					});
					
					//Breaking
					break;
				}
			}
		}
		
		private ContactInfo getItemAtIndex(int index) {
			return filteredItems.get(index - (directAddHeaderVisible ? 1 : 0));
		}
		
		@Override
		public int getItemCount() {
			return filteredItems.size() + (directAddHeaderVisible ? 1 : 0);
		}
		
		@Override
		public int getItemViewType(int position) {
			//Returning "header" if the header is visible and the position is the first one
			if(directAddHeaderVisible && position == 0) return TYPE_HEADER;
			
			//Otherwise returning "item"
			return TYPE_ITEM;
		}
		
		private char getNameHeader(String name) {
			if(name.isEmpty()) return '?';
			return name.charAt(0);
		}
		
		private boolean stringsHeaderEqual(String string1, String string2) {
			if(string1.isEmpty()) return string2.isEmpty();
			return string1.charAt(0) == string2.charAt(0);
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
			
			//Copying the original items
			filteredItems.clear();
			
			//Checking if the filter isn't empty
			if(!filter.isEmpty()) {
				//Normalizing the filter
				String normalizedFilter = Constants.normalizeAddress(filter);
				
				//Filtering the list
				contactLoop:
				for(ContactInfo contactInfo : originalItems) {
					//Adding the item if the name matches the filter
					if(contactInfo.name != null && contactInfo.name.toLowerCase().contains(filter.toLowerCase())) {
						filteredItems.add(contactInfo);
						continue contactLoop;
					}
					
					//Adding the item if any of the contact's addresses match the filter
					for(String address : contactInfo.normalizedAddresses) if(address.startsWith(normalizedFilter)) {
						filteredItems.add(contactInfo);
						continue contactLoop;
					}
				}
				
				//Checking if the filter text is a valid label
				if(Constants.validateAddress(filter)) {
					//Showing the header view
					setHeaderState(true);
				} else {
					//Hiding the header view
					setHeaderState(false);
				}
			} else {
				//Adding all of the items
				filteredItems.addAll(originalItems);
				
				//Removing the header view
				setHeaderState(false);
			}
			
			//Notifying the adapter
			notifyDataSetChanged();
		}
		
		private void setHeaderState(boolean state) {
			//Returning if the requested state matches the current state
			if(directAddHeaderVisible == state) return;
			
			//Updating the state
			directAddHeaderVisible = state;
			
			/* //Adding / removing the header and click listener
			if(state) {
				layoutManager.addHeaderView(directAddHeaderView);
				directAddHeaderView.setOnClickListener(view -> {
					//Adding the chip
					addChip(new Chip(lastFilterText.trim()));
					
					//Clearing the text
					recipientInput.setText("");
				});
			}
			else contactsList.removeHeaderView(directAddHeaderView); */
		}
	}
	
	private static class ContactInfo {
		private final long identifier;
		private final String name;
		private final ArrayList<String> addresses;
		private final ArrayList<String> normalizedAddresses = new ArrayList<>();
		
		ContactInfo(long identifier, String name, ArrayList<String> addresses) {
			this.identifier = identifier;
			this.name = name;
			this.addresses = addresses;
			for(String address : addresses) normalizedAddresses.add(Constants.normalizeAddress(address));
		}
		
		void addAddress(String address) {
			addresses.add(address);
			normalizedAddresses.add(Constants.normalizeAddress(address));
		}
	}
	
	public static class RetainedFragment extends Fragment {
		//Creating the task values
		private NewMessage parentActivity;
		
		//Creating the input values
		private ArrayList<Chip> userChips = new ArrayList<>();
		private boolean recipientInputAlphabetical = true;
		
		//Creating the state values
		private static final byte contactStateIdle = 0;
		private static final byte contactStateLoading = 1;
		private static final byte contactStateLoaded = 2;
		private static final byte contactStateNoAccess = 3;
		private static final byte contactStateFailed = 4;
		private byte contactState = contactStateIdle;
		
		//Creating the other values
		private String service = Constants.serviceIDAppleMessage;
		private ArrayList<ContactInfo> contactList = new ArrayList<>();
		
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
			parentActivity = (NewMessage) context;
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
		
		void updateState(byte state) {
			//Returning if the requested state matches the existing state
			if(contactState == state) return;
			
			//Updating the activity
			if(parentActivity != null) parentActivity.updateContactState(contactState, state);
			
			//Setting the new state
			contactState = state;
		}
		
		void loadContacts(Context appContext) {
			//Returning if the state is not ready to load contacts
			if(contactState == contactStateLoading || contactState == contactStateLoaded) return;
			
			//Starting the task
			new LoadContactsAsyncTask(appContext, this).execute();
			
			//Setting the state
			updateState(contactStateLoading);
		}
		
		public static class LoadContactsAsyncTask extends AsyncTask<Void, ContactInfo, ArrayList<ContactInfo>> {
			//Creating the reference values
			private final WeakReference<Context> contextReference;
			private final WeakReference<RetainedFragment> fragmentReference;
			
			LoadContactsAsyncTask(Context context, RetainedFragment fragment) {
				//Setting the references
				contextReference = new WeakReference<>(context);
				fragmentReference = new WeakReference<>(fragment);
			}
			
			@Override
			protected ArrayList<ContactInfo> doInBackground(Void... parameters) {
				//Getting the context
				Context context = contextReference.get();
				if(context == null) return null;
				
				//Getting the content resolver
				ContentResolver contentResolver = context.getContentResolver();
				
				//Querying the database
				Cursor cursor = contentResolver.query(
						ContactsContract.Data.CONTENT_URI,
						new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data.DATA1},
						ContactsContract.Data.MIMETYPE + " = ? OR (" + ContactsContract.Data.HAS_PHONE_NUMBER + "!= 0 AND " + ContactsContract.Data.MIMETYPE + " = ?)",
						new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
						ContactsContract.Data.DISPLAY_NAME + " ASC");
				
				//Returning null if the cursor is invalid
				if(cursor == null) return null;
				
				//Reading the data
				ArrayList<ContactInfo> contactList = new ArrayList<>();
				int indexContactID = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID);
				int indexDisplayName = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME);
				int indexAddress = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1);
				
				userIterator:
				while(cursor.moveToNext()) {
					//Retrieving and validating the entry's label
					String address = cursor.getString(indexAddress);
					if(address == null || address.isEmpty()) continue;
					
					//Getting the general info
					long contactID = cursor.getLong(indexContactID);
					String contactName = cursor.getString(indexDisplayName);
					if(contactName != null && contactName.isEmpty()) contactName = null;
					
					//Checking if there is a user with a matching contact ID
					String normalizedAddress = Constants.normalizeAddress(address);
					for(ContactInfo contactInfo : contactList)
						if(contactInfo.identifier == contactID) {
							for(String contactAddresses : contactInfo.normalizedAddresses)
								if(contactAddresses.equals(normalizedAddress))
									continue userIterator;
							contactInfo.addAddress(address);
							continue userIterator;
						}
					
					//Adding the user to the list
					ArrayList<String> contactAddresses = new ArrayList<>();
					contactAddresses.add(address);
					ContactInfo contactInfo = new ContactInfo(contactID, contactName, contactAddresses);
					contactList.add(contactInfo);
					
					//Calling the progress update
					publishProgress(contactInfo);
				}
				
				//Closing the cursor
				cursor.close();
				
				//Returning the contact list
				return contactList;
			}
			
			@Override
			protected void onProgressUpdate(ContactInfo... newContacts) {
				//Getting the fragment
				RetainedFragment fragment = fragmentReference.get();
				if(fragment == null) return;
				
				//Adding the contacts
				Collections.addAll(fragment.contactList, newContacts);
				
				//Updating the list
				if(fragment.getActivity() != null) fragment.parentActivity.updateContactList();
			}
			
			@Override
			protected void onPostExecute(ArrayList<ContactInfo> contacts) {
				//Getting the fragment
				RetainedFragment fragment = fragmentReference.get();
				if(fragment == null) return;
				
				//Checking if the result is invalid
				if(contacts == null) {
					//Updating to a failed state
					fragment.updateState(contactStateFailed);
				} else {
					//Setting the contacts
					fragment.contactList.clear();
					fragment.contactList.addAll(contacts);
					
					//Updating the list
					if(fragment.getActivity() != null) fragment.parentActivity.updateContactList();
					
					//Updating to a completed state
					fragment.updateState(contactStateLoaded);
				}
			}
		}
	}
}