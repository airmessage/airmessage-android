package me.tagavari.airmessage.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.tagavari.airmessage.BuildConfig
import me.tagavari.airmessage.R

/**
 * A messaging service that can be displayed as an
 * option to the user
 */
enum class MessageServiceDescription(
	@DrawableRes val icon: Int,
	@StringRes val title: Int,
	@ServiceHandler val serviceHandler: Int,
	@ServiceType val serviceType: String,
	val serviceSupportsEmail: Boolean
) {
	IMESSAGE(
		icon = R.drawable.message_push,
		title = R.string.title_imessage,
		serviceHandler = ServiceHandler.appleBridge,
		serviceType	= ServiceType.appleMessage,
		serviceSupportsEmail = true
	),
	TEXT_MESSAGE_FORWARDING(
		icon = R.drawable.message_bridge,
		title = R.string.title_textmessageforwarding,
		serviceHandler = ServiceHandler.appleBridge,
		serviceType	= ServiceType.appleSMS,
		serviceSupportsEmail = false
	),
	TEXT_MESSAGE(
		icon = R.drawable.message_sms,
		title = R.string.title_textmessage,
		serviceHandler = ServiceHandler.systemMessaging,
		serviceType	= ServiceType.systemSMS,
		serviceSupportsEmail = false
	);
	
	companion object {
		val availableServices = buildList {
			add(IMESSAGE)
			if(BuildConfig.DEBUG) add(TEXT_MESSAGE_FORWARDING)
			add(TEXT_MESSAGE)
		}
	}
}
