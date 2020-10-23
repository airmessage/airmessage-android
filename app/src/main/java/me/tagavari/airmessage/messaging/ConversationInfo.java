package me.tagavari.airmessage.messaging;

import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.collection.LongSparseArray;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.util.BiConsumer;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.BitmapCacheHelper;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.ShortcutUtils;

public class ConversationInfo implements Serializable {
	private static final long serialVersionUID = 0;
	
	//Creating the reference values
	public static final Integer[] standardUserColors = {
			0xFFFF1744, //Red
			0xFFF50057, //Pink
			0xFFB317CF, //Purple
			0xFF703BE3, //Dark purple
			0xFF3D5AFE, //Indigo
			0xFF2979FF, //Blue
			0xFF00B0FF, //Light blue
			0xFF00B8D4, //Cyan
			0xFF00BFA5, //Teal
			0xFF00C853, //Green
			0xFF5DD016, //Light green
			0xFF99CC00, //Lime green
			0xFFF2CC0D, //Yellow
			0xFFFFC400, //Amber
			0xFFFF9100, //Orange
			0xFFFF3D00, //Deep orange
			//0xFF795548, //Brown
			//0xFF607D8B, //Blue grey
	};
	public static final int backupUserColor = 0xFF607D8B; //Blue grey
	private static final int maxUsersToDisplay = 4;
	
	public static final int serviceHandlerAMBridge = 0; //AirMessage / iMessage bridge
	public static final int serviceHandlerSystemMessaging = 1; //SMS and MMS
	
	public static final String serviceTypeAppleMessage = "iMessage";
	public static final String serviceTypeAppleTextMessageForwarding = "SMS";
	
	public static final String serviceTypeSystemMMSSMS = "MMSSMS"; //MMS and SMS
	public static final String serviceTypeSystemRCS= "RCS"; //Rich communication services, Google Chat
	
	//Creating the static values
	private static SelectionSource selectionSource = id -> false;
	
	//Creating the values
	private long localID;
	private String guid;
	private long externalID;
	private ConversationState conversationState;
	private int serviceHandler;
	private String service;
	private transient WeakReference<ArrayList<ConversationItem>> conversationItemsReference = null;
	private transient WeakReference<ArrayList<MessageInfo>> ghostMessagesReference = null;
	private ArrayList<MemberInfo> conversationMembers;
	//private transient WeakReference<Messaging.RecyclerAdapter> arrayAdapterReference = null;
	private transient ActivityCallbacks activityCallbacks = null;
	//private transient View view;
	//private transient ViewGroup iconView = null;
	private String title = null;
	private transient int unreadMessageCount = 0;
	private boolean isArchived = false;
	private boolean isMuted = false;
	private int conversationColor = 0xFF000000; //Black
	private transient WeakReference<MessageInfo> activityStateTargetReadReference = null;
	private transient WeakReference<MessageInfo> activityStateTargetDeliveredReference = null;
	private transient int currentUserViewIndex;
	private transient LightConversationItem lastItem;
	private transient String draftMessage;
	private transient ArrayList<DraftFile> draftFiles;
	private transient long draftUpdateTime;
	
	//private int currentUserViewIndex = -1;
	private transient Constants.ViewHolderSource<ItemViewHolder> viewHolderSource = null;
	
	private transient LongSparseArray<AttachmentInfo> localIDAttachmentMap;
	
	public static void setSelectionSource(SelectionSource source) {
		selectionSource = source;
	}
	
	public ConversationInfo(long localID, ConversationState conversationState) {
		//Setting the local ID and state
		this.localID = localID;
		this.conversationState = conversationState;
		
		//Instantiating the lists
		conversationMembers = new ArrayList<>();
		draftFiles = new ArrayList<>();
	}
	
	public ConversationInfo(long localID, String guid, ConversationState conversationState, int serviceHandler) {
		//Setting the identifiers and the state
		this.localID = localID;
		this.guid = guid;
		this.conversationState = conversationState;
		this.serviceHandler = serviceHandler;
		
		//Instantiating the lists
		conversationMembers = new ArrayList<>();
		draftFiles = new ArrayList<>();
	}
	
	public ConversationInfo(long localID, String guid, ConversationState conversationState, int serviceHandler, String service, ArrayList<MemberInfo> conversationMembers, String title, int unreadMessageCount, int conversationColor, String draftMessage, ArrayList<DraftFile> draftFiles, long draftUpdateTime) {
		//Setting the values
		this.guid = guid;
		this.localID = localID;
		this.conversationState = conversationState;
		this.serviceHandler = serviceHandler;
		this.service = service;
		this.conversationMembers = conversationMembers;
		this.title = title;
		this.unreadMessageCount = unreadMessageCount;
		this.conversationColor = conversationColor;
		this.draftMessage = draftMessage;
		this.draftFiles = draftFiles;
		this.draftUpdateTime = draftUpdateTime;
	}
	
	public void setConversationLists(ArrayList<ConversationItem> items, ArrayList<MessageInfo> ghostItems) {
		conversationItemsReference = new WeakReference<>(items);
		ghostMessagesReference = new WeakReference<>(ghostItems);
	}
		
		/* View createView(Context context, View convertView, ViewGroup parent) {
			//Inflating the layout if the view can't be recycled
			if(convertView == null)
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_conversation, parent, false);
			
			//Setting the flags
			convertView.findViewById(R.id.flag_muted).setVisibility(isMuted ? View.VISIBLE : View.GONE);
			
			//Returning if the conversation has no members
			if(conversationMembers.isEmpty()) return convertView;
			
			//Setting the profile
			currentUserViewIndex = -1;
			updateViewUser(context, convertView);
			updateSelected(convertView);
			
			//Returning if the last message is invalid
			if(lastItem == null) return convertView;
			
			//Updating the view
			updateView(context, convertView);
			
			//Returning the view
			return convertView;
		}
		
		View createSimpleView(Context context, View convertView, ViewGroup parent, Constants.ViewSource viewSource) {
			//Inflating the layout if the view can't be recycled
			if(convertView == null)
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_conversation_simple, parent, false);
			
			final View finalView = convertView;
			
			//Setting the title
			((TextView) convertView.findViewById(R.id.title)).setText(buildTitleDirect(context, title, getConversationMembersAsArray()));
			buildTitle(context, (title, wasTasked) -> {
				//Setting the title
				View view = wasTasked ? viewSource.get() : finalView;
				if(view == null) return;
				((TextView) view.findViewById(R.id.title)).setText(title);
			});
			
			//Updating the users
			updateViewUser(context, convertView);
			
			//Returning the view
			return convertView;
		} */
	
	public void bindView(ItemViewHolder viewHolder, Context context) {
		//Setting the flags
		viewHolder.flagMuted.setVisibility(isMuted ? View.VISIBLE : View.GONE);
		viewHolder.flagDraft.setVisibility(draftMessage != null || !draftFiles.isEmpty() ? View.VISIBLE : View.GONE);
		
		//Setting the profile
		currentUserViewIndex = updateViewUser(context, viewHolder);
		updateSelected(viewHolder);
		
		//Returning if the last message is invalid
		//if(lastItem == null) return convertView;
		
		//Updating the view
		updateView(viewHolder, context);
	}
	
	public void bindViewOnce(ItemViewHolder viewHolder, Context context) {
		//Setting the flags
		viewHolder.flagMuted.setVisibility(isMuted ? View.VISIBLE : View.GONE);
		viewHolder.flagDraft.setVisibility(draftMessage != null || !draftFiles.isEmpty() ? View.VISIBLE : View.GONE);
		
		//Building the profile view
		updateViewUser(context, viewHolder);
		
		//Updating the view
		updateView(viewHolder, context);
	}
	
	public void bindSimpleView(Context context, SimpleItemViewHolder viewHolder, Constants.ViewHolderSource<SimpleItemViewHolder> viewHolderSource) {
		//Setting the title
		viewHolder.conversationTitle.setText(buildTitleDirect(context, title, getConversationMembersAsArray()));
		buildTitle(context, (title, wasTasked) -> {
			//Setting the title
			SimpleItemViewHolder taskedViewHolder = wasTasked ? viewHolderSource.get() : viewHolder;
			if(taskedViewHolder == null) return;
			taskedViewHolder.conversationTitle.setText(title);
		});
		
		//Updating the users
		updateViewUser(context, viewHolder);
	}
	
