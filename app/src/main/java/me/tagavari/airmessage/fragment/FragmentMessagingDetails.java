package me.tagavari.airmessage.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.ColorHelper;
import me.tagavari.airmessage.helper.ColorMathHelper;
import me.tagavari.airmessage.helper.ContactHelper;
import me.tagavari.airmessage.helper.ConversationBuildHelper;
import me.tagavari.airmessage.helper.ConversationColorHelper;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.helper.ViewHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.task.ConversationActionTask;

public class FragmentMessagingDetails extends BottomSheetDialogFragment {
	private static final String savedInstanceKeyConversation = "conversation";
	private static final String colorDialogTag = "colorPickerDialog";
	
	//Parameters
	private ConversationInfo conversationInfo;
	
	//Views
	private LinearLayout listConversationMembers;
	private MaterialButton buttonConversationColor;
	private final Map<String, View> memberViewMap = new HashMap<>();
	
	private ViewGroup viewGroupRename;
	private TextView labelRename;
	
	private ViewGroup groupNotifications;
	private SwitchMaterial switchNotifications;
	private MaterialButton buttonArchive;
	private MaterialButton buttonDelete;
	
	//Dialog
	private DialogFragment currentColorPickerDialog = null;
	private MemberInfo currentColorPickerDialogMember = null;
	
	//Disposables
	private final CompositeDisposable fragmentCD = new CompositeDisposable();
	private Disposable disposableMute, disposableArchive, disposableTitle;
	private final Map<String, CompositeDisposable> memberCDMap = new HashMap<>();
	
	public FragmentMessagingDetails() {
	}
	
	/**
	 * Initializes a messaging details fragment from the provided conversation
	 */
	public FragmentMessagingDetails(ConversationInfo conversationInfo) {
		this.conversationInfo = conversationInfo.clone();
	}
	
