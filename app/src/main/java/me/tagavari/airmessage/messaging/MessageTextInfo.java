package me.tagavari.airmessage.messaging;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.util.ColorHelper;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.DataTransformUtils;
import me.tagavari.airmessage.util.RichPreview;
import me.tagavari.airmessage.view.InvisibleInkView;

public class MessageTextInfo extends MessageComponent<MessageTextInfo.ViewHolder> {
	//Creating the reference values
	static final int itemViewType = MessageComponent.getNextItemViewType();
	
	//Creating the component values
	private String messageText;
	private String messageSubject;
	private boolean messageTextSpannableLoading = false;
	private Spannable messageTextSpannable = null;
	
	public MessageTextInfo(long localID, String guid, MessageInfo message, String messageText, String messageSubject) {
		//Calling the super constructor
		super(localID, guid, message);
		
		//Setting the text
		this.messageText = messageText;
		this.messageSubject = messageSubject;
	}
	
	public String getText() {
		return messageText;
	}
	
	public String getSubject() {
		return messageSubject;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void bindView(ViewHolder viewHolder, Context context) {
		//Checking if there is body text
		if(messageText != null) {
			//Showing the body label
			viewHolder.labelBody.setVisibility(View.VISIBLE);
			
			//Checking if the string consists exclusively of emoji characters
			if(Constants.stringContainsOnlyEmoji(messageText)) {
				//Increasing the text size
				viewHolder.labelBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
				
				//Setting the message text
				viewHolder.labelBody.setText(messageText);
			} else {
				//Resetting the text size
				viewHolder.labelBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
				
				//Checking if the device can use Smart Linkify (Android 9.0 Pie, API 28 and above)
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					//Setting the text
					if(messageTextSpannable != null) {
						viewHolder.labelBody.setMovementMethod(LinkMovementMethod.getInstance());
						viewHolder.labelBody.setText(messageTextSpannable);
					} else {
						//Requesting the text to be processed
						if(!messageTextSpannableLoading) {
							new TextLinksAsyncTask(context, this).execute();
							messageTextSpannableLoading = true;
						}
						
						//Setting the message without links for now
						viewHolder.labelBody.setText(messageText);
					}
				} else {
					//Setting the message text
					viewHolder.labelBody.setText(messageText);
					
					//Defaulting to standard Linkify
					addTextLinksLegacy(viewHolder.labelBody);
				}
			}
			
			//Setting the body text touch listener
			viewHolder.labelBody.setOnTouchListener((View view, MotionEvent event) -> {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					ViewHolder newViewHolder = getViewHolder();
					if(newViewHolder != null) newViewHolder.inkView.reveal();
				} else if(event.getAction() == MotionEvent.ACTION_UP) {
					new Handler(Looper.getMainLooper()).postDelayed(() -> ((TextView) view).setLinksClickable(true), 0);
				}
				
				return view.onTouchEvent(event);
			});
		} else {
			//Hiding the body label
			viewHolder.labelBody.setVisibility(View.GONE);
		}
		
		//Checking if there is subject text
		if(messageSubject != null) {
			//Showing the subject label
			viewHolder.labelSubject.setVisibility(View.VISIBLE);
			
			//Setting the subject text
			viewHolder.labelSubject.setText(messageSubject);
		} else {
			//Hiding the subject label
			viewHolder.labelSubject.setVisibility(View.GONE);
		}
		
		//Setting the message alignment
		((LinearLayout.LayoutParams) viewHolder.itemView.getLayoutParams()).gravity = (getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START);
		
		//Updating the view color
		updateViewColor(viewHolder, context);
		
		//Setting the long click listener
		viewHolder.groupMessage.setOnLongClickListener(clickedView -> {
			//Getting the context
			Context newContext = clickedView.getContext();
			
			//Returning if the view is not an activity
			if(!(newContext instanceof Activity)) return false;
			
			//Displaying the context menu
			displayContextMenu(newContext, viewHolder.groupMessage);
			
			//Returning
			return true;
		});
		viewHolder.labelBody.setOnLongClickListener(clickedView -> {
			//Getting the context
			Context newContext = clickedView.getContext();
			
			//Returning if the view is not an activity
			if(!(newContext instanceof Activity)) return false;
			
			//Displaying the context menu
			displayContextMenu(newContext, viewHolder.groupMessage);
			
			//Disabling link clicks
			viewHolder.labelBody.setLinksClickable(false);
			
			//Returning
			return true;
		});
		
