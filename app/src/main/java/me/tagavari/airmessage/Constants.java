package me.tagavari.airmessage;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Parcel;
import android.provider.OpenableColumns;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.TransformationMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

class Constants {
	//Creating the constants
	static final int intentPickMediaFile = 1;
	static final int intentPickAnyFile = 2;
	static final int intentTakePicture = 3;
	static final int permissionRecordAudio = 4;
	static final int permissionReadContacts = 5;
	
	static final int intentDisconnectService = 6;
	
	static final String intentParamTargetID = "targetID";
	static final String intentParamGuid = "guid";
	static final String intentParamResult = "result";
	static final String intentParamData = "data";
	static final String intentParamDataText = "dataText";
	static final String intentParamDataFile = "dataFile";
	static final String intentParamSize = "size";
	static final String intentParamIndex = "index";
	static final String intentParamTime = "time";
	static final String intentParamIsLast = "isLast";
	static final String intentParamList = "list";
	static final String intentParamAction = "action";
	static final String intentParamCurrent = "current";
	
	static final String notificationReplyKey = "REMOTE_INPUT_REPLY";
	
	static final String appleSendStyleInvisibleInk = "com.apple.MobileSMS.expressivesend.invisibleink";
	static final String appleSendStyleConfetti = "com.apple.messages.effect.CKConfettiEffect";
	static final String appleSendStyleFireworks = "com.apple.messages.effect.CKFireworksEffect";
	
	static final int messageErrorCodeOK = 0; //No error
	
	static final int messageErrorCodeAppleNetwork = 3; //Network error
	static final int messageErrorCodeAppleUnregistered = 22; //Not registered with iMessage
	
	static final int messageErrorCodeAirInvalidContent = -1; //Invalid content
	static final int messageErrorCodeAirFileTooLarge = -2; //Attachment too large
	static final int messageErrorCodeAirIO = -3; //IO exception
	static final int messageErrorCodeAirNetwork = -4; //Network exception
	static final int messageErrorCodeAirExternal = -5; //External exception
	static final int messageErrorCodeAirExpired = -6; //Request expired
	static final int messageErrorCodeAirReferences = -7; //References lost
	//static final int messageErrorCodeLocalReferences = -4; //Unknown error
	
	static final int groupActionInvite = 0;
	static final int groupActionLeave = 1;
	
	static final Uri serverSetupAddress = Uri.parse("http://airmessage.org/guide");
	static final Uri serverUpdateAddress = Uri.parse("https://plus.google.com/communities/106264748879310604272/stream/30250b2a-f7a4-4f6c-a140-fb4b33096f8b");
	static final Uri googlePlusCommunityAddress = Uri.parse("https://plus.google.com/communities/106264748879310604272");
	static final String feedbackEmail = "hello@airmessage.org";
	
	static final String defaultPort = ":1359";
	static final String defaultProtocol = "wss://";
	static final String recordingName = "recording.amr";
	static final String pictureName = "image.jpg";
	static final String defaultFileName = "file";
	
	static final String serviceIDAppleMessage = "iMessage";
	static final String serviceIDSMS = "SMS";
	
	//static final int viewTagTypeKey = 0;
	static final String viewTagTypeItem = "item";
	static final String viewTagTypeAction = "action";
	
	//Creating the reference constants
	static final float disabledAlpha = 0.54f;
	
	//Creating the regular expression constants
	private static final String emailRegEx = "^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$";
	
	static boolean requestPermission(Activity activity, String[] permissions, int requestID) {
		//Creating the missing permission list
		ArrayList<String> missingPermissions = new ArrayList<>();
		
		//Adding all rejected permissions to the list
		for(String permission : permissions)
			if(ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED)
				missingPermissions.add(permission);
		
		//Returning if there are no missing permissions
		if(missingPermissions.isEmpty()) return false;
		
		//Requesting the permission
		ActivityCompat.requestPermissions(activity, missingPermissions.toArray(new String[]{}), requestID);
		
		//Returning true
		return true;
	}
	
