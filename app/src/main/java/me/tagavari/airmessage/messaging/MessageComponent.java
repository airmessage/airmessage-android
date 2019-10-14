package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.data.BitmapCacheHelper;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.util.Constants;

public abstract class MessageComponent<VH extends MessageComponent.ViewHolder> {
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
	
	//Creating the message preview values
	private int messagePreviewState = MessagePreviewInfo.stateNotTried;
	private long messagePreviewID = -1;
	private boolean messagePreviewLoading = false;
	private WeakReference<MessagePreviewInfo> messagePreviewReference = null;
	
	//Creating the other values
	private static int nextItemViewType = 0;
	
	public MessageComponent(long localID, String guid, MessageInfo messageInfo) {
		//Setting the values
		this.localID = localID;
		this.guid = guid;
		this.messageInfo = messageInfo;
		
		//Setting the modifiers
		stickers = new ArrayList<>();
		tapbacks = new ArrayList<>();
	}
	
	public MessageComponent(long localID, String guid, MessageInfo messageInfo, ArrayList<StickerInfo> stickers, ArrayList<TapbackInfo> tapbacks) {
		//Setting the values
		this.localID = localID;
		this.guid = guid;
		this.messageInfo = messageInfo;
		
		//Setting the modifiers
		this.stickers = stickers;
		this.tapbacks = tapbacks;
	}
	
	public void setViewHolderSource(Constants.ViewHolderSource<VH> viewHolderSource) {
		this.viewHolderSource = viewHolderSource;
	}
	
	public VH getViewHolder() {
		if(viewHolderSource == null) return null;
		return viewHolderSource.get();
	}
	
	public static int getNextItemViewType() {
		return nextItemViewType++;
	}
	
	public abstract int getItemViewType();
	
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
	
	public void setMessageInfo(MessageInfo messageInfo) {
		this.messageInfo = messageInfo;
	}
	
	public MessageInfo getMessageInfo() {
		return messageInfo;
	}
	
	public abstract void bindView(VH viewHolder, Context context);
	
	public void buildCommonViews(VH viewHolder, Context context) {
		//Building the sticker view
		buildStickerView(viewHolder, context);
		
		//Building the tapback view
		buildTapbackView(viewHolder, context);
	}
	
	public abstract void updateViewColor(VH viewHolder, Context context);
	
	public abstract void updateViewEdges(VH viewHolder, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored);
	
	public List<StickerInfo> getStickers() {
		return stickers;
	}
	
	public List<TapbackInfo> getTapbacks() {
		return tapbacks;
	}
	
