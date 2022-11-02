package me.tagavari.airmessage.helper

import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.View
import androidx.core.app.Person
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.helper.BitmapHelper.loadBitmap
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.MemberInfo
import me.tagavari.airmessage.util.Union

object ConversationBuildHelper {
	/**
	 * Builds a conversation title using the static title, or the member's addresses
	 * Returns "unknown" if there are no members
	 * See [.buildConversationTitle] to build a conversation title with names
	 */
	@JvmStatic
	fun buildConversationTitleDirect(context: Context, conversationInfo: ConversationInfo): String {
		//Returning the conversation title if it is valid
		conversationInfo.title?.let { return it }
		
		//Returning "unknown" if the conversation has no members
		if(conversationInfo.members.isEmpty()) context.resources.getString(R.string.part_unknown)
		
		//Returning the string
		return LanguageHelper.createLocalizedList(context.resources, conversationInfo.members.map { member -> member.address })
	}
	
	/**
	 * Builds a conversation title asynchronously using the static title, or the member's addresses and names
	 * Returns "unknown" if there are no members
	 */
	@JvmStatic
	@CheckReturnValue
	fun buildConversationTitle(context: Context, conversationInfo: ConversationInfo): Single<String> {
		//Returning the conversation title if it is valid
		val title = conversationInfo.title
		if(!title.isNullOrEmpty()) {
			return Single.just(title)
		}
		
		//Building the title from the conversation's members
		return buildMemberTitle(context, conversationInfo.members)
	}
	
	/**
	 * Builds a conversation title asynchronously from a list of members
	 * Returns "unknown" if there are no members
	 */
	@JvmStatic
	@CheckReturnValue
	fun buildMemberTitle(context: Context, members: List<MemberInfo>): Single<String> {
		//Returning "unknown" if the conversation has no members
		if(members.isEmpty()) {
			return Single.just(context.resources.getString(R.string.part_unknown))
		}
		
		//Map each member to their name
		return Single.concat(members.map { member: MemberInfo ->
			//If the member's name is available, use it, otherwise use their address
			MainApplication.instance.userCacheHelper.getUserInfo(context, member.address)
					.map {userInfo -> userInfo.contactName ?: member.address}
					.onErrorReturnItem(member.address)
		})
				//Create a localized list of members
				.toList().map { nameList -> LanguageHelper.createLocalizedList(context.resources, nameList) }
	}
	
