package me.tagavari.airmessage.common.helper

import java.util.*

object CalendarHelper {
	/**
	 * Compares two calendars by their date (ignores time)
	 * @param cal1 The first calendar to compare
	 * @param cal2 The second calendar to compare
	 * @return the value `0` if `cal1 == cal2`;
	 * a value less than `0` if `cal1 < cal2`; and
	 * a value greater than `0` if `cal1 > cal2`
	 */
	@JvmStatic
	fun compareCalendarDates(cal1: Calendar, cal2: Calendar): Int {
		return when {
			cal1[Calendar.ERA] < cal2[Calendar.ERA] -> -1
			cal1[Calendar.ERA] > cal2[Calendar.ERA] -> 1
			cal1[Calendar.YEAR] < cal2[Calendar.YEAR] -> -1
			cal1[Calendar.YEAR] > cal2[Calendar.YEAR] -> 1
			else -> cal1[Calendar.DAY_OF_YEAR].compareTo(cal2[Calendar.DAY_OF_YEAR])
		}
	}
}