	/* @NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog superDialog = super.onCreateDialog(savedInstanceState);
		
		superDialog.setOnShowListener(dialog -> {
			BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
			FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
			
			//Expanding the dialog to full height
			behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
			behavior.setPeekHeight(0);
		});
		
		return superDialog;
	} */
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_messagingdetails, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View createdView, @Nullable Bundle savedInstanceState) {
		listConversationMembers = createdView.findViewById(R.id.list_conversationmembers);
		buttonConversationColor = createdView.findViewById(R.id.button_changecolor);
		
		//Checking if this conversation can be renamed
		if(isRenameSupported()) {
			//Inflating the rename section
			View viewRename = ((ViewStub) createdView.findViewById(R.id.viewstub_groupname)).inflate();
			viewGroupRename = viewRename.findViewById(R.id.group_groupname);
			labelRename = viewGroupRename.findViewById(R.id.label_groupname);
			
			//Setting the title
			updateViewConversationTitle();
			
			//Setting the click listener
			viewGroupRename.setOnClickListener(view -> showRenameDialog());
		}
		
		groupNotifications = createdView.findViewById(R.id.group_getnotifications);
		switchNotifications = groupNotifications.findViewById(R.id.switch_getnotifications);
		buttonArchive = createdView.findViewById(R.id.button_archive);
		buttonDelete = createdView.findViewById(R.id.button_delete);
		
		//Updating the notifications switch
		updateViewMuted();
		
		//Setting the notifications switch toggle listener
		groupNotifications.setOnClickListener(view -> {
			//Ignoring if there is a process already in progress
			if(disposableMute != null && !disposableMute.isDisposed()) return;
			
			//Updating the conversation's mute state
			fragmentCD.add(disposableMute = ConversationActionTask.muteConversations(Collections.singleton(conversationInfo), !conversationInfo.isMuted()).subscribe());
		});
		
		//Configuring the color button
		if(Preferences.getPreferenceAdvancedColor(requireContext())) {
			buttonConversationColor.setOnClickListener(view -> showColorDialog(null, conversationInfo.getConversationColor()));
		} else {
			buttonConversationColor.setVisibility(View.GONE);
		}
		
		//Updating the accent color
		updateViewConversationColor();
		
		//Adding the member views
		for(MemberInfo member : conversationInfo.getMembers()) addMemberView(member);
		
		//Setting the archive button click listener
		buttonArchive.setOnClickListener(view -> {
			//Ignoring if there is a process already in progress
			if(disposableArchive != null && !disposableArchive.isDisposed()) return;
			
			//Updating the conversation's archived state
			fragmentCD.add(disposableArchive = ConversationActionTask.archiveConversations(Collections.singleton(conversationInfo), !conversationInfo.isArchived()).subscribe());
		});
		
		//Setting the delete button click listener
		buttonDelete.setOnClickListener(view -> {
			//Creating a dialog
			AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
					.setMessage(R.string.message_confirm_deleteconversation_current)
					.setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
					.setPositiveButton(R.string.action_delete, (dialogInterface, which) -> {
						//Deleting the conversation
						ConversationActionTask.deleteConversations(requireContext(), Collections.singleton(conversationInfo)).subscribe();
						
						//Closing the dialog
						dismiss();
					})
					.create();
			
			//Configuring the dialog's listener
			dialog.setOnShowListener(dialogInterface -> {
				//Setting the button's colors
				int color = getResources().getColor(R.color.colorActionDelete, null);
				ColorStateList colorRipple = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, ColorConstants.rippleAlphaInt));
				
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
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Restoring the parameters
		if(savedInstanceState != null) {
			conversationInfo = savedInstanceState.getParcelable(savedInstanceKeyConversation);
		}
		
		//Subscribing to conversation updates
		fragmentCD.add(ReduxEmitterNetwork.getMessageUpdateSubject().subscribe(this::handleUpdate));
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		fragmentCD.clear();
		for(CompositeDisposable compositeDisposable : memberCDMap.values()) {
			compositeDisposable.clear();
		}
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putParcelable(savedInstanceKeyConversation, conversationInfo);
	}
	
	/**
	 * Gets if the current conversation supports being renamed
	 */
	private boolean isRenameSupported() {
		return conversationInfo.getServiceHandler() != ServiceHandler.appleBridge;
	}
	
	/**
	 * Adds a member to the member list view
	 * @param member The member to add
	 */
	private void addMemberView(MemberInfo member) {
		//Creating the view
		LayoutInflater inflater = requireContext().getSystemService(LayoutInflater.class);
		View viewMember = inflater.inflate(R.layout.listitem_member, listConversationMembers, false);
		TextView labelMember = viewMember.findViewById(R.id.label_member);
		ImageView iconDefault = viewMember.findViewById(R.id.profile_default);
		ImageView iconMember = viewMember.findViewById(R.id.profile_image);
		ImageButton changeColorButton = viewMember.findViewById(R.id.button_change_color);
		
		//Formatting the user's address
		String displayAddress = AddressHelper.formatAddress(member.getAddress());
		
		//Setting the default information
		labelMember.setText(displayAddress);
		iconDefault.setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		
		//Filling in the information
		getMemberCD(member.getAddress()).add(
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(requireContext(), member.getAddress())
						.subscribe(userInfo -> {
									//Setting the tag (for a contact view link)
									viewMember.setTag(new ContactAccessInfo(userInfo.getContactLookupUri()));
									
									//Setting the member's name
									labelMember.setText(userInfo.getContactName());
									TextView addressView = viewMember.findViewById(R.id.label_address);
									addressView.setText(displayAddress);
									addressView.setVisibility(View.VISIBLE);
									
									Glide.with(this)
											.load(ContactHelper.getContactImageURI(userInfo.getContactID()))
											.listener(new RequestListener<Drawable>() {
												@Override
												public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
													return false;
												}
												
												@Override
												public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
													//Swapping to the profile view
													iconDefault.setVisibility(View.GONE);
													iconMember.setVisibility(View.VISIBLE);
													
													return false;
												}
											})
											.into(iconMember);
								}, error -> {
									//Setting the tag (for a new contact request)
									viewMember.setTag(new ContactAccessInfo(member.getAddress()));
								}
						)
		);
		
		//Configuring the color editor
		if(conversationInfo.isGroupChat() && Preferences.getPreferenceAdvancedColor(requireContext())) {
			changeColorButton.setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			changeColorButton.setOnClickListener(view -> showColorDialog(member, member.getColor()));
			changeColorButton.setVisibility(View.VISIBLE);
		} else {
			changeColorButton.setVisibility(View.GONE);
		}
		
		//Setting the click listener
		viewMember.setOnClickListener(view -> {
			//Ignoring if the view has no tag
			if(view.getTag() == null) return;
			
			//Opening the user's contact profile
			startActivity(((ContactAccessInfo) view.getTag()).getIntent());
		});
		
		//Adding the view
		listConversationMembers.addView(viewMember);
		memberViewMap.put(member.getAddress(), viewMember);
	}
	
	/**
	 * Removes a member from the member list view
	 * @param member The member to remove
	 */
	private void removeMemberView(MemberInfo member) {
		//Removing the view
		listConversationMembers.removeView(memberViewMap.get(member.getAddress()));
		memberViewMap.remove(member.getAddress());
		
		//Closing the member's color picker dialog
		if(currentColorPickerDialog != null && currentColorPickerDialogMember == member) {
			currentColorPickerDialog.dismiss();
		}
		
		//Clearing the member's tasks
		clearMemberCD(member.getAddress());
	}
	
	/**
	 * Shows the rename conversation dialog
	 */
	private void showRenameDialog() {
		final CompositeDisposable dialogDisposable = new CompositeDisposable();
		
		//Creating the view
		View dialogView = getLayoutInflater().inflate(R.layout.dialog_renamechat, null);
		TextInputLayout input = dialogView.findViewById(R.id.input);
		input.getEditText().setText(conversationInfo.getTitle());
		//getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
		
		//Updating the hint
		dialogDisposable.add(ConversationBuildHelper.buildMemberTitle(requireContext(), conversationInfo.getMembers()).subscribe(title -> {
			input.getEditText().setHint(title);
		}));
		
		//Showing the dialog
		Dialog groupDialog = new MaterialAlertDialogBuilder(getContext())
				.setView(dialogView)
				.setTitle(R.string.action_renamegroup)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					//Updating the title
					String title = input.getEditText().getText().toString();
					if(title.isEmpty()) title = null;
					
					ConversationActionTask.setConversationTitle(conversationInfo, title).subscribe();
					
					//Dismissing the dialog
					dialog.dismiss();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();
		
		groupDialog.setOnShowListener(dialog -> {
			//Focusing the text view
			input.requestFocus();
			getContext().getSystemService(InputMethodManager.class).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		});
		groupDialog.setOnDismissListener(dialog -> {
			//Clearing background tasks
			dialogDisposable.clear();
			
			//Hiding the keyboard
			getContext().getSystemService(InputMethodManager.class).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
		});
		groupDialog.show();
	}
	
	/**
	 * Shows the color dialog for a certain member
	 * @param member The member to show the dialog for
	 * @param currentColor The default selected color
	 */
	private void showColorDialog(MemberInfo member, int currentColor) {
		//Starting a fragment transaction
		FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
		
		//Removing the previous fragment if it already exists
		Fragment previousFragment = getChildFragmentManager().findFragmentByTag(colorDialogTag);
		if(previousFragment != null) fragmentTransaction.remove(previousFragment);
		fragmentTransaction.addToBackStack(null);
		
		//Creating and showing the dialog fragment
		ColorPickerDialog newFragment = ColorPickerDialog.newInstance(conversationInfo, member, currentColor);
		newFragment.show(fragmentTransaction, colorDialogTag);
		currentColorPickerDialog = newFragment;
		currentColorPickerDialogMember = member;
	}
	
	/**
	 * Gets a composite disposable linked to a specific member
	 */
	private CompositeDisposable getMemberCD(String address) {
		CompositeDisposable compositeDisposable = memberCDMap.get(address);
		if(compositeDisposable != null) return compositeDisposable;
		
		compositeDisposable = new CompositeDisposable();
		memberCDMap.put(address, compositeDisposable);
		return compositeDisposable;
	}
	
	/**
	 * Clears and removes a composite disposable linked to a specific member
	 */
	private void clearMemberCD(String address) {
		CompositeDisposable compositeDisposable = memberCDMap.get(address);
		if(compositeDisposable != null) {
			compositeDisposable.clear();
			memberCDMap.remove(address);
		}
	}
	
	/**
	 * Updates the state of the mute toggle
	 */
	private void updateViewMuted() {
		switchNotifications.setChecked(!conversationInfo.isMuted());
	}
	
	/**
	 * Updates the state of the conversation title view
	 */
	private void updateViewConversationTitle() {
		//Ignoring if this conversation can't be renamed
		if(!isRenameSupported()) return;
		
		//Updating the title
		labelRename.setText(conversationInfo.getTitle());
		if(disposableTitle != null && !disposableTitle.isDisposed()) disposableTitle.dispose();
		fragmentCD.add(
				disposableTitle = ConversationBuildHelper.buildConversationTitle(requireContext(), conversationInfo)
				.subscribe(title -> labelRename.setText(title))
		);
	}
	
	/**
	 * Updates the state of the archive button
	 */
	private void updateViewArchived() {
		buttonArchive.setText(conversationInfo.isArchived() ? R.string.action_unarchive : R.string.action_archive);
		buttonArchive.setIconResource(conversationInfo.isArchived() ? R.drawable.unarchive_outlined : R.drawable.archive_outlined);
	}
	
	/**
	 * Updates the state of the conversation color button and accent switches
	 */
	private void updateViewConversationColor() {
		//Getting the color to use
		int color;
		if(Preferences.getPreferenceAdvancedColor(requireContext())) {
			color = conversationInfo.getConversationColor();
		} else {
			color = ColorHelper.getServiceColor(getResources(), conversationInfo.getServiceHandler(), conversationInfo.getServiceType());
		}
		
		//Updating the conversation color button
		buttonConversationColor.setTextColor(color);
		buttonConversationColor.setIconTint(ColorStateList.valueOf(color));
		buttonConversationColor.setRippleColor(ColorStateList.valueOf(color));
		
		//Updating the notifications switch color
		switchNotifications.setThumbTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0xFFFAFAFA, color}));
		switchNotifications.setTrackTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0x61000000, ColorUtils.setAlphaComponent(color, 97)}));
	}
	
	/**
	 * Updates the state of a member view
	 */
	private void updateViewMemberColor(MemberInfo memberInfo) {
		View memberView = memberViewMap.get(memberInfo.getAddress());
		if(memberView != null) {
			((ImageView) memberView.findViewById(R.id.button_change_color)).setColorFilter(memberInfo.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			((ImageView) memberView.findViewById(R.id.profile_default)).setColorFilter(memberInfo.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
		}
	}
	
	/**
	 * Updates this dialog's state in response to a messaging update
	 */
	private void handleUpdate(ReduxEventMessaging event) {
		//Ignoring if the action is for another conversation
		if(!(event instanceof ReduxEventMessaging.ReduxConversationAction)) return;
		ReduxEventMessaging.ReduxConversationAction conversationEvent = (ReduxEventMessaging.ReduxConversationAction) event;
		if(conversationEvent.getConversationInfo().getLocalID() != conversationInfo.getLocalID()) return;
		
		if(event instanceof ReduxEventMessaging.ConversationMute) {
			conversationInfo.setMuted(((ReduxEventMessaging.ConversationMute) event).isMuted());
			updateViewMuted();
		} else if(event instanceof ReduxEventMessaging.ConversationArchive) {
			conversationInfo.setArchived(((ReduxEventMessaging.ConversationArchive) event).isArchived());
			updateViewArchived();
		} else if(event instanceof ReduxEventMessaging.ConversationColor) {
			conversationInfo.setConversationColor(((ReduxEventMessaging.ConversationColor) event).getColor());
			updateViewConversationColor();
		} else if(event instanceof ReduxEventMessaging.ConversationMemberColor) {
			ReduxEventMessaging.ConversationMemberColor memberColorEvent = (ReduxEventMessaging.ConversationMemberColor) event;
			MemberInfo localMemberInfo = conversationInfo.getMembers().stream().filter(member -> member.getAddress().equals(memberColorEvent.getMemberInfo().getAddress())).findAny().orElse(null);
			if(localMemberInfo != null) {
				localMemberInfo.setColor(memberColorEvent.getColor());
				updateViewMemberColor(localMemberInfo);
			}
		} else if(event instanceof ReduxEventMessaging.ConversationTitle) {
			conversationInfo.setTitle(((ReduxEventMessaging.ConversationTitle) event).getTitle());
			updateViewConversationTitle();
		} else if(event instanceof ReduxEventMessaging.ConversationMember) {
			ReduxEventMessaging.ConversationMember memberEvent = (ReduxEventMessaging.ConversationMember) event;
			if(memberEvent.isJoin()) {
				MemberInfo member = memberEvent.getMember().clone();
				conversationInfo.getMembers().add(member);
				addMemberView(member);
			} else {
				conversationInfo.getMembers().removeIf(member -> member.getAddress().equals(memberEvent.getMember().getAddress()));
				removeMemberView(memberEvent.getMember());
			}
		}
	}
	
	private static class ContactAccessInfo {
		private final Uri accessUri;
		private final String address;
		
		/**
		 * Constructs a new contact access info for an existing contact
		 * @param accessUri The URI of the contact
		 */
		ContactAccessInfo(Uri accessUri) {
			this.accessUri = accessUri;
			address = null;
		}
		
		/**
		 * Constructs a new contact access info for a new contact
		 * @param address The address of the new contact
		 */
		ContactAccessInfo(String address) {
			accessUri = null;
			this.address = address;
		}
		
		/**
		 * Gets the intent to be launched when this item is selected
		 */
		Intent getIntent() {
			if(accessUri != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(accessUri);
				return intent;
			} else if(address != null) {
				Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
				intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
				
				if(AddressHelper.validateEmail(address)) return intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
				else if(AddressHelper.validatePhoneNumber(address)) return intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
			}
			
			return null;
		}
	}
	
	public static class ColorPickerDialog extends DialogFragment {
		//Creating the argument values
		private static final String argKeyConversationID = "conversationID";
		private static final String argKeyMember = "member";
		private static final String argKeyColor = "color";
		
		//Creating the parameter values
		private ConversationInfo conversationInfo;
		private MemberInfo member = null;
		private int selectedColor;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			//Reading the arguments
			conversationInfo = getArguments().getParcelable(argKeyConversationID);
			if(getArguments().containsKey(argKeyMember)) member = getArguments().getParcelable(argKeyMember);
			selectedColor = getArguments().getInt(argKeyColor);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			//Configuring the dialog
			getDialog().setTitle(member == null ? R.string.action_editconversationcolor : R.string.action_editcontactcolor);
			
			//Inflating the view
			View dialogView = inflater.inflate(R.layout.dialog_colorpicker, container, false);
			ViewGroup contentViewGroup = dialogView.findViewById(R.id.colorpicker_itemview);
			
			int padding = ResourceHelper.dpToPx(24);
			contentViewGroup.setPadding(padding, padding, padding, padding);
			
			//Adding the elements
			for(int i = 0; i < ConversationColorHelper.standardUserColors.length; i++) {
				//Getting the color
				final int standardColor = ConversationColorHelper.standardUserColors[i];
				
				//Inflating the layout
				View item = inflater.inflate(R.layout.dialog_colorpicker_item, contentViewGroup, false);
				
				//Configuring the layout
				ImageView colorView = item.findViewById(R.id.colorpickeritem_color);
				colorView.setColorFilter(standardColor);
				
				final boolean isSelectedColor = selectedColor == standardColor;
				if(isSelectedColor) item.findViewById(R.id.colorpickeritem_selection).setVisibility(View.VISIBLE);
				
				//Setting the click listener
				colorView.setOnClickListener(view -> {
					//Updating the color
					if(!isSelectedColor) {
						if(member == null) {
							ConversationActionTask.setConversationColor(conversationInfo, standardColor).subscribe();
							if(!conversationInfo.isGroupChat()) {
								for(MemberInfo member : conversationInfo.getMembers()) ConversationActionTask.setConversationMemberColor(conversationInfo, member, standardColor).subscribe();
							}
						}
						else ConversationActionTask.setConversationMemberColor(conversationInfo, member, standardColor).subscribe();
					}
					
					getDialog().dismiss();
				});
				
				//Adding the view to the layout
				contentViewGroup.addView(item);
			}
			
			//Returning the view
			return dialogView;
		}
		
		static ColorPickerDialog newInstance(ConversationInfo conversationInfo, MemberInfo memberInfo, int selectedColor) {
			//Creating the instance
			ColorPickerDialog instance = new ColorPickerDialog();
			
			//Adding the member info
			Bundle bundle = new Bundle();
			bundle.putParcelable(argKeyConversationID, conversationInfo);
			bundle.putParcelable(argKeyMember, memberInfo);
			bundle.putInt(argKeyColor, selectedColor);
			instance.setArguments(bundle);
			
			//Returning the instance
			return instance;
		}
	}
}