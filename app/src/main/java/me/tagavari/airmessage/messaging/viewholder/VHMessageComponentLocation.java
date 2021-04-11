package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.helper.MessageShapeHelper;
import me.tagavari.airmessage.view.RoundedFrameLayout;

public class VHMessageComponentLocation extends VHMessageComponentAttachment {
	public final MaterialCardView groupContent;
	public final View viewBorder;
	public final RoundedFrameLayout mapContainer;
	public final MapView mapView;
	public final TextView labelTitle;
	public final TextView labelAddress;
	
	private final SingleSubject<GoogleMap> mapSubject;
	
	public VHMessageComponentLocation(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer, ViewGroup groupPrompt, TextView labelPromptSize, TextView labelPromptType, ImageView iconPrompt, ViewGroup groupProgress, ProgressBar progressProgress, ImageView iconProgress, ViewGroup groupOpen, TextView labelOpen, ViewGroup groupContentFrame, MaterialCardView groupContent, View viewBorder, RoundedFrameLayout mapContainer, MapView mapView, TextView labelTitle, TextView labelAddress) {
		super(itemView, groupContainer, stickerContainer, tapbackContainer, groupPrompt, labelPromptSize, labelPromptType, iconPrompt, groupProgress, progressProgress, iconProgress, groupOpen, labelOpen, groupContentFrame);
		this.groupContent = groupContent;
		this.viewBorder = viewBorder;
		this.mapContainer = mapContainer;
		this.mapView = mapView;
		this.labelTitle = labelTitle;
		this.labelAddress = labelAddress;
		
		//Initializing the map
		mapView.onCreate(null);
		mapSubject = SingleSubject.create();
		mapView.getMapAsync(mapSubject::onSuccess);
	}
	
	public Single<GoogleMap> getGoogleMap() {
		return mapSubject;
	}
	
	@Override
	void updateContentViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		//Creating the message shape
		ShapeAppearanceModel messageAppearance = MessageShapeHelper.createRoundedMessageAppearance(context.getResources(), anchoredTop, anchoredBottom, alignToRight);
		
		//Assigning the border shape
		MaterialShapeDrawable borderDrawable = new MaterialShapeDrawable(messageAppearance);
		borderDrawable.setFillColor(ColorStateList.valueOf(0));
		borderDrawable.setStroke(2, context.getColor(R.color.colorDivider));
		viewBorder.setBackground(borderDrawable);
		
		//Assigning the map shape
		mapContainer.setRadiiRaw(MessageShapeHelper.createStandardRadiusArrayTop(context.getResources(), anchoredTop, alignToRight));
		
		//Assigning the card shape
		groupContent.setShapeAppearanceModel(messageAppearance);
	}
	
	@Override
	void updateContentViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground) {
	
	}
	
	@Override
	public int getComponentType() {
		return MessageComponentType.attachmentLocation;
	}
}