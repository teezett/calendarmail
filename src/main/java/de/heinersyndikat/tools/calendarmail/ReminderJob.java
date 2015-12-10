package de.heinersyndikat.tools.calendarmail;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execution of a Reminder.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class ReminderJob implements Job {

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	/**
	 * key name to identify the reminder from the collection of reminders.
	 */
	public static final String KEY = "reminder_name";

	/**
	 * Execution implementation.
	 *
	 * @param jec execution context
	 * @throws JobExecutionException
	 */
	@Override
	public void execute(JobExecutionContext jec) throws JobExecutionException {
		// get the reminder implementation
		JobDataMap dataMap = jec.getJobDetail().getJobDataMap();
		String name = dataMap.getString(KEY);
		logger.info("Reminder to execute: " + name);
		Reminder reminder = CalendarMailConfiguration.INSTANCE.getReminders().get(name);
		// perform action
		try {
			reminder.sendEmail();
		} catch (MailExceptionWrapper ex) {
			Throwable internal = ex.getCause();
			logger.error(internal.getClass().getSimpleName() + ": " + internal.getLocalizedMessage());
		}
	}
}
