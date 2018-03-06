package me.tagavari.airmessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.zip.DataFormatException;

import me.tagavari.airmessage.common.SharedValues;

class ConversationManager {
	//Message burst - Sending single messages one after the other
	static final long conversationBurstTimeMillis = 30 * 1000; //30 seconds
	//Message session - A conversation session, where conversation participants are active
	static final long conversationSessionTimeMillis = 5 * 60 * 1000; //5 minutes
	//Just now - A message sent just now
	static final long conversationJustNowTimeMillis = 60 * 1000; //1 minute
	
	static final Comparator<ConversationInfo> conversationComparator = new Comparator<ConversationInfo>() {
		@Override
		public int compare(ConversationInfo info1, ConversationInfo info2) {
			//Getting the last conversation item times
			long lastTime1 = info1.getLastItem() == null ? Long.MIN_VALUE : info1.getLastItem().getDate();
			long lastTime2 = info2.getLastItem() == null ? Long.MIN_VALUE : info2.getLastItem().getDate();
			
			//Returning the comparison
			return Long.compare(lastTime2, lastTime1);
		}
	};
	static final Comparator<ConversationItem> conversationItemComparator = new Comparator<ConversationItem>() {
		@Override
		public int compare(ConversationItem message1, ConversationItem message2) {
			//Returning 0 if either of the values are invalid
			if(message1 == null || message2 == null) return 0;
			
			//Returning the comparison
			return Long.compare(message1.getDate(), message2.getDate());
		}
	};
	static final Comparator<MemberInfo> memberInfoComparator = new Comparator<MemberInfo>() {
		@Override
		public int compare(MemberInfo member1, MemberInfo member2) {
			//Returning 0 if either of the values are invalid
			if(member1 == null || member2 == null) return 0;
			
			//Returning the comparison (lexicographic comparison)
			return member1.name.compareTo(member2.name);
		}
	};
	
	//Creating the conversation list
	//private final ArrayList<ConversationInfo> conversations = new ArrayList<>();
	
	/* public ArrayList<ConversationInfo> getConversations() {
		return conversations;
	}
	ConversationInfo findConversationInfo(long conversationID) {
		//Looping through all of the conversations
		for(ConversationInfo conversation : conversations) {
			//Skipping the remainder of the iteration if the conversation identifiers do not match
			if(conversation.getLocalID() != conversationID) continue;
			
			//Returning the conversation
			return conversation;
		}
		
		//Returning null
		return null;
	}
	
	ConversationInfo findConversationInfo(String conversationGUID) {
		//Looping through all of the conversations
		for(ConversationInfo conversation : conversations) {
			//Skipping the remainder of the iteration if the conversation GUIDs do not match
			if(conversation.getGuid() == null || !conversation.getGuid().equals(conversationGUID)) continue;
			
			//Returning the conversation
			return conversation;
		}
		
		//Returning null
		return null;
	}
	
	ConversationInfo findConversationInfo(String service, ArrayList<String> members) {
		//Sorting the list
		members = (ArrayList<String>) members.clone();
		Collections.sort(members);
		
		//Looping through all of the conversations
		for(ConversationInfo conversation : conversations) {
			//Skipping the remainder of the iteration if the service does not match
			if(!conversation.getService().equals(service)) continue;
			
			//Getting the members
			ArrayList<String> conversationMembers = (ArrayList<String>) conversation.getConversationMembers().clone();
			Collections.sort(conversationMembers);
			
			//Skipping the remainder of the iteration if the members don't match
			if(!conversationMembers.equals(members)) continue;
			
			//Returning the conversation
			return conversation;
		}
		
		//Returning null
		return null;
	}
	
	AttachmentInfo findAttachmentInfoInActiveConversation(String message) {
		//Returning a matching attachment info
		for(long conversationLocalID : Messaging.getForegroundConversations())
			for(ConversationItem conversationItem : findConversationInfo(conversationLocalID).getConversationItems())
				if(conversationItem instanceof MessageInfo)
					for(AttachmentInfo attachmentInfo : ((MessageInfo) conversationItem).getAttachments())
						if(attachmentInfo.message != null && attachmentInfo.message.equals(message))
							return attachmentInfo;
		
		//Returning null
		return null;
	}
	
	AttachmentInfo findAttachmentInfoInActiveConversation(long localID) {
		//Returning a matching attachment info
		for(long conversationLocalID : Messaging.getForegroundConversations())
			for(ConversationItem conversationItem : findConversationInfo(conversationLocalID).getConversationItems())
				if(conversationItem instanceof MessageInfo)
					for(AttachmentInfo attachmentInfo : ((MessageInfo) conversationItem).getAttachments())
						if(attachmentInfo.localID == localID) return attachmentInfo;
		
		//Returning null
		return null;
	} */
	
	static MainApplication.LoadFlagArrayList<ConversationInfo> getConversations() {
		MainApplication app = MainApplication.getInstance();
		if(app == null) return null;
		
		return app.getConversations();
	}
	
	static void sortConversation(ConversationInfo conversation) {
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return;
		
		//Removing the matching conversation
		Iterator<ConversationInfo> iterator = conversations.iterator();
		while(iterator.hasNext()) {
			if(iterator.next().getLocalID() == conversation.getLocalID()) {
				iterator.remove();
				break;
			}
		}
		
		//Re-inserting the conversation
		insertConversation(conversations, conversation);
	}
	
	static void addConversation(ConversationInfo conversation) {
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return;
		
		//Inserting the conversation into the list
		insertConversation(conversations, conversation);
	}
	
	private static void insertConversation(ArrayList<ConversationInfo> conversations, ConversationManager.ConversationInfo conversationInfo) {
		//Adding the item if the list is empty or it has no last item
		if(conversations.isEmpty() || conversationInfo.getLastItem() == null) {
			conversations.add(conversationInfo);
			return;
		}
		
		//Iterating over the conversation items backwards (more recent items appear at the end of the list, and new items are more likely to be new than old)
		for(int i = 0; i < conversations.size(); i++) {
			//Getting the conversation at the index
			ConversationInfo indexConversation = conversations.get(i);
			
			//Skipping the remainder of the iteration if the item is older
			if(indexConversation.getLastItem() == null || conversationInfo.getLastItem().getDate() < indexConversation.getLastItem().getDate())
				continue;
			
			//Adding the item
			conversations.add(i, conversationInfo);
			
			//Returning
			return;
		}
		
		//Placing the item at the bottom of the list
		conversations.add(conversationInfo);
	}
	
	static ConversationManager.ConversationInfo findConversationInfo(long localID) {
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return null;
		
		//Returning the conversation info
		for(ConversationManager.ConversationInfo conversationInfo : conversations) {
			if(conversationInfo.getLocalID() == localID) return conversationInfo;
		}
		
		//Returning null
		return null;
	}
	
	static ArrayList<ConversationInfo> getForegroundConversations() {
		//Creating the list
		ArrayList<ConversationInfo> list = new ArrayList<>();
		
		//Iterating over the loaded conversation IDs
		for(long conversationID : Messaging.getForegroundConversations()) {
			//Adding the conversation
			ConversationInfo conversationInfo = findConversationInfo(conversationID);
			if(conversationInfo != null) list.add(conversationInfo);
		}
		
		//Returning the list
		return list;
	}
	
	static ArrayList<ConversationInfo> getLoadedConversations() {
		//Creating the list
		ArrayList<ConversationInfo> list = new ArrayList<>();
		
		//Iterating over the loaded conversation IDs
		for(long conversationID : Messaging.getLoadedConversations()) {
			//Adding the conversation
			ConversationInfo conversationInfo = findConversationInfo(conversationID);
			if(conversationInfo != null) list.add(conversationInfo);
		}
		
		//Returning the list
		return list;
	}
	
	static ConversationManager.AttachmentInfo findAttachmentInfoInActiveConversation(String guid) {
		//Finding the attachment info
		for(long conversationID : Messaging.getForegroundConversations()) {
			ConversationManager.ConversationInfo conversationInfo = findConversationInfo(conversationID);
			if(conversationInfo == null) continue;
			ConversationManager.AttachmentInfo attachmentInfo = conversationInfo.findAttachmentInfo(guid);
			if(attachmentInfo != null) return attachmentInfo;
		}
		
		//Returning null
		return null;
	}
	
	static void processFileFragmentConfirmed(String guid, short requestID) {
		//Finding the attachment info
		ConversationManager.AttachmentInfo attachmentInfo = findAttachmentInfoInActiveConversation(guid);
		if(attachmentInfo == null || !attachmentInfo.compareRequestID(requestID)) return;
		attachmentInfo.stopTimer(true);
	}
	
	static void processFileFragmentFailed(String guid, short requestID) {
		//Finding the attachment info
		ConversationManager.AttachmentInfo attachmentInfo = findAttachmentInfoInActiveConversation(guid);
		if(attachmentInfo == null || !attachmentInfo.compareRequestID(requestID)) return;
		attachmentInfo.onDownloadFailed();
	}
	
	static void processFileFragmentData(Context context, String guid, short requestID, byte[] compressedBytes, int index, boolean isLast, long fileSize) {
		//Finding the attachment info
		ConversationManager.AttachmentInfo attachmentInfo = findAttachmentInfoInActiveConversation(guid);
		if(attachmentInfo == null || !attachmentInfo.compareRequestID(requestID)) return;
		if(fileSize != -1) attachmentInfo.setFileSize(fileSize);
		attachmentInfo.onFileFragmentReceived(context, compressedBytes, index, isLast);
	}
	
	static class ConversationInfo implements Serializable {
		private static final long serialVersionUID = 0;
		
		//Creating the reference values
		/* private static final String timeFormat = "h:mm a";
		private static final String dayFormat = "MMM d";
		private static final String weekdayFormat = "E";
		private static final String yearFormat = "y"; */
		static final String bullet = " • ";
		static final Integer[] standardUserColors = {
				0xFFF44336, //Red
				0xFFE91E63, //Pink
				0xFF9C27B0, //Purple
				0xFF673AB7, //Dark purple
				0xFF3F51B5, //Indigo
				0xFF2196F3, //Blue
				0xFF03A9F4, //Light blue
				0xFF00BCD4, //Cyan
				0xFF009688, //Teal
				0xFF4CAF50, //Green
				0xFF8BC34A, //Light green
				0xFFCDDC39, //Lime green
				0xFFFDD835, //Yellow (400)
				0xFFFFC107, //Amber
				0xFFFF9800, //Orange
				0xFFFF5722, //Deep orange
				//0xFF795548, //Brown
				//0xFF607D8B, //Blue grey
		};
		static final int backupUserColor = 0xFF607D8B; //Blue grey
		private static final int maxUsersToDisplay = 4;
		
		//Creating the values
		private final long localID;
		private String guid;
		private ConversationState conversationState;
		private String service;
		private transient WeakReference<ArrayList<ConversationItem>> conversationItemsReference = null;
		private transient WeakReference<ArrayList<MessageInfo>> ghostMessagesReference = null;
		private ArrayList<MemberInfo> conversationMembers;
		//private transient WeakReference<Messaging.RecyclerAdapter> arrayAdapterReference = null;
		private transient AdapterUpdater adapterUpdater = null;
		//private transient View view;
		//private transient ViewGroup iconView = null;
		private String title = null;
		private transient int unreadMessageCount = 0;
		private boolean isArchived = false;
		private boolean isMuted = false;
		private transient boolean isSelected = false;
		private int conversationColor = 0xFF000000; //Black
		private transient WeakReference<MessageInfo> activityStateTargetReference = null;
		private transient int currentUserViewIndex;
		private transient LightConversationItem lastItem;
		
		private transient Messaging.EffectCallbacks effectCallbacks = null;
		
		//private int currentUserViewIndex = -1;
		private transient Constants.ViewSource viewSource = null;
		
		//Creating the listeners
		private transient ArrayList<Runnable> titleChangeListeners = new ArrayList<>();
		private transient ArrayList<Runnable> unreadCountChangeListeners = new ArrayList<>();
		
		ConversationInfo(long localID, ConversationState conversationState) {
			//Setting the local ID and state
			this.localID = localID;
			this.conversationState = conversationState;
			
			//Instantiating the conversation items list
			conversationMembers = new ArrayList<>();
		}
		
		ConversationInfo(long localID, String guid, ConversationState conversationState) {
			//Setting the identifiers and the state
			this.localID = localID;
			this.guid = guid;
			this.conversationState = conversationState;
			
			//Instantiating the members list
			conversationMembers = new ArrayList<>();
		}
		
		ConversationInfo(long localID, String guid, ConversationState conversationState, String service, ArrayList<MemberInfo> conversationMembers, String title, int unreadMessageCount, int conversationColor) {
			//Setting the values
			this.guid = guid;
			this.localID = localID;
			this.conversationState = conversationState;
			this.service = service;
			this.conversationMembers = conversationMembers;
			this.title = title;
			this.unreadMessageCount = unreadMessageCount;
			this.conversationColor = conversationColor;
		}
		
		void setConversationItems(ArrayList<ConversationItem> items, ArrayList<MessageInfo> ghostItems) {
			conversationItemsReference = new WeakReference<>(items);
			ghostMessagesReference = new WeakReference<>(ghostItems);
		}
		
		View createView(Context context, View convertView, ViewGroup parent) {
			//Inflating the layout if the view can't be recycled
			if(convertView == null)
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_conversation, parent, false);
			
			//Setting the flags
			convertView.findViewById(R.id.flag_muted).setVisibility(isMuted ? View.VISIBLE : View.GONE);
			
			//Returning if the conversation has no members
			if(conversationMembers.isEmpty())
				return convertView; //TODO add support for empty conversations
			
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
		}
		
		void setViewSource(Constants.ViewSource viewSource) {
			this.viewSource = viewSource;
		}
		
		private View getView() {
			if(viewSource == null) return null;
			return viewSource.get();
		}
		
		void updateView(Context context) {
			View view = getView();
			if(view != null) updateView(context, view);
		}
		
		private void updateView(Context context, View itemView) {
			//Setting the title
			((TextView) itemView.findViewById(R.id.title)).setText(buildTitleDirect(context, title, getConversationMembersAsArray()));
			updateUnreadStatus(itemView);
			
			buildTitle(context, (title, wasTasked) -> {
				//Setting the title
				View view = wasTasked ? getView() : itemView;
				if(view == null) return;
				((TextView) view.findViewById(R.id.title)).setText(title);
			});
			
			if(lastItem == null) {
				((TextView) itemView.findViewById(R.id.message)).setText("");
				((TextView) itemView.findViewById(R.id.time)).setText("");
			} else {
				((TextView) itemView.findViewById(R.id.message)).setText(lastItem.getMessage());
				updateTime(context, itemView);
			}
		}
		
		void updateUnreadStatus() {
			//Calling the overload method
			View view = getView();
			if(view != null) updateUnreadStatus(view);
		}
		
		private void updateUnreadStatus(View itemView) {
			//Getting the views
			TextView titleText = itemView.findViewById(R.id.title);
			TextView messageText = itemView.findViewById(R.id.message);
			TextView unreadCount = itemView.findViewById(R.id.unread);
			
			if(unreadMessageCount > 0) {
				titleText.setTypeface(titleText.getTypeface(), Typeface.BOLD);
				titleText.setTextColor(itemView.getResources().getColor(R.color.colorPrimary, null));
				
				messageText.setTypeface(messageText.getTypeface(), Typeface.BOLD);
				messageText.setTextColor(Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorPrimary));
				
				unreadCount.setVisibility(View.VISIBLE);
				unreadCount.setText(Integer.toString(unreadMessageCount));
			} else {
				titleText.setTypeface(titleText.getTypeface(), Typeface.NORMAL);
				titleText.setTextColor(Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorPrimary));
				
