package me.tagavari.airmessage.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcel;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.TransformationMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.textclassifier.ConversationActions;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.android.mms.transaction.Transaction;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.util.BiConsumer;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.util.Consumer;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.SMSIDParcelable;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.receiver.TextMMSSentReceiver;
import me.tagavari.airmessage.receiver.TextSMSDeliveredReceiver;
import me.tagavari.airmessage.receiver.TextSMSSentReceiver;
import me.tagavari.airmessage.service.FileExportService;
import me.tagavari.airmessage.service.UriExportService;

public class Constants {
	//Creating the constants
	/* static final int permissionRecordAudio = 4;
	static final int permissionReadContacts = 5;
	static final int permissionAccessCoarseLocation = 6; */
	
	//static final int historicCommunicationsWS = 2;
	
	//static final int intentDisconnectService = 6;
	
	public static final String defaultMIMEType = "application/octet-stream";
	
	public static final Pattern regExValidAddress = Pattern.compile("^(((www\\.)?+[a-zA-Z0-9.\\-_]+(\\.[a-zA-Z]{2,})+)|(\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b))(/[a-zA-Z0-9_\\-\\s./?%#&=]*)?(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))?$");
	
	public static final String intentParamTarget = "target";
	public static final String intentParamTargetID = "targetID";
	public static final String intentParamGuid = "guid";
	public static final String intentParamResult = "result";
	public static final String intentParamData = "data";
	public static final String intentParamDataText = "dataText";
	public static final String intentParamDataFile = "dataFile";
	public static final String intentParamSize = "size";
	public static final String intentParamIndex = "index";
	public static final String intentParamTime = "time";
	public static final String intentParamIsLast = "isLast";
	public static final String intentParamList = "list";
	public static final String intentParamAction = "action";
	public static final String intentParamState = "state";
	public static final String intentParamCurrent = "current";
	public static final String intentParamProgress = "progress";
	public static final String intentParamRequestID = "requestID";
	public static final String intentParamLaunchID = "launchID";
	public static final String intentParamCode = "code";
	public static final String intentParamName = "name";
	public static final String intentParamAddress = "address";
	
	public static final String notificationReplyKey = "REMOTE_INPUT_REPLY";
	
	public static final String appleSendStyleBubbleSlam = "com.apple.MobileSMS.expressivesend.impact";
	public static final String appleSendStyleBubbleLoud = "com.apple.MobileSMS.expressivesend.loud";
	public static final String appleSendStyleBubbleGentle = "com.apple.MobileSMS.expressivesend.gentle";
	public static final String appleSendStyleBubbleInvisibleInk = "com.apple.MobileSMS.expressivesend.invisibleink";
	public static final String appleSendStyleScrnEcho = "com.apple.messages.effect.CKEchoEffect";
	public static final String appleSendStyleScrnSpotlight = "com.apple.messages.effect.CKSpotlightEffect";
	public static final String appleSendStyleScrnBalloons = "com.apple.messages.effect.CKHappyBirthdayEffect";
	public static final String appleSendStyleScrnConfetti = "com.apple.messages.effect.CKConfettiEffect";
	public static final String appleSendStyleScrnLove = "com.apple.messages.effect.CKHeartEffect";
	public static final String appleSendStyleScrnLasers = "com.apple.messages.effect.CKLasersEffect";
	public static final String appleSendStyleScrnFireworks = "com.apple.messages.effect.CKFireworksEffect";
	public static final String appleSendStyleScrnShootingStar = "com.apple.messages.effect.CKShootingStarEffect";
	public static final String appleSendStyleScrnCelebration = "com.apple.messages.effect.CKSparklesEffect";
	
	public static final Uri defaultNotificationSound = Settings.System.DEFAULT_NOTIFICATION_URI;//"content://settings/system/notification_sound";
	
	//Message state codes
	public static final int messageStateCodeGhost = 0;
	public static final int messageStateCodeIdle = 1;
	public static final int messageStateCodeSent = 2;
	public static final int messageStateCodeDelivered = 3;
	public static final int messageStateCodeRead = 4;
	
	//Message error codes
	public static final int messageErrorCodeOK = 0; //No error
	
	//AirMessage app-provided error codes (if the app fails a request)
	public static final int messageErrorCodeLocalUnknown = 100; //Unknown error (for example, a version upgrade where error codes change)
	public static final int messageErrorCodeLocalInvalidContent = 101; //Invalid content
	public static final int messageErrorCodeLocalFileTooLarge = 102; //Attachment too large
	public static final int messageErrorCodeLocalIO = 103; //IO exception
	public static final int messageErrorCodeLocalNetwork = 104; //Network exception
	public static final int messageErrorCodeLocalExpired = 106; //Request expired
	public static final int messageErrorCodeLocalReferences = 107; //References lost
	public static final int messageErrorCodeLocalInternal = 108; //Internal exception
	
	//AirMessage server-provided error codes (if the server fails a request, or Apple Messages cannot properly handle it)
	public static final int messageErrorCodeServerUnknown = 200; //An unknown response code was received from the server
	public static final int messageErrorCodeServerExternal = 201; //The server received an external error
	public static final int messageErrorCodeServerBadRequest = 202; //The server couldn't process the request
	public static final int messageErrorCodeServerUnauthorized = 203; //The server doesn't have permission to send messages
	public static final int messageErrorCodeServerNoConversation = 204; //The server couldn't find the requested conversation
	public static final int messageErrorCodeServerRequestTimeout = 205; //The server timed out the client's request
	
	//Apple-provided error codes (converted, from the Messages database)
	public static final int messageErrorCodeAppleUnknown = 300; //An unknown error code
	public static final int messageErrorCodeAppleNetwork = 301; //Network error
	public static final int messageErrorCodeAppleUnregistered = 302; //Not registered with iMessage
	
