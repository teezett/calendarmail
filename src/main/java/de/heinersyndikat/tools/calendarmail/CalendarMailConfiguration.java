package de.heinersyndikat.tools.calendarmail;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Scanner;
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
	 * Application properties
	 */
	final static String APP_PROP_RESSOURCE = "/app.properties";
	private Properties appProperties = null;
	/**
	 * HOCON configuration keyword
	 */
	private static final String CONFIG_KEYWORD = "calendarmail";
	/**
	 * Internationalization bundle
	 */
	private final ResourceBundle messages = ResourceBundle.getBundle("MessageBundle");
	/**
	 * typesafe HOCON configuration
	 */
	private Config config;
	/**
	 * configuration filename from commandline option
	 */
	private String configurationFile;
	/**
	 * flag, if reminders should be executed only once
	 */
	private boolean singleExecution = false;
	/**
	 * name of reminder to be executed once
	 */
	private Optional<String> reminderName = Optional.empty();
	
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
	private Map<String, Reminder> reminders;
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

	/**
	 * Load the application properties.
	 */
	private void load_properties() {
		// Get application properties
		InputStream propertiesStream = CalendarMail.class.getResourceAsStream(APP_PROP_RESSOURCE);
		logger.debug("Application property Stream: " + propertiesStream);
		appProperties = new Properties();
		try {
			getAppProperties().load(propertiesStream);
			getAppProperties().stringPropertyNames().stream().forEach((key) -> {
				String value = getAppProperties().getProperty(key, "");
				logger.debug("application property: " + key + " => " + value);
			});
		} catch (IOException ex) {
			logger.warn("Unable to load application properties: " + ex.getLocalizedMessage());
		}
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
							.collect(Collectors.toMap(Reminder::getName, rem->rem));
			String reminder_names = getReminders().keySet().stream()
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
	public Map<String, Reminder> getReminders() {
		return reminders;
	}

	/**
	 * @return the emailserver
	 */
	public EmailServer getEmailserver() {
		return emailserver;
	}

	/**
	 * Prompt input of password.
	 * @return entered password
	 */
	protected String prompt_for_password() {
		final String PROMPT_TEXT = "No encryption password provided - please enter it now: ";
		Console console = System.console();
		if (console == null) {
			logger.warn("Cannot get console");
			Scanner in = new Scanner(System.in);
			System.out.print(PROMPT_TEXT);
			return in.nextLine();
		}
		char[] input = console.readPassword(PROMPT_TEXT);
		return new String(input);
	}
	
	/**
	 * @return the password
	 */
	public String getPassword() throws NoSuchElementException {
		if (!password.isPresent()) {
			logger.warn("No password given - prompt for input");
			setPassword(prompt_for_password());
		}
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

	/**
	 * @return the singleExecution
	 */
	public boolean isSingleExecution() {
		return singleExecution;
	}

	/**
	 * @param singleExecution the singleExecution to set
	 */
	public void setSingleExecution(boolean singleExecution) {
		this.singleExecution = singleExecution;
	}

	/**
	 * @return the messages
	 */
	public ResourceBundle getMessages() {
		return messages;
	}

	/**
	 * @return the appProperties
	 */
	public Properties getAppProperties() {
		if (appProperties == null) {
			load_properties();
		}
		return appProperties;
	}

	/**
	 * @return the reminderName
	 */
	public Optional<String> getReminderName() {
		return reminderName;
	}

	/**
	 * @param reminderName the reminderName to set
	 */
	public void setReminderName(String reminderName) {
		this.reminderName = Optional.of(reminderName);
	}

}
