package me.tagavari.airmessage.helper

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.google.android.gms.maps.model.LatLng
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Function
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.constants.TimingConstants
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.enums.TapbackType
import me.tagavari.airmessage.helper.CalendarHelper.compareCalendarDates
import me.tagavari.airmessage.messaging.MessageComponentText
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.util.TapbackDisplayData
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

/**
 * Helper class for constructing human-readable strings from data
 */
object LanguageHelper {
	const val bulletSeparator = " • "
	
	/**
	 * Creates a list of items localized for the user's device
	 * @param resources The Android resources to use
	 * @param list The list of items to concatenate
	 * @return A localized string representing the listed items
	 */
	@JvmStatic
	fun createLocalizedList(resources: Resources, list: List<String>): String {
		val stringBuilder = StringBuilder()
		when(list.size) {
			0 -> {}
			1 -> stringBuilder.append(resources.getString(R.string.list_single, list[0]))
			2 -> stringBuilder.append(resources.getString(R.string.list_double, list[0], list[1]))
			else -> {
				stringBuilder.append(resources.getString(R.string.list_n_start, list[0]))
				for(i in 1 until list.size - 1) stringBuilder.append(resources.getString(R.string.list_n_middle, list[i]))
				stringBuilder.append(resources.getString(R.string.list_n_end, list[list.size - 1]))
			}
		}
		
		return stringBuilder.toString()
	}
	
	/**
	 * Creates a human-readable time for a conversation update
	 * @param context The context to use
	 * @param date The date the conversation was last updated
	 * @return A human-readable string for this update time
	 */
	@JvmStatic
	fun getLastUpdateStatusTime(context: Context, date: Long): String {
		val timeNow = System.currentTimeMillis()
		val timeDiff = timeNow - date
		
		//Just now
		if(timeDiff < TimingConstants.conversationJustNowTimeMillis) return context.resources.getString(R.string.time_now)
		
		//Within the hour
		if(timeDiff < 60 * 60 * 1000) return context.resources.getString(R.string.time_minutes, (timeDiff / (60 * 1000)).toInt())
		val thenCal = Calendar.getInstance()
		thenCal.timeInMillis = date
		val nowCal = Calendar.getInstance()
		nowCal.timeInMillis = timeNow
		
		//Within the day (14:11)
		if(thenCal[Calendar.ERA] == nowCal[Calendar.ERA] &&
				thenCal[Calendar.YEAR] == nowCal[Calendar.YEAR] &&
				nowCal[Calendar.DAY_OF_YEAR] == thenCal[Calendar.DAY_OF_YEAR]) {
			return DateFormat.getTimeFormat(context).format(thenCal.time)
		}
		
		//Within the week (Sun)
		if((nowCal.clone() as Calendar)
						//Today (now) -> One week ago
						.apply { add(Calendar.DAY_OF_YEAR, -7) }
						.let { compareCal -> compareCalendarDates(thenCal, compareCal) > 0 }) {
			return DateFormat.format("EEE", thenCal).toString()
		}
		
		//Within the year (Dec 9)
		if((nowCal.clone() as Calendar)
						//Today (now) -> One year ago
						.apply { add(Calendar.YEAR, -1) }
						.let { compareCal -> compareCalendarDates(thenCal, compareCal) > 0 }) {
			return DateFormat.format(context.resources.getString(R.string.dateformat_withinyear), thenCal).toString()
		}
		
		//Anytime (Dec 2018)
		return DateFormat.format(context.getString(R.string.dateformat_outsideyear_simple), thenCal).toString()
	}
	
