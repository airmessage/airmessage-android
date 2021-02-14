package me.tagavari.airmessage;

import org.junit.Test;

import java.util.Calendar;

import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.CalendarHelper;

import static com.google.common.truth.Truth.assertThat;

public class CalendarTest {
	@Test
	public void testCalendarCompare() {
		Calendar calendarNow = Calendar.getInstance();
		
		Calendar calendar1 = Calendar.getInstance();
		calendar1.set(Calendar.HOUR_OF_DAY, 1);
		
		Calendar calendar23 = Calendar.getInstance();
		calendar23.set(Calendar.HOUR_OF_DAY, 23);
		
		Calendar calendarTomorrow = Calendar.getInstance();
		calendarTomorrow.roll(Calendar.DATE, 1);
		
		assertThat(CalendarHelper.compareCalendarDates(calendarNow, calendarNow)).isEqualTo(0);
		assertThat(CalendarHelper.compareCalendarDates(calendar1, calendar23)).isEqualTo(0);
		assertThat(CalendarHelper.compareCalendarDates(calendar23, calendarTomorrow)).isLessThan(0);
	}
}