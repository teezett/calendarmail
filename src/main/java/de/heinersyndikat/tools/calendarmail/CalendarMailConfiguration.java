package de.heinersyndikat.tools.calendarmail;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration singleton.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public enum CalendarMailConfiguration {

	INSTANCE;

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	/**
	 * HOCON configuration keyword
	 */
	private static final String CONFIG_KEYWORD = "calendarmail";
	/**
	 * configuration
	 */
	private Config config;
	/**
	 * configuration filename from commandline option
	 */
	private String configurationFile;
	/**
	 * configuration encryption password from commandline option
	 */
	private Optional<String> password = Optional.empty();

	/**
	 * Collection of remote calendars
	 */
	private List<RemoteCalendar> calendars;
	/**
	 * Collection of reminders
	 */
	private List<Reminder> reminders;
	/**
	 * email server
	 */
	private EmailServer emailserver;

	/**
	 * @param confFile the confFile to set
	 */
	public void setConfigurationFile(String confFile) {
		this.configurationFile = confFile;
	}

	public void load() throws ConfigException {
		// load configuration files
		if (configurationFile != null) {
			Config fileConf = ConfigFactory.parseFile(new File(configurationFile));
			config = fileConf.getConfig(CONFIG_KEYWORD);
		} else {
			Config defaultConf = ConfigFactory.load();
			config = defaultConf.getConfig(CONFIG_KEYWORD);
		}
		// parse calendar configuration
		try {
			calendars = config.getConfigList(RemoteCalendar.CONFIG_KEYWORD).stream()
							.map(c -> ConfigBeanFactory.create(c, RemoteCalendar.class))
							.collect(Collectors.toList());
			String calendar_names = getCalendars().stream().map(RemoteCalendar::getHostname)
							.collect(Collectors.joining(", "));
			logger.debug("Loaded configuration for the calendars: " + calendar_names);
			// parse reminder configuration
			reminders = config.getConfigList(Reminder.CONFIG_KEYWORD).stream()
							.map(c -> ConfigBeanFactory.create(c, Reminder.class))
							.collect(Collectors.toList());
			String reminder_names = getReminders().stream().map(Reminder::getName)
							.collect(Collectors.joining(", "));
			logger.debug("Loaded configuration for the reminders: " + reminder_names);
			// parse configuration for email server
			emailserver = ConfigBeanFactory
							.create(config.getConfig(EmailServer.CONFIG_KEYWORD), EmailServer.class);
			logger.debug("Read email server configuration for address '"
							+ getEmailserver().getFrom()
							+ "' on server " + getEmailserver().getHostname());
		} catch (ConfigException ex) {
			Throwable internal = ex.getCause();
			// unpack exceptions from reflection packed in InvocationTargetException
			if ((internal != null) && (internal.getClass() == InvocationTargetException.class)) {
				InvocationTargetException invoc = (InvocationTargetException) internal;
				throw new ConfigException.Generic(ex.getMessage(), invoc.getTargetException());
			} else {
				throw ex;
			}
		}
	}

	/**
	 * @return the calendars
	 */
	public List<RemoteCalendar> getCalendars() {
		return calendars;
	}

	/**
	 * @return the reminders
	 */
	public List<Reminder> getReminders() {
		return reminders;
	}

	/**
	 * @return the emailserver
	 */
	public EmailServer getEmailserver() {
		return emailserver;
	}

	/**
	 * @return the password
	 */
	public String getPassword() throws NoSuchElementException {
		try {
			return password.get();
		} catch (NoSuchElementException ex) {
			throw new NoSuchElementException("No encryption password provided");
		}
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = Optional.of(password);
	}

}
