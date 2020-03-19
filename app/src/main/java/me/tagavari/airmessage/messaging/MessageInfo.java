package me.tagavari.airmessage.messaging;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.gms.common.util.BiConsumer;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Transaction;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.FilePushRequest;
import me.tagavari.airmessage.connection.request.MessageResponseManager;
import me.tagavari.airmessage.data.BitmapCacheHelper;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.MMSSMSHelper;
import me.tagavari.airmessage.util.NotificationUtils;

public class MessageInfo extends ConversationItem<MessageInfo.ViewHolder> {
	//Creating the constants
	public static final int itemType = 0;
	public static final int itemViewType = viewTypeMessage;
	
	private static final int dpDefaultMessagePadding = 5;
	private static final int dpRelatedMessagePadding = 1;
	private static final int dpInterMessagePadding = 2;
	
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
	
	public MessageInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, String sender, String messageText, String messageSubject, ArrayList<AttachmentInfo> attachments, String sendStyle, boolean sendStyleViewed, long date, int messageState, int errorCode, boolean errorDetailsAvailable, long dateRead) {
		//Calling the super constructor
		super(localID, serverID, guid, date, conversationInfo);
		
		//Invalidating the text if it is empty
		//if(messageText != null && messageText.isEmpty()) messageText = null;
		
		//Setting the values
		this.sender = sender;
		this.messageText = createMessageTextInfo(localID, guid, messageText, messageSubject);
		this.attachments = attachments;
		this.sendStyle = sendStyle;
		this.sendStyleViewed = sendStyleViewed;
		this.messageState = messageState;
		this.errorCode = errorCode;
		this.errorDetailsAvailable = errorDetailsAvailable;
		this.dateRead = dateRead;
	}
	
	public MessageInfo(long localID, long serverID, String guid, ConversationInfo conversationInfo, String sender, String messageText, String messageSubject, String sendStyle, boolean sendStyleViewed, long date, int messageState, int errorCode, boolean errorDetailsAvailable, long dateRead) {
		//Calling the super constructor
		super(localID, serverID, guid, date, conversationInfo);
		
		//Setting the values
		this.sender = sender;
		this.messageText = createMessageTextInfo(localID, guid, messageText, messageSubject);
		this.sendStyle = sendStyle;
		this.sendStyleViewed = sendStyleViewed;
		this.attachments = new ArrayList<>();
		this.messageState = messageState;
		this.errorCode = errorCode;
		this.errorDetailsAvailable = errorDetailsAvailable;
		this.dateRead = dateRead;
	}
	
	private MessageTextInfo createMessageTextInfo(long localID, String guid, String body, String subject) {
		//No message text if there is no text to begin with
		if(body == null && subject == null) return null;
		
		return new MessageTextInfo(localID, guid, this, body, subject);
	}
	
	public void addAttachment(AttachmentInfo attachment) {
		attachments.add(attachment);
	}
	
	public void removeAttachment(AttachmentInfo attachment) {
		attachments.remove(attachment);
	}
	
	public String getSender() {
		return sender;
	}
	
	public String getMessageText() {
		return messageText == null ? null : messageText.getText();
	}
	
	public String getMessageSubject() {
		return messageText == null ? null : messageText.getSubject();
	}
	
	public MessageTextInfo getMessageTextInfo() {
		return messageText;
	}
	
	public ArrayList<AttachmentInfo> getAttachments() {
		return attachments;
	}
	
	public boolean isOutgoing() {
		//Returning if the message is outgoing
		return sender == null;
	}
	
	public int getMessageState() {
		return messageState;
	}
	
	public void setMessageState(int messageState) {
		this.messageState = messageState;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public String getErrorDetails() {
		return errorDetails;
	}
	
	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
		errorDetailsAvailable = errorDetails != null;
	}
	
	public long getDateRead() {
		return dateRead;
	}
	
	public void setDateRead(long dateRead) {
		this.dateRead = dateRead;
	}
	
	public boolean isSending() {
		return isSending;
	}
	
	public void setAnchoredTop(boolean anchoredTop) {
		isAnchoredTop = anchoredTop;
	}
	
	public void setAnchoredBottom(boolean anchoredBottom) {
		isAnchoredBottom = anchoredBottom;
	}
	
	public void updateViewEdges(boolean isLTR) {
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
		int pxCornerAnchored = viewHolder.itemView.getResources().getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		int pxCornerUnanchored = viewHolder.itemView.getResources().getDimensionPixelSize(R.dimen.messagebubble_radius);
		
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
		if(!Preferences.getPreferenceShowReadReceipts(context)) return;
		
		//Getting the requested state
		isShowingMessageState = (this == getConversationInfo().getActivityStateTargetRead() || this == getConversationInfo().getActivityStateTargetDelivered()) &&
				messageState != Constants.messageStateCodeGhost &&
				messageState != Constants.messageStateCodeIdle &&
				messageState != Constants.messageStateCodeSent;
		
		//Setting up the label
		if(isShowingMessageState) {
			viewHolder.labelActivityStatus.setVisibility(View.VISIBLE);
			viewHolder.labelActivityStatus.setCurrentText(getDeliveryStatusText(context));
		} else {
			viewHolder.labelActivityStatus.setVisibility(View.GONE);
		}
	}
	
	public void updateActivityStateDisplay(Context context) {
		//Returning if read receipt showing is disabled
		if(!Preferences.getPreferenceShowReadReceipts(context)) return;
		
		//Getting the requested state
		boolean requestedState = (this == getConversationInfo().getActivityStateTargetRead() || this == getConversationInfo().getActivityStateTargetDelivered()) &&
				messageState != Constants.messageStateCodeGhost &&
				messageState != Constants.messageStateCodeIdle &&
				messageState != Constants.messageStateCodeSent;
		
		//Calling the overload method
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) updateActivityStateDisplay(viewHolder, context, isShowingMessageState, requestedState);
		
		//Setting the current state
		isShowingMessageState = requestedState;
	}
	
	private void updateActivityStateDisplay(ViewHolder viewHolder, Context context, boolean currentState, boolean requestedState) {
		//Returning if read receipt showing is disabled
		if(!Preferences.getPreferenceShowReadReceipts(context)) return;
		
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
			case Constants.messageStateCodeDelivered:
				return context.getResources().getString(R.string.state_delivered);
			case Constants.messageStateCodeRead: {
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
	
	/**
	 * Sends this message, automatically determining the correct service to use
	 * @param context The context to use
	 * @return Whether or not the message was successfully sent
	 */
	public boolean sendMessage(Context context) {
		//Checking if the service handler is AirMessage bridge
		ConversationInfo conversationInfo = getConversationInfo();
		if(conversationInfo.getServiceHandler() == ConversationInfo.serviceHandlerAMBridge) {
			//Sending the message via AirMessage bridge
			return sendMessageAMBridge(MainApplication.getInstance());
		}
		//Checking if the service handler is system messaging
		else if(conversationInfo.getServiceHandler() == ConversationInfo.serviceHandlerSystemMessaging) {
			//Checking if the service is text messaging
			if(ConversationInfo.serviceTypeSystemMMSSMS.equals(conversationInfo.getService())) {
				//Sending the message via MMS / SMS
				return sendMessageMMSSMS(context);
			}
		}
		
		//No valid service could be found
		return false;
	}
	
	/**
	 * Send this message to the bridge server
	 * @param context The context to use
	 * @return Whether or not the message was successfully sent
	 */
	private boolean sendMessageAMBridge(Context context) {
		//Creating a weak reference to the context
		WeakReference<Context> contextReference = new WeakReference<>(context);
		
		//Getting the connection service
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		
		//Creating the callback listener
		MessageResponseManager messageResponseManager = new MessageResponseManager(new ConnectionManager.MessageResponseManagerDeregistrationListener(connectionManager)) {
			@Override
			public void onSuccess() {}
			
			@Override
			public void onFail(int errorCode, String errorDetails) {
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
				
				//Sending a notification
				NotificationUtils.sendErrorNotification(MainApplication.getInstance(), getConversationInfo());
			}
		};
		
		//Checking if the connection manager is unavailable
		if(connectionManager == null) {
			//Telling the response manager
			messageResponseManager.onFail(Constants.messageErrorCodeLocalNetwork, null);
			
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
			return getConversationInfo().getState() == ConversationInfo.ConversationState.READY ?
				   connectionManager.sendMessage(getConversationInfo().getGuid(), getMessageText(), messageResponseManager) :
				   connectionManager.sendMessage(getConversationInfo().getConversationMembersAsArray(), getMessageText(), getConversationInfo().getService(), messageResponseManager);
		} else {
			//Returning false if there are no attachments
			if(attachments.isEmpty()) return false;
			
			//Getting the attachment
			AttachmentInfo attachmentInfo = attachments.get(0);
			
			//Constructing the push request
			FilePushRequest request = attachmentInfo.getDraftingPushRequest();
			if(request != null) {
				request.setAttachmentID(attachmentInfo.getLocalID());
				request.setUploadRequested(true);
			} else {
				if(attachmentInfo.file == null) return false;
				request = new FilePushRequest(attachmentInfo.file, attachmentInfo.fileType, attachmentInfo.fileName, -1, getConversationInfo(), attachmentInfo.getLocalID(), -1, FilePushRequest.stateAttached, System.currentTimeMillis(), true, -1);
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
					attachmentViewHolder.groupContentFrame.setVisibility(View.GONE);
					attachmentViewHolder.groupFailed.setVisibility(View.GONE);
					attachmentViewHolder.labelFailed.setText(attachmentInfo.getResourceTypeName()); //In the case that the attachment decides to display this view group later on
					attachmentViewHolder.groupProcessing.setVisibility(View.GONE);
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
			connectionManager.addFileProcessingRequest(request);
			
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
	
	/**
	 * Prepares to send a message, but uses the provided handler to handle sending the message.
	 * Called primarily to handle updating the view progress and error.
	 * @param context The context to use
	 * @return Whether or not the message was successfully sent
	 */
	private boolean sendMessageMMSSMS(Context context) {
		//Creating a weak reference to the context
		WeakReference<Context> contextReference = new WeakReference<>(context);
		
		//Getting the view holder
		ViewHolder viewHolder = getViewHolder();
		
		if(viewHolder != null) {
			//Hiding the error view
			viewHolder.buttonSendError.setVisibility(View.GONE);
			
			//Hiding the progress view
			viewHolder.progressSend.setVisibility(View.GONE);
		}
		
		//Configuring the message settings
		Transaction transaction = Constants.getMMSSMSTransaction(MainApplication.getInstance(), getLocalID());
		
		//Creating the message
		Message message = new Message();
		
		//Setting the message recipients
		message.setAddresses(getConversationInfo().getNormalizedConversationMembersAsArray());
		
		//Setting the message text
		{
			String messageText = getMessageText();
			if(messageText != null) message.setText(messageText);
			String messageSubject = getMessageSubject();
			if(messageSubject!= null) message.setSubject(messageSubject);
		}
		
		//Checking if there are any attachments
		if(attachments.isEmpty()) {
			//Sending the message immediately
			transaction.sendNewMessage(message, getConversationInfo().getExternalID());
		} else {
			//Getting the connection service
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			
			//Checking if the connection manager is unavailable
			if(connectionManager == null) {
				//Setting the error code
				setErrorCode(Constants.messageErrorCodeLocalNetwork);
				setErrorDetails(null);
				
				//Updating the message's database entry
				new UpdateErrorCodeTask(getLocalID(), Constants.messageErrorCodeLocalNetwork, null).execute();
				
				//Updating the adapter
				ConversationInfo.ActivityCallbacks updater = getConversationInfo().getActivityCallbacks();
				if(updater != null) updater.messageSendFailed(MessageInfo.this);
				
				//Updating the view
				if(viewHolder != null) updateViewProgressState(viewHolder);
				
				//Sending a notification
				NotificationUtils.sendErrorNotification(MainApplication.getInstance(), getConversationInfo());
				
				//Returning
				return false;
			}
			
			Handler handler = new Handler(context.getMainLooper());
			Constants.ValueWrapper<Integer> processedAttachmentsRemaining = new Constants.ValueWrapper<>(attachments.size());
			for(AttachmentInfo attachmentInfo : attachments) {
				//Constructing the push request
				FilePushRequest request = attachmentInfo.getDraftingPushRequest();
				if(request != null) {
					request.setAttachmentID(attachmentInfo.getLocalID());
					request.setUploadRequested(true);
				} else {
					if(attachmentInfo.file == null) continue;
					request = new FilePushRequest(attachmentInfo.file, attachmentInfo.fileType, attachmentInfo.fileName, -1, getConversationInfo(), attachmentInfo.getLocalID(), -1, FilePushRequest.stateAttached, System.currentTimeMillis(), true, MMSSMSHelper.getMaxMessageSize(context));
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
						attachmentViewHolder.groupContentFrame.setVisibility(View.GONE);
						attachmentViewHolder.groupFailed.setVisibility(View.GONE);
						attachmentViewHolder.labelFailed.setText(attachmentInfo.getResourceTypeName()); //In the case that the attachment decides to display this view group later on
						attachmentViewHolder.groupProcessing.setVisibility(View.GONE);
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
					
					//Hiding the progress bar (this will be done once the file is read and sent to carrier servers)
					/* ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					TransitionManager.beginDelayedTransition((ViewGroup) newViewHolder.itemView);
					newViewHolder.progressSend.setVisibility(View.GONE); */
				};
				 final BiConsumer<Integer, String> failConsumer = request.getCallbacks().onFail = (errorCode, errorDetails) -> {
					//Setting the error code
					setErrorCode(errorCode);
					setErrorDetails(errorDetails);
					
					//Updating the message's database entry
					new UpdateErrorCodeTask(getLocalID(), errorCode, errorDetails).execute();
					
					//Updating the adapter
					ConversationInfo.ActivityCallbacks updater = getConversationInfo().getActivityCallbacks();
					if(updater != null) updater.messageSendFailed(MessageInfo.this);
					
					//Setting the message as not sending
					isSending = false;
					
					//Hiding the progress bar and updating the state
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder == null) return;
					newViewHolder.progressSend.setVisibility(View.GONE);
					updateViewProgressState(newViewHolder);
					
					//Sending a notification
					NotificationUtils.sendErrorNotification(MainApplication.getInstance(), getConversationInfo());
				};
				request.setCustomUploadHandler(filePushRequest -> {
					//Immediately completing the upload (as there is no standard upload process in this case; at least not one that's worth nicely displaying to the user)
					handler.post(() -> filePushRequest.getCallbacks().onUploadFinished.accept(null));
					
					//Reading the file
					File file = filePushRequest.getSendFile();
					byte[] fileBytes = new byte[(int) file.length()];
					try(DataInputStream inputStream = new DataInputStream(new FileInputStream(file))) {
						inputStream.readFully(fileBytes);
					} catch(IOException exception) {
						exception.printStackTrace();
						
						//Failing the request
						handler.post(() -> failConsumer.accept(Constants.messageErrorCodeLocalIO, null));
						
						return;
					}
					
					handler.post(() -> {
						//Adding the file to the message
						message.addMedia(fileBytes, attachmentInfo.getContentType(), file.getName());
						
						//Decreasing the remaining attachments count
						processedAttachmentsRemaining.value--;
						
						//Sending the message if all attachments have been processed
						if(processedAttachmentsRemaining.value == 0) {
							transaction.sendNewMessage(message, getConversationInfo().getExternalID());
							
							//Setting the message as not sending
							isSending = false;
							
							//Hiding the progress bar
							ViewHolder newViewHolder = getViewHolder();
							if(newViewHolder == null) return;
							newViewHolder.progressSend.setVisibility(View.GONE);
						}
					});
				});
				
				//Queuing the request
				connectionManager.addFileProcessingRequest(request);
			}
			
			//Setting the upload values
			isSending = true;
			sendProgress = -1;
			
			if(viewHolder != null) {
				//Showing and configuring the progress view
				viewHolder.progressSend.setVisibility(View.VISIBLE);
				viewHolder.progressSend.spin();
			}
		}
		
		//Returning true
		return true;
	}
	
	public void deleteMessage(Context context) {
		//Removing the item from the conversation in memory
		getConversationInfo().removeConversationItem(context, this);
		
		//Updating the last item
		getConversationInfo().updateLastItem(context);
		
		//Updating the view
		getConversationInfo().updateView(context);
		
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
	
	public void updateTimeDivider(Context context) {
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
	
	public void setHasTimeDivider(boolean hasTimeDivider) {
		this.hasTimeDivider = hasTimeDivider;
	}
	
	private interface PoolSourceAccepter {
		void accept(int itemViewType, MessageComponent.ViewHolder viewHolder);
	}
	
	@Override
	public void bindView(ViewHolder viewHolder, Context context) {
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
				else if(componentViewHolder instanceof VLocationAttachmentInfo.ViewHolder) viewAdder.accept(VLocationAttachmentInfo.ITEM_VIEW_TYPE, componentViewHolder);
				else if(componentViewHolder instanceof ContactAttachmentInfo.ViewHolder) viewAdder.accept(ContactAttachmentInfo.ITEM_VIEW_TYPE, componentViewHolder);
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
				public void onImageMeasured(int width, int height) {}
				
				@Override
				public void onImageDecoded(Bitmap result, boolean wasTasked) {
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
	public void onScrollShow() {
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
	public void updateViewColor(Context context) {
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
		viewHolder.progressSend.setBarColor(getConversationInfo().getDisplayConversationColor(context));
		
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
	
	public void updateViewProgressState() {
		//Calling the overload method
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) updateViewProgressState(viewHolder);
	}
	
	private static final float ghostAlpha = 0.50F;
	private void updateViewProgressState(ViewHolder viewHolder) {
		//Setting the message part container's alpha
		if(messageState == Constants.messageStateCodeGhost) viewHolder.containerMessagePart.setAlpha(ghostAlpha);
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
			MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(newContext)
					.setTitle(R.string.message_messageerror_title)
					.setNeutralButton(R.string.action_deletemessage, (dialog, which) -> {
						Context anotherNewContext = contextReference.get();
						if(anotherNewContext != null) deleteMessage(anotherNewContext);
					})
					.setNegativeButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss());
			
			//Getting the error display
			Constants.Tuple2<String, Boolean> errorDisplay = getErrorDisplay(newContext, getConversationInfo(), errorCode);
			
			//Setting the message
			dialogBuilder.setMessage(errorDisplay.item1);
			
			//Showing the retry button (if requested)
			if(errorDisplay.item2) {
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
	
	/**
	 * Gets error details to display to the user from a constant error code
	 * @param context The context to use
	 * @param conversationInfo The conversation relevant to the error
	 * @param errorCode The error code
	 * @return A tuple, containing a String for the error message, and a Boolean for whether the error is recoverable
	 */
	public static Constants.Tuple2<String, Boolean> getErrorDisplay(Context context, ConversationInfo conversationInfo, int errorCode) {
		String message;
		boolean showRetryButton;
		
		switch(errorCode) {
			case Constants.messageErrorCodeLocalUnknown:
			default:
				//Setting the message
				message = context.getResources().getString(R.string.message_unknownerror);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			
			case Constants.messageErrorCodeLocalInvalidContent:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_invalidcontent);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case Constants.messageErrorCodeLocalFileTooLarge:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_filetoolarge);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case Constants.messageErrorCodeLocalIO:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_io);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeLocalNetwork:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_network);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeServerExternal:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_external);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeLocalExpired:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_expired);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeLocalReferences:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_references);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeLocalInternal:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_internal);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeServerBadRequest:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_badrequest);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeServerUnauthorized:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_unauthorized);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeServerNoConversation:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_noconversation);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case Constants.messageErrorCodeServerRequestTimeout:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_serverexpired);
				
				//Disabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeServerUnknown:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_externalunknown);
				
				//Disabling the retry button
				showRetryButton = true;
				
				break;
			case Constants.messageErrorCodeAppleNetwork:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_apple_network);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case Constants.messageErrorCodeAppleUnregistered:
				//Setting the message
				message = conversationInfo.getConversationMembers().isEmpty() ?
						  context.getResources().getString(R.string.message_messageerror_desc_apple_unregistered_generic) :
						  context.getResources().getString(R.string.message_messageerror_desc_apple_unregistered, conversationInfo.getConversationMembers().get(0).getName());
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
		}
		
		//Returning the information
		return new Constants.Tuple2<>(message, showRetryButton);
	}
	
	private static void displayErrorDialog(Context context, String details) {
		//Creating the view
		View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_simplescroll, null);
		TextView textView = dialogView.findViewById(R.id.text);
		textView.setTypeface(Typeface.MONOSPACE);
		textView.setText(details);
		
		//Showing the dialog
		new MaterialAlertDialogBuilder(context)
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
	}
	
	public void animateGhostStateChanges() {
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder == null) return;
		
		if(messageState != Constants.messageStateCodeGhost) {
			viewHolder.containerMessagePart.setAlpha(ghostAlpha);
			viewHolder.containerMessagePart.animate().alpha(1).start();
		}
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
	
	public String getSendStyle() {
		return sendStyle;
	}
	
	public boolean getSendStyleViewed() {
		return sendStyleViewed;
	}
	
	public void playEffect() {
		//Calling the overload method
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder == null) playEffectRequested = true;
		else playEffect(viewHolder);
	}
	
	public void playEffect(ViewHolder viewHolder) {
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
	
	public void setSendStyleViewed(boolean value) {
		sendStyleViewed = value;
	}
	
	@Override
	public void getSummary(Context context, Constants.ResultCallback<String> callback) {
		//Converting the attachment list to a string resource list
		ArrayList<Integer> attachmentStringRes = new ArrayList<>();
		for(AttachmentInfo attachment : attachments) attachmentStringRes.add(ConversationUtils.getNameFromContent(attachment.getContentType(), attachment.getFileName()));
		
		//Returning the summary
		callback.onResult(false, getSummary(context, isOutgoing(), getMessageText(), getMessageSubject(), sendStyle, attachmentStringRes));
	}
	
	public String getSummary(Context context) {
		//Converting the attachment list to a string resource list
		ArrayList<Integer> attachmentStringRes = new ArrayList<>();
		for(AttachmentInfo attachment : attachments) attachmentStringRes.add(ConversationUtils.getNameFromContent(attachment.getContentType(), attachment.getFileName()));
		
		//Returning the result of the static method
		return getSummary(context, isOutgoing(), getMessageText(), getMessageSubject(), sendStyle, attachmentStringRes);
	}
	
	public static String getSummary(Context context, boolean isFromMe, String messageText, String messageSubject, String sendStyle, List<Integer> attachmentStringRes) {
		//Creating the message variable
		String message;
		
		//Applying invisible ink
		if(Constants.appleSendStyleBubbleInvisibleInk.equals(sendStyle)) message = context.getString(R.string.message_messageeffect_invisibleink);
		//Otherwise assigning the message to the message text (without line breaks)
		else if(messageText != null || messageSubject != null) {
			//Only text
			if(messageSubject == null) message = messageText.replace('\n', ' ');
			//Only subject
			else if(messageText == null) message = messageSubject.replace('\n', ' ');
			//Both text and subject
			else message = context.getResources().getString(R.string.prefix_wild, messageSubject.replace('\n', ' '), messageText.replace('\n', ' '));
		}
		//Setting the attachments if there are attachments
		else if(attachmentStringRes.size() == 1) message = context.getResources().getString(attachmentStringRes.get(0));
		else if(attachmentStringRes.size() > 1) message = context.getResources().getQuantityString(R.plurals.message_multipleattachments, attachmentStringRes.size(), attachmentStringRes.size());
		//Otherwise setting the message to "unknown"
		else message = context.getResources().getString(R.string.part_unknown);
		
		//Returning the string with the message
		if(isFromMe) return context.getString(R.string.prefix_you, message);
		else return message;
	}
	
	public String getComponentSummary(Context context, int componentIndex) {
		//Getting the component
		MessageComponent messageComponent = getComponentAtIndex(componentIndex);
		if(messageComponent == null) return null;
		if(messageComponent instanceof MessageTextInfo) return ((MessageTextInfo) messageComponent).getText();
		else if(messageComponent instanceof AttachmentInfo) {
			AttachmentInfo attachment = (AttachmentInfo) messageComponent;
			return context.getResources().getString(ConversationUtils.getNameFromContent(attachment.getContentType(), attachment.getFileName()));
		}
		return null;
	}
	
	@Override
	public int getItemType() {
		return itemType;
	}
	
	@Override
	public int getItemViewType() {
		return itemViewType;
	}
	
	@Override
	public void toLightConversationItem(Context context, Constants.ResultCallback<LightConversationItem> callback) {
		getSummary(context, (wasTasked, result) -> callback.onResult(wasTasked, new LightConversationItem(result, getDate(), getLocalID(), getServerID(), hasError())));
	}
	
	@Override
	public LightConversationItem toLightConversationItemSync(Context context) {
		//Converting the attachment list to a string resource list
		List<Integer> attachmentStringRes = new ArrayList<>();
		for(AttachmentInfo attachment : attachments)
			attachmentStringRes.add(ConversationUtils.getNameFromContent(attachment.getContentType(), attachment.getFileName()));
		
		//Returning the summary
		return new LightConversationItem(getSummary(context, isOutgoing(), getMessageText(), getMessageSubject(), sendStyle, attachmentStringRes), getDate(), getLocalID(), getServerID(), hasError());
	}
	
	public void addSticker(StickerInfo sticker) {
		//Removing the tapback from the item
		MessageComponent component = getComponentAtIndex(sticker.getMessageIndex());
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
	
	public void addLiveSticker(StickerInfo sticker, Context context) {
		//Removing the tapback from the item
		MessageComponent component = getComponentAtIndex(sticker.getMessageIndex());
		if(component == null) return;
		
		component.addLiveSticker(sticker, context);
	}
	
	public void addLiveTapback(TapbackInfo tapback, Context context) {
		//Removing the tapback from the item
		MessageComponent component = getComponentAtIndex(tapback.getMessageIndex());
		if(component == null) return;
		
		component.addLiveTapback(tapback, context);
	}
	
	public void addTapback(TapbackInfo tapback) {
		//Removing the tapback from the item
		MessageComponent component = getComponentAtIndex(tapback.getMessageIndex());
		if(component == null) return;
		
		component.addTapback(tapback);
	}
	
	public void removeLiveTapback(String sender, int messageIndex, Context context) {
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
	
	public List<StickerInfo> getStickers() {
		List<StickerInfo> list = new ArrayList<>();
		if(messageText != null) list.addAll(messageText.getStickers());
		for(AttachmentInfo item : attachments) list.addAll(item.getStickers());
		return list;
	}
	
	public List<TapbackInfo> getTapbacks() {
		List<TapbackInfo> list = new ArrayList<>();
		if(messageText != null) list.addAll(messageText.getTapbacks());
		for(AttachmentInfo item : attachments) list.addAll(item.getTapbacks());
		return list;
	}
	
	public void notifyPause() {
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) viewHolder.pause();
	}
	
	public void notifyResume() {
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) viewHolder.resume();
	}
	
	public static class ViewHolder extends RecyclerView.ViewHolder {
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
		
		public ViewHolder(View view) {
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
		
		public Messaging.MessageListRecyclerAdapter.PoolSource getRemovePoolSource() {
			Messaging.MessageListRecyclerAdapter.PoolSource currentPoolSource = poolSource;
			poolSource = null;
			return currentPoolSource;
		}
		
		public void setPoolSource(Messaging.MessageListRecyclerAdapter.PoolSource poolSource) {
			this.poolSource = poolSource;
		}
		
		public void pause() {
			for(MessageComponent.ViewHolder holder : messageComponents) holder.pause();
		}
		
		public void resume() {
			for(MessageComponent.ViewHolder holder : messageComponents) holder.resume();
		}
		
		public void cleanupState() {
			for(MessageComponent.ViewHolder holder : messageComponents) holder.cleanupState();
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