	public void buildStickerView(VH viewHolder, Context context) {
		//Clearing all previous stickers
		viewHolder.stickerContainer.removeAllViews();
		
		//Weakly referencing the context
		final WeakReference<Context> contextReference = new WeakReference<>(context);
		
		//Iterating over the stickers
		for(StickerInfo sticker : stickers) {
			//Decoding the sticker
			MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromDBSticker(sticker.getGuid(), sticker.getLocalID(), new BitmapCacheHelper.ImageDecodeResult() {
				@Override
				public void onImageMeasured(int width, int height) {}
				
				@Override
				public void onImageDecoded(Bitmap result, boolean wasTasked) {
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
	
	public void addSticker(StickerInfo sticker) {
		//Adding the sticker to the sticker list
		stickers.add(sticker);
	}
	
	public void addLiveSticker(StickerInfo sticker, Context context) {
		//Adding the sticker to the sticker list
		stickers.add(sticker);
		
		//Creating a weak reference to the context
		final WeakReference<Context> contextReference = new WeakReference<>(context);
		
		//Decoding the sticker
		MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromDBSticker(sticker.getGuid(), sticker.getLocalID(), new BitmapCacheHelper.ImageDecodeResult() {
			@Override
			public void onImageMeasured(int width, int height) {}
			
			@Override
			public void onImageDecoded(Bitmap result, boolean wasTasked) {
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
	
	public boolean getRequiredStickerVisibility() {
		return !contextMenuOpen;
	}
	
	public void updateStickerVisibility() {
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
	
	public void addTapback(TapbackInfo tapback) {
		//Updating the tapback if it exists
		for(TapbackInfo allTapbacks : tapbacks) {
			if(tapback.getMessageIndex() == allTapbacks.getMessageIndex() && Objects.equals(tapback.getSender(), allTapbacks.getSender())) {
				allTapbacks.setCode(tapback.getCode());
				return;
			}
		}
		
		//Adding the tapback
		tapbacks.add(tapback);
	}
	
	public void addLiveTapback(TapbackInfo tapback, Context context) {
		//Adding the tapback
		addTapback(tapback);
		
		//Rebuilding the tapback view
		VH viewHolder = getViewHolder();
		if(viewHolder != null) buildTapbackView(viewHolder, context);
	}
	
	public void removeTapback(String sender) {
		//Removing the first matching tapback
		for(Iterator<TapbackInfo> iterator = tapbacks.iterator(); iterator.hasNext();) if(Objects.equals(sender, iterator.next().getSender())) {
			iterator.remove();
			break;
		}
	}
	
	public void removeLiveTapback(String sender, Context context) {
		//Removing the tapback
		removeTapback(sender);
		
		//Rebuilding the tapback view
		VH viewHolder = getViewHolder();
		if(viewHolder != null) buildTapbackView(viewHolder, context);
	}
	
	public void buildTapbackView(VH viewHolder, Context context) {
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
	
	public abstract VH createViewHolder(Context context, ViewGroup parent);
	
	public void setMessagePreviewState(int state) {
		messagePreviewState = state;
	}
	
	public int getMessagePreviewState() {
		return messagePreviewState;
	}
	
	public void setMessagePreviewID(long id) {
		messagePreviewID = id;
	}
	
	public void setMessagePreviewLoading(boolean messagePreviewLoading) {
		this.messagePreviewLoading = messagePreviewLoading;
	}
	
	public boolean isMessagePreviewLoading() {
		return messagePreviewLoading;
	}
	
	/**
	 * Assign this message a preview, to be displayed underneath the original message contents
	 * @param preview the preview to be displayed, NULL if unavailable
	 */
	public void setMessagePreview(MessagePreviewInfo preview) {
		if(preview == null) {
			//Setting the message preview state
			messagePreviewState = MessagePreviewInfo.stateUnavailable;
		} else {
			//Setting the message preview
			messagePreviewState = MessagePreviewInfo.stateAvailable;
			messagePreviewReference = new WeakReference<>(preview);
			
			//Adding the preview view
			addMessagePreviewView(preview);
		}
	}
	
	/**
	 * Apply the message preview from an asynchronous load
	 * @param preview the preview to be displayed, NULL if unavailable
	 */
	public void applyMessagePreview(MessagePreviewInfo preview) {
		//Setting the preview as not loading
		messagePreviewLoading = false;
		
		//Returning if the preview is invalid
		if(preview == null) return;
		
		//Notifying the callback listeners
		//for(MessagePreviewCallback callback : messagePreviewCallbackList) callback.onResult(preview, true);
		//messagePreviewCallbackList.clear();
		
		//Adding the preview view
		addMessagePreviewView(preview);
		
		//Setting the reference
		messagePreviewReference = new WeakReference<>(preview);
	}
	
	/**
	 * Retrieve a copy of the message preview information
	 * This function will return an instance of the preview information immediately if it is available in memory
	 * Otherwise, it will fetch it from disk and return with applyMessagePreview, which calls addMessagePreviewView for each message component to override individually
	 * @return the instance of the message preview info, if available
	 */
	MessagePreviewInfo getLoadMessagePreview() {
		//Returning the preview immediately if there is one available in memory
		if(messagePreviewReference != null) {
			MessagePreviewInfo preview = messagePreviewReference.get();
			if(preview != null) return preview;
		}
		
		//Starting the loading task (if it hasn't already been started)
		if(!messagePreviewLoading) {
			new LoadMessagePreviewAsyncTask(this).execute();
			messagePreviewLoading = true;
		}
		
		//Returning null (to signify that the result will be returned later)
		return null;
	}
	
	private static class LoadMessagePreviewAsyncTask extends AsyncTask<Void, Void, MessagePreviewInfo> {
		private final WeakReference<MessageComponent> messageComponentReference;
		private final long targetID;
		
		LoadMessagePreviewAsyncTask(MessageComponent messageComponent) {
			messageComponentReference = new WeakReference<>(messageComponent);
			targetID = messageComponent.messagePreviewID;
		}
		
		@Override
		protected MessagePreviewInfo doInBackground(Void... voids) {
			return DatabaseManager.getInstance().loadMessagePreview(targetID);
		}
		
		@Override
		protected void onPostExecute(MessagePreviewInfo preview) {
			//Applying the preview
			MessageComponent messageComponent = messageComponentReference.get();
			if(messageComponent != null) messageComponent.applyMessagePreview(preview);
		}
	}
	
	public void addMessagePreviewView(MessagePreviewInfo preview) {
	
	}
	
	public static abstract class ViewHolder extends RecyclerView.ViewHolder {
		final ViewGroup groupContainer;
		
		final ViewGroup stickerContainer;
		final ViewGroup tapbackContainer;
		
		final ViewGroup messagePreviewContainer;
		
		private MessagePreviewInfo.ViewHolder currentPreviewVH = null;
		
		public ViewHolder(View view) {
			super(view);
			
			groupContainer = view.findViewById(R.id.container);
			
			stickerContainer = view.findViewById(R.id.sticker_container);
			tapbackContainer = view.findViewById(R.id.tapback_container);
			
			messagePreviewContainer = view.findViewById(R.id.group_messagepreview);
		}
		
		public MessagePreviewInfo.ViewHolder getCurrentPreviewVH() {
			return currentPreviewVH;
		}
		
		public void setCurrentPreviewVH(MessagePreviewInfo.ViewHolder viewHolder) {
			if(currentPreviewVH != null) currentPreviewVH.setVisibility(false);
			currentPreviewVH = viewHolder;
			if(viewHolder != null) viewHolder.setVisibility(true);
			messagePreviewContainer.setVisibility(viewHolder != null ? View.VISIBLE : View.GONE);
		}
		
		public void releaseResources() {}
		
		public void cleanupState() {}
		
		public void pause() {}
		
		public void resume() {}
	}
	
	/**
	 * Returns the view to use when this component is involved in shared element transitions
	 * @return The view to animate
	 */
	public View getSharedElementView() {
		return null;
	}
	
	/**
	 * Returns the color that this message should take, depending on the current service
	 * This function does not take the advanced conversation coloring preference into account
	 * @param resources The resources to use to fetch the color
	 * @return The current service's color
	 */
	int getServiceColor(Resources resources) {
		return ConversationInfo.getServiceColor(resources, getMessageInfo().getConversationInfo().getServiceHandler(), getMessageInfo().getConversationInfo().getService());
	}
}
