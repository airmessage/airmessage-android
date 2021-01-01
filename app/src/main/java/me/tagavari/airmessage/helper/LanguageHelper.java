package me.tagavari.airmessage.helper;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.format.DateFormat;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.gms.maps.model.LatLng;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.core.Single;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.constants.TimingConstants;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.enums.TapbackType;
import me.tagavari.airmessage.messaging.MessageComponentText;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.TapbackDisplayData;

/**
 * Helper class for constructing human-readable strings from data
 */
public class LanguageHelper {
	public static final String bulletSeparator = " • ";
	
	/**
	 * Creates a list of items localized for the user's device
	 * @param resources The Android resources to use
	 * @param list The list of items to concatenate
	 * @return A localized string representing the listed items
	 */
	public static String createLocalizedList(Resources resources, String[] list) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if(list.length == 1) stringBuilder.append(resources.getString(R.string.list_single, list[0]));
		else if(list.length == 2) stringBuilder.append(resources.getString(R.string.list_double, list[0], list[1]));
		else if(list.length > 2) {
			stringBuilder.append(resources.getString(R.string.list_n_start, list[0]));
			for(int i = 1; i < list.length - 1; i++) stringBuilder.append(resources.getString(R.string.list_n_middle, list[i]));
			stringBuilder.append(resources.getString(R.string.list_n_end, list[list.length - 1]));
		}
		
