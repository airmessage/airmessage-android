package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.util.Constants;

public class MessagePreviewLink extends MessagePreviewInfo<MessagePreviewLink.ViewHolder> {
	public MessagePreviewLink(long messageID, byte[] data, String target, String title, String subtitle, String caption) {
		super(messageID, data, target, title, subtitle, caption);
	}
	
	@Override
	public int getType() {
		return typeLink;
	}
	
	@Override
	public void bind(ViewHolder viewHolder, Context context) {
		//Loading the image (or disabling it if there is none)
		byte[] data = getData();
		if(data == null) {
			viewHolder.imageHeader.setVisibility(View.GONE);
		} else {
			viewHolder.imageHeader.setVisibility(View.VISIBLE);
			if(Constants.validateContext(context)) Glide.with(context).load(data).into(viewHolder.imageHeader);
		}
		
		//Setting the title
		viewHolder.labelTitle.setText(getTitle());
		
		//Setting the description (or disabling it if there is none)
		String subtitle = getSubtitle();
		if(subtitle == null || subtitle.isEmpty()) {
			viewHolder.labelDescription.setVisibility(View.GONE);
		} else {
			viewHolder.labelDescription.setVisibility(View.VISIBLE);
			viewHolder.labelDescription.setText(subtitle);
		}
		
		//Setting the address (as the site name, or otherwise the host)
		viewHolder.labelAddress.setText(getCaption());
		
		//Setting the click listener
		viewHolder.viewRoot.setOnClickListener(view -> Constants.launchUri(context, Uri.parse(getTarget())));
	}
	
	public static class ViewHolder extends MessagePreviewInfo.ViewHolder {
		private final View viewBorder;
		private final ImageView imageHeader;
		private final TextView labelTitle;
		private final TextView labelDescription;
		private final TextView labelAddress;
		
		public ViewHolder(View view) {
			super(view);
			
			viewBorder = view.findViewById(R.id.view_border);
			imageHeader = view.findViewById(R.id.image_header);
			labelTitle = view.findViewById(R.id.label_title);
			labelDescription = view.findViewById(R.id.label_description);
			labelAddress = view.findViewById(R.id.label_address);
		}
		
		@Override
		public void updateViewEdges(boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
			//Assigning the border shape
			//viewRoot.setBackground(Constants.createRoundedDrawableBottom(new GradientDrawable(), anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored));
			viewBorder.setBackground(Constants.createRoundedDrawableBottom((GradientDrawable) viewRoot.getResources().getDrawable(R.drawable.rectangle_chatpreviewfull, null), anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored));
			
			//Updating the touch ripple
			RippleDrawable rippleDrawable = (RippleDrawable) Constants.resolveDrawableAttr(viewRoot.getContext(), android.R.attr.selectableItemBackground); //Ripple drawable from Android attributes
			Drawable shapeDrawable = Constants.createRoundedDrawableBottom(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFFFFFFFF, 0xFFFFFFFF}), anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored); //Getting a standard drawable for the shape
			rippleDrawable.setDrawableByLayerId(android.R.id.mask, shapeDrawable); //Applying the drawable to the ripple drawable
			viewRoot.setForeground(rippleDrawable);
		}
	}
}