				messageText.setTypeface(messageText.getTypeface(), Typeface.NORMAL);
				messageText.setTextColor(Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorSecondary));
				
				unreadCount.setVisibility(View.GONE);
			}
		}
		
		void updateTime(Context context) {
			//Calling the overload method
			View view = getView();
			if(view != null) updateTime(context, view);
		}
		
		private void updateTime(Context context, View itemView) {
			//Returning if the last item is invalid
			if(lastItem == null) return;
			
			//Setting the time
			((TextView) itemView.findViewById(R.id.time)).setText(
					System.currentTimeMillis() - lastItem.getDate() < conversationJustNowTimeMillis ?
							context.getResources().getString(R.string.time_now) :
							DateUtils.getRelativeTimeSpanString(lastItem.getDate(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString());
		}
		
		void updateViewUser(Context context) {
			View view = getView();
			if(view != null) updateViewUser(context, view);
		}
		
		/* private void updateViewUser(Context context, View itemView) {
			//Returning if the conversation has no members
			if(conversationMembers.isEmpty()) return;
			
			//Setting the title
			//((TextView) itemView.findViewById(R.id.title)).setText(buildTitle(context));
			
			//Getting the conversation icon view
			ViewGroup conversationIcons = itemView.findViewById(R.id.conversationicon);
			
			//Getting the view data
			currentUserViewIndex = Math.min(conversationMembers.size() - 1, maxUsersToDisplay - 1);
			View viewAtIndex = conversationIcons.getChildAt(currentUserViewIndex);
			ViewGroup iconView;
			if(viewAtIndex instanceof ViewStub)
				iconView = (ViewGroup) ((ViewStub) viewAtIndex).inflate();
			else iconView = (ViewGroup) viewAtIndex;
			
			//Hiding the other views
			for(int i = 0; i < maxUsersToDisplay; i++)
				conversationIcons.getChildAt(i).setVisibility(i == currentUserViewIndex ? View.VISIBLE : View.GONE);
			
			//Setting the icons
			for(int i = 0; i < iconView.getChildCount(); i++) {
				//Getting the child view
				View child = iconView.getChildAt(i);
				
				//Resetting the contact image
				((ImageView) child.findViewById(R.id.profile_image)).setImageBitmap(null);
				
				//Setting the default profile tint
				((ImageView) child.findViewById(R.id.profile_default)).setColorFilter(getConversationMembers().get(i).getColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
				
				//Assigning the user info
				final int finalIndex = i;
				MainApplication.getInstance().getBitmapCacheHelper().assignContactImage(context, getConversationMembers().get(i).getStaticTitle(), wasTasked -> {
					View view = wasTasked ? getView() : itemView;
					if(view == null) return null;
					
					return ((ViewGroup) ((ViewGroup) view.findViewById(R.id.conversationicon)).getChildAt(currentUserViewIndex)).getChildAt(finalIndex).findViewById(R.id.profile_image);
				});
			}
		} */
		
		private void updateViewUser(Context context, View itemView) {
			//Returning if the conversation has no members
			if(conversationMembers.isEmpty()) return;
			
			//Getting the conversation icon view
			ViewGroup conversationIcons = itemView.findViewById(R.id.conversationicon);
			
			//Getting the view data
			currentUserViewIndex = Math.min(conversationMembers.size() - 1, maxUsersToDisplay - 1);
			View viewAtIndex = conversationIcons.getChildAt(currentUserViewIndex);
			ViewGroup iconView;
			if(viewAtIndex instanceof ViewStub)
				iconView = (ViewGroup) ((ViewStub) viewAtIndex).inflate();
			else iconView = (ViewGroup) viewAtIndex;
			
			//Hiding the other views
			for(int i = 0; i < maxUsersToDisplay; i++)
				conversationIcons.getChildAt(i).setVisibility(i == currentUserViewIndex ? View.VISIBLE : View.GONE);
			
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
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, contactName, contactName, new BitmapCacheHelper.ImageDecodeResult() {
					@Override
					void onImageMeasured(int width, int height) {}
					
					@Override
					void onImageDecoded(Bitmap result, boolean wasTasked) {
						//Returning if the result is invalid
						if(result == null) return;
						
						View view = wasTasked ? getView() : itemView;
						if(view == null) return;
						
						//Getting the icon view
						View viewAtIndex = ((ViewGroup) view.findViewById(R.id.conversationicon)).getChildAt(currentUserViewIndex);
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
		}
		
		void clearMessages() {
			//Getting the list
			ArrayList<ConversationItem> conversationItems = getConversationItems();
			if(conversationItems == null) return;
			
			//Clearing the list
			conversationItems.clear();
			
			//Resetting the active message info state listing
			activityStateTargetReference = null;
			
			//Updating the adapter
			AdapterUpdater updater = getAdapterUpdater();
			if(updater != null) updater.updateFully();
		}
		
		ArrayList<ConversationItem> getConversationItems() {
			return conversationItemsReference == null ? null : conversationItemsReference.get();
		}
		
		ArrayList<MessageInfo> getGhostMessages() {
			return ghostMessagesReference == null ? null : ghostMessagesReference.get();
		}
		
		long getLocalID() {
			return localID;
		}
		
		String getGuid() {
			return guid;
		}
		
		void setGuid(String guid) {
			this.guid = guid;
		}
		
		String getService() {
			return service;
		}
		
		void setService(String service) {
			this.service = service;
		}
		
		int getUnreadMessageCount() {
			return unreadMessageCount;
		}
		
		void setUnreadMessageCount(int unreadMessageCount) {
			//Setting the value
			this.unreadMessageCount = unreadMessageCount;
			
			//Calling the listeners
			for(Runnable listener : unreadCountChangeListeners) listener.run();
		}
		
		boolean isArchived() {
			return isArchived;
		}
		
		void setArchived(boolean archived) {
			isArchived = archived;
		}
		
		boolean isMuted() {
			return isMuted;
		}
		
		void setMuted(boolean muted) {
			//Setting the muted variable
			isMuted = muted;
			
			//Getting the view
			View view = getView();
			if(view == null) return;
			
			//Updating the view
			if(isMuted) view.findViewById(R.id.flag_muted).setVisibility(View.VISIBLE);
			else view.findViewById(R.id.flag_muted).setVisibility(View.GONE);
		}
		
		MessageInfo getActivityStateTarget() {
			if(activityStateTargetReference == null) return null;
			return activityStateTargetReference.get();
		}
		
		void setActivityStateTarget(MessageInfo activityStateTarget) {
			activityStateTargetReference = new WeakReference<>(activityStateTarget);
		}
		
		void tryActivityStateTarget(MessageInfo activityStateTarget, boolean update, Context context) {
			//Getting the current item
			MessageInfo activeMessage = getActivityStateTarget();
			
			//Replacing the item if it is invalid
			if(activeMessage == null) {
				setActivityStateTarget(activityStateTarget);
				
				//Updating the view
				if(update) activityStateTarget.updateActivityStateDisplay(context);
			} else {
				//Replacing the item if the new one is outgoing and more recent
				if(activityStateTarget.isOutgoing() &&
						activityStateTarget.getDate() >= activeMessage.getDate() &&
						(activityStateTarget.getMessageState() == SharedValues.MessageInfo.stateCodeDelivered || activityStateTarget.getMessageState() == SharedValues.MessageInfo.stateCodeRead)) {
					setActivityStateTarget(activityStateTarget);
					
					//Updating the views
					if(update) {
						activityStateTarget.updateActivityStateDisplay(context);
						activeMessage.updateActivityStateDisplay(context);
					}
				}
			}
		}
		
		void replaceConversationItems(Context context, ArrayList<ConversationItem> sortedList) {
			//Returning if there are no items
			if(sortedList.isEmpty()) return;
			
			//Getting the lists
			ArrayList<ConversationItem> conversationItems = getConversationItems();
			if(conversationItems == null) return;
			ArrayList<MessageInfo> ghostMessages = getGhostMessages();
			if(ghostMessages == null) return;
			
			//Adding the items
			conversationItems.clear();
			conversationItems.addAll(sortedList);
			
			//Finding the ghost items
			ghostMessages.clear();
			for(ConversationItem conversationItem : sortedList) {
				if(!(conversationItem instanceof MessageInfo)) continue;
				MessageInfo messageInfo = (MessageInfo) conversationItem;
				if(messageInfo.getMessageState() != SharedValues.MessageInfo.stateCodeGhost) continue;
				ghostMessages.add(messageInfo);
			}
			
			//Updating the last item
			updateLastItem(context);
			//conversationItems.get(conversationItems.size() - 1).toLightConversationItem(context, result -> lastItem = result);
			
			//Updating the adapter
			AdapterUpdater updater = getAdapterUpdater();
			if(updater != null) updater.updateFully();
			
			//Updating the view
			View view = getView();
			if(view != null) updateView(context, view);
		}
		
		void setLastItem(Context context, ConversationItem lastConversationItem) {
			//Setting the last item
			lastItem = new LightConversationItem("", lastConversationItem.getDate());
			lastConversationItem.getSummary(context, (wasTasked, result) -> lastItem.setMessage(result));
		}
		
		void updateLastItem(Context context) {
			//Getting the list
			ArrayList<ConversationItem> conversationItems = getConversationItems();
			if(conversationItems == null) return;
			
			//Getting the last conversation item
			ConversationItem lastConversationItem = conversationItems.get(conversationItems.size() - 1);
			
			//Setting the last item
			lastItem = new LightConversationItem("", lastConversationItem.getDate());
			lastConversationItem.getSummary(context, (wasTasked, result) -> lastItem.setMessage(result));
		}
		
		void addConversationItem(Context context, ConversationItem conversationItem) {
			//Getting the lists
			ArrayList<ConversationItem> conversationItems = getConversationItems();
			if(conversationItems == null) return;
			ArrayList<MessageInfo> ghostMessages = getGhostMessages();
			if(ghostMessages == null) return;
			
			//Getting the adapter updater
			AdapterUpdater updater = getAdapterUpdater();
			
			boolean messageReplaced = false;
			//Checking if the item is a message
			if(conversationItem instanceof MessageInfo) {
				MessageInfo messageInfo = (MessageInfo) conversationItem;
				
				if(messageInfo.isOutgoing() && messageInfo.getMessageState() != SharedValues.MessageInfo.stateCodeGhost) {
					//Scanning the ghost items
					if(messageInfo.getMessageText() != null && messageInfo.getAttachments().isEmpty()) {
						ListIterator<MessageInfo> listIterator = ghostMessages.listIterator();
						while(listIterator.hasNext()) {
							//Getting the item
							MessageInfo ghostMessage = listIterator.next();
							
							//Skipping the remainder of the iteration if the item doesn't match
							if(ghostMessage.getMessageText() == null || !messageInfo.getMessageText().equals(ghostMessage.getMessageText())) continue;
							
							//Updating the ghost item
							ghostMessage.setDate(messageInfo.getDate());
							ghostMessage.setGuid(messageInfo.getGuid());
							ghostMessage.setMessageState(messageInfo.getMessageState());
							ghostMessage.setErrorCode(messageInfo.getErrorCode());
							ghostMessage.setDateRead(messageInfo.getDateRead());
							ghostMessage.updateViewProgressState(context);
							ghostMessage.animateGhostStateChanges();
							
							//Re-sorting the item
							{
								int originalIndex = conversationItems.indexOf(ghostMessage);
								conversationItems.remove(ghostMessage);
								int newIndex = insertConversationItem(ghostMessage, context, false);
								
								//Updating the adapter
								if(originalIndex != newIndex) {
									if(updater != null) updater.updateMove(originalIndex, newIndex);
								}
							}
							
							//Updating the item's relations
							addConversationItemRelation(this, conversationItems, ghostMessage, context, true);
							
							//Setting the message as replaced
							messageReplaced = true;
							
							//Removing the item from the ghost list
							listIterator.remove();
							
							//Breaking from the loop
							break;
						}
					} else if(messageInfo.getAttachments().size() == 1) {
						AttachmentInfo attachmentInfo = messageInfo.getAttachments().get(0);
						if(attachmentInfo.getFileChecksum() != null) {
							ListIterator<MessageInfo> listIterator = ghostMessages.listIterator();
							while(listIterator.hasNext()) {
								//Getting the item
								MessageInfo ghostMessage = listIterator.next();
								
								//Skipping the remainder of the iteration if the item doesn't match
								if(ghostMessage.getAttachments().isEmpty()) continue;
								AttachmentInfo firstAttachment = ghostMessage.getAttachments().get(0);
								if(attachmentInfo.getFileChecksum() == null || !Arrays.equals(attachmentInfo.getFileChecksum(), firstAttachment.getFileChecksum())) continue;
								
								//Updating the ghost item
								ghostMessage.setDate(messageInfo.getDate());
								ghostMessage.setGuid(messageInfo.getGuid());
								ghostMessage.setErrorCode(messageInfo.getErrorCode());
								ghostMessage.setMessageState(messageInfo.getMessageState());
								ghostMessage.updateViewProgressState(context);
								ghostMessage.animateGhostStateChanges();
								
								firstAttachment.setGuid(attachmentInfo.getGuid());
								
								//Re-sorting the item
								{
									int originalIndex = conversationItems.indexOf(ghostMessage);
									conversationItems.remove(ghostMessage);
									int newIndex = insertConversationItem(ghostMessage, context, false);
									
									//Updating the adapter
									if(originalIndex != newIndex) {
										if(updater != null) updater.updateMove(originalIndex, newIndex);
									}
								}
								
								//Updating the item's relations
								addConversationItemRelation(this, conversationItems, ghostMessage, context, true);
								
								//Setting the message as replaced
								messageReplaced = true;
								
								//Removing the item from the ghost list
								listIterator.remove();
								
								//Breaking from the loop
								break;
							}
						}
					}
				}
			}
			
			//Checking if a message could not be replaced
			if(!messageReplaced) {
				//Inserting the item
				int index = insertConversationItem(conversationItem, context, false);
				
				//Determining the item's relations if it is a message
				if(conversationItem instanceof MessageInfo) addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
				
				//Updating the last item
				updateLastItem(context);
				
				//Updating the adapter
				if(updater != null) updater.updateScroll(index);
				
				//Updating the view
				View view = getView();
				if(view != null) updateView(context, view);
			}
			
			//Updating the adapter's unread messages
			if(updater != null) updater.updateUnread();
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
				if(update && conversationItem instanceof MessageInfo) addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
				
				//Returning the index
				return 0;
			}
			
			//Iterating over the conversation items backwards (more recent items appear at the end of the list, and new items are more likely to be recent than old)
			for(int i = conversationItems.size() - 1; i >= 0; i--) {
				//Skipping the remainder of the iteration if the item is newer
				if(conversationItems.get(i).date > conversationItem.date) continue;
				
				//Adding the item
				int addedIndex = i + 1;
				conversationItems.add(addedIndex, conversationItem);
				
				//Redetermining the relation
				if(update && conversationItem instanceof MessageInfo) addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
				
				//Returning the index
				return addedIndex;
			}
			
			//Adding the item at index 0 (the item is the oldest item in the list)
			conversationItems.add(0, conversationItem);
			
			//Redetermining the relation
			if(update && conversationItem instanceof MessageInfo) addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
			
			//Returning the index
			return 0;
		}
		
		/* void updateLastItem(Context context) {
			//Sorting the conversation items
			sortConversationItems();
			
			//Getting the last conversation item
			ConversationItem conversationItem = conversationItems.get(conversationItems.size() - 1);
			
			//Setting the summary in memory
			lastItem = conversationItem.toLightConversationItem(context);
			
			//Updating the view
			updateView(context);
		} */
		
		void addGhostMessage(Context context, MessageInfo message) {
			//Getting the lists
			ArrayList<ConversationItem> conversationItems = getConversationItems();
			if(conversationItems == null) return;
			ArrayList<MessageInfo> ghostMessages = getGhostMessages();
			if(ghostMessages == null) return;
			
			//Adding the message
			conversationItems.add(message); //The item can be appended to the end because it'll always be the most recent item (it was just added)
			ghostMessages.add(message);
			
			//Determining and updating the item's relations
			addConversationItemRelation(this, conversationItems, message, context, true);
			
			//Updating the last item
			updateLastItem(context);
			
			//Updating the adapter
			AdapterUpdater updater = getAdapterUpdater();
			if(updater != null) updater.updateScroll(conversationItems.size() - 1);
			
			//Updating the view
			View view = getView();
			if(view != null) updateView(context, view);
		}
		
		void delete(final Context context) {
			//Removing the conversation from memory
			ArrayList<ConversationInfo> conversations = MainApplication.getInstance().getConversations();
			if(conversations != null) conversations.remove(this);
			
			//Removing the conversation from the database
			new DeleteConversationTask(context, this).execute();
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
				DatabaseManager.deleteConversation(DatabaseManager.getWritableDatabase(context), conversationInfo);
				
				//Returning
				return null;
			}
		}
		
		/* void setListAdapter(Messaging.RecyclerAdapter arrayAdapter) {
			//Setting the adapter
			arrayAdapterReference = new WeakReference<>(arrayAdapter);
		}
		
		Messaging.RecyclerAdapter getListAdapter() {
			return arrayAdapterReference == null ? null : arrayAdapterReference.get();
		} */
		
		void setAdapterUpdater(AdapterUpdater adapterUpdater) {
			this.adapterUpdater = adapterUpdater;
		}
		
		private AdapterUpdater getAdapterUpdater() {
			return adapterUpdater;
		}
		
		static abstract class AdapterUpdater {
			abstract void updateFully();
			abstract void updateScroll(int index);
			abstract void updateMove(int from, int to);
			abstract void updateUnread();
		}
		
		int getNextUserColor() {
			//Creating a list of the user colors
			SparseIntArray colorUses = new SparseIntArray();
			
			//Adding all of the standard colors
			for(int color : standardUserColors) colorUses.put(color, 0);
			
			//Counting the colors
			for(MemberInfo member : conversationMembers) {
				//Only allowing standard colors to be counted
				if(!Constants.arrayContains(standardUserColors, member.color)) continue;
				
				//Increasing the usage count
				colorUses.put(member.color, colorUses.get(member.color, 0) + 1);
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
		
		int[] getMassUserColors(int userCount) {
			//Creating a random generator based on the GUID
			Random random = new Random(guid.hashCode());
			
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
		
		static int getRandomColor() {
			return standardUserColors[Constants.getRandom().nextInt(standardUserColors.length)];
		}
		
		boolean conversationMembersContain(String user) {
			return findConversationMember(user) != null;
		}
		
		MemberInfo findConversationMember(String user) {
			for(MemberInfo member : conversationMembers) if(member.name.equals(user)) return member;
			return null;
		}
		
		void removeConversationMember(String user) {
			MemberInfo member = findConversationMember(user);
			if(member != null) conversationMembers.remove(member);
		}
		
		ArrayList<String> getConversationMembersAsCollection() {
			//Creating the array
			ArrayList<String> list = new ArrayList<>(conversationMembers.size());
			for(int i = 0; i < conversationMembers.size(); i++)
				list.add(conversationMembers.get(i).name);
			
			//Returning the list
			return list;
		}
		
		String[] getConversationMembersAsArray() {
			//Creating the array
			String[] array = new String[conversationMembers.size()];
			for(int i = 0; i < conversationMembers.size(); i++)
				array[i] = conversationMembers.get(i).name;
			
			//Returning the array
			return array;
		}
		
		String[] getNormalizedConversationMembersAsArray() {
			//Creating the array
			String[] array = new String[conversationMembers.size()];
			for(int i = 0; i < conversationMembers.size(); i++)
				array[i] = Constants.normalizeAddress(conversationMembers.get(i).name);
			
			//Returning the array
			return array;
		}
		
		ArrayList<MemberInfo> getConversationMembers() {
			return conversationMembers;
		}
		
		void setConversationMembers(ArrayList<MemberInfo> value) {
			conversationMembers = value;
		}
		
		void setConversationMembersCreateColors(List<String> value) {
			//Inheriting the conversation color if there is only one member
			if(value.size() == 1) {
				conversationMembers.add(new MemberInfo(value.get(0), conversationColor));
			} else {
				//Sorting the values
				Collections.sort(value);
				
				//Getting color values
				int[] colorValues = getMassUserColors(value.size());
				
				//Copying the values to the map
				for(int i = 0; i < value.size(); i++)
					conversationMembers.add(new MemberInfo(value.get(i), colorValues[i]));
			}
		}
		
		boolean isGroupChat() {
			return conversationMembers.size() > 1;
		}
		
		void buildTitle(Context context, Constants.TaskedResultCallback<String> resultCallback) {
			//Returning the result of the static method
			buildTitle(context, title, getConversationMembersAsArray(), resultCallback);
		}
		
		static void buildTitle(Context context, String name, String[] members, Constants.TaskedResultCallback<String> resultCallback) {
			//Returning the conversation title if it is valid
			if(name != null && !name.isEmpty()) {
				resultCallback.onResult(name, false);
				return;
			}
			
			//Returning "unknown" if the conversation has no members
			if(members.length == 0) {
				resultCallback.onResult(context.getResources().getString(R.string.part_unknown), false);
				return;
			}
			
			//Checking if there is only one conversation member
			if(members.length == 1) {
				//Getting the user info
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, members[0], new UserCacheHelper.UserFetchResult() {
					@Override
					void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
						//Returning the user's name
						resultCallback.onResult(userInfo != null ? userInfo.getContactName() : members[0], wasTasked);
					}
				});
				
				//Returning
				return;
			}
			
			//Creating the named conversation title list
			ArrayList<String> namedConversationMembers = new ArrayList<>();
			
			//Converting the list to named members
			for(String username : members) {
				//Getting the user info
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, username, new UserCacheHelper.UserFetchResult() {
					@Override
					void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
						//Adding the name
						namedConversationMembers.add(userInfo != null ? userInfo.getContactName() : username);
						
						//Returning if the names have not all been added
						if(members.length != namedConversationMembers.size()) return;
						
						//Creating the string builder
						StringBuilder stringBuilder = new StringBuilder();
						
						//Adding the first name
						stringBuilder.append(namedConversationMembers.get(0));
						
						//Checking if there are more than 2 conversation members
						if(namedConversationMembers.size() > 2)
							for(int i = 1; i < namedConversationMembers.size() - 1; i++)
								stringBuilder.append(", ").append(namedConversationMembers.get(i)); //TODO might cause localization issues
						
						//Adding the last name
						stringBuilder.append(" & ").append(namedConversationMembers.get(namedConversationMembers.size() - 1));
						
						//Returning the string
						resultCallback.onResult(stringBuilder.toString(), wasTasked);
					}
				});
			}
		}
		
		static String buildTitleDirect(Context context, String name, String[] members) {
			//Returning the conversation title if it is valid
			if(name != null && !name.isEmpty()) return name;
			
			//Returning "unknown" if the conversation has no members
			if(members.length == 0) return context.getResources().getString(R.string.part_unknown);
			
			//Returning the member's name if there is only one member
			if(members.length == 1) return members[0];
			
			//Creating the string builder
			StringBuilder stringBuilder = new StringBuilder();
			
			//Adding the first name
			stringBuilder.append(members[0]);
			
			//Checking if there are more than 2 conversation members
			if(members.length > 2)
				for(int i = 1; i < members.length - 1; i++)
					stringBuilder.append(", ").append(members[i]); //TODO might cause localization issues
			
			//Adding the last name
			stringBuilder.append(" & ").append(members[members.length - 1]);
			
			//Returning the string
			return stringBuilder.toString();
		}
		
		String getStaticTitle() {
			return title;
		}
		
		void setTitle(Context context, String value) {
			//Returning if the operation is invalid
			if((title != null && title.equals(value))) return;
			
			//Setting the new title
			title = value;
			
			//Calling the listeners
			for(Runnable runnable : titleChangeListeners) runnable.run();
			
			//Updating the view
			View view = getView();
			if(view != null) {
				//Setting the title
				((TextView) view.findViewById(R.id.title)).setText("");
				buildTitle(context, (title, wasTasked) -> {
					//Setting the title
					View currentView = getView();
					if(currentView == null) return;
					((TextView) currentView.findViewById(R.id.title)).setText(title);
				});
			}
		}
		
		LightConversationItem getLastItem() {
			//Returning the last conversation item
			return lastItem;
		}
		
		void setLastItem(LightConversationItem lastItem) {
			this.lastItem = lastItem;
		}
		
		boolean isSelected() {
			return isSelected;
		}
		
		void setSelected(boolean selected) {
			isSelected = selected;
		}
		
		void updateSelected() {
			//Calling the overload method
			View view = getView();
			if(view != null) updateSelected(view);
		}
		
		private void updateSelected(View itemView) {
			//Setting the visibility of the selected indicator
			itemView.findViewById(R.id.selected).setVisibility(isSelected ? View.VISIBLE : View.GONE);
			if(currentUserViewIndex != -1)
				((ViewGroup) itemView.findViewById(R.id.conversationicon)).getChildAt(currentUserViewIndex).setVisibility(isSelected ? View.GONE : View.VISIBLE);
		}
		
		int getConversationColor() {
			return conversationColor;
		}
		
		void setConversationColor(int conversationColor) {
			//Setting the color
			this.conversationColor = conversationColor;
		}
		
		static int getDefaultConversationColor(String guid) {
			return standardUserColors[new Random(guid.hashCode()).nextInt(standardUserColors.length)];
		}
		
		static String getFormattedTime(long date) {
			//Returning the formatting
			return DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, 0).toString();
		}
		
		void addTitleChangeListener(Runnable runnable) {
			titleChangeListeners.add(runnable);
		}
		
		void removeTitleChangeListener(Runnable runnable) {
			titleChangeListeners.remove(runnable);
		}
		
		void addUnreadCountChangeListener(Runnable runnable) {
			unreadCountChangeListeners.add(runnable);
		}
		
		void removeUnreadCountChangeListener(Runnable runnable) {
			unreadCountChangeListeners.remove(runnable);
		}
		
		ConversationItem findConversationItem(long localID) {
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
		
		ConversationItem findConversationItem(String guid) {
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
		
		AttachmentInfo findAttachmentInfo(long localID) {
			//Getting the list
			ArrayList<ConversationItem> conversationItems = getConversationItems();
			if(conversationItems == null) return null;
			
			//Returning a matching attachment info
			for(ConversationManager.ConversationItem conversationItem : conversationItems)
				if(conversationItem instanceof ConversationManager.MessageInfo)
					for(ConversationManager.AttachmentInfo attachmentInfo : ((ConversationManager.MessageInfo) conversationItem).getAttachments())
						if(attachmentInfo.getGuid() != null && attachmentInfo.getLocalID() == localID)
							return attachmentInfo;
			
			//Returning null
			return null;
		}
		
		AttachmentInfo findAttachmentInfo(String guid) {
			//Getting the list
			ArrayList<ConversationItem> conversationItems = getConversationItems();
			if(conversationItems == null) return null;
			
			//Returning a matching attachment info
			for(ConversationManager.ConversationItem conversationItem : conversationItems)
				if(conversationItem instanceof ConversationManager.MessageInfo)
					for(ConversationManager.AttachmentInfo attachmentInfo : ((ConversationManager.MessageInfo) conversationItem).getAttachments())
						if(attachmentInfo.getGuid() != null && attachmentInfo.getGuid().equals(guid))
							return attachmentInfo;
			
			//Returning null
			return null;
		}
		
		ConversationState getState() {
			return conversationState;
		}
		
		void setState(ConversationState conversationState) {
			this.conversationState = conversationState;
		}
		
		enum ConversationState {
			READY(0), //The conversation is in sync with the server
			INCOMPLETE_SERVER(1), //The conversation is a result of a message from the server, but is missing info
			INCOMPLETE_CLIENT(2); //The conversation was created on the client, but isn't linked to the server
			
			private final int identifier;
			
			ConversationState(int identifier) {
				this.identifier = identifier;
			}
			
			int getIdentifier() {
				return identifier;
			}
			
			static ConversationState fromIdentifier(int identifier) {
				//Returning the matching conversation state
				for(ConversationState conversationState : values())
					if(conversationState.getIdentifier() == identifier) return conversationState;
				
				//Returning null
				return null;
			}
		}
		
		void setEffectCallbacks(Messaging.EffectCallbacks effectCallbacks) {
			this.effectCallbacks = effectCallbacks;
		}
		
		void requestScreenEffect(String effect) {
			//Returning if the callback isn't set up
			if(effectCallbacks == null) return;
			
			//Setting and playing the effect
			effectCallbacks.setCurrentScreenEffect(effect);
			effectCallbacks.playCurrentScreenEffect();
		}
	}
	
	static class MemberInfo implements Serializable {
		private static final long serialVersionUID = 0;
		
		private final String name;
		private int color;
		
		MemberInfo(String name, int color) {
			this.name = name;
			this.color = color;
		}
		
		public String getName() {
			return name;
		}
		
		public int getColor() {
			return color;
		}
		
		public void setColor(int color) {
			this.color = color;
		}
	}
	
	static class MessageInfo extends ConversationItem implements Cloneable {
		//Creating the constants
		static final int itemType = 0;
		private static final int dpDefaultMessagePadding = 5;
		private static final int dpRelatedMessagePadding = 1;
		private static final int dpInterMessagePadding = 2;
		private static final int dpCornerAnchored = 5;
		private static final int dpCornerUnanchored = 20;
		
		//Creating the values
		private final String sender;
		private final MessageTextInfo messageText;
		private final ArrayList<AttachmentInfo> attachments;
		private final String sendEffect;
		private int messageState;
		private int errorCode;
		private long dateRead = -1;
		private boolean isSending = false;
		private float sendProgress = -1;
		
		//Creating the placement values
		private transient boolean hasTimeDivider = false;
		private transient boolean isAnchoredTop = false;
		private transient boolean isAnchoredBottom = false;
		private transient boolean isShowingMessageState = false;
		
		MessageInfo(long localID, String guid, ConversationInfo conversationInfo, String sender, String messageText, ArrayList<AttachmentInfo> attachments, String sendEffect, long date, int messageState, int errorCode, long dateRead) {
			//Calling the super constructor
			super(localID, guid, date, conversationInfo);
			
			//Invalidating the text if it is empty
			//if(messageText != null && messageText.isEmpty()) messageText = null;
			
			//Setting the values
			this.sender = sender;
			this.messageText = messageText == null ? null : new MessageTextInfo(localID, guid, this, messageText);
			this.attachments = attachments;
			this.sendEffect = sendEffect == null || sendEffect.equals(" ") ? "" : sendEffect;
			this.messageState = messageState;
			this.errorCode = errorCode;
			this.dateRead = dateRead;
		}
		
		MessageInfo(long localID, String guid, ConversationInfo conversationInfo, String sender, String messageText, String sendEffect, long date, int messageState, int errorCode, long dateRead) {
			//Calling the super constructor
			super(localID, guid, date, conversationInfo);
			
			//Setting the values
			this.sender = sender;
			this.messageText = messageText == null ? null : new MessageTextInfo(localID, guid, this, messageText);
			this.sendEffect = sendEffect == null || sendEffect.equals(" ") ? "" : sendEffect;
			this.attachments = new ArrayList<>();
			this.messageState = messageState;
			this.errorCode = errorCode;
			this.dateRead = dateRead;
		}
		
		void addAttachment(AttachmentInfo attachment) {
			attachments.add(attachment);
		}
		
		String getSender() {
			return sender;
		}
		
		String getMessageText() {
			return messageText == null ? null : messageText.getText();
		}
		
		ArrayList<AttachmentInfo> getAttachments() {
			return attachments;
		}
		
		boolean isOutgoing() {
			//Returning if the message is outgoing
			return sender == null;
		}
		
		int getMessageState() {
			return messageState;
		}
		
		void setMessageState(int messageState) {
			this.messageState = messageState;
		}
		
		int getErrorCode() {
			return errorCode;
		}
		
		void setErrorCode(int errorCode) {
			this.errorCode = errorCode;
		}
		
		long getDateRead() {
			return dateRead;
		}
		
		void setDateRead(long dateRead) {
			this.dateRead = dateRead;
		}
		
		void setAnchoredTop(boolean anchoredTop) {
			isAnchoredTop = anchoredTop;
		}
		
		void setAnchoredBottom(boolean anchoredBottom) {
			isAnchoredBottom = anchoredBottom;
		}
		
		void updateViewEdges(boolean isLTR) {
			//Calling the overload method
			View view = getView();
			if(view != null) updateViewEdges(view, isLTR);
		}
		
		private void updateViewEdges(View itemView, boolean isLTR) {
			/*
			true + true = true
			true + false = false
			false + true = false
			false + false = true
			 */
			boolean alignToRight = isOutgoing() == isLTR;
			
			//Updating the padding
			itemView.setPadding(itemView.getPaddingLeft(), Constants.dpToPx(isAnchoredTop ? dpRelatedMessagePadding : dpDefaultMessagePadding), itemView.getPaddingRight(), Constants.dpToPx(isAnchoredBottom ? dpRelatedMessagePadding : dpDefaultMessagePadding));
			
			//Checking if the message is outgoing
			if(!isOutgoing()) {
				//Setting the user information
				boolean showUserInfo = !isAnchoredTop; //If the message isn't anchored to the top
				if(getConversationInfo().isGroupChat())
					itemView.findViewById(R.id.sender).setVisibility(showUserInfo ? View.VISIBLE : View.GONE);
				itemView.findViewById(R.id.profile).setVisibility(showUserInfo ? View.VISIBLE : View.GONE);
			}
			
			//Getting the corner values in pixels
			int pxCornerAnchored = Constants.dpToPx(dpCornerAnchored);
			int pxCornerUnanchored = Constants.dpToPx(dpCornerUnanchored);
			
			//Checking if there is text
			if(messageText != null) messageText.updateViewEdges(itemView.findViewById(R.id.content_text), isAnchoredTop, isAnchoredBottom || !attachments.isEmpty(), alignToRight, pxCornerAnchored, pxCornerUnanchored);
			
			//Iterating over the attachments
			for(int i = 0; i < attachments.size(); i++) {
				//Getting the attachment view
				View attachmentView = ((ViewGroup) itemView.findViewById(R.id.messagepart_container)).getChildAt((messageText == null ? 0 : 1) + i);
				
				//Updating the padding
				attachmentView.setPadding(attachmentView.getPaddingLeft(), messageText != null || i > 0 ? Constants.dpToPx(dpInterMessagePadding) : 0, attachmentView.getPaddingRight(), attachmentView.getPaddingBottom());
				
				//Updating the attachment's edges
				attachments.get(i).updateViewEdges(attachmentView,
						messageText != null || i > 0 || isAnchoredTop, //There is message text above, there is an attachment above or the message is anchored anyways
						i < attachments.size() - 1 || isAnchoredBottom, //There is an attachment below or the message is anchored anyways
						alignToRight,
						pxCornerAnchored, pxCornerUnanchored);
			}
		}
		
		private void prepareActivityStateDisplay(View itemView, Context context) {
			//Getting the requested state
			isShowingMessageState = this == getConversationInfo().getActivityStateTarget() &&
					messageState != SharedValues.MessageInfo.stateCodeGhost &&
					messageState != SharedValues.MessageInfo.stateCodeIdle &&
					messageState != SharedValues.MessageInfo.stateCodeSent;
			
			//Getting the activity status label
			TextSwitcher label = itemView.findViewById(R.id.activitystatus);
			
			//Setting up the label
			if(isShowingMessageState) {
				label.setVisibility(View.VISIBLE);
				label.setCurrentText(getDeliveryStatusText(context));
			} else {
				label.setVisibility(View.GONE);
			}
		}
		
		void updateActivityStateDisplay(Context context) {
			//Getting the requested state
			boolean requestedState = this == getConversationInfo().getActivityStateTarget() &&
					messageState != SharedValues.MessageInfo.stateCodeGhost &&
					messageState != SharedValues.MessageInfo.stateCodeIdle &&
					messageState != SharedValues.MessageInfo.stateCodeSent;
			
			//Calling the overload method
			View view = getView();
			if(view != null) updateActivityStateDisplay(view, context, isShowingMessageState, requestedState);
			
			//Setting the current state
			isShowingMessageState = requestedState;
		}
		
		private void updateActivityStateDisplay(View itemView, Context context, boolean currentState, boolean requestedState) {
			//Getting the activity status label
			TextSwitcher label = itemView.findViewById(R.id.activitystatus);
			
			//Checking if the requested state matches the current state
			if(requestedState == currentState) {
				//Updating the text
				if(requestedState) label.setText(getDeliveryStatusText(context));
			} else {
				//Checking if the conversation should display its state
				if(requestedState) {
					//Setting the text
					label.setCurrentText(getDeliveryStatusText(context));
					
					//Showing the label
					label.setVisibility(View.VISIBLE);
					label.startAnimation(AnimationUtils.loadAnimation(context, R.anim.messagestatus_slide_in_bottom));
					
					//Measuring the label
					label.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
					
					//Expanding the parent view
					ViewGroup parentView = (ViewGroup) label.getParent();
					parentView.getLayoutParams().height = parentView.getHeight(); //Freezing the parent view height (to prevent it from expanding for a few moments before the label's view pass)
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) label.getLayoutParams();
					Constants.ResizeAnimation parentAnim = new Constants.ResizeAnimation(parentView, parentView.getHeight(), parentView.getHeight() + (label.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin));
					parentAnim.setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime));
					parentAnim.setInterpolator(new AccelerateDecelerateInterpolator());
					parentView.startAnimation(parentAnim);
				} else {
					//Hiding the label
					Animation labelAnim = AnimationUtils.loadAnimation(context, R.anim.messagestatus_slide_out_top);
					labelAnim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							//Setting the label's visibility
							View view = getView();
							if(view != null) view.findViewById(R.id.activitystatus).setVisibility(View.GONE);
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {}
					});
					label.startAnimation(labelAnim);
					
					//Collapsing the parent view
					ViewGroup parentView = (ViewGroup) label.getParent();
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) label.getLayoutParams();
					Constants.ResizeAnimation parentAnim = new Constants.ResizeAnimation(parentView, parentView.getHeight(), parentView.getHeight() - (label.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin));
					parentAnim.setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime));
					parentAnim.setInterpolator(new AccelerateDecelerateInterpolator());
					parentView.startAnimation(parentAnim);
				}
			}
		}
		
		private CharSequence getDeliveryStatusText(Context context) {
			//Getting the state
			switch(messageState) {
				default:
					return null;
				case SharedValues.MessageInfo.stateCodeDelivered:
					return context.getResources().getString(R.string.state_delivered);
				case SharedValues.MessageInfo.stateCodeRead: {
					//Creating the when variable
					String when;
					
					//Creating the calendars
					Calendar calNow = Calendar.getInstance();
					Calendar calThen = Calendar.getInstance();
					calThen.setTimeInMillis(dateRead);
					
					//Formatting the when as the time if the days are the same
					if(calNow.get(Calendar.ERA) == calThen.get(Calendar.ERA) &&
							calNow.get(Calendar.YEAR) == calThen.get(Calendar.YEAR) &&
							calNow.get(Calendar.DAY_OF_YEAR) == calThen.get(Calendar.DAY_OF_YEAR)) when = DateFormat.getTimeInstance(DateFormat.SHORT).format(dateRead);
					//Otherwise formatting the when as the date
					else when = DateFormat.getDateInstance(DateFormat.SHORT).format(dateRead);
					
					return Html.fromHtml("<b>" + context.getResources().getString(R.string.state_read) + "</b> " + when);
				}
			}
		}
		
		boolean sendMessage(Context context) {
			//Creating the callback listener
			ConnectionService.MessageResponseManager messageResponseManager = new ConnectionService.MessageResponseManager() {
				@Override
				void onSuccess() {
				
				}
				
				@Override
				void onFail(byte resultCode) {
					//Setting the error code
					errorCode = uploadToMessageErrorCode(resultCode);
					
					//Getting the context
					Context context = MainApplication.getInstance();
					if(context == null) return;
					
					//Updating the view
					View view = getView();
					if(view != null) updateViewProgressState(view, context);
					
					//Updating the message's database entry
					new UpdateErrorCodeTask(context, getLocalID(), errorCode).execute();
				}
			};
			
			//Checking if the service is dead
			ConnectionService connectionService = ConnectionService.getInstance();
			if(connectionService == null) {
				//Starting the service
				/* if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(new Intent(context, ConnectionService.class));
				else context.startService(new Intent(context, ConnectionService.class)); */
				context.startService(new Intent(context, ConnectionService.class));
				
				//Telling the response manager
				messageResponseManager.onFail(ConnectionService.messageSendNetworkException);
				
				//Returning false
				return false;
			}
			
			//Getting the view
			View view = getView();
			
			//Hiding the error view
			if(view != null) view.findViewById(R.id.send_error).setVisibility(View.GONE);
			
			//Checking if there is text
			if(messageText != null) {
				//Hiding the progress views
				if(view != null) {
					view.findViewById(R.id.send_progress).setVisibility(View.GONE);
					//view.findViewById(R.id.sendProgressIndeterminate).setVisibility(View.GONE);
					//view.findViewById(R.id.sendProgressDeterminate).setVisibility(View.GONE);
				}
				
				//Sending the message and returning the result
				return getConversationInfo().getState() == ConversationManager.ConversationInfo.ConversationState.READY ?
						connectionService.sendMessage(getConversationInfo().getGuid(), getMessageText(), messageResponseManager) :
						connectionService.sendMessage(getConversationInfo().getNormalizedConversationMembersAsArray(), getMessageText(), getConversationInfo().getService(), messageResponseManager);
			} else {
				//Returning false if there are no attachments
				if(attachments.isEmpty()) return false;
				
				//Showing and configuring the progress view
				if(view != null) {
					ProgressWheel progressBar = view.findViewById(R.id.send_progress);
					progressBar.setVisibility(View.VISIBLE);
					progressBar.spin();
				}
				
				//Getting the attachment
				AttachmentInfo attachmentInfo = attachments.get(0);
				
				//Setting the upload values
				isSending = true;
				sendProgress = -1;
				
				//Creating the callbacks
				ConnectionService.FileSendRequestCallbacks callbacks = new ConnectionService.FileSendRequestCallbacks() {
					@Override
					public void onStart() {
						//Getting the view
						View view = getView();
						if(view == null) return;
						
						//Updating the progress bar
						ProgressWheel progressBar = view.findViewById(R.id.send_progress);
						progressBar.setProgress(0);
					}
					
					@Override
					public void onCopyFinished(File location) {
						//Setting the data
						AttachmentInfo attachmentInfo = attachments.get(0);
						attachmentInfo.file = location;
						attachmentInfo.fileUri = null;
						
						//Updating the view
						attachmentInfo.updateContentView();
					}
					
					@Override
					public void onUploadFinished(byte[] checksum) {
						//Setting the checksum
						attachments.get(0).setFileChecksum(checksum);
						
						//Getting the view
						View view = getView();
						if(view == null) return;
						
						//Updating the progress bar
						((ProgressWheel) view.findViewById(R.id.send_progress)).setProgress(1);
					}
					
					@Override
					public void onResponseReceived() {
						//Forwarding the event to the response manager
						messageResponseManager.onSuccess();
						
						//Setting the message as not sending
						isSending = false;
						
						//Getting the view
						View view = getView();
						if(view == null) return;
						
						//Hiding the progress bar
						view.findViewById(R.id.send_progress).setVisibility(View.GONE);
					}
					
					@Override
					public void onFail(byte resultCode) {
						//Forwarding the event to the response manager
						messageResponseManager.onFail(resultCode);
						
						//Setting the message as not sending
						isSending = false;
						
						//Getting the view
						View view = getView();
						if(view == null) return;
						
						//Hiding the progress bar
						view.findViewById(R.id.send_progress).setVisibility(View.GONE);
					}
					
					@Override
					public void onProgress(float value) {
						//Setting the send progress
						sendProgress = value;
						
						//Getting the view
						View view = getView();
						if(view == null) return;
						
						//Updating the progress bar
						((ProgressWheel) view.findViewById(R.id.send_progress)).setProgress(value);
					}
				};
				
				//Sending the attachment
				if(attachmentInfo.file != null) connectionService.queueUploadRequest(callbacks, attachmentInfo.file, getConversationInfo(), attachmentInfo.localID);
				else connectionService.queueUploadRequest(callbacks, attachmentInfo.fileUri, getConversationInfo(), attachmentInfo.localID);
				
				//Returning true
				return true;
			}
		}
		
		private static byte uploadToMessageErrorCode(byte code) {
			switch(code) {
				case ConnectionService.messageSendInvalidContent:
					return Constants.messageErrorCodeAirInvalidContent;
				case ConnectionService.messageSendFileTooLarge:
					return Constants.messageErrorCodeAirFileTooLarge;
				case ConnectionService.messageSendIOException:
					return Constants.messageErrorCodeAirIO;
				case ConnectionService.messageSendNetworkException:
					return Constants.messageErrorCodeAirNetwork;
				case ConnectionService.messageSendExternalException:
					return Constants.messageErrorCodeAirExternal;
				case ConnectionService.messageSendRequestExpired:
					return Constants.messageErrorCodeAirExpired;
				case ConnectionService.messageSendReferencesLost:
					return Constants.messageErrorCodeAirReferences;
				default:
					throw new UnsupportedOperationException("Received upload request error code (" + code + ") which is out of range");
			}
		}
		
		private static class UpdateErrorCodeTask extends AsyncTask<Void, Void, Void> {
			private final WeakReference<Context> contextReference;
			private final long messageID;
			private final int errorCode;
			
			UpdateErrorCodeTask(Context context, long messageID, int errorCode) {
				contextReference = new WeakReference<>(context);
				this.messageID = messageID;
				this.errorCode = errorCode;
			}
			
			@Override
			protected Void doInBackground(Void... parameters) {
				//Getting the context
				Context context = contextReference.get();
				if(context == null) return null;
				
				//Updating the entry in the database
				DatabaseManager.updateMessageErrorCode(DatabaseManager.getWritableDatabase(context), messageID, errorCode);
				
				//Returning
				return null;
			}
		}
		
		void updateTimeDivider(Context context) {
			//Calling the overload method
			View view = getView();
			if(view != null) configureTimeDivider(context, view.findViewById(R.id.timedivider), hasTimeDivider);
		}
		
		private void configureTimeDivider(Context context, TextView textView, boolean visible) {
			//Checking if the time divider should be visible
			if(visible) {
				//Setting the time
				textView.setText(generateTimeDividerString(context));
				
				//Setting the time divider's visibility
				textView.setVisibility(View.VISIBLE);
			} else textView.setVisibility(View.GONE);
		}
		
		private String generateTimeDividerString(Context context) {
			//Getting the calendars
			Calendar nowCalendar = Calendar.getInstance();
			Calendar sentCalendar = Calendar.getInstance();
			sentCalendar.setTimeInMillis(getDate());
			
			//Creating the date
			Date sentDate = new Date(getDate());
			
			//Checking if the calendars are of the same year
			if(sentCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)) {
				//If the message was sent today
				if(nowCalendar.get(Calendar.DAY_OF_YEAR) == sentCalendar.get(Calendar.DAY_OF_YEAR))
					return context.getResources().getString(R.string.time_today) + ConversationInfo.bullet + android.text.format.DateFormat.getTimeFormat(context).format(sentDate);
					//If the message was sent yesterday
				else {
					nowCalendar.add(Calendar.DAY_OF_YEAR, -1); //Today (now) -> Yesterday
					if(nowCalendar.get(Calendar.DAY_OF_YEAR) == sentCalendar.get(Calendar.DAY_OF_YEAR))
						return context.getResources().getString(R.string.time_yesterday) + ConversationInfo.bullet + android.text.format.DateFormat.getTimeFormat(context).format(sentDate);
				}
			}
			
			//Returning an absolute time
			return android.text.format.DateFormat.getDateFormat(context).format(sentDate) + ConversationInfo.bullet + android.text.format.DateFormat.getTimeFormat(context).format(sentDate);
		}
		
		void setHasTimeDivider(boolean hasTimeDivider) {
			this.hasTimeDivider = hasTimeDivider;
		}
		
		@Override
		void bindView(Context context, RecyclerView.ViewHolder viewHolder) {
			//Casting the view holder
			MessageViewHolder pViewHolder = (MessageViewHolder) viewHolder;
			
			//Getting the properties
			boolean isFromMe = isOutgoing();
			
			//Getting the message part container
			ViewGroup messagePartContainer = pViewHolder.getGroupMPC();
			
			//Setting the message part container's draw order
			messagePartContainer.setZ(1);
			
			//Converting the view to a view group
			LinearLayout view = pViewHolder.getView();
			
			{
				//Getting the text content
				View textContent = messagePartContainer.findViewById(R.id.content_text);
				
				//Checking if there is text
				if(messageText != null) {
					//Creating the view
					View textView = messageText.createView(context, textContent, messagePartContainer);
					
					//Adding the view if it wasn't recycled
					if(textContent == null) messagePartContainer.addView(textView, 0);
					
					//Updating the view source
					messageText.setViewSource(() -> {
						//Getting the view
						View itemView = getView();
						if(itemView == null) return null;
						
						//Returning the view
						return ((ViewGroup) itemView.findViewById(R.id.messagepart_container)).getChildAt(0);
					});
				}
				//Removing the view if it shouldn't be there
				else if(textContent != null) messagePartContainer.removeView(textContent);
				
				//Counting the other views
				SparseArray<List<View>> attachmentViews = new SparseArray<>();
				attachmentViews.put(ContentType.IMAGE.getIdentifier(), new ArrayList<>());
				attachmentViews.put(ContentType.AUDIO.getIdentifier(), new ArrayList<>());
				attachmentViews.put(ContentType.VIDEO.getIdentifier(), new ArrayList<>());
				attachmentViews.put(ContentType.OTHER.getIdentifier(), new ArrayList<>());
				
				//Iterating backwards (so that child views can be removed while iterating)
				for(int i = messagePartContainer.getChildCount() - 1; i >= (messageText == null ? 0 : 1); i--) {
					//Getting the child
					View child = messagePartContainer.getChildAt(i);
					
					//Sorting the child
					switch(child.getId()) {
						case R.id.content_image:
							attachmentViews.get(ContentType.IMAGE.getIdentifier()).add(child);
							break;
						case R.id.content_audio:
							attachmentViews.get(ContentType.AUDIO.getIdentifier()).add(child);
							break;
						case R.id.content_video:
							attachmentViews.get(ContentType.VIDEO.getIdentifier()).add(child);
							break;
						case R.id.content_other:
							attachmentViews.get(ContentType.OTHER.getIdentifier()).add(child);
							break;
					}
					
					//Removing the view
					messagePartContainer.removeView(child);
				}
				
				//Assigning views to the attachments
				for(int i = 0; i < attachments.size(); i++) {
					//Getting the attachment info
					AttachmentInfo attachment = attachments.get(i);
					
					//Getting the associated list
					List<View> existingAttachmentViews = attachmentViews.get(attachment.getContentType().getIdentifier());
					
					//Getting the view
					View attachmentView;
					if(existingAttachmentViews.isEmpty())
						attachmentView = attachment.createView(context, null, messagePartContainer);
					else {
						attachmentView = attachment.createView(context, existingAttachmentViews.get(0), messagePartContainer);
						existingAttachmentViews.remove(0);
					}
					
					//Adding the view
					messagePartContainer.addView(attachmentView);
					int viewIndex = messagePartContainer.indexOfChild(attachmentView);
					
					//Setting the view source
					attachment.setViewSource(() -> {
						//Getting the view
						View itemView = getView();
						if(itemView == null) return null;
						
						//Returning the view
						return ((ViewGroup) itemView.findViewById(R.id.messagepart_container)).getChildAt(viewIndex);
					});
				}
			}
			
			//Setting the message part container's gravity
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) messagePartContainer.getLayoutParams();
			layoutParams.gravity = isFromMe ? Gravity.END : Gravity.START;
			messagePartContainer.setLayoutParams(layoutParams);
			
			//Checking if the message is outgoing
			if(isFromMe) {
				//Hiding the user info
				ViewGroup profileView = pViewHolder.getProfile();
				if(profileView != null) profileView.setVisibility(View.GONE);
				
				//Hiding the sender
				pViewHolder.getLabelSender().setVisibility(View.GONE);
			} else {
				//Inflating the profile stub and getting the profile view
				pViewHolder.inflateProfile();
				ViewGroup profileView = pViewHolder.getProfile();
				
				//Showing the profile view
				if(profileView.getVisibility() != View.VISIBLE) profileView.setVisibility(View.VISIBLE);
				
				//Removing the profile image
				pViewHolder.getImageProfileImage().setImageBitmap(null);
				pViewHolder.getImageProfileDefault().setVisibility(View.VISIBLE);
				
				//Assigning the profile image
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, sender, sender, new BitmapCacheHelper.ImageDecodeResult() {
					@Override
					void onImageMeasured(int width, int height) {}
					
					@Override
					void onImageDecoded(Bitmap result, boolean wasTasked) {
						//Returning if the result is invalid
						if(result == null) return;
						
						View currentView = wasTasked ? getView() : view;
						if(currentView == null) return;
						
						//Getting the icon view
						View iconView = currentView.findViewById(R.id.profile);
						
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
				
				//Checking if the chat is a group chat
				if(getConversationInfo().isGroupChat()) {
					//Getting the sender
					TextView labelSender = pViewHolder.getLabelSender();
					
					//Setting the sender's name (temporarily)
					labelSender.setText(sender);
					
					//Assigning the sender's name
					MainApplication.getInstance().getUserCacheHelper().assignUserInfo(context, sender, wasTasked -> {
						//Getting the view
						View currentView = wasTasked ? getView() : view;
						if(currentView == null) return null;
						
						//Returning the sender label
						return currentView.findViewById(R.id.sender);
					});
					
					//Showing the sender
					labelSender.setVisibility(View.VISIBLE);
				} else {
					//Hiding the sender
					pViewHolder.getLabelSender().setVisibility(View.GONE);
				}
			}
			
			//Checking if the message has no send effect
			if(sendEffect.isEmpty()) {
				//Hiding the "replay" button
				pViewHolder.getGroupEffectReplay().setVisibility(View.GONE);
			} else {
				//Showing and configuring the "replay" button
				View replayButton = pViewHolder.getGroupEffectReplay();
				replayButton.setVisibility(View.VISIBLE);
				replayButton.setOnClickListener(clickedView -> getConversationInfo().requestScreenEffect(sendEffect));
			}
			
			//Setting the text switcher's animations
			pViewHolder.getLabelActivityStatus().setInAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_delayed));
			pViewHolder.getLabelActivityStatus().setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
			
			//Setting the status
			//((TextView) view.findViewById(R.id.status)).setText(ConversationInfo.getFormattedTime(getDate()));
			
			//Updating the view edges
			updateViewEdges(view, context.getResources().getBoolean(R.bool.is_left_to_right));
			
			//Updating the view state display
			prepareActivityStateDisplay(view, context);
			
			//Updating the view color
			updateViewColor(view, false);
			
			//Updating the view state
			updateViewProgressState(view, context);
			
			//Updating the time divider
			configureTimeDivider(context, pViewHolder.getLabelTimeDivider(), hasTimeDivider);
			
			//Building the sticker view
			//buildStickerView(view);
			
			//Building the tapback view
			//buildTapbackView(view);
			
			//Restoring the upload state
			restoreUploadState(pViewHolder);
			
			//Setting the gravity
			view.setGravity(isFromMe ? Gravity.END : Gravity.START);
		}
		
		@Override
		void updateViewColor() {
			//Calling the overload method
			View view = getView();
			if(view != null) updateViewColor(view, true);
		}
		
		private void updateViewColor(View itemView, boolean updateAttachments) {
			//Setting the user tint
			if(!isOutgoing()) {
				MemberInfo memberInfo = getConversationInfo().findConversationMember(sender);
				int backgroundColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				
				((ImageView) itemView.findViewById(R.id.profile).findViewById(R.id.profile_default))
							.setColorFilter(backgroundColor, android.graphics.PorterDuff.Mode.MULTIPLY);
			}
			
			//Setting the upload spinner tint
			((ProgressWheel) itemView.findViewById(R.id.send_progress)).setBarColor(getConversationInfo().getConversationColor());
			
			//Getting the message part container
			ViewGroup messagePartContainer = itemView.findViewById(R.id.messagepart_container);
			
			//Updating the message colors
			if(messageText != null) messageText.updateViewColor(messagePartContainer.findViewById(R.id.content_text));
			
			//Updating the attachment colors
			if(updateAttachments) for(int i = 0; i < attachments.size(); i++)
				attachments.get(i).updateViewColor(messagePartContainer.getChildAt((messageText == null ? 0 : 1) + i));
		}
		
		void updateViewProgressState(Context context) {
			//Calling the overload method
			View view = getView();
			if(view != null) updateViewProgressState(view, context);
		}
		
		private static final float ghostAlpha = 0.50F;
		private void updateViewProgressState(View itemView, Context context) {
			//Setting the message part container's alpha
			ViewGroup messagePartContainer = itemView.findViewById(R.id.messagepart_container);
			if(messageState == SharedValues.MessageInfo.stateCodeGhost) messagePartContainer.setAlpha(ghostAlpha);
			else messagePartContainer.setAlpha(1);
			
			//Getting the send error warning
			View sendError = itemView.findViewById(R.id.send_error);
			
			//Hiding the error and returning if there wasn't any problem
			if(errorCode == Constants.messageErrorCodeOK) {
				sendError.setVisibility(View.GONE);
				return;
			}
			
			//Showing the error
			sendError.setVisibility(View.VISIBLE);
			
			//Configuring the dialog
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(itemView.getContext())
					.setTitle(R.string.message_messageerror_title)
					.setNegativeButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss());
			boolean showRetryButton;
			
			switch(errorCode) {
				default:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_unknownerror);
					
					//Enabling the retry button
					showRetryButton = true;
					
					break;
				
				case Constants.messageErrorCodeAirInvalidContent:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_air_invalidcontent);
					
					//Disabling the retry button
					showRetryButton = false;
					
					break;
				case Constants.messageErrorCodeAirFileTooLarge:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_air_filetoolarge);
					
					//Disabling the retry button
					showRetryButton = false;
					
					break;
				case Constants.messageErrorCodeAirIO:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_air_io);
					
					//Enabling the retry button
					showRetryButton = true;
					
					break;
				case Constants.messageErrorCodeAirNetwork:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_air_network);
					
					//Enabling the retry button
					showRetryButton = true;
					
					break;
				case Constants.messageErrorCodeAirExternal:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_air_external);
					
					//Enabling the retry button
					showRetryButton = true;
					
					break;
				case Constants.messageErrorCodeAirExpired:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_air_expired);
					
					//Enabling the retry button
					showRetryButton = true;
					
					break;
				case Constants.messageErrorCodeAirReferences:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_air_references);
					
					//Enabling the retry button
					showRetryButton = true;
					
					break;
				case Constants.messageErrorCodeAppleNetwork:
					//Setting the message
					dialogBuilder.setMessage(R.string.message_messageerror_desc_apple_network);
					
					//Disabling the retry button
					showRetryButton = false;
					
					break;
				case Constants.messageErrorCodeAppleUnregistered:
					//Setting the message
					dialogBuilder.setMessage(getConversationInfo().getConversationMembers().isEmpty() ?
							context.getResources().getString(R.string.message_messageerror_desc_apple_unregistered_generic) :
							context.getResources().getString(R.string.message_messageerror_desc_apple_unregistered, getConversationInfo().getConversationMembers().get(0).getName()));
					
					//Disabling the retry button
					showRetryButton = false;
					
					break;
			}
			
			//Showing the retry button (if requested)
			if(showRetryButton)
				dialogBuilder.setPositiveButton(R.string.action_retry, (dialog, which) -> sendMessage(MainApplication.getInstance()));
			
			//Showing the dialog when the button is clicked
			sendError.setOnClickListener(view -> dialogBuilder.create().show());
		}
		
		void animateGhostStateChanges() {
			View view = getView();
			if(view == null) return;
			
			View messagePartContainer = view.findViewById(R.id.messagepart_container);
			messagePartContainer.setAlpha(ghostAlpha);
			messagePartContainer.animate().alpha(1).start();
		}
		
		private void restoreUploadState(MessageViewHolder viewHolder) {
			//Getting the progress bar
			ProgressWheel progressBar = viewHolder.getProgressSend();
			
			//Checking if there is an upload in progress
			if(isSending) {
				//Showing the progress bar
				progressBar.setVisibility(View.VISIBLE);
				
				//Configuring the progress view
				if(sendProgress == -1) progressBar.spin();
				else progressBar.setInstantProgress(sendProgress);
			} else {
				//Hiding the progress bar
				progressBar.setVisibility(View.GONE);
			}
		}
		
		String getSendEffect() {
			return sendEffect;
		}
		
		@Override
		void getSummary(Context context, Constants.ResultCallback<String> callback) {
			//Converting the attachment list to a string resource list
			ArrayList<Integer> attachmentStringRes = new ArrayList<>();
			for(AttachmentInfo attachment : attachments)
				attachmentStringRes.add(attachment.getContentType().getName());
			
			//Returning the summary
			callback.onResult(false, getSummary(context, isOutgoing(), getMessageText(), sendEffect, attachmentStringRes));
		}
		
		String getSummary(Context context) {
			//Converting the attachment list to a string resource list
			ArrayList<Integer> attachmentStringRes = new ArrayList<>();
			for(AttachmentInfo attachment : attachments)
				attachmentStringRes.add(attachment.getContentType().getName());
			
			//Returning the result of the static method
			return getSummary(context, isOutgoing(), getMessageText(), sendEffect, attachmentStringRes);
		}
		
		static String getSummary(Context context, boolean isFromMe, String messageText, String sendStyle, ArrayList<Integer> attachmentStringRes) {
			//Creating the prefix
			String prefix = isFromMe ? context.getString(R.string.prefix_you) + " " : "";
			
			//Removing line breaks
			String modifiedMessage = messageText != null ? messageText.replace('\n', ' ') : null;
			
			//Applying invisible ink
			if(sendStyle.equals(Constants.appleSendStyleInvisibleInk))
				modifiedMessage = context.getString(R.string.message_messageeffect_invisibleink);
			
			//Setting the text if there is text
			if(messageText != null) return prefix + modifiedMessage;
			
			//Setting the attachments if there are attachments
			if(attachmentStringRes.size() == 1)
				return prefix + context.getResources().getString(attachmentStringRes.get(0));
			else if(attachmentStringRes.size() > 1)
				return prefix + context.getResources().getQuantityString(R.plurals.message_multipleattachments, attachmentStringRes.size(), attachmentStringRes.size());
			
			//Returning an empty string
			return "";
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
		
		@Override
		void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
			getSummary(context, (wasTasked, result) -> callback.onResult(wasTasked, new LightConversationItem(result, getDate())));
		}
		
		@Override
		LightConversationItem toLightConversationItemSync(Context context) {
			//Converting the attachment list to a string resource list
			ArrayList<Integer> attachmentStringRes = new ArrayList<>();
			for(AttachmentInfo attachment : attachments)
				attachmentStringRes.add(attachment.getContentType().getName());
			
			//Returning the summary
			return new LightConversationItem(getSummary(context, isOutgoing(), getMessageText(), sendEffect, attachmentStringRes), getDate());
		}
		
		void addSticker(StickerInfo sticker) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(sticker.messageIndex);
			if(component == null) return;
			
			component.addSticker(sticker);
		}
		
		/* private void buildStickerView(View itemView) {
			//Getting the message part container
			ViewGroup messagePartContainer = itemView.findViewById(R.id.messagepart_container);
			
			//Building the tapback views of the components
			if(messageText != null) messageText.buildStickerView(messagePartContainer.getChildAt(0));
			for(int i = 0; i < attachments.size(); i++) attachments.get(i).buildStickerView(messagePartContainer.getChildAt((messageText == null ? 0 : 1) + i));
		} */
		
		void addLiveSticker(StickerInfo sticker) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(sticker.messageIndex);
			if(component == null) return;
			
			component.addLiveSticker(sticker);
		}
		
		void addLiveTapback(TapbackInfo tapback) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(tapback.messageIndex);
			if(component == null) return;
			
			component.addLiveTapback(tapback);
		}
		
		void addTapback(TapbackInfo tapback) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(tapback.messageIndex);
			if(component == null) return;
			
			component.addTapback(tapback);
		}
		
		void removeLiveTapback(String sender, int messageIndex) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(messageIndex);
			if(component == null) return;
			
			component.removeLiveTapback(sender);
		}
		
		private MessageComponent getComponentAtIndex(int index) {
			if(messageText == null) {
				if(index < attachments.size()) return attachments.get(index);
				else return null;
			} else {
				if(index == 0) return messageText;
				else if(index - 1 < attachments.size()) return attachments.get(index - 1);
				else return null;
			}
		}
		
		/* private void buildTapbackView(View itemView) {
			//Getting the message part container
			ViewGroup messagePartContainer = itemView.findViewById(R.id.messagepart_container);
			
			//Building the tapback views of the components
			if(messageText != null) messageText.buildTapbackView(messagePartContainer.getChildAt(0));
			for(int i = 0; i < attachments.size(); i++) attachments.get(i).buildTapbackView(messagePartContainer.getChildAt((messageText == null ? 0 : 1) + i));
		} */
	}
	
	static abstract class MessageComponent {
		//Creating the data values
		long localID;
		String guid;
		final MessageInfo messageInfo;
		
		private Constants.ViewSource viewSource;
		
		//Creating the modifier values
		final ArrayList<StickerInfo> stickers;
		final ArrayList<TapbackInfo> tapbacks;
		
		//Creating the state values
		boolean contextMenuOpen = false;
		
		MessageComponent(long localID, String guid, MessageInfo messageInfo) {
			//Setting the values
			this.localID = localID;
			this.guid = guid;
			this.messageInfo = messageInfo;
			
			//Setting the modifiers
			stickers = new ArrayList<>();
			tapbacks = new ArrayList<>();
		}
		
		MessageComponent(long localID, String guid, MessageInfo messageInfo, ArrayList<StickerInfo> stickers, ArrayList<TapbackInfo> tapbacks) {
			//Setting the values
			this.localID = localID;
			this.guid = guid;
			this.messageInfo = messageInfo;
			
			//Setting the modifiers
			this.stickers = stickers;
			this.tapbacks = tapbacks;
		}
		
		void setViewSource(Constants.ViewSource viewSource) {
			this.viewSource = viewSource;
		}
		
		View getView() {
			return viewSource == null ? null : viewSource.get();
		}
		
		long getLocalID() {
			return localID;
		}
		
		void setLocalID(long localID) {
			this.localID = localID;
		}
		
		String getGuid() {
			return guid;
		}
		
		void setGuid(String guid) {
			this.guid = guid;
		}
		
		MessageInfo getMessageInfo() {
			return messageInfo;
		}
		
		abstract View createView(Context context, View convertView, ViewGroup parent);
		
		void buildCommonViews(View view) {
			//Building the sticker view
			buildStickerView(view);
			
			//Building the tapback view
			buildTapbackView(view);
		}
		
		abstract void updateViewColor(View itemView);
		
		abstract void updateViewEdges(View itemView, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored);
		
		void buildStickerView(View itemView) {
			//Clearing all previous stickers
			((ViewGroup) itemView.findViewById(R.id.sticker_container)).removeAllViews();
			
			//Iterating over the stickers
			for(StickerInfo sticker : stickers) {
				//Decoding the sticker
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromCompressedBytes(sticker.guid, sticker.compressedData, new BitmapCacheHelper.ImageDecodeResult() {
					@Override
					void onImageMeasured(int width, int height) {}
					
					@Override
					void onImageDecoded(Bitmap result, boolean wasTasked) {
						//Returning if the bitmap is invalid
						if(result == null) return;
						
						//Getting the view
						View view = wasTasked ? getView() : itemView;
						
						//Returning if the view is invalid
						if(view == null) return;
						
						//Determining the maximum image size
						DisplayMetrics displayMetrics = new DisplayMetrics();
						((WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
						int maxStickerSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) / 3; //One third of the smaller side of the display
						
						//Creating the image view
						ImageView imageView = new ImageView(view.getContext());
						RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
						imageView.setLayoutParams(layoutParams);
						imageView.setMaxWidth(maxStickerSize);
						imageView.setMaxHeight(maxStickerSize);
						imageView.setImageBitmap(result);
						
						//Adding the view to the sticker container
						((ViewGroup) view.findViewById(R.id.sticker_container)).addView(imageView);
						
						//Setting the bitmap
						imageView.setImageBitmap(result);
					}
				});
			}
		}
		
		void addSticker(StickerInfo sticker) {
			//Adding the sticker to the sticker list
			stickers.add(sticker);
		}
		
		void addLiveSticker(StickerInfo sticker) {
			//Adding the sticker to the sticker list
			stickers.add(sticker);
			
			//Decoding the sticker
			MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromCompressedBytes(sticker.guid, sticker.compressedData, new BitmapCacheHelper.ImageDecodeResult() {
				@Override
				void onImageMeasured(int width, int height) {}
				
				@Override
				void onImageDecoded(Bitmap result, boolean wasTasked) {
					//Getting the view
					View view = getView();
					
					//Returning if the view is invalid
					if(view == null) return;
					
					//Determining the maximum image size
					DisplayMetrics displayMetrics = new DisplayMetrics();
					((WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
					int maxStickerSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) / 3; //One third of the smaller side of the display
					
					//Creating the image view
					ImageView imageView = new ImageView(view.getContext());
					RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
					imageView.setLayoutParams(layoutParams);
					imageView.setMaxWidth(maxStickerSize);
					imageView.setMaxHeight(maxStickerSize);
					imageView.setImageBitmap(result);
					
					//Adding the view to the sticker container
					((ViewGroup) view.findViewById(R.id.sticker_container)).addView(imageView);
					
					//Setting the bitmap
					imageView.setImageBitmap(result);
					
					//Checking if the stickers should be shown
					if(getRequiredStickerVisibility()) {
						//Animating the image view
						ScaleAnimation anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5F, Animation.RELATIVE_TO_SELF, 0.5F);
						anim.setDuration(500);
						anim.setInterpolator(new OvershootInterpolator());
						imageView.startAnimation(anim);
					} else {
						//Setting the image view as invisible
						imageView.setVisibility(View.INVISIBLE);
					}
				}
			});
		}
		
		boolean getRequiredStickerVisibility() {
			return !contextMenuOpen;
		}
		
		void updateStickerVisibility() {
			//Getting the view
			View view = getView();
			if(view == null) return;
			
			//Getting the sticker container
			ViewGroup stickerContainer = view.findViewById(R.id.sticker_container);
			
			//Checking if the stickers should be shown
			if(getRequiredStickerVisibility()) {
				//Showing the stickers
				for(int i = 0; i < stickerContainer.getChildCount(); i++) {
					View stickerView = stickerContainer.getChildAt(i);
					stickerView.setVisibility(View.VISIBLE);
					stickerView.animate().alpha(1).start();
				}
			} else {
				//Hiding the stickers
				for(int i = 0; i < stickerContainer.getChildCount(); i++) {
					View stickerView = stickerContainer.getChildAt(i);
					stickerView.animate().alpha(0).withEndAction(() -> stickerView.setVisibility(View.INVISIBLE)).start();
				}
			}
		}
		
		void addTapback(TapbackInfo tapback) {
			//Updating the tapback if it exists
			for(TapbackInfo allTapbacks : tapbacks) {
				if(tapback.getMessageIndex() == allTapbacks.getMessageIndex() && Objects.equals(tapback.getSender(), allTapbacks.getSender())) {
					allTapbacks.setCode(tapback.code);
					return;
				}
			}
			
			//Adding the tapback
			tapbacks.add(tapback);
		}
		
		void addLiveTapback(TapbackInfo tapback) {
			//Adding the tapback
			addTapback(tapback);
			
			//Rebuilding the tapback view
			View view = getView();
			if(view != null) buildTapbackView(view);
		}
		
		void removeTapback(String sender) {
			//Removing the first matching tapback
			for(Iterator<TapbackInfo> iterator = tapbacks.iterator(); iterator.hasNext();) if(Objects.equals(sender, iterator.next().sender)) {
				iterator.remove();
				break;
			}
		}
		
		void removeLiveTapback(String sender) {
			//Removing the tapback
			removeTapback(sender);
			
			//Rebuilding the tapback view
			View view = getView();
			if(view != null) buildTapbackView(view);
		}
		
		void buildTapbackView(View itemView) {
			//Getting the tapback container
			ViewGroup tapbackContainer = itemView.findViewById(R.id.tapback_container);
			
			//Clearing the tapback container
			tapbackContainer.removeAllViews();
			
			//Returning if there are no tapbacks
			if(tapbacks.isEmpty()) return;
			
			//Counting the associated tapbacks
			/* SparseIntArray tapbackCounts = new SparseIntArray();
			for(TapbackInfo tapback : tapbacks) tapbackCounts.put(tapback.getCode(), tapbackCounts.get(tapback.getCode(), 1)); */
			Map<Integer, Integer> tapbackCounts = new HashMap<>();
			for(TapbackInfo tapback : tapbacks) {
				if(tapbackCounts.containsKey(tapback.getCode())) tapbackCounts.put(tapback.getCode(), tapbackCounts.get(tapback.getCode()) + 1);
				else tapbackCounts.put(tapback.getCode(), 1);
			}
			
			//Sorting the tapback counts by value (descending)
			tapbackCounts = Constants.sortMapByValueDesc(tapbackCounts);
			
			//Iterating over the tapback groups
			for(Map.Entry<Integer, Integer> entry : tapbackCounts.entrySet()) {
				//Inflating the view
				View tapbackView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.chip_tapback, tapbackContainer, false);
				
				//Getting the display info
				TapbackInfo.TapbackDisplay displayInfo = TapbackInfo.getTapbackDisplay(entry.getKey(), itemView.getContext());
				
				//Getting the count text
				TextView count = tapbackView.findViewById(R.id.label_count);
				
				//Setting the count
				count.setText(Integer.toString(entry.getValue()));
				
				//Checking if the display info is valid
				if(displayInfo != null) {
					//Setting the icon drawable and color
					ImageView icon = tapbackView.findViewById(R.id.icon);
					icon.setImageResource(displayInfo.iconResource);
					icon.setImageTintList(ColorStateList.valueOf(displayInfo.color));
					
					//Setting the text color
					count.setTextColor(displayInfo.color);
				}
				
				//Adding the view to the container
				tapbackContainer.addView(tapbackView);
			}
		}
	}
	
	static class MessageTextInfo extends MessageComponent {
		//Creating the values
		String messageText;
		
		MessageTextInfo(long localID, String guid, MessageInfo message, String messageText) {
			//Calling the super constructor
			super(localID, guid, message);
			
			//Setting the text
			this.messageText = messageText;
		}
		
		public String getText() {
			return messageText;
		}
		
		@Override
		View createView(Context context, View convertView, ViewGroup parent) {
			//Inflating the layout if none was provided
			if(convertView == null) convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contenttext, parent, false);
			
			//Setting the text
			final TextView textView = convertView.findViewById(R.id.message);
			textView.setText(messageText);
			
			//Inflating and adding the text content
			setupTextLinks(textView);
			
			//Assigning the interaction listeners
			assignInteractionListeners(textView);
			
			//Setting the gravity
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) convertView.getLayoutParams();
			layoutParams.gravity = getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START;
			textView.setLayoutParams(layoutParams);
			
			//Limiting the width of the bubble
			//Getting the maximum content width
			int maxContentWidth = (int) Math.min(context.getResources().getDimensionPixelSize(R.dimen.contentwidth_max) * .7F, context.getResources().getDisplayMetrics().widthPixels * .7F);
			
			//Enforcing the maximum content width
			View contentView = convertView.findViewById(R.id.content);
			contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			if(contentView.getMeasuredWidth() > maxContentWidth) contentView.getLayoutParams().width = maxContentWidth;
			
			//Building the common views
			buildCommonViews(convertView);
			
			//Returning the view
			return convertView;
		}
		
		private void setupTextLinks(TextView textView) {
			//Setting up the URL checker
			textView.setTransformationMethod(new Constants.CustomTabsLinkTransformationMethod(getMessageInfo().getConversationInfo().getConversationColor()));
			textView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		private void assignInteractionListeners(TextView textView) {
			//Setting the long click listener
			textView.setOnLongClickListener(clickedView -> {
				//Getting the context
				Context context = clickedView.getContext();
				
				//Returning if the view is not an activity
				if(!(context instanceof Activity)) return false;
				
				//Displaying the context menu
				displayContextMenu(context, textView);
				
				//Disabling link clicks
				((TextView) clickedView).setLinksClickable(false);
				
				//Returning
				return true;
			});
			
			//Setting the touch listener
			textView.setOnTouchListener((View view, MotionEvent event) -> {
				if(event.getAction() == MotionEvent.ACTION_UP) {
					new Handler(Looper.getMainLooper()).postDelayed(() -> ((TextView) view).setLinksClickable(true), 0);
				}
				
				return view.onTouchEvent(event);
			});
		}
		
		@Override
		void updateViewEdges(View itemView, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Getting the text background view
			View textView = itemView.findViewById(R.id.message);
			
			//Updating the text view's background
			textView.setBackground(Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored));
		}
		
		@Override
		void updateViewColor(View itemView) {
			//Getting the message text
			TextView messageTextView = itemView.findViewById(R.id.message);
			
			//Getting the colors
			int backgroundColor;
			int textColor;
			
			if(getMessageInfo().isOutgoing()) {
				//backgroundColor = resources.getColor(R.color.colorMessageOutgoing, null);
				backgroundColor = itemView.getResources().getColor(R.color.colorMessageOutgoing, null);
				textColor = Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorPrimary);
			} else {
				MemberInfo memberInfo = getMessageInfo().getConversationInfo().findConversationMember(getMessageInfo().getSender());
				backgroundColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				textColor = itemView.getResources().getColor(android.R.color.white, null);
			}
			
			//Assigning the colors
			messageTextView.setTextColor(textColor);
			messageTextView.setLinkTextColor(textColor);
			messageTextView.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
			
			//Setting up the text links (to update the toolbar color in Chrome's custom tabs)
			setupTextLinks(messageTextView);
		}
		
		private void displayContextMenu(final Context context, View itemView) {
			//Creating a new popup menu
			PopupMenu popupMenu = new PopupMenu(context, itemView);
			
			//Inflating the menu
			popupMenu.inflate(R.menu.menu_conversationitem_contextual);
			
			//Removing the delete file option
			Menu menu = popupMenu.getMenu();
			menu.removeItem(R.id.action_deletedata);
			
			//Setting the click listener
			popupMenu.setOnMenuItemClickListener(menuItem -> {
				switch(menuItem.getItemId()) {
					case R.id.action_details: {
						//Building the message
						StringBuilder stringBuilder = new StringBuilder();
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_type, context.getResources().getString(R.string.part_content_text))).append('\n'); //Message type
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_sender, getMessageInfo().getSender() != null ? getMessageInfo().getSender() : context.getResources().getString(R.string.you))).append('\n'); //Sender
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_datesent, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(getMessageInfo().getDate())))).append('\n'); //Time sent
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_sendeffect, getMessageInfo().getSendEffect().isEmpty() ? context.getResources().getString(R.string.part_none) : getMessageInfo().getSendEffect())); //Send effect
						
						//Showing a dialog
						new AlertDialog.Builder(context)
								.setTitle(R.string.message_messagedetails_title)
								.setMessage(stringBuilder.toString())
								.create()
								.show();
						
						//Returning true
						return true;
					}
					case R.id.action_copytext: {
						//Getting the clipboard manager
						ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
						
						//Creating the clip data
						ClipData clipData = ClipData.newPlainText("message", messageText);
						
						//Applying the clip data
						clipboardManager.setPrimaryClip(clipData);
						
						//Showing a confirmation toast
						Toast.makeText(context, R.string.message_textcopied, Toast.LENGTH_SHORT).show();
						
						//Returning true
						return true;
					}
					case R.id.action_share: {
						//Starting the intent immediately if the user is "you"
						if(getMessageInfo().getSender() == null)
							shareMessageText(context, getMessageInfo().getDate(), null, messageText);
							//Requesting the user info
						else MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, getMessageInfo().getSender(), new UserCacheHelper.UserFetchResult() {
							@Override
							void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
								//Starting the intent
								shareMessageText(context, getMessageInfo().getDate(), userInfo == null ? getMessageInfo().getSender() : userInfo.getContactName(), messageText);
							}
						});
						
						//Returning true
						return true;
					}
				}
				
				//Returning false
				return false;
			});
			
			//Setting the context menu as closed when the menu closes
			popupMenu.setOnDismissListener(closedMenu -> {
				contextMenuOpen = false;
				updateStickerVisibility();
			});
			
			//Showing the menu
			popupMenu.show();
			
			//Setting the context menu as open
			contextMenuOpen = true;
			
			//Hiding the stickers
			updateStickerVisibility();
		}
		
		private static void shareMessageText(Context context, long date, String name, String message) {
			//Creating the intent
			Intent intent = new Intent();
			
			//Setting the action
			intent.setAction(Intent.ACTION_SEND);
			
			//Creating the formatters
			DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM); //android.text.format.DateFormat.getLongDateFormat(activity);
			
			//Getting the text
			String text = name == null ?
					context.getResources().getString(R.string.message_shareable_text_you, dateFormat.format(date), message) :
					context.getResources().getString(R.string.message_shareable_text, dateFormat.format(date), name, message);
			
			//Setting the text
			intent.putExtra(Intent.EXTRA_TEXT, text);
			
			//Setting the intent type
			intent.setType("text/plain");
			
			//Starting the intent
			context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.action_sharemessage)));
		}
	}
	
	static abstract class AttachmentInfo extends MessageComponent {
		//Creating the values
		final String fileName;
		File file = null;
		long fileSize = -1;
		byte[] fileChecksum = null;
		Uri fileUri = null;
		
		//Creating the attachment request values
		private AttachmentWriter attachmentWriterThread = null;
		private CountDownTimer countdownTimer = null;
		private short requestID = -1;
		private int lastIndex = -1;
		private long bytesWritten = 0;
		boolean isFetching = false;
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName) {
			//Calling the super constructor
			super(localID, guid, message);
			
			//Setting the values
			this.fileName = fileName;
		}
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, File file) {
			//Calling the main constructor
			this(localID, guid, message, fileName);
			
			//Setting the file
			if(file.exists()) this.file = file;
		}
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, byte[] fileChecksum) {
			//Calling the main constructor
			this(localID, guid, message, fileName);
			
			//Setting the checksum
			fileChecksum = fileChecksum;
		}
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, Uri fileUri) {
			//Calling the main constructor
			this(localID, guid, message, fileName);
			
			//Setting the uri
			this.fileUri = fileUri;
		}
		
		abstract ContentType getContentType();
		
		abstract void updateContentView();
		
		abstract void onClick(Messaging activity);
		
		void stopTimer(boolean restart) {
			countdownTimer.cancel();
			if(restart) countdownTimer.start();
		}
		
		void setFileSize(long fileSize) {
			this.fileSize = fileSize;
		}
		
		void onFileFragmentReceived(Context context, final byte[] compressedBytes, int index, boolean isLast) {
			//Returning if the attachment is not fetching
			if(!isFetching) return;
			
			//Checking if the index doesn't line up
			if(lastIndex + 1 != index) {
				//Failing the download
				onDownloadFailed();
				
				//Returning
				return;
			}
			
			//Setting the last index
			lastIndex = index;
			
			//Getting the view
			View view = getView();
			if(view != null) {
				//Checking if this is the last message
				if(isLast) {
					//Stopping the timer
					stopTimer(false);
					
					//Filling the progress bar
					((ProgressBar) view.findViewById(R.id.progressBar)).setProgress(100);
				} else {
					//Restarting the timer
					stopTimer(true);
					
					//Updating the progress bar
					ProgressBar progressBar = view.findViewById(R.id.progressBar);
					if(progressBar.isIndeterminate()) progressBar.setIndeterminate(false);
					//progressBar.setProgress((int) (((double) bytesWritten + (double) (ConnectionService.attachmentChunkSize / 2)) / (double) fileSize * 100D));
				}
			}
			
			//Checking if there is no save thread
			if(attachmentWriterThread == null) {
				//Creating and starting the attachment writer thread
				attachmentWriterThread = new AttachmentWriter(context.getApplicationContext());
				attachmentWriterThread.execute();
			}
			
			//Adding the data struct
			synchronized(attachmentWriterThread.dataStructsLock) {
				attachmentWriterThread.dataStructs.add(new AttachmentWriterDataStruct(compressedBytes, isLast));
				attachmentWriterThread.dataStructsLock.notifyAll();
			}
			
			/* //Launching a new asynchronous task
			new AsyncTask<Context, Void, File>() {
				@Override
				protected File doInBackground(Context... context) {
					//Finding the path
					File directory = new File(MainApplication.getDownloadDirectory(context[0]), message);
					if(!directory.exists()) directory.mkdir();
					else if(directory.isFile()) {
						directory.delete();
						directory.mkdir();
					}
					File saveFile = new File(directory, fileName);
					
					//Writing the file to disk
					try(RandomAccessFile randomAccessFile = new RandomAccessFile(saveFile, "wr")) {
						randomAccessFile.write(bytes, offset, bytes.length);
					} catch(IOException exception) {
						//Printing the stack trace
						exception.printStackTrace();
						
						//Returning null
						return null;
					}
					
					//Saving to the database
					if(isEnd)
						DatabaseManager.updateAttachmentFile(context[0], localID, saveFile.getPath());
					
					//Returning the save file
					return saveFile;
				}
				
				@Override
				protected void onPostExecute(File saveFile) {
					//Calling the failed method and returning if the result failed
					if(saveFile == null) {
						onDownloadFailed();
						return;
					}
				}
			}.execute(context); */
		}
		
		void downloadContent(Context context) {
			//Returning if the content has already been fetched is being fetched, or the message is in a ghost state
			if(file != null || isFetching || messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeGhost)
				return;
			
			//Checking if the service isn't running
			ConnectionService connectionService = ConnectionService.getInstance();
			if(connectionService == null) {
				//Showing a toast
				Toast.makeText(context, R.string.message_connectionerrror, Toast.LENGTH_SHORT).show();
				
				//Returning
				return;
			}
			
			//Setting the request ID
			requestID = connectionService.getNextRequestID();
			
			//Requesting the attachment info
			boolean result = connectionService.requestAttachmentInfo(guid, requestID);
			
			//Returning if the request was unsuccessful
			if(!result) return;
			
			//Setting isFetching to true
			isFetching = true;
			
			//Resetting the last index and bytes written
			lastIndex = -1;
			bytesWritten = 0;
			
			//Creating and starting the timer
			if(countdownTimer == null) {
				countdownTimer = new CountDownTimer(10 * 1000, 10 * 1000) { //10-second timer
					public void onTick(long millisUntilFinished) {}
					
					public void onFinish() {
						onDownloadFailed();
					}
				};
			}
			countdownTimer.start();
			
			//Getting the view
			View view = getView();
			if(view != null) {
				//Hiding the content type
				view.findViewById(R.id.download_label).setVisibility(View.GONE);
				
				//Disabling the download button visually
				view.findViewById(R.id.download_button).setAlpha(Constants.disabledAlpha);
				
				//Getting and preparing the progress bar
				ProgressBar progressBar = view.findViewById(R.id.progressBar);
				progressBar.setIndeterminate(true);
				progressBar.setProgress(0);
				progressBar.setVisibility(View.VISIBLE);
			}
		}
		
		void onDownloadFinished(File saveFile) {
			//Returning if the attachment info is no longer relevant
			if(!messageInfo.getAttachments().contains(this)) return;
			
			//Removing the attachment writer thread
			//if(attachmentWriterThread != null) attachmentWriterThread.cancel(false); //This can only be called when the thread has finished itself
			attachmentWriterThread = null;
			
			//Setting isFetching to false
			isFetching = false;
			
			//Setting the file in memory
			file = saveFile;
			
			//Setting the view data
			updateContentView();
			
			//Getting the view
			View view = getView();
			if(view != null) {
				//Hiding the download view
				(view.findViewById(R.id.downloadcontent)).setVisibility(View.GONE);
				
				//Showing the content
				view.findViewById(R.id.content).setVisibility(View.VISIBLE);
				
				//Notifying the list of a view resize
				//getMessageInfo().getConversationInfo().notifyListOfViewResize(view);
			}
		}
		
		void onDownloadFailed() {
			//Returning if the attachment is not fetching
			if(!isFetching) return;
			
			//Returning if the attachment info is no longer relevant
			if(!messageInfo.getAttachments().contains(AttachmentInfo.this)) return;
			
			//Removing the attachment writer thread
			if(attachmentWriterThread != null) attachmentWriterThread.cancel(false);
			attachmentWriterThread = null;
			
			//Setting isFetching to false
			isFetching = false;
			
			//Getting the view
			View view = getView();
			if(view != null) {
				//Enabling the download button visually
				view.findViewById(R.id.download_button).setAlpha(1);
				
				//Showing the content type
				view.findViewById(R.id.download_label).setVisibility(View.VISIBLE);
				
				//Hiding the progress bar
				view.findViewById(R.id.progressBar).setVisibility(View.GONE);
			}
		}
		
		void discardFile() {
			//Returning if there is no file
			if(file == null) return;
			
			//Invalidating the file
			file = null;
			
			//Getting the view
			View view = getView();
			if(view != null) {
				//Showing the standard download content view
				view.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				{
					View failedContent = view.findViewById(R.id.failedcontent);
					if(failedContent != null) failedContent.setVisibility(View.GONE);
				}
				view.findViewById(R.id.processingcontent).setVisibility(View.GONE);
				
				view.findViewById(R.id.download_label).setVisibility(View.VISIBLE);
				view.findViewById(R.id.download_button).setAlpha(1);
				view.findViewById(R.id.progressBar).setVisibility(View.GONE);
				
				//Notifying the list of a view resize
				//getMessageInfo().getConversationInfo().notifyListOfViewResize(view);
			}
		}
		
		void assignInteractionListeners(View view) {
			//Setting the click listener
			view.setOnClickListener(clickView -> {
				//Getting the context
				Context context = clickView.getContext();
				
				//Returning if the view is not the messaging activity
				if(!(context instanceof Messaging)) return;
				
				//Checking if there is no data
				if(file == null && !isFetching) {
					//Downloading the data
					downloadContent(context);
				} else {
					//Calling the on click method
					AttachmentInfo.this.onClick((Messaging) context);
				}
			});
			
			//Setting the long click listener
			view.setOnLongClickListener(clickView -> {
				//Getting the context
				Context context = clickView.getContext();
				
				//Returning if the view is not an activity
				if(!(context instanceof Activity)) return false;
				
				//Displaying the context menu
				displayContextMenu(view, context);
				
				//Returning
				return true;
			});
		}
		
		/* void resetToDownloadView() {
			//Getting the view
			View view = getView();
			if(view == null) return;
			
			//Showing the download view
			view.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
			view.findViewById(R.id.download_button).setAlpha(1);
			view.findViewById(R.id.download_label).setVisibility(View.VISIBLE);
			view.findViewById(R.id.progressBar).setVisibility(View.GONE);
			
			//Hiding the content
			view.findViewById(R.id.content).setVisibility(View.GONE);
		} */
		
		private void displayContextMenu(View view, final Context context) {
			//Returning if there is no view
			if(view == null) return;
			
			//Creating a new popup menu
			PopupMenu popupMenu = new PopupMenu(context, view);
			
			//Inflating the menu
			popupMenu.inflate(R.menu.menu_conversationitem_contextual);
			
			//Removing the copy text option
			Menu menu = popupMenu.getMenu();
			menu.removeItem(R.id.action_copytext);
			
			//Disabling the share and delete option if there is no data
			if(file == null) {
				menu.findItem(R.id.action_share).setEnabled(false);
				menu.findItem(R.id.action_deletedata).setEnabled(false);
			}
			
			//Setting the click listener
			popupMenu.setOnMenuItemClickListener(menuItem -> {
				switch(menuItem.getItemId()) {
					case R.id.action_details: {
						//Building the message
						StringBuilder stringBuilder = new StringBuilder();
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_type, context.getResources().getString(getContentType().getName()))).append('\n'); //Message type
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_sender, messageInfo.getSender() != null ? messageInfo.getSender() : context.getResources().getString(R.string.you))).append('\n'); //Sender
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_datesent, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(getMessageInfo().getDate())))).append('\n'); //Time sent
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_size, file != null ? Formatter.formatShortFileSize(context, file.length()) : context.getResources().getString(R.string.part_nodata))).append('\n'); //Attachment size
						stringBuilder.append(context.getResources().getString(R.string.message_messagedetails_sendeffect, getMessageInfo().getSendEffect().isEmpty() ? context.getResources().getString(R.string.part_none) : getMessageInfo().getSendEffect())); //Send effect
						
						//Showing a dialog
						new AlertDialog.Builder(context)
								.setTitle(R.string.message_messagedetails_title)
								.setMessage(stringBuilder.toString())
								.create()
								.show();
						
						//Returning true
						return true;
					}
					case R.id.action_share: {
						//Getting the file mime type
						String fileName = file.getName();
						int substringStart = file.getName().lastIndexOf(".") + 1;
						
						//Returning if the file cannot be substringed
						if(fileName.length() <= substringStart) return false;
						
						//Creating a new intent
						Intent intent = new Intent();
						
						//Setting the intent action
						intent.setAction(Intent.ACTION_SEND);
						
						//Creating a content URI
						Uri content = FileProvider.getUriForFile(context, MainApplication.fileAuthority, file);
						
						//Setting the intent file
						intent.putExtra(Intent.EXTRA_STREAM, content);
						
						//Getting the mime type
						String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(substringStart));
						
						//Setting the type
						intent.setType(mimeType);
						
						//Starting the activity
						context.startActivity(Intent.createChooser(intent, context.getResources().getText(R.string.action_sharemessage)));
						
						//Returning true
						return true;
					}
					case R.id.action_deletedata: {
						//Deleting the attachment
						new AttachmentDeleter(context, file, localID).execute();
						
						//Discarding the file in memory
						discardFile();
						
						//Returning true
						return true;
					}
				}
				
				//Returning false
				return false;
			});
			
			//Setting the context menu as closed when the menu closes
			popupMenu.setOnDismissListener(closedMenu -> {
				contextMenuOpen = false;
				updateStickerVisibility();
			});
			
			//Showing the menu
			popupMenu.show();
			
			//Setting the context menu as open
			contextMenuOpen = true;
			
			//Hiding the stickers
			updateStickerVisibility();
		}
		
		private static class AttachmentDeleter extends AsyncTask<Void, Void, Void> {
			//Creating the references
			private final WeakReference<Context> contextReference;
			
			//Creating the request values
			private final File file;
			private final long localID;
			
			AttachmentDeleter(Context context, File file, long localID) {
				this.contextReference = new WeakReference<>(context);
				this.file = file;
				this.localID = localID;
			}
			
			@Override
			protected Void doInBackground(Void... voids) {
				//Deleting the file
				file.delete();
				
				//Updating the database entry
				Context context = contextReference.get();
				if(context != null) DatabaseManager.invalidateAttachment(DatabaseManager.getWritableDatabase(context), localID);
				
				//Returning
				return null;
			}
		}
		
		//TODO convert to static class to avoid reference leaking
		private class AttachmentWriter extends AsyncTask<Void, Integer, Boolean> {
			//Creating the references
			final WeakReference<Context> contextReference;
			
			final Object dataStructsLock = new Object();
			ArrayList<AttachmentWriterDataStruct> dataStructs = new ArrayList<>();
			
			//Creating the process values
			File targetFile;
			
			AttachmentWriter(Context context) {
				//Setting the references
				contextReference = new WeakReference<>(context);
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				//Getting the context
				Context context = contextReference.get();
				if(context == null) return false;
				
				//Getting the file path
				File directory = new File(MainApplication.getDownloadDirectory(context), Long.toString(localID));
				if(!directory.exists()) directory.mkdir();
				else if(directory.isFile()) {
					Constants.recursiveDelete(directory);
					directory.mkdir();
				}
				
				//Preparing to write to the file
				targetFile = new File(directory, fileName);
				try(FileOutputStream outputStream = new FileOutputStream(targetFile)) {
					while(true) {
						//Returning if the task has been cancelled
						if(isCancelled()) return null;
						
						//Moving the structs (to be able to release the lock sooner)
						ArrayList<AttachmentWriterDataStruct> localDataStructs = new ArrayList<>();
						synchronized(dataStructsLock) {
							if(!dataStructs.isEmpty()) {
								localDataStructs = dataStructs;
								dataStructs = new ArrayList<>();
							}
						}
						
						//Iterating over the data structs
						for(AttachmentWriterDataStruct dataStruct : localDataStructs) {
							//Decompressing the bytes
							byte[] bytes = SharedValues.decompress(dataStruct.compressedBytes);
							
							//Writing the bytes
							outputStream.write(bytes);
							
							//Adding to the bytes written
							bytesWritten += bytes.length;
							
							//Updating the progress
							publishProgress((int) ((double) bytesWritten / (double) fileSize * 100D));
							
							//Checking if the file is the last one
							if(dataStruct.isLast) {
								//Cleaning the thread
								//cleanThread();
								
								//Saving to the database
								DatabaseManager.updateAttachmentFile(DatabaseManager.getWritableDatabase(context), localID, targetFile);
								
								//Returning true
								return true;
							}
						}
						
						//Waiting for entries to appear
						try {
							synchronized(dataStructsLock) {
								dataStructsLock.wait(10 * 1000); //10-second timeout
							}
						} catch(InterruptedException exception) {
							//Returning
							return null;
							//Cleaning up the thread
							//cleanThread();
							
							//Returning
							//return null;
						}
					}
				} catch(IOException | DataFormatException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Deleting the file
					targetFile.delete();
					
					//Returning false
					return false;
				}
			}
			
			@Override
			protected void onProgressUpdate(Integer... values) {
				//Getting the view
				View view = getView();
				if(view == null) return;
				
				//Updating the progress bar
				ProgressBar progressBar = view.findViewById(R.id.progressBar);
				progressBar.setProgress(values[0]);
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				//Returning if the result is invalid (the task was cancelled)
				if(result == null) return;
				
				//Forwarding the result
				if(result) onDownloadFinished(targetFile);
				else onDownloadFailed();
			}
		}
		
		static class AttachmentWriterDataStruct {
			final byte[] compressedBytes;
			final boolean isLast;
			
			AttachmentWriterDataStruct(byte[] compressedBytes, boolean isLast) {
				this.compressedBytes = compressedBytes;
				this.isLast = isLast;
			}
		}
		
		/* long getRequestID() {
			return requestID;
		} */
		
		boolean compareRequestID(short value) {
			return isFetching && requestID == value;
		}
		
		byte[] getFileChecksum() {
			return fileChecksum;
		}
		
		void setFileChecksum(byte[] fileChecksum) {
			this.fileChecksum = fileChecksum;
		}
	}
	
	static class ImageAttachmentInfo extends AttachmentInfo {
		ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName) {
			super(localID, guid, message, fileName);
		}
		
		ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, File file) {
			super(localID, guid, message, fileName, file);
		}
		
		ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, Uri uri) {
			super(localID, guid, message, fileName, uri);
		}
		
		@Override
		ContentType getContentType() {
			return ContentType.IMAGE;
		}
		
		@Override
		View createView(Context context, View convertView, ViewGroup parent) {
			//Checking if the view needs to be inflated
			if(convertView == null) {
				//Creating the view
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contentimage, parent, false);
			}
			
			//Setting the gravity
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) convertView.getLayoutParams();
			layoutParams.gravity = messageInfo.isOutgoing() ? Gravity.END : Gravity.START;
			convertView.setLayoutParams(layoutParams);
			
			//Setting the view color
			updateViewColor(convertView);
			
			//Updating the content view
			updateContentView(convertView);
			
			//Building the common views
			buildCommonViews(convertView);
			
			//Assigning the click listeners
			assignInteractionListeners(convertView);
			
			//Submitting the view
			return convertView;
		}
		
		@Override
		void updateViewColor(View itemView) {
			//Creating the color values
			ColorStateList textColorStateList;
			ColorStateList backgroundColorStateList;
			ColorStateList accentColorStateList;
			
			//Getting the colors
			if(messageInfo.isOutgoing()) {
				textColorStateList = ColorStateList.valueOf(Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorPrimary));
				backgroundColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoing, null));
				accentColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoingAccent, null));
			} else {
				MemberInfo memberInfo = messageInfo.getConversationInfo().findConversationMember(messageInfo.getSender());
				int bubbleColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				
				textColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(android.R.color.white, null));
				backgroundColorStateList = ColorStateList.valueOf(bubbleColor);
				accentColorStateList = ColorStateList.valueOf(ColorHelper.lightenColor(bubbleColor));
			}
			
			//Coloring the views
			View downloadView = itemView.findViewById(R.id.downloadcontent);
			downloadView.setBackgroundTintList(backgroundColorStateList);
			((TextView) downloadView.findViewById(R.id.download_label)).setTextColor(textColorStateList);
			((ImageView) downloadView.findViewById(R.id.download_button)).setImageTintList(textColorStateList);
			ProgressBar progressBar = downloadView.findViewById(R.id.progressBar);
			progressBar.setProgressTintList(accentColorStateList);
			progressBar.setIndeterminateTintList(accentColorStateList);
			progressBar.setProgressBackgroundTintList(accentColorStateList);
			
			View failedView = itemView.findViewById(R.id.failedcontent);
			failedView.setBackgroundTintList(backgroundColorStateList);
			((TextView) failedView.findViewById(R.id.failedcontent_label)).setTextColor(textColorStateList);
			((ImageView) failedView.findViewById(R.id.failedcontent_button)).setImageTintList(textColorStateList);
		}
		
		@Override
		void updateContentView() {
			//Calling the overload method
			View view = getView();
			if(view != null) updateContentView(view);
		}
		
		private void updateContentView(View itemView) {
			//Checking if there is no file
			if(file == null) {
				//Checking if the attachment is being fetched
				if(isFetching) {
					//Showing the download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					//Hiding the content type
					itemView.findViewById(R.id.download_label).setVisibility(View.GONE);
					
					//Disabling the download button visually
					itemView.findViewById(R.id.download_button).setAlpha(Constants.disabledAlpha);
					
					//Getting and preparing the progress bar
					ProgressBar progressBar = itemView.findViewById(R.id.progressBar);
					progressBar.setIndeterminate(true);
					progressBar.setProgress(0);
					progressBar.setVisibility(View.VISIBLE);
				}
				//Otherwise checking if the attachment is being uploaded
				else if(messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeGhost || messageInfo.isSending) {
					//Showing the processing view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.VISIBLE);
				} else {
					//Showing the standard download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					itemView.findViewById(R.id.download_label).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.download_button).setAlpha(1);
					itemView.findViewById(R.id.progressBar).setVisibility(View.GONE);
				}
			} else {
				//Configuring the content view
				ViewGroup content = itemView.findViewById(R.id.content);
				ViewGroup.LayoutParams params = content.getLayoutParams();
				content.getLayoutParams().width = 0;
				content.getLayoutParams().height = 0;
				content.setLayoutParams(params);
				
				//Switching to the content view
				content.setVisibility(View.VISIBLE);
				itemView.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
				itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
				itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
				itemView.setTag(guid);
				
				//Setting the bitmap
				((ImageView) content.findViewById(R.id.content_view)).setImageBitmap(null);
				int pxBitmapSizeMax = (int) itemView.getResources().getDimension(R.dimen.image_size_max);
				
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromImageFile(file.getPath(), file, new BitmapCacheHelper.ImageDecodeResult(wasTasked -> wasTasked ? getView() : itemView) {
					@Override
					public void onImageMeasured(int width, int height) {
						//Getting the content view
						View itemView = viewSource.get(true);
						if(itemView == null) return;
						View content = itemView.findViewById(R.id.content);
						
						//Getting the multiplier
						float multiplier = Constants.calculateImageAttachmentMultiplier(itemView.getResources(), width, height);
						
						//Configuring the layout
						content.getLayoutParams().width = (int) (width * multiplier);
						content.getLayoutParams().height = (int) (height * multiplier);
						content.setLayoutParams(params);
					}
					
					@Override
					public void onImageDecoded(Bitmap bitmap, boolean wasTasked) {
						//Getting the item view
						View itemView = viewSource.get(wasTasked);
						if(itemView == null) return;
						
						//Checking if the bitmap is invalid
						if(bitmap == null) {
							//Showing the simplified view
							itemView.findViewById(R.id.content).setVisibility(View.GONE);
							itemView.findViewById(R.id.failedcontent).setVisibility(View.VISIBLE);
						} else {
							//Configuring the content view
							ViewGroup content = itemView.findViewById(R.id.content);
							ViewGroup.LayoutParams params = content.getLayoutParams();
							
							float multiplier = Constants.calculateImageAttachmentMultiplier(itemView.getResources(), bitmap.getWidth(), bitmap.getHeight());
							
							content.getLayoutParams().width = (int) (bitmap.getWidth() * multiplier);
							content.getLayoutParams().height = (int) (bitmap.getHeight() * multiplier);
							content.setLayoutParams(params);
							
							//Setting the bitmap
							ImageView imageView = content.findViewById(R.id.content_view);
							imageView.setImageBitmap(bitmap);
							
							//Fading in the view
							if(wasTasked) {
								imageView.setAlpha(0F);
								imageView.animate().alpha(1).setDuration(300).start();
							}
						}
					}
				}, true, pxBitmapSizeMax, pxBitmapSizeMax);
			}
		}
		
		@Override
		void updateViewEdges(View itemView, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Creating the drawable
			Drawable drawable = Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored);
			
			//Assigning the drawable
			itemView.findViewById(R.id.downloadcontent).setBackground(drawable);
			itemView.findViewById(R.id.content).findViewById(R.id.content_background).setBackground(drawable.getConstantState().newDrawable());
			itemView.findViewById(R.id.failedcontent).setBackground(drawable.getConstantState().newDrawable());
			
			//Rounding the image view
			int radiusTop = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
			int radiusBottom = anchoredBottom ? pxCornerAnchored : pxCornerUnanchored;
			
			RoundedImageView imageView = itemView.findViewById(R.id.content_view);
			if(alignToRight) imageView.setRadii(pxCornerUnanchored, radiusTop, radiusBottom, pxCornerUnanchored);
			else imageView.setRadii(radiusTop, pxCornerUnanchored, pxCornerUnanchored, radiusBottom);
			imageView.invalidate();
		}
		
		@Override
		void onClick(Messaging activity) {
			//Returning if there is no content
			if(file == null) return;
			
			//Getting the file extension
			String fileName = file.getName();
			int substringStart = file.getName().lastIndexOf(".") + 1;
			if(fileName.length() <= substringStart) return;
			
			//Getting the file mime type
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(substringStart));
			
			//Creating a content URI
			Uri content = FileProvider.getUriForFile(activity, MainApplication.fileAuthority, file);
			
			//Launching the content viewer
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(content, mimeType);
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			if(intent.resolveActivity(activity.getPackageManager()) != null) activity.startActivity(intent);
			else Toast.makeText(activity, R.string.message_intenterror_open, Toast.LENGTH_SHORT).show();
		}
	}
	
	static class AudioAttachmentInfo extends AttachmentInfo {
		//Creating the media values
		private long duration = -1;
		private boolean fileLoaded = false;
		private boolean mediaPlaying = false;
		private int mediaProgress = 0;
		
		AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName) {
			super(localID, guid, message, fileName);
		}
		
		AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, File file) {
			super(localID, guid, message, fileName, file);
		}
		
		AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, Uri uri) {
			super(localID, guid, message, fileName, uri);
		}
		
		@Override
		ContentType getContentType() {
			return ContentType.AUDIO;
		}
		
		@Override
		View createView(Context context, View convertView, ViewGroup parent) {
			//Checking if the view needs to be inflated
			if(convertView == null) {
				//Creating the view
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contentaudio, parent, false);
			}
			
			//Setting the gravity
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) convertView.getLayoutParams();
			layoutParams.gravity = messageInfo.isOutgoing() ? Gravity.END : Gravity.START;
			convertView.setLayoutParams(layoutParams);
			
			//Setting the view color
			updateViewColor(convertView);
			
			//Updating the content view
			updateContentView(convertView);
			
			//Building the common views
			buildCommonViews(convertView);
			
			//Assigning the click listeners
			assignInteractionListeners(convertView);
			
			//Submitting the view
			return convertView;
		}
		
		@Override
		void updateViewColor(View itemView) {
			//Creating the color values
			ColorStateList textColorStateList;
			ColorStateList backgroundColorStateList;
			ColorStateList accentColorStateList;
			
			//Getting the colors
			if(messageInfo.isOutgoing()) {
				textColorStateList = ColorStateList.valueOf(Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorPrimary));
				backgroundColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoing, null));
				accentColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoingAccent, null));
			} else {
				MemberInfo memberInfo = messageInfo.getConversationInfo().findConversationMember(messageInfo.getSender());
				int bubbleColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				
				textColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(android.R.color.white, null));
				backgroundColorStateList = ColorStateList.valueOf(bubbleColor);
				accentColorStateList = ColorStateList.valueOf(ColorHelper.lightenColor(bubbleColor));
			}
			
			//Coloring the views
			View downloadView = itemView.findViewById(R.id.downloadcontent);
			downloadView.setBackgroundTintList(backgroundColorStateList);
			((TextView) downloadView.findViewById(R.id.download_label)).setTextColor(textColorStateList);
			((ImageView) downloadView.findViewById(R.id.download_button)).setImageTintList(textColorStateList);
			ProgressBar progressBar = downloadView.findViewById(R.id.progressBar);
			progressBar.setProgressTintList(accentColorStateList);
			progressBar.setIndeterminateTintList(accentColorStateList);
			progressBar.setProgressBackgroundTintList(accentColorStateList);
			
			itemView.findViewById(R.id.content).setBackgroundTintList(backgroundColorStateList);
			((ImageView) itemView.findViewById(R.id.button_play_pause_toggle)).setImageTintList(textColorStateList);
			((TextView) itemView.findViewById(R.id.audio_duration)).setTextColor(textColorStateList);
			ProgressBar audioProgressBar = itemView.findViewById(R.id.audio_progress_bar);
			audioProgressBar.setBackgroundTintList(backgroundColorStateList);
			audioProgressBar.setProgressTintList(textColorStateList);
			
			View failedView = itemView.findViewById(R.id.failedcontent);
			failedView.setBackgroundTintList(backgroundColorStateList);
			((TextView) failedView.findViewById(R.id.failedcontent_label)).setTextColor(textColorStateList);
			((ImageView) failedView.findViewById(R.id.failedcontent_button)).setImageTintList(textColorStateList);
		}
		
		@Override
		void updateContentView() {
			//Calling the overload method
			View view = getView();
			if(view != null) updateContentView(view);
		}
		
		private void updateContentView(View itemView) {
			//Loading the file data if it needs to be loaded
			if(!fileLoaded && file != null) new GetDurationTask(this).execute(file);
			
			//Checking if there is no file
			if(file == null) {
				//Checking if the attachment is being fetched
				if(isFetching) {
					//Showing the download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					//Hiding the content type
					itemView.findViewById(R.id.download_label).setVisibility(View.GONE);
					
					//Disabling the download button visually
					itemView.findViewById(R.id.download_button).setAlpha(Constants.disabledAlpha);
					
					//Getting and preparing the progress bar
					ProgressBar progressBar = itemView.findViewById(R.id.progressBar);
					progressBar.setIndeterminate(true);
					progressBar.setProgress(0);
					progressBar.setVisibility(View.VISIBLE);
				}
				//Otherwise checking if the attachment is being uploaded
				else if(messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeGhost || messageInfo.isSending) {
					//Showing the processing view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.VISIBLE);
				} else {
					//Showing the standard download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					itemView.findViewById(R.id.download_label).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.download_button).setAlpha(1);
					itemView.findViewById(R.id.progressBar).setVisibility(View.GONE);
				}
			} else {
				//Hiding the other views
				(itemView.findViewById(R.id.downloadcontent)).setVisibility(View.GONE);
				(itemView.findViewById(R.id.processingcontent)).setVisibility(View.GONE);
				
				//Checking if the duration is invalid
				if(fileLoaded && duration == -1) {
					//Showing the failed content view
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.VISIBLE);
				} else {
					//Showing the content view
					itemView.findViewById(R.id.content).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					
					//Resetting the media if the file isn't loaded
					if(!fileLoaded) resetPlaying(itemView);
					else {
						//Updating the media
						updateMediaPlaying(itemView);
						updateMediaProgress(itemView);
					}
					
					/* //Setting the duration
					if(fileLoaded) ((TextView) itemView.findViewById(R.id.audio_duration)).setText(Constants.getFormattedDuration((int) Math.floor(duration / 1000)));
					else ((TextView) itemView.findViewById(R.id.audio_duration)).setText(Constants.getFormattedDuration(0));
					
					//Checking if the media is playing
					if(mediaPlaying) {
						//Restoring the progress bar
						((ProgressBar) itemView.findViewById(R.id.audio_progress_bar)).setProgress((int) ((float) mediaProgress / (float) duration * 100F));
						
						//Setting the button icon
						((ImageView) itemView.findViewById(R.id.button_play_pause_toggle)).setImageResource(R.drawable.pause);
					} else {
						//Emptying the progress bar
						((ProgressBar) itemView.findViewById(R.id.audio_progress_bar)).setProgress(0);
						
						//Setting the button icon
						((ImageView) itemView.findViewById(R.id.button_play_pause_toggle)).setImageResource(R.drawable.play);
					} */
				}
			}
		}
		
		private static class GetDurationTask extends AsyncTask<File, Void, Long> {
			//Creating the values
			private final WeakReference<AudioAttachmentInfo> attachmentReference;
			
			GetDurationTask(AudioAttachmentInfo attachmentInfo) {
				//Setting the reference
				attachmentReference = new WeakReference<>(attachmentInfo);
			}
			
			@Override
			protected Long doInBackground(File... parameters) {
				//Returning the duration
				return getDurationMillis(parameters[0]);
			}
			
			@Override
			protected void onPostExecute(Long result) {
				//Getting the attachment
				AudioAttachmentInfo attachmentInfo = attachmentReference.get();
				if(attachmentInfo == null) return;
				
				//Setting the duration
				attachmentInfo.fileLoaded = true;
				attachmentInfo.duration = result;
				attachmentInfo.updateContentView();
			}
		}
		
		@Override
		void updateViewEdges(View itemView, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Creating the drawable
			Drawable drawable = Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored);
			
			//Assigning the drawable
			itemView.findViewById(R.id.downloadcontent).setBackground(drawable);
			itemView.findViewById(R.id.content).setBackground(drawable.getConstantState().newDrawable());
			itemView.findViewById(R.id.failedcontent).setBackground(drawable);
		}
		
		@Override
		void onClick(Messaging activity) {
			//Returning if there is no content
			if(file == null) return;
			
			//Getting the audio message manager
			Messaging.RetainedFragment.AudioMessageManager audioMessageManager = activity.getAudioMessageManager();
			
			//Checking if the GUID matches
			if(audioMessageManager.isCurrentMessage(file)) {
				//Toggling play
				audioMessageManager.togglePlaying();
				
				//Returning
				return;
			}
			
			//Preparing the media player
			audioMessageManager.prepareMediaPlayer(localID, file, this);
		}
		
		void setMediaPlaying(boolean playing) {
			//Calling the overload method
			View view = getView();
			if(view != null) setMediaPlaying(view, playing);
		}
		
		private void setMediaPlaying(View itemView, boolean playing) {
			mediaPlaying = playing;
			updateMediaPlaying(itemView);
		}
		
		private void updateMediaPlaying(View itemView) {
			((ImageView) itemView.findViewById(R.id.button_play_pause_toggle)).setImageResource(mediaPlaying ?
					R.drawable.pause :
					R.drawable.play);
		}
		
		void setMediaProgress(int progress) {
			//Calling the overload method
			View view = getView();
			if(view != null) setMediaProgress(view, progress);
		}
		
		private void setMediaProgress(View itemView, int progress) {
			mediaProgress = progress;
			updateMediaProgress(itemView);
		}
		
		private void updateMediaProgress(View itemView) {
			((ProgressBar) itemView.findViewById(R.id.audio_progress_bar)).setProgress((int) ((float) mediaProgress / (float) duration * 100F));
			((TextView) itemView.findViewById(R.id.audio_duration)).setText(Constants.getFormattedDuration((int) Math.floor(mediaProgress <= 0 ? duration / 1000L : mediaProgress / 1000L)));
		}
		
		void resetPlaying() {
			//Calling the overload method
			View view = getView();
			if(view != null) resetPlaying(view);
		}
		
		private void resetPlaying(View itemView) {
			setMediaPlaying(itemView, false);
			setMediaProgress(itemView, 0);
		}
		
		static long getDurationMillis(File file) {
			//Creating a new media metadata retriever
			MediaMetadataRetriever mmr = new MediaMetadataRetriever();
			
			try {
				//Setting the source file
				mmr.setDataSource(file.getPath());
				
				//Getting the duration
				return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
			} catch(RuntimeException exception) {
				//Printing the stack trace
				exception.printStackTrace();
			} finally {
				//Releasing the media metadata retriever
				mmr.release();
			}
			
			//Returning an invalid value
			return -1;
		}
	}
	
	static class VideoAttachmentInfo extends AttachmentInfo {
		VideoAttachmentInfo(long localID, String guid, MessageInfo message, String fileName) {
			super(localID, guid, message, fileName);
		}
		
		VideoAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, File file) {
			super(localID, guid, message, fileName, file);
		}
		
		VideoAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, Uri uri) {
			super(localID, guid, message, fileName, uri);
		}
		
		@Override
		ContentType getContentType() {
			return ContentType.VIDEO;
		}
		
		@Override
		View createView(Context context, View convertView, ViewGroup parent) {
			//Checking if the view needs to be inflated
			if(convertView == null) {
				//Creating the view
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contentvideo, parent, false);
			}
			
			//Setting the gravity
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) convertView.getLayoutParams();
			layoutParams.gravity = messageInfo.isOutgoing() ? Gravity.END : Gravity.START;
			convertView.setLayoutParams(layoutParams);
			
			//Setting the view color
			updateViewColor(convertView);
			
			//Updating the content view
			updateContentView(convertView);
			
			//Building the common views
			buildCommonViews(convertView);
			
			//Assigning the click listeners
			assignInteractionListeners(convertView);
			
			//Submitting the view
			return convertView;
		}
		
		@Override
		void updateViewColor(View itemView) {
			//Creating the color values
			ColorStateList textColorStateList;
			ColorStateList backgroundColorStateList;
			ColorStateList accentColorStateList;
			
			//Getting the colors
			if(messageInfo.isOutgoing()) {
				textColorStateList = ColorStateList.valueOf(Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorPrimary));
				backgroundColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoing, null));
				accentColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoingAccent, null));
			} else {
				MemberInfo memberInfo = messageInfo.getConversationInfo().findConversationMember(messageInfo.getSender());
				int bubbleColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				
				textColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(android.R.color.white, null));
				backgroundColorStateList = ColorStateList.valueOf(bubbleColor);
				accentColorStateList = ColorStateList.valueOf(ColorHelper.lightenColor(bubbleColor));
			}
			
			//Coloring the views
			View downloadView = itemView.findViewById(R.id.downloadcontent);
			downloadView.setBackgroundTintList(backgroundColorStateList);
			((TextView) downloadView.findViewById(R.id.download_label)).setTextColor(textColorStateList);
			((ImageView) downloadView.findViewById(R.id.download_button)).setImageTintList(textColorStateList);
			ProgressBar progressBar = downloadView.findViewById(R.id.progressBar);
			progressBar.setProgressTintList(accentColorStateList);
			progressBar.setIndeterminateTintList(accentColorStateList);
			progressBar.setProgressBackgroundTintList(accentColorStateList);
			
			View failedView = itemView.findViewById(R.id.failedcontent);
			failedView.setBackgroundTintList(backgroundColorStateList);
			((TextView) failedView.findViewById(R.id.failedcontent_label)).setTextColor(textColorStateList);
			((ImageView) failedView.findViewById(R.id.failedcontent_button)).setImageTintList(textColorStateList);
		}
		
		@Override
		void updateContentView() {
			//Calling the overload method
			View view = getView();
			if(view != null) updateContentView(view);
		}
		
		private void updateContentView(View itemView) {
			//Checking if there is no file
			if(file == null) {
				//Checking if the attachment is being fetched
				if(isFetching) {
					//Showing the download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					//Hiding the content type
					itemView.findViewById(R.id.download_label).setVisibility(View.GONE);
					
					//Disabling the download button visually
					itemView.findViewById(R.id.download_button).setAlpha(Constants.disabledAlpha);
					
					//Getting and preparing the progress bar
					ProgressBar progressBar = itemView.findViewById(R.id.progressBar);
					progressBar.setIndeterminate(true);
					progressBar.setProgress(0);
					progressBar.setVisibility(View.VISIBLE);
				}
				//Otherwise checking if the attachment is being uploaded
				else if(messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeGhost || messageInfo.isSending) {
					//Showing the processing view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.VISIBLE);
				} else {
					//Showing the standard download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					itemView.findViewById(R.id.download_label).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.download_button).setAlpha(1);
					itemView.findViewById(R.id.progressBar).setVisibility(View.GONE);
				}
			} else {
				//Configuring the content view
				ViewGroup content = itemView.findViewById(R.id.content);
				ViewGroup.LayoutParams params = content.getLayoutParams();
				content.getLayoutParams().width = 0;
				content.getLayoutParams().height = 0;
				content.setLayoutParams(params);
				
				//Switching to the content view
				content.setVisibility(View.VISIBLE);
				itemView.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
				itemView.findViewById(R.id.failedcontent).setVisibility(View.GONE);
				itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
				
				//Requesting the bitmap
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromVideoFile(file.getPath(), file, new BitmapCacheHelper.ImageDecodeResult(wasTasked -> wasTasked ? getView() : itemView) {
					@Override
					public void onImageMeasured(int width, int height) {
						//Getting the content view
						View itemView = viewSource.get(true);
						if(itemView == null) return;
						View content = itemView.findViewById(R.id.content);
						
						//Getting the multiplier
						float multiplier = Constants.calculateImageAttachmentMultiplier(itemView.getResources(), width, height);
						
						//Configuring the layout
						content.getLayoutParams().width = (int) (width * multiplier);
						content.getLayoutParams().height = (int) (height * multiplier);
						content.setLayoutParams(params);
					}
					
					@Override
					public void onImageDecoded(Bitmap bitmap, boolean wasTasked) {
						//Getting the item view
						View itemView = viewSource.get(true);
						if(itemView == null) return;
						
						//Checking if the bitmap is invalid
						if(bitmap == null) {
							//Showing the simplified view
							itemView.findViewById(R.id.content).setVisibility(View.GONE);
							itemView.findViewById(R.id.failedcontent).setVisibility(View.VISIBLE);
						} else {
							//Configuring the content view
							ViewGroup content = itemView.findViewById(R.id.content);
							ViewGroup.LayoutParams params = content.getLayoutParams();
							
							float multiplier = Constants.calculateImageAttachmentMultiplier(itemView.getResources(), bitmap.getWidth(), bitmap.getHeight());
							
							content.getLayoutParams().width = (int) (bitmap.getWidth() * multiplier);
							content.getLayoutParams().height = (int) (bitmap.getHeight() * multiplier);
							content.setLayoutParams(params);
							
							//Setting the bitmap
							ImageView imageView = content.findViewById(R.id.content_view);
							imageView.setImageBitmap(bitmap);
							
							//Fading in the view
							if(wasTasked) {
								imageView.setAlpha(0F);
								imageView.animate().alpha(1).setDuration(300).start();
							}
						}
					}
				});
			}
		}
		
		@Override
		void updateViewEdges(View itemView, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Creating the drawable
			Drawable drawable = Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored);
			
			//Assigning the drawable
			itemView.findViewById(R.id.downloadcontent).setBackground(drawable);
			itemView.findViewById(R.id.content_background).setBackground(drawable.getConstantState().newDrawable());
			itemView.findViewById(R.id.failedcontent).setBackground(drawable.getConstantState().newDrawable());
			
			//Rounding the image view
			int radiusTop = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
			int radiusBottom = anchoredBottom ? pxCornerAnchored : pxCornerUnanchored;
			if(alignToRight)
				((RoundedImageView) itemView.findViewById(R.id.content_view)).setRadii(pxCornerUnanchored,
						radiusTop,
						radiusBottom,
						pxCornerUnanchored);
			else ((RoundedImageView) itemView.findViewById(R.id.content_view)).setRadii(radiusTop,
					pxCornerUnanchored,
					pxCornerUnanchored,
					radiusBottom);
		}
		
		@Override
		void onClick(Messaging activity) {
			//Returning if there is no content
			if(file == null) return;
			
			//Creating a content URI
			Uri content = FileProvider.getUriForFile(activity, MainApplication.fileAuthority, file);
			
			//Launching the content viewer
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(content, "video/*");
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			if(intent.resolveActivity(activity.getPackageManager()) != null)
				activity.startActivity(intent);
			else Toast.makeText(activity, R.string.message_intenterror_open, Toast.LENGTH_SHORT).show();
		}
	}
	
	static class OtherAttachmentInfo extends AttachmentInfo {
		OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName) {
			super(localID, guid, message, fileName);
		}
		
		OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, File file) {
			super(localID, guid, message, fileName, file);
		}
		
		OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, Uri uri) {
			super(localID, guid, message, fileName, uri);
		}
		
		@Override
		ContentType getContentType() {
			return ContentType.OTHER;
		}
		
		@Override
		View createView(Context context, View convertView, ViewGroup parent) {
			//Checking if the view needs to be inflated
			if(convertView == null) {
				//Creating the view
				convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contentother, parent, false);
			}
			
			//Setting the gravity
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) convertView.getLayoutParams();
			layoutParams.gravity = messageInfo.isOutgoing() ? Gravity.END : Gravity.START;
			convertView.setLayoutParams(layoutParams);
			
			//Setting the view color
			updateViewColor(convertView);
			
			//Updating the content view
			updateContentView(convertView);
			
			//Building the common views
			buildCommonViews(convertView);
			
			//Assigning the click listeners
			assignInteractionListeners(convertView);
			
			//Submitting the view
			return convertView;
		}
		
		@Override
		void updateViewColor(View itemView) {
			//Creating the color values
			ColorStateList textColorStateList;
			ColorStateList backgroundColorStateList;
			ColorStateList accentColorStateList;
			
			//Getting the colors
			if(messageInfo.isOutgoing()) {
				textColorStateList = ColorStateList.valueOf(Constants.resolveColorAttr(itemView.getContext(), android.R.attr.textColorPrimary));
				backgroundColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoing, null));
				accentColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(R.color.colorMessageOutgoingAccent, null));
			} else {
				MemberInfo memberInfo = messageInfo.getConversationInfo().findConversationMember(messageInfo.getSender());
				int bubbleColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				
				textColorStateList = ColorStateList.valueOf(itemView.getResources().getColor(android.R.color.white, null));
				backgroundColorStateList = ColorStateList.valueOf(bubbleColor);
				accentColorStateList = ColorStateList.valueOf(ColorHelper.lightenColor(bubbleColor));
			}
			
			//Coloring the views
			View downloadView = itemView.findViewById(R.id.downloadcontent);
			downloadView.setBackgroundTintList(backgroundColorStateList);
			((TextView) downloadView.findViewById(R.id.download_label)).setTextColor(textColorStateList);
			((ImageView) downloadView.findViewById(R.id.download_button)).setImageTintList(textColorStateList);
			ProgressBar progressBar = downloadView.findViewById(R.id.progressBar);
			progressBar.setProgressTintList(accentColorStateList);
			progressBar.setIndeterminateTintList(accentColorStateList);
			progressBar.setProgressBackgroundTintList(accentColorStateList);
			
			View contentView = itemView.findViewById(R.id.content);
			contentView.setBackgroundTintList(backgroundColorStateList);
			((TextView) contentView.findViewById(R.id.content_label)).setTextColor(textColorStateList);
			((ImageView) contentView.findViewById(R.id.content_button)).setImageTintList(textColorStateList);
		}
		
		@Override
		void updateContentView() {
			//Calling the overload method
			View view = getView();
			if(view != null) updateContentView(view);
		}
		
		private void updateContentView(View itemView) {
			//Checking if there is no file
			if(file == null) {
				//Checking if the attachment is being fetched
				if(isFetching) {
					//Showing the download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					//Hiding the content type
					itemView.findViewById(R.id.download_label).setVisibility(View.GONE);
					
					//Disabling the download button visually
					itemView.findViewById(R.id.download_button).setAlpha(Constants.disabledAlpha);
					
					//Getting and preparing the progress bar
					ProgressBar progressBar = itemView.findViewById(R.id.progressBar);
					progressBar.setIndeterminate(true);
					progressBar.setProgress(0);
					progressBar.setVisibility(View.VISIBLE);
				}
				//Otherwise checking if the attachment is being uploaded
				else if(messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeGhost || messageInfo.isSending) {
					//Showing the processing view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.VISIBLE);
				} else {
					//Showing the standard download content view
					itemView.findViewById(R.id.downloadcontent).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.content).setVisibility(View.GONE);
					itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
					
					itemView.findViewById(R.id.download_label).setVisibility(View.VISIBLE);
					itemView.findViewById(R.id.download_button).setAlpha(1);
					itemView.findViewById(R.id.progressBar).setVisibility(View.GONE);
				}
			} else {
				//Configuring the content view
				((TextView) itemView.findViewById(R.id.content_label)).setText(fileName);
				
				//Switching to the content view
				itemView.findViewById(R.id.content).setVisibility(View.VISIBLE);
				itemView.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
				itemView.findViewById(R.id.processingcontent).setVisibility(View.GONE);
			}
		}
		
		@Override
		void updateViewEdges(View itemView, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Creating the drawable
			Drawable drawable = Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored);
			
			//Assigning the drawable
			itemView.findViewById(R.id.downloadcontent).setBackground(drawable);
			itemView.findViewById(R.id.content).setBackground(drawable.getConstantState().newDrawable());
		}
		
		@Override
		void onClick(Messaging activity) {
			//Returning if there is no content
			if(file == null) return;
			
			//Creating a content URI
			Uri content = FileProvider.getUriForFile(activity, MainApplication.fileAuthority, file);
			
			//Getting the MIME type
			String fileName = file.getName();
			int substringStart = file.getName().lastIndexOf(".") + 1;
			if(fileName.length() <= substringStart) return;
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(substringStart));
			
			//Launching the content viewer
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(content, mimeType);
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			if(intent.resolveActivity(activity.getPackageManager()) != null)
				activity.startActivity(intent);
			else Toast.makeText(activity, R.string.message_intenterror_open, Toast.LENGTH_SHORT).show();
		}
	}
	
	static class StickerInfo {
		//Creating the sticker values
		private long localID;
		private String guid;
		private long messageID;
		private int messageIndex;
		private String sender;
		private long date;
		private byte[] compressedData;
		
		StickerInfo(long localID, String guid, long messageID, int messageIndex, String sender, long date, byte[] compressedData) {
			this.localID = localID;
			this.guid = guid;
			this.messageID = messageID;
			this.messageIndex = messageIndex;
			this.sender = sender;
			this.date = date;
			this.compressedData = compressedData;
		}
		
		long getMessageID() {
			return messageID;
		}
		
		int getMessageIndex() {
			return messageIndex;
		}
	}
	
	static class TapbackInfo {
		//Creating the reference values
		private static final int tapbackLove = 0;
		private static final int tapbackLike = 1;
		private static final int tapbackDislike = 2;
		private static final int tapbackLaugh = 3;
		private static final int tapbackEmphasis = 4;
		private static final int tapbackQuestion = 5;
		
		//Creating the tapback values
		private long localID;
		private long messageID;
		private int messageIndex;
		private String sender;
		private int code;
		
		TapbackInfo(long localID, long messageID, int messageIndex, String sender, int code) {
			this.localID = localID;
			this.messageID = messageID;
			this.messageIndex = messageIndex;
			this.sender = sender;
			this.code = code;
		}
		
		long getMessageID() {
			return messageID;
		}
		
		int getMessageIndex() {
			return messageIndex;
		}
		
		String getSender() {
			return sender;
		}
		
		int getCode() {
			return code;
		}
		
		void setCode(int code) {
			this.code = code;
		}
		
		static int convertToPrivateCode(int publicCode) {
			//Returning if the code is not in the 2000s
			if(publicCode < SharedValues.TapbackModifierInfo.tapbackBaseAdd || publicCode >= SharedValues.TapbackModifierInfo.tapbackBaseRemove) return -1;
			
			//Returning the associated version
			switch(publicCode) {
				case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackLove:
					return tapbackLove;
				case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackLike:
					return tapbackLike;
				case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackDislike:
					return tapbackDislike;
				case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackLaugh:
					return tapbackLaugh;
				case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackEmphasis:
					return tapbackEmphasis;
				case SharedValues.TapbackModifierInfo.tapbackBaseAdd + SharedValues.TapbackModifierInfo.tapbackQuestion:
					return tapbackQuestion;
					default:
						return -1;
			}
		}
		
		static TapbackDisplay getTapbackDisplay(int code, Context context) {
			switch(code) {
				case tapbackLove:
					return new TapbackDisplay(R.drawable.heart, context.getResources().getColor(R.color.tapback_red, null));
				case tapbackLike:
					return new TapbackDisplay(R.drawable.like, context.getResources().getColor(R.color.tapback_yellow, null));
				case tapbackDislike:
					return new TapbackDisplay(R.drawable.dislike, context.getResources().getColor(R.color.tapback_orange, null));
				case tapbackLaugh:
					return new TapbackDisplay(R.drawable.excited, context.getResources().getColor(R.color.tapback_pink, null));
				case tapbackEmphasis:
					return new TapbackDisplay(R.drawable.exclamation, context.getResources().getColor(R.color.tapback_purple, null));
				case tapbackQuestion:
					return new TapbackDisplay(R.drawable.question, context.getResources().getColor(R.color.tapback_blue, null));
				default:
					return null;
			}
		}
		
		static class TapbackDisplay {
			@DrawableRes
			final int iconResource;
			final int color;
			
			private TapbackDisplay(int iconResource, int color) {
				this.iconResource = iconResource;
				this.color = color;
			}
		}
	}
	
	static class GroupActionInfo extends ConversationItem {
		//Creating the constants
		static final int itemType = 1;
		
		//Creating the values
		final int actionType; //0 - Invite / 1 - Leave
		final String agent;
		final String other;
		
		//Creating the other values
		public transient int color;
		
		GroupActionInfo(long localID, String guid, ConversationInfo conversationInfo, int actionType, String agent, String other, long date) {
			//Calling the super constructor
			super(localID, guid, date, conversationInfo);
			
			//Setting the values
			this.actionType = actionType;
			this.agent = agent;
			this.other = other;
		}
		
		@Override
		void bindView(Context context, RecyclerView.ViewHolder viewHolder) {
			//Getting the view holder
			ActionLineViewHolder pViewHolder = ((ActionLineViewHolder) viewHolder);
			
			//Setting the message
			pViewHolder.getLabelMessage().setText(getDirectSummary(context, agent, other, actionType));
			if(agent != null || other != null) getSummary(context, (wasTasked, result) -> {
				if(wasTasked) {
					View itemView = getView();
					if(itemView != null) ((TextView) itemView.findViewById(R.id.message)).setText(result);
				} else pViewHolder.getLabelMessage().setText(result);
			});
		}
		
		@Override
		void getSummary(Context context, Constants.ResultCallback<String> resultCallback) {
			//Calling the static method
			getSummary(context, agent, other, actionType, resultCallback);
		}
		
		static void getSummary(Context context, final String agent, final String other, int actionType, Constants.ResultCallback<String> resultCallback) {
			//Creating the availability values
			Constants.ValueWrapper<String> agentWrapper = new Constants.ValueWrapper<>(null);
			Constants.ValueWrapper<Boolean> agentAvailableWrapper = new Constants.ValueWrapper<>(false);
			Constants.ValueWrapper<String> otherWrapper = new Constants.ValueWrapper<>(null);
			Constants.ValueWrapper<Boolean> otherAvailableWrapper = new Constants.ValueWrapper<>(false);
			
			//Setting the agent to null if the agent is invalid
			if(agent == null) {
				agentWrapper.value = null;
				agentAvailableWrapper.value = true;
			} else {
				//Setting the agent's name if there is user information
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, agent, new UserCacheHelper.UserFetchResult() {
					@Override
					void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
						//Setting the user information
						agentWrapper.value = userInfo == null ? agent : userInfo.getContactName();
						agentAvailableWrapper.value = true;
						
						//Returning the result if both values are available
						if(otherAvailableWrapper.value) resultCallback.onResult(wasTasked, getDirectSummary(context, agentWrapper.value, otherWrapper.value, actionType));
					}
				});
			}
			
			//Setting the other to null if the other is invalid
			if(other == null) {
				otherWrapper.value = null;
				otherAvailableWrapper.value = true;
			} else {
				//Setting the agent's name if there is user information
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, other, new UserCacheHelper.UserFetchResult() {
					@Override
					void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
						//Setting the user information
						otherWrapper.value = userInfo == null ? other : userInfo.getContactName();
						otherAvailableWrapper.value = true;
						
						//Returning the result if both values are available
						if(agentAvailableWrapper.value) resultCallback.onResult(wasTasked, getDirectSummary(context, agentWrapper.value, otherWrapper.value, actionType));
					}
				});
			}
			
			//Returning the result if both values are available
			//if(agentAvailableWrapper.value && otherAvailableWrapper.value) resultCallback.onResult(false, getDirectSummary(context, agentWrapper.value, otherWrapper.value, actionType));
		}
		
		static String getDirectSummary(Context context, String agent, String other, int actionType) {
			//Returning the message based on the action type
			if(actionType == Constants.groupActionInvite) {
				if(Objects.equals(agent, other)) {
					if(agent == null) return context.getString(R.string.message_eventtype_join_you);
					else return context.getString(R.string.message_eventtype_join, agent);
				} else {
					if(agent == null) return context.getString(R.string.message_eventtype_invite_you_agent, other);
					else if(other == null) return context.getString(R.string.message_eventtype_invite_you_object, agent);
					else return context.getString(R.string.message_eventtype_invite, agent, other);
				}
			}
			else if(actionType == Constants.groupActionLeave) {
				if(Objects.equals(agent, other)) {
					if(agent == null) return context.getString(R.string.message_eventtype_leave_you);
					else return context.getString(R.string.message_eventtype_leave, agent);
				} else {
					if(agent == null) return context.getString(R.string.message_eventtype_kick_you_agent, other);
					else if(other == null) return context.getString(R.string.message_eventtype_kick_you_object, agent);
					else return context.getString(R.string.message_eventtype_kick, agent, other);
				}
			}
			
			//Returning an unknown message
			return context.getString(R.string.message_eventtype_unknown);
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
		
		int getActionType() {
			return actionType;
		}
		
		@Override
		void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
			getSummary(context, (wasTasked, result) -> callback.onResult(wasTasked, new LightConversationItem(result, getDate())));
		}
		
		@Override
		LightConversationItem toLightConversationItemSync(Context context) {
			//Getting the titled agent
			String titledAgent = agent;
			if(titledAgent != null) {
				UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, agent);
				if(userInfo == null) titledAgent = agent;
				else titledAgent = userInfo.getContactName();
			}
			
			//Getting the titled other
			String titledOther = other;
			if(titledOther != null) {
				UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, other);
				if(userInfo == null) titledOther = other;
				else titledOther = userInfo.getContactName();
			}
			
			//Returning the light conversation item
			return new LightConversationItem(getDirectSummary(context, titledAgent, titledOther, actionType), getDate());
		}
	}
	
	static class ChatRenameActionInfo extends ConversationItem {
		//Creating the constants
		static final int itemType = 2;
		
		//Creating the values
		final String agent;
		final String title;
		
		ChatRenameActionInfo(long localID, String guid, ConversationInfo conversationInfo, String agent, String title, long date) {
			//Calling the super constructor
			super(localID, guid, date, conversationInfo);
			
			//Setting the values
			this.agent = agent;
			this.title = title;
		}
		
		@Override
		void bindView(Context context, RecyclerView.ViewHolder viewHolder) {
			//Getting the view holder
			ActionLineViewHolder pViewHolder = ((ActionLineViewHolder) viewHolder);
			
			//Setting the message
			pViewHolder.getLabelMessage().setText(getDirectSummary(context, agent, title));
			if(agent != null) getSummary(context, (wasTasked, result) -> {
				if(wasTasked) {
					View itemView = getView();
					if(itemView != null) ((TextView) itemView.findViewById(R.id.message)).setText(result);
				} else pViewHolder.getLabelMessage().setText(result);
			});
		}
		
		@Override
		void getSummary(Context context, Constants.ResultCallback<String> callback) {
			//Returning the summary
			getSummary(context, agent, title, callback);
		}
		
		static void getSummary(Context context, String agent, String title, Constants.ResultCallback<String> callback) {
			//Checking if the agent is invalid
			if(agent == null) {
				//Returning the message
				callback.onResult(false, getDirectSummary(context, null, title));
			} else {
				//Getting the data
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, agent, new UserCacheHelper.UserFetchResult() {
					@Override
					void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
						//Getting the agent
						String namedAgent = userInfo == null ? agent : userInfo.getContactName();
						
						//Returning the message
						callback.onResult(wasTasked, getDirectSummary(context, namedAgent, title));
					}
				});
			}
		}
		
		static String getDirectSummary(Context context, String agent, String title) {
			if(agent == null) {
				if(title == null) return context.getString(R.string.message_eventtype_chatrename_remove_you);
				else return context.getString(R.string.message_eventtype_chatrename_change_you, title);
			} else {
				if(title == null) return context.getString(R.string.message_eventtype_chatrename_remove, agent);
				else return context.getString(R.string.message_eventtype_chatrename_change, agent, title);
			}
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
		
		@Override
		void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
			getSummary(context, (wasTasked, result) -> {
				callback.onResult(wasTasked, new LightConversationItem(result, getDate()));
			});
		}
		
		@Override
		LightConversationItem toLightConversationItemSync(Context context) {
			//Getting the summary
			String summary;
			
			//Checking if the agent is invalid
			if(agent == null) {
				//Returning the message
				summary = getDirectSummary(context, null, title);
			} else {
				//Getting the data
				UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, agent);
				
				//Getting the agent
				String namedAgent = userInfo == null ? agent : userInfo.getContactName();
				
				//Returning the message
				summary = getDirectSummary(context, namedAgent, title);
			}
			
			//Returning the light conversation item
			return new LightConversationItem(summary, getDate());
		}
	}
	
	static class ChatCreationMessage extends ConversationItem {
		//Creating the constants
		static final int itemType = 3;
		
		ChatCreationMessage(long localID, long date, ConversationInfo conversationInfo) {
			super(localID, null, date, conversationInfo);
		}
		
		@Override
		void bindView(Context context, RecyclerView.ViewHolder viewHolder) {
			//Setting the message
			((ActionLineViewHolder) viewHolder).getLabelMessage().setText(getDirectSummary(context));
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
		
		@Override
		void getSummary(Context context, Constants.ResultCallback<String> callback) {
			callback.onResult(false, getDirectSummary(context));
		}
		
		private static String getDirectSummary(Context context) {
			//Returning the string
			return context.getResources().getString(R.string.message_conversationcreated);
		}
		
		@Override
		void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
			callback.onResult(false, new LightConversationItem(getDirectSummary(context), getDate()));
		}
		
		@Override
		LightConversationItem toLightConversationItemSync(Context context) {
			return new LightConversationItem(getDirectSummary(context), getDate());
		}
	}
	
	static class ActionLineViewHolder extends RecyclerView.ViewHolder {
		//Creating the common item values
		private final TextView labelMessage;
		
		//Creating the view value
		private View view;
		
		ActionLineViewHolder(View view) {
			//Calling the super method and setting the view
			super(view);
			this.view = view;
			
			//Assigning the views
			labelMessage = view.findViewById(R.id.message);
		}
		
		View getView() {
			return view;
		}
		
		TextView getLabelMessage() {
			return labelMessage;
		}
	}
	
	static abstract class ConversationItem {
		//Creating the conversation item values
		private long localID;
		private String guid;
		private long date;
		private ConversationInfo conversationInfo;
		private Constants.ViewSource viewSource;
		
		ConversationItem(long localID, String guid, long date, ConversationInfo conversationInfo) {
			//Setting the identifiers
			this.localID = localID;
			this.guid = guid;
			
			//Setting the date
			this.date = date;
			
			//Setting the conversation info
			this.conversationInfo = conversationInfo;
		}
		
		long getLocalID() {
			return localID;
		}
		
		void setLocalID(long value) {
			localID = value;
		}
		
		String getGuid() {
			return guid;
		}
		
		void setGuid(String value) {
			guid = value;
		}
		
		long getDate() {
			return date;
		}
		
		void setDate(long value) {
			date = value;
		}
		
		void setViewSource(Constants.ViewSource viewSource) {
			this.viewSource = viewSource;
		}
		
		View getView() {
			return viewSource == null ? null : viewSource.get();
		}
		
		ConversationInfo getConversationInfo() {
			return conversationInfo;
		}
		
		void setConversationInfo(ConversationInfo conversationInfo) {
			this.conversationInfo = conversationInfo;
		}
		
		abstract void bindView(Context context, RecyclerView.ViewHolder viewHolder);
		
		static class MessageViewHolder extends RecyclerView.ViewHolder {
			//Creating the common item values
			private final TextView labelTimeDivider;
			private final TextView labelSender;
			private final ViewGroup groupMPC;
			private final TextSwitcher labelActivityStatus;
			private final ViewGroup groupEffectReplay;
			private final ProgressWheel progressSend;
			private final ImageButton buttonSendError;
			
			private final ViewStub stubProfile;
			private ViewGroup groupProfile = null;
			private ImageView imageProfileDefault = null;
			private ImageView imageProfileImage = null;
			
			//Creating the view value
			private LinearLayout view;
			
			MessageViewHolder(LinearLayout view) {
				//Calling the super method and setting the view
				super(view);
				this.view = view;
				
				//Assigning the views
				labelTimeDivider = view.findViewById(R.id.timedivider);
				labelSender = view.findViewById(R.id.sender);
				groupMPC = view.findViewById(R.id.messagepart_container);
				labelActivityStatus = view.findViewById(R.id.activitystatus);
				groupEffectReplay = view.findViewById(R.id.sendeffect_replay);
				progressSend = view.findViewById(R.id.send_progress);
				buttonSendError = view.findViewById(R.id.send_error);
				
				stubProfile = view.findViewById(R.id.stub_profile);
			}
			
			LinearLayout getView() {
				return view;
			}
			
			TextView getLabelTimeDivider() {
				return labelTimeDivider;
			}
			
			TextView getLabelSender() {
				return labelSender;
			}
			
			ViewGroup getGroupMPC() {
				return groupMPC;
			}
			
			TextSwitcher getLabelActivityStatus() {
				return labelActivityStatus;
			}
			
			ViewGroup getGroupEffectReplay() {
				return groupEffectReplay;
			}
			
			ProgressWheel getProgressSend() {
				return progressSend;
			}
			
			ImageButton getButtonSendError() {
				return buttonSendError;
			}
			
			void inflateProfile() {
				//Returning if the profile already exists
				if(groupProfile != null) return;
				
				//Inflating the stub
				groupProfile = (ViewGroup) stubProfile.inflate();
				
				//Setting the views
				imageProfileDefault = groupProfile.findViewById(R.id.profile_default);
				imageProfileImage = groupProfile.findViewById(R.id.profile_image);
			}
			
			ViewGroup getProfile() {
				return groupProfile;
			}
			
			ImageView getImageProfileDefault() {
				return imageProfileDefault;
			}
			
			ImageView getImageProfileImage() {
				return imageProfileImage;
			}
		}
		
		void updateViewColor() {}
		
		abstract void getSummary(Context context, Constants.ResultCallback<String> resultCallback);
		
		abstract int getItemType();
		
		abstract void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback);
		
		abstract LightConversationItem toLightConversationItemSync(Context context);
	}
	
	static class LightConversationItem {
		//Creating the message values
		private String message;
		private final long date;
		
		LightConversationItem(String message, long date) {
			//Setting the values
			this.message = message;
			this.date = date;
		}
		
		public String getMessage() {
			return message;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
		
		long getDate() {
			return date;
		}
	}
	
	static void setupConversationItemRelations(ArrayList<ConversationItem> conversationItems, ConversationInfo conversationInfo) {
		//Iterating over the items
		for(int i = 0; i < conversationItems.size(); i++) {
			//Getting the item
			ConversationManager.ConversationItem item = conversationItems.get(i);
			if(!(item instanceof ConversationManager.MessageInfo)) continue;
			ConversationManager.MessageInfo messageItem = (ConversationManager.MessageInfo) item;
			
			//Checking if there is a less recent item
			if(i > 0) {
				//Getting the item
				ConversationManager.ConversationItem adjacentItem = conversationItems.get(i - 1);
				
				//Checking if the item is a valid anchor point (is a message and is within the burst time)
				if(adjacentItem instanceof ConversationManager.MessageInfo && Math.abs(item.getDate() - adjacentItem.getDate()) < ConversationManager.conversationBurstTimeMillis) {
					//Updating the anchorage
					messageItem.setAnchoredTop(messageItem.getSender() == null ? ((ConversationManager.MessageInfo) adjacentItem).getSender() == null : messageItem.getSender().equals(((ConversationManager.MessageInfo) adjacentItem).getSender()));
				}
				
				//Finding the last message
				int currentIndex = i - 1;
				while(!(adjacentItem instanceof ConversationManager.MessageInfo)) {
					currentIndex--;
					if(currentIndex < 0) break;
					adjacentItem = conversationItems.get(currentIndex);
				}
						/* if(!(adjacentItem instanceof ConversationManager.MessageInfo)) {
							do {
								currentIndex--;
								adjacentItem = conversationItems.get(currentIndex);
							} while(!(adjacentItem instanceof ConversationManager.MessageInfo) && currentIndex > 0);
						} */
				
				if(currentIndex >= 0) messageItem.setHasTimeDivider(Math.abs(item.getDate() - adjacentItem.getDate()) >= ConversationManager.conversationSessionTimeMillis);
				else messageItem.setHasTimeDivider(true); //The item is the first message (not conversation item)
			} else messageItem.setHasTimeDivider(true); //The item is at the beginning of the conversation
			
			//Checking if there is a more recent item
			if(i < conversationItems.size() - 1) {
				//Getting the item
				ConversationManager.ConversationItem adjacentItem = conversationItems.get(i + 1);
				
				//Checking if the item is a valid anchor point (is a message and is within the burst time)
				if(adjacentItem instanceof ConversationManager.MessageInfo && Math.abs(item.getDate() - adjacentItem.getDate()) < ConversationManager.conversationBurstTimeMillis) {
					//Updating the anchorage
					messageItem.setAnchoredBottom(messageItem.getSender() == null ? ((ConversationManager.MessageInfo) adjacentItem).getSender() == null : messageItem.getSender().equals(((ConversationManager.MessageInfo) adjacentItem).getSender()));
				}
			}
		}
		
		//Finding the message to show the state on
		for(int i = conversationItems.size() - 1; i >= 0; i--) {
			//Getting the item
			ConversationManager.ConversationItem item = conversationItems.get(i);
			
			//Skipping the remainder of the iteration if the item is not a message
			if(!(item instanceof ConversationManager.MessageInfo)) continue;
			
			//Getting the message
			ConversationManager.MessageInfo messageItem = (ConversationManager.MessageInfo) item;
			
			//Skipping the remainder of the iteration if the message is incoming
			if(messageItem.getSender() != null) continue;
			
			//Setting the conversation's active message state list ID
			conversationInfo.setActivityStateTarget(messageItem);
			
			//Breaking from the loop
			break;
		}
	}
	
	static void addConversationItemRelation(ConversationInfo conversation, ArrayList<ConversationItem> conversationItems, MessageInfo messageInfo, Context context, boolean update) {
		//Getting the index
		int index = conversationItems.indexOf(messageInfo);
		
		//Checking if there is a less recent item
		if(index > 0) {
			//Getting the item
			ConversationManager.ConversationItem adjacentItem = conversationItems.get(index - 1);
			
			//Checking if the item is a valid anchor point (is a message and is within the burst time)
			if(adjacentItem instanceof ConversationManager.MessageInfo && Math.abs(messageInfo.getDate() - adjacentItem.getDate()) < ConversationManager.conversationBurstTimeMillis) {
				//Updating the anchorage
				boolean isAnchored = messageInfo.getSender() == null ? ((ConversationManager.MessageInfo) adjacentItem).getSender() == null : messageInfo.getSender().equals(((ConversationManager.MessageInfo) adjacentItem).getSender());
				messageInfo.setAnchoredTop(isAnchored);
				((MessageInfo) adjacentItem).setAnchoredBottom(isAnchored);
				
				//Updating the views
				if(update) {
					messageInfo.updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
					((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
				}
			}
			
			//Finding the last message
			int currentIndex = index - 1;
			while(!(adjacentItem instanceof ConversationManager.MessageInfo)) {
				currentIndex--;
				if(currentIndex < 0) break;
				adjacentItem = conversationItems.get(currentIndex);
			}
			
			if(currentIndex >= 0) messageInfo.setHasTimeDivider(Math.abs(messageInfo.getDate() - adjacentItem.getDate()) >= ConversationManager.conversationSessionTimeMillis);
			else messageInfo.setHasTimeDivider(true); //The item is the first message (not conversation item)
		} else messageInfo.setHasTimeDivider(true); //The item is at the beginning of the conversation
		
		//Updating the view
		if(update) messageInfo.updateTimeDivider(context);
		
		//Checking if there is a more recent item
		if(index < conversationItems.size() - 1) {
			//Getting the item
			ConversationManager.ConversationItem adjacentItem = conversationItems.get(index + 1);
			
			//Checking if the item is a valid anchor point (is a message and is within the burst time)
			if(adjacentItem instanceof ConversationManager.MessageInfo && Math.abs(messageInfo.getDate() - adjacentItem.getDate()) < ConversationManager.conversationBurstTimeMillis) {
				//Updating the anchorage
				boolean isAnchored = messageInfo.getSender() == null ? ((ConversationManager.MessageInfo) adjacentItem).getSender() == null : messageInfo.getSender().equals(((ConversationManager.MessageInfo) adjacentItem).getSender());
				messageInfo.setAnchoredBottom(isAnchored);
				((MessageInfo) adjacentItem).setAnchoredTop(isAnchored);
				
				//Updating the views
				if(update) {
					messageInfo.updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
					((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
				}
			}
		}
		
		//Comparing (and replacing) the activity state target
		conversation.tryActivityStateTarget(messageInfo, update, context);
		
		//Comparing the conversation item to the active message state listing item
		/* {
			//Getting the current item
			MessageInfo activeMessage = conversation.getActivityStateTarget();
			
			//Replacing the item if it is invalid
			if(activeMessage == null) {
				conversation.setActivityStateTarget(messageInfo);
				
				//Updating the view
				if(update) messageInfo.updateActivityStateDisplay(context);
			} else {
				//Replacing the item if the new one is outgoing and more recent
				if(messageInfo.isOutgoing() &&
						messageInfo.getDate() >= activeMessage.getDate() &&
						(messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeDelivered || messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeRead)) {
					conversation.setActivityStateTarget(messageInfo);
					
					//Updating the views
					if(update) {
						messageInfo.updateActivityStateDisplay(context);
						activeMessage.updateActivityStateDisplay(context);
					}
				}
			}
		} */
	}
	
	enum ContentType {
		//TEXT (0),
		IMAGE(1, "image", R.string.part_content_image),
		VIDEO(2, "video", R.string.part_content_video),
		AUDIO(3, "audio", R.string.part_content_audio),
		OTHER(4, "other", R.string.part_content_other);
		
		//Creating the values
		private final int identifier;
		private final String mimeTypeLabel;
		@StringRes
		private final int name;
		
		ContentType(int identifier) {
			this.identifier = identifier;
			this.mimeTypeLabel = null;
			this.name = -1;
		}
		
		ContentType(int identifier, String mimeTypeLabel, @StringRes int name) {
			this.identifier = identifier;
			this.mimeTypeLabel = mimeTypeLabel;
			this.name = name;
		}
		
		public int getIdentifier() {
			return identifier;
		}
		
		public String getMimeTypeLabel() {
			return mimeTypeLabel;
		}
		
		@StringRes
		public int getName() {
			return name;
		}
		
		public static ContentType fromIdentifier(int identifier) {
			//Returning the content type
			for(ContentType contentType : values())
				if(contentType.identifier == identifier) return contentType;
			
			//Returning null
			return null;
		}
		
		public static ContentType getType(String mimeType) {
			//Returning the time
			if(mimeType == null || mimeType.isEmpty()) return OTHER;
			if(mimeType.startsWith(IMAGE.getMimeTypeLabel())) return IMAGE;
			if(mimeType.startsWith(VIDEO.getMimeTypeLabel())) return VIDEO;
			if(mimeType.startsWith(AUDIO.getMimeTypeLabel())) return AUDIO;
			return OTHER;
		}
	}
}