package me.tagavari.airmessage.helper

object VersionHelper {
	/* Compares 2 version lists and returns which one is larger
	-1: version 1 is smaller
	 0: versions are equal
	 1: version 1 is greater
	 */
	fun compareVersions(version1: List<Int>, version2: List<Int>): Int {
		//Iterate over the arrays
		for(i in 0 until version1.size.coerceAtLeast(version2.size)) {
			//Compare the version values
			val code1 = if(i >= version1.size) 0 else version1[i]
			val code2 = if(i >= version2.size) 0 else version2[i]
			val comparison = code1.compareTo(code2)
			
			//Return if the value is not 0
			if(comparison != 0) return comparison
		}
		
		//Return 0 (the loop finished, meaning that there was no difference)
		return 0
	}
}