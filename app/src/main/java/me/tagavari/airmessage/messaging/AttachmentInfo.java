package me.tagavari.airmessage.messaging;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;

import com.google.android.gms.common.util.BiConsumer;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.lukhnos.nnio.file.Paths;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.FileDownloadRequest;
import me.tagavari.airmessage.connection.request.FilePushRequest;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.MediaViewer;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.util.ColorHelper;
import me.tagavari.airmessage.util.Constants;

public abstract class AttachmentInfo<VH extends AttachmentInfo.ViewHolder> extends MessageComponent<VH> {
	//Creating the values
	final String fileName;
	final String fileType;
	final long fileSize;
	File file = null;
	byte[] fileChecksum = null;
	Uri fileUri = null;
	FilePushRequest draftingPushRequest = null;
	
	//Creating the attachment request values
	boolean isFetching = false;
	boolean isFetchWaiting = false;
	float fetchProgress = 0;
	
	//Creating the listener values
	private final FileDownloadRequest.Callbacks fileDownloadRequestCallbacks = new FileDownloadRequest.Callbacks() {
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
		public void onFail(int errorCode) {
			//Setting the attachment as not fetching
			isFetching = false;
			isFetchWaiting = false;
			
			//Getting the view holder
			VH viewHolder = getViewHolder();
			if(viewHolder == null) return;
			
			Context context = viewHolder.itemView.getContext();
			
			//Rebuilding the view
			buildView(viewHolder, context);
			
			//Displaying a toast
			String text;
			
			switch(errorCode) {
				case errorCodeTimeout:
					text = context.getResources().getString(R.string.message_attachmentreqerror_timeout);
					break;
				case errorCodeBadResponse:
					text = context.getResources().getString(R.string.message_attachmentreqerror_badresponse);
					break;
				case errorCodeReferencesLost:
					text = context.getResources().getString(R.string.message_attachmentreqerror_referenceslost);
					break;
				case errorCodeIO:
					text = context.getResources().getString(R.string.message_attachmentreqerror_io);
					break;
				case errorCodeServerNotFound:
					text = context.getResources().getString(R.string.message_attachmentreqerror_server_notfound);
					break;
				case errorCodeServerNotSaved:
					text = context.getResources().getString(R.string.message_attachmentreqerror_server_notsaved);
					break;
				case errorCodeServerUnreadable:
					text = context.getResources().getString(R.string.message_attachmentreqerror_server_unreadable);
					break;
				case errorCodeServerIO:
					text = context.getResources().getString(R.string.message_attachmentreqerror_server_io);
					break;
				default:
					text = null;
			}
			
			if(text != null) Toast.makeText(context, context.getResources().getString(R.string.message_attachmentreqerror_desc, text), Toast.LENGTH_SHORT).show();
		}
	};
	
	public AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
		//Calling the super constructor
		super(localID, guid, message);
		
