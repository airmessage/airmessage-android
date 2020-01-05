package me.tagavari.airmessage.data;

import android.os.Parcel;
import android.os.Parcelable;

public class SMSIDParcelable implements Parcelable {
	private final long messageID;
	
	public SMSIDParcelable(long messageID) {
		this.messageID = messageID;
	}
	
	private SMSIDParcelable(Parcel in) {
		messageID = in.readLong();
	}
	
	public long getMessageID() {
		return messageID;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeLong(messageID);
	}
	
	public static final Parcelable.Creator<SMSIDParcelable> CREATOR = new Parcelable.Creator<SMSIDParcelable>() {
		public SMSIDParcelable createFromParcel(Parcel in) {
			return new SMSIDParcelable(in);
		}
		
		public SMSIDParcelable[] newArray(int size) {
			return new SMSIDParcelable[size];
		}
	};
}