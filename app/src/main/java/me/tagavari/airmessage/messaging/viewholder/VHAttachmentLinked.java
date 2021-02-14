package me.tagavari.airmessage.messaging.viewholder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.File;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.LanguageHelper;
import me.tagavari.airmessage.util.DisposableViewHolder;
import me.tagavari.airmessage.util.Union;

/**
 * A view holder to handle different types of attachment tiles in a linked picker
 */
public class VHAttachmentLinked extends DisposableViewHolder {
	//Creating the constants
	private static final float selectedScale = 0.85F;
	
	//Creating the view values
	public final ViewStub viewStubSelection;
	public ViewGroup groupSelection;
	public TextView labelSelection;
	public final VHAttachmentTileContent content;
	
	public VHAttachmentLinked(@NonNull View itemView, ViewStub viewStubSelection, VHAttachmentTileContent content) {
		super(itemView);
		this.viewStubSelection = viewStubSelection;
		this.content = content;
	}
	
	/**
	 * Binds a file to this view
	 * @param context The context to use
	 * @param source The file, either a {@link File} or {@link Uri}
	 * @param fileName The name of the file
	 * @param fileType The MIME type of the file
	 * @param fileSize The size of the file
	 * @param draftID The file's draft ID (or -1 if unavailable)
	 * @param dateModified The file's media provider modification date (or -1 if unavailable)
	 */
	public void bind(@NonNull Context context, @NonNull Union<File, Uri> source, @NonNull String fileName, @NonNull String fileType, long fileSize, long draftID, long dateModified) {
		content.bind(context, getCompositeDisposable(), source, fileName, fileType, fileSize, draftID, dateModified);
	}
	
	/**
	 * Gets if this view has a selection indicator available
	 */
	public boolean hasSelectionIndicator() {
		return viewStubSelection != null || labelSelection != null;
	}
	
	/**
	 * Shows the selection indicator on this view
	 * @param resources The resources to use
	 * @param animate Whether to animate this change
	 * @param index The index to display on the selection indicator
	 */
	public void setSelected(Resources resources, boolean animate, int index) {
		//Returning if the view state is already selected
		if(groupSelection != null && groupSelection.getVisibility() == View.VISIBLE) return;
		
		//Inflating the view if it hasn't yet been
		if(groupSelection == null) {
			groupSelection = (ViewGroup) viewStubSelection.inflate();
			labelSelection = groupSelection.findViewById(R.id.label_selectionindex);
		}
		
		//Showing the view
		if(animate) {
			int duration = resources.getInteger(android.R.integer.config_shortAnimTime);
			groupSelection.animate().withStartAction(() -> groupSelection.setVisibility(View.VISIBLE)).alpha(1).setDuration(duration).start();
			{
				ValueAnimator animator = ValueAnimator.ofFloat(itemView.getScaleX(), selectedScale);
				animator.setDuration(duration);
				animator.addUpdateListener(animation -> {
					float value = (float) animation.getAnimatedValue();
					itemView.setScaleX(value);
					itemView.setScaleY(value);
				});
				animator.start();
			}
		} else {
			groupSelection.setVisibility(View.VISIBLE);
			groupSelection.setAlpha(1);
			itemView.setScaleX(selectedScale);
			itemView.setScaleY(selectedScale);
		}
		
		labelSelection.setText(LanguageHelper.intToFormattedString(resources, index));
	}
	
	/**
	 * Hides the selection indicator from this view
	 * @param resources The resources to use
	 * @param animate Whether to animate this change
	 */
	public void setDeselected(Resources resources, boolean animate) {
		//Returning if the view state is already deselected
		if(groupSelection == null || groupSelection.getVisibility() == View.GONE) return;
		
		//Inflating the view if it hasn't yet been
		if(groupSelection == null) {
			groupSelection = (ViewGroup) viewStubSelection.inflate();
			labelSelection = groupSelection.findViewById(R.id.label_selectionindex);
		}
		
		//Hiding the view
		if(animate) {
			int duration = resources.getInteger(android.R.integer.config_shortAnimTime);
			groupSelection.animate().withEndAction(() -> groupSelection.setVisibility(View.GONE)).alpha(0).setDuration(duration).start();
			{
				ValueAnimator animator = ValueAnimator.ofFloat(itemView.getScaleX(), 1);
				animator.setDuration(duration);
				animator.addUpdateListener(animation -> {
					float value = (float) animation.getAnimatedValue();
					itemView.setScaleX(value);
					itemView.setScaleY(value);
				});
				animator.start();
			}
		} else {
			groupSelection.setVisibility(View.GONE);
			groupSelection.setAlpha(1);
			itemView.setScaleX(1);
			itemView.setScaleY(1);
		}
	}
	
	/**
	 * Sets the index number displayed on the selection indicator
	 */
	public void setSelectionIndex(Resources resources, int index) {
		//Inflating the view if it hasn't yet been
		if(groupSelection == null) {
			groupSelection = (ViewGroup) viewStubSelection.inflate();
			labelSelection = groupSelection.findViewById(R.id.label_selectionindex);
		}
		
		labelSelection.setText(LanguageHelper.intToFormattedString(resources, index));
	}
}