	/**
	 * Creates a human-readable time for a message status timestamp
	 * @param context The context to use
	 * @param date The date of the message activity
	 * @return A human-readable string for this update time
	 */
	@JvmStatic
	fun getDeliveryStatusTime(context: Context, date: Long): String {
		//Creating the calendars
		val sentCal = Calendar.getInstance()
		sentCal.timeInMillis = date
		val nowCal = Calendar.getInstance()
		
		//If the message was sent today
		if(sentCal[Calendar.ERA] == nowCal[Calendar.ERA] &&
				sentCal[Calendar.YEAR] == nowCal[Calendar.YEAR] &&
				nowCal[Calendar.DAY_OF_YEAR] == sentCal[Calendar.DAY_OF_YEAR]) {
					return DateFormat.getTimeFormat(context).format(date)
		}
		
		//If the message was sent yesterday
		if((nowCal.clone() as Calendar)
						//Today (now) -> Yesterday
						.apply { add(Calendar.DAY_OF_YEAR, -1) }
						.let { compareCal ->
							sentCal[Calendar.ERA] == compareCal[Calendar.ERA] &&
									sentCal[Calendar.YEAR] == compareCal[Calendar.YEAR] &&
									sentCal[Calendar.DAY_OF_YEAR] == compareCal[Calendar.DAY_OF_YEAR] }) {
			return context.resources.getString(R.string.time_yesterday)
		}
		
		//If the days are within the same 7-day period (Sunday)
		if((nowCal.clone() as Calendar)
						//Today (now) -> One week ago
						.apply { add(Calendar.DAY_OF_YEAR, -7) }
						.let { compareCal -> compareCalendarDates(sentCal, compareCal) > 0 }) {
			return DateFormat.format("EEEE", sentCal).toString()
		}
		
		//If the days are within the same year period (Dec 9)
		if((nowCal.clone() as Calendar)
						//Today (now) -> One year ago
						.apply { add(Calendar.YEAR, -1) }
						.let { compareCal -> compareCalendarDates(sentCal, compareCal) > 0 }) {
			return DateFormat.format(context.getString(R.string.dateformat_withinyear), sentCal).toString()
		}
		
		//Different years (Dec 9, 2018)
		return DateFormat.format(context.getString(R.string.dateformat_outsideyear), sentCal).toString()
	}
	
	/**
	 * Creates a human-readable time for a time divider string
	 * @param context The context to use
	 * @param date The date the message was created
	 * @return A human-readable string for this update time
	 */
	@JvmStatic
	fun generateTimeDividerString(context: Context, date: Long): String {
		//Getting the calendars
		val sentCal = Calendar.getInstance()
		sentCal.timeInMillis = date
		val nowCal = Calendar.getInstance()
		
		//Creating the date
		val sentDate = Date(date)
		
		//If the message was sent today
		if(sentCal[Calendar.ERA] == nowCal[Calendar.ERA] &&
				sentCal[Calendar.YEAR] == nowCal[Calendar.YEAR] &&
				nowCal[Calendar.DAY_OF_YEAR] == sentCal[Calendar.DAY_OF_YEAR]) {
					return DateFormat.getTimeFormat(context).format(sentDate)
		}
		
		//If the message was sent yesterday
		if((nowCal.clone() as Calendar)
						//Today (now) -> Yesterday
						.apply { add(Calendar.DAY_OF_YEAR, -1) }
						.let { compareCal ->
							sentCal[Calendar.ERA] == compareCal[Calendar.ERA] &&
									sentCal[Calendar.YEAR] == compareCal[Calendar.YEAR] &&
									sentCal[Calendar.DAY_OF_YEAR] == compareCal[Calendar.DAY_OF_YEAR] }) {
			return context.resources.getString(R.string.time_yesterday) + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate)
		}
		
