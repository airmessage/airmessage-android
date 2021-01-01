package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.enums.TapbackType;

public class TapbackInfo implements Parcelable {
	private long localID;
	@Nullable private String sender;
	@TapbackType private int code;
	
	/**
	 * Represents a tapback message
	 * @param localID The local ID of this entry
	 * @param sender The sender of this tapback, or NULL if the sender is the local user
	 * @param code The tapback's value code
	 */
	public TapbackInfo(long localID, @Nullable String sender, @TapbackType int code) {
		this.localID = localID;
		this.sender = sender;
		this.code = code;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	@Nullable
	public String getSender() {
		return sender;
	}
	
	@TapbackType
	public int getCode() {
		return code;
	}
	
	public void setCode(@TapbackType int code) {
		this.code = code;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(localID);
		out.writeString(sender);
		out.writeInt(code);
	}
	
	public static final Parcelable.Creator<TapbackInfo> CREATOR = new Parcelable.Creator<TapbackInfo>() {
		public TapbackInfo createFromParcel(Parcel in) {
			return new TapbackInfo(in);
		}
		
		public TapbackInfo[] newArray(int size) {
			return new TapbackInfo[size];
		}
	};
	
	private TapbackInfo(Parcel in) {
		localID = in.readLong();
		sender = in.readString();
		code = in.readInt();
	}
}