		//Enforcing the maximum content width
		{
			int maxWidth = ConversationUtils.getMaxMessageWidth(context.getResources());
			viewHolder.labelBody.setMaxWidth(maxWidth);
			viewHolder.labelSubject.setMaxWidth(maxWidth);
			viewHolder.messagePreviewContainer.getLayoutParams().width = maxWidth;
		}
		
		//Resetting the preview view
		clearMessagePreviewView(viewHolder);
		
		//Checking if previews are enabled
		if(Preferences.getPreferenceMessagePreviews(context)) {
			//Checking if a message preview is available
			if(getMessagePreviewState() == MessagePreviewInfo.stateAvailable) {
				//Requesting the message preview information
				MessagePreviewInfo preview = getLoadMessagePreview();
				if(preview != null) {
					addMessagePreviewView(preview, viewHolder, context);
				}
			}
			//Checking if a message preview should be fetched
			else if(getMessagePreviewState() == MessagePreviewInfo.stateNotTried && !isMessagePreviewLoading() && messageText != null) {
				//Finding any URL spans
				Matcher matcher = Patterns.WEB_URL.matcher(messageText);
				int matchOffset = 0;
				String targetUrl = null;
				
				while(matcher.find(matchOffset)) {
					//Getting the URL
					String urlString = matcher.group();
					
					//Updating the offset
					matchOffset = matcher.end();
					
					//Ignoring email addresses
					int matchStart = matcher.start();
					if(matchStart > 0 && messageText.charAt(matchStart - 1) == '@') continue;
					
					//Skipping the URL if it has a custom scheme (the WEB_URL matcher will not include the scheme in the URL if it is unknown)
					String schemeOutside = messageText.substring(0, matcher.start());
					if(schemeOutside.matches("\\w(?:\\w|\\d|\\+|-|\\.)*:\\/\\/$")) continue; //https://regex101.com/r/hW5bOW/1
					
					if(urlString.contains("…")) continue; //Crashes okhttp for some reason
					
					if(!urlString.contains("://")) urlString = "https://" + urlString; //Adding the scheme if it doesn't have one
					else if(urlString.startsWith("http://")) urlString = urlString.replaceFirst("http://", "https://"); //Replacing HTTP schemes with HTTPS schemes
					else if(!urlString.startsWith("https://")) continue; //Ignoring URLs of other schemes
					
					//Setting the url
					targetUrl = urlString;
					break;
				}
				
				//Checking if a URL was found
				if(targetUrl != null) {
					//Fetching the data
					setMessagePreviewLoading(true);
					RichPreview.getPreview(targetUrl, new RichPreviewResponseListener(this, targetUrl));
				} else {
					//Updating the preview
					setMessagePreview(null);
					
					//Updating the state on disk
					new MessagePreviewStateUpdateAsyncTask(getLocalID(), MessagePreviewInfo.stateUnavailable).execute();
				}
			}
		}
		
		//Building the common views
		buildCommonViews(viewHolder, context);
		
