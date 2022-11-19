package me.tagavari.airmessage.common.helper

import android.content.res.Resources
import me.tagavari.airmessage.R
import me.tagavari.airmessage.common.enums.ServiceHandler
import me.tagavari.airmessage.common.enums.ServiceType

object ColorHelper {
	/**
	 * Gets the color used to represent the specified service
	 */
	@JvmStatic
	fun getServiceColor(resources: Resources, @ServiceHandler serviceHandler: Int, @ServiceType serviceType: String?): Int {
		//AirMessage bridge
		if(serviceHandler == ServiceHandler.appleBridge) {
			//Returning a default color if the service is invalid
			return when(serviceType) {
				ServiceType.appleMessage -> resources.getColor(R.color.colorPrimary, null) //iMessage
				ServiceType.appleSMS -> resources.getColor(R.color.colorMessageTextMessageForwarding, null) //SMS bridge
				else -> resources.getColor(R.color.colorMessageDefault, null)
			}
		} else if(serviceHandler == ServiceHandler.systemMessaging) {
			return when(serviceType) {
				ServiceType.systemSMS -> resources.getColor(R.color.colorMessageTextMessage, null)
				ServiceType.systemRCS -> resources.getColor(R.color.colorMessageRCS, null)
				else -> resources.getColor(R.color.colorMessageDefault, null)
			}
		}
		
		//Returning a default color
		return resources.getColor(R.color.colorMessageDefault, null)
	}
}