package me.tagavari.airmessage;

import android.app.ActivityManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import in.srain.cube.views.GridViewWithHeaderAndFooter;

public class MessagingInfo extends AppCompatActivity {
	//Creating the activity values
	private ConversationManager.ConversationInfo conversationInfo;
	
	//Creating the difference values
	private boolean originalMuted;
	private int originalColor;
	private final HashMap<String, Integer> originalMemberColors = new HashMap<>();
	
	//Creating the listener values
	private CompoundButton.OnCheckedChangeListener notificationSwitchListener = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			//Updating the conversation
			conversationInfo.setMuted(!isChecked);
		}
	};
	
	//Creating the view variables
	private MenuItem conversationColorMenuItem = null;
	
	//Creating the view values
	private GridViewWithHeaderAndFooter fileGridView;
	private ViewGroup headerView;
	
	//Creating the other values
	private final HashMap<String, View> memberListViews = new HashMap<>();
	private ActivityManager.TaskDescription lastTaskDescription;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Inflating the layout
		setContentView(R.layout.activity_messaginginfo);
		
		//Enabling the toolbar's up navigation
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Configuring the grid layout
		headerView = (ViewGroup) getLayoutInflater().inflate(R.layout.activity_messaginginfo_header, null, false);
		
		fileGridView = findViewById(R.id.content);
		fileGridView.addHeaderView(headerView);
		fileGridView.setAdapter(new AttachmentAdapter(this, 0, new ArrayList<>()));
		
		//Enforcing the maximum content width
		Constants.enforceContentWidth(getResources(), fileGridView);
		
		//Getting the conversation info
		long conversationID = getIntent().getLongExtra(Constants.intentParamTargetID, -1);
		ConversationManager.ConversationInfo conversationInfo = ConversationManager.findConversationInfo(conversationID);
		
		//Checking if the conversation info is invalid
		if(conversationInfo == null) {
			//Loading the conversation
			new LoadConversationTask(this, conversationID);
		} else {
			//Applying the conversation
			applyConversation(conversationInfo);
		}
	}
	
	private static class LoadConversationTask extends AsyncTask<Void, Void, ConversationManager.ConversationInfo> {
		//Creating the request values
		private final WeakReference<MessagingInfo> superclassReference;
		private final long identifier;
		
		LoadConversationTask(MessagingInfo superclass, long identifier) {
			//Setting the request values
			superclassReference = new WeakReference<>(superclass);
			this.identifier = identifier;
		}
		
		@Override
		protected ConversationManager.ConversationInfo doInBackground(Void... voids) {
			//Getting the context
			Context context = superclassReference.get();
			if(context == null) return null;
			
			//Returning the conversation
			return DatabaseManager.getInstance().fetchConversationInfo(context, identifier);
		}
		
		@Override
		protected void onPostExecute(ConversationManager.ConversationInfo conversationInfo) {
			//Getting the superclass
			MessagingInfo superclass = superclassReference.get();
			if(superclass == null) return;
			
			//Applying the conversation
			superclass.applyConversation(conversationInfo);
		}
	}
	
	void applyConversation(ConversationManager.ConversationInfo conversationInfo) {
		//Setting the conversation
		this.conversationInfo = conversationInfo;
		
		//Swapping to the content view
		findViewById(R.id.loading_text).setVisibility(View.GONE);
		fileGridView.setVisibility(View.VISIBLE);
		
		//Updating the menu
		if(conversationColorMenuItem != null) conversationColorMenuItem.setVisible(conversationInfo.isGroupChat());
		
		//Setting the notifications switch
		{
			Switch switchView = headerView.findViewById(R.id.switch_receivenotifications);
			switchView.setChecked(!conversationInfo.isMuted());
			switchView.setOnCheckedChangeListener(notificationSwitchListener);
		}
		
		//Adding the conversation members
		addConversationMembers(conversationInfo.getConversationMembers());
		
		//Setting the conversation title
		conversationInfo.buildTitle(this, (result, wasTasked) -> setTaskDescription(lastTaskDescription = new ActivityManager.TaskDescription(result, BitmapFactory.decodeResource(getResources(), R.drawable.app_icon), conversationInfo.getConversationColor())));
		
		//Coloring the UI
		colorUI(headerView);
		
		//Recording the original data
		originalMuted = conversationInfo.isMuted();
		originalColor = conversationInfo.getConversationColor();
		for(ConversationManager.MemberInfo memberInfo : conversationInfo.getConversationMembers())
			originalMemberColors.put(memberInfo.getName(), memberInfo.getColor());
	}
	
	@Override
	protected void onStart() {
		//Calling the super method
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		//Calling the super method
		super.onStop();
		
		//Returning if the conversation is invalid
		if(conversationInfo == null) return;
		
		//Finding the differences
		final boolean conversationMutedChanged = conversationInfo.isMuted() != originalMuted;
		final boolean conversationColorChanged = conversationInfo.getConversationColor() != originalColor;
		final ArrayList<ConversationManager.MemberInfo> modifiedMembers = new ArrayList<>();
		for(int i = 0; i < conversationInfo.getConversationMembers().size(); i++) {
			//Getting the member
			ConversationManager.MemberInfo member = conversationInfo.getConversationMembers().get(i);
			
			//Skipping the remainder of the iteration if the member never had an original value or the color hasn't changed
			if(!originalMemberColors.containsKey(member.getName()) || originalMemberColors.get(member.getName()) == member.getColor())
				continue;
			
			//Adding the member to the list
			modifiedMembers.add(member);
		}
		
		//Returning if there are no modifications
		if(!conversationMutedChanged && !conversationColorChanged && modifiedMembers.isEmpty())
			return;
		
		//Updating the conversation view
		conversationInfo.updateViewUser(this);
		
		//Saving the data in the database
		new UpdateConversationAsyncTask(this, conversationInfo, conversationMutedChanged, conversationColorChanged, modifiedMembers).execute();
		/* new AsyncTask<Object, Void, Void>() {
			@Override
			protected Void doInBackground(Object... params) {
				//Getting the data
				ConversationManager.ConversationInfo conversationInfo = (ConversationManager.ConversationInfo) params[0];
				
				//Getting an instance of the database
				SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(MessagingInfo.this);
				
				//Updating the database
				ContentValues contentValues = new ContentValues();
				if(conversationMutedChanged)
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_MUTED, conversationInfo.isMuted());
				if(conversationColorChanged)
					contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationInfo.getConversationColor());
				
				if(contentValues.size() != 0)
					DatabaseManager.updateConversation(writableDatabase, conversationInfo.getLocalID(), contentValues);
				
				//Updating the member colors
				if(!modifiedMembers.isEmpty())
					DatabaseManager.updateMemberColors(writableDatabase, conversationInfo.getLocalID(), modifiedMembers.toArray(new ConversationManager.MemberInfo[0]));
				
				//Returning
				return null;
			}
		}.execute(conversationInfo); */
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Inflating the menu resource
		getMenuInflater().inflate(R.menu.menu_messaginginfo, menu);
		
		//Setting the button
		conversationColorMenuItem = menu.findItem(R.id.action_editcolor);
		
		//Setting the item's visibility
		conversationColorMenuItem.setVisible(conversationInfo != null && conversationInfo.isGroupChat());
		
		//Returning true
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.action_editcolor: //Edit color button
				//Returning if the conversation is invalid
				if(conversationInfo == null) return true;
				
				//Showing the color dialog
				showColorDialog(null, conversationInfo.getConversationColor());
				return true;
			case android.R.id.home: //Up button
				//Finishing the activity
				finish();
				return true;
		}
		
		//Returning false
		return false;
	}
	
	public void toggleNotifications(View view) {
		//Toggling the switch
		Switch notificationsSwitch = view.findViewById(R.id.switch_receivenotifications);
		notificationsSwitch.setChecked(!notificationsSwitch.isChecked());
	}
	
	private void addConversationMembers(List<ConversationManager.MemberInfo> members) {
		//Getting the members layout
		ViewGroup membersLayout = headerView.findViewById(R.id.conversation_members);
		
		//Sorting the members
		Collections.sort(members, ConversationManager.memberInfoComparator);
		
		//Iterating over the members
		for(final ConversationManager.MemberInfo member : members) {
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
			changeColorButton.setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			changeColorButton.setOnClickListener(view -> showColorDialog(member, member.getColor()));
			
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
		}
	}
	
	void showColorDialog(ConversationManager.MemberInfo member, int currentColor) {
		//Starting a fragment transaction
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		
		//Removing the previous fragment if it already exists
		Fragment previousFragment = getFragmentManager().findFragmentByTag("dialog");
		if(previousFragment != null) fragmentTransaction.remove(previousFragment);
		fragmentTransaction.addToBackStack(null);
		
		//Creating and showing the dialog fragment
		ColorPickerDialog newFragment = ColorPickerDialog.newInstance(member, currentColor);
		newFragment.show(fragmentTransaction, "dialog");
	}
	
	private static class AttachmentAdapter extends ArrayAdapter<ConversationManager.AttachmentInfo> {
		AttachmentAdapter(Context context, int resource, List<ConversationManager.AttachmentInfo> items) {
			//Calling the super method
			super(context, resource, items);
		}
		
		@Override
		@NonNull
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			ImageView imageView;
			if(convertView == null) {
				// if it's not recycled, initialize some attributes
				imageView = new ImageView(getContext());
				imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(8, 8, 8, 8);
			} else {
				imageView = (ImageView) convertView;
			}
			
			//imageView.setImageResource(mThumbIds[position]);
			return imageView;
		}
	}
	
	void switchMemberColor(ConversationManager.MemberInfo member, int newColor) {
		//Updating the user's color
		member.setColor(newColor);
		
		//Updating the view
		View memberView = memberListViews.get(member.getName());
		((ImageView) memberView.findViewById(R.id.button_change_color)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		((ImageView) memberView.findViewById(R.id.profile_default)).setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		
		//Checking if the conversation is a one-on-one chat
		if(!conversationInfo.isGroupChat()) {
			//Updating the conversation color
			conversationInfo.setConversationColor(newColor);
			
			//Coloring the UI
			colorUI(headerView);
		}
	}
	
	void switchConversationColor(int newColor) {
		//Updating the conversation color
		conversationInfo.setConversationColor(newColor);
		
		//Coloring the UI
		colorUI(headerView);
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
			if(view instanceof ImageView)
				((ImageView) view).setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
			else if(view instanceof Switch) {
				Switch switchView = (Switch) view;
				switchView.setThumbTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0xFFFAFAFA, color}));
				switchView.setTrackTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0x61000000, color}));
			}
		}
	}
	
	private static class UpdateConversationAsyncTask extends AsyncTask<Void, Void, Void> {
		//Creating the reference values
		private final WeakReference<Context> contextReference;
		
		//Creating the task values
		private final ConversationManager.ConversationInfo conversationInfo;
		private final boolean conversationMutedChanged;
		private final boolean conversationColorChanged;
		private final List<ConversationManager.MemberInfo> modifiedMembers;
		
		UpdateConversationAsyncTask(Context context, ConversationManager.ConversationInfo conversationInfo, boolean conversationMutedChanged, boolean conversationColorChanged, List<ConversationManager.MemberInfo> modifiedMembers) {
			//Setting the context values
			contextReference = new WeakReference<>(context);
			
			//Setting the task values
			this.conversationInfo = conversationInfo;
			this.conversationMutedChanged = conversationMutedChanged;
			this.conversationColorChanged = conversationColorChanged;
			this.modifiedMembers = modifiedMembers;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Updating the database
			ContentValues contentValues = new ContentValues();
			if(conversationMutedChanged)
				contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_MUTED, conversationInfo.isMuted());
			if(conversationColorChanged)
				contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationInfo.getConversationColor());
			
			if(contentValues.size() != 0)
				DatabaseManager.getInstance().updateConversation(conversationInfo.getLocalID(), contentValues);
			
			//Updating the member colors
			if(!modifiedMembers.isEmpty())
				DatabaseManager.getInstance().updateMemberColors(conversationInfo.getLocalID(), modifiedMembers.toArray(new ConversationManager.MemberInfo[0]));
			
			//Returning
			return null;
		}
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
				if(isSelectedColor)
					item.findViewById(R.id.colorpickeritem_selection).setVisibility(View.VISIBLE);
				
				colorView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						//Telling the activity
						if(!isSelectedColor) {
							if(member == null)
								((MessagingInfo) getActivity()).switchConversationColor(standardColor);
							else
								((MessagingInfo) getActivity()).switchMemberColor(member, standardColor);
						}
						
						//Dismissing the dialog
						getDialog().dismiss();
					}
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
}