package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class StickerInfo implements Parcelable {
	//Creating the sticker values
	private final long localID;
	private final String guid;
	private final String sender;
	private final long date;
	private final File file;
	
	public StickerInfo(long localID, String guid, String sender, long date, File file) {
		this.localID = localID;
		this.guid = guid;
		this.sender = sender;
		this.date = date;
		this.file = file;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public String getSender() {
		return sender;
	}
	
	public long getDate() {
		return date;
	}
	
	public File getFile() {
		return file;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(localID);
		out.writeString(guid);
		out.writeString(sender);
		out.writeLong(date);
		out.writeString(file.getPath());
	}
	
	public static final Parcelable.Creator<StickerInfo> CREATOR = new Parcelable.Creator<StickerInfo>() {
		public StickerInfo createFromParcel(Parcel in) {
			return new StickerInfo(in);
		}
		
		public StickerInfo[] newArray(int size) {
			return new StickerInfo[size];
		}
	};
	
	private StickerInfo(Parcel in) {
		localID = in.readLong();
		guid = in.readString();
		sender = in.readString();
		date = in.readLong();
		file = new File(in.readString());
	}
}
