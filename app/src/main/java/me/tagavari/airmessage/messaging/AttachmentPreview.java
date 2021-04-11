package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

//A preview for an attachment file
public final class AttachmentPreview implements Serializable, Parcelable {
	private final String name;
	private final String type;
	
	/**
	 * Creates a preview for an attachment file
	 * @param name The name of the file
	 * @param type The MIME type of the file
	 */
	public AttachmentPreview(String name, String type) {
		this.name = name;
		this.type = type;
	}
	
	private AttachmentPreview(Parcel in) {
		name = in.readString();
		type = in.readString();
	}
	
	/**
	 * Gets the name of this attachment file
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the MIME type of this attachment file
	 */
	public String getType() {
		return type;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeString(type);
	}
	
	public static final Parcelable.Creator<AttachmentPreview> CREATOR = new Parcelable.Creator<AttachmentPreview>() {
		public AttachmentPreview createFromParcel(Parcel in) {
			return new AttachmentPreview(in);
		}
		
		public AttachmentPreview[] newArray(int size) {
			return new AttachmentPreview[size];
		}
	};
}