	/**
	 * Generates a [Bitmap] shortcut icon from a conversation
	 */
	@JvmStatic
	@CheckReturnValue
	fun generateShortcutIcon(context: Context, conversationInfo: ConversationInfo): Single<Bitmap> {
		//Rendering and returning the view
		val memberInfos = conversationInfo.members.toList()
		return (if(memberInfos.isEmpty()) Single.just(emptyList()) else Observable.fromIterable(memberInfos)
				//Limit to a maximum of 4 members
				.take(4)
				//Map each user to their color or their bitmap
				.flatMapSingle { member: MemberInfo ->
					MainApplication.instance.userCacheHelper.getUserInfo(context, member.address)
							.flatMap { userInfo ->
								if(userInfo.thumbnailURI == null) {
									throw Exception("No thumbnail URI")
								}
								
								loadBitmap(context, userInfo.thumbnailURI, true)
										.map<Union<Int, Bitmap>> { Union.ofB(it) }
							}
							.onErrorReturnItem(Union.ofA(member.color))
				}
				.toList(memberInfos.size))
				.map { contactDataList: List<Union<Int, Bitmap>> ->
					//Calculating layer sizes
					val layerSizeOuter = ResourceHelper.dpToPx(108F)
					
					//Creating the canvas
					val output = Bitmap.createBitmap(layerSizeOuter, layerSizeOuter, Bitmap.Config.ARGB_8888)
					val canvas = Canvas(output)
					canvas.drawColor(0xFFF5F5F5.toInt())
					
					//Creating the paint
					val backgroundPaint = Paint()
					backgroundPaint.isAntiAlias = true
					backgroundPaint.color = 0xFF424242.toInt()
					
					val userPaint = Paint()
					userPaint.isAntiAlias = true
					
					val bitmapPaint = Paint()
					bitmapPaint.isAntiAlias = true
					bitmapPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
					
					//If we have just one member, fill the canvas with their image
					when(contactDataList.size) {
						1 -> {
							val layerSizeInner = ResourceHelper.dpToPx((72 + 1).toFloat())
							val layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2
							
							val contactData = contactDataList[0]
							
							val drawRect = Rect(layerSizeInnerPadding, layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + layerSizeInner)
							drawContact(context, canvas, drawRect, backgroundPaint, userPaint, bitmapPaint, contactData)
						}
						2 -> {
							val layerSizeInner = ResourceHelper.dpToPx(54f)
							val layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2
							val userSize = ResourceHelper.dpToPx(30f)
							
							val drawRectLeft = Rect(layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + userSize, layerSizeInnerPadding + layerSizeInner)
							drawContact(context, canvas, drawRectLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList[0])
							
							val drawRectRight = Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + userSize)
							drawContact(context, canvas, drawRectRight, backgroundPaint, userPaint, bitmapPaint, contactDataList[1])
						}
						3 -> {
							val layerSizeInner = ResourceHelper.dpToPx(48f)
							val layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2
							val userSize = ResourceHelper.dpToPx(23f)
							
							val drawRectTop = Rect(layerSizeInnerPadding + (layerSizeInner - userSize) / 2, layerSizeInnerPadding, layerSizeInnerPadding + (layerSizeInner + userSize) / 2, layerSizeInnerPadding + userSize)
							drawContact(context, canvas, drawRectTop, backgroundPaint, userPaint, bitmapPaint, contactDataList[0])
							
							val drawRectLeft = Rect(layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + userSize, layerSizeInnerPadding + layerSizeInner)
							drawContact(context, canvas, drawRectLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList[1])
							
							val drawRectRight = Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + layerSizeInner)
							drawContact(context, canvas, drawRectRight, backgroundPaint, userPaint, bitmapPaint, contactDataList[2])
						}
						4 -> {
							val layerSizeInner = ResourceHelper.dpToPx(48f)
							val layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2
							val userSize = ResourceHelper.dpToPx(23f)
							
							val drawRectTopLeft = Rect(layerSizeInnerPadding, layerSizeInnerPadding, layerSizeInnerPadding + userSize, layerSizeInnerPadding + userSize)
							drawContact(context, canvas, drawRectTopLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList[0])
							
							val drawRectTopRight = Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + userSize)
							drawContact(context, canvas, drawRectTopRight, backgroundPaint, userPaint, bitmapPaint, contactDataList[1])
							
							val drawRectBottomLeft = Rect(layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + userSize, layerSizeInnerPadding + layerSizeInner)
							drawContact(context, canvas, drawRectBottomLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList[2])
							
							val drawRectBottomRight = Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + layerSizeInner)
							drawContact(context, canvas, drawRectBottomRight, backgroundPaint, userPaint, bitmapPaint, contactDataList[3])
						}
					}
					
					return@map output
				}
	}
	
	/**
	 * Draws a contact bitmap to the canvas
	 * @param context The context to use
	 * @param canvas The canvas to draw to
	 * @param drawRect The rectangle to draw everything
	 * @param backgroundPaint The paint to use for the background of profile pictures
	 * @param userPaint The paint to use when drawing default users
	 * @param bitmapPaint The paint to use when drawing the bitmap
	 * @param contactData The contact data to draw
	 */
	private fun drawContact(context: Context, canvas: Canvas, drawRect: Rect, backgroundPaint: Paint, userPaint: Paint, bitmapPaint: Paint, contactData: Union<Int, Bitmap>) {
		if(contactData.isA) {
			val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.user, null) ?: return
			drawable.bounds = drawRect
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) drawable.colorFilter = BlendModeColorFilter(contactData.a, BlendMode.MULTIPLY)
			else drawable.setColorFilter(contactData.a, PorterDuff.Mode.MULTIPLY)
			drawable.draw(canvas)
		} else {
			//canvas.drawCircle(drawRect.left + (drawRect.right - drawRect.left) / 2, drawRect.top + (drawRect.bottom - drawRect.top) / 2, (drawRect.right - drawRect.left) / 2, backgroundPaint);
			val path = Path()
			path.addCircle((drawRect.left + (drawRect.right - drawRect.left) / 2).toFloat(), (drawRect.top + (drawRect.bottom - drawRect.top) / 2).toFloat(), ((drawRect.right - drawRect.left) / 2).toFloat(), Path.Direction.CCW)
			canvas.save()
			canvas.clipPath(path)
			canvas.drawBitmap(contactData.b, null, drawRect, null)
			canvas.restore()
		}
	}
	
	/**
	 * Generates a list of [Person] from a conversation's members
	 */
	suspend fun generatePersonListCompat(context: Context, conversationInfo: ConversationInfo): List<Person> {
		//Return if the conversation has no members
		if(conversationInfo.members.isEmpty()) {
			return listOf()
		}
		
		//Getting member info for each member
		return if(MainApplication.canUseContacts(context)) {
			return conversationInfo.members.mapNotNull { member ->
				val userInfo = try {
					MainApplication.instance.userCacheHelper.getUserInfo(context, member.address).await()
				} catch(exception: Throwable) {
					return@mapNotNull null
				}
				
				androidx.core.app.Person.Builder().apply {
					setName(userInfo.contactName)
					setKey(userInfo.lookupKey)
					userInfo.thumbnailURI?.let { uri ->
						setIcon(IconCompat.createWithContentUri(uri))
					}
					setUri(userInfo.contactLookupUri.toString())
				}
					.build()
			}
		} else {
			conversationInfo.members.map { member ->
				androidx.core.app.Person.Builder().setKey(member.address).build()
			}
		}
	}
	
	private fun loadBitmapFromView(view: View): Bitmap {
		val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		view.measure(measureSpec, measureSpec)
		val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)
		view.layout(0, 0, view.measuredWidth, view.measuredHeight)
		view.draw(canvas)
		return bitmap
	}
}