package me.tagavari.airmessage.compose.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.ContactHelper
import me.tagavari.airmessage.messaging.MemberInfo

private val bitmapCacheMutex = Mutex()
private val bitmapCache = object : LruCache<Long, Bitmap>(1024 * 1024) {
	override fun sizeOf(key: Long, value: Bitmap): Int {
		return value.byteCount
	}
}

/**
 * Displays a circular image for a member
 */
@Composable
fun MemberImage(
	modifier: Modifier = Modifier,
	member: MemberInfo
) {
	//Get the user
	val context = LocalContext.current
	val contactID by produceState<Long?>(null, member.address) {
		value = try {
			MainApplication.getInstance().userCacheHelper
				.getUserInfo(context, member.address).await()
				.contactID
		} catch(exception: Throwable) {
			exception.printStackTrace()
			return@produceState
		}
	}
	
	MemberImage(
		modifier = modifier,
		color = Color(member.color),
		contactID = contactID
	)
}

/**
 * Displays a circular image for a member
 */
@Composable
fun MemberImage(
	modifier: Modifier = Modifier,
	color: Color,
	contactID: Long?
) {
	val context = LocalContext.current
	val contactBitmapPainter by produceState<Painter?>(null, contactID) {
		if(contactID == null) return@produceState
		
		val bitmap = bitmapCacheMutex.withLock {
			//Look up a bitmap from the cache
			bitmapCache[contactID]?.let {
				return@withLock it
			}
			
			//Decode the contact's image
			val bitmap = withContext(Dispatchers.IO) {
				ContactHelper.getContactImageThumbnailStream(context, contactID)
					?.let { BitmapFactory.decodeStream(it) }
			}
			
			//Save the bitmap in the cache
			bitmap?.let {
				bitmapCache.put(contactID, it)
			}
			
			return@withLock bitmap
		}
		
		//Convert the bitmap to a painter
		bitmap?.let {
			value = BitmapPainter(it.asImageBitmap())
		}
	}
	
	Image(
		modifier = modifier.clip(CircleShape),
		painter = contactBitmapPainter ?: painterResource(id = R.drawable.user),
		contentDescription = null,
		colorFilter = if(contactBitmapPainter != null) null else ColorFilter.tint(color, BlendMode.Multiply),
	)
}
