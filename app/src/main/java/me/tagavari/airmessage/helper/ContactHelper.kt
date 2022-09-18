package me.tagavari.airmessage.helper

import android.content.Context
import io.reactivex.rxjava3.core.Maybe
import me.tagavari.airmessage.MainApplication

object ContactHelper {
	/**
	 * Accepts a nullable address to produce an optional of the user's name or address
	 */
	@JvmStatic
	fun getUserDisplayName(context: Context, address: String?): Maybe<String> {
		return if(address != null) {
			MainApplication.instance.userCacheHelper.getUserInfo(context, address)
					.map { userInfo -> userInfo.contactName }
					.toMaybe()
					.onErrorReturnItem(address) as Maybe<String>
		} else {
			Maybe.empty()
		}
	}
}