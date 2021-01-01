package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.AttachmentType;
import me.tagavari.airmessage.helper.ThemeHelper;
import me.tagavari.airmessage.messaging.FileDisplayMetadata;
import me.tagavari.airmessage.util.AudioPlaybackManager;
import me.tagavari.airmessage.util.Union;

public class VHAttachmentTileContentDocument extends VHAttachmentTileContent {
	public final View itemView; //The tile view group
	public final TextView documentName; //The name of the document
	public final ImageView documentIcon; //An icon to represent the document
	public final TextView documentSize; //The file size of the document
	
	public VHAttachmentTileContentDocument(View itemView, TextView documentName, ImageView documentIcon, TextView documentSize) {
		this.itemView = itemView;
		this.documentName = documentName;
		this.documentIcon = documentIcon;
		this.documentSize = documentSize;
	}
	
	@Override
	public void bind(@NonNull Context context, @NonNull CompositeDisposable compositeDisposable, @NonNull Union<File, Uri> source, @NonNull String fileName, @NonNull String fileType, long fileSize, long draftID, long dateModified) {
		//Getting the type-based details
		int iconResource = R.drawable.file;
		int viewColorBG = R.color.tile_grey_bg;
		int viewColorFG = R.color.tile_grey_fg;
		switch(fileType) {
			default:
				if(fileType.split("/")[0].startsWith("text")) {
					iconResource = R.drawable.file_document;
					viewColorBG = R.color.tile_indigo_bg;
					viewColorFG = R.color.tile_indigo_fg;
				}
				break;
			case "application/zip":
			case "application/x-tar":
			case "application/x-rar-compressed":
			case "application/x-7z-compressed":
			case "application/x-bzip":
			case "application/x-bzip2":
				iconResource = R.drawable.file_zip;
				viewColorBG = R.color.tile_brown_bg;
				viewColorFG = R.color.tile_brown_fg;
				break;
			case "application/pdf":
				iconResource = R.drawable.file_pdf;
				viewColorBG = R.color.tile_red_bg;
				viewColorFG = R.color.tile_red_fg;
				break;
			case "text/xml":
			case "application/xml":
			case "text/html":
				iconResource = R.drawable.file_xml;
				viewColorBG = R.color.tile_orange_bg;
				viewColorFG = R.color.tile_orange_fg;
				break;
			case "text/vcard":
				iconResource = R.drawable.file_user;
				viewColorBG = R.color.tile_cyan_bg;
				viewColorFG = R.color.tile_cyan_fg;
				break;
			case "application/msword":
			case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
			case "application/vnd.openxmlformats-officedocument.wordprocessingml.template":
			case "application/vnd.ms-word.document.macroEnabled.12":
			case "application/vnd.ms-word.template.macroEnabled.12":
				iconResource = R.drawable.file_msword;
				viewColorBG = R.color.tile_blue_bg;
				viewColorFG = R.color.tile_blue_fg;
				break;
			case "application/vnd.ms-excel":
			case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
			case "application/vnd.openxmlformats-officedocument.spreadsheetml.template":
			case "application/vnd.ms-excel.sheet.macroEnabled.12":
			case "application/vnd.ms-excel.sheet.binary.macroEnabled.12":
			case "application/vnd.ms-excel.template.macroEnabled.12":
			case "application/vnd.ms-excel.addin.macroEnabled.12":
				iconResource = R.drawable.file_msexcel;
				viewColorBG = R.color.tile_green_bg;
				viewColorFG = R.color.tile_green_fg;
				break;
			case "application/vnd.ms-powerpoint":
			case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
			case "application/vnd.openxmlformats-officedocument.presentationml.template":
			case "application/vnd.openxmlformats-officedocument.presentationml.slideshow":
			case "application/vnd.ms-powerpoint.addin.macroEnabled.12":
			case "application/vnd.ms-powerpoint.presentation.macroEnabled.12":
			case "application/vnd.ms-powerpoint.template.macroEnabled.12":
			case "application/vnd.ms-powerpoint.slideshow.macroEnabled.12":
				iconResource = R.drawable.file_mspowerpoint;
				viewColorBG = R.color.tile_yellow_bg;
				viewColorFG = R.color.tile_yellow_fg;
				break;
		}
		
		//Resolving the color resources
		viewColorBG = context.getResources().getColor(viewColorBG, null);
		viewColorFG = context.getResources().getColor(viewColorFG, null);
		if(ThemeHelper.isNightMode(context.getResources())) {
			int temp = viewColorBG;
			viewColorBG = viewColorFG;
			viewColorFG = temp;
		}
		
		//Filling in the view data
		documentName.setText(fileName);
		documentName.setTextColor(viewColorFG);
		
		documentIcon.setImageResource(iconResource);
		documentIcon.setImageTintList(ColorStateList.valueOf(viewColorFG));
		
		documentSize.setText(Formatter.formatFileSize(context, fileSize));
		documentSize.setTextColor(viewColorFG);
		
		itemView.setBackgroundTintList(ColorStateList.valueOf(viewColorBG));
	}
	
	@Override
	public int getAttachmentType() {
		return AttachmentType.document;
	}
}