package me.tagavari.airmessage.messaging.viewbinder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.CollectionHelper;
import me.tagavari.airmessage.helper.LanguageHelper;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.util.TapbackDisplayData;

public class VBMessageComponent {
	/**
	 * Rebuilds the sticker view for a component
	 * @param context The context to use
	 * @param stickers The list of stickers to display
	 * @param stickerContainer The view container to add the sticker views to
	 */
	public static void buildStickerView(Context context, List<StickerInfo> stickers, ViewGroup stickerContainer) {
		//Clearing all previous stickers
		stickerContainer.removeAllViews();
		
		//Determining the maximum image size
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
		int maxStickerSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) / 3; //One third of the smaller side of the display
		
		for(StickerInfo sticker : stickers) {
			//Creating the image view
			ImageView imageView = new ImageView(context);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			imageView.setLayoutParams(layoutParams);
			imageView.setMaxWidth(maxStickerSize);
			imageView.setMaxHeight(maxStickerSize);
			Glide.with(context).load(sticker.getFile()).into(imageView);
			
			//Adding the view to the sticker container
			stickerContainer.addView(imageView);
		}
		
		/* return Observable.fromIterable(stickers)
				.flatMapSingle(sticker -> MainApplication.getInstance().getBitmapCacheHelper().getSticker(context, sticker.getLocalID()))
				.observeOn(AndroidSchedulers.mainThread())
				.doOnNext(bitmap -> {
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
					imageView.setImageBitmap(bitmap);
					
					//Adding the view to the sticker container
					stickerContainer.addView(imageView);
				})
				.ignoreElements(); */
	}
	
	/**
	 * Adds and animates the entry of a sticker view for a component
	 * @param context The context to use
	 * @param sticker The sticker to add
	 * @param stickerContainer The view container to add the sticker view to
	 */
	public static void addStickerView(Context context, StickerInfo sticker, ViewGroup stickerContainer) {
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
		Glide.with(context)
				.load(sticker.getFile())
				.listener(new RequestListener<Drawable>() {
					@Override
					public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
						return false;
					}
					
					@Override
					public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
						//Animating the image view
						ScaleAnimation anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5F, Animation.RELATIVE_TO_SELF, 0.5F);
						anim.setDuration(500);
						anim.setInterpolator(new OvershootInterpolator());
						imageView.startAnimation(anim);
						
						return false;
					}
				})
				.into(imageView);
		
		//Adding the view to the sticker container
		stickerContainer.addView(imageView);
	}
	
	/**
	 * Rebuilds the tapback view for a component
	 * @param context The context to use
	 * @param tapbacks The list of tapbacks to display
	 * @param tapbackContainer The view container to add the tapback views to
	 */
	public static void buildTapbackView(Context context, List<TapbackInfo> tapbacks, ViewGroup tapbackContainer) {
		//Clearing all previous tapbacks
		tapbackContainer.removeAllViews();
		
		//Returning if there are no tapbacks
		if(tapbacks.isEmpty()) return;
		
		//Counting the associated tapbacks
		Map<Integer, Integer> tapbackCounts = new HashMap<>();
		for(TapbackInfo tapback : tapbacks) {
			if(tapbackCounts.containsKey(tapback.getCode())) tapbackCounts.put(tapback.getCode(), tapbackCounts.get(tapback.getCode()) + 1);
			else tapbackCounts.put(tapback.getCode(), 1);
		}
		
		//Sorting the tapback counts by value (descending)
		tapbackCounts = CollectionHelper.sortMapByValueDesc(tapbackCounts);
		
		//Iterating over the tapback groups
		for(Map.Entry<Integer, Integer> entry : tapbackCounts.entrySet()) {
			//Inflating the view
			View tapbackView = LayoutInflater.from(context).inflate(R.layout.chip_tapback, tapbackContainer, false);
			
			//Getting the display info
			TapbackDisplayData displayInfo = LanguageHelper.getTapbackDisplay(entry.getKey());
			
			//Getting the count text
			TextView count = tapbackView.findViewById(R.id.label_count);
			
			//Setting the count
			count.setText(LanguageHelper.intToFormattedString(context.getResources(), entry.getValue()));
			
			//Checking if the display info is valid
			if(displayInfo != null) {
				//Setting the icon drawable and color
				ImageView icon = tapbackView.findViewById(R.id.icon);
				icon.setImageResource(displayInfo.getIconResource());
				icon.setImageTintList(ColorStateList.valueOf(context.getColor(displayInfo.getColor())));
				
				//Setting the text color
				count.setTextColor(context.getColor(displayInfo.getColor()));
			}
			
			//Adding the view to the container
			tapbackContainer.addView(tapbackView);
		}
	}
}