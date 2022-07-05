package me.tagavari.airmessage.compose.component

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
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
	val contactBitmapPainter by produceState<Painter?>(null, member) {
		//Get the user
		val userInfo = try {
			MainApplication.getInstance().userCacheHelper.getUserInfo(context, member.address).await()
		} catch(exception: Throwable) {
			exception.printStackTrace()
			return@produceState
		}
		
		//Get the contact's image
		val contactImageURI = ContactHelper.getContactImageURI(userInfo.contactID)
		
		//Decode the contact's image
		@Suppress("BlockingMethodInNonBlockingContext", "DEPRECATION")
		val bitmap = withContext(Dispatchers.IO) {
			try {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, contactImageURI))
				} else {
					MediaStore.Images.Media.getBitmap(context.contentResolver, contactImageURI)
				}
			} catch(exception: Throwable) {
				exception.printStackTrace()
				return@withContext null
			}
		} ?: return@produceState
		
		//Convert the bitmap to a painter
		value = BitmapPainter(bitmap.asImageBitmap())
	}
	
	Image(
		painter = contactBitmapPainter ?: painterResource(id = R.drawable.user),
		contentDescription = null,
		modifier = modifier.clip(CircleShape),
		colorFilter = if(contactBitmapPainter != null) null else ColorFilter.tint(Color(member.color), BlendMode.Multiply),
	)
}