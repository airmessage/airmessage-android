package me.tagavari.airmessage.messaging.viewholder;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.messaging.MessageComponent;
import me.tagavari.airmessage.util.DisposableViewHolder;

public class VHMessageStructure extends DisposableViewHolder {
	public final TextView labelTimeDivider;
	public final TextView labelSender;
	
	private boolean isProfileInflated = false;
	private ViewStub profileStub;
	public ViewGroup profileGroup = null;
	private final @IdRes int idProfileDefault;
	public ImageView profileDefault = null;
	private final @IdRes int idProfileImage;
	public ImageView profileImage = null;
	
	public final ViewGroup containerMessagePart;
	
	public final TextSwitcher labelActivityStatus;
	public final View buttonSendEffectReplay;
	public final ImageButton buttonSendError;
	
	public final List<VHMessageComponent> messageComponents = new ArrayList<>();
	
	public VHMessageStructure(View itemView, TextView labelTimeDivider, TextView labelSender, ViewStub profileStub, ViewGroup containerMessagePart, @IdRes int idProfileDefault, @IdRes int idProfileImage, TextSwitcher labelActivityStatus, View buttonSendEffectReplay, ImageButton buttonSendError) {
		super(itemView);
		
		this.labelTimeDivider = labelTimeDivider;
		this.labelSender = labelSender;
		this.profileStub = profileStub;
		this.containerMessagePart = containerMessagePart;
		this.idProfileDefault = idProfileDefault;
		this.idProfileImage = idProfileImage;
		this.labelActivityStatus = labelActivityStatus;
		this.buttonSendEffectReplay = buttonSendEffectReplay;
		this.buttonSendError = buttonSendError;
	}
	
	/**
	 * Gets if the profile view stub of this view is inflated
	 */
	public boolean isProfileInflated() {
		return isProfileInflated;
	}
	
	/**
	 * Inflates the profile view stub of this view
	 */
	public void inflateProfile() {
		if(isProfileInflated) return;
		
		profileGroup = (ViewGroup) profileStub.inflate();
		profileStub = null;
		profileDefault = profileGroup.findViewById(idProfileDefault);
		profileImage = profileGroup.findViewById(idProfileImage);
		
		isProfileInflated = true;
	}
}