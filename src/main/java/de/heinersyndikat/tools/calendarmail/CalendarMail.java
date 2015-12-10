package de.heinersyndikat.tools.calendarmail;

import com.typesafe.config.ConfigException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;

/**
 * Main controller class.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public enum CalendarMail {

	INSTANCE;

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	/**
	 * Application properties
	 */
	final static String APP_PROP_RESSOURCE = "/application.properties";
	Properties appProperties = new Properties();

	private static Optional<String> to_encrypt = Optional.empty();
	private static Optional<String> to_decrypt = Optional.empty();

	/**
	 * Load the application properties.
	 */
	private void load_properties() {
		// Get application properties
		InputStream propertiesStream = CalendarMail.class.getResourceAsStream(APP_PROP_RESSOURCE);
		logger.debug("Application property Stream: " + propertiesStream);
		try {
			appProperties.load(propertiesStream);
			appProperties.stringPropertyNames().stream().forEach((key) -> {
				String value = appProperties.getProperty(key, "");
				logger.debug("application property: " + key + " => " + value);
			});
		} catch (IOException ex) {
			logger.warn("Unable to load application properties: " + ex.getLocalizedMessage());
		}
	}

	/**
	 * Define the command line options.
	 *
	 * @return the available command line options
	 */
	protected Options defineOptions() {
		Options options = new Options();
		// -h help
		Option help = new Option("h", "print this message");
		options.addOption(help);
		// -f configuration file
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("configuration file to be read");
		Option opt = OptionBuilder.create("f");
		options.addOption(opt);
		// -s (single execution)
		OptionBuilder.withArgName("single execution");
		OptionBuilder.hasArg(false);
		OptionBuilder.withDescription("perform just one single execution of reminders");
		opt = OptionBuilder.create("s");
		options.addOption(opt);
		// -p encryption password
		OptionBuilder.withArgName("password");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("encryption password");
		opt = OptionBuilder.create("p");
		options.addOption(opt);
		// -e string encryption
		OptionBuilder.withArgName("string");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("string to encrypt");
		opt = OptionBuilder.create("e");
		options.addOption(opt);
		// -u string decryption
		OptionBuilder.withArgName("string");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("string to unencrypt");
		opt = OptionBuilder.create("u");
		options.addOption(opt);
		// return options definition
		return options;
	}

	/**
	 * Parse commandline for given parameters
	 *
	 * @param cmdline commandline with parmeters
	 */
	protected void start(CommandLine cmdline) {
		// help
		if (cmdline.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CalendarMail", defineOptions());
			System.exit(0);
		}
		// load configuration file
		if (cmdline.hasOption("f")) {
			logger.debug("found option -f");
			String filename = cmdline.getOptionValue("f");
			CalendarMailConfiguration.INSTANCE.setConfigurationFile(filename);
		}
		// perform just one single execution
		if (cmdline.hasOption("s")) {
			logger.debug("found option -s");
			CalendarMailConfiguration.INSTANCE.setSingleExecution(true);
		}
		// set encryption password
		if (cmdline.hasOption("p")) {
			logger.debug("found option -p");
			String password = cmdline.getOptionValue("p");
			CalendarMailConfiguration.INSTANCE.setPassword(password);
		}
		if (cmdline.hasOption("e")) {
			logger.debug("found option -e");
			to_encrypt = Optional.of(cmdline.getOptionValue("e"));
		}
		if (cmdline.hasOption("u")) {
			logger.debug("found option -u");
			to_decrypt = Optional.of(cmdline.getOptionValue("u"));
		}
	}

	/**
	 * Initialize application.
	 *
	 * @param args command line options
	 */
	protected void init(String[] args) {
		// load application properties
		load_properties();
		String version = appProperties.getProperty("application.version", "");
		String app_name = appProperties.getProperty("application.name", "CalendarMail");
		System.out.println(app_name + " [" + version + "]: Sending reminder for calendar entries via email");
		// parse command line parameters
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine line = parser.parse(defineOptions(), args);
			start(line);
		} catch (ParseException ex) {
			logger.error("Parse Error: " + ex.getLocalizedMessage());
			System.exit(1);
		}
	}

	/**
	 * Encrypt a given string.
	 *
	 * @param unencrypted string to be encrypted
	 */
	protected static void encrypt_string(String unencrypted) {
		try {
			Encryption encryption = new Encryption();
			String encrypted = encryption.encrypt(unencrypted);
			logger.info("Encryption of '" + unencrypted + "' = 'ENC(" + encrypted + ")'");
			logger.info("Decrypted: " + encryption.decrypt(encrypted));
			System.exit(0);
		} catch (NoSuchElementException | GeneralSecurityException ex) {
			logger.error("Unable to encrypt string: " + ex.getLocalizedMessage());
			System.exit(4);
		}
	}

	/**
	 * Decrypt a given string.
	 *
	 * @param encrypted string to be decrypted
	 */
	protected static void decrypt_string(String encrypted) {
		try {
			Encryption encryption = new Encryption();
			String decrypted = encryption.decrypt(encrypted);
			logger.info("Decryption of string '" + encrypted + "' = '" + decrypted + "'");
			System.exit(0);
		} catch (NoSuchElementException | GeneralSecurityException ex) {
			logger.error("Unable to decrypt string: " + ex.getLocalizedMessage());
			System.exit(4);
		}
	}

	/**
	 * Load configuration from file.
	 */
	protected void loadConfig() {
		try {
			CalendarMailConfiguration.INSTANCE.load();
		} catch (MailExceptionWrapper ex) {
			logger.error("Encryption failed!");
			Throwable internal = ex.getCause();
			logger.error(internal.getClass().getSimpleName() + ": " + internal.getLocalizedMessage());
			System.exit(5);
		} catch (ConfigException ex) {
			Throwable internal = ex.getCause();
			if (internal != null) {
				logger.error("Configuration failed: " + internal.getLocalizedMessage());
			} else {
				logger.error("Configuration failed: " + ex.getLocalizedMessage());
			}
			System.exit(2);
		}
	}

	/**
	 * Start scheduling of reminders.
	 */
	protected void scheduleReminders() {
		// initialize scheduler
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		try {
			Scheduler sched = schedFact.getScheduler();
			Map<String, Reminder> reminders = CalendarMailConfiguration.INSTANCE.getReminders();
			reminders.values().stream().forEach(rem -> {
				JobDetail job = rem.createJob();
				try {
					Trigger trigger = rem.createTrigger();
					sched.scheduleJob(job, trigger);
				} catch (java.text.ParseException ex) {
					logger.warn("Cannot parse cron trigger: " + ex.getLocalizedMessage());
				} catch (SchedulerException ex) {
					logger.warn("Scheduling failed: " + ex.getLocalizedMessage());
				}
			});
			sched.start();
			Optional cron_execution = reminders.values().stream()
							.filter(Reminder::isCron_triggered).findAny();
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
	 * Main function.
	 *
	 * @param args command line options
	 */
	public static void main(String[] args) {
		INSTANCE.init(args);

		to_encrypt.ifPresent(CalendarMail::encrypt_string);
		to_decrypt.ifPresent(CalendarMail::decrypt_string);

		INSTANCE.loadConfig();

		INSTANCE.scheduleReminders();
	}

}
