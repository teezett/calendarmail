package de.heinersyndikat.tools.calendarmail;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.filter.Rule;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
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

/**
 * Main class.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class CalendarMail {

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	/**
	 * Application properties
	 */
	static Properties appProperties = new Properties();
	final static String APP_PROP_RESSOURCE = "/application.properties";
//	private static String username;
//	private static String password;

	/**
	 * Load the application properties.
	 */
	private static void load_properties() {
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
			logger.warn("Unable to load application properties");
		}
	}

	/**
	 * Define the command line options.
	 *
	 * @return the available command line options
	 */
	protected static Options defineOptions() {
		Options options = new Options();
		Option help = new Option("h", "print this message");
		options.addOption(help);
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("configuration file to be read");
		Option readFile = OptionBuilder.create("f");
		options.addOption(readFile);
		return options;
	}

	/**
	 * Parse commandline for given parameters
	 *
	 * @param cmdline commandline with parmeters
	 */
	protected static void start(CommandLine cmdline) {
		if (cmdline.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CalendarMail", defineOptions());
			System.exit(0);
		}
		if (cmdline.hasOption("f")) {
			logger.debug("found option -f");
			String filename = cmdline.getOptionValue("f");
			CalendarMailConfiguration.INSTANCE.setConfigurationFile(filename);
		}
	}

//	protected static void readPassword() {
//		Console console = System.console();
//		if (console == null) {
//			logger.error("Cannot get console");
//			return;
//		}
//		final String default_user = "info@heinersyndikat.de";
//		console.printf("Please enter your username[%s]: ", default_user);
//		username = console.readLine();
//		if (username.equals("")) {
//			username = default_user;
//		}
//		console.printf(username + "\n");
//
//		console.printf("Please enter your password: ");
//		char[] passwordChars = console.readPassword();
//		password = new String(passwordChars);
//
//		console.printf(password + "\n");
//	}

	protected static void loadConfig() {
		try {
			Config conf = ConfigFactory.load();
			Config calendarmail = conf.getConfig("calendarmail");
			List<String> monthly = calendarmail.getStringList("receivers.monthly");
			String monthly_mails = String.join("; ", monthly);
			logger.info("monthly mails: " + monthly_mails);
			List<RemoteCalendar> calendars = calendarmail.getConfigList("calendars").stream()
							.map(c -> ConfigBeanFactory.create(c, RemoteCalendar.class))
							.collect(Collectors.toList());
			String calendar_names = calendars.stream().map(RemoteCalendar::getHostname)
							.collect(Collectors.joining(", "));
			logger.info("Loaded calendars: " + calendar_names);
		} catch (ConfigException ex) {
			logger.warn("Loading configuration: " + ex.getLocalizedMessage());
		}
	}

	private static void connectDav() {
		List<RemoteCalendar> calendars = CalendarMailConfiguration.INSTANCE.getCalendars();
		if (calendars == null) {
			return;
		}
		// define filter
		java.util.Calendar today = java.util.Calendar.getInstance();
		today.set(java.util.Calendar.HOUR_OF_DAY, 0);
		today.clear(java.util.Calendar.MINUTE);
		today.clear(java.util.Calendar.SECOND);
		Period period = new Period(new DateTime(today.getTime()), new Dur(30, 0, 0, 0));
		Rule[] rules = {new PeriodRule(period)};
		Filter filter = new Filter(rules, Filter.MATCH_ALL);
		// get String representation for all calendars
		String all_events = calendars.stream()
						.map(cal -> {
							try {
								return cal.toString(filter);
							} catch (IOException | ParserException ex) {
								logger.warn(ex.getLocalizedMessage());
							}
							return "";
						})
						.filter(txt -> !txt.isEmpty())
						.collect(Collectors.joining("\n"));
		logger.info("All Events:\n" + all_events);
	}

	/**
	 * Main function.
	 *
	 * @param args command line options
	 */
	public static void main(String[] args) {
		// load application properties
		load_properties();
		String version = appProperties.getProperty("application.version", "");
		String app_name = appProperties.getProperty("application.name", "CalendarMail");
		System.out.println(app_name + " " + version + ": Sending reminder for calendar entries via email");
		// parse command line parameters
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine line = parser.parse(defineOptions(), args);
			start(line);
		} catch (ParseException ex) {
			logger.error("Parse Error: " + ex.getLocalizedMessage());
			System.exit(1);
		}
		// load configuration
		CalendarMailConfiguration.INSTANCE.load();
		// WebDAV access
		connectDav();
	}

}