		//Setting up the message effects
		if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) {
			viewHolder.inkView.setVisibility(View.VISIBLE);
			viewHolder.inkView.setState(true);
		}
		else viewHolder.inkView.setVisibility(View.GONE);
	}
	
	private void addTextLinksLegacy(TextView textView) {
		//Setting up the URL checker
		Linkify.addLinks(textView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
		textView.setTransformationMethod(new Constants.CustomTabsLinkTransformationMethod());
		textView.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	public void updateTextLinks(Spannable spannable) {
		//Updating the text
		messageTextSpannableLoading = false;
		messageTextSpannable = spannable;
		
		//Updating the message label, if it is available
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) {
			viewHolder.labelBody.setMovementMethod(LinkMovementMethod.getInstance());
			viewHolder.labelBody.setText(spannable);
		}
	}
	
	@RequiresApi(api = Build.VERSION_CODES.P)
	private static class TextLinksAsyncTask extends AsyncTask<Void, Void, Spannable> {
		private final TextClassifier textClassifier;
		private final String messageText;
		private final WeakReference<MessageTextInfo> messageReference;
		
		TextLinksAsyncTask(Context context, MessageTextInfo message) {
			textClassifier = ((TextClassificationManager) context.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE)).getTextClassifier();
			messageText = message.getText();
			messageReference = new WeakReference<>(message);
		}
		
		@Override
		protected Spannable doInBackground(Void... voids) {
			if(messageText.length() > textClassifier.getMaxGenerateLinksTextLength()) return null;
			
			Spannable spannable = new SpannableString(messageText);
			TextLinks textLinks = textClassifier.generateLinks(new TextLinks.Request.Builder(messageText).build());
			textLinks.apply(spannable, TextLinks.APPLY_STRATEGY_REPLACE, null);
			
			//The following code block would be used to feed Smart Linkify's output into the message preview generator
			/* String targetWebURL = null;
			for(TextLinks.TextLink link : textLinks.getLinks()) {
				//Skipping non-URL links
				if(!TextClassifier.TYPE_URL.equals(link.getEntity(0))) continue;
				
				//Getting the URL
				String urlString = messageText.substring(link.getStart(), link.getEnd());
				
				if(!urlString.contains("://")) urlString = "https://" + urlString; //Adding the scheme if it doesn't have one
				else if(urlString.startsWith("http://")) urlString = urlString.replaceFirst("http://", "https://"); //Replacing HTTP schemes with HTTPS schemes
				else if(!urlString.startsWith("https://")) continue; //Ignoring URLs of other schemes
				
				//Setting the target
				targetWebURL = urlString;
				
				break;
			} */
			
			return spannable;
		}
		
		@Override
		protected void onPostExecute(Spannable spannable) {
			if(spannable == null) return;
			
			MessageTextInfo messageTextInfo = messageReference.get();
			if(messageTextInfo != null) messageTextInfo.updateTextLinks(spannable);
		}
	}
	
	private static class MessagePreviewStateUpdateAsyncTask extends AsyncTask<Void, Void, Void> {
		private final long localID;
		private final int state;
		
		MessagePreviewStateUpdateAsyncTask(long localID, int state) {
			this.localID = localID;
			this.state = state;
		}
		
		@Override
		protected Void doInBackground(Void... voids) {
			DatabaseManager.getInstance().setMessagePreviewState(localID, state);
			return null;
		}
	}
	
	private static class RichPreviewResponseListener implements RichPreview.ResponseListener {
		private final WeakReference<MessageTextInfo> messageTextReference;
		private final long messageTextID;
		private final String originalURL;
		
		RichPreviewResponseListener(MessageTextInfo messageTextInfo, String originalURL) {
			//Setting the message reference
			messageTextReference = new WeakReference<>(messageTextInfo);
			messageTextID = messageTextInfo.getLocalID();
			this.originalURL = originalURL;
		}
		
		@Override
		public void onData(RichPreview.Metadata metadata) {
			//Getting the message
			MessageTextInfo messageText = messageTextReference.get();
			
			//Checking if there is no useful data
			String title = metadata.getTitle();
			if(title == null || title.isEmpty()) {
				//Updating the preview
				if(messageText != null) messageText.setMessagePreview(null);
				
				//Updating the state on disk
				new MessagePreviewStateUpdateAsyncTask(messageTextID, MessagePreviewInfo.stateUnavailable).execute();
				
				return;
			}
			
			//Fetching the link preview
			new LinkPreviewAsyncTask(messageTextReference, messageTextID, originalURL, metadata).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		
		@Override
		public void onError(Exception exception) {
			//Updating the message
			MessageTextInfo messageTextInfo = messageTextReference.get();
			if(messageTextInfo != null) messageTextInfo.setMessagePreviewLoading(false);
		}
	}
	
	private static class LinkPreviewAsyncTask extends AsyncTask<Void, Void, MessagePreviewInfo> {
		private final WeakReference<MessageTextInfo> messageTextReference;
		private final long messageID;
		private final String originalURL;
		private final RichPreview.Metadata linkMetaData;
		private final String downloadURL;
		
		LinkPreviewAsyncTask(WeakReference<MessageTextInfo> messageTextReference, long messageID, String originalURL, RichPreview.Metadata linkMetaData) {
			this.messageTextReference = messageTextReference;
			this.messageID = messageID;
			this.originalURL = originalURL;
			this.linkMetaData = linkMetaData;
			this.downloadURL = linkMetaData.getImageURL();
		}
		
		@Override
		protected MessagePreviewInfo doInBackground(Void... params) {
			//Fetching the image data
			byte[] imageBytes = null;
			
			if(downloadURL != null && !downloadURL.isEmpty()) {
				try(BufferedInputStream in = new BufferedInputStream(new URL(downloadURL).openStream());
					ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					DataTransformUtils.copyStream(in, out);
					imageBytes = out.toByteArray();
				} catch(FileNotFoundException exception) {
					exception.printStackTrace();
					//Not returning, as the preview should simply be displayed without an image if the URI 404'd
				} catch(IOException exception) {
					exception.printStackTrace();
					return null;
				}
			}
			
			//Creating the message preview
			String caption;
			if(linkMetaData.getSiteName() != null && !linkMetaData.getSiteName().isEmpty()) caption = linkMetaData.getSiteName();
			else {
				try {
					caption = Constants.getDomainName(originalURL);
					if(caption == null) return null;
				} catch(URISyntaxException exception) {
					exception.printStackTrace();
					return null;
				}
			}
			MessagePreviewInfo messagePreview = new MessagePreviewLink(messageID, imageBytes, originalURL, linkMetaData.getTitle(), linkMetaData.getDescription(), caption);
			
			//Saving the data
			DatabaseManager.getInstance().setMessagePreviewData(messageID, messagePreview);
			
			//Returning the preview
			return messagePreview;
		}
		
		@Override
		protected void onPostExecute(MessagePreviewInfo preview) {
			//Getting the message
			MessageTextInfo messageTextInfo = messageTextReference.get();
			
			//Assigning the preview
			if(messageTextInfo != null) {
				messageTextInfo.setMessagePreviewLoading(false);
				if(preview != null) messageTextInfo.setMessagePreview(preview);
			}
		}
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
	
	@Override
	public void addMessagePreviewView(MessagePreviewInfo preview) {
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) addMessagePreviewView(preview, viewHolder, viewHolder.content.getContext());
	}
	
	private void addMessagePreviewView(MessagePreviewInfo preview, ViewHolder viewHolder, Context context) {
		//Getting the message preview's view holder
		MessagePreviewInfo.ViewHolder messagePreviewVH;
		
		if(preview instanceof MessagePreviewLink) {
			messagePreviewVH = viewHolder.getMessagePreviewLinkVH();
		} else {
			throw new IllegalArgumentException("Unsupported type of message preview provided: " + preview.getClass().getName());
		}
		
		//Binding the view
		preview.bind(messagePreviewVH, context);
		
		//Setting the current preview view holder
		viewHolder.setCurrentPreviewVH(messagePreviewVH);
		
		//Expanding the message text bubble to match the view
		viewHolder.groupMessage.getLayoutParams().width = 0;
		
		//Updating the view edges
		getMessageInfo().updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
	}
	
	private void clearMessagePreviewView(ViewHolder viewHolder) {
		//Resetting the message text bubble's width to its default
		viewHolder.groupMessage.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
		
		//Removing the current preview view
		viewHolder.setCurrentPreviewVH(null);
	}
	
	@Override
	public void updateViewColor(ViewHolder viewHolder, Context context) {
		//Getting the colors
		int backgroundColor;
		int textColor;
		
		if(getMessageInfo().isOutgoing()) {
			if(Preferences.getPreferenceAdvancedColor(context)) {
				backgroundColor = context.getResources().getColor(R.color.colorMessageOutgoing, null);
				textColor = Constants.resolveColorAttr(context, android.R.attr.textColorPrimary);
			} else {
				//backgroundColor = context.getResources().getServiceColor(R.color.colorPrimary, null);
				backgroundColor = getServiceColor(context.getResources());
				//textColor = context.getResources().getServiceColor(R.color.textColorWhite, null);
				textColor = Constants.resolveColorAttr(context, R.attr.colorOnPrimary);
			}
		} else {
			if(Preferences.getPreferenceAdvancedColor(context)) {
				MemberInfo memberInfo = getMessageInfo().getConversationInfo().findConversationMember(getMessageInfo().getSender());
				int targetColor = memberInfo == null ? ConversationInfo.backupUserColor : memberInfo.getColor();
				textColor = ColorHelper.modifyColorMultiply(targetColor, Constants.isNightMode(context.getResources()) ? 1.5F : 0.7F);
				backgroundColor = ColorUtils.setAlphaComponent(targetColor, 50);
			} else {
				backgroundColor = context.getResources().getColor(R.color.colorMessageOutgoing, null);
				textColor = Constants.resolveColorAttr(context, android.R.attr.textColorPrimary);
			}
		}
		
		//Assigning the colors
		viewHolder.labelBody.setTextColor(textColor);
		viewHolder.labelBody.setLinkTextColor(textColor);
		viewHolder.labelSubject.setTextColor(textColor);
		viewHolder.groupMessage.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
		
		viewHolder.inkView.setBackgroundColor(backgroundColor);
	}
	
	@Override
	public void updateViewEdges(ViewHolder viewHolder, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
		//Checking if there is not a message preview active
		if(viewHolder.getCurrentPreviewVH() == null) {
			//Updating the message text bubble's background
			viewHolder.groupMessage.setBackground(Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored));
			
			//Updating the ink view's background
			int radiusTop = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
			int radiusBottom = anchoredBottom ? pxCornerAnchored : pxCornerUnanchored;
			
			if(alignToRight) viewHolder.inkView.setRadii(pxCornerUnanchored, radiusTop, radiusBottom, pxCornerUnanchored);
			else viewHolder.inkView.setRadii(radiusTop, pxCornerUnanchored, pxCornerUnanchored, radiusBottom);
		} else {
			//Updating the message text bubble's background
			viewHolder.groupMessage.setBackground(Constants.createRoundedDrawableTop(new GradientDrawable(), anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored));
			
			//Updating the ink view's background
			int radiusTop = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
			//int radiusBottom = anchoredBottom ? pxCornerAnchored : pxCornerUnanchored;
			
			if(alignToRight) viewHolder.inkView.setRadii(pxCornerUnanchored, radiusTop, 0, 0);
			else viewHolder.inkView.setRadii(radiusTop, pxCornerUnanchored, 0, 0);
			
			//Updating the preview view's background
			viewHolder.getMessagePreviewLinkVH().updateViewEdges(anchoredTop, anchoredBottom, alignToRight, pxCornerAnchored, pxCornerUnanchored);
		}
	}
	
	@Override
	public int getItemViewType() {
		return itemViewType;
	}
	
	private void displayContextMenu(Context context, View targetView) {
		//Creating a new popup menu
		PopupMenu popupMenu = new PopupMenu(context, targetView);
		
		//Inflating the menu
		popupMenu.inflate(R.menu.menu_conversationitem_contextual);
		
		//Removing attachment-specfic options
		Menu menu = popupMenu.getMenu();
		menu.removeItem(R.id.action_save);
		menu.removeItem(R.id.action_deletedata);
		
		//Removing the copy message and share option if there is no message
		if(messageText == null) {
			menu.removeItem(R.id.action_copytext);
			menu.removeItem(R.id.action_share);
		}
		
		//Removing the tapback info option if there are no tapbacks
		if(tapbacks.isEmpty()) menu.removeItem(R.id.action_tapbackdetails);
		
		//Creating the context reference
		WeakReference<Context> contextReference = new WeakReference<>(context);
		
		//Setting the click listener
		popupMenu.setOnMenuItemClickListener(menuItem -> {
			//Getting the context
			Context newContext = contextReference.get();
			if(newContext == null) return false;
			
			switch(menuItem.getItemId()) {
				case R.id.action_tapbackdetails: {
					//Displaying the tapback list
					displayTapbackDialog(context);
					
					//Returning true
					return true;
				}
				case R.id.action_details: {
					Date sentDate = new Date(getMessageInfo().getDate());
					
					//Building the message
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_type, newContext.getResources().getString(R.string.part_content_text))).append('\n'); //Message type
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_sender, getMessageInfo().getSender() != null ? getMessageInfo().getSender() : newContext.getResources().getString(R.string.part_you))).append('\n'); //Sender
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_datesent, DateFormat.getTimeFormat(newContext).format(sentDate) + Constants.bulletSeparator + DateFormat.getLongDateFormat(newContext).format(sentDate))).append('\n'); //Time sent
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_sendeffect, getMessageInfo().getSendStyle() == null ? newContext.getResources().getString(R.string.part_none) : getMessageInfo().getSendStyle())); //Send effect
					
					//Showing a dialog
					new MaterialAlertDialogBuilder(newContext)
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
						public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
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
	
	@Override
	public ViewHolder createViewHolder(Context context, ViewGroup parent) {
		return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contenttext, parent, false));
	}
	
	public static class ViewHolder extends MessageComponent.ViewHolder {
		final ViewGroup content;
		final ViewGroup groupMessage;
		final TextView labelBody;
		final TextView labelSubject;
		final InvisibleInkView inkView;
		
		private MessagePreviewLink.ViewHolder messagePreviewLinkVH = null;
		
		public ViewHolder(View view) {
			super(view);
			
			content = view.findViewById(R.id.content);
			groupMessage = content.findViewById(R.id.group_message);
			labelBody = groupMessage.findViewById(R.id.label_body);
			labelSubject = groupMessage.findViewById(R.id.label_subject);
			inkView = content.findViewById(R.id.content_ink);
		}
		
		public MessagePreviewLink.ViewHolder getMessagePreviewLinkVH() {
			//Inflating and creating the view holder if it isn't already ready
			if(messagePreviewLinkVH == null) {
				ViewStub viewStub = messagePreviewContainer.findViewById(R.id.viewstub_messagepreview_linklarge);
				messagePreviewLinkVH = new MessagePreviewLink.ViewHolder(viewStub.inflate());
			}
			
			//Returning the view holder
			return messagePreviewLinkVH;
		}
		
		@Override
		public void cleanupState() {
			inkView.setState(false);
		}
	}
}
