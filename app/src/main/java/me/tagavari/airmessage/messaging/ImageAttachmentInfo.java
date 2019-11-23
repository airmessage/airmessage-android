package me.tagavari.airmessage.messaging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;

import jp.wasabeef.glide.transformations.BlurTransformation;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.view.InvisibleInkView;
import me.tagavari.airmessage.view.RoundedImageView;

public class ImageAttachmentInfo extends AttachmentInfo<ImageAttachmentInfo.ViewHolder> {
	//Creating the reference values
	public static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
	public static final String MIME_TYPE = "image/*";
	public static final int RESOURCE_NAME = R.string.part_content_image;
	
	public ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
		super(localID, guid, message, fileName, fileType, fileSize);
	}
	
	public ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
		super(localID, guid, message, fileName, fileType, fileSize, file);
	}
	
	public ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
		super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
	}
	
	public ImageAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
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
	
	public static boolean checkFileApplicability(String fileType, String fileName) {
		return Constants.compareMimeTypes(MIME_TYPE, fileType);
	}
	
	@SuppressLint("CheckResult")
	@Override
	public void updateContentView(ViewHolder viewHolder, Context context) {
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
					//.apply(RequestOptions.placeholderOf(new ColorDrawable(context.getResources().getServiceColor(R.color.colorImageUnloaded, null))));
			requestBuilder.addListener(new RequestListener<Drawable>() {
				@Override
				public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
					return false;
				}
				
				@Override
				public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
					viewHolder.imageContent.post(viewHolder.imageContent::requestLayout);
					return false;
				}
			});
			if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) requestBuilder.apply(RequestOptions.bitmapTransform(new BlurTransformation(ConversationUtils.invisibleInkBlurRadius, ConversationUtils.invisibleInkBlurSampling)));
			requestBuilder.into(viewHolder.imageContent);
		}
		
		//Updating the ink view
		if(Constants.appleSendStyleBubbleInvisibleInk.equals(getMessageInfo().getSendStyle())) {
			viewHolder.inkView.setVisibility(View.VISIBLE);
			viewHolder.inkView.setState(true);
		} else viewHolder.inkView.setVisibility(View.GONE);
		
		//Revealing the layout
		viewHolder.groupContentFrame.setVisibility(View.VISIBLE);
		
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
					newViewHolder.groupContentFrame.getLayoutParams().width = (int) (width * multiplier);
					newViewHolder.groupContentFrame.getLayoutParams().height = (int) (height * multiplier);
					
					//Showing the layout
					newViewHolder.groupContentFrame.setVisibility(View.VISIBLE);
					
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
					newViewHolder.groupContentFrame.getLayoutParams().width = (int) (width * multiplier);
					newViewHolder.groupContentFrame.getLayoutParams().height = (int) (height * multiplier);
					
					//Showing the layout
					newViewHolder.groupContentFrame.setVisibility(View.VISIBLE);
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
						newViewHolder.groupContentFrame.setVisibility(View.GONE);
						newViewHolder.groupFailed.setVisibility(View.VISIBLE);
					} else {
						//Configuring the layout
						//ViewGroup.LayoutParams params = content.getLayoutParams();
						
						float multiplier = Constants.calculateImageAttachmentMultiplier(context.getResources(), bitmap.getWidth(), bitmap.getHeight());
						newViewHolder.groupContentFrame.getLayoutParams().width = (int) (bitmap.getWidth() * multiplier);
						newViewHolder.groupContentFrame.getLayoutParams().height = (int) (bitmap.getHeight() * multiplier);
						//content.setLayoutParams(params);
						
						//Showing the layout
						newViewHolder.groupContentFrame.setVisibility(View.VISIBLE);
						
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
	public void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
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
		viewHolder.imageContent.invalidate();
		viewHolder.backgroundView.setBackground(backgroundDrawable);
	}
	
	@Override
	public void onClick(Messaging activity) {
		View transitionView = null;
		float[] radiiRaw = new float[8];
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) {
			transitionView = viewHolder.imageContent;
			radiiRaw = viewHolder.imageContent.getRadiiRaw();
		}
		
		openAttachmentFileMediaViewer(activity, transitionView, radiiRaw);
	}
	
	@Override
	public int getItemViewType() {
		return ITEM_VIEW_TYPE;
	}
	
	@Override
	public int getResourceTypeName() {
		return RESOURCE_NAME;
	}
	
	@Override
	public ViewHolder createViewHolder(Context context, ViewGroup parent) {
		return new ViewHolder(buildAttachmentView(LayoutInflater.from(context), parent, R.layout.listitem_contentimage));
	}
	
	@Override
	public View getSharedElementView() {
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder == null) return null;
		return viewHolder.imageContent;
	}
	
	public static class ViewHolder extends AttachmentInfo.ViewHolder {
		final ViewGroup groupContent;
		final RoundedImageView imageContent;
		final InvisibleInkView inkView;
		final View backgroundView;
		
		public ViewHolder(View view) {
			super(view);
			
			groupContent = groupContentFrame.findViewById(R.id.content);
			imageContent = groupContent.findViewById(R.id.content_view);
			inkView = groupContent.findViewById(R.id.content_ink);
			backgroundView = groupContent.findViewById(R.id.content_background);
		}
		
		@Override
		public void cleanupState() {
			inkView.setState(false);
		}
		
		@Override
		public void pause() {
			//inkView.onPause();
		}
		
		@Override
		public void resume() {
			//inkView.onResume();
		}
		
		@Override
		public void releaseResources() {
			imageContent.setImageBitmap(null);
		}
	}
}
