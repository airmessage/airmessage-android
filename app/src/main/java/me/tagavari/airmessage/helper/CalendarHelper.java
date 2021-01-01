package me.tagavari.airmessage.helper;

import java.util.Calendar;

public class CalendarHelper {
	/**
	 * Compares two calendars by their date (ignores time)
	 * @param cal1 The first calendar to compare
	 * @param cal2 The second calendar to compare
	 * @return the value {@code 0} if {@code cal1 == cal2};
	 *         a value less than {@code 0} if {@code cal1 < cal2}; and
	 *         a value greater than {@code 0} if {@code cal1 > cal2}
	 */
	public static int compareCalendarDates(Calendar cal1, Calendar cal2) {
		if(cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) return -1;
		if(cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) return 1;
		if(cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return -1;
		if(cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) return 1;
		return Integer.compare(cal1.get(Calendar.DAY_OF_YEAR), cal2.get(Calendar.DAY_OF_YEAR));
	}
}