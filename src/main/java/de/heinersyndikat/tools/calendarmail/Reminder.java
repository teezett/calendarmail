package de.heinersyndikat.tools.calendarmail;

import java.util.Calendar;
import java.util.List;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.filter.Rule;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reminder for special purpose and its own email receivers list.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class Reminder {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	private String name;
	private int days_in_advance;
	private List<String> receivers;

	/**
	 * Get the defined filter for this reminder.
	 * 
	 * @return 
	 */
	public Filter getFilter() {
		// get actual timestamp (beginning of day)
		Calendar today = Calendar.getInstance();
		today.set(java.util.Calendar.HOUR_OF_DAY, 0);
		today.clear(java.util.Calendar.MINUTE);
		today.clear(java.util.Calendar.SECOND);
		// define the time period
		Period period = new Period(new DateTime(today.getTime()), new Dur(days_in_advance, 0, 0, 0));
		// create filter for time period
		Rule[] rules = {new PeriodRule(period)};
		Filter filter = new Filter(rules, Filter.MATCH_ALL);
		return filter;
	}
	
	/**
	 * Send the emails for all calendars to given receivers.
	 */
	public void sendEmail() {
		List<RemoteCalendar> calendars = CalendarMailConfiguration.INSTANCE.getCalendars();
		String all_events = RemoteCalendar.filterAll(calendars, getFilter());
		if (all_events.isEmpty()) {
			logger.info("No events for reminder [" + getName() + "] - Skip sending");
			return;
		}
		logger.info("Events[" + getName() + "]:\n" + all_events);
		// @todo: Implement sending
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the days_in_advance
	 */
	public int getDays_in_advance() {
		return days_in_advance;
	}

	/**
	 * @param days_in_advance the days_in_advance to set
	 */
	public void setDays_in_advance(int days_in_advance) {
		this.days_in_advance = days_in_advance;
	}

	/**
	 * @return the receivers
	 */
	public List<String> getReceivers() {
		return receivers;
	}

	/**
	 * @param receivers the receivers to set
	 */
	public void setReceivers(List<String> receivers) {
		this.receivers = receivers;
	}
	
}