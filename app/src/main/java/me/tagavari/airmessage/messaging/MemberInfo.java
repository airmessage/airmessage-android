package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class MemberInfo implements Parcelable {
	@NonNull private final String address;
	private int color;
	
	public MemberInfo(@NonNull String address, int color) {
		this.address = address;
		this.color = color;
	}
	
	private MemberInfo(Parcel in) {
		address = in.readString();
		color = in.readInt();
	}
	
	@NonNull
	public String getAddress() {
		return address;
	}
	
	public int getColor() {
		return color;
	}
	
	public void setColor(int color) {
		this.color = color;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(address);
		out.writeInt(color);
	}
	
	public static final Parcelable.Creator<MemberInfo> CREATOR = new Parcelable.Creator<MemberInfo>() {
		public MemberInfo createFromParcel(Parcel in) {
			return new MemberInfo(in);
		}
		
		public MemberInfo[] newArray(int size) {
			return new MemberInfo[size];
		}
	};
	
	@NonNull
	@Override
	public MemberInfo clone() {
		return new MemberInfo(address, color);
	}
}
