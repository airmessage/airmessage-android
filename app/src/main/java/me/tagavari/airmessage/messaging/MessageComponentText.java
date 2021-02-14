package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.enums.MessageComponentType;

public class MessageComponentText extends MessageComponent {
	//Creating the component values
	@Nullable private final String messageText;
	@Nullable private final String messageSubject;
	
	public MessageComponentText(long localID, @Nullable String guid, @Nullable String messageText, @Nullable String messageSubject) {
		//Calling the super constructor
		super(localID, guid);
		
		//Setting the text
		this.messageText = messageText;
		this.messageSubject = messageSubject;
	}
	
	/**
	 * Gets a text message component from a body string and a subject string
	 * @param localID The local ID of this text component
	 * @param guid The GUID of this text component
	 * @param body The body text of this message component (or NULL if unavailable)
	 * @param subject The subject text of this message component (or NULL if unavailable)
	 * @return A message component if valid, otherwise NULL
	 */
	@Nullable
	public static MessageComponentText fromText(long localID, @Nullable String guid, @Nullable String body, @Nullable String subject) {
		//No message text if there is no text to begin with
		if(body == null && subject == null) return null;
		
		return new MessageComponentText(localID, guid, body, subject);
	}
	
	@Nullable
	public String getText() {
		return messageText;
	}
	
	@Nullable
	public String getSubject() {
		return messageSubject;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		
		out.writeString(messageText);
		out.writeString(messageSubject);
	}
	
	public static final Parcelable.Creator<MessageComponentText> CREATOR = new Parcelable.Creator<MessageComponentText>() {
		public MessageComponentText createFromParcel(Parcel in) {
			return new MessageComponentText(in);
		}
		
		public MessageComponentText[] newArray(int size) {
			return new MessageComponentText[size];
		}
	};
	
	private MessageComponentText(Parcel in) {
		super(in);
		
		messageText = in.readString();
		messageSubject = in.readString();
	}
}