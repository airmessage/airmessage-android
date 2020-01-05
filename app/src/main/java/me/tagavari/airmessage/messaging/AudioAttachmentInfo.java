package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.util.ColorHelper;
import me.tagavari.airmessage.util.Constants;

public class AudioAttachmentInfo extends AttachmentInfo<AudioAttachmentInfo.ViewHolder> {
	//Creating the reference values
	public static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
	public static final String MIME_TYPE = "audio/*";
	public static final int RESOURCE_NAME = R.string.part_content_audio;
	
	private static final int resDrawablePlay = R.drawable.play_rounded;
	private static final int resDrawablePause = R.drawable.pause_rounded;
	
	private static final int fileStateIdle = 0;
	private static final int fileStateLoading = 1;
	private static final int fileStateLoaded = 2;
	private static final int fileStateFailed = 3;
	
	//Creating the media values
	private long duration = 0;
	private long mediaProgress = 0;
	private int fileState = fileStateIdle;
	private boolean isPlaying = false;
	
	public AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
		super(localID, guid, message, fileName, fileType, fileSize);
	}
	
	public AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
		super(localID, guid, message, fileName, fileType, fileSize, file);
	}
	
	public AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
		super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
	}
	
	public AudioAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
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
	
	public static boolean checkFileApplicability(String fileType, String fileName) {
		return Constants.compareMimeTypes(MIME_TYPE, fileType);
	}
	
	@Override
	public void updateContentViewColor(ViewHolder viewHolder, Context context, ColorStateList cslText, ColorStateList cslBackground, ColorStateList cslAccent) {
		viewHolder.groupContent.setBackgroundTintList(cslBackground);
		viewHolder.contentIcon.setImageTintList(cslText);
		viewHolder.contentLabel.setTextColor(cslText);
		viewHolder.contentProgress.setProgressTintList(cslText);
		//viewHolder.contentProgress.setBackgroundTintList(cslText); //cslBackground
		viewHolder.contentProgress.setProgressBackgroundTintList(ColorStateList.valueOf(ColorHelper.modifyColorRaw(cslBackground.getDefaultColor(), 0.9F))); //cslBackground //TODO possibly use less lazy and hacky method
	}
	
	@Override
	public void updateContentView(ViewHolder viewHolder, Context context) {
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
			viewHolder.groupContentFrame.setVisibility(View.VISIBLE);
			
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
	public void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
		//Assigning the drawable
		viewHolder.groupContent.setBackground(drawable.getConstantState().newDrawable());
	}
	
	@Override
	public void onClick(Messaging activity) {
		//Returning if there is no content
		if(file == null) return;
		
		//Checking if the file couldn't be processed
		if(fileState == fileStateFailed) {
			//Opening the file directly
			openAttachmentFile(activity);
		} else {
			//Getting the activity callbacks
			ConversationInfo.ActivityCallbacks callbacks = getMessageInfo().getConversationInfo().getActivityCallbacks();
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
			}, activity);
		}
	}
	
	@Override
	public int getItemViewType() {
		return ITEM_VIEW_TYPE;
	}
	
	@Override
	public int getResourceTypeName() {
		return RESOURCE_NAME;
	}
	
	public void setMediaPlaying(boolean playing) {
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
	
	public void setMediaProgress(long progress) {
		//Calling the overload method
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) setMediaProgress(viewHolder, progress);
	}
	
	private void setMediaProgress(ViewHolder viewHolder, long progress) {
		mediaProgress = progress;
		updateMediaProgress(viewHolder);
	}
	
	public void updateMediaProgress() {
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
	public ViewHolder createViewHolder(Context context, ViewGroup parent) {
		return new ViewHolder(buildAttachmentView(LayoutInflater.from(context), parent, R.layout.listitem_contentaudio));
	}
	
	public static class ViewHolder extends AttachmentInfo.ViewHolder {
		final ViewGroup groupContent;
		final ImageView contentIcon;
		final TextView contentLabel;
		final ProgressBar contentProgress;
		
		public ViewHolder(View view) {
			super(view);
			
			groupContent = groupContentFrame.findViewById(R.id.content);
			contentIcon = groupContent.findViewById(R.id.content_icon);
			contentLabel = groupContent.findViewById(R.id.content_duration);
			contentProgress = groupContent.findViewById(R.id.content_progress);
		}
		
		@Override
		public void releaseResources() {
			contentIcon.setImageResource(resDrawablePlay);
			contentProgress.setProgress(0);
			contentLabel.setText(DateUtils.formatElapsedTime(0));
		}
	}
}
