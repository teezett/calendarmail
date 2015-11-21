package de.heinersyndikat.tools.calendarmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class CalendarMail {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	public static void main(String[] args) {
		logger.info("CalendarMail: Sending reminder for calendar entries via email");
	}
}