	static String getFormattedDuration(long seconds) {
		return DateUtils.formatElapsedTime(seconds);
		/* //Getting the values
		int seconds = (int) (duration / 1000L);
		int minutes = seconds / 60;
		seconds %= 60;
		int hours = minutes / 60;
		minutes %= 60;
		
		//Getting the values as string
		String hourString = Integer.toString(hours);
		String minuteString = Integer.toString(minutes);
		String secondString = Integer.toString(seconds);
		
		//Adding an extra 0 if the number is only 1 digit
		if(minuteString.length() <= 1) minuteString = "0" + minuteString;
		if(secondString.length() <= 1) secondString = "0" + secondString;
		
		//Checking if the duration is more than an hour
		if(hours >= 1f) {
			//Returning the time with hours
			return hourString + ":" + minuteString + ":" + secondString;
		} else {
			//Returning the time without hours
			return minuteString + ":" + secondString;
		} */
	}
	
	static int dpToPx(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}
	
	static int pxToDp(int px) {
		return (int) (px / Resources.getSystem().getDisplayMetrics().density);
	}
	
	static void recursiveDelete(File file) {
		if(file.isFile()) file.delete();
		else {
			for(File childFiles : file.listFiles()) recursiveDelete(childFiles);
			file.delete();
		}
	}
	
	static <T> boolean arrayContains(T[] array, T value) {
		for(T entry : array) if(entry.equals(value)) return true;
		return false;
	}
	