		//If the days are within the same 7-day period (Sunday)
		if((nowCal.clone() as Calendar)
						//Today (now) -> One week ago
						.apply { add(Calendar.DAY_OF_YEAR, -7) }
						.let { compareCal -> compareCalendarDates(sentCal, compareCal) > 0 }) {
			return DateFormat.format("EEEE", sentCal).toString() + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate)
		}
		
		//If the days are within the same year period (Sunday, Dec 9)
		if((nowCal.clone() as Calendar)
						//Today (now) -> One year ago
						.apply { add(Calendar.YEAR, -1) }
						.let { compareCal -> compareCalendarDates(sentCal, compareCal) > 0 }) {
			return DateFormat.format(context.getString(R.string.dateformat_withinyear_weekday), sentCal).toString() + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate)
		}
		
		//Different years (Dec 9, 2018)
		return DateFormat.format(context.getString(R.string.dateformat_outsideyear), sentCal).toString() + bulletSeparator + DateFormat.getTimeFormat(context).format(sentDate)
	}
	
	/**
	 * Converts an integer to a human-readable string
	 */
	@JvmStatic
	fun intToFormattedString(resources: Resources, value: Int): String {
		return String.format(if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales[0] else resources.configuration.locale, "%d", value)
	}
	
	/**
	 * Converts latitude and longitude coordinates to a human-readable string
	 */
	@JvmStatic
	fun coordinatesToString(position: LatLng): String {
		return "(" + String.format(Locale.getDefault(), "%.5f", position.latitude) + ", " + String.format(Locale.getDefault(), "%.5f", position.longitude) + ")"
	}
	
	/**
	 * Gets a human-readable representation of an amount of bytes, with decimals
	 * @param bytes The bytes to use
	 * @param si Whether to use SI units
	 * @return A formatted string of the byte count
	 */
	fun getHumanReadableByteCount(bytes: Long, si: Boolean): String {
		val unit = if(si) 1000 else 1024
		if(bytes < unit) return "$bytes B"
		val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
		val pre = (if(si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if(si) "" else "i"
		return String.format(Locale.getDefault(), "%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
	}
	
	/**
	 * Gets a human-readable representation of an amount of bytes
	 * @param bytes The bytes to use
	 * @param si Whether to use SI units
	 * @return A formatted string of the byte count
	 */
	@JvmStatic
	fun getHumanReadableByteCountInt(bytes: Long, si: Boolean): String {
		val unit = if(si) 1000 else 1024
		if(bytes < unit) return "$bytes B"
		val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
		val pre = (if(si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if(si) "" else "i"
		return String.format(Locale.getDefault(), "%d %sB", (bytes / unit.toDouble().pow(exp.toDouble())).toInt(), pre)
	}
	
	/**
	 * Gets a human-readable content type from a file MIME type
	 * @param resources The resources to use
	 * @param fileType The MIME type of the file
	 * @return The human-readable string of this file type
	 */
	@JvmStatic
	fun getHumanReadableContentType(resources: Resources, fileType: String?): String {
		return if(fileType.isNullOrEmpty()) resources.getString(R.string.part_unknown)
		else when {
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeImage) -> resources.getString(R.string.part_content_image)
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVideo) -> resources.getString(R.string.part_content_video)
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeAudio) -> resources.getString(R.string.part_content_audio)
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVLocation) -> resources.getString(R.string.part_content_location)
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVCard) -> resources.getString(R.string.part_content_contact)
			else -> resources.getString(R.string.part_content_other)
		}
	}
	
	/**
	 * Gets a human-readable representation of a summary of a message
	 */
	@JvmStatic
	fun messageToString(resources: Resources, messageInfo: MessageInfo): String {
		//Applying invisible ink
		val message: String = if(SendStyleHelper.appleSendStyleBubbleInvisibleInk == messageInfo.sendStyle) {
			resources.getString(R.string.message_messageeffect_invisibleink)
		} else if(messageInfo.messageTextComponent != null) {
			textComponentToString(resources, messageInfo.messageTextComponent) ?: ""
		} else if(messageInfo.attachments.isNotEmpty()) {
			val attachmentCount = messageInfo.attachments.size
			if(attachmentCount == 1) {
				resources.getString(getNameFromContentType(messageInfo.attachments[0].contentType))
			} else {
				resources.getQuantityString(R.plurals.message_multipleattachments, attachmentCount, attachmentCount)
			}
		} else {
			resources.getString(R.string.part_unknown)
		}
		
		//Returning the string with the message
		return if(messageInfo.isOutgoing) resources.getString(R.string.prefix_you, message) else message
	}
	
	/**
	 * Gets the string resource for the provided attachment details
	 * @param fileType The MIME type of the file
	 * @return A string resource for the name of the content type
	 */
	@JvmStatic
	@StringRes
	fun getNameFromContentType(fileType: String?): Int {
		//Returning the type
		return if(fileType.isNullOrEmpty()) R.string.part_content_other
		else when {
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeImage) -> R.string.part_content_image
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVideo) -> R.string.part_content_video
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeAudio) -> R.string.part_content_audio
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVLocation) -> R.string.part_content_location
			FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVCard) -> R.string.part_content_contact
			else -> R.string.part_content_other
		}
	}
	
	/**
	 * Gets a human-readable representation of a message text component, with both the subject and body text
	 */
	@JvmStatic
	fun textComponentToString(resources: Resources, textComponent: MessageComponentText?): String? {
		return if(textComponent!!.text != null || textComponent.subject != null) {
			//Only text
			if(textComponent.text != null) {
				textComponent.text!!.replace('\n', ' ')
			} else if(textComponent.subject != null) {
				textComponent.subject!!.replace('\n', ' ')
			} else {
				resources.getString(R.string.prefix_wild, textComponent.subject!!.replace('\n', ' '), textComponent.text!!.replace('\n', ' '))
			}
		} else {
			null
		}
	}
	
	/**
	 * Generates the display data required to render a tapback
	 * @param code The tapback's value code
	 */
	@JvmStatic
	fun getTapbackDisplay(@TapbackType code: Int): TapbackDisplayData? {
		return when(code) {
			TapbackType.heart -> TapbackDisplayData(R.drawable.love_rounded, R.color.tapback_love, R.string.part_tapback_heart)
			TapbackType.like -> TapbackDisplayData(R.drawable.like_rounded, R.color.tapback_like, R.string.part_tapback_like)
			TapbackType.dislike -> TapbackDisplayData(R.drawable.dislike_rounded, R.color.tapback_dislike, R.string.part_tapback_dislike)
			TapbackType.laugh -> TapbackDisplayData(R.drawable.excited_rounded, R.color.tapback_laugh, R.string.part_tapback_laugh)
			TapbackType.exclamation -> TapbackDisplayData(R.drawable.exclamation_rounded, R.color.tapback_exclamation, R.string.part_tapback_exclamation)
			TapbackType.question -> TapbackDisplayData(R.drawable.question_rounded, R.color.tapback_question, R.string.part_tapback_question)
			else -> null
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
	@JvmStatic
	fun getTapbackSummary(context: Context, sender: String?, @TapbackType code: Int, messageText: String?): Single<String> {
		@StringRes val stringID: Int
		
		//Checking if there is no sender
		return if(sender == null) {
			//Getting the string ID to use
			stringID = when(code) {
				TapbackType.heart -> R.string.message_tapback_add_heart_you
				TapbackType.like -> R.string.message_tapback_add_like_you
				TapbackType.dislike -> R.string.message_tapback_add_dislike_you
				TapbackType.laugh -> R.string.message_tapback_add_laugh_you
				TapbackType.exclamation -> R.string.message_tapback_add_exclamation_you
				TapbackType.question -> R.string.message_tapback_add_question_you
				TapbackType.unknown -> throw IllegalStateException("Unknown tapback code: $code")
				else -> throw IllegalStateException("Unknown tapback code: $code")
			}
			
			//Returning the string immediately
			Single.just(context.resources.getString(stringID, messageText))
		} else {
			//Getting the string ID to use
			stringID = when(code) {
				TapbackType.heart -> R.string.message_tapback_add_heart
				TapbackType.like -> R.string.message_tapback_add_like
				TapbackType.dislike -> R.string.message_tapback_add_dislike
				TapbackType.laugh -> R.string.message_tapback_add_laugh
				TapbackType.exclamation -> R.string.message_tapback_add_exclamation
				TapbackType.question -> R.string.message_tapback_add_question
				TapbackType.unknown -> throw IllegalStateException("Unknown tapback code: $code")
				else -> throw IllegalStateException("Unknown tapback code: $code")
			}
			
			//Returning the string with the sender's name
			MainApplication.getInstance().userCacheHelper.getUserInfo(context, sender)
					.map { userInfo -> userInfo.contactName }
					.onErrorReturnItem(sender)
					.map { name -> context.resources.getString(stringID, name, messageText) }
		}
	}
	
	/**
	 * Gets the domain name from a URL
	 * @param url The URL to retrieve the domain name for
	 * @return The domain name of the URL
	 * @throws URISyntaxException If the URL could not be correctly parsed
	 */
	@JvmStatic
	fun getDomainName(url: String?): String? {
		try {
			val uri = URI(url)
			val domain = uri.host ?: return null
			return if(domain.startsWith("www.")) domain.substring(4) else domain
		} catch(exception: URISyntaxException) {
			exception.printStackTrace();
			return null;
		}
	}
}