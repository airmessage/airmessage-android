package me.tagavari.airmessage.helper

object StringHelper {
	@JvmStatic
	fun nullifyEmptyString(string: String?): String? {
		return if(string.isNullOrEmpty()) null else string
	}
	
	@JvmStatic
	fun defaultEmptyString(string: String?, fallback: String): String {
		return if(string.isNullOrEmpty()) fallback else string
	}
	
	@JvmStatic
	fun stringContainsOnlyEmoji(string: String): Boolean {
		//Ignoring if the string is empty
		if(string.isEmpty()) return false
		
		//Returning if there are any non-emoji characters in the string
		val length = string.length
		var offset = 0
		while(offset < length) {
			val codePoint = string.codePointAt(offset)
			if(!isCharEmoji(codePoint) && !isZeroWidthJoiner(codePoint)) return false
			offset += Character.charCount(codePoint)
		}
		
		//Returning true
		return true
	}
	
	@JvmStatic
	fun isCharEmoji(codePoint: Int) =
			codePoint in 0x1F600..0x1F64F ||
			codePoint in 0x1F300..0x1F5FF ||
			codePoint in 0x1F680..0x1F6FF ||
			codePoint in 0x2600..0x26FF ||
			codePoint in 0x2700..0x27BF ||
			codePoint in 0xE0020..0xE007F ||
			codePoint in 0xFE00..0xFE0F ||
			codePoint in 0x1F900..0x1F9FF ||
			codePoint in 0x1F018..0x1F270 ||
			codePoint in 0x238C..0x2454 ||
			codePoint in 0x20D0..0x20FF
	
	@JvmStatic
	fun isZeroWidthJoiner(codePoint: Int) = codePoint == 8205
}