	static ArrayList<View> getViewsByTag(ViewGroup root, String tag) {
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
	
	interface ViewSource {
		View get();
	}
	
	interface TaskedViewSource {
		View get(boolean wasTasked);
	}
	
	private static final Pattern regExNumerated = Pattern.compile("_\\d+?$");
	private static final String regExSplitFilename = "\\.(?=[^.]+$)";
	static File findFreeFile(File directory, String fileName) {
		//Creating the file
		File file = new File(directory, fileName);
		
		//Checking if the file directory doesn't exist
		if(!directory.exists()) {
			//Creating the directory
			directory.mkdir();
			
			//Returning the file
			return file;
		}
		
		//Looping while the file exists
		while(file.exists()) {
			//Getting the file name and extension
			String[] fileData = file.getName().split(regExSplitFilename);
			String baseFileName = fileData[0];
			String fileExtension = fileData.length > 1 ? fileData[1] : "";
			
			//Checking if the base file name ends with an underscore followed by a number
			if(regExNumerated.matcher(baseFileName).find()) {
				//Creating the starting substring variable
				int numberStartChar = 0;
				
				//Finding the substring start
				for(int i = 0; i < baseFileName.length(); i++)
					if(baseFileName.charAt(i) == '_') numberStartChar = i + 1;
				
				//Getting the number
				int number = Integer.parseInt(baseFileName.substring(numberStartChar)) + 1;
				
				//Substringing the base file name to leave just the name and the underscore
				baseFileName = baseFileName.substring(0, numberStartChar);
				
				//Adding the number to the base file name
				baseFileName += number;
			} else {
				//Adding the number to the base file name
				baseFileName += "_0";
			}
			
			//Adding the extension to the base file name
			baseFileName += "." + fileExtension;
			
			//Setting the new file
			file = new File(directory, baseFileName);
		}
		
		//Returning the file
		return file;
	}
	
	static String getFileName(Context context, Uri uri) {
		//Creating the file name variable
		String fileName = null;
		
		//Attempting to pull the file name from the content resolver
		if(uri.getScheme().equals("content")) {
			Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
			if(cursor != null) {
				if(cursor.moveToFirst()) fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
				cursor.close();
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
	
	private static Random random = new Random(System.currentTimeMillis());
	static Random getRandom() {
		return random;
	}
	
	static void printViewHierarchy(ViewGroup vg, String prefix) {
		for(int i = 0; i < vg.getChildCount(); i++) {
			View v = vg.getChildAt(i);
			String desc = prefix + " | " + "[" + i + "/" + (vg.getChildCount()-1) + "] "+ v.getClass().getSimpleName() + " " + v.getId();
			System.out.println(desc);
			
			if(v instanceof ViewGroup) printViewHierarchy((ViewGroup) v, desc);
		}
	}
	
	static Drawable createRoundedDrawable(boolean softenTop, boolean softenBottom, boolean alignToRight, int pxRadiusNormal, int pxRadiusSoftened) {
		//Creating the drawable
		GradientDrawable drawable = new GradientDrawable();
		
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
	
	/* interface ViewSource {
		View getView();
	} */
	
	static String getMimeType(Context context, Uri uri) {
		return context.getContentResolver().getType(uri);
	}
	
	static String getMimeType(File file) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(file.getPath());
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}
	
	interface ResultCallback<T> {
		void onResult(boolean wasTasked, T result);
	}
	
	interface MultiResultCallback<T> {
		void onResult(T... result);
	}
	
	interface TaskedResultCallback<T> {
		void onResult(T result, boolean wasTasked);
	}
	
	static class ValueWrapper<T> {
		public T value;
		
		ValueWrapper(T value) {
			this.value = value;
		}
	}
	
	public static class CustomTabsLinkTransformationMethod implements TransformationMethod {
		//Creating the custom values
		private final int color;
		
		CustomTabsLinkTransformationMethod(int color) {
			this.color = color;
		}
		
		@Override
		public CharSequence getTransformation(CharSequence source, View view) {
			if (view instanceof TextView) {
				TextView textView = (TextView) view;
				Linkify.addLinks(textView, Linkify.ALL);
				if (textView.getText() == null || !(textView.getText() instanceof Spannable)) {
					return source;
				}
				Spannable text = (Spannable) textView.getText();
				URLSpan[] spans = text.getSpans(0, textView.length(), URLSpan.class);
				for (int i = spans.length - 1; i >= 0; i--) {
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
	
	static boolean containsIgnoreCase(String str, String searchStr) {
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
	
	static String normalizeAddress(String address) {
		//Returning the E-Mail if the address is one (can't be normalized)
		if(address.contains("@")) return address;
		
		//Returning the stripped phone number the address it is one
		if(address.matches("^\\+?[ \\d().-]+$")) return address.replaceAll("[^\\d+]", "");
		
		//Returning the address (unknown type)
		return address;
	}
	
	static ArrayList<String> normalizeAddresses(ArrayList<String> addresses) {
		//Normalizing the addresses
		ListIterator<String> iterator = addresses.listIterator();
		while(iterator.hasNext()) iterator.set(normalizeAddress(iterator.next()));
		
		//Returning the addresses
		return addresses;
	}
	
	static boolean validateAddress(String address) {
		//Returning true if the address is an E-Mail address
		if(address.matches(emailRegEx)) return true;
		
		//Returning true if the address is a telephone number
		if(address.replaceAll("[^\\d+]", "").length() >= 3 && address.matches("^\\+?[ \\d().-]+$")) return true;
		
		//Returning false
		return false;
	}
	
	static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueDesc(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort( list, (o1, o2) -> -o1.getValue().compareTo(o2.getValue()));
		
		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) result.put(entry.getKey(), entry.getValue());
		return result;
	}
	
	static float calculateImageAttachmentMultiplier(Resources resources, int width, int height) {
		//Getting the min and max values
		int pxBitmapSizeMin = (int) resources.getDimension(R.dimen.image_size_min);
		int pxBitmapSizeMax = (int) resources.getDimension(R.dimen.image_size_max);
		
		//Calculating the multiplier
		int[] sortedDimens = width < height ? new int[]{width, height} : new int[]{height, width};
		float multiplier = 1;
		if(sortedDimens[0] < pxBitmapSizeMin) multiplier = (float) pxBitmapSizeMin / sortedDimens[0];
		if(sortedDimens[1] > pxBitmapSizeMax) multiplier = (float) pxBitmapSizeMax / sortedDimens[1];
		
		//Returning the multiplier
		return multiplier;
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
}