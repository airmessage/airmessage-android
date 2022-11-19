package me.tagavari.airmessage.messaging.viewbinder

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.common.data.UserCacheHelper
import me.tagavari.airmessage.common.helper.ConversationBuildHelper.buildConversationTitle
import me.tagavari.airmessage.common.helper.ConversationBuildHelper.buildConversationTitleDirect
import me.tagavari.airmessage.common.helper.LanguageHelper.getLastUpdateStatusTime
import me.tagavari.airmessage.common.helper.LanguageHelper.intToFormattedString
import me.tagavari.airmessage.common.helper.ResourceHelper.resolveColorAttr
import me.tagavari.airmessage.common.messaging.ConversationInfo
import me.tagavari.airmessage.common.messaging.ConversationPreview
import me.tagavari.airmessage.common.messaging.MemberInfo
import me.tagavari.airmessage.common.util.IndexedItem
import java.util.*

/**
 * Binds conversation data to a view holder
 */
object VBConversation {
	private val TAG = VBConversation::class.java.simpleName
	private const val maxUsersToDisplay = 4
	
	/**
	 * Binds a conversation's members to an icon group
	 * @param context The context to use
	 * @param iconGroup A view representing a group of members
	 * @param conversationInfo The conversation to display
	 * @return A completable representing this task's state
	 */
	@JvmStatic
	fun bindUsers(context: Context, iconGroup: ViewGroup, conversationInfo: ConversationInfo): Completable {
		return bindUsers(context, iconGroup, conversationInfo.members)
	}
	
