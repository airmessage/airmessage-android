package me.tagavari.airmessage.helper;

import android.content.res.Resources;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;

public class ColorHelper {
	/**
	 * Gets the color used to represent the specified service
	 */
	public static int getServiceColor(Resources resources, @ServiceHandler int serviceHandler, @ServiceType String serviceType) {
		//AirMessage bridge
		if(serviceHandler == ServiceHandler.appleBridge) {
			//Returning a default color if the service is invalid
			if(serviceType == null) return resources.getColor(R.color.colorMessageDefault, null);
			
			switch(serviceType) {
				case ServiceType.appleMessage:
					//iMessage
					return resources.getColor(R.color.colorPrimary, null);
				case ServiceType.appleSMS:
					//SMS bridge
					return resources.getColor(R.color.colorMessageTextMessageForwarding, null);
				default:
					return resources.getColor(R.color.colorMessageDefault, null);
			}
		}
		//System messaging
		else if(serviceHandler == ServiceHandler.systemMessaging) {
			switch(serviceType) {
				case ServiceType.systemSMS:
					return resources.getColor(R.color.colorMessageTextMessage, null);
				case ServiceType.systemRCS:
					return resources.getColor(R.color.colorMessageRCS, null);
				default:
					return resources.getColor(R.color.colorMessageDefault, null);
			}
		}
		
		//Returning a default color
		return resources.getColor(R.color.colorMessageDefault, null);
	}
}