	public static final int groupActionUnknown = 0;
	public static final int groupActionJoin = 1;
	public static final int groupActionLeave = 2;
	
	public static final Uri serverSetupAddress = Uri.parse("https://airmessage.org/guide");
	public static final Uri helpAddress = Uri.parse("https://airmessage.org/help");
	public static final Uri helpTopicConnectionTroubleshootingAddress = Uri.parse("https://airmessage.org/help/topic/connection_troubleshooting");
	public static final Uri serverUpdateAddress = Uri.parse("https://airmessage.org/serverupdate");
	public static final Uri communityAddress = Uri.parse("https://reddit.com/r/AirMessage");
	public static final String feedbackEmail = "hello@airmessage.org";
	
	public static final int defaultPort = 1359;
	public static final String recordingName = "Audio Message.amr";
	public static final String pictureName = "image.jpg";
	public static final String videoName = "video.mp4";
	public static final String locationName = "location.loc.vcf";
	public static final String defaultFileName = "file";
	public static final int smartReplyHistoryLength = 10;
	
	//public static final int viewTagTypeKey = 0;
	public static final String viewTagTypeItem = "item";
	public static final String viewTagTypeAction = "action";
	
	public static final String bulletSeparator = " • ";
	
	//Creating the reference constants
	public static final float disabledAlpha = 0.54F;
	public static final int rippleAlphaInt = 51; //20%
	public static final int secondaryAlphaInt = 179; //70%
	public static final int[] effectColors = {
			0xFCE18A, //Yellow
			0xFF726D, //Orange
			0xB48DEF, //Purple
			0xF4306D, //Pink
			0x42A5F5, //Blue
			0x7986CB //Indigo
	};
	public static final int colorAMOLED = 0xFF000000;
	
	//Creating the regular expression constants
	private static final String emailRegEx = "(?i)(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])";
	
	public static boolean requestPermission(Activity activity, String[] permissions, int requestID) {
		//Creating the missing permission list
		ArrayList<String> missingPermissions = new ArrayList<>();
		
		//Adding all rejected permissions to the list
		for(String permission : permissions)
			if(ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED)
				missingPermissions.add(permission);
		
		//Returning if there are no missing permissions
		if(missingPermissions.isEmpty()) return false;
		
		//Requesting the permission
		ActivityCompat.requestPermissions(activity, missingPermissions.toArray(new String[0]), requestID);
		
		//Returning true
		return true;
	}
	
	public static int dpToPx(float dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}
	
	public static float pxToDp(int px) {
		return px / Resources.getSystem().getDisplayMetrics().density;
	}
	
	public static boolean isLTR(Resources resources) {
		return resources.getBoolean(R.bool.is_left_to_right);
	}
	
	public static void recursiveDelete(File file) {
		if(file.isFile()) file.delete();
		else {
			File[] childFiles = file.listFiles();
			if(childFiles != null) for(File child : childFiles) recursiveDelete(child);
			file.delete();
		}
	}
	
	public static void recursiveInvalidate(ViewGroup layout) {
		View child;
		for (int i = 0; i < layout.getChildCount(); i++) {
			child = layout.getChildAt(i);
			child.invalidate();
			if(child instanceof ViewGroup) recursiveInvalidate((ViewGroup) child);
		}
	}
	
	public static <T> boolean arrayContains(T[] array, T value) {
		for(T entry : array) if(entry.equals(value)) return true;
		return false;
	}
	