		return stringBuilder.toString();
	}
	
	/**
	 * Creates a human-readable time for a conversation update
	 * @param context The context to use
	 * @param date The date the conversation was last updated
	 * @return A human-readable string for this update time
	 */
	public static String getLastUpdateStatusTime(Context context, long date) {
		long timeNow = System.currentTimeMillis();
		long timeDiff = timeNow - date;
		
		//Just now
		if(timeDiff < TimingConstants.conversationJustNowTimeMillis) return context.getResources().getString(R.string.time_now);
		
		//Within the hour
		if(timeDiff < 60 * 60 * 1000) return context.getResources().getString(R.string.time_minutes, (int) (timeDiff / (60 * 1000)));
		
		Calendar thenCal = Calendar.getInstance();
		thenCal.setTimeInMillis(date);
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTimeInMillis(timeNow);
		
		//Within the day (14:11)
		if(thenCal.get(Calendar.ERA) == nowCal.get(Calendar.ERA) && thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == thenCal.get(Calendar.DAY_OF_YEAR)) return DateFormat.getTimeFormat(context).format(thenCal.getTime());
		
		//Within the week (Sun)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.DAY_OF_YEAR, -7); //Today (now) -> One week ago
			if(CalendarHelper.compareCalendarDates(thenCal, compareCal) > 0) return DateFormat.format("EEE", thenCal).toString();
		}
		
		//Within the year (Dec 9)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.YEAR, -1); //Today (now) -> One year ago
			if(CalendarHelper.compareCalendarDates(thenCal, compareCal) > 0) return DateFormat.format(context.getResources().getString(R.string.dateformat_withinyear), thenCal).toString();//return DateFormat.format("MMM d", thenCal).toString();
		}
		
		//Anytime (Dec 2018)
		return DateFormat.format(context.getString(R.string.dateformat_outsideyear_simple), thenCal).toString();
	}
	
	/**
	 Creates a human-readable time for a message status timestamp
	 * @param context The context to use
	 * @param date The date of the message activity
	 * @return A human-readable string for this update time
	 */
	public static String getDeliveryStatusTime(Context context, long date) {
		//Creating the calendars
		Calendar sentCal = Calendar.getInstance();
		sentCal.setTimeInMillis(date);
		Calendar nowCal = Calendar.getInstance();
		
		//If the message was sent today
		if(sentCal.get(Calendar.ERA) == nowCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == sentCal.get(Calendar.DAY_OF_YEAR))
			return DateFormat.getTimeFormat(context).format(date);
		
		//If the message was sent yesterday
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.DAY_OF_YEAR, -1); //Today (now) -> Yesterday
			if(sentCal.get(Calendar.ERA) == compareCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == compareCal.get(Calendar.YEAR) && sentCal.get(Calendar.DAY_OF_YEAR) == compareCal.get(Calendar.DAY_OF_YEAR))
				return context.getResources().getString(R.string.time_yesterday);
		}
		
		//If the days are within the same 7-day period (Sunday)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.DAY_OF_YEAR, -7); //Today (now) -> One week ago
			if(CalendarHelper.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format("EEEE", sentCal).toString();
		}
		
		//If the days are within the same year period (Dec 9)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.YEAR, -1); //Today (now) -> One year ago
			if(CalendarHelper.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format(context.getString(R.string.dateformat_withinyear), sentCal).toString();
		}
		
		//Different years (Dec 9, 2018)
		return DateFormat.format(context.getString(R.string.dateformat_outsideyear), sentCal).toString();
	}
	
	/**
	 Creates a human-readable time for a time divider string
	 * @param context The context to use
	 * @param date The date the message was created
	 * @return A human-readable string for this update time
	 */
	public static String generateTimeDividerString(Context context, long date) {
		//Getting the calendars
		Calendar sentCal = Calendar.getInstance();
		sentCal.setTimeInMillis(date);
		Calendar nowCal = Calendar.getInstance();
		
		//Creating the date
		Date sentDate = new Date(date);
		
		//If the message was sent today
		if(sentCal.get(Calendar.ERA) == nowCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == sentCal.get(Calendar.DAY_OF_YEAR))
			return DateFormat.getTimeFormat(context).format(sentDate);
		
		//If the message was sent yesterday
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.DAY_OF_YEAR, -1); //Today (now) -> Yesterday
			if(sentCal.get(Calendar.ERA) == compareCal.get(Calendar.ERA) && sentCal.get(Calendar.YEAR) == compareCal.get(Calendar.YEAR) && sentCal.get(Calendar.DAY_OF_YEAR) == compareCal.get(Calendar.DAY_OF_YEAR))
				return context.getResources().getString(R.string.time_yesterday) + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
		}
		
		//If the days are within the same 7-day period (Sunday)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.DAY_OF_YEAR, -7); //Today (now) -> One week ago
			if(CalendarHelper.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format("EEEE", sentCal) + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
		}
		
		//If the days are within the same year period (Sunday, Dec 9)
		{
			Calendar compareCal = (Calendar) nowCal.clone();
			compareCal.add(Calendar.YEAR, -1); //Today (now) -> One year ago
			if(CalendarHelper.compareCalendarDates(sentCal, compareCal) > 0) return DateFormat.format(context.getString(R.string.dateformat_withinyear_weekday), sentCal) + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
		}
		
		//Different years (Dec 9, 2018)
		return DateFormat.format(context.getString(R.string.dateformat_outsideyear), sentCal) + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate);
	}
	
	/**
	 * Converts an integer to a human-readable string
	 */
	public static String intToFormattedString(Resources resources, int value) {
		return String.format(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? resources.getConfiguration().getLocales().get(0) : resources.getConfiguration().locale, "%d", value);
	}
	
	/**
	 * Converts latitude and longitude coordinates to a human-readable string
	 */
	public static String coordinatesToString(LatLng position) {
		return "(" + String.format(Locale.getDefault(), "%.5f", position.latitude) + ", " + String.format(Locale.getDefault(), "%.5f", position.longitude) + ")";
	}
	
	/**
	 * Gets a human-readable representation of an amount of bytes, with decimals
	 * @param bytes The bytes to use
	 * @param si Whether to use SI units
	 * @return A formatted string of the byte count
	 */
	public static String getHumanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if(bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	/**
	 * Gets a human-readable representation of an amount of bytes
	 * @param bytes The bytes to use
	 * @param si Whether to use SI units
	 * @return A formatted string of the byte count
	 */
	public static String getHumanReadableByteCountInt(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if(bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format(Locale.getDefault(), "%d %sB", (int) (bytes / Math.pow(unit, exp)), pre);
	}
	
	/**
	 * Gets a human-readable content type from a file MIME type
	 * @param resources The resources to use
	 * @param fileType The MIME type of the file
	 * @return The human-readable string of this file type
	 */
	public static String getHumanReadableContentType(Resources resources, String fileType) {
		if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeImage)) {
			return resources.getString(R.string.part_content_image);
		} else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVideo)) {
			return resources.getString(R.string.part_content_video);
		} else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeAudio)) {
			return resources.getString(R.string.part_content_audio);
		} else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVLocation)) {
			return resources.getString(R.string.part_content_location);
		} else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVCard)) {
			return resources.getString(R.string.part_content_contact);
		} else {
			return resources.getString(R.string.part_content_other);
		}
	}
	
	/**
	 * Gets a human-readable representation of a summary of a message
	 */
	public static String messageToString(Resources resources, MessageInfo messageInfo) {
		//Creating the message variable
		String message;
		
		//Applying invisible ink
		if(SendStyleHelper.appleSendStyleBubbleInvisibleInk.equals(messageInfo.getSendStyle())) message = resources.getString(R.string.message_messageeffect_invisibleink);
		//Otherwise assigning the message to the message text
		else if(messageInfo.getMessageTextInfo() != null) message = textComponentToString(resources, messageInfo.getMessageTextInfo());
		//Setting the attachments if there are attachments
		else if(!messageInfo.getAttachments().isEmpty()) {
			int attachmentCount = messageInfo.getAttachments().size();
			if(attachmentCount == 1) message = resources.getString(getNameFromContentType(messageInfo.getAttachments().get(0).getContentType()));
			else message = resources.getQuantityString(R.plurals.message_multipleattachments, attachmentCount, attachmentCount);
		}
		//Otherwise setting the message to "unknown"
		else message = resources.getString(R.string.part_unknown);
		
		//Returning the string with the message
		if(messageInfo.isOutgoing()) return resources.getString(R.string.prefix_you, message);
		else return message;
	}
	
	/**
	 * Gets the string resource for the provided attachment details
	 * @param fileType The MIME type of the file
	 * @return A string resource for the name of the content type
	 */
	@StringRes
	public static int getNameFromContentType(String fileType) {
		//Returning the type
		if(fileType == null || fileType.isEmpty()) return R.string.part_content_other;
		else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeImage)) return R.string.part_content_image;
		else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVideo)) return R.string.part_content_video;
		else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeAudio)) return R.string.part_content_audio;
		else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVLocation)) return R.string.part_content_location;
		else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVCard)) return R.string.part_content_contact;
		else return R.string.part_content_other;
	}
	
	/**
	 * Gets a human-readable representation of a message text component, with both the subject and body text
	 */
	public static String textComponentToString(Resources resources, MessageComponentText textComponent) {
		if(textComponent.getText() != null || textComponent.getSubject() != null) {
			//Only text
			if(textComponent.getText() != null) {
				return textComponent.getText().replace('\n', ' ');
			}
			//Only subject
			else if(textComponent.getSubject() != null) {
				return textComponent.getSubject().replace('\n', ' ');
			}
			//Both text and subject
			else {
				return resources.getString(R.string.prefix_wild, textComponent.getSubject().replace('\n', ' '), textComponent.getText().replace('\n', ' '));
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Generates the display data required to render a tapback
	 * @param code The tapback's value code
	 */
	public static TapbackDisplayData getTapbackDisplay(@TapbackType int code) {
		switch(code) {
			case TapbackType.heart:
				return new TapbackDisplayData(R.drawable.love_rounded, R.color.tapback_love, R.string.part_tapback_heart);
			case TapbackType.like:
				return new TapbackDisplayData(R.drawable.like_rounded, R.color.tapback_like, R.string.part_tapback_like);
			case TapbackType.dislike:
				return new TapbackDisplayData(R.drawable.dislike_rounded, R.color.tapback_dislike, R.string.part_tapback_dislike);
			case TapbackType.laugh:
				return new TapbackDisplayData(R.drawable.excited_rounded, R.color.tapback_laugh, R.string.part_tapback_laugh);
			case TapbackType.exclamation:
				return new TapbackDisplayData(R.drawable.exclamation_rounded, R.color.tapback_exclamation, R.string.part_tapback_exclamation);
			case TapbackType.question:
				return new TapbackDisplayData(R.drawable.question_rounded, R.color.tapback_question, R.string.part_tapback_question);
			default:
				return null;
		}
	}
	
	/**
	 * Gets a summary message for a tapback
	 * @param context The context to use
	 * @param sender The sender of the tapback, or NULL if the sender is the local user
	 * @param code The tapback's value code
	 * @param messageText The summary of the message this tapback was applied to
	 * @return A single for the summary message of the tapback
	 */
	public static Single<String> getTapbackSummary(Context context, @Nullable String sender, @TapbackType int code, String messageText) {
		@StringRes
		int stringID;
		
		//Checking if there is no sender
		if(sender == null) {
			//Getting the string ID to use
			switch(code) {
				case TapbackType.heart:
					stringID = R.string.message_tapback_add_heart_you;
					break;
				case TapbackType.like:
					stringID = R.string.message_tapback_add_like_you;
					break;
				case TapbackType.dislike:
					stringID = R.string.message_tapback_add_dislike_you;
					break;
				case TapbackType.laugh:
					stringID = R.string.message_tapback_add_laugh_you;
					break;
				case TapbackType.exclamation:
					stringID = R.string.message_tapback_add_exclamation_you;
					break;
				case TapbackType.question:
					stringID = R.string.message_tapback_add_question_you;
					break;
				case TapbackType.unknown:
				default:
					throw new IllegalStateException("Unknown tapback code: " + code);
			}
			
			//Returning the string immediately
			return Single.just(context.getResources().getString(stringID, messageText));
		} else {
			//Getting the string ID to use
			switch(code) {
				case TapbackType.heart:
					stringID = R.string.message_tapback_add_heart;
					break;
				case TapbackType.like:
					stringID = R.string.message_tapback_add_like;
					break;
				case TapbackType.dislike:
					stringID = R.string.message_tapback_add_dislike;
					break;
				case TapbackType.laugh:
					stringID = R.string.message_tapback_add_laugh;
					break;
				case TapbackType.exclamation:
					stringID = R.string.message_tapback_add_exclamation;
					break;
				case TapbackType.question:
					stringID = R.string.message_tapback_add_question;
					break;
				case TapbackType.unknown:
				default:
					throw new IllegalStateException("Unknown tapback code: " + code);
			}
			
			//Returning the string with the sender's name
			return MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, sender)
					.map(UserCacheHelper.UserInfo::getContactName)
					.onErrorReturnItem(sender)
					.map(senderName -> context.getResources().getString(stringID, senderName, messageText));
		}
	}
	
	/**
	 * Gets the domain name from a URL
	 * @param url The URL to retrieve the domain name for
	 * @return The domain name of the URL
	 * @throws URISyntaxException If the URL could not be correctly parsed
	 */
	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		if(domain == null) return null;
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}
}