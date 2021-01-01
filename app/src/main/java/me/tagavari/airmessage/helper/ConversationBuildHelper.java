package me.tagavari.airmessage.helper;

import android.app.Person;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.util.Union;

public class ConversationBuildHelper {
	/**
	 * Builds a conversation title using the static title, or the member's addresses
	 * Returns "unknown" if there are no members
	 * See {@link #buildConversationTitle(Context, ConversationInfo)} to build a conversation title with names
	 */
	public static String buildConversationTitleDirect(Context context, ConversationInfo conversationInfo) {
		//Returning the conversation title if it is valid
		if(!TextUtils.isEmpty(conversationInfo.getTitle())) return conversationInfo.getTitle();
		
		//Returning "unknown" if the conversation has no members
		if(conversationInfo.getMembers().size() == 0) return context.getResources().getString(R.string.part_unknown);
		
		//Returning the string
		return LanguageHelper.createLocalizedList(context.getResources(), conversationInfo.getMembers().stream().map(MemberInfo::getAddress).toArray(String[]::new));
	}
	
	/**
	 * Builds a conversation title asynchronously using the static title, or the member's addresses and names
	 * Returns "unknown" if there are no members
	 */
	@CheckReturnValue
	public static Single<String> buildConversationTitle(Context context, ConversationInfo conversationInfo) {
		//Returning the conversation title if it is valid
		if(!TextUtils.isEmpty(conversationInfo.getTitle())) {
			return Single.just(conversationInfo.getTitle());
		}
		
		//Building the title from the conversation's members
		return buildMemberTitle(context, conversationInfo.getMembers());
	}
	
