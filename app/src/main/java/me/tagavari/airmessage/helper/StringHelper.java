package me.tagavari.airmessage.helper;

public class StringHelper {
	public static String nullifyEmptyString(String string) {
		return defaultEmptyString(string, null);
	}
	
	public static String defaultEmptyString(String string, String fallback) {
		if(string != null && string.isEmpty()) return fallback;
		else return string;
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
	
	public static boolean isCharEmoji(int codePoint) {
		return (codePoint >= 0x1F600 && codePoint <= 0x1F64F) ||
				(codePoint >= 0x1F300 && codePoint <= 0x1F5FF) ||
				(codePoint >= 0x1F680 && codePoint <= 0x1F6FF) ||
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
}