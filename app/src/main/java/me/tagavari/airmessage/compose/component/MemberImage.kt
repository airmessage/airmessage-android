package me.tagavari.airmessage.compose.component

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.deriveContactUpdates
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.messaging.MemberInfo

/**
 * Displays a circular image for a member
 */
@Composable
fun MemberImage(
	modifier: Modifier = Modifier,
	member: MemberInfo,
	highRes: Boolean = false
) {
	//Get the user
	val context = LocalContext.current
	val userInfo by produceState<UserCacheHelper.UserInfo?>(null, member.address, deriveContactUpdates()) {
		value = try {
			MainApplication.instance.userCacheHelper
				.getUserInfo(context, member.address).await()
		} catch(exception: Throwable) {
			exception.printStackTrace()
			return@produceState
		}
	}
	
	MemberImage(
		modifier = modifier,
		color = Color(member.color),
		thumbnailURI = userInfo?.run {
			if(highRes) photoURI else thumbnailURI
		}
	)
}

/**
 * Displays a circular image for a member
 */
@Composable
fun MemberImage(
	modifier: Modifier = Modifier,
	color: Color,
	thumbnailURI: Uri?
) {
	@Composable
	fun FallbackImage() {
		Image(
			painter = painterResource(id = R.drawable.user),
			contentDescription = null,
			colorFilter = ColorFilter.tint(color, BlendMode.Multiply)
		)
	}
	
	SubcomposeAsyncImage(
		modifier = modifier.clip(CircleShape),
		model = thumbnailURI,
		loading = { FallbackImage() },
		error = { FallbackImage() },
		contentDescription = null
	)
}
