package de.heinersyndikat.tools.calendarmail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
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

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	public static final String CONFIG_KEYWORD = "reminders";

	private String name;
	private int days_in_advance;
	private List<String> receivers;
//	private List<Address> addresses;

	/**
	 * Get the defined filter for this reminder.
	 *
	 * @return
	 */
	protected Filter getFilter() {
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
	 * Create the email message body.
	 *
	 * @return string containing message body
	 */
	protected String createBody() {
		List<RemoteCalendar> calendars = CalendarMailConfiguration.INSTANCE.getCalendars();
		String all_events = RemoteCalendar.filterAll(calendars, getFilter());
		if (all_events.isEmpty()) {
			logger.info("No events for reminder [" + getName() + "] - Skip sending");
			return "";
		}
		logger.debug("Events[" + getName() + "]:\n" + all_events);
		StringBuilder builder = new StringBuilder();
		builder.append("Summary for Calender Reminder [").append(getName()).append("]:\n\n");
		builder.append(all_events);
		builder.append("\nGreetings\n");
		return builder.toString();
	}

	/**
	 * Send the emails for all calendars to given receivers.
	 */
	public void sendEmail() {
		try {
			String body = createBody();
			if (body.isEmpty()) {
				return;
			}
			// configure email content
			EmailServer emailServer = CalendarMailConfiguration.INSTANCE.getEmailserver();
			Message message = emailServer.createMessage();
			LocalDate today = LocalDate.now();
			message.setSubject("Calendar Reminder [" + getName() + "] at "
							+ today.format(DATE_FORMAT) );
			message.setText(createBody());
			receivers.stream().forEach(rec -> {
				try {
					Address addr = new InternetAddress(rec, true);
					message.addRecipient(Message.RecipientType.BCC, addr);
				} catch (MessagingException ex) {
					logger.warn("Invalid receiver address '" + rec + "': " + ex.getLocalizedMessage());
				}
			});
			// send email
			Transport.send(message);
			logger.info("Email for reminder [" + getName() + "] sent");
		} catch (MessagingException ex) {
			throw new MailExceptionWrapper(ex);
		}
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
//		addresses.clear();
//		receivers.stream().forEach((rec) -> {
//			try {
//				Address addr = new InternetAddress(rec, true);
//				addresses.add(addr);
//			} catch (AddressException ex) {
//				logger.warn("Invalid receiver address '" + rec + "': " + ex.getLocalizedMessage());
//			}
//		});
	}

}
