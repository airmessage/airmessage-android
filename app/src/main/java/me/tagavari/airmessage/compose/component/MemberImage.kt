package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import coil.compose.AsyncImage
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.helper.ContactHelper
import me.tagavari.airmessage.messaging.MemberInfo

/**
 * Displays a circular image for a member
 */
@Composable
fun MemberImage(
	member: MemberInfo,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val userInfo by produceState<UserCacheHelper.UserInfo?>(null, member) {
		try {
			value = MainApplication.getInstance().userCacheHelper.getUserInfo(context, member.address).await()
		} catch(exception: Throwable) {
			exception.printStackTrace()
		}
	}
	
	var isImageLoaded by remember { mutableStateOf(false) }
	
	AsyncImage(
		model = userInfo?.contactID?.let { ContactHelper.getContactImageURI(it) },
		placeholder = painterResource(id = R.drawable.user),
		contentDescription = null,
		modifier = modifier.clip(CircleShape),
		colorFilter = if(!isImageLoaded) ColorFilter.tint(Color(member.color), BlendMode.Multiply) else null,
		onLoading = { isImageLoaded = false },
		onSuccess = { isImageLoaded = true }
	)
}