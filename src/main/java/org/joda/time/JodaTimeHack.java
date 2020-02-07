package org.joda.time;

public class JodaTimeHack {

	public static long getLocalMillis(LocalDate date) {
		return date.getLocalMillis();
	}

	public static long getLocalMillis(LocalTime time) {
		return time.getLocalMillis();
	}

	public static long getLocalMillis(LocalDateTime dateTime) {
		return dateTime.getLocalMillis();
	}
}