	public void generateShortcutIcon(Context context, Constants.TaskedResultCallback<Bitmap> callback) {
		//Returning if the conversation has no members
		if(conversationMembers.isEmpty()) {
			callback.onResult(null, false);
			return;
		}
		
		//Getting the view
		ViewGroup iconGroup = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.layout_conversationicon, null);
		
		//Getting the view data
		int currentUserViewIndex = Math.min(conversationMembers.size() - 1, maxUsersToDisplay - 1);
		View viewAtIndex = iconGroup.getChildAt(currentUserViewIndex);
		ViewGroup iconView = (ViewGroup) ((ViewStub) viewAtIndex).inflate();
		
		//Hiding the other views
		//for(int i = 0; i < maxUsersToDisplay; i++) iconGroup.getChildAt(i).setVisibility(i == currentUserViewIndex ? View.VISIBLE : View.GONE);
		
		//Setting the icons
		int displayMemberCount = iconView.getChildCount();
		Constants.ValueWrapper<Integer> completionCount = new Constants.ValueWrapper<>(0);
		
		for(int i = 0; i < displayMemberCount; i++) {
			//Getting the member info
			MemberInfo member = getConversationMembers().get(i);
			//Getting the child view
			View child = iconView.getChildAt(i);
			
			//Resetting the contact image
			ImageView imageProfile = child.findViewById(R.id.profile_image);
			imageProfile.setImageBitmap(null);
			
			//Setting the default profile tint
			ImageView defaultProfile = child.findViewById(R.id.profile_default);
			defaultProfile.setVisibility(View.VISIBLE);
			defaultProfile.setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			
			//Assigning the user info
			String contactName = member.getName();
			MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context.getApplicationContext(), contactName, contactName, new BitmapCacheHelper.ImageDecodeResult() {
				@Override
				public void onImageMeasured(int width, int height) {}
				
				@Override
				public void onImageDecoded(Bitmap result, boolean wasTasked) {
					//Checking if the result is valid
					if(result != null) {
						//Hiding the default view
						defaultProfile.setVisibility(View.INVISIBLE);
						
						//Setting the bitmap
						imageProfile.setImageBitmap(result);
					}
					
					//Adding to the count
					completionCount.value++;
					
					//Checking if all images have been processed
					if(completionCount.value == displayMemberCount) {
						//Rendering the image and returning the result
						callback.onResult(Constants.loadBitmapFromView(iconGroup), wasTasked);
					}
				}
			});
		}
	}
	
	@RequiresApi(api = Build.VERSION_CODES.P)
	public void generatePersonList(Context context, Constants.TaskedResultCallback<Person[]> callback) {
		//Returning if the conversation has no members
		if(conversationMembers.isEmpty()) {
			callback.onResult(new Person[0], false);
			return;
		}
		
		//Getting member info for each member
		final int totalMemberCount = conversationMembers.size();
		final Constants.ValueWrapper<Integer> completionCount = new Constants.ValueWrapper<>(0);
		final Person[] personArray = new Person[totalMemberCount];
		final Constants.ValueWrapper<Boolean> totalWasTasked = new Constants.ValueWrapper<>(false);
		for(ListIterator<MemberInfo> iterator = conversationMembers.listIterator(); iterator.hasNext();) {
			int index = iterator.nextIndex();
			MemberInfo member = iterator.next();
			
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, member.getName(), new UserCacheHelper.UserFetchResult() {
				void onCompleted() {
					callback.onResult(personArray, totalWasTasked.value);
				}
				
				@Override
				public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					if(wasTasked) totalWasTasked.value = true;
					
					if(userInfo != null) {
						//Fetching the user's icon
						MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, member.getName(), member.getName(), new BitmapCacheHelper.ImageDecodeResult() {
							@Override
							public void onImageMeasured(int width, int height) {
							
							}
							
							@Override
							public void onImageDecoded(Bitmap result, boolean wasTasked) {
								if(wasTasked) totalWasTasked.value = true;
								
								//Adding the person to the list
								personArray[index] = new Person.Builder()
										.setName(userInfo.getContactName())
										.setKey(userInfo.getLookupKey())
										.setIcon(result == null ? null : Icon.createWithBitmap(result))
										.build();
								
								//Checking if all members have been processed
								completionCount.value++;
								if(completionCount.value == totalMemberCount) onCompleted();
							}
						});
					} else {
						//Adding the person's address to the list
						personArray[index] = new Person.Builder()
								.setName(Constants.formatAddress(member.getName()))
								.build();
						
						//Checking if all members have been processed
						completionCount.value++;
						if(completionCount.value == totalMemberCount) onCompleted();
					}
				}
			});
		}
	}
	
	public void setViewHolderSource(Constants.ViewHolderSource<ItemViewHolder> viewHolderSource) {
		this.viewHolderSource = viewHolderSource;
	}
	
	private ItemViewHolder getViewHolder() {
		if(viewHolderSource == null) return null;
		return viewHolderSource.get();
	}
	
	public void updateView(Context context) {
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) updateView(itemView, context);
	}
	
	private void updateView(ItemViewHolder itemView, Context context) {
		//Setting the title
		itemView.conversationTitle.setText(buildTitleDirect(context, title, getConversationMembersAsArray()));
		updateUnreadStatus(itemView, context);
		
		buildTitle(context, (title, wasTasked) -> {
			//Setting the title
			ItemViewHolder viewHolder = wasTasked ? getViewHolder() : itemView;
			if(viewHolder == null) return;
			viewHolder.conversationTitle.setText(title);
		});
		
		if(lastItem != null) {
			itemView.conversationMessage.setText(lastItem.getMessage());
			updateTime(context, itemView);
		} else {
			itemView.conversationMessage.setText(R.string.part_unknown);
			itemView.conversationTime.setText(R.string.part_unknown);
			itemView.conversationTime.setTextColor(Constants.resolveColorAttr(context, android.R.attr.textColorSecondary));
		}
	}
	
	public void updateUnreadStatus(Context context) {
		//Calling the overload method
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) updateUnreadStatus(itemView, context);
	}
	
	private void updateUnreadStatus(ItemViewHolder itemView, Context context) {
		if(unreadMessageCount > 0) {
			itemView.conversationTitle.setTypeface(null, Typeface.BOLD);
			itemView.conversationTitle.setTextColor(itemView.conversationTitle.getResources().getColor(R.color.colorPrimary, null));
			
			itemView.conversationMessage.setTypeface(null, Typeface.BOLD);
			itemView.conversationMessage.setTextColor(Constants.resolveColorAttr(itemView.conversationMessage.getContext(), android.R.attr.textColorPrimary));
			
			itemView.conversationUnread.setVisibility(View.VISIBLE);
			itemView.conversationUnread.setText(Constants.intToFormattedString(context.getResources(), unreadMessageCount));
		} else {
			itemView.conversationTitle.setTypeface(null, Typeface.NORMAL);
			itemView.conversationTitle.setTextColor(Constants.resolveColorAttr(itemView.conversationTitle.getContext(), android.R.attr.textColorPrimary));
			
			itemView.conversationMessage.setTypeface(null, Typeface.NORMAL);
			itemView.conversationMessage.setTextColor(Constants.resolveColorAttr(itemView.conversationMessage.getContext(), android.R.attr.textColorSecondary));
			
			itemView.conversationUnread.setVisibility(View.GONE);
		}
	}
	
	public void updateTime(Context context) {
		//Calling the overload method
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) updateTime(context, itemView);
	}
	
	private void updateTime(Context context, ItemViewHolder itemView) {
		//Returning if the last item is invalid
		if(lastItem == null) {
			itemView.conversationTime.setText(R.string.part_unknown);
			return;
		}
		
		if(lastItem.isError()) {
			//Setting "not sent"
			itemView.conversationTime.setText(R.string.message_senderror);
			itemView.conversationTime.setTextColor(context.getResources().getColor(R.color.colorError, null));
		} else {
			//Setting the time
			itemView.conversationTime.setText(getLastUpdateStatusTime(context, lastItem.getDate()));
			itemView.conversationTime.setTextColor(Constants.resolveColorAttr(context, android.R.attr.textColorSecondary));
		}
	}
	
	private static String getLastUpdateStatusTime(Context context, long date) {
		long timeNow = System.currentTimeMillis();
		long timeDiff = timeNow - date;
		
		//Just now
		if(timeDiff < ConversationUtils.conversationJustNowTimeMillis) return context.getResources().getString(R.string.time_now);
		
		//Within the hour
		if(timeDiff < 60 * 60 * 1000) return context.getResources().getString(R.string.time_minutes, (int) (timeDiff / (60 * 1000)));
		
		Calendar thenCal = Calendar.getInstance();
		thenCal.setTimeInMillis(date);
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTimeInMillis(timeNow);
		
		//Within the day (14:11)
		if(thenCal.get(Calendar.ERA) == nowCal.get(Calendar.ERA) && thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == thenCal.get(Calendar.DAY_OF_YEAR)) return DateFormat.getTimeFormat(context).format(thenCal.getTime());
		
		//Within the week (Sun)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.DAY_OF_YEAR, -7); //Today (now) -> One week ago
			if(Constants.compareCalendarDates(thenCal, compareCal) > 0) return DateFormat.format("EEE", thenCal).toString();
		}
		
		//Within the year (Dec 9)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.YEAR, -1); //Today (now) -> One year ago
			if(Constants.compareCalendarDates(thenCal, compareCal) > 0) return DateFormat.format(context.getResources().getString(R.string.dateformat_withinyear), thenCal).toString();//return DateFormat.format("MMM d", thenCal).toString();
		}
		
		//Anytime (Dec 2018)
		return DateFormat.format(context.getString(R.string.dateformat_outsideyear_simple), thenCal).toString();
	}
	
	public void updateViewUser(Context context) {
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) currentUserViewIndex = updateViewUser(context, itemView);
	}
	
	private int updateViewUser(Context context, BaseViewHolder itemView) {
		//Returning if the conversation has no members
		if(conversationMembers.isEmpty()) return -1;
		
		//Getting the view data
		int currentUserViewIndex = Math.min(conversationMembers.size() - 1, maxUsersToDisplay - 1);
		View viewAtIndex = itemView.iconGroup.getChildAt(currentUserViewIndex);
		ViewGroup iconView;
		if(viewAtIndex instanceof ViewStub)
			iconView = (ViewGroup) ((ViewStub) viewAtIndex).inflate();
		else iconView = (ViewGroup) viewAtIndex;
		
		//Hiding the other views
		for(int i = 0; i < maxUsersToDisplay; i++)
			itemView.iconGroup.getChildAt(i).setVisibility(i == currentUserViewIndex ? View.VISIBLE : View.GONE);
		
		//Setting the icons
		for(int i = 0; i < iconView.getChildCount(); i++) {
			//Getting the member info
			MemberInfo member = getConversationMembers().get(i);
			//Getting the child view
			View child = iconView.getChildAt(i);
			
			//Resetting the contact image
			((ImageView) child.findViewById(R.id.profile_image)).setImageBitmap(null);
			
			//Setting the default profile tint
			ImageView defaultProfile = child.findViewById(R.id.profile_default);
			defaultProfile.setVisibility(View.VISIBLE);
			defaultProfile.setColorFilter(member.getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
			
			//Assigning the user info
			String contactName = member.getName();
			final int finalIndex = i;
			MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context.getApplicationContext(), contactName, contactName, new BitmapCacheHelper.ImageDecodeResult() {
				@Override
				public void onImageMeasured(int width, int height) {}
				
				@Override
				public void onImageDecoded(Bitmap result, boolean wasTasked) {
					//Returning if the result is invalid
					if(result == null) return;
					
					BaseViewHolder viewHolder = wasTasked ? getViewHolder() : itemView;
					if(viewHolder == null) return;
					
					//Getting the icon view
					View viewAtIndex = viewHolder.iconGroup.getChildAt(currentUserViewIndex);
					ViewGroup iconGroup;
					if(viewAtIndex instanceof ViewStub) iconGroup = (ViewGroup) ((ViewStub) viewAtIndex).inflate();
					else iconGroup = (ViewGroup) viewAtIndex;
					View iconView = iconGroup.getChildAt(finalIndex);
					
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
		}
		
		//Returning the current user view index
		return currentUserViewIndex;
	}
	
	public void clearMessages() {
		//Getting the list
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return;
		
		//Clearing the list
		conversationItems.clear();
		
		//Resetting the active message info state listing
		activityStateTargetReadReference = null;
		activityStateTargetDeliveredReference = null;
		
		//Updating the adapter
		ActivityCallbacks updater = getActivityCallbacks();
		if(updater != null) updater.listUpdateFully();
	}
	
	public void addDraftFileUpdate(Context context, DraftFile draft, long updateTime) {
		draftFiles.add(draft);
		if(updateTime != -1) registerDraftChange(context, updateTime);
	}
	
	public void removeDraftFileUpdate(Context context, DraftFile draft, long updateTime) {
		draftFiles.remove(draft);
		if(updateTime != -1) registerDraftChange(context, updateTime);
	}
	
	public void clearDraftsUpdate(Context context) {
		draftMessage = null;
		draftFiles.clear();
		registerDraftChange(context, -1);
	}
	
	public List<DraftFile> getDrafts() {
		return draftFiles;
	}
	
	public void setDraftMessageUpdate(Context context, String message, long updateTime) {
		draftMessage = message;
		if(updateTime != -1) registerDraftChange(context, updateTime);
	}
	
	public String getDraftMessage() {
		return draftMessage;
	}
	
	private void registerDraftChange(Context context, long updateTime) {
		//Setting the update time
		draftUpdateTime = updateTime;
		
		//Updating the last item
		updateLastItem(context);
		
		//Updating the view
		updateView(context);
		
		//Re-sorting the conversation
		ConversationUtils.sortConversation(this);
		
		//Updating the list
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
	}
	
	public ArrayList<ConversationItem> getConversationItems() {
		return conversationItemsReference == null ? null : conversationItemsReference.get();
	}
	
	public ArrayList<MessageInfo> getGhostMessages() {
		return ghostMessagesReference == null ? null : ghostMessagesReference.get();
	}
	
	public boolean isDataAvailable() {
		return getConversationItems() != null && getGhostMessages() != null;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public void setLocalID(long localID) {
		this.localID = localID;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public void setGuid(String guid) {
		this.guid = guid;
	}
	
	public long getExternalID() {
		return externalID;
	}
	
	public void setExternalID(long externalID) {
		this.externalID = externalID;
	}
	
	public int getServiceHandler() {
		return serviceHandler;
	}
	
	public String getService() {
		return service;
	}
	
	public void setService(String service) {
		this.service = service;
	}
	
	public int getUnreadMessageCount() {
		return unreadMessageCount;
	}
	
	public void setUnreadMessageCount(int unreadMessageCount) {
		//Setting the value
		this.unreadMessageCount = unreadMessageCount;
		
		//Telling the activity
		if(activityCallbacks != null) activityCallbacks.chatUpdateUnreadCount();
	}
	
	public boolean isArchived() {
		return isArchived;
	}
	
	public void setArchived(boolean archived) {
		isArchived = archived;
	}
	
	public boolean isMuted() {
		return isMuted;
	}
	
	public void setMuted(boolean muted) {
		//Setting the muted variable
		isMuted = muted;
		
		//Getting the view
		ItemViewHolder itemView = getViewHolder();
		if(itemView == null) return;
		
		//Updating the view
		itemView.flagMuted.setVisibility(isMuted ? View.VISIBLE : View.GONE);
	}
	
	public LongSparseArray<AttachmentInfo> getLocalIDAttachmentMap() {
		return localIDAttachmentMap;
	}
	
	public void setLocalIDAttachmentMap(LongSparseArray<AttachmentInfo> localIDAttachmentMap) {
		this.localIDAttachmentMap = localIDAttachmentMap;
	}
	
	public MessageInfo getActivityStateTargetRead() {
		if(activityStateTargetReadReference == null) return null;
		return activityStateTargetReadReference.get();
	}
	
	public void setActivityStateTargetRead(MessageInfo activityStateTarget) {
		activityStateTargetReadReference = new WeakReference<>(activityStateTarget);
	}
	
	public MessageInfo getActivityStateTargetDelivered() {
		if(activityStateTargetDeliveredReference == null) return null;
		return activityStateTargetDeliveredReference.get();
	}
	
	public void setActivityStateTargetDelivered(MessageInfo activityStateTarget) {
		activityStateTargetDeliveredReference = new WeakReference<>(activityStateTarget);
	}
	
	public void tryActivityStateTarget(MessageInfo activityStateTarget, boolean update, Context context) {
		//Returning if the item is incoming
		if(!activityStateTarget.isOutgoing()) return;
		
		//Checking if the item is delivered
		if(activityStateTarget.getMessageState() == Constants.messageStateCodeDelivered) {
			//Getting the current item
			MessageInfo activeMessageDelivered = getActivityStateTargetDelivered();
			
			//Replacing the item if it is invalid
			if(activeMessageDelivered == null) {
				setActivityStateTargetDelivered(activityStateTarget);
				
				//Updating the view
				if(update) activityStateTarget.updateActivityStateDisplay(context);
			} else {
				//Replacing the item if the new one is more recent
				if(ConversationUtils.compareConversationItems(activityStateTarget, activeMessageDelivered) >= 0) {
					setActivityStateTargetDelivered(activityStateTarget);
					
					//Updating the views
					if(update) {
						activityStateTarget.updateActivityStateDisplay(context);
						activeMessageDelivered.updateActivityStateDisplay(context);
					}
				}
			}
		}
		//Otherwise checking if the item is read
		else if(activityStateTarget.getMessageState() == Constants.messageStateCodeRead) {
			//Getting the current item
			MessageInfo activeMessageDelivered = getActivityStateTargetDelivered();
			MessageInfo activeMessageRead = getActivityStateTargetRead();
			
			//Replacing the item if it is invalid
			if(activeMessageRead == null) {
				setActivityStateTargetDelivered(activityStateTarget);
				setActivityStateTargetRead(activityStateTarget);
				
				//Updating the view
				if(update) {
					activityStateTarget.updateActivityStateDisplay(context);
					//if(activeMessageRead != null) activeMessageRead.updateActivityStateDisplay(context);
				}
			} else {
				//Replacing the item if the new one is more recent
				if(ConversationUtils.compareConversationItems(activityStateTarget, activeMessageRead) >= 0 &&
				   (activityStateTarget.getMessageState() == Constants.messageStateCodeDelivered || activityStateTarget.getMessageState() == Constants.messageStateCodeRead)) {
					setActivityStateTargetDelivered(activityStateTarget);
					setActivityStateTargetRead(activityStateTarget);
					
					//Updating the views
					if(update) {
						activityStateTarget.updateActivityStateDisplay(context);
						activeMessageRead.updateActivityStateDisplay(context);
						if(activeMessageDelivered != null && activeMessageDelivered != activeMessageRead) activeMessageDelivered.updateActivityStateDisplay(context);
					}
				}
			}
		}
	}
	
	public void replaceConversationItems(Context context, List<ConversationItem> sortedList) {
		//Returning if there are no items
		if(sortedList.isEmpty()) return;
		
		//Getting the lists
		List<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return;
		List<MessageInfo> ghostMessages = getGhostMessages();
		if(ghostMessages == null) return;
		
		//Adding the items
		conversationItems.clear();
		conversationItems.addAll(sortedList);
		
		//Finding the ghost items
		ghostMessages.clear();
		for(ConversationItem conversationItem : sortedList) {
			if(!(conversationItem instanceof MessageInfo)) continue;
			MessageInfo messageInfo = (MessageInfo) conversationItem;
			if(messageInfo.getMessageState() != Constants.messageStateCodeGhost) continue;
			ghostMessages.add(messageInfo);
		}
		
		//Updating the last item
		updateLastItem(context);
		//conversationItems.get(conversationItems.size() - 1).toLightConversationItem(context, result -> lastItem = result);
		
		//Updating the adapter
		ActivityCallbacks updater = getActivityCallbacks();
		if(updater != null) updater.listUpdateFully();
		
		//Updating the view
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) updateView(itemView, context);
	}
	
	public LightConversationItem getLastItem() {
		//Returning the last conversation item
		return lastItem;
	}
	
	public boolean trySetLastItem(LightConversationItem item, boolean force) {
		if(force || lastItem == null || (ConversationUtils.compareConversationItems(lastItem, item) <= 0 && !lastItem.isPinned())) {
			lastItem = item;
			return true;
		} else return false;
	}
	
	private static boolean checkError(ConversationItem conversationItem) {
		return conversationItem instanceof MessageInfo && ((MessageInfo) conversationItem).hasError();
	}
	
	public void trySetLastItemUpdate(Context context, ConversationItem lastConversationItem, boolean force) {
		//Setting the last item
		LightConversationItem item = new LightConversationItem("", lastConversationItem.getDate(), lastConversationItem.getLocalID(), lastConversationItem.getDate(), checkError(lastConversationItem));
		if(trySetLastItem(item, force)) lastConversationItem.getSummary(context, (wasTasked, result) -> item.setMessage((String) result));
	}
	
	public void updateLastItem(Context context) {
		//Getting the last conversation item
		ConversationItem lastConversationItem = null;
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems != null && !conversationItems.isEmpty()) lastConversationItem = conversationItems.get(conversationItems.size() - 1);
		
		//Getting if there is a draft message available
		boolean draftAvailable = draftMessage != null || !draftFiles.isEmpty();
		
		//Returning if there are no items in the conversation
		if(lastConversationItem == null && !draftAvailable) return;
		
		//Checking if there is a draft message available, and it was updated more recently than the last item in the conversation
		if(draftAvailable && (lastConversationItem == null || draftUpdateTime > lastConversationItem.getDate())) {
			//Checking if there is a draft message
			if(draftMessage != null) {
				lastItem = new LightConversationItem(context.getResources().getString(R.string.prefix_draft, draftMessage), draftUpdateTime, true);
			}
			//Setting the draft file list message
			else if(!draftFiles.isEmpty()) {
				//Converting the draft list to a string resource list
				ArrayList<Integer> draftStringRes = new ArrayList<>();
				for(DraftFile draft : draftFiles) draftStringRes.add(ConversationUtils.getNameFromContent(draft.getFileType(), draft.getFileName()));
				
				String summary;
				if(draftStringRes.size() == 1) summary = context.getResources().getString(draftStringRes.get(0));
				else summary = context.getResources().getQuantityString(R.plurals.message_multipleattachments, draftStringRes.size(), draftStringRes.size());
				lastItem = new LightConversationItem(context.getResources().getString(R.string.prefix_draft, summary), draftUpdateTime, true);
			}
		} else {
			//Setting the last conversation item
			lastItem = new LightConversationItem("", lastConversationItem.getDate(), lastConversationItem.getLocalID(), lastConversationItem.getServerID(), checkError(lastConversationItem));
			lastConversationItem.getSummary(context, new Constants.ResultCallback<String>() {
				@Override
				public void onResult(boolean wasTasked, String result) {
					lastItem.setMessage(result);
				}
			});
		}
	}
	
	public boolean addConversationItems(Context context, List<ConversationItem> list) {
		//Getting the lists
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return false;
		ArrayList<MessageInfo> ghostMessages = getGhostMessages();
		if(ghostMessages == null) return false;
		
		//Getting the adapter updater
		ActivityCallbacks updater = getActivityCallbacks();
		
		//Creating the tracking lists
		List<ConversationItem> updateList = new ArrayList<>(list);
		List<ConversationItem> sortQueue = new ArrayList<>();
		List<ConversationItemMoveRecord> movedList = new ArrayList<>();
		List<ConversationItem> newItems = new ArrayList<>(); //New items that aren't replacing ghost messages
		//ConversationItem latestNewMessage = null; //Most recent message that isn't replacing a ghost message
		
		//Iterating over the conversation items
		boolean messageReplaced;
		for(ConversationItem conversationItem : list) {
			//Defaulting to the message as not replaced
			messageReplaced = false;
			
			//Checking if the item is a message
			if(conversationItem instanceof MessageInfo) {
				MessageInfo messageInfo = (MessageInfo) conversationItem;
				
				if(messageInfo.isOutgoing() && messageInfo.getMessageState() != Constants.messageStateCodeGhost) {
					//Scanning the ghost items
					if(messageInfo.getMessageText() != null && messageInfo.getAttachments().isEmpty()) {
						for(ListIterator<MessageInfo> listIterator = ghostMessages.listIterator(); listIterator.hasNext();) {
							//Getting the item
							MessageInfo ghostMessage = listIterator.next();
							
							//Skipping the remainder of the iteration if the item doesn't match
							if(ghostMessage.getMessageText() == null || !messageInfo.getMessageText().equals(ghostMessage.getMessageText())) continue;
							
							//Updating the ghost item
							ghostMessage.setServerID(messageInfo.getServerID());
							ghostMessage.setGuid(messageInfo.getGuid());
							ghostMessage.setDate(messageInfo.getDate());
							ghostMessage.setMessageState(messageInfo.getMessageState());
							ghostMessage.setErrorCode(messageInfo.getErrorCode());
							ghostMessage.setDateRead(messageInfo.getDateRead());
							ghostMessage.updateViewProgressState();
							ghostMessage.animateGhostStateChanges();
							
							//Adding the item to the reinsert queue
							sortQueue.add(ghostMessage);
								
								/* //Re-sorting the item
								{
									int originalIndex = conversationItems.indexOf(ghostMessage);
									conversationItems.remove(ghostMessage);
									int newIndex = insertConversationItem(ghostMessage, context, false);
									
									//Updating the adapter
									if(originalIndex != newIndex) {
										//if(updater != null) updater.listUpdateMove(originalIndex, newIndex);
										movedList.add(new ConversationItemMoveRecord(originalIndex, ghostMessage));
									}
								} */
							
							//Replacing the item in the final list
							updateList.set(list.indexOf(conversationItem), ghostMessage);
							
							//Updating the item's relations
							//addConversationItemRelation(this, conversationItems, ghostMessage, context, true);
							
							//Setting the message as replaced
							messageReplaced = true;
							
							//Removing the item from the ghost list
							listIterator.remove();
							
							//Breaking from the loop
							break;
						}
					} else if(!messageInfo.getAttachments().isEmpty()) {
						//Creating the tracking values
						MessageInfo sharedMessageInfo = null;
						List<Long> replacedAttachmentIDList = new ArrayList<>();
						List<AttachmentInfo> unmatchedAttachments = new ArrayList<>();
						
						//Iterating over the attachments
						for(AttachmentInfo attachmentInfo : messageInfo.getAttachments()) {
							//Checking if the attachment has no checksum
							if(attachmentInfo.getFileChecksum() == null) {
								//Queuing the attachment if no message has been found
								if(sharedMessageInfo == null) unmatchedAttachments.add(attachmentInfo);
									//Otherwise adding the attachment directly
								else sharedMessageInfo.addAttachment(attachmentInfo);
							}
							
							//Iterating over the ghost messages
							for(ListIterator<MessageInfo> listIterator = ghostMessages.listIterator(); listIterator.hasNext();) {
								//Getting the item
								MessageInfo ghostMessage = listIterator.next();
								
								//Skipping the remainder of the iteration if there are no matching attachments
								if(ghostMessage.getAttachments().isEmpty()) continue;
								AttachmentInfo matchingAttachment = null;
								for(AttachmentInfo ghostAttachment : ghostMessage.getAttachments()) {
									if(replacedAttachmentIDList.contains(ghostAttachment.getLocalID()) || !Arrays.equals(ghostAttachment.getFileChecksum(), attachmentInfo.getFileChecksum())) continue;
									matchingAttachment = ghostAttachment;
									break;
								}
								if(matchingAttachment == null) continue;
								
								//Checking if there is no shared message
								if(sharedMessageInfo == null) {
									//Setting the shared message info
									sharedMessageInfo = ghostMessage;
									
									//Adding the unmatched attachments
									for(AttachmentInfo unmatchedAttachment : unmatchedAttachments) sharedMessageInfo.addAttachment(unmatchedAttachment);
								} else {
									//Switching the attachment's message
									attachmentInfo.setMessageInfo(sharedMessageInfo);
									messageInfo.removeAttachment(attachmentInfo);
									sharedMessageInfo.addAttachment(attachmentInfo);
									
									//Removing the message if it is empty
									if(messageInfo.getMessageText() == null && messageInfo.getAttachments().isEmpty()) {
										listIterator.remove();
										if(updater != null) updater.listUpdateRemoved(conversationItems.indexOf(messageInfo));
										conversationItems.remove(messageInfo);
									}
								}
								
								//Updating the attachment
								matchingAttachment.setGuid(attachmentInfo.getGuid());
								
								//Marking the item as updated
								replacedAttachmentIDList.add(attachmentInfo.getLocalID());
								
								//Breaking from the loop
								break;
							}
						}
						
						//Checking if a message was found
						if(sharedMessageInfo != null) {
							//Updating the ghost item
							sharedMessageInfo.setServerID(messageInfo.getServerID());
							sharedMessageInfo.setGuid(messageInfo.getGuid());
							sharedMessageInfo.setDate(messageInfo.getDate());
							sharedMessageInfo.setErrorCode(messageInfo.getErrorCode());
							sharedMessageInfo.setMessageState(messageInfo.getMessageState());
							sharedMessageInfo.updateViewProgressState();
							sharedMessageInfo.animateGhostStateChanges();
							
							//Adding the item to the reinsert queue
							sortQueue.add(sharedMessageInfo);
							
							//Replacing the item in the final list
							updateList.set(list.indexOf(conversationItem), sharedMessageInfo);
							
							//Removing the item from the ghost list
							ghostMessages.remove(sharedMessageInfo);
							
							//Setting the message as replaced
							messageReplaced = true;
						}
					}
				}
			}
			
			//Checking if a message could not be replaced
			if(!messageReplaced) {
				//Marking the item as a new item
				newItems.add(conversationItem);
				
				//Inserting the item
				//int index = insertConversationItem(conversationItem, context, false);
				
				//Determining the item's relations if it is a message
				//if(conversationItem instanceof MessageInfo) addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
				
				//Updating the last item
				//updateLastItem(context);
				
				//Setting the target scroll index
				//if(latestNewMessage == null || latestNewMessage.getDate() < conversationItem.getDate()) latestNewMessage = conversationItem;
				//if(updater != null) updater.updateInsertedScroll(index);
				
				//Updating the view
				//View view = getView();
				//if(view != null) updateView(context, view);
			}
		}
		
		//Re-sorting the queued items
		ArrayList<Integer> originalIndices = new ArrayList<>();
		for(ConversationItem item : sortQueue) originalIndices.add(conversationItems.indexOf(item));
		for(ListIterator<ConversationItem> iterator = sortQueue.listIterator(); iterator.hasNext();) {
			int oldIndex = originalIndices.get(iterator.nextIndex());
			ConversationItem item = iterator.next();
			conversationItems.remove(item);
			insertConversationItem(item, context, false);
			movedList.add(new ConversationItemMoveRecord(oldIndex, item));
		}
		
		//Inserting the new items
		for(ConversationItem item : newItems) insertConversationItem(item, context, false);
		
		//Updating the conversation items' relations
		ConversationUtils.addConversationItemRelations(this, conversationItems, updateList, MainApplication.getInstance(), true);
		
		//Updating the last item
		updateLastItem(context);
		
		//Updating the adapter
		if(updater != null) {
			//Telling the updater
			updater.itemsAdded(newItems);
			
			//Updating the moved messages
			for(ConversationItemMoveRecord record : movedList) {
				int newIndex = conversationItems.indexOf(record.item);
				if(record.index != newIndex) updater.listUpdateMove(record.index, newIndex);
			}
			
			//Updating the new messages
			if(!newItems.isEmpty()) {
				int[] indices = new int[newItems.size()];
				int incomingMessagesCount = 0;
				for(ConversationItem newItem : newItems) {
					//Finding the item index
					int itemIndex = conversationItems.indexOf(newItem);
					
					//Notifying the list
					updater.listUpdateInserted(itemIndex);
					
					//Adding the item index
					indices[incomingMessagesCount++] = itemIndex;
				}
				
				//Scrolling the list
				updater.listAttemptScrollToBottom(Arrays.copyOf(indices, incomingMessagesCount));
			}
			
			//Updating the unread messages
			updater.listUpdateUnread();
		}
		
		//Returning true
		return true;
	}
	
	public boolean removeConversationItem(Context context, ConversationItem item) {
		//Getting the lists
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return false;
		ArrayList<MessageInfo> ghostMessages = getGhostMessages();
		if(ghostMessages == null) return false;
		
		//Finding the item index
		int itemIndex = conversationItems.indexOf(item);
		if(itemIndex == -1) return false;
		
		//Removing the item
		conversationItems.remove(itemIndex);
		if(item instanceof MessageInfo) ghostMessages.remove(item);
		
		//Updating the adjacent messages
		ConversationUtils.removeConversationItemRelation(this, conversationItems, itemIndex, context, true);
		
		//Notifying the listeners
		ActivityCallbacks callbacks = getActivityCallbacks();
		if(callbacks != null) callbacks.listUpdateRemoved(itemIndex);
		
		//Returning true
		return true;
	}
	
	private static class ConversationItemMoveRecord {
		int index;
		ConversationItem item;
		
		ConversationItemMoveRecord(int index, ConversationItem item) {
			this.index = index;
			this.item = item;
		}
	}
	
	public void addChunk(List<ConversationItem> list) {
		//Adding the items
		List<ConversationItem> conversationItems = getConversationItems();
		conversationItems.addAll(0, list);
		
		//Updating the adapter
			/* AdapterUpdater updater = getAdapterUpdater();
			if(updater != null) updater.updateRangeInserted(0, list.size()); */
	}
	
	private int insertConversationItem(ConversationItem conversationItem, Context context, boolean update) {
		//Getting the list
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return -1;
		
		//Checking if the list is empty
		if(conversationItems.isEmpty()) {
			//Adding the item
			conversationItems.add(conversationItem);
			
			//Redetermining the relation
			if(update && conversationItem instanceof MessageInfo) ConversationUtils.addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
			
			//Returning the index
			return 0;
		}
		
		//Iterating over the conversation items backwards (more recent items appear at the end of the list, and new items are more likely to be recent than old)
		for(int i = conversationItems.size() - 1; i >= 0; i--) {
			//Skipping the remainder of the iteration if the item is newer
			if(ConversationUtils.compareConversationItems(conversationItems.get(i), conversationItem) > 0) continue;
			
			//Adding the item
			int addedIndex = i + 1;
			conversationItems.add(addedIndex, conversationItem);
			
			//Redetermining the relation
			if(update && conversationItem instanceof MessageInfo) ConversationUtils.addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
			
			//Returning the index
			return addedIndex;
		}
		
		//Adding the item at index 0 (the item is the oldest item in the list)
		conversationItems.add(0, conversationItem);
		
		//Redetermining the relation
		if(update && conversationItem instanceof MessageInfo) ConversationUtils.addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
		
		//Returning the index
		return 0;
	}
	
	public void addGhostMessage(Context context, MessageInfo message) {
		//Getting the lists
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return;
		ArrayList<MessageInfo> ghostMessages = getGhostMessages();
		if(ghostMessages == null) return;
		
		//Adding the message
		conversationItems.add(message); //The item can be appended to the end because it'll always be the most recent item (it was just added)
		ghostMessages.add(message);
		
		//Determining and updating the item's relations
		ConversationUtils.addConversationItemRelation(this, conversationItems, message, context, true);
		
		//Updating the last item
		trySetLastItemUpdate(context, message, false);
		
		//Telling the updater
		ActivityCallbacks updater = getActivityCallbacks();
		if(updater != null) {
			updater.itemsAdded(Collections.singletonList(message));
			updater.listUpdateInserted(conversationItems.size() - 1);
			updater.listScrollToBottom();
		}
		
		//Updating the view
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) updateView(itemView, context);
	}
	
	private void deleteMemory() {
		//Removing the conversation from memory
		ArrayList<ConversationInfo> conversations = MainApplication.getInstance().getConversations();
		if(conversations != null) conversations.remove(this);
	}
	
	public void delete(Context context) {
		//Removing the conversation from memory
		deleteMemory();
		
		//Removing the conversation shortcut
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			ShortcutUtils.disableShortcuts(context, Collections.singletonList(this));
		}
		
		//Removing the conversation from the database
		new DeleteConversationTask(context, this).execute();
	}
	
	public void deleteSync(Context context) {
		//Removing the conversation from memory
		deleteMemory();
		
		//Deleting the conversation from the database
		DatabaseManager.getInstance().deleteConversation(this);
		
		//Deleting the conversation from the external database
		if(serviceHandler == serviceHandlerSystemMessaging) {
			ConversationUtils.deleteMMSSMSConversationSync(context, new HashSet<>(getConversationMembersAsCollection()));
		}
	}
	
	private static class DeleteConversationTask extends AsyncTask<Void, Void, Void> {
		//Creating the values
		private final WeakReference<Context> contextReference;
		private final ConversationInfo conversationInfo;
		
		DeleteConversationTask(Context context, ConversationInfo conversationInfo) {
			//Setting the context reference
			contextReference = new WeakReference<>(context);
			
			//Setting the conversation info
			this.conversationInfo = conversationInfo;
		}
		
		@Override
		protected Void doInBackground(Void... voids) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Deleting the conversation
			DatabaseManager.getInstance().deleteConversation(conversationInfo);
			
			//Deleting the conversation from the external database
			if(conversationInfo.getServiceHandler() == serviceHandlerSystemMessaging) {
				if(Preferences.isTextMessageIntegrationActive(context)) ConversationUtils.deleteMMSSMSConversationSync(context, new HashSet<>(conversationInfo.getConversationMembersAsCollection()));
			}
			
			//Returning
			return null;
		}
	}
	
	public void setActivityCallbacks(ActivityCallbacks activityCallbacks) {
		this.activityCallbacks = activityCallbacks;
	}
	
	public ActivityCallbacks getActivityCallbacks() {
		return activityCallbacks;
	}
	
	public static abstract class ActivityCallbacks {
		public abstract void listUpdateFully();
		public abstract void listUpdateInserted(int index);
		public abstract void listUpdateRemoved(int index);
		public abstract void listUpdateMove(int from, int to);
		public abstract void listUpdateUnread();
		public abstract void listScrollToBottom();
		public abstract void listAttemptScrollToBottom(int... newIndices);
		
		public abstract void chatUpdateTitle();
		public abstract void chatUpdateUnreadCount();
		public abstract void chatUpdateMemberAdded(MemberInfo member, int index);
		public abstract void chatUpdateMemberRemoved(MemberInfo member, int index);
		
		public abstract void itemsAdded(List<ConversationItem> list);
		public abstract void tapbackAdded(TapbackInfo item);
		public abstract void stickerAdded(StickerInfo item);
		public abstract void messageSendFailed(MessageInfo message);
		
		public abstract Messaging.AudioPlaybackManager getAudioPlaybackManager();
		
		public abstract void playScreenEffect(String effect, View target);
		
		public abstract void requestPermission(String permission, int requestCode, BiConsumer<Context, Boolean> resultListener);
		public abstract void saveFile(File file);
	}
	
	public int getNextUserColor() {
		//Creating a list of the user colors
		SparseIntArray colorUses = new SparseIntArray();
		
		//Adding all of the standard colors
		for(int color : standardUserColors) colorUses.put(color, 0);
		
		//Counting the colors
		for(MemberInfo member : conversationMembers) {
			//Only allowing standard colors to be counted
			if(!Constants.arrayContains(standardUserColors, member.getColor())) continue;
			
			//Increasing the usage count
			colorUses.put(member.getColor(), colorUses.get(member.getColor(), 0) + 1);
		}
		
		//Finding the smallest use value
		int leastUses = conversationMembers.size();
		for(int i = 0; i < colorUses.size(); i++) {
			int uses = colorUses.valueAt(i);
			if(uses >= leastUses) continue;
			leastUses = uses;
			if(leastUses == 0) break;
		}
		
		//Finding all values with the least amount of uses
		ArrayList<Integer> leastUsedColors = new ArrayList<>();
		for(int i = 0; i < colorUses.size(); i++) {
			int uses = colorUses.valueAt(i);
			if(uses != leastUses) continue;
			leastUsedColors.add(colorUses.keyAt(i));
		}
		
		//Picking a least used color (randomly if there is more than 1 entry)
		if(leastUsedColors.size() == 1) return leastUsedColors.get(0);
		else return leastUsedColors.get(Constants.getRandom().nextInt(leastUsedColors.size()));
	}
	
	public int[] getMassUserColors(int userCount) {
		//Creating a random generator based on the GUID
		Random random = guid == null ? new Random() : new Random(guid.hashCode());
		
		//Creating the color array
		int[] array = new int[userCount];
		
		//Adding the colors
		ArrayList<Integer> colors = new ArrayList<>();
		for(int i = 0; i < userCount; i++) {
			//Getting the colors if there are no more
			if(colors.isEmpty()) colors.addAll(Arrays.asList(standardUserColors));
			
			//Picking a color
			Integer color = colors.get(random.nextInt(colors.size()));
			
			//Setting the color
			array[i] = color;
			
			//Removing the color from use
			colors.remove(color);
		}
		
		//Returning the color array
		return array;
	}
	
	public static int getRandomConversationColor() {
		return standardUserColors[Constants.getRandom().nextInt(standardUserColors.length)];
	}
	
	public boolean conversationMembersContain(String user) {
		return findConversationMember(user) != null;
	}
	
	public List<MemberInfo> getConversationMembers() {
		return conversationMembers;
	}
	
	public MemberInfo findConversationMember(String user) {
		for(MemberInfo member : conversationMembers) if(member.getName().equals(user)) return member;
		return null;
	}
		
		/* void setConversationMembers(ArrayList<MemberInfo> value) {
			conversationMembers = value;
		} */
	
	public void addConversationMember(MemberInfo member) {
		int index = Collections.binarySearch(conversationMembers, member, ConversationUtils.memberInfoComparator);
		if(index >= 0) return;
		index = (index * -1) - 1;
		conversationMembers.add(index, member);
		if(activityCallbacks != null) activityCallbacks.chatUpdateMemberAdded(member, index);
	}
	
	public void removeConversationMember(MemberInfo member) {
		int index = conversationMembers.indexOf(member);
		conversationMembers.remove(member);
		if(activityCallbacks != null) activityCallbacks.chatUpdateMemberRemoved(member, index);
	}
		
		/* void removeConversationMember(String user) {
			MemberInfo member = findConversationMember(user);
			if(member != null) conversationMembers.remove(member);
		} */
	
	public ArrayList<String> getConversationMembersAsCollection() {
		//Returning null if the conversation is server incomplete (awaiting information from the server, which includes the members)
		if(conversationState == ConversationState.INCOMPLETE_SERVER) return null;
		
		//Creating the array
		ArrayList<String> list = new ArrayList<>(conversationMembers.size());
		for(int i = 0; i < conversationMembers.size(); i++)
			list.add(conversationMembers.get(i).getName());
		
		//Returning the list
		return list;
	}
	
	public String[] getConversationMembersAsArray() {
		//Returning null if the conversation is server incomplete (awaiting information from the server, which includes the members)
		if(conversationState == ConversationState.INCOMPLETE_SERVER) return null;
		
		//Creating the array
		String[] array = new String[conversationMembers.size()];
		for(int i = 0; i < conversationMembers.size(); i++)
			array[i] = conversationMembers.get(i).getName();
		
		//Returning the array
		return array;
	}
	
	public String[] getNormalizedConversationMembersAsArray() {
		//Creating the array
		String[] array = new String[conversationMembers.size()];
		for(int i = 0; i < conversationMembers.size(); i++)
			array[i] = Constants.normalizeAddress(conversationMembers.get(i).getName());
		
		//Returning the array
		return array;
	}
	
	public void setConversationMembersCreateColors(String[] members) {
		//Inheriting the conversation color if there is only one member
		if(members.length == 1) {
			conversationMembers.add(new MemberInfo(members[0], conversationColor));
		} else {
			//Sorting the values
			Arrays.sort(members);
			
			//Getting color values
			int[] colorValues = getMassUserColors(members.length);
			
			//Copying the values to the map
			for(int i = 0; i < members.length; i++)
				conversationMembers.add(new MemberInfo(members[i], colorValues[i]));
		}
	}
	
	public boolean isGroupChat() {
		return conversationMembers.size() > 1;
	}
	
	public void buildTitle(Context context, Constants.TaskedResultCallback<String> resultCallback) {
		//Returning null if the conversation is server incomplete (awaiting information from the server, which includes the members)
		
		//Returning the result of the static method
		buildTitle(context, title, getConversationMembersAsArray(), resultCallback);
	}
	
	public static void buildTitle(Context context, String name, String[] members, Constants.TaskedResultCallback<String> resultCallback) {
		//Returning the conversation title if it is valid
		if(name != null && !name.isEmpty()) {
			resultCallback.onResult(name, false);
			return;
		}
		
		//Returning "unknown" if the conversation has no members
		if(members == null || members.length == 0) {
			resultCallback.onResult(context.getResources().getString(R.string.part_unknown), false);
			return;
		}
		
		//Checking if there is only one conversation member
		if(members.length == 1) {
			//Getting the user info
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, members[0], new UserCacheHelper.UserFetchResult() {
				@Override
				public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Returning the user's name
					resultCallback.onResult(userInfo != null && !TextUtils.isEmpty(userInfo.getContactName()) ? userInfo.getContactName() : Constants.formatAddress(members[0]), wasTasked);
				}
			});
			
			//Returning
			return;
		}
		
		//Creating the named conversation title list
		ArrayList<String> namedConversationMembers = new ArrayList<>();
		
		//Creating a weak reference to the context
		final WeakReference<Context> contextReference = new WeakReference<>(context);
		
		//Converting the list to named members
		for(String member : members) {
			//Getting the user info
			MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, member, new UserCacheHelper.UserFetchResult() {
				@Override
				public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Adding the name
					namedConversationMembers.add(userInfo != null && !TextUtils.isEmpty(userInfo.getContactName()) ? userInfo.getContactName() : Constants.formatAddress(member));
					
					//Returning if the names have not all been added
					if(members.length != namedConversationMembers.size()) return;
					
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					//Returning the string
					resultCallback.onResult(Constants.createLocalizedList(namedConversationMembers.toArray(new String[0]), context.getResources()), wasTasked);
				}
			});
		}
	}
	
	public static String buildTitleDirect(Context context, String name, String[] members) {
		//Returning the conversation title if it is valid
		if(name != null && !name.isEmpty()) return name;
		
		//Returning "unknown" if the conversation has no members
		if(members.length == 0) return context.getResources().getString(R.string.part_unknown);
		
		//Returning the string
		return Constants.createLocalizedList(members, context.getResources());
	}
	
	public String getStaticTitle() {
		return title;
	}
	
	public boolean setTitle(Context context, String value) {
		//Returning if the operation is invalid
		if(title != null && title.equals(value)) return false;
		
		//Setting the new title
		title = value;
		
		//Telling the activity listener
		if(activityCallbacks != null) activityCallbacks.chatUpdateTitle();
		
		//Updating the view
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) {
			//Setting the title
			itemView.conversationTitle.setText("");
			buildTitle(context, (title, wasTasked) -> {
				//Setting the title
				ItemViewHolder viewHolder = getViewHolder();
				if(viewHolder == null) return;
				viewHolder.conversationTitle.setText(title);
			});
		}
		
		//Returning true
		return true;
	}
	
	private boolean getSelected() {
		return selectionSource.getSelected(localID);
	}
	
	public void updateSelected() {
		//Calling the overload method
		ItemViewHolder itemView = getViewHolder();
		if(itemView != null) updateSelected(itemView);
	}
	
	private void updateSelected(ItemViewHolder itemView) {
		//Setting the visibility of the selected indicator
		boolean isSelected = getSelected();
		itemView.selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
		if(currentUserViewIndex != -1) itemView.iconGroup.getChildAt(currentUserViewIndex).setVisibility(isSelected ? View.GONE : View.VISIBLE);
	}
	
	public interface SelectionSource {
		public boolean getSelected(long identifier);
	}
	
	public int getConversationColor() {
		return conversationColor;
	}
	
	public void setConversationColor(int conversationColor) {
		//Setting the color
		this.conversationColor = conversationColor;
	}
	
	/**
	 * Fetch the color that should be displayed to the user while viewing this conversation.
	 * This function takes into account the advanced color preference, as well as the current service.
	 * @return the color value
	 */
	public int getDisplayConversationColor(Context context) {
		//Returning the color on a per-conversation basis
		if(Preferences.getPreferenceAdvancedColor(context)) return getConversationColor();
		
		//Returning the service color
		return ConversationInfo.getServiceColor(context.getResources(), getServiceHandler(), getService());
	}
	
	public static int getDefaultConversationColor(long id) {
		return standardUserColors[new Random(id).nextInt(standardUserColors.length)];
	}
	
	public static int getDefaultConversationColor(String guid) {
		return standardUserColors[new Random(guid.hashCode()).nextInt(standardUserColors.length)];
	}
	
	public static String getFormattedTime(long date) {
		//Returning the formatting
		return DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, 0).toString();
	}
	
	public ConversationItem findConversationItem(long localID) {
		//Getting the list
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return null;
		
		//Returning a matching conversation item
		for(ConversationItem conversationItem : conversationItems)
			if(conversationItem.getLocalID() == localID)
				return conversationItem;
		
		//Returning null
		return null;
	}
	
	public ConversationItem findConversationItem(String guid) {
		//Getting the list
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return null;
		
		//Returning a matching conversation item
		for(ConversationItem conversationItem : conversationItems)
			if(guid.equals(conversationItem.getGuid()))
				return conversationItem;
		
		//Returning null
		return null;
	}
	
	public AttachmentInfo findAttachmentInfo(long localID) {
		//Getting the list
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return null;
		
		//Returning a matching attachment info
		for(ConversationItem conversationItem : conversationItems)
			if(conversationItem instanceof MessageInfo)
				for(AttachmentInfo attachmentInfo : ((MessageInfo) conversationItem).getAttachments())
					if(attachmentInfo.getGuid() != null && attachmentInfo.getLocalID() == localID)
						return attachmentInfo;
		
		//Returning null
		return null;
	}
	
	public AttachmentInfo findAttachmentInfo(String guid) {
		//Getting the list
		ArrayList<ConversationItem> conversationItems = getConversationItems();
		if(conversationItems == null) return null;
		
		//Returning a matching attachment info
		for(ConversationItem conversationItem : conversationItems)
			if(conversationItem instanceof MessageInfo)
				for(AttachmentInfo attachmentInfo : ((MessageInfo) conversationItem).getAttachments())
					if(attachmentInfo.getGuid() != null && attachmentInfo.getGuid().equals(guid))
						return attachmentInfo;
		
		//Returning null
		return null;
	}
	
	public ConversationState getState() {
		return conversationState;
	}
	
	public void setState(ConversationState conversationState) {
		this.conversationState = conversationState;
	}
	
	public enum ConversationState {
		READY(0), //The conversation is in sync with the server
		INCOMPLETE_SERVER(1), //The conversation is a result of a message from the server, but is missing info
		INCOMPLETE_CLIENT(2); //The conversation was created on the client, but isn't linked to the server
		
		private final int identifier;
		
		ConversationState(int identifier) {
			this.identifier = identifier;
		}
		
		public int getIdentifier() {
			return identifier;
		}
		
		public static ConversationState fromIdentifier(int identifier) {
			//Returning the matching conversation state
			for(ConversationState conversationState : values())
				if(conversationState.getIdentifier() == identifier) return conversationState;
			
			//Returning null
			return null;
		}
	}
	
	public void requestScreenEffect(String effect, View target) {
		//Returning if the callback isn't set up
		if(activityCallbacks == null) return;
		
		//Setting and playing the effect
		activityCallbacks.playScreenEffect(effect, target);
	}
	
	public static abstract class BaseViewHolder extends RecyclerView.ViewHolder {
		//Creating the view values
		final ViewGroup iconGroup;
		final TextView conversationTitle;
		
		BaseViewHolder(View view) {
			super(view);
			
			//Getting the views
			iconGroup = view.findViewById(R.id.conversationicon);
			conversationTitle = view.findViewById(R.id.title);
		}
	}
	
	public static class ItemViewHolder extends BaseViewHolder {
		//Creating the view values
		//final ViewGroup iconGroup;
		final View selectedIndicator;
		
		//final TextView conversationTitle;
		final TextView conversationMessage;
		final TextView conversationTime;
		final TextView conversationUnread;
		
		final View flagMuted;
		final View flagDraft;
		
		public ItemViewHolder(View view) {
			super(view);
			
			//Getting the views
			//iconGroup = view.findViewById(R.id.conversationicon);
			selectedIndicator = view.findViewById(R.id.selected);
			
			//conversationTitle = view.findViewById(R.id.title);
			conversationMessage = view.findViewById(R.id.message);
			conversationTime = view.findViewById(R.id.time);
			conversationUnread = view.findViewById(R.id.unread);
			
			flagMuted = view.findViewById(R.id.flag_muted);
			flagDraft = view.findViewById(R.id.flag_draft);
		}
	}
	
	public static class SimpleItemViewHolder extends BaseViewHolder {
		public SimpleItemViewHolder(View view) {
			super(view);
		}
	}
	
	public static int getServiceColor(Resources resources, int serviceHandler, String serviceName) {
		//AirMessage bridge
		if(serviceHandler == ConversationInfo.serviceHandlerAMBridge) {
			//Returning a default color if the service is invalid
			if(serviceName == null) return resources.getColor(R.color.colorMessageDefault, null);
			
			switch(serviceName) {
				case ConversationInfo.serviceTypeAppleMessage:
					//iMessage
					return resources.getColor(R.color.colorPrimary, null);
				case ConversationInfo.serviceTypeAppleTextMessageForwarding:
					//SMS bridge
					return resources.getColor(R.color.colorMessageTextMessageForwarding, null);
				default:
					return resources.getColor(R.color.colorMessageDefault, null);
			}
		}
		//System messaging
		else if(serviceHandler == ConversationInfo.serviceHandlerSystemMessaging) {
			switch(serviceName) {
				case ConversationInfo.serviceTypeSystemMMSSMS:
					return resources.getColor(R.color.colorMessageTextMessage, null);
				case ConversationInfo.serviceTypeSystemRCS:
					return resources.getColor(R.color.colorMessageRCS, null);
				default:
					return resources.getColor(R.color.colorMessageDefault, null);
			}
		}
		
		//Returning a default color
		return resources.getColor(R.color.colorMessageDefault, null);
	}
}