package me.tagavari.airmessage.messaging.viewbinder

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.CollectionHelper.sortMapByValueDesc
import me.tagavari.airmessage.helper.LanguageHelper.getTapbackDisplay
import me.tagavari.airmessage.helper.LanguageHelper.intToFormattedString
import me.tagavari.airmessage.messaging.StickerInfo
import me.tagavari.airmessage.messaging.TapbackInfo

object VBMessageComponent {
	/**
	 * Rebuilds the sticker view for a component
	 * @param activity The activity context to use
	 * @param stickers The list of stickers to display
	 * @param stickerContainer The view container to add the sticker views to
	 */
	@kotlin.jvm.JvmStatic
	fun buildStickerView(activity: Activity, stickers: List<StickerInfo>, stickerContainer: ViewGroup) {
		//Clearing all previous stickers
		stickerContainer.removeAllViews()
		
		val decorView = activity.window.decorView
		val maxStickerSize = decorView.width.coerceAtMost(decorView.height) / 3 //One third of the smaller side of the display
		
		for(sticker in stickers) {
			//Creating the image view
			val imageView = ImageView(activity)
				.apply {
					layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
						.apply { addRule(RelativeLayout.CENTER_IN_PARENT) }
					maxWidth = maxStickerSize
					maxHeight = maxStickerSize
					adjustViewBounds = true
				}
			
			//Loading the image
			Glide.with(activity).load(sticker.file).into(imageView)
			
			//Adding the view to the sticker container
			stickerContainer.addView(imageView)
		}
	}
	
	/**
	 * Adds and animates the entry of a sticker view for a component
	 * @param activity The activity context to use
	 * @param sticker The sticker to add
	 * @param stickerContainer The view container to add the sticker view to
	 */
	@kotlin.jvm.JvmStatic
	fun addStickerView(activity: Activity, sticker: StickerInfo, stickerContainer: ViewGroup) {
		val decorView = activity.window.decorView
		val maxStickerSize = decorView.width.coerceAtMost(decorView.height) / 3 //One third of the smaller side of the display
		
		//Creating the image view
		val imageView = ImageView(activity)
			.apply {
				layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
					.apply { addRule(RelativeLayout.CENTER_IN_PARENT) }
				maxWidth = maxStickerSize
				maxHeight = maxStickerSize
				adjustViewBounds = true
			}
		
		//Loading the image
		Glide.with(activity)
			.load(sticker.file)
			.listener(object : RequestListener<Drawable?> {
				override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
					return false
				}
				
				override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean
				): Boolean {
					//Animating the image view
					imageView.startAnimation(
						ScaleAnimation(0F, 1F, 0F, 1F, Animation.RELATIVE_TO_SELF, 0.5F, Animation.RELATIVE_TO_SELF, 0.5F).apply {
							duration = 500
							interpolator = OvershootInterpolator()
						}
					)
					return false
				}
			})
			.into(imageView)
		
		//Adding the view to the sticker container
		stickerContainer.addView(imageView)
	}
	
	/**
	 * Rebuilds the tapback view for a component
	 * @param context The context to use
	 * @param tapbacks The list of tapbacks to display
	 * @param tapbackContainer The view container to add the tapback views to
	 */
	@JvmStatic
	fun buildTapbackView(context: Context, tapbacks: List<TapbackInfo>, tapbackContainer: ViewGroup) {
		//Clearing all previous tapbacks
		tapbackContainer.removeAllViews()
		
		//Returning if there are no tapbacks
		if(tapbacks.isEmpty()) return
		
		//Counting the associated tapbacks
		var tapbackCounts: MutableMap<Int?, Int> = HashMap()
		for(tapback in tapbacks) {
			if(tapbackCounts.containsKey(tapback.code)) tapbackCounts[tapback.code] =
				tapbackCounts[tapback.code]!! + 1 else tapbackCounts[tapback.code] = 1
		}
		
		//Sorting the tapback counts by value (descending)
		tapbackCounts = sortMapByValueDesc(tapbackCounts)
		
		//Iterating over the tapback groups
		for((key, value) in tapbackCounts) {
			//Inflating the view
			val tapbackView = LayoutInflater.from(context).inflate(R.layout.chip_tapback, tapbackContainer, false)
			
			//Getting the display info
			val displayInfo = getTapbackDisplay(key!!)
			
			//Getting the count text
			val count = tapbackView.findViewById<TextView>(R.id.label_count)
			
			//Setting the count
			count.text = intToFormattedString(context.resources, value)
			
			//Checking if the display info is valid
			if(displayInfo != null) {
				//Setting the icon drawable and color
				val icon = tapbackView.findViewById<ImageView>(R.id.icon)
				icon.setImageResource(displayInfo.iconResource)
				icon.imageTintList = ColorStateList.valueOf(context.getColor(displayInfo.color))
				
				//Setting the text color
				count.setTextColor(context.getColor(displayInfo.color))
			}
			
			//Adding the view to the container
			tapbackContainer.addView(tapbackView)
		}
	}
}