package me.tagavari.airmessage;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.lukhnos.nnio.file.Paths;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
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

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import java9.util.function.Consumer;
import jp.wasabeef.glide.transformations.BlurTransformation;
import me.tagavari.airmessage.common.SharedValues;
import me.tagavari.airmessage.view.InvisibleInkView;
import me.tagavari.airmessage.view.RoundedImageView;

class ConversationManager {
	//Message burst - Sending single messages one after the other
	static final long conversationBurstTimeMillis = 30 * 1000; //30 seconds
	//Message session - A conversation session, where conversation participants are active
	static final long conversationSessionTimeMillis = 5 * 60 * 1000; //5 minutes
	//Just now - A message sent just now
	static final long conversationJustNowTimeMillis = 60 * 1000; //1 minute
	
	static final Comparator<ConversationInfo> conversationComparator = (conversation1, conversation2) -> {
		//Getting the last conversation item times
		long lastTime1 = conversation1.getLastItem() == null ? Long.MIN_VALUE : conversation1.getLastItem().getDate();
		long lastTime2 = conversation2.getLastItem() == null ? Long.MIN_VALUE : conversation2.getLastItem().getDate();
		
		//Returning the comparison
		return Long.compare(lastTime2, lastTime1);
	};
	static final Comparator<ConversationItem> conversationItemComparator = ConversationManager::compareConversationItems;
	static final Comparator<MemberInfo> memberInfoComparator = (member1, member2) -> {
		//Returning 0 if either of the values are invalid
		if(member1 == null || member2 == null) return 0;
		
		//Returning the comparison (lexicographic comparison)
		return member1.name.compareTo(member2.name);
	};
	
	private static final int invisibleInkBlurRadius = 2;
	private static final int invisibleInkBlurSampling = 80;
	
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
		
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return list;
		
		//Iterating over the conversations
		for(ConversationInfo item : conversations) {
			//Skipping the remainder of the iteration if the conversation's items aren't loaded
			if(!item.isDataAvailable()) continue;
			
			//Adding the conversation to the list
			list.add(item);
		}
		