	public static ArrayList<View> getViewsByTag(ViewGroup root, String tag) {
		ArrayList<View> views = new ArrayList<>();
		final int childCount = root.getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = root.getChildAt(i);
			if (child instanceof ViewGroup) {
				views.addAll(getViewsByTag((ViewGroup) child, tag));
			}
			
			final Object tagObj = child.getTag();
			if (tagObj != null && tagObj.equals(tag)) {
				views.add(child);
			}
			
		}
		return views;
	}
	
	public interface ViewSource {
		View get();
	}
	
	public interface TaskedViewSource {
		View get(boolean wasTasked);
	}
	
	public interface ViewHolderSource<VH> {
		VH get();
	}
	
	public static class ViewHolderSourceImpl<VH> implements ViewHolderSource<VH> {
		private final WeakReference<RecyclerView> recyclerViewReference;
		private final long itemId;
		
		public ViewHolderSourceImpl(RecyclerView recyclerView, long itemId) {
			recyclerViewReference = new WeakReference<>(recyclerView);
			this.itemId = itemId;
		}
		
		@Override
		public VH get() {
			RecyclerView recyclerView = recyclerViewReference.get();
			if(recyclerView == null) return null;
			return (VH) recyclerView.findViewHolderForItemId(itemId);
		}
	}
	
	public static File findFreeFile(File directory, boolean splitFileExtension) {
		return findFreeFile(directory, "", splitFileExtension, "", 0);
	}
	
	public static File findFreeFile(File directory, String fileName, boolean splitFileExtension) {
		return findFreeFile(directory, fileName, splitFileExtension, "_", 0);
	}
	
	/**
	 * Finds a free file in the specified directory based on the file name by appending a counter to the end, increasing it until a suitable option is found
	 * @param directory the directory to find a file in
	 * @param fileName the name of the file
	 * @param splitFileExtension if the counter should be placed between the file's name and the file's extension
	 * @param separator a string of characters to place between the file's name and the file's counter
	 * @param startIndex the number to start the counter at
	 * @return the first available file found
	 */
	public static File findFreeFile(File directory, String fileName, boolean splitFileExtension, String separator, int startIndex) {
		//Creating the default file
		File file = new File(directory, fileName.isEmpty() ? separator + startIndex : fileName);
		
		//Checking if the file directory doesn't exist
		if(!directory.exists()) {
			//Creating the directory
			directory.mkdir();
			
			//Returning the file
			return file;
		}
		
		//Returning the file if it doesn't exist
		if(!file.exists()) return file;
		
		if(splitFileExtension) {
			//Getting the file name and extension
			String[] fileData = fileName.split("\\.(?=[^.]+$)");
			String baseFileName = fileData[0];
			String fileExtension = fileData.length > 1 ? fileData[1] : "";
			
			//Finding the first free file
			do {
				file = new File(directory, baseFileName + separator + startIndex++ + '.' + fileExtension);
			} while(file.exists());
		} else {
			//Finding the first free file
			do {
				file = new File(directory, fileName + separator + startIndex++);
			} while(file.exists());
		}
		
		//Returning the file
		return file;
	}
	
	public static String getUriName(Context context, Uri uri) {
		//Creating the file name variable
		String fileName = null;
		
		//Attempting to pull the file name from the content resolver
		if("content".equals(uri.getScheme())) {
			try(Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
				if(cursor != null && cursor.moveToFirst()) fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		
		//Attempting to pull the file name from the URI path
		if(fileName == null) {
			fileName = uri.getPath();
			int cut = fileName.lastIndexOf('/');
			if(cut != -1) fileName = fileName.substring(cut + 1);
		}
		
		//Returning the file name
		return fileName;
	}
	
	public static long getUriSize(Context context, Uri uri) {
		//Attempting to pull the file name from the content resolver
		if("content".equals(uri.getScheme())) {
			try(Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
				if(cursor != null && cursor.moveToFirst()) return cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
			} catch(SecurityException exception) {
				exception.printStackTrace();
				return -1;
			}
		}
		
		//Returning -1
		return -1;
	}
	
	/**
	 * Compares file paths to check if the target file is parented anywhere under the parent directory
	 * EXAMPLE:
	 * /user/photos/2016, /user/photos/2016/image.jpg -> TRUE
	 * /user/photos/2016, /user/photos/2016/january/12/image.jpg -> TRUE
	 * /user/photos/2016, /user/images/2016/image.jpg -> FALSE
	 * /user/photos/2016, /user/photos/2016 -> TRUE
	 * /user/photos/2016, /user/photos/image.jpg -> FALSE
	 * @param parentDir the parent directory
	 * @param targetFile the target file to check ownership over
	 * @return whether or not the parent directory directly or indirectly encapsulates the target file
	 */
	public static boolean checkFileParent(File parentDir, File targetFile) {
		//Path-indexing both files
		List<File> parentDirPath = new ArrayList<>();
		List<File> targetFilePath = new ArrayList<>();
		{
			File file = parentDir;
			while((file = file.getParentFile()) != null) parentDirPath.add(0, file);
			file = targetFile;
			while((file = file.getParentFile()) != null) targetFilePath.add(0, file);
		}
		
		//Returning false if the parent path is longer than the target path (the target file path should be longer, as it is checking for a sub-file)
		if(parentDirPath.size() > targetFilePath.size()) return false;
		
		//Returning false if there is a folder mismatch anywhere along the way (the paths don't line up)
		for(int i = 0; i < parentDirPath.size(); i++) if(!parentDirPath.get(i).equals(targetFilePath.get(i))) return false;
		
		//Returning true (all the folders matched)
		return true;
	}
	
	/**
	 * Retrieves the duration of a media file (ie. audio)
	 * @param file the location of the file to check
	 * @return the duration of the media file in milliseconds
	 */
	public static long getMediaDuration(File file) {
		//Creating a new media metadata retriever
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		
		try {
			//Setting the source file
			mmr.setDataSource(file.getPath());
			
			//Getting the duration
			return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} catch(RuntimeException exception) {
			//Printing the stack trace
			exception.printStackTrace();
		} finally {
			//Releasing the media metadata retriever
			mmr.release();
		}
		
		//Returning an invalid value
		return -1;
	}
	
	/**
	 * Retrieves the duration of a media file (ie. audio)
	 * @param context context to use to resolve the URI
	 * @param uri the uri of the file to check
	 * @return the duration of the media file in milliseconds
	 */
	public static long getMediaDuration(Context context, Uri uri) {
		//Creating a new media metadata retriever
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		
		try {
			//Setting the source file
			mmr.setDataSource(context, uri);
			
			//Getting the duration
			return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} catch(RuntimeException exception) {
			//Printing the stack trace
			exception.printStackTrace();
		} finally {
			//Releasing the media metadata retriever
			mmr.release();
		}
		
		//Returning an invalid value
		return -1;
	}
	
	private static Random random = new Random(System.currentTimeMillis());
	public static Random getRandom() {
		return random;
	}
	
	public static float lerp(float val, float start, float end) {
		return val * (end - start) + start;
	}
	
	public static Drawable createRoundedDrawable(boolean softenTop, boolean softenBottom, boolean alignToRight, int pxRadiusNormal, int pxRadiusSoftened) {
		return createRoundedDrawable(new GradientDrawable(), softenTop, softenBottom, alignToRight, pxRadiusNormal, pxRadiusSoftened);
	}
	
	public static Drawable createRoundedDrawable(GradientDrawable drawable, boolean softenTop, boolean softenBottom, boolean alignToRight, int pxRadiusNormal, int pxRadiusSoftened) {
		//Determining the radius values
		int radiusTop = softenTop ? pxRadiusSoftened : pxRadiusNormal;
		int radiusBottom = softenBottom ? pxRadiusSoftened : pxRadiusNormal;
		
		//Setting the radius
		drawable.setCornerRadii(alignToRight ?
								new float[]{pxRadiusNormal, pxRadiusNormal,
											radiusTop, radiusTop,
											radiusBottom, radiusBottom,
											pxRadiusNormal, pxRadiusNormal} :
								new float[]{radiusTop, radiusTop,
											pxRadiusNormal, pxRadiusNormal,
											pxRadiusNormal, pxRadiusNormal,
											radiusBottom, radiusBottom});
		
		//Returning the drawable
		return drawable;
	}
	
	public static Drawable createRoundedDrawableTop(GradientDrawable drawable, boolean softenTop, boolean softenBottom, boolean alignToRight, int pxRadiusNormal, int pxRadiusSoftened) {
		//Determining the radius values
		int radiusTop = softenTop ? pxRadiusSoftened : pxRadiusNormal;
		
		//Setting the radius
		drawable.setCornerRadii(alignToRight ?
								new float[]{pxRadiusNormal, pxRadiusNormal,
											radiusTop, radiusTop,
											0, 0,
											0, 0} :
								new float[]{radiusTop, radiusTop,
											pxRadiusNormal, pxRadiusNormal,
											0, 0,
											0, 0});
		
		//Returning the drawable
		return drawable;
	}
	
	public static Drawable createRoundedDrawableBottom(GradientDrawable drawable, boolean softenTop, boolean softenBottom, boolean alignToRight, int pxRadiusNormal, int pxRadiusSoftened) {
		//Determining the radius values
		int radiusBottom = softenBottom ? pxRadiusSoftened : pxRadiusNormal;
		
		//Setting the radius
		drawable.setCornerRadii(alignToRight ?
								new float[]{0, 0,
											0, 0,
											radiusBottom, radiusBottom,
											pxRadiusNormal, pxRadiusNormal} :
								new float[]{0, 0,
											0, 0,
											pxRadiusNormal, pxRadiusNormal,
											radiusBottom, radiusBottom});
		
		//Returning the drawable
		return drawable;
	}
	
	/* interface ViewSource {
		View getView();
	} */
	
	public static String getMimeType(Context context, Uri uri) {
		//if(uri == null) return defaultMIMEType;
		String type = context.getContentResolver().getType(uri);
		return type == null ? defaultMIMEType : type;
	}
	
	public static String fileExtensionFromURL(String url) {
		int separatorIndex = url.lastIndexOf(".");
		int queryIndex = url.lastIndexOf("?");
		if(separatorIndex == -1) return null;
		else if(separatorIndex < queryIndex) return url.substring(separatorIndex + 1, queryIndex);
		else return url.substring(separatorIndex + 1);
	}
	
	public static String getMimeType(File file) {
		String extension = fileExtensionFromURL(file.getPath());
		if(extension != null) {
			String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			return type == null ? defaultMIMEType : type;
		}
		return defaultMIMEType;
	}
	
	public interface ResultCallback<T> {
		void onResult(boolean wasTasked, T result);
	}
	
	public interface MultiResultCallback<T> {
		void onResult(T... result);
	}
	
	public interface TaskedResultCallback<T> {
		void onResult(T result, boolean wasTasked);
	}
	
	public static class ValueWrapper<T> {
		public T value;
		
		public ValueWrapper(T value) {
			this.value = value;
		}
	}
	
	public static class CustomTabsLinkTransformationMethod implements TransformationMethod {
		//Creating the custom values
		private final int color;
		
		public CustomTabsLinkTransformationMethod(int color) {
			this.color = color;
		}
		
		@Override
		public CharSequence getTransformation(CharSequence source, View view) {
			if(view instanceof TextView) {
				TextView textView = (TextView) view;
				if(textView.getText() == null || !(textView.getText() instanceof Spannable)) {
					return source;
				}
				Spannable text = (Spannable) textView.getText();
				URLSpan[] spans = text.getSpans(0, textView.length(), URLSpan.class);
				for(int i = spans.length - 1; i >= 0; i--) {
					URLSpan oldSpan = spans[i];
					int start = text.getSpanStart(oldSpan);
					int end = text.getSpanEnd(oldSpan);
					String url = oldSpan.getURL();
					text.removeSpan(oldSpan);
					text.setSpan(new CustomTabsURLSpan(url, color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				return text;
			}
			return source;
		}
		
		@Override
		public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect) {
		
		}
	}
	
	public static class CustomTabsURLSpan extends URLSpan {
		//Creating the custom values
		private final int color;
		
		CustomTabsURLSpan(String url, int color) {
			super(url);
			this.color = color;
		}
		
		CustomTabsURLSpan(Parcel src, int color) {
			super(src);
			this.color = color;
		}
		
		@Override
		public void onClick(View widget) {
			//Returning if the widget has disabled link clicking
			if(!((TextView) widget).getLinksClickable()) return;
			
			//Launching the custom tab
			new CustomTabsIntent.Builder()
					.setToolbarColor(color)
					.setSecondaryToolbarColor(color)
					.build()
					.launchUrl(widget.getContext(), Uri.parse(getURL()));
			//super.onClick(widget);
			// attempt to open with custom tabs, if that fails, call super.onClick
		}
	}
	
	public static boolean containsIgnoreCase(String str, String searchStr) {
		if(str == null || searchStr == null) return false;
		
		final int length = searchStr.length();
		if (length == 0)
			return true;
		
		for (int i = str.length() - length; i >= 0; i--) {
			if (str.regionMatches(true, i, searchStr, 0, length))
				return true;
		}
		return false;
	}
	
	public static String normalizeAddress(String address) {
		//Returning the E-Mail if the address is one (can't be normalized)
		if(address.contains("@")) return address;
		
		//Returning the stripped phone number if the address is one
		//if(address.matches("^\\+?[ \\d().-]+$")) return address.replaceAll("[^\\d+]", "");
		if(PhoneNumberUtils.isWellFormedSmsAddress(address)) {
			String formattedNumber = PhoneNumberUtils.formatNumberToE164(address, "US");
			if(formattedNumber != null) return formattedNumber;
		}
		
		//Returning the address directly (unknown type)
		return address;
	}
	
	public static List<String> normalizeAddresses(List<String> addresses) {
		//Normalizing the addresses
		ListIterator<String> iterator = addresses.listIterator();
		while(iterator.hasNext()) iterator.set(normalizeAddress(iterator.next()));
		
		//Returning the addresses
		return addresses;
	}
	
	public static boolean validateAddress(String address) {
		return validateEmail(address) || validatePhoneNumber(address);
	}
	
	public static boolean validateEmail(String address) {
		return address.matches(emailRegEx);
	}
	
	public static boolean validatePhoneNumber(String address) {
		return address.replaceAll("[^\\d+]", "").length() >= 3 && address.matches("^\\+?[ \\d().-]+$");
	}
	
	public static String stripPhoneNumber(String address) {
		return address.replaceAll("[^\\d]", "");
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueDesc(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, (o1, o2) -> -o1.getValue().compareTo(o2.getValue()));
		
		Map<K, V> result = new LinkedHashMap<>();
		for(Map.Entry<K, V> entry : list) result.put(entry.getKey(), entry.getValue());
		return result;
	}
	
	public static String createLocalizedList(String[] list, Resources resources) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if(list.length == 1) stringBuilder.append(resources.getString(R.string.list_single, list[0]));
		else if(list.length == 2) stringBuilder.append(resources.getString(R.string.list_double, list[0], list[1]));
		else if(list.length > 2) {
			stringBuilder.append(resources.getString(R.string.list_n_start, list[0]));
			for(int i = 1; i < list.length - 1; i++) stringBuilder.append(resources.getString(R.string.list_n_middle, list[i]));
			stringBuilder.append(resources.getString(R.string.list_n_end, list[list.length - 1]));
		}
		
		return stringBuilder.toString();
	}
	
	/* public static abstract class ActivityViewModel<A extends Activity> extends ViewModel {
		private final WeakReference<A> activityReference;
		
		public ActivityViewModel(@NonNull A activity) {
			activityReference = new WeakReference<>(activity);
		}
		
		public A getActivity() {
			return activityReference.get();
		}
	} */
	
	/* static void enforceContentWidthView(Resources resources, View view) {
		//Getting the maximum content width
		int maxContentWidth = resources.getDimensionPixelSize(R.dimen.contentwidth_max);
		
		//Enforcing the maximum content width
		view.post(() -> {if(view.getMeasuredWidth() > maxContentWidth) view.getLayoutParams().width = maxContentWidth;});
	} */
	
	public static int getMaxContentWidth(Resources resources) {
		return resources.getDimensionPixelSize(R.dimen.contentwidth_max);
	}
	
	/**
	 * Helper method for calculatePaddingContentWidth() that handles finding the width
	 * @param resources Android resources, to fetch the maximum content width
	 * @param view The view to limit
	 * @return padding (in pixels) that should be applied to the view
	 */
	public static int calculatePaddingContentWidth(Resources resources, View view) {
		return calculatePaddingContentWidth(getMaxContentWidth(resources), view);
	}
	
	/**
	 * Calculates the size that should be allocated to padding in order to keep the view a reasonable width
	 * @param maxContentWidth The size to limit the view to
	 * @param view The view to limit
	 * @return padding (in pixels) that should be applied to the view
	 */
	public static int calculatePaddingContentWidth(int maxContentWidth, View view) {
		//Getting the width
		int width = view.getWidth();
		
		//Returning if the view is already below the width
		if(width <= maxContentWidth) return 0;
		
		//Returning the padding
		return (width - maxContentWidth) / 2;
	}
	
	/**
	 * Friendly method to quickly enforce the maximum content width on a specified view
	 * @param resources Android resources, to fetch the maximum content width
	 * @param view The view to limit
	 */
	public static void enforceContentWidthView(Resources resources, View view) {
		view.post(() -> enforceContentWidthImmediate(getMaxContentWidth(resources), view));
	}
	
	/**
	 * Adds padding to either side of the view to prevent it from expanding to take up the entire display, if the screen is large enough
	 * @param maxContentWidth The size to limit the view to
	 * @param view The view to limit
	 */
	public static void enforceContentWidthImmediate(int maxContentWidth, View view) {
		//Calculating and applying the margin
		int padding = calculatePaddingContentWidth(maxContentWidth, view);
		//view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
		ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		layoutParams.leftMargin = padding;
		layoutParams.rightMargin = padding;
		view.setLayoutParams(layoutParams);
	}
	
	public static void enforceContentWidthImmediatePadding(int maxContentWidth, View view) {
		//Calculating and applying the margin
		int padding = calculatePaddingContentWidth(maxContentWidth, view);
		view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
	}
	
	/* private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
	static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
		//Creating a list of the entries
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		
		//Sorting by value
		Collections.sort(list, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));
		
		//Rebuilding the hashmap
		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) result.put(entry.getKey(), entry.getValue());
		
		//Returning the list
		return result;
	} */
	
	public static String exceptionToString(Throwable exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		return exception.getMessage() + ":\n" + sw.toString();
	}
	
	public static TypedValue resolveThemeAttr(Context context, @AttrRes int attrRes) {
		Resources.Theme theme = context.getTheme();
		TypedValue typedValue = new TypedValue();
		theme.resolveAttribute(attrRes, typedValue, true);
		return typedValue;
	}
	
	@ColorInt
	public static int resolveColorAttr(Context context, @AttrRes int colorAttr) {
		TypedValue resolvedAttr = resolveThemeAttr(context, colorAttr);
		// resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
		int colorRes = resolvedAttr.resourceId != 0 ? resolvedAttr.resourceId : resolvedAttr.data;
		return ContextCompat.getColor(context, colorRes);
	}
	
	public static float resolveFloatAttr(Context context, @AttrRes int floatAttr) {
		TypedArray typedArray = context.obtainStyledAttributes(new TypedValue().data, new int[]{floatAttr});
		float value = typedArray.getFloat(0, -1);
		typedArray.recycle();
		return value;
	}
	
	public static Drawable resolveDrawableAttr(Context context, @AttrRes int floatAttr) {
		TypedArray typedArray = context.obtainStyledAttributes(new TypedValue().data, new int[]{floatAttr});
		Drawable value = typedArray.getDrawable(0);
		typedArray.recycle();
		return value;
	}
	
	public static boolean isNightMode(Resources resources) {
		switch (resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
			case Configuration.UI_MODE_NIGHT_YES:
				return true;
			case Configuration.UI_MODE_NIGHT_NO:
			case Configuration.UI_MODE_NIGHT_UNDEFINED:
			default:
				return false;
		}
	}
	
	public static void themeToolbar(Toolbar toolbar) {
		if(isNightMode(toolbar.getResources())) return;
		toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Dark);
	}
	
	public static class ResizeAnimation extends Animation {
		//Creating the parameter values
		private int startHeight;
		private int deltaHeight; // distance between start and end height
		
		//Creating the view value
		private final View view;
		
		/**
		 * constructor, do not forget to use the setParams(int, int) method before
		 * starting the animation
		 * @param view the target view to animate
		 * @param startHeight height in pixels
		 * @param endHeight height in pixels
		 */
		public ResizeAnimation(View view, int startHeight, int endHeight) {
			//Setting the view
			this.view = view;
			
			//Setting the parameters
			this.startHeight = startHeight;
			deltaHeight = endHeight - startHeight;
		}
		
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation transformation) {
			view.getLayoutParams().height = (int) (startHeight + deltaHeight * interpolatedTime);
			view.requestLayout();
		}
		
		/**
		 * sets the duration for the animation
		 * @param duration duration in millis
		 */
		@Override
		public void setDuration(long duration) {
			super.setDuration(duration);
		}
		
		@Override
		public boolean willChangeBounds() {
			return true;
		}
	}
	
	/* interface BiConsumer<A1, A2> {
		void accept(A1 a1, A2 a2);
	} */
	
	public interface TriConsumer<A1, A2, A3> {
		void accept(A1 a1, A2 a2, A3 a3);
	}
	
	public static boolean validateContext(Context context) {
		if(context instanceof Activity) {
			Activity activity = (Activity) context;
			return !activity.isDestroyed() && !activity.isFinishing();
		}
		
		return true;
	}
	
	public static boolean checkBrokenPipe(IOException exception) {
		return exception.getMessage().toLowerCase().contains("broken pipe");
	}
	
	public static byte[] compressGZIP(byte[] data, int length) throws IOException, OutOfMemoryError {
		try(ByteArrayOutputStream fin = new ByteArrayOutputStream(); GZIPOutputStream out = new GZIPOutputStream(fin)) {
			out.write(data, 0, length);
			out.close();
			return fin.toByteArray();
		}
	}
	
	public static byte[] decompressGZIP(byte[] data) throws IOException, OutOfMemoryError {
		try(ByteArrayInputStream src = new ByteArrayInputStream(data); GZIPInputStream in = new GZIPInputStream(src);
			ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
			in.close();
			return out.toByteArray();
		}
	}
	
	public static boolean validateEffect(String effect) {
		return validateScreenEffect(effect) || validateBubbleEffect(effect) || validatePassiveEffect(effect);
	}
	
	public static boolean validateScreenEffect(String effect) {
		return appleSendStyleScrnEcho.equals(effect) ||
				appleSendStyleScrnSpotlight.equals(effect) ||
				appleSendStyleScrnBalloons.equals(effect) ||
				appleSendStyleScrnConfetti.equals(effect) ||
				appleSendStyleScrnLove.equals(effect) ||
				appleSendStyleScrnLasers.equals(effect) ||
				appleSendStyleScrnFireworks.equals(effect) ||
				appleSendStyleScrnShootingStar.equals(effect) ||
				appleSendStyleScrnCelebration.equals(effect);
	}
	
	public static boolean validateBubbleEffect(String effect) {
		return appleSendStyleBubbleSlam.equals(effect) ||
				appleSendStyleBubbleLoud.equals(effect) ||
				appleSendStyleBubbleGentle.equals(effect);
	}
	
	public static boolean validatePassiveEffect(String effect) {
		return appleSendStyleBubbleInvisibleInk.equals(effect);
	}
	
	public static float interpolate(float start, float end, float progress) {
		return start + (end - start) * progress;
	}
	
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public static String humanReadableByteCountInt(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%d %sB", (int) (bytes / Math.pow(unit, exp)), pre);
	}
	
	public static String intToFormattedString(int value) {
		return String.format(Locale.getDefault(), "%d", value);
	}
	
	public static String intToFormattedString(Resources resources, int value) {
		return String.format(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? resources.getConfiguration().getLocales().get(0) : resources.getConfiguration().locale, "%d", value);
	}
	
	/*
	cal1 < cal2 -> <0
	cal2 > cal1 -> >0
	 */
	public static int compareCalendarDates(Calendar cal1, Calendar cal2) {
		if(cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) return -1;
		if(cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) return 1;
		if(cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return -1;
		if(cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) return 1;
		return Integer.compare(cal1.get(Calendar.DAY_OF_YEAR), cal2.get(Calendar.DAY_OF_YEAR));
	}
	
	/* public public static abstract class BindingViewHolder extends RecyclerView.ViewHolder {
		public BindingViewHolder(View itemView) {
			super(itemView);
		}
		
		abstract BindingViewHolder bindView(View view);
	} */
	
	public static String listToString(List list, String delimiter) {
		StringBuilder stringBuilder = new StringBuilder();
		if(list.isEmpty()) return stringBuilder.toString();
		stringBuilder.append(list.get(0));
		for(int i = 1; i < list.size(); i++) stringBuilder.append(delimiter).append(list.get(i));
		return stringBuilder.toString();
	}
	
	public static boolean compareMimeTypes(String one, String two) {
		if(one.equals("*/*") || two.equals("*/*")) return true;
		if(!one.contains("/") || !two.contains("/")) {
			Crashlytics.logException(new IllegalArgumentException("Couldn't compare MIME types. Attempting to compare " + one + " and " + two));
			return false;
		}
		String[] oneComponents = one.split("/");
		String[] twoComponents = two.split("/");
		if(oneComponents[1].equals("*") || twoComponents[1].equals("*")) return oneComponents[0].equals(twoComponents[0]);
		return one.equals(two);
	}
	
	public static int getWindowHeight(Activity activity) {
		return activity.getWindow().getDecorView().getHeight();
	}
	
	public static void debugIntent(Intent intent, String tag) {
		Log.v(tag, "action: " + intent.getAction());
		Log.v(tag, "component: " + intent.getComponent());
		Bundle extras = intent.getExtras();
		if(extras != null) {
			for(String key : extras.keySet()) {
				Log.v(tag, "key [" + key + "]: " + extras.get(key));
			}
		} else {
			Log.v(tag, "no extras");
		}
	}
	
	public static class WeakRunnable implements Runnable {
		private WeakReference<Runnable> reference = null;
		
		void set(Runnable runnable) {
			reference = new WeakReference<>(runnable);
		}
		
		Runnable get() {
			return reference == null ? null : reference.get();
		}
		
		@Override
		public void run() {
			if(reference == null) return;
			Runnable runnable = reference.get();
			if(runnable == null) return;
			runnable.run();
		}
	}
	
	public static class WeakConsumer<T> implements Consumer<T> {
		private WeakReference<Consumer<T>> reference = null;
		
		void set(Consumer<T> consumer) {
			reference = new WeakReference<>(consumer);
		}
		
		Consumer<T> get() {
			return reference == null ? null : reference.get();
		}
		
		@Override
		public void accept(T t) {
			if(reference == null) return;
			Consumer<T> consumer = reference.get();
			if(consumer == null) return;
			consumer.accept(t);
		}
	}
	
	public static class WeakBiConsumer<T, U> implements BiConsumer<T, U> {
		private WeakReference<BiConsumer<T, U>> reference = null;
		
		void set(BiConsumer<T, U> consumer) {
			reference = new WeakReference<>(consumer);
		}
		
		BiConsumer<T, U> get() {
			return reference == null ? null : reference.get();
		}
		
		@Override
		public void accept(T t, U u) {
			if(reference == null) return;
			BiConsumer<T, U> consumer = reference.get();
			if(consumer == null) return;
			consumer.accept(t, u);
		}
	}
	
	public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
		private final int spaceTop, spaceBottom, spaceLeft, spaceRight;
		
		SpacesItemDecoration(int spaceTop, int spaceBottom, int spaceLeft, int spaceRight) {
			this.spaceTop = spaceTop;
			this.spaceBottom = spaceBottom;
			this.spaceLeft = spaceLeft;
			this.spaceRight = spaceRight;
		}
		
		@Override
		public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			outRect.top = spaceTop;
			outRect.bottom = spaceBottom;
			outRect.left = spaceLeft;
			outRect.right = spaceRight;
		}
	}
	
	public static class Tuple2<A, B> {
		public final A item1;
		public final B item2;
		
		public Tuple2(A item1, B item2) {
			this.item1 = item1;
			this.item2 = item2;
		}
	}
	
	public static class Tuple3<A, B, C> {
		public final A item1;
		public final B item2;
		public final C item3;
		
		public Tuple3(A item1, B item2, C item3) {
			this.item1 = item1;
			this.item2 = item2;
			this.item3 = item3;
		}
	}
	
	public static List<FirebaseTextMessage> messageToFirebaseMessageList(List<MessageInfo> messageList) {
		List<FirebaseTextMessage> list = new ArrayList<>();
		
		for(MessageInfo message : messageList) {
			if(message.getMessageText() == null) continue;
			list.add(message.getSender() == null ? FirebaseTextMessage.createForLocalUser(message.getMessageText(), message.getDate()) : FirebaseTextMessage.createForRemoteUser(message.getMessageText(), message.getDate(), message.getSender()));
		}
		
		return list;
	}
	
	@RequiresApi(api = Build.VERSION_CODES.Q)
	public static List<ConversationActions.Message> messageToTextClassifierMessageList(List<MessageInfo> messageList) {
		List<ConversationActions.Message> list = new ArrayList<>();
		
		for(MessageInfo message : messageList) {
			if(message.getMessageText() == null) continue;
			Person author = message.isOutgoing() ? ConversationActions.Message.PERSON_USER_SELF : ConversationActions.Message.PERSON_USER_OTHERS;//new Person.Builder().setName(message.getSender()).build();
			ConversationActions.Message systemMessage = new ConversationActions.Message.Builder(author)
					.setReferenceTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.getDate()), ZoneId.systemDefault()))
					.setText(message.getMessageText())
					.build();
			list.add(systemMessage);
		}
		
		return list;
	}
	
	public static void printViewHierarchy(ViewGroup vg, String prefix) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View v = vg.getChildAt(i);
			String desc = prefix + " | " + "[" + i + "/" + (vg.getChildCount()-1) + "] "+ v.getClass().getSimpleName() + " " + v.getId();
			Log.v("x", desc);
			
			if (v instanceof ViewGroup) {
				printViewHierarchy((ViewGroup)v, desc);
			}
		}
	}
	
	public static void setActivityAMOLEDBase(AppCompatActivity activity) {
		activity.findViewById(android.R.id.content).getRootView().setBackgroundColor(Constants.colorAMOLED);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) activity.getWindow().setNavigationBarColor(Constants.colorAMOLED); //Leaving the transparent navigation bar on Android 10
		activity.getWindow().setStatusBarColor(Constants.colorAMOLED);
		activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Constants.colorAMOLED));
		
		for(View view : Constants.getViewsByTag(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.tag_amoleddivider))) {
			view.setVisibility(View.VISIBLE);
		}
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			//The bottom divider is only necessary for actual navigation bars
			for(View view : Constants.getViewsByTag(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.tag_amoleddivider_bottom))) {
				view.setVisibility(View.VISIBLE);
			}
		}
	}
	
	public static boolean shouldUseAMOLED(Context context) {
		return isNightMode(context.getResources()) && Preferences.getPreferenceAMOLED(context);
	}
	
	public static boolean isChromeOS(Context context) {
		return context.getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
	}
	
	public static void updateChromeOSStatusBar(AppCompatActivity activity) {
		//Ignoring if not running on a Chrome OS device
		if(!isChromeOS(activity)) return;
		
		//Setting the statusbar color
		activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.colorSubBackground, null));
	}
	
	public static Bitmap loadBitmapFromView(View view) {
		int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		view.measure(measureSpec, measureSpec);
		Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		view.draw(canvas);
		return bitmap;
	}
	
	public static void launchUri(Context context, Uri uri) {
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		
		if(intent.resolveActivity(context.getPackageManager()) != null) context.startActivity(intent);
		else Toast.makeText(context, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show();
	}
	
	public static String cleanFileName(String fileName) {
		return fileName.replace('\u0000', '?').replace('/', '-');
	}
	
	//https://stackoverflow.com/questions/9607903/get-domain-name-from-given-url
	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		if(domain == null) return null;
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}
	
	public static String locationToString(LatLng position) {
		return "(" + String.format(Locale.getDefault(), "%.5f", position.latitude) + ", " + String.format(Locale.getDefault(), "%.5f", position.longitude) + ")";
	}
	
	public static void createFileSAF(Activity activity, int requestCode, String mimeType, String fileName) {
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		
		// Filter to only show results that can be "opened", such as
		// a file (as opposed to a list of contacts or timezones).
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		
		// Create a file with the requested MIME type.
		intent.setType(mimeType);
		intent.putExtra(Intent.EXTRA_TITLE, fileName);
		activity.startActivityForResult(intent, requestCode);
	}
	
	public static void exportFile(Context context, File file) {
		Intent intent = new Intent(context, FileExportService.class);
		intent.putExtra(Constants.intentParamData, file.getPath());
		context.startService(intent);
	}
	
	public static void exportUri(Context context, File sourceFile, Uri targetUri) {
		Intent intent = new Intent(context, UriExportService.class);
		intent.putExtra(Constants.intentParamData, sourceFile);
		intent.putExtra(Constants.intentParamTarget, targetUri);
		context.startService(intent);
	}
	
	public static boolean isCharEmoji(int codePoint) {
		return (codePoint >= 0x1F600 && codePoint <= 0x1F64F) ||
		   (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) ||
		   (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) ||
		   (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF) ||
		   (codePoint >= 0x2600 && codePoint <= 0x26FF) ||
		   (codePoint >= 0x2700 && codePoint <= 0x27BF) ||
		   (codePoint >= 0xE0020 && codePoint <= 0xE007F) ||
		   (codePoint >= 0xFE00 && codePoint <= 0xFE0F) ||
		   (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) ||
		   (codePoint >= 0x1F018 && codePoint <= 0x1F270) ||
		   (codePoint >= 0x238C && codePoint <= 0x2454) ||
		   (codePoint >= 0x20D0 && codePoint <= 0x20FF);
	}
	
	public static boolean isZeroWidthJoiner(int codePoint) {
		return codePoint == 8205;
	}
	
	public static boolean stringContainsOnlyEmoji(String string) {
		//Ignoring if the string is empty
		if(string.isEmpty()) return false;
		
		//Returning if there are any non-emoji characters in the string
		int length = string.length();
		for(int offset = 0; offset < length;) {
			int codePoint = string.codePointAt(offset);
			if(!isCharEmoji(codePoint) && !isZeroWidthJoiner(codePoint)) return false;
			offset += Character.charCount(codePoint);
		}
		
		//Returning true
		return true;
	}
	
	public static boolean isDefaultMessagingApp(Context context) {
		return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
	}
	
	public static com.klinker.android.send_message.Transaction getMMSSMSTransaction(Context context, long messageLocalID) {
		com.klinker.android.send_message.Settings settings = new com.klinker.android.send_message.Settings();
		settings.setUseSystemSending(true);
		settings.setDeliveryReports(Preferences.getPreferenceSMSDeliveryReports(MainApplication.getInstance()));
		com.klinker.android.send_message.Transaction transaction = new com.klinker.android.send_message.Transaction(context, settings);
		
		//Creating the parcelable data
		SMSIDParcelable parcelData = new SMSIDParcelable(messageLocalID);
		
		//Setting the intent data
		Bundle bundle = new Bundle();
		bundle.putParcelable(Constants.intentParamData, parcelData);
		
		{
			Intent intent = new Intent(MainApplication.getInstance(), TextMMSSentReceiver.class);
			intent.putExtra(Constants.intentParamData, bundle);
			transaction.setExplicitBroadcastForSentMms(intent);
		}
		{
			Intent intent = new Intent(MainApplication.getInstance(), TextSMSSentReceiver.class);
			intent.putExtra(Constants.intentParamData, bundle);
			transaction.setExplicitBroadcastForSentSms(intent);
		}
		{
			Intent intent = new Intent(MainApplication.getInstance(), TextSMSDeliveredReceiver.class);
			intent.putExtra(Constants.intentParamData, bundle);
			transaction.setExplicitBroadcastForDeliveredSms(intent);
		}
		
		//Returning the transaction
		return transaction;
	}
}