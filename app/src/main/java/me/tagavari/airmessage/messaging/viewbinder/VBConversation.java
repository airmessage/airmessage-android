package me.tagavari.airmessage.messaging.viewbinder;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.ContactHelper;
import me.tagavari.airmessage.helper.ConversationBuildHelper;
import me.tagavari.airmessage.helper.LanguageHelper;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.util.IndexedItem;

/**
 * Binds conversation data to a view holder
 */
public class VBConversation {
	private static final String TAG = VBConversation.class.getSimpleName();
	
	private static final int maxUsersToDisplay = 4;
	
	/**
	 * Binds a conversation's members to an icon group
	 * @param context The context to use
	 * @param iconGroup A view representing a group of members
	 * @param conversationInfo The conversation to display
	 * @return A completable representing this task's state
	 */
	public static Completable bindUsers(Context context, ViewGroup iconGroup, ConversationInfo conversationInfo) {
		return bindUsers(context, iconGroup, conversationInfo.getMembers());
	}
	
	/**
	 * Binds a list of conversation members to an icon group
	 * @param context The context to use
	 * @param iconGroup A view representing a group of members
	 * @param members A list of members to display
	 * @return A completable representing this task's state
	 */
	public static Completable bindUsers(Context context, ViewGroup iconGroup, List<MemberInfo> members) {
		if(members.isEmpty()) {
			//Hide all views
			for(int i = 0; i < maxUsersToDisplay; i++) {
				View child = iconGroup.getChildAt(i);
				if(child instanceof ViewGroup) child.setVisibility(View.GONE);
			}
			
			return Completable.complete();
		}
		
		//Getting the view data
		int usersToDisplay = Math.min(members.size(), maxUsersToDisplay);
		int viewIndex = usersToDisplay - 1;
		View viewAtIndex = iconGroup.getChildAt(viewIndex);
		
		//Retrieving the target view, inflating if necessary
		ViewGroup iconView;
		if(viewAtIndex instanceof ViewStub) iconView = (ViewGroup) ((ViewStub) viewAtIndex).inflate();
		else {
			iconView = (ViewGroup) viewAtIndex;
			iconView.setVisibility(View.VISIBLE);
		}
		
		//Hiding all other views
		for(int i = 0; i < maxUsersToDisplay; i++) {
			if(i == viewIndex) continue;
			View child = iconGroup.getChildAt(i);
			if(child instanceof ViewGroup) child.setVisibility(View.GONE);
		}
		
		//Getting user data for each member
		return Observable.range(0, usersToDisplay)
				//Map to an object containing each member and their index
				.map((index) -> new IndexedItem<>(index, members.get(index)))
				.doOnNext((data) -> {
					//Getting the child view
					View child = iconView.getChildAt(data.index);
					
					//Getting the views
					ImageView imageDefault = child.findViewById(R.id.profile_default);
					ImageView imageProfile = child.findViewById(R.id.profile_image);
					
					//Setting the default profile tint
					imageDefault.setVisibility(View.VISIBLE);
					imageDefault.setColorFilter(data.item.getColor(), PorterDuff.Mode.MULTIPLY);
					
					//Resetting the contact image
					imageProfile.setImageBitmap(null);
				})
				//Get user info for each member
				.flatMapSingle((data) ->
						MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, data.item.getAddress())
								.map(Optional::of)
								.onErrorReturnItem(Optional.empty())
								.map((optionalUserInfo) -> new IndexedItem<>(data.index, new Pair<>(data.item, optionalUserInfo.orElse(null)))))
				.doOnNext((data) -> {
					//Getting the child view
					View child = iconView.getChildAt(data.index);
					
					//Getting the views
					ImageView imageDefault = child.findViewById(R.id.profile_default);
					ImageView imageProfile = child.findViewById(R.id.profile_image);
					
					//Checking if a user was found
					if(data.item.second != null) {
						//Loading the user's picture
						Glide.with(context)
								.load(ContactHelper.getContactImageURI(data.item.second.getContactID()))
								.listener(new RequestListener<Drawable>() {
									@Override
									public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
										return false;
									}
									
									@Override
									public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
										//Swapping to the profile view
										imageDefault.setVisibility(View.GONE);
										imageProfile.setVisibility(View.VISIBLE);
										
										return false;
									}
								})
								.into(imageProfile);
					}
				})
				//We're done
				.ignoreElements();
	}
	
	/**
	 * Binds a conversation's title to a TextView
	 * @param context The context to use
	 * @param label The TextView to use
	 * @param conversationInfo The conversation to display
	 * @return A completable representing this task's state
	 */
	public static Completable bindTitle(Context context, TextView label, ConversationInfo conversationInfo) {
		//Apply a temporary title right away
		label.setText(ConversationBuildHelper.buildConversationTitleDirect(context, conversationInfo));
		
		//Apply a title with member names asynchronously
		return ConversationBuildHelper.buildConversationTitle(context, conversationInfo).doOnSuccess(label::setText).ignoreElement();
	}
	
	/**
	 * Binds a conversation's unread status
	 * @param context The context to use
	 * @param labelTitle The TextView of the conversation's title
	 * @param labelMessage The TextView of the conversation's preview message
	 * @param labelUnread The TextView of the conversation's unread indicator
	 * @param unreadCount The amount of unread messages; set to 0 for none
	 */
	public static void bindUnreadStatus(Context context, TextView labelTitle, TextView labelMessage, TextView labelUnread, int unreadCount) {
		if(unreadCount > 0) {
			labelTitle.setTypeface(null, Typeface.BOLD);
			labelTitle.setTextColor(context.getResources().getColor(R.color.colorPrimary, null));
			
			labelMessage.setTypeface(null, Typeface.BOLD);
			labelMessage.setTextColor(ResourceHelper.resolveColorAttr(context, android.R.attr.textColorPrimary));
			
			labelUnread.setVisibility(View.VISIBLE);
			labelUnread.setText(LanguageHelper.intToFormattedString(context.getResources(), unreadCount));
		} else {
			labelTitle.setTypeface(null, Typeface.NORMAL);
			labelTitle.setTextColor(ResourceHelper.resolveColorAttr(context, android.R.attr.textColorPrimary));
			
			labelMessage.setTypeface(null, Typeface.NORMAL);
			labelMessage.setTextColor(ResourceHelper.resolveColorAttr(context, android.R.attr.textColorSecondary));
			
			labelUnread.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Binds a conversation's preview section
	 * @param context The context to use
	 * @param labelMessage The TextView of the conversation's preview message
	 * @param labelStatus The TextView of the conversation's status
	 * @param preview The preview item to display
	 */
	public static void bindPreview(Context context, TextView labelMessage, TextView labelStatus, @Nullable ConversationPreview preview) {
		if(preview == null) {
			//Set everything to "unknown"
			labelMessage.setText(R.string.part_unknown);
			labelStatus.setText(R.string.part_unknown);
			labelStatus.setTextColor(ResourceHelper.resolveColorAttr(context, android.R.attr.textColorSecondary));
			
			return;
		}
		
		//Setting the message preview
		labelMessage.setText(preview.buildString(context));
		
		if(preview instanceof ConversationPreview.Message && ((ConversationPreview.Message) preview).isError()) {
			//Setting the status to "not sent"
			labelStatus.setText(R.string.message_senderror);
			labelStatus.setTextColor(context.getResources().getColor(R.color.colorError, null));
		} else {
			//Setting the status to the preview time
			labelStatus.setText(LanguageHelper.getLastUpdateStatusTime(context, preview.getDate()));
			labelStatus.setTextColor(ResourceHelper.resolveColorAttr(context, android.R.attr.textColorSecondary));
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
	public static void bindSelectionIndicator(View mainView, ViewGroup iconGroup, View selectionIndicator, View selectionTint, boolean isSelected, boolean animate) {
		mainView.setSelected(isSelected);
		
		if(animate) {
			if(isSelected) {
				iconGroup.animate().alpha(0).withEndAction(() -> iconGroup.setVisibility(View.INVISIBLE));
				selectionIndicator.animate().alpha(1).withStartAction(() -> selectionIndicator.setVisibility(View.VISIBLE));
				selectionTint.animate().alpha(1).withStartAction(() -> selectionTint.setVisibility(View.VISIBLE));
			} else {
				iconGroup.animate().alpha(1).withStartAction(() -> iconGroup.setVisibility(View.VISIBLE));
				selectionIndicator.animate().alpha(0).withEndAction(() -> selectionIndicator.setVisibility(View.GONE));
				selectionTint.animate().alpha(0).withEndAction(() -> selectionTint.setVisibility(View.GONE));
			}
		} else {
			if(isSelected) {
				iconGroup.setVisibility(View.INVISIBLE);
				iconGroup.setAlpha(0);
				
				selectionIndicator.setVisibility(View.VISIBLE);
				selectionIndicator.setAlpha(1);
				selectionTint.setVisibility(View.VISIBLE);
				selectionTint.setAlpha(1);
			} else {
				iconGroup.setVisibility(View.VISIBLE);
				iconGroup.setAlpha(1);
				
				selectionIndicator.setVisibility(View.GONE);
				selectionIndicator.setAlpha(0);
				selectionTint.setVisibility(View.GONE);
				selectionTint.setAlpha(0);
			}
		}
	}
}