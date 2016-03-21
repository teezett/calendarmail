package de.heinersyndikat.tools.calendarmail;

import java.util.Map;
import java.util.Optional;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
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
	 * Factory for scheduler
	 */
	private static final SchedulerFactory schedFact = new StdSchedulerFactory();
	/**
	 * Scheduler instance
	 */
	protected static Scheduler sched;

	/**
	 * Start scheduling of reminders.
	 */
	public static void scheduleReminders() {
		// initialize scheduler
		try {
			sched = schedFact.getScheduler();
			// create job and trigger for each Reminder
			CalendarMailConfiguration config = CalendarMailConfiguration.INSTANCE;
			Map<String, Reminder> reminders = config.getReminders();
			Optional<Reminder> cron_execution = Optional.empty();
			if (config.getReminderName().isPresent()) {
				String reminder_name = config.getReminderName().get();
				Optional<Reminder> reminder = reminders.values().stream()
								.filter(f -> f.getName().equals(reminder_name)).findAny();
				if (reminder.isPresent()) {
					config.setSingleExecution(true);
					scheduleReminder(sched, reminder.get());
				} else {
					logger.error("Reminder with given name '" + reminder_name + "' not configured");
					return;
				}
			} else {
				reminders.values().stream()
								.forEach(rem -> scheduleReminder(sched, rem));
				cron_execution = reminders.values().stream()
								.filter(Reminder::isCron_triggered).findAny();
			}
			// start scheduler
			sched.start();
			// if no cron triggers used, shutdown scheduler after first executions
			if (!cron_execution.isPresent()) {
				try {
					Thread.sleep(1000);
				} catch (Exception ex) {
				}
				logger.info("No Cron triggers found -> exiting");
				sched.shutdown(true);
			}
		} catch (SchedulerException ex) {
			logger.error("Scheduling failed: " + ex.getLocalizedMessage());
			System.exit(6);
		}
	}

	/**
	 * Schedule a single reminder.
	 * 
	 * @param sched reference to scheduler
	 * @param rem reminder to be scheduled
	 */
	private static void scheduleReminder(Scheduler sched, Reminder rem) {
		JobDetail job = rem.createJob();
		logger.debug("Created scheduler job for reminder [" + rem.getName() + "]");
		try {
			Trigger trigger = rem.createTrigger();
			sched.scheduleJob(job, trigger);
			if (rem.isCron_triggered()) {
				logger.info("Reminder [" + rem.getName() + "] scheduled with cron trigger '"
								+ rem.getCron_trigger() + "'");
			} else {
				logger.info("Reminder [" + rem.getName() + "] scheduled for one single execution");
			}
		} catch (java.text.ParseException ex) {
			logger.warn("Cannot parse cron trigger: " + ex.getLocalizedMessage());
		} catch (SchedulerException ex) {
			logger.warn("Scheduling failed: " + ex.getLocalizedMessage());
		}
	}

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
