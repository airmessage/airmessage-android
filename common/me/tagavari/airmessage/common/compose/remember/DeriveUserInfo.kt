package me.tagavari.airmessage.common.compose.remember

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.common.data.UserCacheHelper

@Composable
fun deriveUserInfo(address: String?): State<UserCacheHelper.UserInfo?> {
	val context = LocalContext.current
	return produceState<UserCacheHelper.UserInfo?>(null, address, deriveContactUpdates()) {
		value = try {
			address?.let {
				MainApplication.instance.userCacheHelper.getUserInfo(context, address).await()
			}
		} catch(exception: Throwable) {
			exception.printStackTrace()
			return@produceState
		}
	}
}