		//Setting the values
		this.fileName = fileName;
		this.fileType = fileType;
		this.fileSize = fileSize;
	}
	
	public AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
		//Calling the main constructor
		this(localID, guid, message, fileName, fileType, fileSize);
		
		//Setting the file
		if(file != null && file.exists()) this.file = file;
	}
	
	public AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
		//Calling the main constructor
		this(localID, guid, message, fileName, fileType, fileSize);
		
		//Setting the checksum
		this.fileChecksum = fileChecksum;
	}
	
	public AttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
		//Calling the main constructor
		this(localID, guid, message, fileName, fileType, fileSize);
		
		//Setting the uri
		this.fileUri = fileUri;
	}
	
	public abstract void updateContentView(VH viewHolder, Context context);
	
	public abstract void onClick(Messaging activity);
	
	/**
	 * Binds the view to the view holder
	 * @param viewHolder The view holder to bind
	 * @param context The context to be used in the creation of the view
	 *
	 * Be sure to call the super() method when overriding!
	 */
	@Override
	public void bindView(VH viewHolder, Context context) {
		//Getting the attachment request data
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager != null) {
			FileDownloadRequest.ProgressStruct progress = connectionManager.updateDownloadRequestAttachment(localID, fileDownloadRequestCallbacks);
			if(progress != null) {
				isFetching = true;
				isFetchWaiting = progress.isWaiting;
				fetchProgress = progress.progress;
			}
		}
		
		//Setting the alignment
		((LinearLayout.LayoutParams) viewHolder.itemView.getLayoutParams()).gravity = (getMessageInfo().isOutgoing() ? Gravity.END : Gravity.START);
		
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
				viewHolder.groupContentFrame.setVisibility(View.GONE);
				viewHolder.groupFailed.setVisibility(View.GONE);
				viewHolder.groupProcessing.setVisibility(View.GONE);
				
				//Hiding the content type
				viewHolder.labelDownloadSize.setVisibility(View.GONE);
				viewHolder.labelDownloadType.setVisibility(View.GONE);
				
				//Disabling the download icon visually
				viewHolder.iconDownload.setAlpha(Constants.disabledAlpha);
				
				//Getting and preparing the progress bar
				viewHolder.progressDownload.setIndeterminate(isFetchWaiting);
				viewHolder.progressDownload.setProgress((int) (fetchProgress * viewHolder.progressDownload.getMax()));
				viewHolder.progressDownload.setVisibility(View.VISIBLE);
			}
			//Otherwise checking if the attachment is being uploaded
			else if(messageInfo.getMessageState() == Constants.messageStateCodeGhost || messageInfo.isSending()) {
				//Showing the processing view
				viewHolder.groupDownload.setVisibility(View.GONE);
				viewHolder.groupContentFrame.setVisibility(View.GONE);
				viewHolder.groupFailed.setVisibility(View.GONE);
				viewHolder.groupProcessing.setVisibility(View.VISIBLE);
				viewHolder.labelProcessing.setText(getResourceTypeName());
			} else {
				//Showing the standard download content view
				viewHolder.groupDownload.setVisibility(View.VISIBLE);
				viewHolder.groupContentFrame.setVisibility(View.GONE);
				viewHolder.groupFailed.setVisibility(View.GONE);
				viewHolder.groupProcessing.setVisibility(View.GONE);
				
				if(fileSize != -1) viewHolder.labelDownloadSize.setVisibility(View.VISIBLE);
				viewHolder.labelDownloadType.setVisibility(View.VISIBLE);
				viewHolder.iconDownload.setAlpha(1F);
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
			viewHolder.groupContentFrame.setVisibility(View.GONE);
			viewHolder.groupFailed.setVisibility(View.GONE);
			viewHolder.labelFailed.setText(getResourceTypeName()); //In the case that the attachment decides to display this view group later on
			viewHolder.groupProcessing.setVisibility(View.GONE);
			
			//Setting up the content view
			updateContentView(viewHolder, context);
		}
	}
	
	//TODO: Check what exactly is going on here?
	public void onScrollShow() {
		//VH viewHolder = getViewHolder();
		//Context context = viewHolder.itemView.getContext();
		
		//Building the view
		//buildView(viewHolder, context);
		
		//Building the common views
		//buildCommonViews(viewHolder, context);
	}
	
	@Override
	public void updateViewEdges(VH viewHolder, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
		//Creating the drawable
		Drawable drawable = Constants.createRoundedDrawable(anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored);
		
		//Assigning the drawable
		viewHolder.groupDownload.setBackground(drawable);
		viewHolder.groupProcessing.setBackground(drawable);
		viewHolder.groupFailed.setBackground(drawable);
		
		//Updating the content view's edges
		updateContentViewEdges(viewHolder, drawable, anchoredTop, anchoredBottom, alignToRight, pxCornerAnchored, pxCornerUnanchored);
	}
	
	public void updateContentViewEdges(VH viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {}
	
	@Override
	public void updateViewColor(VH viewHolder, Context context) {
		//Creating the color values
		ColorStateList cslText;
		ColorStateList cslSecondaryText;
		ColorStateList cslBackground;
		ColorStateList cslAccent;
		
		//Getting the colors
		if(messageInfo.isOutgoing()) {
			if(Preferences.getPreferenceAdvancedColor(context)) {
				cslText = ColorStateList.valueOf(Constants.resolveColorAttr(context, android.R.attr.textColorPrimary));
				cslSecondaryText = ColorStateList.valueOf(Constants.resolveColorAttr(context, android.R.attr.textColorSecondary));
				cslBackground = ColorStateList.valueOf(context.getResources().getColor(R.color.colorMessageOutgoing, null));
				cslAccent = ColorStateList.valueOf(context.getResources().getColor(R.color.colorMessageOutgoingAccent, null));
			} else {
				int colorOnPrimary = Constants.resolveColorAttr(context, R.attr.colorOnPrimary);
				cslText = ColorStateList.valueOf(colorOnPrimary);
				cslSecondaryText = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorOnPrimary, Constants.secondaryAlphaInt));
				cslBackground = ColorStateList.valueOf(context.getResources().getColor(R.color.colorPrimary, null));
				cslAccent = ColorStateList.valueOf(context.getResources().getColor(R.color.colorPrimaryLight, null));
			}
		} else {
			if(Preferences.getPreferenceAdvancedColor(context)) {
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
		viewHolder.iconDownload.setImageTintList(cslText);
		viewHolder.progressDownload.setProgressTintList(cslText);
		//viewHolder.progressDownload.setSecondaryProgressTintList(cslAccent);
		viewHolder.progressDownload.setIndeterminateTintList(ColorStateList.valueOf(ColorHelper.modifyColorRaw(cslBackground.getDefaultColor(), 0.9F)));
		viewHolder.progressDownload.setProgressBackgroundTintList(ColorStateList.valueOf(ColorHelper.modifyColorRaw(cslBackground.getDefaultColor(), 0.9F)));
		
		viewHolder.groupProcessing.setBackgroundTintList(cslBackground);
		viewHolder.labelProcessing.setTextColor(cslText);
		viewHolder.labelProcessing.setCompoundDrawableTintList(cslText);
		
		viewHolder.groupFailed.setBackgroundTintList(cslBackground);
		viewHolder.labelFailed.setTextColor(cslText);
		viewHolder.labelFailed.setCompoundDrawableTintList(cslText);
		
		//Updating the content view color
		updateContentViewColor(viewHolder, context, cslText, cslBackground, cslAccent);
	}
	
	public void updateContentViewColor(VH viewHolder, Context context, ColorStateList cslText, ColorStateList cslBackground, ColorStateList cslAccent) {}
	
	public void downloadContent(Context context) {
		//Returning if the content has already been fetched is being fetched, or the message is in a ghost state
		if(file != null || isFetching || messageInfo.getMessageState() == Constants.messageStateCodeGhost) return;
		
		//Checking if the service isn't running
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager == null || connectionManager.getCurrentState() != ConnectionManager.stateConnected) {
			//Showing a toast
			Toast.makeText(context, R.string.message_connectionerrror, Toast.LENGTH_SHORT).show();
			
			//Returning
			return;
		}
		
		//Making a download request
		boolean result = connectionManager.addDownloadRequest(fileDownloadRequestCallbacks, localID, guid, fileName);
		
		//Returning if the request couldn't be placed
		if(!result) return;
		
		//Updating the fetch state
		isFetching = true;
		isFetchWaiting = true;
		
		//Rebuilding the view
		VH viewHolder = getViewHolder();
		if(viewHolder != null) buildView(viewHolder, context);
	}
	
	public void discardFile(Context context) {
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
	
	public void displayContextMenu(View view, Context context) {
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
			menu.findItem(R.id.action_save).setEnabled(false);
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
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_type, newContext.getResources().getString(ConversationUtils.getNameFromContent(getContentType(), fileName)))).append('\n'); //Message type
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_sender, messageInfo.getSender() != null ? messageInfo.getSender() : newContext.getResources().getString(R.string.you))).append('\n'); //Sender
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_datesent, DateFormat.getTimeFormat(newContext).format(sentDate) + Constants.bulletSeparator + DateFormat.getLongDateFormat(newContext).format(sentDate))).append('\n'); //Time sent
					stringBuilder.append(newContext.getResources().getString(R.string.message_messagedetails_size, fileSize != -1 ? Formatter.formatFileSize(newContext, fileSize) : newContext.getResources().getString(R.string.part_nodata))).append('\n'); //Attachment file size
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
				case R.id.action_save: {
					if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) Constants.exportFile(context, file);
					//Otherwise requesting the permission
					else {
						ConversationInfo.ActivityCallbacks updater = getMessageInfo().getConversationInfo().getActivityCallbacks();
						if(updater != null) updater.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ConversationUtils.permissionRequestWriteStorageDownload, new ExportFileResultListener(file));
					}
					
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
	
	private static class ExportFileResultListener implements BiConsumer<Context, Boolean> {
		private final File sendFile;
		
		ExportFileResultListener(File sendFile) {
			this.sendFile = sendFile;
		}
		
		@Override
		public void accept(Context context, Boolean result) {
			if(result) Constants.exportFile(context, sendFile);
		}
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
	
	public byte[] getFileChecksum() {
		return fileChecksum;
	}
	
	public void setFileChecksum(byte[] fileChecksum) {
		this.fileChecksum = fileChecksum;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public String getContentType() {
		return fileType;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	
	public File getFile() {
		return file;
	}
	
	abstract int getResourceTypeName();
	
	public FilePushRequest getDraftingPushRequest() {
		return draftingPushRequest;
	}
	
	public void setDraftingPushRequest(FilePushRequest draftingPushRequest) {
		this.draftingPushRequest = draftingPushRequest;
	}
	
	//abstract ViewHolder createViewHolder(Context context, ViewGroup parent);
	
	public void openAttachmentFile(Context context) {
		//Returning if there is no content
		if(file == null) return;
		
		//Creating a content URI
		Uri content = FileProvider.getUriForFile(context, MainApplication.fileAuthority, file);
		
		//Launching the content viewer
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(content, fileType);
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if(intent.resolveActivity(context.getPackageManager()) != null) context.startActivity(intent);
		else Toast.makeText(context, R.string.message_intenterror_open, Toast.LENGTH_SHORT).show();
	}
	
	public void openAttachmentFileMediaViewer(Activity activity, View transitionView, float[] radiiRaw) {
		//Assembling a list of all attachment files
		ArrayList<ConversationAttachmentList.Item> itemList = new ArrayList<>();
		LongSparseArray<AttachmentInfo> localIDAttachmentMap = new LongSparseArray<>();
		int itemIndex = -1;
		List<ConversationItem> conversationItemList = getMessageInfo().getConversationInfo().getConversationItems();
		if(conversationItemList == null) {
			itemList.add(new ConversationAttachmentList.Item(this));
			localIDAttachmentMap.put(getLocalID(), this);
			itemIndex = 0;
		}
		else {
			for(ConversationItem conversationItem : conversationItemList) {
				if(!(conversationItem instanceof MessageInfo)) continue;
				List<AttachmentInfo> attachmentList = ((MessageInfo) conversationItem).getAttachments();
				for(AttachmentInfo attachmentInfo : attachmentList) {
					if(attachmentInfo.getFile() == null || !(attachmentInfo instanceof ImageAttachmentInfo || attachmentInfo instanceof VideoAttachmentInfo)) continue;
					if(attachmentInfo == this) itemIndex = itemList.size();
					itemList.add(new ConversationAttachmentList.Item(attachmentInfo));
					localIDAttachmentMap.put(attachmentInfo.getLocalID(), attachmentInfo);
				}
			}
		}
		
		//Setting the local ID attachment map
		getMessageInfo().getConversationInfo().setLocalIDAttachmentMap(localIDAttachmentMap);
		
		//Launching the media viewer
		if(itemIndex == -1) return;
		Intent intent = new Intent(activity, MediaViewer.class);
		intent.putExtra(MediaViewer.PARAM_INDEX, itemIndex);
		intent.putParcelableArrayListExtra(MediaViewer.PARAM_DATALIST, itemList);
		intent.putExtra(MediaViewer.PARAM_RADIIRAW, radiiRaw);
		activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity, transitionView, "mediaViewer").toBundle());
	}
	
	public static abstract class ViewHolder extends MessageComponent.ViewHolder {
		final ViewGroup groupDownload;
		final ProgressBar progressDownload;
		final TextView labelDownloadSize;
		final TextView labelDownloadType;
		final ImageView iconDownload;
		
		final ViewGroup groupFailed;
		final TextView labelFailed;
		
		final ViewGroup groupProcessing;
		final TextView labelProcessing;
		
		final ViewGroup groupContentFrame;
		
		ViewHolder(View view) {
			super(view);
			
			groupDownload = view.findViewById(R.id.downloadcontent);
			progressDownload = groupDownload.findViewById(R.id.download_progress);
			labelDownloadSize = groupDownload.findViewById(R.id.label_size);
			labelDownloadType = groupDownload.findViewById(R.id.label_type);
			iconDownload = groupDownload.findViewById(R.id.download_icon);
			
			groupFailed = view.findViewById(R.id.failedcontent);
			labelFailed = groupFailed.findViewById(R.id.failed_label);
			
			groupProcessing = view.findViewById(R.id.processingcontent);
			labelProcessing = groupProcessing.findViewById(R.id.processing_label);
			
			groupContentFrame = view.findViewById(R.id.frame_content);
		}
	}
	
	public static String getRelativePath(Context context, File file) {
		return MainApplication.getAttachmentDirectory(context).toURI().relativize(file.toURI()).getPath();
	}
	
	public static File getAbsolutePath(Context context, String path) {
		return Paths.get(MainApplication.getAttachmentDirectory(context).getPath()).resolve(path).toFile();
	}
	
	public static View getEmptyAttachmentView(LayoutInflater layoutInflater, ViewGroup parent) {
		return layoutInflater.inflate(R.layout.listitem_contentstructure, parent, false);
	}
	
	public static View buildAttachmentView(LayoutInflater layoutInflater, ViewGroup parent, @LayoutRes int contentLayout) {
		//Inflating the structure view
		View structureView = getEmptyAttachmentView(layoutInflater, parent);
		
		//Adding the content layout
		layoutInflater.inflate(contentLayout, structureView.findViewById(R.id.frame_content), true);
		
		//Returning the view
		return structureView;
	}
}