	/**
	 * Builds a conversation title asynchronously from a list of members
	 * Returns "unknown" if there are no members
	 */
	@CheckReturnValue
	public static Single<String> buildMemberTitle(Context context, List<MemberInfo> members) {
		//Returning "unknown" if the conversation has no members
		if(members.size() == 0) {
			return Single.just(context.getResources().getString(R.string.part_unknown));
		}
		
		//Map each member to their name
		return Single.concat(members.stream().map(member ->
				MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, member.getAddress())
						.map(Optional::of)
						.onErrorReturnItem(Optional.empty())
						//If the member's name is available, use it, otherwise use their address
						.map(optionalUserInfo -> optionalUserInfo.map(UserCacheHelper.UserInfo::getContactName).orElse(member.getAddress())))
				.collect(Collectors.toList()))
				//Create a localized list of members
				.toList().map(nameList -> LanguageHelper.createLocalizedList(context.getResources(), nameList.toArray(new String[0])));
	}
	
	/**
	 * Generates a {@link Bitmap} shortcut icon from a conversation
	 */
	@CheckReturnValue
	public static Single<Bitmap> generateShortcutIcon(Context context, ConversationInfo conversationInfo) {
		//Returning if the conversation has no members
		if(conversationInfo.getMembers().isEmpty()) {
			return Single.error(new Throwable("No members available"));
		}
		
		//if(true) return MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, conversationInfo.getMembers().get(0).getAddress()).flatMap(user -> BitmapHelper.loadBitmap(context, ContactHelper.getContactImageURI(user.getContactID())));
		
		//Rendering and returning the view
		ArrayList<MemberInfo> memberInfos = new ArrayList<>(conversationInfo.getMembers());
		return Observable.fromIterable(memberInfos)
				//Limit to a maximum of 4 members
				.take(4)
				//Map each user to their color or their bitmap
				.flatMapSingle(member ->
						MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, member.getAddress())
						.flatMap(userInfo -> BitmapHelper.loadBitmap(context, ContactHelper.getContactImageURI(userInfo.getContactID()), true).map(Union::<Integer, Bitmap>ofB))
						.onErrorReturnItem(Union.<Integer, Bitmap>ofA(member.getColor()))
				)
				.toList()
				.map(contactDataList -> {
					//Calculating layer sizes
					int layerSizeOuter = ResourceHelper.dpToPx(108);
					
					//Creating the canvas
					Bitmap output = Bitmap.createBitmap(layerSizeOuter, layerSizeOuter, Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(output);
					canvas.drawColor(0xFFF5F5F5);
					
					//Creating the paint
					Paint backgroundPaint = new Paint();
					backgroundPaint.setAntiAlias(true);
					backgroundPaint.setColor(0xFF424242);
					
					Paint userPaint = new Paint();
					userPaint.setAntiAlias(true);
					
					Paint bitmapPaint = new Paint();
					bitmapPaint.setAntiAlias(true);
					bitmapPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
					
					//If we have just one member, fill the canvas with their image
					if(contactDataList.size() == 1) {
						int layerSizeInner = ResourceHelper.dpToPx(72 + 1);
						int layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2;
						
						Union<Integer, Bitmap> contactData = contactDataList.get(0);
						
						Rect drawRect = new Rect(layerSizeInnerPadding, layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + layerSizeInner);
						drawContact(context, canvas, drawRect, backgroundPaint, userPaint, bitmapPaint, contactData);
					} else if(contactDataList.size() == 2) {
						int layerSizeInner = ResourceHelper.dpToPx(54);
						int layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2;
						int userSize = ResourceHelper.dpToPx(30);
						
						Rect drawRectLeft = new Rect(layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + userSize, layerSizeInnerPadding + layerSizeInner);
						drawContact(context, canvas, drawRectLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(0));
						
						Rect drawRectRight = new Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + userSize);
						drawContact(context, canvas, drawRectRight, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(1));
					} else if(contactDataList.size() == 3) {
						int layerSizeInner = ResourceHelper.dpToPx(48);
						int layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2;
						int userSize = ResourceHelper.dpToPx(23);
						
						Rect drawRectTop = new Rect(layerSizeInnerPadding + (layerSizeInner - userSize) / 2, layerSizeInnerPadding, layerSizeInnerPadding + (layerSizeInner + userSize) / 2, layerSizeInnerPadding + userSize);
						drawContact(context, canvas, drawRectTop, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(0));
						
						Rect drawRectLeft = new Rect(layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + userSize, layerSizeInnerPadding + layerSizeInner);
						drawContact(context, canvas, drawRectLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(1));
						
						Rect drawRectRight = new Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + layerSizeInner);
						drawContact(context, canvas, drawRectRight, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(2));
					} else if(contactDataList.size() == 4) {
						int layerSizeInner = ResourceHelper.dpToPx(48);
						int layerSizeInnerPadding = (layerSizeOuter - layerSizeInner) / 2;
						int userSize = ResourceHelper.dpToPx(23);
						
						Rect drawRectTopLeft = new Rect(layerSizeInnerPadding, layerSizeInnerPadding, layerSizeInnerPadding + userSize, layerSizeInnerPadding + userSize);
						drawContact(context, canvas, drawRectTopLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(0));
						
						Rect drawRectTopRight = new Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + userSize);
						drawContact(context, canvas, drawRectTopRight, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(1));
						
						Rect drawRectBottomLeft = new Rect(layerSizeInnerPadding, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + userSize, layerSizeInnerPadding + layerSizeInner);
						drawContact(context, canvas, drawRectBottomLeft, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(2));
						
						Rect drawRectBottomRight = new Rect(layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner - userSize, layerSizeInnerPadding + layerSizeInner, layerSizeInnerPadding + layerSizeInner);
						drawContact(context, canvas, drawRectBottomRight, backgroundPaint, userPaint, bitmapPaint, contactDataList.get(3));
					}
					
					return output;
				});
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
	private static void drawContact(Context context, Canvas canvas, Rect drawRect, Paint backgroundPaint, Paint userPaint, Paint bitmapPaint, Union<Integer, Bitmap> contactData) {
		if(contactData.isA()) {
			Drawable drawable = context.getResources().getDrawable(R.drawable.user, null);
			drawable.setBounds(drawRect);
			drawable.setColorFilter(contactData.getA(), PorterDuff.Mode.MULTIPLY);
			drawable.draw(canvas);
		} else {
			//canvas.drawCircle(drawRect.left + (drawRect.right - drawRect.left) / 2, drawRect.top + (drawRect.bottom - drawRect.top) / 2, (drawRect.right - drawRect.left) / 2, backgroundPaint);
			Path path = new Path();
			path.addCircle(drawRect.left + (drawRect.right - drawRect.left) / 2, drawRect.top + (drawRect.bottom - drawRect.top) / 2, (drawRect.right - drawRect.left) / 2, Path.Direction.CCW);
			canvas.save();
			canvas.clipPath(path);
			canvas.drawBitmap(contactData.getB(), null, drawRect, null);
			canvas.restore();
		}
	}
	
	/**
	 * Generates a list of {@link Person} from a conversation's members
	 */
	@RequiresApi(api = Build.VERSION_CODES.P)
	@CheckReturnValue
	public static Single<List<Person>> generatePersonList(Context context, ConversationInfo conversationInfo) {
		//Returning if the conversation has no members
		if(conversationInfo.getMembers().isEmpty()) {
			return Single.just(new ArrayList<>(0));
		}
		
		//Getting member info for each member
		if(MainApplication.canUseContacts(context)) {
			return Single.concat(conversationInfo.getMembers().stream().map(memberInfo -> MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, memberInfo.getAddress())).collect(Collectors.toList()))
					.observeOn(Schedulers.io())
					.map((userInfo) -> new Person.Builder()
							.setName(userInfo.getContactName())
							.setKey(userInfo.getLookupKey())
							.setIcon(Icon.createWithContentUri(ContactHelper.getContactImageURI(userInfo.getContactID())))
							.build()
					).toList();
		} else {
			return Single.just(conversationInfo.getMembers().stream().map(member -> new Person.Builder().setKey(member.getAddress()).build()).collect(Collectors.toList()));
		}
	}
	
	private static Bitmap loadBitmapFromView(View view) {
		int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		view.measure(measureSpec, measureSpec);
		Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		view.draw(canvas);
		return bitmap;
	}
}