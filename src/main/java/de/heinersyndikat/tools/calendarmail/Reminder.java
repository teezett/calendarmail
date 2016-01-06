package de.heinersyndikat.tools.calendarmail;

import java.text.MessageFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
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
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
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

	private boolean cron_triggered = false;

	private String name;
	private String cron_trigger;
	private int days_in_advance;
	private List<String> receivers;
//	private List<Address> addresses;

	/**
	 * Create a new Quartz job.
	 *
	 * @return job details
	 */
	public JobDetail createJob() {
		JobDetail job = JobBuilder.newJob(ReminderJob.class)
						.withIdentity(this.getName())
						.storeDurably(false)
						.usingJobData(ReminderJob.KEY, this.getName())
						.build();
		return job;
	}

	/**
	 * Create trigger as configured.
	 *
	 * @return created trigger
	 * @throws java.text.ParseException
	 */
	public Trigger createTrigger() throws ParseException {
		TriggerBuilder builder = TriggerBuilder.newTrigger()
						.withIdentity(this.getName());
		if (CalendarMailConfiguration.INSTANCE.isSingleExecution() || getCron_trigger().isEmpty()) {
			builder = builder.startNow();
		} else {
			builder = builder.withSchedule(CronScheduleBuilder.cronSchedule(getCron_trigger()));
			cron_triggered = true;
		}
		return builder.build();
	}

	/**
	 * Get the defined filter for this reminder.
	 *
	 * @return  created filter
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
		ResourceBundle message_bundle = CalendarMailConfiguration.INSTANCE.getMessages();
		logger.debug("Events[" + getName() + "]:\n" + all_events);
		MessageFormat bodyFormat = new MessageFormat(message_bundle.getString("email.body.intro"));
		String calendarNames = calendars.stream()
						.map(RemoteCalendar::getHostname)
						.collect(Collectors.joining(", "));
		Object[] params = {getName(), getDays_in_advance(), calendarNames};
		String intro = bodyFormat.format(params);
		StringBuilder builder = new StringBuilder();
		builder.append(intro);
		builder.append(all_events);
		builder.append(message_bundle.getString("email.body.greeting"));
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
			// Subject
			ResourceBundle message_bundle = CalendarMailConfiguration.INSTANCE.getMessages();
			MessageFormat subjectFormat = new MessageFormat(message_bundle.getString("email.subject"));
			Object[] params = {getName(), new Date()};
			String subject = subjectFormat.format(params);
			message.setSubject(subject);
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

	/**
	 * @return the cron_trigger
	 */
	public String getCron_trigger() {
		return cron_trigger;
	}

	/**
	 * @param cron_trigger the cron_trigger to set
	 */
	public void setCron_trigger(String cron_trigger) {
		this.cron_trigger = cron_trigger;
	}

	/**
	 * @return the cron_triggered
	 */
	public boolean isCron_triggered() {
		return cron_triggered;
	}

}