	/**
	 * Binds a list of conversation members to an icon group
	 * @param context The context to use
	 * @param iconGroup A view representing a group of members
	 * @param members A list of members to display
	 * @return A completable representing this task's state
	 */
	@JvmStatic
	fun bindUsers(context: Context, iconGroup: ViewGroup, members: List<MemberInfo>): Completable {
		if(members.isEmpty()) {
			//Hide all views
			for(i in 0 until maxUsersToDisplay) {
				val child = iconGroup.getChildAt(i)
				if(child is ViewGroup) child.visibility = View.GONE
			}
			
			return Completable.complete()
		}
		
		//Getting the view data
		val usersToDisplay = members.size.coerceAtMost(maxUsersToDisplay)
		val viewIndex = usersToDisplay - 1
		val viewAtIndex = iconGroup.getChildAt(viewIndex)
		
		//Retrieving the target view, inflating if necessary
		val iconView: ViewGroup
		if(viewAtIndex is ViewStub) {
			iconView = viewAtIndex.inflate() as ViewGroup
		} else {
			iconView = viewAtIndex as ViewGroup
			iconView.visibility = View.VISIBLE
		}
		
		//Hiding all other views
		for(i in 0 until maxUsersToDisplay) {
			if(i == viewIndex) continue
			
			val child = iconGroup.getChildAt(i)
			if(child is ViewGroup) child.visibility = View.GONE
		}
		
		//Getting user data for each member
		return Observable.range(0, usersToDisplay)
			//Map to an object containing each member and their index
			.map { i -> IndexedItem(i, members[i]) }
			.doOnNext { data: IndexedItem<MemberInfo> ->
				//Getting the child view
				val child = iconView.getChildAt(data.index)
				
				//Getting the views
				val imageDefault = child.findViewById<ImageView>(R.id.profile_default)
				val imageProfile = child.findViewById<ImageView>(R.id.profile_image)
				
				//Setting the default profile tint
				imageDefault.visibility = View.VISIBLE
				imageDefault.setColorFilter(data.item.color, PorterDuff.Mode.MULTIPLY)
				
				//Resetting the contact image
				imageProfile.setImageBitmap(null)
			}
			//Get user info for each member
			.flatMapSingle { data: IndexedItem<MemberInfo> ->
				MainApplication.instance.userCacheHelper.getUserInfo(context, data.item.address)
					.map { Optional.of(it) }
					.onErrorReturnItem(Optional.empty())
					.map { optionalUserInfo -> IndexedItem(data.index, Pair(data.item, optionalUserInfo.orElse(null))) }
			}
			.doOnNext { data: IndexedItem<Pair<MemberInfo, UserCacheHelper.UserInfo?>> ->
				//Getting the child view
				val child = iconView.getChildAt(data.index)
				
				//Getting the views
				val imageDefault = child.findViewById<ImageView>(R.id.profile_default)
				val imageProfile = child.findViewById<ImageView>(R.id.profile_image)
				
				//Checking if a user was found
				if(data.item.second != null) {
					//Loading the user's picture
					Glide.with(context)
						.load(data.item.second!!.thumbnailURI)
						.listener(object : RequestListener<Drawable?> {
							override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
								return false
							}
							
							override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
								//Swapping to the profile view
								imageDefault.visibility = View.GONE
								imageProfile.visibility = View.VISIBLE
								return false
							}
						})
						.into(imageProfile)
				}
			} //We're done
			.ignoreElements()
	}
	
	/**
	 * Binds a conversation's title to a TextView
	 * @param context The context to use
	 * @param label The TextView to use
	 * @param conversationInfo The conversation to display
	 * @return A completable representing this task's state
	 */
	@JvmStatic
	fun bindTitle(context: Context, label: TextView, conversationInfo: ConversationInfo): Completable {
		//Apply a temporary title right away
		label.text = buildConversationTitleDirect(context, conversationInfo)
		
		//Apply a title with member names asynchronously
		return buildConversationTitle(context, conversationInfo)
			.doOnSuccess { label.text = it }
			.ignoreElement()
	}
	
	/**
	 * Binds a conversation's unread status
	 * @param context The context to use
	 * @param labelTitle The TextView of the conversation's title
	 * @param labelMessage The TextView of the conversation's preview message
	 * @param labelUnread The TextView of the conversation's unread indicator
	 * @param unreadCount The amount of unread messages; set to 0 for none
	 */
	@JvmStatic
	fun bindUnreadStatus(context: Context, labelTitle: TextView, labelMessage: TextView, labelUnread: TextView, unreadCount: Int) {
		if(unreadCount > 0) {
			labelTitle.setTypeface(null, Typeface.BOLD)
			labelTitle.setTextColor(context.resources.getColor(R.color.colorPrimary, null))
			
			labelMessage.setTypeface(null, Typeface.BOLD)
			labelMessage.setTextColor(resolveColorAttr(context, android.R.attr.textColorPrimary))
			
			labelUnread.visibility = View.VISIBLE
			labelUnread.text = intToFormattedString(context.resources, unreadCount)
		} else {
			labelTitle.setTypeface(null, Typeface.NORMAL)
			labelTitle.setTextColor(resolveColorAttr(context, android.R.attr.textColorPrimary))
			
			labelMessage.setTypeface(null, Typeface.NORMAL)
			labelMessage.setTextColor(resolveColorAttr(context, android.R.attr.textColorSecondary))
			
			labelUnread.visibility = View.GONE
		}
	}
	
	/**
	 * Binds a conversation's preview section
	 * @param context The context to use
	 * @param labelMessage The TextView of the conversation's preview message
	 * @param labelStatus The TextView of the conversation's status
	 * @param preview The preview item to display
	 */
	@JvmStatic
	fun bindPreview(context: Context, labelMessage: TextView, labelStatus: TextView, preview: ConversationPreview?) {
		if(preview == null) {
			//Set everything to "unknown"
			labelMessage.setText(R.string.part_unknown)
			labelStatus.setText(R.string.part_unknown)
			labelStatus.setTextColor(resolveColorAttr(context, android.R.attr.textColorSecondary))
			
			return
		}
		
		//Setting the message preview
		labelMessage.text = preview.buildString(context)
		
		if(preview is ConversationPreview.Message && preview.isError) {
			//Setting the status to "not sent"
			labelStatus.setText(R.string.message_senderror)
			labelStatus.setTextColor(context.resources.getColor(R.color.colorError, null))
		} else {
			//Setting the status to the preview time
			labelStatus.text = getLastUpdateStatusTime(context, preview.date)
			labelStatus.setTextColor(resolveColorAttr(context, android.R.attr.textColorSecondary))
		}
	}
	
	/**
	 * Binds a conversation's selected indicator
	 * @param mainView The view of the conversation
	 * @param iconGroup A view representing the conversation's group of members
	 * @param selectionIndicator The conversation's selection indicator view
	 * @param isSelected Whether this conversation is selected
	 * @param animate Whether to animate this change
	 */
	@JvmStatic
	fun bindSelectionIndicator(mainView: View, iconGroup: ViewGroup, selectionIndicator: View, selectionTint: View, isSelected: Boolean, animate: Boolean) {
		mainView.isSelected = isSelected
		
		if(animate) {
			if(isSelected) {
				iconGroup.animate().alpha(0f).withEndAction { iconGroup.visibility = View.INVISIBLE }
				selectionIndicator.animate().alpha(1f).withStartAction { selectionIndicator.visibility = View.VISIBLE }
				selectionTint.animate().alpha(1f).withStartAction { selectionTint.visibility = View.VISIBLE }
			} else {
				iconGroup.animate().alpha(1f).withStartAction { iconGroup.visibility = View.VISIBLE }
				selectionIndicator.animate().alpha(0f).withEndAction { selectionIndicator.visibility = View.GONE }
				selectionTint.animate().alpha(0f).withEndAction { selectionTint.visibility = View.GONE }
			}
		} else {
			if(isSelected) {
				iconGroup.visibility = View.INVISIBLE
				iconGroup.alpha = 0f
				
				selectionIndicator.visibility = View.VISIBLE
				selectionIndicator.alpha = 1f
				selectionTint.visibility = View.VISIBLE
				selectionTint.alpha = 1f
			} else {
				iconGroup.visibility = View.VISIBLE
				iconGroup.alpha = 1f
				
				selectionIndicator.visibility = View.GONE
				selectionIndicator.alpha = 0f
				selectionTint.visibility = View.GONE
				selectionTint.alpha = 0f
			}
		}
	}
}