		//Returning the list
		return list;
	}
	
	static int compareConversationItems(ConversationItem item1, ConversationItem item2) {
		//Returning 0 if either of the arguments are invalid
		if(item1 == null || item2 == null) return 0;
		
		//Returning the comparison
		if(item1.getServerID() != -1 && item2.getServerID() != -1) return Long.compare(item1.getServerID(), item2.getServerID());
		if(item1.getLocalID() != -1 && item2.getLocalID() != -1) return Long.compare(item1.getLocalID(), item2.getLocalID());
		if(item1.getLocalID() == -1 && item2.getLocalID() == -1) return Long.compare(item1.getDate(), item2.getDate());
		if(item1.getLocalID() == -1) return 1;
		return -1; //Item 2's local ID is -1
	}
	
	static int compareConversationItems(LightConversationItem item1, ConversationItem item2) {
		//Returning 0 if either of the values are invalid
		if(item1 == null || item2 == null) return 0;
		
		//Returning the comparison
		if(item1.getServerID() != -1 && item2.getServerID() != -1) return Long.compare(item1.getServerID(), item2.getServerID());
		if(item1.getLocalID() != -1 && item2.getLocalID() != -1) return Long.compare(item1.getLocalID(), item2.getLocalID());
		if(item1.getLocalID() == -1 && item2.getLocalID() == -1) return Long.compare(item1.getDate(), item2.getDate());
		if(item1.getLocalID() == -1) return 1;
		return -1; //Item 2's local ID is -1
	}
	
	static int compareConversationItems(LightConversationItem item1, LightConversationItem item2) {
		//Returning 0 if either of the values are invalid
		if(item1 == null || item2 == null) return 0;
		
		//Returning the comparison
		if(item1.getServerID() != -1 && item2.getServerID() != -1) return Long.compare(item1.getServerID(), item2.getServerID());
		if(item1.getLocalID() != -1 && item2.getLocalID() != -1) return Long.compare(item1.getLocalID(), item2.getLocalID());
		if(item1.getLocalID() == -1 && item2.getLocalID() == -1) return Long.compare(item1.getDate(), item2.getDate());
		if(item1.getLocalID() == -1) return 1;
		return -1; //Item 2's local ID is -1
	}
	
	static class ConversationInfo implements Serializable {
		private static final long serialVersionUID = 0;
		
		//Creating the reference values
		/* private static final String timeFormat = "h:mm a";
		private static final String dayFormat = "MMM d";
		private static final String weekdayFormat = "E";
		private static final String yearFormat = "y"; */
		static final Integer[] standardUserColors = {
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
		static final int backupUserColor = 0xFF607D8B; //Blue grey
		private static final int maxUsersToDisplay = 4;
		
		//Creating the static values
		private static SelectionSource selectionSource = id -> false;
		
		//Creating the values
		private final long localID;
		private String guid;
		private ConversationState conversationState;
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
		
		static void setSelectionSource(SelectionSource source) {
			selectionSource = source;
		}
		
		ConversationInfo(long localID, ConversationState conversationState) {
			//Setting the local ID and state
			this.localID = localID;
			this.conversationState = conversationState;
			
			//Instantiating the lists
			conversationMembers = new ArrayList<>();
			draftFiles = new ArrayList<>();
		}
		
		ConversationInfo(long localID, String guid, ConversationState conversationState) {
			//Setting the identifiers and the state
			this.localID = localID;
			this.guid = guid;
			this.conversationState = conversationState;
			
			//Instantiating the lists
			conversationMembers = new ArrayList<>();
			draftFiles = new ArrayList<>();
		}
		
		ConversationInfo(long localID, String guid, ConversationState conversationState, String service, ArrayList<MemberInfo> conversationMembers, String title, int unreadMessageCount, int conversationColor, String draftMessage, ArrayList<DraftFile> draftFiles, long draftUpdateTime) {
			//Setting the values
			this.guid = guid;
			this.localID = localID;
			this.conversationState = conversationState;
			this.service = service;
			this.conversationMembers = conversationMembers;
			this.title = title;
			this.unreadMessageCount = unreadMessageCount;
			this.conversationColor = conversationColor;
			this.draftMessage = draftMessage;
			this.draftFiles = draftFiles;
			this.draftUpdateTime = draftUpdateTime;
		}
		
		void setConversationLists(ArrayList<ConversationItem> items, ArrayList<MessageInfo> ghostItems) {
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
		
		void bindView(ItemViewHolder viewHolder, Context context) {
			//Setting the flags
			viewHolder.flagMuted.setVisibility(isMuted ? View.VISIBLE : View.GONE);
			
			//Setting the profile
			currentUserViewIndex = updateViewUser(context, viewHolder);
			updateSelected(viewHolder);
			
			//Returning if the last message is invalid
			//if(lastItem == null) return convertView;
			
			//Updating the view
			updateView(viewHolder, context);
		}
		
		void bindViewOnce(ItemViewHolder viewHolder, Context context) {
			//Setting the flags
			viewHolder.flagMuted.setVisibility(isMuted ? View.VISIBLE : View.GONE);
			
			//Building the profile view
			updateViewUser(context, viewHolder);
			
			//Updating the view
			updateView(viewHolder, context);
		}
		
		void bindSimpleView(Context context, SimpleItemViewHolder viewHolder, Constants.ViewHolderSource<SimpleItemViewHolder> viewHolderSource) {
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
		
		void setViewHolderSource(Constants.ViewHolderSource<ItemViewHolder> viewHolderSource) {
			this.viewHolderSource = viewHolderSource;
		}
		
		private ItemViewHolder getViewHolder() {
			if(viewHolderSource == null) return null;
			return viewHolderSource.get();
		}
		
		void updateView(Context context) {
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
			
			/*if(draftMessage != null) {
				itemView.conversationMessage.setText(context.getResources().getString(R.string.prefix_draft, draftMessage));
				itemView.conversationTime.setText("");
			} else if(!draftFiles.isEmpty()) {
				//Converting the draft list to a string resource list
				ArrayList<Integer> draftStringRes = new ArrayList<>();
				for(DraftFile draft : draftFiles) draftStringRes.add(getNameFromContentType(draft.getFileType()));
				
				String summary;
				if(draftStringRes.size() == 1) summary = context.getResources().getString(draftStringRes.get(0));
				else summary = context.getResources().getQuantityString(R.plurals.message_multipleattachments, draftStringRes.size(), draftStringRes.size());
				itemView.conversationMessage.setText(context.getResources().getString(R.string.prefix_draft, summary));
				itemView.conversationTime.setText("");
			} else */if(lastItem != null) {
				itemView.conversationMessage.setText(lastItem.getMessage());
				updateTime(context, itemView);
			} else {
				itemView.conversationMessage.setText(R.string.part_unknown);
				itemView.conversationTime.setText(R.string.part_unknown);
			}
		}
		
		void updateUnreadStatus(Context context) {
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
		
		void updateTime(Context context) {
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
			
			//Setting the time
			/* itemView.conversationTime.setText(
					System.currentTimeMillis() - lastItem.getDate() < conversationJustNowTimeMillis ?
							context.getResources().getString(R.string.time_now) :
							DateUtils.getRelativeTimeSpanString(lastItem.getDate(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString()); */
			itemView.conversationTime.setText(getLastUpdateStatusTime(context, lastItem.getDate()));
		}
		
		private static String getLastUpdateStatusTime(Context context, long date) {
			long timeNow = System.currentTimeMillis();
			long timeDiff = timeNow - date;
			
			//Just now
			if(timeDiff < conversationJustNowTimeMillis) return context.getResources().getString(R.string.time_now);
			
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
		
		void updateViewUser(Context context) {
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
					void onImageMeasured(int width, int height) {}
					
					@Override
					void onImageDecoded(Bitmap result, boolean wasTasked) {
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
		
		void clearMessages() {
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
		
		void addDraftFileUpdate(Context context, DraftFile draft, long updateTime) {
			draftFiles.add(draft);
			if(updateTime != -1) registerDraftChange(context, updateTime);
		}
		
		void removeDraftFileUpdate(Context context, DraftFile draft, long updateTime) {
			draftFiles.remove(draft);
			if(updateTime != -1) registerDraftChange(context, updateTime);
		}
		
		void clearDraftsUpdate(Context context) {
			draftMessage = null;
			draftFiles.clear();
			registerDraftChange(context, -1);
		}
		
		List<DraftFile> getDrafts() {
			return draftFiles;
		}
		
		void setDraftMessageUpdate(Context context, String message, long updateTime) {
			draftMessage = message;
			if(updateTime != -1) registerDraftChange(context, updateTime);
		}
		
		String getDraftMessage() {
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
			ConversationManager.sortConversation(this);
			
			//Updating the list
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
		}
		
		ArrayList<ConversationItem> getConversationItems() {
			return conversationItemsReference == null ? null : conversationItemsReference.get();
		}
		
		ArrayList<MessageInfo> getGhostMessages() {
			return ghostMessagesReference == null ? null : ghostMessagesReference.get();
		}
		
		boolean isDataAvailable() {
			return getConversationItems() != null && getGhostMessages() != null;
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
			
			//Telling the activity
			if(activityCallbacks != null) activityCallbacks.chatUpdateUnreadCount();
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
			ItemViewHolder itemView = getViewHolder();
			if(itemView == null) return;
			
			//Updating the view
			if(isMuted) itemView.flagMuted.setVisibility(View.VISIBLE);
			else itemView.flagMuted.setVisibility(View.GONE);
		}
		
		MessageInfo getActivityStateTargetRead() {
			if(activityStateTargetReadReference == null) return null;
			return activityStateTargetReadReference.get();
		}
		
		void setActivityStateTargetRead(MessageInfo activityStateTarget) {
			activityStateTargetReadReference = new WeakReference<>(activityStateTarget);
		}
		
		MessageInfo getActivityStateTargetDelivered() {
			if(activityStateTargetDeliveredReference == null) return null;
			return activityStateTargetDeliveredReference.get();
		}
		
		void setActivityStateTargetDelivered(MessageInfo activityStateTarget) {
			activityStateTargetDeliveredReference = new WeakReference<>(activityStateTarget);
		}
		
		void tryActivityStateTarget(MessageInfo activityStateTarget, boolean update, Context context) {
			//Returning if the item is incoming
			if(!activityStateTarget.isOutgoing()) return;
			
			//Checking if the item is delivered
			if(activityStateTarget.getMessageState() == SharedValues.MessageInfo.stateCodeDelivered) {
				//Getting the current item
				MessageInfo activeMessageDelivered = getActivityStateTargetDelivered();
				
				//Replacing the item if it is invalid
				if(activeMessageDelivered == null) {
					setActivityStateTargetDelivered(activityStateTarget);
					
					//Updating the view
					if(update) activityStateTarget.updateActivityStateDisplay(context);
				} else {
					//Replacing the item if the new one is more recent
					if(ConversationManager.compareConversationItems(activityStateTarget, activeMessageDelivered) >= 0) {
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
			else if(activityStateTarget.getMessageState() == SharedValues.MessageInfo.stateCodeRead) {
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
					if(ConversationManager.compareConversationItems(activityStateTarget, activeMessageRead) >= 0 &&
							(activityStateTarget.getMessageState() == SharedValues.MessageInfo.stateCodeDelivered || activityStateTarget.getMessageState() == SharedValues.MessageInfo.stateCodeRead)) {
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
		
		void replaceConversationItems(Context context, List<ConversationItem> sortedList) {
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
				if(messageInfo.getMessageState() != SharedValues.MessageInfo.stateCodeGhost) continue;
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
		
		LightConversationItem getLastItem() {
			//Returning the last conversation item
			return lastItem;
		}
		
		boolean trySetLastItem(LightConversationItem item, boolean force) {
			if(force || lastItem == null || (ConversationManager.compareConversationItems(lastItem, item) < 0 && !lastItem.isPinned())) {
				lastItem = item;
				return true;
			} else return false;
		}
		
		void trySetLastItemUpdate(Context context, ConversationItem lastConversationItem, boolean force) {
			//Setting the last item
			LightConversationItem item = new LightConversationItem("", lastConversationItem.getDate(), lastConversationItem.getLocalID(), lastConversationItem.getDate());
			if(trySetLastItem(item, force)) lastConversationItem.getSummary(context, (wasTasked, result) -> item.setMessage((String) result));
		}
		
		void updateLastItem(Context context) {
			//Checking if there is a draft message
			if(draftMessage != null) {
				lastItem = new LightConversationItem(context.getResources().getString(R.string.prefix_draft, draftMessage), draftUpdateTime, true);
			} else if(!draftFiles.isEmpty()) {
				//Converting the draft list to a string resource list
				ArrayList<Integer> draftStringRes = new ArrayList<>();
				for(DraftFile draft : draftFiles) draftStringRes.add(getNameFromContentType(draft.getFileType()));
				
				String summary;
				if(draftStringRes.size() == 1) summary = context.getResources().getString(draftStringRes.get(0));
				else summary = context.getResources().getQuantityString(R.plurals.message_multipleattachments, draftStringRes.size(), draftStringRes.size());
				lastItem = new LightConversationItem(context.getResources().getString(R.string.prefix_draft, summary), draftUpdateTime, true);
			} else {
				//Getting the list
				ArrayList<ConversationItem> conversationItems = getConversationItems();
				if(conversationItems == null || conversationItems.isEmpty()) return;
				
				//Getting the last conversation item
				ConversationItem lastConversationItem = conversationItems.get(conversationItems.size() - 1);
				
				//Setting the last item
				lastItem = new LightConversationItem("", lastConversationItem.getDate(), lastConversationItem.getLocalID(), lastConversationItem.getServerID());
				lastConversationItem.getSummary(context, new Constants.ResultCallback<String>() {
					@Override
					public void onResult(boolean wasTasked, String result) {
						lastItem.setMessage(result);
					}
				});
			}
		}
		
		boolean addConversationItems(Context context, List<ConversationItem> list) {
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
					
					if(messageInfo.isOutgoing() && messageInfo.getMessageState() != SharedValues.MessageInfo.stateCodeGhost) {
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
							ConversationManager.MessageInfo sharedMessageInfo = null;
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
			addConversationItemRelations(this, conversationItems, updateList, MainApplication.getInstance(), true);
			
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
		
		boolean removeConversationItem(Context context, ConversationItem item) {
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
			removeConversationItemRelation(this, conversationItems, itemIndex, context, true);
			
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
		
		void addChunk(List<ConversationItem> list) {
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
				if(update && conversationItem instanceof MessageInfo) addConversationItemRelation(this, conversationItems, (MessageInfo) conversationItem, context, true);
				
				//Returning the index
				return 0;
			}
			
			//Iterating over the conversation items backwards (more recent items appear at the end of the list, and new items are more likely to be recent than old)
			for(int i = conversationItems.size() - 1; i >= 0; i--) {
				//Skipping the remainder of the iteration if the item is newer
				if(ConversationManager.compareConversationItems(conversationItems.get(i), conversationItem) > 0) continue;
				
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
				DatabaseManager.getInstance().deleteConversation(conversationInfo);
				
				//Returning
				return null;
			}
		}
		
		void setActivityCallbacks(ActivityCallbacks activityCallbacks) {
			this.activityCallbacks = activityCallbacks;
		}
		
		private ActivityCallbacks getActivityCallbacks() {
			return activityCallbacks;
		}
		
		static abstract class ActivityCallbacks {
			abstract void listUpdateFully();
			abstract void listUpdateInserted(int index);
			abstract void listUpdateRemoved(int index);
			abstract void listUpdateMove(int from, int to);
			abstract void listUpdateUnread();
			abstract void listScrollToBottom();
			abstract void listAttemptScrollToBottom(int... newIndices);
			
			abstract void chatUpdateTitle();
			abstract void chatUpdateUnreadCount();
			abstract void chatUpdateMemberAdded(MemberInfo member, int index);
			abstract void chatUpdateMemberRemoved(MemberInfo member, int index);
			
			abstract void itemsAdded(List<ConversationManager.ConversationItem> list);
			abstract void tapbackAdded(TapbackInfo item);
			abstract void stickerAdded(StickerInfo item);
			abstract void messageSendFailed(MessageInfo message);
			
			abstract Messaging.AudioPlaybackManager getAudioPlaybackManager();
			
			abstract void playScreenEffect(String effect, View target);
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
		
		static int getRandomConversationColor() {
			return standardUserColors[Constants.getRandom().nextInt(standardUserColors.length)];
		}
		
		boolean conversationMembersContain(String user) {
			return findConversationMember(user) != null;
		}
		
		List<MemberInfo> getConversationMembers() {
			return conversationMembers;
		}
		
		MemberInfo findConversationMember(String user) {
			for(MemberInfo member : conversationMembers) if(member.name.equals(user)) return member;
			return null;
		}
		
		/* void setConversationMembers(ArrayList<MemberInfo> value) {
			conversationMembers = value;
		} */
		
		void addConversationMember(MemberInfo member) {
			int index = Collections.binarySearch(conversationMembers, member, memberInfoComparator);
			if(index >= 0) return;
			index = (index * -1) - 1;
			conversationMembers.add(index, member);
			if(activityCallbacks != null) activityCallbacks.chatUpdateMemberAdded(member, index);
		}
		
		void removeConversationMember(MemberInfo member) {
			int index = conversationMembers.indexOf(member);
			conversationMembers.remove(member);
			if(activityCallbacks != null) activityCallbacks.chatUpdateMemberRemoved(member, index);
		}
		
		/* void removeConversationMember(String user) {
			MemberInfo member = findConversationMember(user);
			if(member != null) conversationMembers.remove(member);
		} */
		
		ArrayList<String> getConversationMembersAsCollection() {
			//Returning null if the conversation is server incomplete (awaiting information from the server, which includes the members)
			if(conversationState == ConversationState.INCOMPLETE_SERVER) return null;
			
			//Creating the array
			ArrayList<String> list = new ArrayList<>(conversationMembers.size());
			for(int i = 0; i < conversationMembers.size(); i++)
				list.add(conversationMembers.get(i).name);
			
			//Returning the list
			return list;
		}
		
		String[] getConversationMembersAsArray() {
			//Returning null if the conversation is server incomplete (awaiting information from the server, which includes the members)
			if(conversationState == ConversationState.INCOMPLETE_SERVER) return null;
			
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
		
		void setConversationMembersCreateColors(String[] members) {
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
		
		boolean isGroupChat() {
			return conversationMembers.size() > 1;
		}
		
		void buildTitle(Context context, Constants.TaskedResultCallback<String> resultCallback) {
			//Returning null if the conversation is server incomplete (awaiting information from the server, which includes the members)
			
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
			if(members == null || members.length == 0) {
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
			
			//Creating a weak reference to the context
			final WeakReference<Context> contextReference = new WeakReference<>(context);
			
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
						
						//Getting the context
						Context context = contextReference.get();
						if(context == null) return;
						
						//Returning the string
						resultCallback.onResult(Constants.createLocalizedList(namedConversationMembers.toArray(new String[0]), context.getResources()), wasTasked);
					}
				});
			}
		}
		
		static String buildTitleDirect(Context context, String name, String[] members) {
			//Returning the conversation title if it is valid
			if(name != null && !name.isEmpty()) return name;
			
			//Returning "unknown" if the conversation has no members
			if(members.length == 0) return context.getResources().getString(R.string.part_unknown);
			
			//Returning the string
			return Constants.createLocalizedList(members, context.getResources());
		}
		
		String getStaticTitle() {
			return title;
		}
		
		void setTitle(Context context, String value) {
			//Returning if the operation is invalid
			if((title != null && title.equals(value))) return;
			
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
		}
		
		private boolean getSelected() {
			return selectionSource.getSelected(localID);
		}
		
		void updateSelected() {
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
		
		interface SelectionSource {
			boolean getSelected(long identifier);
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
		
		void requestScreenEffect(String effect, View target) {
			//Returning if the callback isn't set up
			if(activityCallbacks == null) return;
			
			//Setting and playing the effect
			activityCallbacks.playScreenEffect(effect, target);
		}
		
		static abstract class BaseViewHolder extends RecyclerView.ViewHolder {
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
		
		static class ItemViewHolder extends BaseViewHolder {
			//Creating the view values
			//final ViewGroup iconGroup;
			final View selectedIndicator;
			
			//final TextView conversationTitle;
			final TextView conversationMessage;
			final TextView conversationTime;
			final TextView conversationUnread;
			
			final View flagMuted;
			
			ItemViewHolder(View view) {
				super(view);
				
				//Getting the views
				//iconGroup = view.findViewById(R.id.conversationicon);
				selectedIndicator = view.findViewById(R.id.selected);
				
				//conversationTitle = view.findViewById(R.id.title);
				conversationMessage = view.findViewById(R.id.message);
				conversationTime = view.findViewById(R.id.time);
				conversationUnread = view.findViewById(R.id.unread);
				
				flagMuted = view.findViewById(R.id.flag_muted);
			}
		}
		
		static class SimpleItemViewHolder extends BaseViewHolder {
			SimpleItemViewHolder(View view) {
				super(view);
			}
		}
	}
	
	static class DraftFile {
		private final long localID;
		private final File file;
		private final String fileName;
		private final long fileSize;
		private final String fileType;
		
		private final File originalFile;
		private final long modificationDate;
		
		DraftFile(long localID, File file, String fileName, long fileSize, String fileType) {
			this.localID = localID;
			this.file = file;
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.fileType = fileType;
			this.originalFile = null;
			this.modificationDate = 0;
		}
		
		DraftFile(long localID, File file, String fileName, long fileSize, String fileType, File originalFile, long modificationDate) {
			this.localID = localID;
			this.file = file;
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.fileType = fileType;
			this.originalFile = originalFile;
			this.modificationDate = modificationDate;
		}
		
		long getLocalID() {
			return localID;
		}
		
		File getFile() {
			return file;
		}
		
		String getFileName() {
			return fileName;
		}
		
		long getFileSize() {
			return fileSize;
		}
		
		String getFileType() {
			return fileType;
		}
		
		File getOriginalFile() {
			return originalFile;
		}
		
		long getModificationDate() {
			return modificationDate;
		}
		
		static String getRelativePath(Context context, File file) {
			return MainApplication.getDraftDirectory(context).toURI().relativize(file.toURI()).getPath();
		}
		
		static File getAbsolutePath(Context context, String path) {
			return Paths.get(MainApplication.getDraftDirectory(context).getPath()).resolve(path).toFile();
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
	
	static class MessageInfo extends ConversationItem<MessageInfo.ViewHolder> {
		//Creating the constants
		static final int itemType = 0;
		static final int itemViewType = ConversationItem.viewTypeMessage;
		
		private static final int dpDefaultMessagePadding = 5;
		private static final int dpRelatedMessagePadding = 1;
		private static final int dpInterMessagePadding = 2;
		private static final int dpCornerAnchored = 5;
		private static final int dpCornerUnanchored = 20;
		
		//Creating the values
		private final String sender;
		private final MessageTextInfo messageText;
		private final ArrayList<AttachmentInfo> attachments;
		private final String sendStyle;
		private boolean sendStyleViewed;
		private int messageState;
		private int errorCode;
		private boolean errorDetailsAvailable;
		private String errorDetails = null;
		private long dateRead;
		private boolean isSending = false;
		private float sendProgress = -1;
		
		//Creating the placement values
		private transient boolean hasTimeDivider = false;
		private transient boolean isAnchoredTop = false;
		private transient boolean isAnchoredBottom = false;
		private transient boolean isShowingMessageState = false;
		
		//Creating the other values
		private transient boolean playEffectRequested = false;
		
		MessageInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, String sender, String messageText, ArrayList<AttachmentInfo> attachments, String sendStyle, boolean sendStyleViewed, long date, int messageState, int errorCode, boolean errorDetailsAvailable, long dateRead) {
			//Calling the super constructor
			super(localID, serverID, guid, date, conversationInfo);
			
			//Invalidating the text if it is empty
			//if(messageText != null && messageText.isEmpty()) messageText = null;
			
			//Setting the values
			this.sender = sender;
			this.messageText = messageText == null ? null : new MessageTextInfo(localID, guid, this, messageText);
			this.attachments = attachments;
			this.sendStyle = sendStyle;
			this.sendStyleViewed = sendStyleViewed;
			this.messageState = messageState;
			this.errorCode = errorCode;
			this.dateRead = dateRead;
		}
		
		MessageInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, String sender, String messageText, String sendStyle, boolean sendStyleViewed, long date, int messageState, int errorCode, boolean errorDetailsAvailable, long dateRead) {
			//Calling the super constructor
			super(localID, serverID, guid, date, conversationInfo);
			
			//Setting the values
			this.sender = sender;
			this.messageText = messageText == null ? null : new MessageTextInfo(localID, guid, this, messageText);
			this.sendStyle = sendStyle;
			this.sendStyleViewed = sendStyleViewed;
			this.attachments = new ArrayList<>();
			this.messageState = messageState;
			this.errorCode = errorCode;
			this.dateRead = dateRead;
		}
		
		void addAttachment(AttachmentInfo attachment) {
			attachments.add(attachment);
		}
		
		void removeAttachment(AttachmentInfo attachment) {
			attachments.remove(attachment);
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
		
		String getErrorDetails() {
			return errorDetails;
		}
		
		void setErrorDetails(String errorDetails) {
			this.errorDetails = errorDetails;
			errorDetailsAvailable = errorDetails != null;
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
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) updateViewEdges(viewHolder, isLTR);
		}
		
		private void updateViewEdges(ViewHolder viewHolder, boolean isLTR) {
			/*
			true + true = true
			true + false = false
			false + true = false
			false + false = true
			 */
			boolean alignToRight = isOutgoing() == isLTR;
			
			//Updating the padding
			viewHolder.itemView.setPadding(viewHolder.itemView.getPaddingLeft(), Constants.dpToPx(isAnchoredTop ? dpRelatedMessagePadding : dpDefaultMessagePadding), viewHolder.itemView.getPaddingRight(), Constants.dpToPx(isAnchoredBottom ? dpRelatedMessagePadding : dpDefaultMessagePadding));
			
			//Checking if the message is outgoing
			if(!isOutgoing()) {
				//Setting the user information
				boolean showUserInfo = !isAnchoredTop; //If the message isn't anchored to the top
				if(getConversationInfo().isGroupChat()) viewHolder.labelSender.setVisibility(showUserInfo ? View.VISIBLE : View.GONE);
				if(viewHolder.profileGroup != null) viewHolder.profileGroup.setVisibility(showUserInfo ? View.VISIBLE : View.GONE);
			}
			
			//Getting the corner values in pixels
			int pxCornerAnchored = Constants.dpToPx(dpCornerAnchored);
			int pxCornerUnanchored = Constants.dpToPx(dpCornerUnanchored);
			
			//Checking if there is text
			if(messageText != null) messageText.updateViewEdges((MessageTextInfo.ViewHolder) viewHolder.messageComponents.get(0), isAnchoredTop, isAnchoredBottom || !attachments.isEmpty(), alignToRight, pxCornerAnchored, pxCornerUnanchored);
			
			//Iterating over the attachments
			int attachmentIndex = 0;
			int messageTextDiff = messageText == null ? 0 : 1;
			for(AttachmentInfo attachment : attachments) {
				//Getting the view holder
				AttachmentInfo.ViewHolder attachmentViewHolder = (AttachmentInfo.ViewHolder) viewHolder.messageComponents.get(attachmentIndex + messageTextDiff);
				
				//Calculating the anchorage
				boolean itemAnchoredTop = messageText != null || attachmentIndex > 0 || isAnchoredTop;
				boolean itemAnchoredBottom = attachmentIndex < attachments.size() - 1 || isAnchoredBottom;
				
				//Updating the upper padding
				attachmentViewHolder.itemView.setPadding(attachmentViewHolder.itemView.getPaddingLeft(), attachmentIndex + messageTextDiff > 0 ? Constants.dpToPx(dpInterMessagePadding) : 0, attachmentViewHolder.itemView.getPaddingRight(), attachmentViewHolder.itemView.getPaddingBottom());
				
				//Updating the attachment's edges
				attachment.updateViewEdges(attachmentViewHolder,
						itemAnchoredTop, //There is message text above, there is an attachment above or the message is anchored anyways
						itemAnchoredBottom, //There is an attachment below or the message is anchored anyways
						alignToRight,
						pxCornerAnchored,
						pxCornerUnanchored);
				
				//Increasing the index
				attachmentIndex++;
			}
		}
		
		private void prepareActivityStateDisplay(ViewHolder viewHolder, Context context) {
			//Returning if read receipt showing is disabled
			if(!Preferences.checkPreferenceShowReadReceipts(context)) return;
			
			//Getting the requested state
			isShowingMessageState = (this == getConversationInfo().getActivityStateTargetRead() || this == getConversationInfo().getActivityStateTargetDelivered()) &&
					messageState != SharedValues.MessageInfo.stateCodeGhost &&
					messageState != SharedValues.MessageInfo.stateCodeIdle &&
					messageState != SharedValues.MessageInfo.stateCodeSent;
			
			//Setting up the label
			if(isShowingMessageState) {
				viewHolder.labelActivityStatus.setVisibility(View.VISIBLE);
				viewHolder.labelActivityStatus.setCurrentText(getDeliveryStatusText(context));
			} else {
				viewHolder.labelActivityStatus.setVisibility(View.GONE);
			}
		}
		
		void updateActivityStateDisplay(Context context) {
			//Returning if read receipt showing is disabled
			if(!Preferences.checkPreferenceShowReadReceipts(context)) return;
			
			//Getting the requested state
			boolean requestedState = (this == getConversationInfo().getActivityStateTargetRead() || this == getConversationInfo().getActivityStateTargetDelivered()) &&
					messageState != SharedValues.MessageInfo.stateCodeGhost &&
					messageState != SharedValues.MessageInfo.stateCodeIdle &&
					messageState != SharedValues.MessageInfo.stateCodeSent;
			
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) updateActivityStateDisplay(viewHolder, context, isShowingMessageState, requestedState);
			
			//Setting the current state
			isShowingMessageState = requestedState;
		}
		
		private void updateActivityStateDisplay(ViewHolder viewHolder, Context context, boolean currentState, boolean requestedState) {
			//Returning if read receipt showing is disabled
			if(!Preferences.checkPreferenceShowReadReceipts(context)) return;
			
			//Checking if the requested state matches the current state
			if(requestedState == currentState) {
				//Updating the text
				CharSequence text = getDeliveryStatusText(context);
				if(requestedState && text != null && !text.toString().equals(((TextView) viewHolder.labelActivityStatus.getCurrentView()).getText().toString())) viewHolder.labelActivityStatus.setText(getDeliveryStatusText(context));
			} else {
				//Checking if the conversation should display its state
				if(requestedState) {
					//Setting the text
					viewHolder.labelActivityStatus.setCurrentText(getDeliveryStatusText(context));
					
					//Showing the label
					viewHolder.labelActivityStatus.setVisibility(View.VISIBLE);
					viewHolder.labelActivityStatus.startAnimation(AnimationUtils.loadAnimation(context, R.anim.messagestatus_slide_in_bottom));
					
					//Measuring the label
					viewHolder.labelActivityStatus.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
					
					//Expanding the parent view
					ViewGroup parentView = (ViewGroup) viewHolder.itemView;
					parentView.getLayoutParams().height = parentView.getHeight(); //Freezing the parent view height (to prevent it from expanding for a few moments before the label's view pass)
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewHolder.labelActivityStatus.getLayoutParams();
					Constants.ResizeAnimation parentAnim = new Constants.ResizeAnimation(parentView, parentView.getHeight(), parentView.getHeight() + (viewHolder.labelActivityStatus.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin));
					parentAnim.setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime));
					parentAnim.setInterpolator(new AccelerateDecelerateInterpolator());
					parentAnim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							parentView.post(() -> {
								//Getting the view holder
								ViewHolder newViewHolder = getViewHolder();
								if(newViewHolder == null) return;
								
								//Restoring the content container's size
								newViewHolder.itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
								newViewHolder.itemView.requestLayout();
							});
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {}
					});
					parentView.startAnimation(parentAnim);
				} else {
					//Hiding the label
					Animation labelAnim = AnimationUtils.loadAnimation(context, R.anim.messagestatus_slide_out_top);
					/* labelAnim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							//Setting the label's visibility
							ViewHolder newViewHolder = getViewHolder();
							if(newViewHolder != null) newViewHolder.labelActivityStatus.setVisibility(View.GONE);
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {}
					}); */
					viewHolder.labelActivityStatus.startAnimation(labelAnim);
					
					//Collapsing the parent view
					ViewGroup parentView = (ViewGroup) viewHolder.labelActivityStatus.getParent();
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewHolder.labelActivityStatus.getLayoutParams();
					Constants.ResizeAnimation parentAnim = new Constants.ResizeAnimation(parentView, parentView.getHeight(), parentView.getHeight() - (viewHolder.labelActivityStatus.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin));
					parentAnim.setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime));
					parentAnim.setInterpolator(new AccelerateDecelerateInterpolator());
					parentAnim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							//Getting the view holder
							ViewHolder newViewHolder = getViewHolder();
							if(newViewHolder == null) return;
							
							//Hiding the label
							newViewHolder.labelActivityStatus.setVisibility(View.GONE);
							
							//Restoring the content container
							newViewHolder.itemView.post(() -> {
								ViewHolder anotherNewViewHolder = getViewHolder();
								if(anotherNewViewHolder == null) return;
								
								anotherNewViewHolder.itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
								anotherNewViewHolder.itemView.requestLayout();
							});
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {}
					});
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
					//Creating the calendars
					Calendar sentCal = Calendar.getInstance();
					sentCal.setTimeInMillis(dateRead);
					Calendar nowCal = Calendar.getInstance();
					
					//Creating the string
					return context.getResources().getString(R.string.state_read) + Constants.bulletSeparator + getDeliveryStatusTime(context, sentCal, nowCal);
				}
			}
		}
		
		private static String getDeliveryStatusTime(Context context, Calendar sentCal, Calendar nowCal) {
			//Creating the date
			Date sentDate = sentCal.getTime();
			
			//If the message was sent today
			if(sentCal.get(Calendar.ERA) == nowCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == sentCal.get(Calendar.DAY_OF_YEAR))
				return DateFormat.getTimeFormat(context).format(sentDate);
			
			//If the message was sent yesterday
			{
				Calendar compareCal = (Calendar) nowCal.clone();
				compareCal.add(Calendar.DAY_OF_YEAR, -1); //Today (now) -> Yesterday
				if(sentCal.get(Calendar.ERA) == compareCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == compareCal.get(Calendar.YEAR) && sentCal.get(Calendar.DAY_OF_YEAR) == compareCal.get(Calendar.DAY_OF_YEAR))
					return context.getResources().getString(R.string.time_yesterday);
			}
			
			//If the days are within the same 7-day period (Sunday)
			{
				Calendar compareCal = (Calendar) nowCal.clone();
				compareCal.add(Calendar.DAY_OF_YEAR, -7); //Today (now) -> One week ago
				if(Constants.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format("EEEE", sentCal).toString();
			}
			
			//If the days are within the same year period (Dec 9)
			{
				Calendar compareCal = (Calendar) nowCal.clone();
				compareCal.add(Calendar.YEAR, -1); //Today (now) -> One year ago
				if(Constants.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format(context.getString(R.string.dateformat_withinyear), sentCal).toString();
			}
			
			//Different years (Dec 9, 2018)
			return DateFormat.format(context.getString(R.string.dateformat_outsideyear), sentCal).toString();
		}
		
		boolean sendMessage(Context context) {
			//Creating a weak reference to the context
			WeakReference<Context> contextReference = new WeakReference<>(context);
			
			//Creating the callback listener
			ConnectionService.MessageResponseManager messageResponseManager = new ConnectionService.MessageResponseManager() {
				@Override
				void onSuccess() {}
				
				@Override
				void onFail(int errorCode, String errorDetails) {
					//Setting the error code
					setErrorCode(errorCode);
					setErrorDetails(errorDetails);
					
					//Updating the message's database entry
					new UpdateErrorCodeTask(getLocalID(), errorCode, errorDetails).execute();
					
					//Updating the adapter
					ConversationInfo.ActivityCallbacks updater = getConversationInfo().getActivityCallbacks();
					if(updater != null) updater.messageSendFailed(MessageInfo.this);
					
					//Getting the context
					//Context context = contextReference.get();
					//if(context == null) return;
					
					//Updating the view
					ViewHolder viewHolder = getViewHolder();
					if(viewHolder != null) updateViewProgressState(viewHolder);
				}
			};
			
			//Checking if the service is dead
			ConnectionService connectionService = ConnectionService.getInstance();
			if(connectionService == null) {
				//Starting the service
				//if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(new Intent(context, ConnectionService.class));
				//else context.startService(new Intent(context, ConnectionService.class));
				
				//context.startService(new Intent(context, ConnectionService.class));
				
				//Telling the response manager
				messageResponseManager.onFail(Constants.messageErrorCodeAirNetwork, null);
				
				//Returning false
				return false;
			}
			
			//Getting the view holder
			ViewHolder viewHolder = getViewHolder();
			
			//Checking if there is text
			if(messageText != null) {
				if(viewHolder != null) {
					//Hiding the error view
					viewHolder.buttonSendError.setVisibility(View.GONE);
					
					//Hiding the progress view
					viewHolder.progressSend.setVisibility(View.GONE);
				}
				
				//Sending the message and returning the result
				return getConversationInfo().getState() == ConversationManager.ConversationInfo.ConversationState.READY ?
						connectionService.sendMessage(getConversationInfo().getGuid(), getMessageText(), messageResponseManager) :
						connectionService.sendMessage(getConversationInfo().getNormalizedConversationMembersAsArray(), getMessageText(), getConversationInfo().getService(), messageResponseManager);
			} else {
				//Returning false if there are no attachments
				if(attachments.isEmpty()) return false;
				
				//Getting the attachment
				AttachmentInfo attachmentInfo = attachments.get(0);
				
				//Constructing the push request
				ConnectionService.FilePushRequest request = attachmentInfo.getDraftingPushRequest();
				if(request != null) {
					request.setAttachmentID(attachmentInfo.getLocalID());
					request.setUploadRequested(true);
				} else {
					if(attachmentInfo.file == null) return false;
					request = new ConnectionService.FilePushRequest(attachmentInfo.file, attachmentInfo.fileType, attachmentInfo.fileName, -1, getConversationInfo(), attachmentInfo.getLocalID(), -1, ConnectionService.FilePushRequest.stateAttached, System.currentTimeMillis(), true);
				}
				request.getCallbacks().onStart = () -> {
					//Updating the progress bar
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					newViewHolder.progressSend.setProgress(0);
				};
				request.getCallbacks().onAttachmentPreparationFinished = file -> {
					//Setting the attachment data
					attachmentInfo.file = file;
					attachmentInfo.fileUri = null;
					
					//Getting the context
					Context newContext = contextReference.get();
					if(newContext == null) return;
					
					//Updating the view
					AttachmentInfo.ViewHolder attachmentViewHolder = (AttachmentInfo.ViewHolder) attachmentInfo.getViewHolder();
					if(attachmentViewHolder != null) {
						attachmentViewHolder.groupDownload.setVisibility(View.GONE);
						attachmentViewHolder.groupContent.setVisibility(View.GONE);
						attachmentViewHolder.groupProcessing.setVisibility(View.GONE);
						if(attachmentViewHolder.groupFailed != null) attachmentViewHolder.groupFailed.setVisibility(View.GONE);
						attachmentInfo.updateContentView(attachmentViewHolder, newContext);
					}
				};
				request.getCallbacks().onUploadProgress = value -> {
					//Setting the send progress
					sendProgress = value;
					
					//Updating the progress bar
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					newViewHolder.progressSend.setProgress(value);
				};
				request.getCallbacks().onUploadFinished = checksum -> {
					//Setting the checksum
					attachmentInfo.setFileChecksum(checksum);
					
					//Updating the progress bar
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					newViewHolder.progressSend.setProgress(1);
				};
				request.getCallbacks().onUploadResponseReceived = () -> {
					//Forwarding the event to the response manager
					messageResponseManager.onSuccess();
					
					//Setting the message as not sending
					isSending = false;
					
					//Getting the view
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					
					//Hiding the progress bar
					TransitionManager.beginDelayedTransition((ViewGroup) newViewHolder.itemView);
					newViewHolder.progressSend.setVisibility(View.GONE);
				};
				request.getCallbacks().onFail = (resultCode, details) -> {
					//Forwarding the event to the response manager
					messageResponseManager.onFail(resultCode, details);
					
					//Setting the message as not sending
					isSending = false;
					
					//Hiding the progress bar
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					newViewHolder.progressSend.setVisibility(View.GONE);
				};
				
				//Queuing the request
				connectionService.addFileProcessingRequest(request);
				
				//Setting the upload values
				isSending = true;
				sendProgress = -1;
				
				if(viewHolder != null) {
					//Hiding the error view
					viewHolder.buttonSendError.setVisibility(View.GONE);
					
					//Showing and configuring the progress view
					viewHolder.progressSend.setVisibility(View.VISIBLE);
					viewHolder.progressSend.spin();
				}
				
				//Returning true
				return true;
			}
		}
		
		void deleteMessage(Context context) {
			//Removing the item from the conversation in memory
			getConversationInfo().removeConversationItem(context, this);
			
			//Deleting the message on disk
			new DeleteMessagesTask().execute(getLocalID());
		}
		
		private static class DeleteMessagesTask extends AsyncTask<Long, Void, Void> {
			@Override
			protected Void doInBackground(Long... identifiers) {
				for(long id : identifiers) DatabaseManager.getInstance().deleteMessage(id);
				return null;
			}
		}
		
		private static class UpdateErrorCodeTask extends AsyncTask<Void, Void, Void> {
			private final long messageID;
			private final int errorCode;
			private final String details;
			
			UpdateErrorCodeTask(long messageID, int errorCode, String details) {
				this.messageID = messageID;
				this.errorCode = errorCode;
				this.details = details;
			}
			
			@Override
			protected Void doInBackground(Void... parameters) {
				//Updating the entry in the database
				DatabaseManager.getInstance().updateMessageErrorCode(messageID, errorCode, details);
				
				//Returning
				return null;
			}
		}
		
		void updateTimeDivider(Context context) {
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) configureTimeDivider(context, viewHolder.labelTimeDivider, hasTimeDivider);
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
			Calendar sentCal = Calendar.getInstance();
			sentCal.setTimeInMillis(getDate());
			Calendar nowCal = Calendar.getInstance();
			
			//Creating the date
			Date sentDate = new Date(getDate());
			
			//If the message was sent today
			if(sentCal.get(Calendar.ERA) == nowCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == sentCal.get(Calendar.DAY_OF_YEAR))
				return /*context.getResources().getString(R.string.time_today) + Constants.bulletSeparator + */DateFormat.getTimeFormat(context).format(sentDate);
			
			//If the message was sent yesterday
			{
				Calendar compareCal = (Calendar) nowCal.clone();
				compareCal.add(Calendar.DAY_OF_YEAR, -1); //Today (now) -> Yesterday
				if(sentCal.get(Calendar.ERA) == compareCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == compareCal.get(Calendar.YEAR) && sentCal.get(Calendar.DAY_OF_YEAR) == compareCal.get(Calendar.DAY_OF_YEAR))
					return context.getResources().getString(R.string.time_yesterday) + Constants.bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
			}
			
			//If the days are within the same 7-day period (Sunday)
			{
				Calendar compareCal = (Calendar) nowCal.clone();
				compareCal.add(Calendar.DAY_OF_YEAR, -7); //Today (now) -> One week ago
				if(Constants.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format("EEEE", sentCal) + Constants.bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
			}
			
			//If the days are within the same year period (Sunday, Dec 9)
			{
				Calendar compareCal = (Calendar) nowCal.clone();
				compareCal.add(Calendar.YEAR, -1); //Today (now) -> One year ago
				if(Constants.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format(context.getString(R.string.dateformat_withinyear_weekday), sentCal) + Constants.bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
			}
			
			//Different years (Dec 9, 2018)
			return DateFormat.format(context.getString(R.string.dateformat_outsideyear), sentCal) + Constants.bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
		}
		
		void setHasTimeDivider(boolean hasTimeDivider) {
			this.hasTimeDivider = hasTimeDivider;
		}
		
		private interface PoolSourceAccepter {
			void accept(int itemViewType, MessageComponent.ViewHolder viewHolder);
		}
		
		@Override
		void bindView(ViewHolder viewHolder, Context context) {
			//Getting the properties
			boolean isFromMe = isOutgoing();
			
			//Setting the message part container's draw order
			viewHolder.containerMessagePart.setZ(1);
			
			//Converting the view to a view group
			//LinearLayout view = (LinearLayout) viewHolder.itemView;
			
			//Getting the pool source
			Messaging.MessageListRecyclerAdapter.PoolSource poolSource = viewHolder.getRemovePoolSource();
			
			SparseArray<List<MessageComponent.ViewHolder>> componentViewHolderList = new SparseArray<>();
			if(!viewHolder.messageComponents.isEmpty()) {
				//Creating the view adder
				PoolSourceAccepter viewAdder = (itemViewType, itemViewHolder) -> {
					List<MessageComponent.ViewHolder> list = componentViewHolderList.get(itemViewType);
					if(list == null) {
						list = new ArrayList<>();
						componentViewHolderList.put(itemViewType, list);
					}
					
					list.add(itemViewHolder);
				};
				
				//Sorting the components by type into the map
				for(MessageComponent.ViewHolder componentViewHolder : viewHolder.messageComponents) {
					if(componentViewHolder instanceof MessageTextInfo.ViewHolder) viewAdder.accept(MessageTextInfo.itemViewType, componentViewHolder);
					else if(componentViewHolder instanceof ImageAttachmentInfo.ViewHolder) viewAdder.accept(ImageAttachmentInfo.ITEM_VIEW_TYPE, componentViewHolder);
					else if(componentViewHolder instanceof AudioAttachmentInfo.ViewHolder) viewAdder.accept(AudioAttachmentInfo.ITEM_VIEW_TYPE, componentViewHolder);
					else if(componentViewHolder instanceof VideoAttachmentInfo.ViewHolder) viewAdder.accept(VideoAttachmentInfo.ITEM_VIEW_TYPE, componentViewHolder);
					else if(componentViewHolder instanceof OtherAttachmentInfo.ViewHolder) viewAdder.accept(OtherAttachmentInfo.ITEM_VIEW_TYPE, componentViewHolder);
				}
				
				//Clearing and removing the components
				viewHolder.messageComponents.clear();
				viewHolder.containerMessagePart.removeAllViews();
			}
			
			Consumer<MessageComponent> viewHolderGetter = component -> {
				//Getting the view holder
				MessageComponent.ViewHolder componentViewHolder;
				
				List<MessageComponent.ViewHolder> list = componentViewHolderList.get(component.getItemViewType());
				if(list == null || list.isEmpty()) componentViewHolder = poolSource.getComponent(component, viewHolder.containerMessagePart, context);
				else {
					componentViewHolder = list.get(0);
					list.remove(0);
				}
				
				//Adding the component view holder to the message view holder
				viewHolder.messageComponents.add(componentViewHolder);
				viewHolder.containerMessagePart.addView(componentViewHolder.itemView);
				final int componentIndex = viewHolder.messageComponents.size() - 1;
				
				component.bindView(componentViewHolder, context);
				component.setViewHolderSource(() -> {
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return null;
					return newViewHolder.messageComponents.get(componentIndex);
				});
			};
			
			//Building the component views
			if(messageText != null) viewHolderGetter.accept(messageText);
			for(AttachmentInfo<?> attachmentInfo : attachments) viewHolderGetter.accept(attachmentInfo);
			
			//Sending any excess component views back to the pool
			for(int i = 0; i < componentViewHolderList.size(); i++) {
				int itemViewType = componentViewHolderList.keyAt(i);
				List<MessageComponent.ViewHolder> list = componentViewHolderList.valueAt(i);
				for(MessageComponent.ViewHolder componentViewHolder : list) {
					componentViewHolder.releaseResources();
					poolSource.releaseComponent(itemViewType, componentViewHolder);
				}
			}
			
			//Setting the alignment
			{
				ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) viewHolder.containerMessagePart.getLayoutParams();
				if(isFromMe) {
					params.startToStart = ConstraintLayout.LayoutParams.UNSET;
					params.endToEnd = R.id.barrier_alert;
				} else {
					params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
					params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
				}
			}
			
			//Checking if the message is outgoing
			if(isFromMe) {
				//Hiding the user info
				if(viewHolder.profileGroup != null) viewHolder.profileGroup.setVisibility(View.GONE);
				
				//Hiding the sender
				viewHolder.labelSender.setVisibility(View.GONE);
			} else {
				//Inflating the profile stub and getting the profile view
				viewHolder.inflateProfile();
				
				//Showing the profile view
				viewHolder.profileGroup.setVisibility(View.VISIBLE);
				
				//Removing the profile image
				viewHolder.profileImage.setImageBitmap(null);
				viewHolder.profileDefault.setVisibility(View.VISIBLE);
				
				//Assigning the profile image
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context.getApplicationContext(), sender, sender, new BitmapCacheHelper.ImageDecodeResult() {
					@Override
					void onImageMeasured(int width, int height) {}
					
					@Override
					void onImageDecoded(Bitmap result, boolean wasTasked) {
						//Returning if the result is invalid
						if(result == null) return;
						
						//Getting the view holder
						ViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
						if(newViewHolder == null) return;
						newViewHolder.inflateProfile();
						
						//Hiding the default view
						newViewHolder.profileDefault.setVisibility(View.INVISIBLE);
						
						//Setting the bitmap
						newViewHolder.profileImage.setImageBitmap(result);
						
						//Fading in the view
						if(wasTasked) {
							newViewHolder.profileImage.setAlpha(0F);
							newViewHolder.profileImage.animate().alpha(1).setDuration(300).start();
						}
					}
				});
				
				//Checking if the chat is a group chat
				if(getConversationInfo().isGroupChat()) {
					//Setting the sender's name (temporarily)
					viewHolder.labelSender.setText(sender);
					
					//Assigning the sender's name
					MainApplication.getInstance().getUserCacheHelper().assignUserInfo(context.getApplicationContext(), sender, wasTasked -> {
						//Getting the view
						ViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
						if(newViewHolder == null) return null;
						
						//Returning the sender label
						return newViewHolder.labelSender;
					});
					
					//Showing the sender
					viewHolder.labelSender.setVisibility(View.VISIBLE);
				} else {
					//Hiding the sender
					viewHolder.labelSender.setVisibility(View.GONE);
				}
			}
			
			//Checking if the message has no send effect
			if(sendStyle == null || (!Constants.validateBubbleEffect(sendStyle) && !Constants.validateScreenEffect(sendStyle))) {
				//Hiding the "replay" button
				viewHolder.buttonSendEffectReplay.setVisibility(View.GONE);
			} else {
				//Showing and configuring the "replay" button
				viewHolder.buttonSendEffectReplay.setVisibility(View.VISIBLE);
				viewHolder.buttonSendEffectReplay.setOnClickListener(clickedView -> playEffect());
			}
			
			//Setting the text switcher's animations
			viewHolder.labelActivityStatus.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_delayed));
			viewHolder.labelActivityStatus.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
			
			//Setting the status
			//((TextView) view.findViewById(R.id.status)).setText(ConversationInfo.getFormattedTime(getDate()));
			
			//Updating the view edges
			updateViewEdges(viewHolder, context.getResources().getBoolean(R.bool.is_left_to_right));
			
			//Updating the view state display
			prepareActivityStateDisplay(viewHolder, context);
			
			//Enforcing the maximum view width
			/* {
				int maxWidth = getMaxMessageWidth(context.getResources());
				viewHolder.containerMessagePart.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
				if(viewHolder.containerMessagePart.getMeasuredWidth() > maxWidth) viewHolder.containerMessagePart.getLayoutParams().width = maxWidth;
				else viewHolder.containerMessagePart.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
			} */
			
			//Updating the view color
			updateViewColor(viewHolder, context, false);
			
			//Updating the view state
			updateViewProgressState(viewHolder);
			
			//Updating the time divider
			configureTimeDivider(context, viewHolder.labelTimeDivider, hasTimeDivider);
			
			//Building the sticker view
			//buildStickerView(view);
			
			//Building the tapback view
			//buildTapbackView(view);
			
			//Restoring the upload state
			restoreUploadState(viewHolder);
			
			//Playing the screen effect if requested
			if(playEffectRequested) {
				playEffect(viewHolder);
				playEffectRequested = false;
			}
		}
		
		/**
		 * When the view is scrolled to on the list
		 * This can be used to update view components (eg. download progress), because sometimes bindView() will not be called when a view is just off the screen and there is no reason to recycle it
		 */
		void onScrollShow() {
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder == null) return;
			
			//Restoring the upload state
			restoreUploadState(viewHolder);
			
			//Updating the view state display
			prepareActivityStateDisplay(viewHolder, MainApplication.getInstance());
			updateViewProgressState();
			
			//Updating the attachments
			for(AttachmentInfo attachmentInfo : attachments) attachmentInfo.onScrollShow();
		}
		
		@Override
		void updateViewColor(Context context) {
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) updateViewColor(viewHolder, context, true);
		}
		
		private void updateViewColor(ViewHolder viewHolder, Context context, boolean updateComponents) {
			//Setting the user tint
			if(!isOutgoing() && viewHolder.profileGroup != null) {
				MemberInfo memberInfo = getConversationInfo().findConversationMember(sender);
				int backgroundColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				
				viewHolder.profileDefault.setColorFilter(backgroundColor, android.graphics.PorterDuff.Mode.MULTIPLY);
			}
			
			//Setting the upload spinner tint
			viewHolder.progressSend.setBarColor(Preferences.checkPreferenceAdvancedColor(context) ? getConversationInfo().getConversationColor() : context.getResources().getColor(R.color.colorPrimary, null));
			
			//Updating the message colors
			if(updateComponents && messageText != null) {
				MessageTextInfo.ViewHolder componentVH = messageText.getViewHolder();
				if(componentVH != null) messageText.updateViewColor(componentVH, context);
			}
			
			//Updating the attachment colors
			if(updateComponents) for(AttachmentInfo attachmentInfo : attachments) {
				AttachmentInfo.ViewHolder componentVH = (AttachmentInfo.ViewHolder) attachmentInfo.getViewHolder();
				if(componentVH != null) attachmentInfo.updateViewColor(componentVH, context);
			}
		}
		
		void updateViewProgressState() {
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) updateViewProgressState(viewHolder);
		}
		
		private static final float ghostAlpha = 0.50F;
		private void updateViewProgressState(ViewHolder viewHolder) {
			//Setting the message part container's alpha
			if(messageState == SharedValues.MessageInfo.stateCodeGhost) viewHolder.containerMessagePart.setAlpha(ghostAlpha);
			else viewHolder.containerMessagePart.setAlpha(1);
			
			//Hiding the error and returning if there wasn't any problem
			if(errorCode == Constants.messageErrorCodeOK) {
				viewHolder.buttonSendError.setVisibility(View.GONE);
				return;
			}
			
			//Showing the error
			viewHolder.buttonSendError.setVisibility(View.VISIBLE);
			
			//Showing the dialog when the button is clicked
			viewHolder.buttonSendError.setOnClickListener(view -> {
				//Getting the context
				Context newContext = view.getContext();
				if(newContext == null) return;
				
				//Creating a weak reference to the context
				final WeakReference<Context> contextReference = new WeakReference<>(newContext);
				
				//Configuring the dialog
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newContext)
						.setTitle(R.string.message_messageerror_title)
						.setNeutralButton(R.string.action_deletemessage, (dialog, which) -> {
							Context anotherNewContext = contextReference.get();
							if(anotherNewContext != null) deleteMessage(anotherNewContext);
						})
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
					case Constants.messageErrorCodeAirServerExternal:
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
					case Constants.messageErrorCodeAirInternal:
						//Setting the message
						dialogBuilder.setMessage(R.string.message_messageerror_desc_air_internal);
						
						//Enabling the retry button
						showRetryButton = true;
						
						break;
					case Constants.messageErrorCodeAirServerBadRequest:
						//Setting the message
						dialogBuilder.setMessage(R.string.message_messageerror_desc_air_badrequest);
						
						//Enabling the retry button
						showRetryButton = true;
						
						break;
					case Constants.messageErrorCodeAirServerUnauthorized:
						//Setting the message
						dialogBuilder.setMessage(R.string.message_messageerror_desc_air_unauthorized);
						
						//Enabling the retry button
						showRetryButton = true;
						
						break;
					case Constants.messageErrorCodeAirServerNoConversation:
						//Setting the message
						dialogBuilder.setMessage(R.string.message_messageerror_desc_air_noconversation);
						
						//Disabling the retry button
						showRetryButton = false;
						
						break;
					case Constants.messageErrorCodeAirServerRequestTimeout:
						//Setting the message
						dialogBuilder.setMessage(R.string.message_messageerror_desc_air_serverexpired);
						
						//Disabling the retry button
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
								newContext.getResources().getString(R.string.message_messageerror_desc_apple_unregistered_generic) :
								newContext.getResources().getString(R.string.message_messageerror_desc_apple_unregistered, getConversationInfo().getConversationMembers().get(0).getName()));
						
						//Disabling the retry button
						showRetryButton = false;
						
						break;
				}
				
				//Showing the retry button (if requested)
				if(showRetryButton) {
					dialogBuilder.setPositiveButton(R.string.action_retry, (dialog, which) -> {
						Context anotherNewContext = contextReference.get();
						if(anotherNewContext != null) sendMessage(anotherNewContext);
					});
				}
				
				//Showing the dialog
				dialogBuilder.create().show();
			});
			
			viewHolder.buttonSendError.setOnLongClickListener(view -> {
				//Getting the context
				Context context = view.getContext();
				if(context == null) return false;
				
				if(errorDetailsAvailable) {
					if(errorDetails == null) {
						//Fetching the error details
						new ReadErrorMessageTask(this).execute();
					} else displayErrorDialog(context, errorDetails);
				} else {
					//Notifying the user via a toast
					Toast.makeText(context, R.string.message_messageerror_details_unavailable, Toast.LENGTH_SHORT).show();
				}
				
				return true;
			});
		}
		
		private static void displayErrorDialog(Context context, String details) {
			//Creating the view
			View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_simplescroll, null);
			TextView textView = dialogView.findViewById(R.id.text);
			textView.setTypeface(Typeface.MONOSPACE);
			textView.setText(details);
			
			//Showing the dialog
			new AlertDialog.Builder(context)
					.setTitle(R.string.message_messageerror_details_title)
					.setView(dialogView)
					.setNeutralButton(R.string.action_copytoclipboard, (dialog, which) -> {
						ClipboardManager clipboard = (ClipboardManager) MainApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
						clipboard.setPrimaryClip(ClipData.newPlainText("Error details", details));
						Toast.makeText(MainApplication.getInstance(), R.string.message_textcopied, Toast.LENGTH_SHORT).show();
						dialog.dismiss();
					})
					.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
					.create().show();
		}
		
		void animateGhostStateChanges() {
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder == null) return;
			
			viewHolder.containerMessagePart.setAlpha(ghostAlpha);
			viewHolder.containerMessagePart.animate().alpha(1).start();
		}
		
		private void restoreUploadState(ViewHolder viewHolder) {
			//Checking if there is an upload in progress
			if(isSending) {
				//Showing the progress bar
				viewHolder.progressSend.setVisibility(View.VISIBLE);
				
				//Configuring the progress view
				if(sendProgress == -1) viewHolder.progressSend.spin();
				else viewHolder.progressSend.setInstantProgress(sendProgress);
			} else {
				//Hiding the progress bar
				viewHolder.progressSend.setVisibility(View.GONE);
			}
		}
		
		String getSendStyle() {
			return sendStyle;
		}
		
		boolean getSendStyleViewed() {
			return sendStyleViewed;
		}
		
		void playEffect() {
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder == null) playEffectRequested = true;
			else playEffect(viewHolder);
		}
		
		void playEffect(ViewHolder viewHolder) {
			//Returning if there is no playable effect
			if(sendStyle == null || (!Constants.validateBubbleEffect(sendStyle) && !Constants.validateScreenEffect(sendStyle))) return;
			
			switch(sendStyle) {
				default:
					//Playing the screen effect
					viewHolder.containerMessagePart.post(() -> getConversationInfo().requestScreenEffect(sendStyle, viewHolder.containerMessagePart));
					break;
				case Constants.appleSendStyleBubbleSlam: {
					viewHolder.containerMessagePart.post(() -> {
						//Animating the message part container
						viewHolder.containerMessagePart.setRotation(Constants.getRandom().nextFloat() * 15F * 2F - 15F);
						viewHolder.containerMessagePart.setScaleX(3);
						viewHolder.containerMessagePart.setScaleY(3);
						viewHolder.containerMessagePart.setAlpha(0.0F);
						viewHolder.containerMessagePart.setPivotX(viewHolder.containerMessagePart.getWidth() / 2);
						viewHolder.containerMessagePart.setPivotY(viewHolder.containerMessagePart.getHeight() / 2);
						viewHolder.containerMessagePart.setTranslationY(Constants.dpToPx(5));
						viewHolder.containerMessagePart.animate()
								.alpha(1)
								.setInterpolator(new AccelerateInterpolator())
								.setDuration(400)
								.start();
						viewHolder.containerMessagePart.animate()
								.rotation(0)
								.translationY(0)
								.setInterpolator(new AccelerateDecelerateInterpolator())
								.setDuration(1000)
								.start();
						viewHolder.containerMessagePart.animate()
								.scaleX(1)
								.scaleY(1)
								//.setInterpolator(new AccelerateInterpolator(1.1F))
								.setInterpolator(new BounceInterpolator())
								.setDuration(1000)
								.start();
					});
					
					break;
				}
				case Constants.appleSendStyleBubbleLoud: {
					viewHolder.containerMessagePart.post(() -> {
						viewHolder.containerMessagePart.setPivotX(isOutgoing() ? viewHolder.containerMessagePart.getWidth() : 0);
						viewHolder.containerMessagePart.setPivotY(viewHolder.containerMessagePart.getHeight() / 2);
						ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
						animator.setDuration(2 * 1000);
						animator.addUpdateListener(animation -> {
							float val = (float) animation.getAnimatedValue();
							float rotationValue = (float) (Math.sin(20F * Math.PI * val + Math.PI * -0.5F) * Math.sin(Math.PI * val));
							float scaleValue;
							if(val < 0.8F) scaleValue = (float) (Math.cos((val * (1F / 0.8F) + 1) * Math.PI) / 2F) + 0.5F;
							else scaleValue = (float) (Math.cos(((val - 0.8F) * 5F) * Math.PI) / 2F) + 0.5F;
							
							//Getting the view holder
							ViewHolder newViewHolder = getViewHolder();
							if(newViewHolder == null) {
								animator.cancel();
								viewHolder.containerMessagePart.setRotation(0);
								viewHolder.containerMessagePart.setScaleX(1);
								viewHolder.containerMessagePart.setScaleY(1);
								return;
							}
							
							//Applying the transformations
							newViewHolder.containerMessagePart.setRotation(rotationValue * 5);
							newViewHolder.containerMessagePart.setScaleX(Constants.interpolate(1, 2, scaleValue));
							newViewHolder.containerMessagePart.setScaleY(Constants.interpolate(1, 2, scaleValue));
						});
						animator.start();
					});
					
					break;
				}
				case Constants.appleSendStyleBubbleGentle: {
					viewHolder.containerMessagePart.post(() -> {
						//Animating the message part container
						viewHolder.containerMessagePart.setPivotX(isOutgoing() ? viewHolder.containerMessagePart.getWidth() : 0);
						viewHolder.containerMessagePart.setPivotY(viewHolder.containerMessagePart.getHeight() / 2);
						viewHolder.containerMessagePart.setScaleX(0.6F);
						viewHolder.containerMessagePart.setScaleY(0.6F);
						viewHolder.containerMessagePart.animate()
								.setStartDelay(1500)
								.scaleX(1)
								.scaleY(1)
								.setInterpolator(new AccelerateDecelerateInterpolator())
								.setDuration(2 * 1000)
								.start();
					});
					
					break;
				}
			}
		}
		
		void setSendStyleViewed(boolean value) {
			sendStyleViewed = value;
		}
		
		@Override
		void getSummary(Context context, Constants.ResultCallback<String> callback) {
			//Converting the attachment list to a string resource list
			ArrayList<Integer> attachmentStringRes = new ArrayList<>();
			for(AttachmentInfo attachment : attachments) attachmentStringRes.add(getNameFromContentType(attachment.getContentType()));
			
			//Returning the summary
			callback.onResult(false, getSummary(context, isOutgoing(), getMessageText(), sendStyle, attachmentStringRes));
		}
		
		String getSummary(Context context) {
			//Converting the attachment list to a string resource list
			ArrayList<Integer> attachmentStringRes = new ArrayList<>();
			for(AttachmentInfo attachment : attachments)
				attachmentStringRes.add(getNameFromContentType(attachment.getContentType()));
			
			//Returning the result of the static method
			return getSummary(context, isOutgoing(), getMessageText(), sendStyle, attachmentStringRes);
		}
		
		static String getSummary(Context context, boolean isFromMe, String messageText, String sendStyle, List<Integer> attachmentStringRes) {
			//Creating the message variable
			String message;
			
			//Applying invisible ink
			if(Constants.appleSendStyleBubbleInvisibleInk.equals(sendStyle)) message = context.getString(R.string.message_messageeffect_invisibleink);
			//Otherwise assigning the message to the message text (without line breaks)
			else if(messageText != null) message = messageText.replace('\n', ' ');
			//Setting the attachments if there are attachments
			else if(attachmentStringRes.size() == 1) message = context.getResources().getString(attachmentStringRes.get(0));
			else if(attachmentStringRes.size() > 1) message = context.getResources().getQuantityString(R.plurals.message_multipleattachments, attachmentStringRes.size(), attachmentStringRes.size());
			//Otherwise setting the message to "unknown"
			else message = context.getResources().getString(R.string.part_unknown);
			
			//Returning the string with the message
			if(isFromMe) return context.getString(R.string.prefix_you, message);
			else return message;
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
		
		@Override
		int getItemViewType() {
			return itemViewType;
		}
		
		@Override
		void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
			getSummary(context, (wasTasked, result) -> callback.onResult(wasTasked, new LightConversationItem(result, getDate(), getLocalID(), getServerID())));
		}
		
		@Override
		LightConversationItem toLightConversationItemSync(Context context) {
			//Converting the attachment list to a string resource list
			List<Integer> attachmentStringRes = new ArrayList<>();
			for(AttachmentInfo attachment : attachments)
				attachmentStringRes.add(getNameFromContentType(attachment.getContentType()));
			
			//Returning the summary
			return new LightConversationItem(getSummary(context, isOutgoing(), getMessageText(), sendStyle, attachmentStringRes), getDate(), getLocalID(), getServerID());
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
		
		void addLiveSticker(StickerInfo sticker, Context context) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(sticker.messageIndex);
			if(component == null) return;
			
			component.addLiveSticker(sticker, context);
		}
		
		void addLiveTapback(TapbackInfo tapback, Context context) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(tapback.messageIndex);
			if(component == null) return;
			
			component.addLiveTapback(tapback, context);
		}
		
		void addTapback(TapbackInfo tapback) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(tapback.messageIndex);
			if(component == null) return;
			
			component.addTapback(tapback);
		}
		
		void removeLiveTapback(String sender, int messageIndex, Context context) {
			//Removing the tapback from the item
			MessageComponent component = getComponentAtIndex(messageIndex);
			if(component == null) return;
			
			component.removeLiveTapback(sender, context);
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
		
		List<StickerInfo> getStickers() {
			List<StickerInfo> list = new ArrayList<>();
			if(messageText != null) list.addAll(messageText.getStickers());
			for(AttachmentInfo item : attachments) list.addAll(item.getStickers());
			return list;
		}
		
		List<TapbackInfo> getTapbacks() {
			List<TapbackInfo> list = new ArrayList<>();
			if(messageText != null) list.addAll(messageText.getTapbacks());
			for(AttachmentInfo item : attachments) list.addAll(item.getTapbacks());
			return list;
		}
		
		void notifyPause() {
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) viewHolder.pause();
		}
		
		void notifyResume() {
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) viewHolder.resume();
		}
		
		static class ViewHolder extends RecyclerView.ViewHolder {
			final TextView labelTimeDivider;
			final TextView labelSender;
			
			ViewStub profileStub;
			ViewGroup profileGroup = null;
			ImageView profileDefault = null;
			ImageView profileImage = null;
			
			//final ViewGroup containerContent;
			final ViewGroup containerMessagePart;
			
			//final View spaceContent;
			
			final TextSwitcher labelActivityStatus;
			final View buttonSendEffectReplay;
			
			final ProgressWheel progressSend;
			final ImageButton buttonSendError;
			
			final List<MessageComponent.ViewHolder> messageComponents = new ArrayList<>();
			
			private Messaging.MessageListRecyclerAdapter.PoolSource poolSource = null;
			
			ViewHolder(View view) {
				super(view);
				
				labelTimeDivider = view.findViewById(R.id.timedivider);
				labelSender = view.findViewById(R.id.sender);
				
				profileStub = view.findViewById(R.id.stub_profile);
				if(profileStub == null) {
					profileDefault = view.findViewById(R.id.profile_default);
					profileImage = view.findViewById(R.id.profile_image);
				}
				
				//containerContent = view.findViewById(R.id.content_container);
				containerMessagePart = view.findViewById(R.id.messagepart_container);
				
				//spaceContent = view.findViewById(R.id.space_content);
				
				labelActivityStatus = view.findViewById(R.id.activitystatus);
				buttonSendEffectReplay = view.findViewById(R.id.sendeffect_replay);
				
				progressSend = view.findViewById(R.id.send_progress);
				buttonSendError = view.findViewById(R.id.send_error);
			}
			
			private void inflateProfile() {
				if(profileGroup != null) return;
				
				profileGroup = (ViewGroup) profileStub.inflate();
				profileStub = null;
				
				profileDefault = profileGroup.findViewById(R.id.profile_default);
				profileImage = profileGroup.findViewById(R.id.profile_image);
			}
			
			Messaging.MessageListRecyclerAdapter.PoolSource getRemovePoolSource() {
				Messaging.MessageListRecyclerAdapter.PoolSource currentPoolSource = poolSource;
				poolSource = null;
				return currentPoolSource;
			}
			
			void setPoolSource(Messaging.MessageListRecyclerAdapter.PoolSource poolSource) {
				this.poolSource = poolSource;
			}
			
			void pause() {
				for(MessageComponent.ViewHolder holder : messageComponents) holder.pause();
			}
			
			void resume() {
				for(MessageComponent.ViewHolder holder : messageComponents) holder.resume();
			}
		}
		
		private static class ReadErrorMessageTask extends AsyncTask<Void, Void, String> {
			private final long messageID;
			private final WeakReference<MessageInfo> messageReference;
			
			private ReadErrorMessageTask(MessageInfo message) {
				//Getting the message ID
				messageID = message.getLocalID();
				
				//Setting the reference
				messageReference = new WeakReference<>(message);
			}
			
			@Override
			protected String doInBackground(Void... voids) {
				//Fetching the error message
				return DatabaseManager.getInstance().getMessageErrorDetails(messageID);
			}
			
			@Override
			protected void onPostExecute(String errorDetail) {
				if(errorDetail == null) {
					//Notifying the user via a toast
					Toast.makeText(MainApplication.getInstance(), R.string.message_messageerror_details_loadfailed, Toast.LENGTH_SHORT).show();
				} else {
					//Updating the message
					MessageInfo message = messageReference.get();
					if(message != null) {
						message.setErrorDetails(errorDetail);
						MessageInfo.displayErrorDialog(MainApplication.getInstance(), errorDetail);
					}
				}
			}
		}
	}
	
	static abstract class MessageComponent<VH extends MessageComponent.ViewHolder> {
		//Creating the data values
		long localID;
		String guid;
		MessageInfo messageInfo;
		
		private Constants.ViewHolderSource<VH> viewHolderSource;
		
		//Creating the modifier values
		final List<StickerInfo> stickers;
		final List<TapbackInfo> tapbacks;
		
		//Creating the state values
		boolean contextMenuOpen = false;
		
		//Creating the other values
		private static int nextItemViewType = 0;
		
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
		
		void setViewHolderSource(Constants.ViewHolderSource<VH> viewHolderSource) {
			this.viewHolderSource = viewHolderSource;
		}
		
		VH getViewHolder() {
			if(viewHolderSource == null) return null;
			return viewHolderSource.get();
		}
		
		static int getNextItemViewType() {
			return nextItemViewType++;
		}
		
		abstract int getItemViewType();
		
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
		
		void setMessageInfo(MessageInfo messageInfo) {
			this.messageInfo = messageInfo;
		}
		
		MessageInfo getMessageInfo() {
			return messageInfo;
		}
		
		abstract void bindView(VH viewHolder, Context context);
		
		void buildCommonViews(VH viewHolder, Context context) {
			//Building the sticker view
			buildStickerView(viewHolder, context);
			
			//Building the tapback view
			buildTapbackView(viewHolder, context);
		}
		
		abstract void updateViewColor(VH viewHolder, Context context);
		
		abstract void updateViewEdges(VH viewHolder, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored);
		
		List<StickerInfo> getStickers() {
			return stickers;
		}
		
		List<TapbackInfo> getTapbacks() {
			return tapbacks;
		}
		
		void buildStickerView(VH viewHolder, Context context) {
			//Clearing all previous stickers
			viewHolder.stickerContainer.removeAllViews();
			
			//Weakly referencing the context
			final WeakReference<Context> contextReference = new WeakReference<>(context);
			
			//Iterating over the stickers
			for(StickerInfo sticker : stickers) {
				//Decoding the sticker
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromDBSticker(sticker.guid, sticker.localID, new BitmapCacheHelper.ImageDecodeResult() {
					@Override
					void onImageMeasured(int width, int height) {}
					
					@Override
					void onImageDecoded(Bitmap result, boolean wasTasked) {
						//Returning if the bitmap is invalid
						if(result == null) return;
						
						//Getting the view holder
						ViewHolder holder = wasTasked ? getViewHolder() : viewHolder;
						if(holder == null) return;
						
						//Getting the context
						Context context = contextReference.get();
						if(context == null) return;
						
						//Determining the maximum image size
						DisplayMetrics displayMetrics = new DisplayMetrics();
						((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
						int maxStickerSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) / 3; //One third of the smaller side of the display
						
						//Creating the image view
						ImageView imageView = new ImageView(context);
						RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
						imageView.setLayoutParams(layoutParams);
						imageView.setMaxWidth(maxStickerSize);
						imageView.setMaxHeight(maxStickerSize);
						imageView.setImageBitmap(result);
						
						//Adding the view to the sticker container
						holder.stickerContainer.addView(imageView);
						
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
		
		void addLiveSticker(StickerInfo sticker, Context context) {
			//Adding the sticker to the sticker list
			stickers.add(sticker);
			
			//Creating a weak reference to the context
			final WeakReference<Context> contextReference = new WeakReference<>(context);
			
			//Decoding the sticker
			MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromDBSticker(sticker.guid, sticker.localID, new BitmapCacheHelper.ImageDecodeResult() {
				@Override
				void onImageMeasured(int width, int height) {}
				
				@Override
				void onImageDecoded(Bitmap result, boolean wasTasked) {
					//Getting the view
					VH viewHolder = getViewHolder();
					if(viewHolder == null) return;
					
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					//Determining the maximum image size
					DisplayMetrics displayMetrics = new DisplayMetrics();
					((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
					int maxStickerSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) / 3; //One third of the smaller side of the display
					
					//Creating the image view
					ImageView imageView = new ImageView(context);
					RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
					imageView.setLayoutParams(layoutParams);
					imageView.setMaxWidth(maxStickerSize);
					imageView.setMaxHeight(maxStickerSize);
					imageView.setImageBitmap(result);
					
					//Adding the view to the sticker container
					viewHolder.stickerContainer.addView(imageView);
					
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
			//Getting the view holder
			VH viewHolder = getViewHolder();
			if(viewHolder == null) return;
			
			//Checking if the stickers should be shown
			if(getRequiredStickerVisibility()) {
				//Showing the stickers
				for(int i = 0; i < viewHolder.stickerContainer.getChildCount(); i++) {
					View stickerView = viewHolder.stickerContainer.getChildAt(i);
					stickerView.setVisibility(View.VISIBLE);
					stickerView.animate().alpha(1).start();
				}
			} else {
				//Hiding the stickers
				for(int i = 0; i < viewHolder.stickerContainer.getChildCount(); i++) {
					View stickerView = viewHolder.stickerContainer.getChildAt(i);
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
		
		void addLiveTapback(TapbackInfo tapback, Context context) {
			//Adding the tapback
			addTapback(tapback);
			
			//Rebuilding the tapback view
			VH viewHolder = getViewHolder();
			if(viewHolder != null) buildTapbackView(viewHolder, context);
		}
		
		void removeTapback(String sender) {
			//Removing the first matching tapback
			for(Iterator<TapbackInfo> iterator = tapbacks.iterator(); iterator.hasNext();) if(Objects.equals(sender, iterator.next().sender)) {
				iterator.remove();
				break;
			}
		}
		
		void removeLiveTapback(String sender, Context context) {
			//Removing the tapback
			removeTapback(sender);
			
			//Rebuilding the tapback view
			VH viewHolder = getViewHolder();
			if(viewHolder != null) buildTapbackView(viewHolder, context);
		}
		
		void buildTapbackView(VH viewHolder, Context context) {
			//Emptying the tapback container
			viewHolder.tapbackContainer.removeAllViews();
			
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
				View tapbackView = LayoutInflater.from(viewHolder.itemView.getContext()).inflate(R.layout.chip_tapback, viewHolder.tapbackContainer, false);
				
				//Getting the display info
				TapbackInfo.TapbackDisplay displayInfo = TapbackInfo.getTapbackDisplay(entry.getKey(), context);
				
				//Getting the count text
				TextView count = tapbackView.findViewById(R.id.label_count);
				
				//Setting the count
				count.setText(String.format(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? context.getResources().getConfiguration().getLocales().get(0) : context.getResources().getConfiguration().locale, "%d", entry.getValue()));
				
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
				viewHolder.tapbackContainer.addView(tapbackView);
			}
		}
		
		abstract VH createViewHolder(Context context, ViewGroup parent);
		
		static abstract class ViewHolder extends RecyclerView.ViewHolder {
			final ViewGroup groupContainer;
			
			final ViewGroup stickerContainer;
			final ViewGroup tapbackContainer;
			
			ViewHolder(View view) {
				super(view);
				
				groupContainer = view.findViewById(R.id.container);
				
				stickerContainer = view.findViewById(R.id.sticker_container);
				tapbackContainer = view.findViewById(R.id.tapback_container);
			}
			
			void releaseResources() {}
			
			void cleanupState() {}
			
			void pause() {}
			
			void resume() {}
		}
	}
	
	static class MessageTextInfo extends MessageComponent<MessageTextInfo.ViewHolder> {
		//Creating the reference values
		static final int itemViewType = MessageComponent.getNextItemViewType();
		
		//Creating the component values
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
		void bindView(ViewHolder viewHolder, Context context) {
			//Setting the text
			viewHolder.labelMessage.setText(messageText);
			
			{
				//Setting the alignment
				//viewHolder.itemView.setForegroundGravity(getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START);
				((LinearLayout.LayoutParams) viewHolder.itemView.getLayoutParams()).gravity = (getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START);
				//FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) viewHolder.groupContainer.getLayoutParams();
				//params.gravity = (getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
				
				/* RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) viewHolder.groupContainer.getLayoutParams();
				if(getMessageInfo().isOutgoing()) {
					params.removeRule(RelativeLayout.ALIGN_PARENT_START);
					params.addRule(RelativeLayout.ALIGN_PARENT_END);
				} else {
					params.removeRule(RelativeLayout.ALIGN_PARENT_END);
					params.addRule(RelativeLayout.ALIGN_PARENT_START);
				} */
				
				/* ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) viewHolder.groupContainer.getLayoutParams();
				if(getMessageInfo().isOutgoing()) {
					params.startToStart = ConstraintLayout.LayoutParams.UNSET;
					params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
				} else {
					params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
					params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
				} */
			}
			//((RelativeLayout) viewHolder.itemView).setGravity(getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START);
			
			//Inflating and adding the text content
			//setupTextLinks(viewHolder.labelMessage);
			
			//Updating the view color
			updateViewColor(viewHolder, context);
			
			//Assigning the interaction listeners
			//assignInteractionListeners(viewHolder);
			assignInteractionListenersLegacy(viewHolder.labelMessage);
			
			//Getting the maximum content width
			//int maxContentWidth = (int) Math.min(context.getResources().getDimensionPixelSize(R.dimen.contentwidth_max) * .7F, context.getResources().getDisplayMetrics().widthPixels * .7F);
			
			//Enforcing the maximum content width
			/* View contentView = convertView.findViewById(R.id.content);
			contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			if(contentView.getMeasuredWidth() > maxContentWidth) contentView.getLayoutParams().width = maxContentWidth; */
			
			viewHolder.labelMessage.setMaxWidth(getMaxMessageWidth(context.getResources()));
			/* viewHolder.labelMessage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			if(viewHolder.labelMessage.getMeasuredWidth() > maxContentWidth) viewHolder.labelMessage.getLayoutParams().width = maxContentWidth;
			else viewHolder.labelMessage.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT; */
			
			//Building the common views
			buildCommonViews(viewHolder, context);
			
			//Setting up the message effects
			if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) {
				viewHolder.inkView.setVisibility(View.VISIBLE);
				viewHolder.inkView.setState(true);
			}
			else viewHolder.inkView.setVisibility(View.GONE);
		}
		
		private void setupTextLinks(TextView textView) {
			//Setting up the URL checker
			textView.setTransformationMethod(new Constants.CustomTabsLinkTransformationMethod(0xFFFFFFFF));
			textView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		/* @SuppressLint("ClickableViewAccessibility")
		private void assignInteractionListeners(ViewHolder viewHolder) {
			//Setting the long click listener
			viewHolder.content.setOnLongClickListener(clickedView -> {
				//Getting the context
				Context context = clickedView.getContext();
				
				//Returning if the view is not an activity
				//if(!(context instanceof Activity)) return false;
				
				//Displaying the context menu
				displayContextMenu(context, clickedView);
				
				//Disabling link clicks
				ViewHolder newViewHolder = getViewHolder();
				if(newViewHolder != null) newViewHolder.labelMessage.setLinksClickable(false);
				
				//Returning
				return true;
			});
			
			//Setting the touch listener
			viewHolder.content.setOnTouchListener((View view, MotionEvent event) -> {
				ViewHolder newViewHolder = getViewHolder();
				if(newViewHolder != null) {
					if(event.getAction() == MotionEvent.ACTION_DOWN) newViewHolder.inkView.reveal();
					else if(event.getAction() == MotionEvent.ACTION_UP) new Handler(Looper.getMainLooper()).postDelayed(() -> newViewHolder.labelMessage.setLinksClickable(true), 0);
				}
				
				return view.onTouchEvent(event);
			});
		} */
		
		@SuppressLint("ClickableViewAccessibility")
		private void assignInteractionListenersLegacy(TextView textView) {
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
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder != null) newViewHolder.inkView.reveal();
				} else if(event.getAction() == MotionEvent.ACTION_UP) {
					new Handler(Looper.getMainLooper()).postDelayed(() -> ((TextView) view).setLinksClickable(true), 0);
				}
				
				return view.onTouchEvent(event);
			});
		}
		
		@Override
		void updateViewColor(ViewHolder viewHolder, Context context) {
			//Getting the colors
			int backgroundColor;
			int textColor;
			
			if(getMessageInfo().isOutgoing()) {
				if(Preferences.checkPreferenceAdvancedColor(context)) {
					backgroundColor = context.getResources().getColor(R.color.colorMessageOutgoing, null);
					textColor = Constants.resolveColorAttr(context, android.R.attr.textColorPrimary);
				} else {
					backgroundColor = context.getResources().getColor(R.color.colorPrimary, null);
					textColor = context.getResources().getColor(R.color.colorTextWhite, null);
				}
			} else {
				if(Preferences.checkPreferenceAdvancedColor(context)) {
					MemberInfo memberInfo = getMessageInfo().getConversationInfo().findConversationMember(getMessageInfo().getSender());
					int targetColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
					//textColor = context.getResources().getColor(R.color.colorTextWhite, null);
					textColor = ColorHelper.modifyColorRaw(targetColor, Constants.isNightMode(context.getResources()) ? 1.3F : 0.75F);
					backgroundColor = Color.argb(80, Color.red(targetColor), Color.green(targetColor), Color.blue(targetColor));
				} else {
					backgroundColor = context.getResources().getColor(R.color.colorMessageOutgoing, null);
					textColor = Constants.resolveColorAttr(context, android.R.attr.textColorPrimary);
				}
			}
			
			//Assigning the colors
			viewHolder.labelMessage.setTextColor(textColor);
			viewHolder.labelMessage.setLinkTextColor(textColor);
			viewHolder.labelMessage.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
			
			viewHolder.inkView.setBackgroundColor(backgroundColor);
			
			//Setting up the text links (to update the toolbar color in Chrome's custom tabs)
			setupTextLinks(viewHolder.labelMessage);
		}
		
		@Override
		void updateViewEdges(ViewHolder viewHolder, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Updating the text view's background
			viewHolder.labelMessage.setBackground(Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored));
			
			int radiusTop = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
			int radiusBottom = anchoredBottom ? pxCornerAnchored : pxCornerUnanchored;
			
			if(alignToRight) viewHolder.inkView.setRadii(pxCornerUnanchored, radiusTop, radiusBottom, pxCornerUnanchored);
			else viewHolder.inkView.setRadii(radiusTop, pxCornerUnanchored, pxCornerUnanchored, radiusBottom);
		}
		
		@Override
		int getItemViewType() {
			return itemViewType;
		}
		
		private void displayContextMenu(Context context, View targetView) {
			//Creating a new popup menu
			PopupMenu popupMenu = new PopupMenu(context, targetView);
			
			//Inflating the menu
			popupMenu.inflate(R.menu.menu_conversationitem_contextual);
			
			//Removing the delete file option
			Menu menu = popupMenu.getMenu();
			menu.removeItem(R.id.action_deletedata);
			
			//Creating the context reference
			WeakReference<Context> contextReference = new WeakReference<>(context);
			
			//Setting the click listener
			popupMenu.setOnMenuItemClickListener(menuItem -> {
				//Getting the context
				Context newContext = contextReference.get();
				if(newContext == null) return false;
				
				switch(menuItem.getItemId()) {
					case R.id.action_details: {
						Date sentDate = new Date(getMessageInfo().getDate());
						
						//Building the message
						StringBuilder stringBuilder = new StringBuilder();
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_type, newContext.getResources().getString(R.string.part_content_text))).append('\n'); //Message type
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_sender, getMessageInfo().getSender() != null ? getMessageInfo().getSender() : newContext.getResources().getString(R.string.you))).append('\n'); //Sender
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_datesent, DateFormat.getTimeFormat(newContext).format(sentDate) + Constants.bulletSeparator + DateFormat.getLongDateFormat(newContext).format(sentDate))).append('\n'); //Time sent
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_sendeffect, getMessageInfo().getSendStyle() == null ? newContext.getResources().getString(R.string.part_none) : getMessageInfo().getSendStyle())); //Send effect
						
						//Showing a dialog
						new AlertDialog.Builder(newContext)
								.setTitle(R.string.message_messagedetails_title)
								.setMessage(stringBuilder.toString())
								.create()
								.show();
						
						//Returning true
						return true;
					}
					case R.id.action_copytext: {
						//Getting the clipboard manager
						ClipboardManager clipboardManager = (ClipboardManager) newContext.getSystemService(Context.CLIPBOARD_SERVICE);
						
						//Applying the clip data
						clipboardManager.setPrimaryClip(ClipData.newPlainText("message", messageText));
						
						//Showing a confirmation toast
						Toast.makeText(newContext, R.string.message_textcopied, Toast.LENGTH_SHORT).show();
						
						//Returning true
						return true;
					}
					case R.id.action_share: {
						//Starting the intent immediately if the user is "you"
						if(getMessageInfo().getSender() == null)
							shareMessageText(newContext, getMessageInfo().getDate(), null, messageText);
							//Requesting the user info
						else MainApplication.getInstance().getUserCacheHelper().getUserInfo(newContext, getMessageInfo().getSender(), new UserCacheHelper.UserFetchResult() {
							@Override
							void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
								//Starting the intent
								shareMessageText(newContext, getMessageInfo().getDate(), userInfo == null ? getMessageInfo().getSender() : userInfo.getContactName(), messageText);
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
			//DateFormat dateFormat = DateFormat.getDateFormat(context); //android.text.format.DateFormat.getLongDateFormat(activity);
			
			//Getting the text
			String text = name == null ?
					context.getResources().getString(R.string.message_shareable_text_you, DateFormat.getLongDateFormat(context).format(date), DateFormat.getTimeFormat(context).format(date), message) :
					context.getResources().getString(R.string.message_shareable_text, DateFormat.getLongDateFormat(context).format(date), DateFormat.getTimeFormat(context).format(date), name, message);
			
			//Setting the text
			intent.putExtra(Intent.EXTRA_TEXT, text);
			
			//Setting the intent type
			intent.setType("text/plain");
			
			//Starting the intent
			context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.action_sharemessage)));
		}
		
		List<StickerInfo> getStickers() {
			return stickers;
		}
		
		List<TapbackInfo> getTapbacks() {
			return tapbacks;
		}
		
		@Override
		ViewHolder createViewHolder(Context context, ViewGroup parent) {
			return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contenttext, parent, false));
		}
		
		static class ViewHolder extends MessageComponent.ViewHolder {
			final ViewGroup content;
			final TextView labelMessage;
			final InvisibleInkView inkView;
			
			ViewHolder(View view) {
				super(view);
				
				content = view.findViewById(R.id.content);
				labelMessage = content.findViewById(R.id.message);
				inkView = content.findViewById(R.id.content_ink);
			}
			
			@Override
			void cleanupState() {
				inkView.setState(false);
			}
		}
	}
	
	static abstract class AttachmentInfo<VH extends AttachmentInfo.ViewHolder> extends MessageComponent<VH> {
		//Creating the values
		final String fileName;
		final String fileType;
		final long fileSize;
		File file = null;
		byte[] fileChecksum = null;
		Uri fileUri = null;
		ConnectionService.FilePushRequest draftingPushRequest = null;
		
		//Creating the attachment request values
		boolean isFetching = false;
		boolean isFetchWaiting = false;
		float fetchProgress = 0;
		
		//Creating the listener values
		private final ConnectionService.FileDownloadRequestCallbacks fileDownloadRequestCallbacks = new ConnectionService.FileDownloadRequestCallbacks() {
			@Override
			public void onResponseReceived() {
				//Setting the fetch as not waiting (a response was received from the server)
				isFetchWaiting = false;
				
				//Getting the view holder
				VH viewHolder = getViewHolder();
				if(viewHolder == null) return;
				
				//Setting the progress bar as determinate
				viewHolder.progressDownload.setProgress(0);
				viewHolder.progressDownload.setIndeterminate(false);
			}
			
			@Override
			public void onStart() {}
			
			@Override
			public void onProgress(float progress) {
				//Getting the view holder
				VH viewHolder = getViewHolder();
				if(viewHolder == null) return;
				
				//Updating the progress bar's progress
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) viewHolder.progressDownload.setProgress((int) (progress * viewHolder.progressDownload.getMax()), true);
				else viewHolder.progressDownload.setProgress((int) (progress * viewHolder.progressDownload.getMax()));
			}
			
			@Override
			public void onFinish(File file) {
				//Setting the attachment as not fetching
				isFetching = false;
				
				//Setting the file in memory
				AttachmentInfo.this.file = file;
				
				//Getting the view holder
				VH viewHolder = getViewHolder();
				if(viewHolder == null) return;
				
				//Rebuilding the view
				buildView(viewHolder, viewHolder.itemView.getContext());
				
				//Swapping to the content view
				/* view.findViewById(R.id.downloadcontent).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.VISIBLE); */
			}
			
			@Override
			public void onFail() {
				//Setting the attachment as not fetching
				isFetching = false;
				isFetchWaiting = false;
				
				//Getting the view holder
				VH viewHolder = getViewHolder();
				if(viewHolder == null) return;
				
				//Rebuilding the view
				buildView(viewHolder, viewHolder.itemView.getContext());
			}
		};
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
			//Calling the super constructor
			super(localID, guid, message);
			
			//Setting the values
			this.fileName = fileName;
			this.fileType = fileType;
			this.fileSize = fileSize;
		}
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
			//Calling the main constructor
			this(localID, guid, message, fileName, fileType, fileSize);
			
			//Setting the file
			if(file != null && file.exists()) this.file = file;
		}
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
			//Calling the main constructor
			this(localID, guid, message, fileName, fileType, fileSize);
			
			//Setting the checksum
			this.fileChecksum = fileChecksum;
		}
		
		AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
			//Calling the main constructor
			this(localID, guid, message, fileName, fileType, fileSize);
			
			//Setting the uri
			this.fileUri = fileUri;
		}
		
		abstract void updateContentView(VH viewHolder, Context context);
		
		abstract void onClick(Messaging activity);
		
		/**
		 * Binds the view to the view holder
		 * @param viewHolder The view holder to bind
		 * @param context The context to be used in the creation of the view
		 *
		 * Be sure to call the super() method when overriding!
		 */
		@Override
		void bindView(VH viewHolder, Context context) {
			//Getting the attachment request data
			ConnectionService connectionService = ConnectionService.getInstance();
			if(connectionService != null) {
				ConnectionService.FileDownloadRequest.ProgressStruct progress = connectionService.updateDownloadRequestAttachment(localID, fileDownloadRequestCallbacks);
				if(progress != null) {
					isFetching = true;
					isFetchWaiting = progress.isWaiting;
					fetchProgress = progress.progress;
				}
			}
			
			{
				//Setting the alignment
				//viewHolder.itemView.setForegroundGravity(getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START);
				((LinearLayout.LayoutParams) viewHolder.itemView.getLayoutParams()).gravity = (getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START);
				/* RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) viewHolder.groupContainer.getLayoutParams();
				if(getMessageInfo().isOutgoing()) {
					params.removeRule(RelativeLayout.ALIGN_PARENT_START);
					params.addRule(RelativeLayout.ALIGN_PARENT_END);
				} else {
					params.removeRule(RelativeLayout.ALIGN_PARENT_END);
					params.addRule(RelativeLayout.ALIGN_PARENT_START);
				} */
			}
			
			//((RelativeLayout) viewHolder.itemView).setGravity(messageInfo.isOutgoing() ? Gravity.END : Gravity.START);
			/* LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) viewHolder.itemView.getLayoutParams();
			layoutParams.gravity = messageInfo.isOutgoing() ? Gravity.END : Gravity.START;
			viewHolder.itemView.setLayoutParams(layoutParams); */
			
			/* //Configuring the download view
			viewHolder.labelDownloadType.setText(getResourceTypeName());
			if(fileSize == -1) viewHolder.labelDownloadSize.setVisibility(View.GONE);
			else {
				viewHolder.labelDownloadSize.setVisibility(View.GONE);
				viewHolder.labelDownloadSize.setText(Constants.humanReadableByteCount(fileSize, true));
			} */
			
			//Building the view
			buildView(viewHolder, context);
			
			//Updating the view color
			updateViewColor(viewHolder, context);
			
			//Assigning the interaction listeners
			assignInteractionListeners(viewHolder.itemView);
			
			//Building the common views
			buildCommonViews(viewHolder, context);
		}
		
		/**
		 * Builds the attachment's view based on the state
		 * Switches between the 4 view types, and passes the content view building on to the subclass if necessary
		 * @param viewHolder The attachment's view holder
		 */
		private void buildView(VH viewHolder, Context context) {
			//Checking if there is no file
			if(file == null) {
				//Checking if the attachment is being fetched
				if(isFetching) {
					//Showing the download content view
					viewHolder.groupDownload.setVisibility(View.VISIBLE);
					viewHolder.groupContent.setVisibility(View.GONE);
					if(viewHolder.groupFailed != null) viewHolder.groupFailed.setVisibility(View.GONE);
					viewHolder.groupProcessing.setVisibility(View.GONE);
					
					//Hiding the content type
					viewHolder.labelDownloadSize.setVisibility(View.GONE);
					viewHolder.labelDownloadType.setVisibility(View.GONE);
					
					//Disabling the download icon visually
					viewHolder.imageDownload.setAlpha(Constants.disabledAlpha);
					
					//Getting and preparing the progress bar
					viewHolder.progressDownload.setIndeterminate(isFetchWaiting);
					viewHolder.progressDownload.setProgress((int) (fetchProgress * viewHolder.progressDownload.getMax()));
					viewHolder.progressDownload.setVisibility(View.VISIBLE);
				}
				//Otherwise checking if the attachment is being uploaded
				else if(messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeGhost || messageInfo.isSending) {
					//Showing the processing view
					viewHolder.groupDownload.setVisibility(View.GONE);
					viewHolder.groupContent.setVisibility(View.GONE);
					if(viewHolder.groupFailed != null) viewHolder.groupFailed.setVisibility(View.GONE);
					viewHolder.groupProcessing.setVisibility(View.VISIBLE);
				} else {
					//Showing the standard download content view
					viewHolder.groupDownload.setVisibility(View.VISIBLE);
					viewHolder.groupContent.setVisibility(View.GONE);
					if(viewHolder.groupFailed != null) viewHolder.groupFailed.setVisibility(View.GONE);
					viewHolder.groupProcessing.setVisibility(View.GONE);
					
					if(fileSize != -1) viewHolder.labelDownloadSize.setVisibility(View.VISIBLE);
					viewHolder.labelDownloadType.setVisibility(View.VISIBLE);
					viewHolder.imageDownload.setAlpha(1F);
					viewHolder.progressDownload.setVisibility(View.GONE);
					
					//Configuring the download view
					viewHolder.labelDownloadType.setText(getResourceTypeName());
					if(fileSize == -1) viewHolder.labelDownloadSize.setVisibility(View.GONE);
					else {
						viewHolder.labelDownloadSize.setVisibility(View.VISIBLE);
						viewHolder.labelDownloadSize.setText(Formatter.formatShortFileSize(context, fileSize));
					}
				}
			} else {
				//Hiding the views
				viewHolder.groupDownload.setVisibility(View.GONE);
				viewHolder.groupContent.setVisibility(View.GONE);
				if(viewHolder.groupFailed != null) viewHolder.groupFailed.setVisibility(View.GONE);
				viewHolder.groupProcessing.setVisibility(View.GONE);
				
				//Setting up the content view
				updateContentView(viewHolder, context);
			}
		}
		
		//TODO: Check what exactly is going on here?
		void onScrollShow() {
			//VH viewHolder = getViewHolder();
			//Context context = viewHolder.itemView.getContext();
			
			//Building the view
			//buildView(viewHolder, context);
			
			//Building the common views
			//buildCommonViews(viewHolder, context);
		}
		
		@Override
		void updateViewEdges(VH viewHolder, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Creating the drawable
			Drawable drawable = Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored);
			
			//Assigning the drawable
			viewHolder.groupDownload.setBackground(drawable);
			viewHolder.groupProcessing.setBackground(drawable);
			if(viewHolder.groupFailed != null) viewHolder.groupFailed.setBackground(drawable);
			
			//Updating the content view's edges
			updateContentViewEdges(viewHolder, drawable, anchoredTop, anchoredBottom, alignToRight, pxCornerAnchored, pxCornerUnanchored);
		}
		
		void updateContentViewEdges(VH viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {}
		
		@Override
		void updateViewColor(VH viewHolder, Context context) {
			//Creating the color values
			ColorStateList cslText;
			ColorStateList cslSecondaryText;
			ColorStateList cslBackground;
			ColorStateList cslAccent;
			
			//Getting the colors
			if(messageInfo.isOutgoing()) {
				if(Preferences.checkPreferenceAdvancedColor(context)) {
					cslText = ColorStateList.valueOf(Constants.resolveColorAttr(context, android.R.attr.textColorPrimary));
					cslSecondaryText = ColorStateList.valueOf(Constants.resolveColorAttr(context, android.R.attr.textColorSecondary));
					cslBackground = ColorStateList.valueOf(context.getResources().getColor(R.color.colorMessageOutgoing, null));
					cslAccent = ColorStateList.valueOf(context.getResources().getColor(R.color.colorMessageOutgoingAccent, null));
				} else {
					cslText = ColorStateList.valueOf(context.getResources().getColor(R.color.colorTextWhite, null));
					cslSecondaryText = ColorStateList.valueOf(context.getResources().getColor(R.color.colorTextWhiteSecondary, null));
					cslBackground = ColorStateList.valueOf(context.getResources().getColor(R.color.colorPrimary, null));
					cslAccent = ColorStateList.valueOf(context.getResources().getColor(R.color.colorPrimaryLight, null));
				}
			} else {
				if(Preferences.checkPreferenceAdvancedColor(context)) {
					MemberInfo memberInfo = messageInfo.getConversationInfo().findConversationMember(messageInfo.getSender());
					int bubbleColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
					
					cslText = ColorStateList.valueOf(context.getResources().getColor(R.color.colorTextWhite, null));
					cslSecondaryText = ColorStateList.valueOf(context.getResources().getColor(R.color.colorTextWhiteSecondary, null));
					cslBackground = ColorStateList.valueOf(bubbleColor);
					cslAccent = ColorStateList.valueOf(ColorHelper.lightenColor(bubbleColor));
				} else {
					cslText = ColorStateList.valueOf(Constants.resolveColorAttr(context, android.R.attr.textColorPrimary));
					cslSecondaryText = ColorStateList.valueOf(Constants.resolveColorAttr(context, android.R.attr.textColorSecondary));
					cslBackground = ColorStateList.valueOf(context.getResources().getColor(R.color.colorMessageOutgoing, null));
					cslAccent = ColorStateList.valueOf(context.getResources().getColor(R.color.colorMessageOutgoingAccent, null));
				}
			}
			
			//Coloring the views
			viewHolder.groupDownload.setBackgroundTintList(cslBackground);
			viewHolder.groupDownload.invalidate();
			viewHolder.labelDownloadSize.setTextColor(cslSecondaryText);
			viewHolder.labelDownloadType.setTextColor(cslText);
			viewHolder.imageDownload.setImageTintList(cslText);
			viewHolder.progressDownload.setProgressTintList(cslText);
			//viewHolder.progressDownload.setSecondaryProgressTintList(cslAccent);
			viewHolder.progressDownload.setIndeterminateTintList(ColorStateList.valueOf(ColorHelper.modifyColorRaw(cslBackground.getDefaultColor(), 0.9F)));
			viewHolder.progressDownload.setProgressBackgroundTintList(ColorStateList.valueOf(ColorHelper.modifyColorRaw(cslBackground.getDefaultColor(), 0.9F)));
			
			viewHolder.groupProcessing.setBackgroundTintList(cslBackground);
			viewHolder.labelProcessing.setTextColor(cslText);
			viewHolder.labelProcessing.setCompoundDrawableTintList(cslText);
			
			if(viewHolder.groupFailed != null) {
				viewHolder.groupFailed.setBackgroundTintList(cslBackground);
				viewHolder.labelFailed.setTextColor(cslText);
				viewHolder.labelFailed.setCompoundDrawableTintList(cslText);
			}
			
			//Updating the content view color
			updateContentViewColor(viewHolder, context, cslText, cslBackground, cslAccent);
		}
		
		void updateContentViewColor(VH viewHolder, Context context, ColorStateList cslText, ColorStateList cslBackground, ColorStateList cslAccent) {}
		
		void downloadContent(Context context) {
			//Returning if the content has already been fetched is being fetched, or the message is in a ghost state
			if(file != null || isFetching || messageInfo.getMessageState() == SharedValues.MessageInfo.stateCodeGhost) return;
			
			//Checking if the service isn't running
			ConnectionService connectionService = ConnectionService.getInstance();
			if(connectionService == null || connectionService.getCurrentState() != ConnectionService.stateConnected) {
				//Showing a toast
				Toast.makeText(context, R.string.message_connectionerrror, Toast.LENGTH_SHORT).show();
				
				//Returning
				return;
			}
			
			//Making a download request
			boolean result = connectionService.addDownloadRequest(fileDownloadRequestCallbacks, localID, guid, fileName);
			
			//Returning if the request couldn't be placed
			if(!result) return;
			
			//Updating the fetch state
			isFetching = true;
			isFetchWaiting = true;
			
			//Rebuilding the view
			VH viewHolder = getViewHolder();
			if(viewHolder != null) buildView(viewHolder, context);
		}
		
		void discardFile(Context context) {
			//Returning if there is no file
			if(file == null) return;
			
			//Invalidating the file
			file = null;
			
			//Rebuilding the view
			VH viewHolder = getViewHolder();
			if(viewHolder != null) buildView(viewHolder, context);
			
			/* //Getting the view
			VH viewHolder = getViewHolder();
			if(viewHolder != null) {
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
			} */
		}
		
		private void assignInteractionListeners(View view) {
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
		
		private void displayContextMenu(View view, Context context) {
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
			
			//Creating a weak reference to the context
			WeakReference<Context> contextReference = new WeakReference<>(context);
			
			//Setting the click listener
			popupMenu.setOnMenuItemClickListener(menuItem -> {
				//Getting the context
				Context newContext = contextReference.get();
				if(newContext == null) return false;
				
				switch(menuItem.getItemId()) {
					case R.id.action_details: {
						Date sentDate = new Date(getMessageInfo().getDate());
						
						//Building the message
						StringBuilder stringBuilder = new StringBuilder();
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_type, newContext.getResources().getString(getNameFromContentType(getContentType())))).append('\n'); //Message type
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_sender, messageInfo.getSender() != null ? messageInfo.getSender() : newContext.getResources().getString(R.string.you))).append('\n'); //Sender
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_datesent, DateFormat.getTimeFormat(newContext).format(sentDate) + Constants.bulletSeparator + DateFormat.getLongDateFormat(newContext).format(sentDate))).append('\n'); //Time sent
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_size, fileSize != -1 ? Formatter.formatFileSize(newContext, fileSize) : newContext.getResources().getString(R.string.part_nodata))).append('\n'); //Attachment file size
						stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_sendeffect, getMessageInfo().getSendStyle() == null ? newContext.getResources().getString(R.string.part_none) : getMessageInfo().getSendStyle())); //Send effect
						
						//Showing a dialog
						new AlertDialog.Builder(newContext)
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
						Uri content = FileProvider.getUriForFile(newContext, MainApplication.fileAuthority, file);
						
						//Setting the intent file
						intent.putExtra(Intent.EXTRA_STREAM, content);
						
						//Getting the mime type
						String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(substringStart));
						
						//Setting the type
						intent.setType(mimeType);
						
						//Starting the activity
						newContext.startActivity(Intent.createChooser(intent, newContext.getResources().getText(R.string.action_sharemessage)));
						
						//Returning true
						return true;
					}
					case R.id.action_deletedata: {
						//Deleting the attachment
						new AttachmentDeleter(newContext, file, localID).execute();
						
						//Discarding the file in memory
						discardFile(newContext);
						
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
				//noinspection ResultOfMethodCallIgnored
				file.delete();
				
				//Updating the database entry
				Context context = contextReference.get();
				if(context != null) DatabaseManager.getInstance().invalidateAttachment(localID);
				
				//Returning
				return null;
			}
		}
		
		byte[] getFileChecksum() {
			return fileChecksum;
		}
		
		void setFileChecksum(byte[] fileChecksum) {
			this.fileChecksum = fileChecksum;
		}
		
		String getContentType() {
			return fileType;
		}
		
		abstract int getResourceTypeName();
		
		ConnectionService.FilePushRequest getDraftingPushRequest() {
			return draftingPushRequest;
		}
		
		void setDraftingPushRequest(ConnectionService.FilePushRequest draftingPushRequest) {
			this.draftingPushRequest = draftingPushRequest;
		}
		
		//abstract ViewHolder createViewHolder(Context context, ViewGroup parent);
		
		static abstract class ViewHolder extends MessageComponent.ViewHolder {
			final ViewGroup groupDownload;
			final ProgressBar progressDownload;
			final TextView labelDownloadSize;
			final TextView labelDownloadType;
			final ImageView imageDownload;
			
			final ViewGroup groupContent;
			
			final ViewGroup groupFailed; //This field can be null in some cases (other attachment info), so always do validity checks before using it
			final TextView labelFailed;
			
			final ViewGroup groupProcessing;
			final TextView labelProcessing;
			
			ViewHolder(View view) {
				super(view);
				
				groupDownload = view.findViewById(R.id.downloadcontent);
				progressDownload = groupDownload.findViewById(R.id.download_progress);
				labelDownloadSize = groupDownload.findViewById(R.id.label_size);
				labelDownloadType = groupDownload.findViewById(R.id.label_type);
				imageDownload = groupDownload.findViewById(R.id.download_icon);
				
				groupContent = view.findViewById(R.id.content);
				
				groupFailed = view.findViewById(R.id.failedcontent);
				labelFailed = groupFailed == null ? null : groupFailed.findViewById(R.id.failed_label);
				
				groupProcessing = view.findViewById(R.id.processingcontent);
				labelProcessing = groupProcessing.findViewById(R.id.processing_label);
			}
		}
		
		static String getRelativePath(Context context, File file) {
			return MainApplication.getAttachmentDirectory(context).toURI().relativize(file.toURI()).getPath();
		}
		
		static File getAbsolutePath(Context context, String path) {
			return Paths.get(MainApplication.getAttachmentDirectory(context).getPath()).resolve(path).toFile();
		}
	}
	
	static class ImageAttachmentInfo extends AttachmentInfo<ImageAttachmentInfo.ViewHolder> {
		//Creating the reference values
		static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
		static final String MIME_PREFIX = "image";
		static final int RESOURCE_NAME = R.string.part_content_image;
		
		ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
			super(localID, guid, message, fileName, fileType, fileSize);
		}
		
		ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
			super(localID, guid, message, fileName, fileType, fileSize, file);
		}
		
		ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
			super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
		}
		
		ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
			super(localID, guid, message, fileName, fileType, fileSize, fileUri);
		}
		
		/* @Override
		View createView(Context context, View convertView, ViewGroup parent) {
			//Calling the super method
			super.createView(context, convertView, parent);
			
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
		} */
		
		@Override
		void updateContentView(ViewHolder viewHolder, Context context) {
			/* //Configuring the content view
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
			((ImageView) content.findViewById(R.id.content_view)).setImageBitmap(null); */
			//int pxBitmapSizeMax = (int) context.getResources().getDimension(R.dimen.image_size_max);
			
			//Requesting a Glide image load
			if(Constants.validateContext(context)) {
				viewHolder.imageContent.layout(0, 0, 0, 0);
				RequestBuilder<Drawable> requestBuilder = Glide.with(context)
						.load(file)
						.transition(DrawableTransitionOptions.withCrossFade());
						//.apply(RequestOptions.placeholderOf(new ColorDrawable(context.getResources().getColor(R.color.colorImageUnloaded, null))));
				if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) requestBuilder.apply(RequestOptions.bitmapTransform(new BlurTransformation(invisibleInkBlurRadius, invisibleInkBlurSampling)));
				requestBuilder.into(viewHolder.imageContent);
			}
			
			//Updating the ink view
			if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) {
				viewHolder.inkView.setVisibility(View.VISIBLE);
				viewHolder.inkView.setState(true);
			} else viewHolder.inkView.setVisibility(View.GONE);
			
			//Revealing the layout
			viewHolder.groupContent.setVisibility(View.VISIBLE);
			
			/*//Creating a weak reference to the context
			WeakReference<Context> contextReference = new WeakReference<>(context);
			
			//Checking if the image is a GIF
			if("image/gif".equals(fileType)) {
				MainApplication.getInstance().getBitmapCacheHelper().measureBitmapFromImageFile(file.getPath(), file, new BitmapCacheHelper.ImageDecodeResult() {
					@Override
					public void onImageMeasured(int width, int height) {
						//Getting the context
						Context context = contextReference.get();
						if(context == null) return;
						
						//Getting the view holder
						ViewHolder newViewHolder = getViewHolder();
						if(newViewHolder == null) return;
						
						//Getting the multiplier
						float multiplier = Constants.calculateImageAttachmentMultiplier(context.getResources(), width, height);
						
						//Configuring the layout
						newViewHolder.groupContent.getLayoutParams().width = (int) (width * multiplier);
						newViewHolder.groupContent.getLayoutParams().height = (int) (height * multiplier);
						
						//Showing the layout
						newViewHolder.groupContent.setVisibility(View.VISIBLE);
						
						try {
							//Configuring the animated image
							newViewHolder.imageContent.setImageDrawable(new GifDrawable(file));
						} catch(IOException exception) {
							//Logging the exception
							exception.printStackTrace();
							Crashlytics.logException(exception);
						}
						
						//Updating the ink view
						if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) {
							viewHolder.inkView.setVisibility(View.VISIBLE);
							viewHolder.inkView.setState(true);
						}
						else viewHolder.inkView.setVisibility(View.GONE);
					}
				}, true, pxBitmapSizeMax, pxBitmapSizeMax);
			} else {
				MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromImageFile(file.getPath(), file, new BitmapCacheHelper.ImageDecodeResult() {
					@Override
					public void onImageMeasured(int width, int height) {
						//Getting the context
						Context context = contextReference.get();
						if(context == null) return;
						
						//Getting the view holder
						ViewHolder newViewHolder = getViewHolder();
						if(newViewHolder == null) return;
						
						//Getting the multiplier
						float multiplier = Constants.calculateImageAttachmentMultiplier(context.getResources(), width, height);
						
						//Configuring the layout
						newViewHolder.groupContent.getLayoutParams().width = (int) (width * multiplier);
						newViewHolder.groupContent.getLayoutParams().height = (int) (height * multiplier);
						
						//Showing the layout
						newViewHolder.groupContent.setVisibility(View.VISIBLE);
					}
					
					@Override
					public void onImageDecoded(Bitmap bitmap, boolean wasTasked) {
						//Getting the context
						Context context = contextReference.get();
						if(context == null) return;
						
						//Getting the view holder
						ViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
						if(newViewHolder == null) return;
						
						//Checking if the bitmap is invalid
						if(bitmap == null) {
							//Showing the failed view
							newViewHolder.groupContent.setVisibility(View.GONE);
							newViewHolder.groupFailed.setVisibility(View.VISIBLE);
						} else {
							//Configuring the layout
							//ViewGroup.LayoutParams params = content.getLayoutParams();
							
							float multiplier = Constants.calculateImageAttachmentMultiplier(context.getResources(), bitmap.getWidth(), bitmap.getHeight());
							newViewHolder.groupContent.getLayoutParams().width = (int) (bitmap.getWidth() * multiplier);
							newViewHolder.groupContent.getLayoutParams().height = (int) (bitmap.getHeight() * multiplier);
							//content.setLayoutParams(params);
							
							//Showing the layout
							newViewHolder.groupContent.setVisibility(View.VISIBLE);
							
							//Setting the bitmap
							newViewHolder.imageContent.setImageBitmap(bitmap);
							
							//Updating the view
							if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) {
								viewHolder.inkView.setVisibility(View.VISIBLE);
								viewHolder.inkView.setState(true);
							}
							else viewHolder.inkView.setVisibility(View.GONE);
							
							//Fading in the view
							if(wasTasked) {
								newViewHolder.imageContent.setAlpha(0F);
								newViewHolder.imageContent.animate().alpha(1).setDuration(300).start();
							}
							
						}
					}
				}, true, pxBitmapSizeMax, pxBitmapSizeMax);
			} */
		}
		
		@Override
		void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Assigning the drawable
			//viewHolder.backgroundContent.setBackground(drawable.getConstantState().newDrawable());
			
			int radiusTop = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
			int radiusBottom = anchoredBottom ? pxCornerAnchored : pxCornerUnanchored;
			PaintDrawable backgroundDrawable = new PaintDrawable();
			
			if(alignToRight) {
				viewHolder.imageContent.setRadii(pxCornerUnanchored, radiusTop, radiusBottom, pxCornerUnanchored);
				viewHolder.inkView.setRadii(pxCornerUnanchored, radiusTop, radiusBottom, pxCornerUnanchored);
				backgroundDrawable.setCornerRadii(new float[]{pxCornerUnanchored, pxCornerUnanchored, radiusTop, radiusTop, radiusBottom, radiusBottom, pxCornerUnanchored, pxCornerUnanchored});
			} else {
				viewHolder.imageContent.setRadii(radiusTop, pxCornerUnanchored, pxCornerUnanchored, radiusBottom);
				viewHolder.inkView.setRadii(radiusTop, pxCornerUnanchored, pxCornerUnanchored, radiusBottom);
				backgroundDrawable.setCornerRadii(new float[]{radiusTop, radiusTop, pxCornerUnanchored, pxCornerUnanchored, pxCornerUnanchored, pxCornerUnanchored, radiusBottom, radiusBottom});
			}
			viewHolder.backgroundView.setBackground(backgroundDrawable);
		}
		
		@Override
		void onClick(Messaging activity) {
			//Returning if there is no content
			if(file == null) return;
			
			//Getting the view holder
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder == null) return;
			
			//Revealing the ink view (and checking if is already running a reveal)
			//if(viewHolder.inkView.getVisibility() != View.VISIBLE || viewHolder.inkView.reveal()) {
			//}
			
			//Getting the file extension
				/* String fileName = file.getName();
				int substringStart = file.getName().lastIndexOf(".") + 1;
				if(fileName.length() <= substringStart) return; */
			
			//Getting the file mime type
			//String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(substringStart));
			
			//Creating a content URI
			Uri content = FileProvider.getUriForFile(activity, MainApplication.fileAuthority, file);
			
			//Launching the content viewer
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(content, fileType);
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			if(intent.resolveActivity(activity.getPackageManager()) != null) activity.startActivity(intent);
			else Toast.makeText(activity, R.string.message_intenterror_open, Toast.LENGTH_SHORT).show();
		}
		
		@Override
		int getItemViewType() {
			return ITEM_VIEW_TYPE;
		}
		
		@Override
		int getResourceTypeName() {
			return RESOURCE_NAME;
		}
		
		@Override
		ViewHolder createViewHolder(Context context, ViewGroup parent) {
			return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contentimage, parent, false));
		}
		
		static class ViewHolder extends AttachmentInfo.ViewHolder {
			final RoundedImageView imageContent;
			final InvisibleInkView inkView;
			final View backgroundView;
			
			ViewHolder(View view) {
				super(view);
				
				imageContent = groupContent.findViewById(R.id.content_view);
				inkView = groupContent.findViewById(R.id.content_ink);
				backgroundView = groupContent.findViewById(R.id.content_background);
			}
			
			@Override
			void cleanupState() {
				inkView.setState(false);
			}
			
			@Override
			void pause() {
				//inkView.onPause();
			}
			
			@Override
			void resume() {
				//inkView.onResume();
			}
			
			@Override
			void releaseResources() {
				imageContent.setImageBitmap(null);
			}
		}
	}
	
	static class AudioAttachmentInfo extends AttachmentInfo<AudioAttachmentInfo.ViewHolder> {
		//Creating the reference values
		static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
		static final String MIME_PREFIX = "audio";
		static final int RESOURCE_NAME = R.string.part_content_audio;
		
		private static final int resDrawablePlay = R.drawable.play_rounded;
		private static final int resDrawablePause = R.drawable.pause_rounded;
		
		private static final byte fileStateIdle = 0;
		private static final byte fileStateLoading = 1;
		private static final byte fileStateLoaded = 2;
		private static final byte fileStateFailed = 3;
		
		//Creating the media values
		private long duration = 0;
		private long mediaProgress = 0;
		private byte fileState = fileStateIdle;
		private boolean isPlaying = false;
		
		AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
			super(localID, guid, message, fileName, fileType, fileSize);
		}
		
		AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
			super(localID, guid, message, fileName, fileType, fileSize, file);
		}
		
		AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
			super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
		}
		
		AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
			super(localID, guid, message, fileName, fileType, fileSize, fileUri);
		}
		
		/* @Override
		View createView(Context context, View convertView, ViewGroup parent) {
			//Calling the super method
			super.createView(context, convertView, parent);
			
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
		} */
		
		@Override
		void updateContentViewColor(ViewHolder viewHolder, Context context, ColorStateList cslText, ColorStateList cslBackground, ColorStateList cslAccent) {
			viewHolder.groupContent.setBackgroundTintList(cslBackground);
			viewHolder.contentIcon.setImageTintList(cslText);
			viewHolder.contentLabel.setTextColor(cslText);
			viewHolder.contentProgress.setProgressTintList(cslText);
			//viewHolder.contentProgress.setBackgroundTintList(cslText); //cslBackground
			viewHolder.contentProgress.setProgressBackgroundTintList(ColorStateList.valueOf(ColorHelper.modifyColorRaw(cslBackground.getDefaultColor(), 0.9F))); //cslBackground //TODO possibly use less lazy and hacky method
		}
		
		@Override
		void updateContentView(ViewHolder viewHolder, Context context) {
			//Loading the file data if the state is idle
			if(file != null && fileState == fileStateIdle) {
				new GetDurationTask(this).execute(file);
				fileState = fileStateLoading;
			}
			
			//Checking if the file state is invalid
			if(fileState == fileStateFailed) {
				//Showing the failed view
				viewHolder.groupFailed.setVisibility(View.VISIBLE);
			} else {
				//Showing the content view
				viewHolder.groupContent.setVisibility(View.VISIBLE);
				
				//Checking if the state is playing or the file is ready
				if(isPlaying || fileState == fileStateLoaded) {
					//Updating the media
					updateMediaPlaying(viewHolder);
					updateMediaProgress(viewHolder);
				} else resetPlaying(viewHolder);
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
				return Constants.getMediaDuration(parameters[0]);
			}
			
			@Override
			protected void onPostExecute(Long result) {
				//Getting the attachment
				AudioAttachmentInfo attachmentInfo = attachmentReference.get();
				if(attachmentInfo == null) return;
				
				//Setting the duration
				attachmentInfo.fileState = fileStateLoaded;
				attachmentInfo.duration = result;
				attachmentInfo.updateMediaProgress();
			}
		}
		
		@Override
		void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Assigning the drawable
			viewHolder.groupContent.setBackground(drawable.getConstantState().newDrawable());
		}
		
		@Override
		void onClick(Messaging activity) {
			//Returning if there is no content
			if(file == null) return;
			
			//Getting the activity callbacks
			ConversationInfo.ActivityCallbacks callbacks = getMessageInfo().getConversationInfo().activityCallbacks;
			if(callbacks == null) return;
			
			//Getting the audio message manager
			Messaging.AudioPlaybackManager audioPlaybackManager = callbacks.getAudioPlaybackManager();
			
			//Checking if the request ID matches
			if(audioPlaybackManager.compareRequestID(Messaging.AudioPlaybackManager.requestTypeAttachment + localID)) {
				//Toggling play
				audioPlaybackManager.togglePlaying();
				
				//Returning
				return;
			}
			
			//Preparing the media player
			audioPlaybackManager.play(Messaging.AudioPlaybackManager.requestTypeAttachment + localID, file, new Messaging.AudioPlaybackManager.Callbacks() {
				@Override
				public void onPlay() {
					setMediaPlaying(true);
				}
				
				@Override
				public void onProgress(long time) {
					setMediaProgress(time);
				}
				
				@Override
				public void onPause() {
					setMediaPlaying(false);
				}
				
				@Override
				public void onStop() {
					ViewHolder viewHolder = getViewHolder();
					if(viewHolder != null) {
						setMediaPlaying(viewHolder, false);
						setMediaProgress(viewHolder, 0);
					}
				}
			});
		}
		
		@Override
		int getItemViewType() {
			return ITEM_VIEW_TYPE;
		}
		
		@Override
		int getResourceTypeName() {
			return RESOURCE_NAME;
		}
		
		void setMediaPlaying(boolean playing) {
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) setMediaPlaying(viewHolder, playing);
		}
		
		private void setMediaPlaying(ViewHolder viewholder, boolean playing) {
			isPlaying = playing;
			updateMediaPlaying(viewholder);
		}
		
		private void updateMediaPlaying(ViewHolder viewHolder) {
			viewHolder.contentIcon.setImageResource(isPlaying ?
					resDrawablePause :
					resDrawablePlay);
		}
		
		void setMediaProgress(long progress) {
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) setMediaProgress(viewHolder, progress);
		}
		
		private void setMediaProgress(ViewHolder viewHolder, long progress) {
			mediaProgress = progress;
			updateMediaProgress(viewHolder);
		}
		
		void updateMediaProgress() {
			//Calling the overload method
			ViewHolder viewHolder = getViewHolder();
			if(viewHolder != null) updateMediaProgress(viewHolder);
		}
		
		private void updateMediaProgress(ViewHolder viewHolder) {
			viewHolder.contentProgress.setProgress((int) ((float) mediaProgress / (float) duration * 100F));
			viewHolder.contentLabel.setText(DateUtils.formatElapsedTime(((int) Math.floor(mediaProgress <= 0 ? duration / 1000L : mediaProgress / 1000L))));
		}
		
		private void resetPlaying(ViewHolder viewHolder) {
			setMediaPlaying(viewHolder, false);
			setMediaProgress(viewHolder, 0);
		}
		
		@Override
		ViewHolder createViewHolder(Context context, ViewGroup parent) {
			return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contentaudio, parent, false));
		}
		
		static class ViewHolder extends AttachmentInfo.ViewHolder {
			final ImageView contentIcon;
			final TextView contentLabel;
			final ProgressBar contentProgress;
			
			ViewHolder(View view) {
				super(view);
				
				contentIcon = groupContent.findViewById(R.id.content_icon);
				contentLabel = groupContent.findViewById(R.id.content_duration);
				contentProgress = groupContent.findViewById(R.id.content_progress);
			}
			
			@Override
			void releaseResources() {
				contentIcon.setImageResource(resDrawablePlay);
				contentProgress.setProgress(0);
				contentLabel.setText(DateUtils.formatElapsedTime(0));
			}
		}
	}
	
	static class VideoAttachmentInfo extends AttachmentInfo<VideoAttachmentInfo.ViewHolder> {
		//Creating the reference values
		static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
		static final String MIME_PREFIX = "video";
		static final int RESOURCE_NAME = R.string.part_content_video;
		
		VideoAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
			super(localID, guid, message, fileName, fileType, fileSize);
		}
		
		VideoAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
			super(localID, guid, message, fileName, fileType, fileSize, file);
		}
		
		VideoAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
			super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
		}
		
		VideoAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
			super(localID, guid, message, fileName, fileType, fileSize, fileUri);
		}
		
		@Override
		void updateContentView(ViewHolder viewHolder, Context context) {
			/* //Configuring the content view
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
			((ImageView) content.findViewById(R.id.content_view)).setImageBitmap(null); */
			
			//Requesting a Glide image load
			if(Constants.validateContext(context)) {
				viewHolder.imageContent.layout(0, 0, 0, 0);
				RequestBuilder<Drawable> requestBuilder = Glide.with(context)
						.load(file)
						.transition(DrawableTransitionOptions.withCrossFade());
						//.apply(RequestOptions.placeholderOf(new ColorDrawable(context.getResources().getColor(R.color.colorImageUnloaded, null))));
				if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) requestBuilder.apply(RequestOptions.bitmapTransform(new BlurTransformation(invisibleInkBlurRadius, invisibleInkBlurSampling)));
				
				requestBuilder.into(viewHolder.imageContent);
			}
			
			//Updating the ink view
			if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) {
				viewHolder.inkView.setVisibility(View.VISIBLE);
				viewHolder.inkView.setState(true);
			} else viewHolder.inkView.setVisibility(View.GONE);
			
			//Revealing the layout
			viewHolder.groupContent.setVisibility(View.VISIBLE);
			
			/* //Creating a weak reference to the context
			WeakReference<Context> contextReference = new WeakReference<>(context);
			
			MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromVideoFile(file.getPath(), file, new BitmapCacheHelper.ImageDecodeResult() {
				@Override
				public void onImageMeasured(int width, int height) {
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					//Getting the view holder
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					
					//Getting the multiplier
					float multiplier = Constants.calculateImageAttachmentMultiplier(context.getResources(), width, height);
					
					//Configuring the layout
					newViewHolder.groupContent.getLayoutParams().width = (int) (width * multiplier);
					newViewHolder.groupContent.getLayoutParams().height = (int) (height * multiplier);
					
					//Showing the layout
					newViewHolder.groupContent.setVisibility(View.VISIBLE);
				}
				
				@Override
				public void onImageDecoded(Bitmap bitmap, boolean wasTasked) {
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					//Getting the view holder
					ViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
					if(newViewHolder == null) return;
					
					//Checking if the bitmap is invalid
					if(bitmap == null) {
						//Showing the failed view
						newViewHolder.groupContent.setVisibility(View.GONE);
						newViewHolder.groupFailed.setVisibility(View.VISIBLE);
					} else {
						//Configuring the layout
						//ViewGroup.LayoutParams params = content.getLayoutParams();
						
						float multiplier = Constants.calculateImageAttachmentMultiplier(context.getResources(), bitmap.getWidth(), bitmap.getHeight());
						newViewHolder.groupContent.getLayoutParams().width = (int) (bitmap.getWidth() * multiplier);
						newViewHolder.groupContent.getLayoutParams().height = (int) (bitmap.getHeight() * multiplier);
						//content.setLayoutParams(params);
						
						//Showing the layout
						newViewHolder.groupContent.setVisibility(View.VISIBLE);
						
						//Setting the bitmap
						newViewHolder.imageContent.setImageBitmap(bitmap);
						
						//Fading in the view
						if(wasTasked) {
							newViewHolder.imageContent.setAlpha(0F);
							newViewHolder.imageContent.animate().alpha(1).setDuration(300).start();
						}
					}
				}
			}); */
		}
		
		@Override
		void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Assigning the drawable
			//viewHolder.backgroundContent.setBackground(drawable.getConstantState().newDrawable());
			
			int radiusTop = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
			int radiusBottom = anchoredBottom ? pxCornerAnchored : pxCornerUnanchored;
			PaintDrawable backgroundDrawable = new PaintDrawable();
			
			if(alignToRight) {
				viewHolder.imageContent.setRadii(pxCornerUnanchored, radiusTop, radiusBottom, pxCornerUnanchored);
				viewHolder.inkView.setRadii(pxCornerUnanchored, radiusTop, radiusBottom, pxCornerUnanchored);
				backgroundDrawable.setCornerRadii(new float[]{pxCornerUnanchored, pxCornerUnanchored, radiusTop, radiusTop, radiusBottom, radiusBottom, pxCornerUnanchored, pxCornerUnanchored});
			} else {
				viewHolder.imageContent.setRadii(radiusTop, pxCornerUnanchored, pxCornerUnanchored, radiusBottom);
				viewHolder.inkView.setRadii(radiusTop, pxCornerUnanchored, pxCornerUnanchored, radiusBottom);
				backgroundDrawable.setCornerRadii(new float[]{radiusTop, radiusTop, pxCornerUnanchored, pxCornerUnanchored, pxCornerUnanchored, pxCornerUnanchored, radiusBottom, radiusBottom});
			}
			viewHolder.backgroundView.setBackground(backgroundDrawable);
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
		
		@Override
		int getItemViewType() {
			return ITEM_VIEW_TYPE;
		}
		
		@Override
		int getResourceTypeName() {
			return RESOURCE_NAME;
		}
		
		@Override
		ViewHolder createViewHolder(Context context, ViewGroup parent) {
			return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contentvideo, parent, false));
		}
		
		static class ViewHolder extends AttachmentInfo.ViewHolder {
			final RoundedImageView imageContent;
			final InvisibleInkView inkView;
			final View backgroundView;
			
			ViewHolder(View view) {
				super(view);
				
				imageContent = groupContent.findViewById(R.id.content_view);
				inkView = groupContent.findViewById(R.id.content_ink);
				backgroundView = groupContent.findViewById(R.id.content_background);
			}
			
			@Override
			void cleanupState() {
				inkView.setState(false);
			}
			
			@Override
			void releaseResources() {
				imageContent.setImageBitmap(null);
			}
		}
	}
	
	static class OtherAttachmentInfo extends AttachmentInfo<OtherAttachmentInfo.ViewHolder> {
		//Creating the reference values
		static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
		//static final String MIME_PREFIX = "other";
		static final int RESOURCE_NAME = R.string.part_content_other;
		
		OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
			super(localID, guid, message, fileName, fileType, fileSize);
		}
		
		OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
			super(localID, guid, message, fileName, fileType, fileSize, file);
		}
		
		OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
			super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
		}
		
		OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
			super(localID, guid, message, fileName, fileType, fileSize, fileUri);
		}
		
		@Override
		void updateContentViewColor(ViewHolder viewHolder, Context context, ColorStateList cslText, ColorStateList cslBackground, ColorStateList cslAccent) {
			viewHolder.groupContent.setBackgroundTintList(cslBackground);
			viewHolder.labelContent.setTextColor(cslText);
			viewHolder.labelContent.setCompoundDrawableTintList(cslText);
		}
		
		@Override
		void updateContentView(ViewHolder viewHolder, Context context) {
			//Enforcing the maximum content width
			viewHolder.labelContent.setMaxWidth(getMaxMessageWidth(context.getResources()));
			
			//Configuring the content view
			viewHolder.labelContent.setText(fileName);
			
			//Showing the content view
			viewHolder.groupContent.setVisibility(View.VISIBLE);
		}
		
		@Override
		void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Assigning the drawable
			viewHolder.groupContent.setBackground(drawable.getConstantState().newDrawable());
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
		
		@Override
		int getItemViewType() {
			return ITEM_VIEW_TYPE;
		}
		
		@Override
		int getResourceTypeName() {
			return RESOURCE_NAME;
		}
		
		@Override
		ViewHolder createViewHolder(Context context, ViewGroup parent) {
			return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contentother, parent, false));
		}
		
		static class ViewHolder extends AttachmentInfo.ViewHolder {
			final TextView labelContent;
			
			ViewHolder(View view) {
				super(view);
				
				labelContent = view.findViewById(R.id.content_label);
			}
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
		
		StickerInfo(long localID, String guid, long messageID, int messageIndex, String sender, long date) {
			this.localID = localID;
			this.guid = guid;
			this.messageID = messageID;
			this.messageIndex = messageIndex;
			this.sender = sender;
			this.date = date;
		}
		
		long getLocalID() {
			return localID;
		}
		
		long getMessageID() {
			return messageID;
		}
		
		void setMessageID(long value) {
			messageID = value;
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
		
		long getLocalID() {
			return localID;
		}
		
		long getMessageID() {
			return messageID;
		}
		
		void setMessageID(long value) {
			messageID = value;
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
					return new TapbackDisplay(R.drawable.love_rounded, context.getResources().getColor(R.color.tapback_love, null));
				case tapbackLike:
					return new TapbackDisplay(R.drawable.like_rounded, context.getResources().getColor(R.color.tapback_like, null));
				case tapbackDislike:
					return new TapbackDisplay(R.drawable.dislike_rounded, context.getResources().getColor(R.color.tapback_dislike, null));
				case tapbackLaugh:
					return new TapbackDisplay(R.drawable.excited_rounded, context.getResources().getColor(R.color.tapback_laugh, null));
				case tapbackEmphasis:
					return new TapbackDisplay(R.drawable.exclamation_rounded, context.getResources().getColor(R.color.tapback_exclamation, null));
				case tapbackQuestion:
					return new TapbackDisplay(R.drawable.question_rounded, context.getResources().getColor(R.color.tapback_question, null));
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
	
	static class GroupActionInfo extends ConversationItem<ActionLineViewHolder> {
		//Creating the constants
		static final int itemType = 1;
		static final int itemViewType = ConversationItem.viewTypeAction;
		
		//Creating the values
		final int actionType; //0 - Invite / 1 - Leave
		final String agent;
		final String other;
		
		//Creating the other values
		public transient int color;
		
		GroupActionInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, int actionType, String agent, String other, long date) {
			//Calling the super constructor
			super(localID, serverID, guid, date, conversationInfo);
			
			//Setting the values
			this.actionType = actionType;
			this.agent = agent;
			this.other = other;
		}
		
		@Override
		void bindView(ActionLineViewHolder viewHolder, Context context) {
			//Setting the message
			viewHolder.labelMessage.setText(getDirectSummary(context, agent, other, actionType));
			if(agent != null || other != null) getSummary(context, (wasTasked, result) -> {
				ActionLineViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
				if(newViewHolder != null) newViewHolder.labelMessage.setText(result);
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
		
		@Override
		int getItemViewType() {
			return itemViewType;
		}
		
		int getActionType() {
			return actionType;
		}
		
		@Override
		void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
			getSummary(context, (wasTasked, result) -> callback.onResult(wasTasked, new LightConversationItem(result, getDate(), getLocalID(), getServerID())));
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
			return new LightConversationItem(getDirectSummary(context, titledAgent, titledOther, actionType), getDate(), getLocalID(), getServerID());
		}
	}
	
	static class ChatRenameActionInfo extends ConversationItem<ActionLineViewHolder> {
		//Creating the constants
		static final int itemType = 2;
		static final int itemViewType = ConversationItem.viewTypeAction;
		
		//Creating the values
		final String agent;
		final String title;
		
		ChatRenameActionInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, String agent, String title, long date) {
			//Calling the super constructor
			super(localID, serverID, guid, date, conversationInfo);
			
			//Setting the values
			this.agent = agent;
			this.title = title;
		}
		
		@Override
		void bindView(ActionLineViewHolder viewHolder, Context context) {
			//Setting the message
			viewHolder.labelMessage.setText(getDirectSummary(context, agent, title));
			if(agent != null) getSummary(context, (wasTasked, result) -> {
				ActionLineViewHolder newViewHolder = wasTasked ? getViewHolder() : viewHolder;
				if(newViewHolder != null) newViewHolder.labelMessage.setText(result);
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
		int getItemViewType() {
			return itemViewType;
		}
		
		@Override
		void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
			getSummary(context, (wasTasked, result) -> {
				callback.onResult(wasTasked, new LightConversationItem(result, getDate(), getLocalID(), getServerID()));
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
			return new LightConversationItem(summary, getDate(), getLocalID(), getServerID());
		}
	}
	
	static class ChatCreationMessage extends ConversationItem<ActionLineViewHolder> {
		//Creating the constants
		static final int itemType = 3;
		static final int itemViewType = ConversationItem.viewTypeAction;
		
		ChatCreationMessage(long localID, long date, ConversationInfo conversationInfo) {
			super(localID, -1, null, date, conversationInfo);
		}
		
		@Override
		void bindView(ActionLineViewHolder viewHolder, Context context) {
			//Setting the message
			viewHolder.labelMessage.setText(getDirectSummary(context));
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
		
		@Override
		int getItemViewType() {
			return itemViewType;
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
			callback.onResult(false, new LightConversationItem(getDirectSummary(context), getDate(), getLocalID(), getServerID()));
		}
		
		@Override
		LightConversationItem toLightConversationItemSync(Context context) {
			return new LightConversationItem(getDirectSummary(context), getDate(), getLocalID(), getServerID());
		}
	}
	
	static class ActionLineViewHolder extends RecyclerView.ViewHolder {
		final TextView labelMessage;
		
		ActionLineViewHolder(View view) {
			super(view);
			
			labelMessage = view.findViewById(R.id.message);
		}
	}
	
	static abstract class ConversationItem<VH> {
		//Creating the reference values
		static final int viewTypeMessage = 0;
		static final int viewTypeAction = 1;
		
		//Creating the conversation item values
		private long localID;
		private long serverID;
		private String guid;
		private long date;
		private ConversationInfo conversationInfo;
		//private Constants.ViewSource viewSource;
		private Constants.ViewHolderSource<VH> viewHolderSource;
		
		ConversationItem(long localID, long serverID, String guid, long date, ConversationInfo conversationInfo) {
			//Setting the identifiers
			this.localID = localID;
			this.serverID = serverID;
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
		
		long getServerID() {
			return serverID;
		}
		
		void setServerID(long value) {
			serverID = value;
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
		
		void setViewHolderSource(Constants.ViewHolderSource<VH> viewHolderSource) {
			this.viewHolderSource = viewHolderSource;
		}
		
		VH getViewHolder() {
			if(viewHolderSource == null) return null;
			return viewHolderSource.get();
		}
		
		ConversationInfo getConversationInfo() {
			return conversationInfo;
		}
		
		void setConversationInfo(ConversationInfo conversationInfo) {
			this.conversationInfo = conversationInfo;
		}
		
		abstract void bindView(VH viewHolder, Context context);
		
		/* static class MessageViewHolder extends RecyclerView.ViewHolder {
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
		} */
		
		void updateViewColor(Context context) {}
		
		abstract void getSummary(Context context, Constants.ResultCallback<String> resultCallback);
		
		abstract int getItemType();
		
		abstract int getItemViewType();
		
		abstract void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback);
		
		abstract LightConversationItem toLightConversationItemSync(Context context);
	}
	
	static class LightConversationItem {
		//Creating the message values
		private String message;
		private final long date;
		private final long localID;
		private final long serverID;
		private final boolean isPinned;
		
		LightConversationItem(String message, long date, long localID, long serverID) {
			//Setting the values
			this.message = message;
			this.date = date;
			this.localID = localID;
			this.serverID = serverID;
			this.isPinned = false;
		}
		
		LightConversationItem(String message, long date, boolean isPinned) {
			this.message = message;
			this.date = date;
			localID = serverID = -1;
			this.isPinned = isPinned;
		}
		
		String getMessage() {
			return message;
		}
		
		void setMessage(String message) {
			this.message = message;
		}
		
		long getDate() {
			return date;
		}
		
		long getLocalID() {
			return localID;
		}
		
		long getServerID() {
			return serverID;
		}
		
		boolean isPinned() {
			return isPinned;
		}
	}
	
	static void setupConversationItemRelations(List<ConversationItem> conversationItems, ConversationInfo conversationInfo) {
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
		boolean targetDeliveredSet = false;
		//boolean targetReadSet = false;
		for(int i = conversationItems.size() - 1; i >= 0; i--) {
			//Getting the item
			ConversationManager.ConversationItem item = conversationItems.get(i);
			
			//Skipping the remainder of the iteration if the item is not a message
			if(!(item instanceof ConversationManager.MessageInfo)) continue;
			
			//Getting the message
			ConversationManager.MessageInfo messageItem = (ConversationManager.MessageInfo) item;
			
			//Skipping the remainder of the iteration if the message is incoming
			if(!messageItem.isOutgoing()) continue;
			
			//Setting the conversation's active message state list ID
			if(!targetDeliveredSet && messageItem.getMessageState() == SharedValues.MessageInfo.stateCodeDelivered) {
				conversationInfo.setActivityStateTargetDelivered(messageItem);
				targetDeliveredSet = true;
			}
			if(/*!targetReadSet && */messageItem.getMessageState() == SharedValues.MessageInfo.stateCodeRead) {
				if(!targetDeliveredSet) conversationInfo.setActivityStateTargetDelivered(messageItem); //The delivered and read message would be the same thing
				conversationInfo.setActivityStateTargetRead(messageItem);
				//targetReadSet = true;
				break; //Break on the first instance of a read message; if no delivered message has been found, then this takes priority anyways (the delivered message will overlap with this one, or be someplace above due to an awkward update)
			}
			
			//Breaking from the loop
			//if(targetDeliveredSet && targetReadSet) break;
		}
	}
	
	static void addConversationItemRelation(ConversationInfo conversation, List<ConversationItem> conversationItems, MessageInfo messageInfo, Context context, boolean update) {
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
	
	static void removeConversationItemRelation(ConversationInfo conversation, List<ConversationItem> conversationItems, int index, Context context, boolean update) {
		//Getting the items adjacent to the message
		ConversationItem itemOlder = index > 0 ? conversationItems.get(index - 1) : null;
		MessageInfo messageOlder = itemOlder != null && itemOlder instanceof MessageInfo ? (MessageInfo) itemOlder : null;
		ConversationItem itemNewer = index < conversationItems.size() ? conversationItems.get(index) : null;
		MessageInfo messageNewer = itemNewer != null && itemNewer instanceof MessageInfo ? (MessageInfo) itemNewer : null;
		
		//Updating the items individually if there is only one of the two
		if(messageOlder == null && messageNewer == null) return;
		if(messageOlder != null && messageNewer == null) {
			//The item is at the end of the conversation (the removed message was at the bottom of the chat)
			messageOlder.setAnchoredBottom(false);
			if(update) messageOlder.updateViewEdges(Constants.isLTR(context.getResources()));
			
			//Replacing the activity state target
			conversation.tryActivityStateTarget(messageOlder, update, context);
		} else if(messageOlder == null) { //messageNewer will always be not null
			//The item is at the beginning of the conversation (the removed message was at the top of the chat)
			messageNewer.setHasTimeDivider(true);
			if(update) messageNewer.updateTimeDivider(context);
			
			messageNewer.setAnchoredTop(false);
			if(update) messageNewer.updateViewEdges(Constants.isLTR(context.getResources()));
		} else { //Both the older message and newer message are valid
			//Checking if the item is a valid anchor point (is a message and is within the burst time)
			boolean isAnchored = Math.abs(messageOlder.getDate() - messageNewer.getDate()) < ConversationManager.conversationBurstTimeMillis && Objects.equals(messageOlder.getSender(), messageNewer.getSender());
			messageOlder.setAnchoredBottom(isAnchored);
			messageNewer.setAnchoredTop(isAnchored);
			
			//Recalculating the time divider visibility
			messageNewer.setHasTimeDivider(Math.abs(messageNewer.getDate() - messageOlder.getDate()) >= ConversationManager.conversationSessionTimeMillis);
			
			//Updating the views
			if(update) {
				boolean isLTR = Constants.isLTR(context.getResources());
				messageOlder.updateViewEdges(isLTR);
				messageNewer.updateViewEdges(isLTR);
				
				messageNewer.updateTimeDivider(context);
			}
		}
	}
	
	static void addConversationItemRelations(ConversationInfo conversation, List<ConversationItem> conversationItems, List<ConversationItem> newConversationItems, Context context, boolean update) {
		int highestIndex = -1;
		
		//Iterating over the new items
		for(ConversationItem conversationItem : newConversationItems) {
			//Skipping the remainder of the iteration if the item is not a message
			if(!(conversationItem instanceof MessageInfo)) continue;
			
			//Getting the message info
			MessageInfo messageInfo = (MessageInfo) conversationItem;
			
			//Getting the item's positioning
			int index = conversationItems.indexOf(messageInfo);
			if(index > highestIndex) highestIndex = index;
			
			//Used to skip updating other items in the chunk, as they will be iterated over too
			//int chunkIndex = newConversationItems.indexOf(conversationItem);
			//boolean isOldestInChunk = chunkIndex == 0;
			//boolean isNewestInChunk = newConversationItems.indexOf(conversationItem) == newConversationItems.size() - 1;
			
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
						if(!newConversationItems.contains(adjacentItem)) ((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
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
						if(!newConversationItems.contains(adjacentItem)) ((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
					}
				}
			}
			
			//Comparing (and replacing) the activity state target if the message is the newest in the chunk
			if(newConversationItems.indexOf(conversationItem) == newConversationItems.size() - 1) conversation.tryActivityStateTarget(messageInfo, update, context);
		}
		
		//Updating the time divider of the item below the group
		if(highestIndex != -1 && conversationItems.size() > highestIndex + 1) {
			ConversationItem groupItem = conversationItems.get(highestIndex);
			ConversationItem adjacentItem = conversationItems.get(highestIndex + 1);
			
			//Checking if both items are messages
			if(groupItem instanceof MessageInfo && adjacentItem instanceof MessageInfo) {
				MessageInfo adjacentMessage = (MessageInfo) adjacentItem;
				adjacentMessage.setHasTimeDivider(Math.abs(groupItem.getDate() - adjacentItem.getDate()) >= ConversationManager.conversationSessionTimeMillis);
				if(update) adjacentMessage.updateTimeDivider(context);
			}
		}
	}
	
	static int getNameFromContentType(String mimeType) {
		//Returning the type
		if(mimeType == null || mimeType.isEmpty()) return OtherAttachmentInfo.RESOURCE_NAME;
		else if(mimeType.startsWith(ImageAttachmentInfo.MIME_PREFIX)) return ImageAttachmentInfo.RESOURCE_NAME;
		else if(mimeType.startsWith(VideoAttachmentInfo.MIME_PREFIX)) return VideoAttachmentInfo.RESOURCE_NAME;
		else if(mimeType.startsWith(AudioAttachmentInfo.MIME_PREFIX)) return AudioAttachmentInfo.RESOURCE_NAME;
		else return OtherAttachmentInfo.RESOURCE_NAME;
	}
	
	static AttachmentInfo<?> createAttachmentInfoFromType(long fileID, String fileGuid, MessageInfo messageInfo, String fileName, String fileType, long fileSize) {
		if(fileType == null) fileType = Constants.defaultMIMEType;
		if(fileType.startsWith(ImageAttachmentInfo.MIME_PREFIX)) return new ConversationManager.ImageAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		else if(fileType.startsWith(AudioAttachmentInfo.MIME_PREFIX)) return new ConversationManager.AudioAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		else if(fileType.startsWith(VideoAttachmentInfo.MIME_PREFIX)) return new ConversationManager.VideoAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		return new ConversationManager.OtherAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
	}
	
	static AttachmentInfo<?> createAttachmentInfoFromType(long fileID, String fileGuid, MessageInfo messageInfo, String fileName, String fileType, long fileSize, File file) {
		if(fileType == null) fileType = Constants.defaultMIMEType;
		if(fileType.startsWith(ImageAttachmentInfo.MIME_PREFIX)) return new ConversationManager.ImageAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		else if(fileType.startsWith(AudioAttachmentInfo.MIME_PREFIX)) return new ConversationManager.AudioAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		else if(fileType.startsWith(VideoAttachmentInfo.MIME_PREFIX)) return new ConversationManager.VideoAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		return new ConversationManager.OtherAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
	}
	
	static AttachmentInfo<?> createAttachmentInfoFromType(long fileID, String fileGuid, MessageInfo messageInfo, String fileName, String fileType, long fileSize, Uri fileUri) {
		if(fileType.startsWith(ImageAttachmentInfo.MIME_PREFIX)) return new ConversationManager.ImageAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		else if(fileType.startsWith(AudioAttachmentInfo.MIME_PREFIX)) return new ConversationManager.AudioAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		else if(fileType.startsWith(VideoAttachmentInfo.MIME_PREFIX)) return new ConversationManager.VideoAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		return new ConversationManager.OtherAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
	}
	
	private static int getMaxMessageWidth(Resources resources) {
		return (int) Math.min(resources.getDimensionPixelSize(R.dimen.contentwidth_max) * .7F, resources.getDisplayMetrics().widthPixels * 0.7F);
	}
}