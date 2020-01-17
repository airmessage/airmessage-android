package me.tagavari.airmessage.util;

import android.content.Context;
import android.telephony.CarrierConfigManager;

public class MMSSMSHelper {
	public static int getMaxMessageSize(Context context) {
		return context.getSystemService(CarrierConfigManager.class).getConfig().getInt(CarrierConfigManager.KEY_MMS_MAX_MESSAGE_SIZE_INT);
	}
}