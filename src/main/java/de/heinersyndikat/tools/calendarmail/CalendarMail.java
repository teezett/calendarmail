package de.heinersyndikat.tools.calendarmail;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.filter.Rule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
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
	private static String username;
	private static String password;
	
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

	protected static void readPassword() {
		Console console = System.console();
		if (console == null) {
			logger.error("Cannot get console");
			return;
		}
		final String default_user = "info@heinersyndikat.de";
		console.printf("Please enter your username[%s]: ", default_user);
		username = console.readLine();
		if (username.equals("")) {
			username = default_user;
		}
		console.printf(username + "\n");

		console.printf("Please enter your password: ");
		char[] passwordChars = console.readPassword();
		password = new String(passwordChars);

		console.printf(password + "\n");
	}

	protected static void loadConfig() {
		try {
			Config conf = ConfigFactory.load();
			Config calendarmail = conf.getConfig("calendarmail");
			List<String> monthly = calendarmail.getStringList("receivers.monthly");
			String monthly_mails = String.join("; ", monthly);
			logger.info("monthly mails: " + monthly_mails);
			List<CalendarBean> calendars = calendarmail.getConfigList("calendars").stream()
							.map(c -> ConfigBeanFactory.create(c, CalendarBean.class))
							.collect(Collectors.toList());
			String calendar_names = calendars.stream().map(CalendarBean::getHostname)
							.collect(Collectors.joining(", "));
			logger.info("Loaded calendars: " + calendar_names);
		} catch (ConfigException ex) {
			logger.warn("Loading configuration: " + ex.getLocalizedMessage());
		}
	}

	private static void connectDav() {
		List<CalendarBean> calendars = CalendarMailConfiguration.INSTANCE.getCalendars();
		if (calendars == null) {
			return;
		}
		calendars.stream().forEach((cal) -> {
			SardineDAVAccess website = new SardineDAVAccess(cal.getHostname(), cal.getUsername(), cal.getPassword());
			Calendar iCal = new Calendar();
			InputStream is = website.read(cal.getAddress());
			if (is != null) {
				CalendarBuilder builder = new CalendarBuilder();
				try {
					iCal = builder.build(is);
					manageICal(iCal);
				} catch (IOException ex) {
					logger.error("I/O Error: " + ex.getLocalizedMessage());
				} catch (ParserException ex) {
					logger.error("Parse Error: " + ex.getLocalizedMessage());
				}
			}
//			website.list(cal.getAddress());
		});
	}

	public static void manageICal(Calendar iCal) {
		ComponentList components = iCal.getComponents(Component.VEVENT);
		logger.info("Found " + components.size() + " events in calendar");
		components.stream().forEach((comp) -> {
			VEvent event = (VEvent) comp;
			logger.info("Found event with summary " + event.getProperty(Property.SUMMARY).getValue());
		});
		java.util.Calendar today = java.util.Calendar.getInstance();
		today.set(java.util.Calendar.HOUR_OF_DAY, 0);
		today.clear(java.util.Calendar.MINUTE);
		today.clear(java.util.Calendar.SECOND);
		Period period = new Period(new DateTime(today.getTime()), new Dur(3, 0, 0, 0));
		Rule[] rules = { new PeriodRule(period) };
		Filter filter = new Filter(rules, Filter.MATCH_ALL);
		Collection events = filter.filter(iCal.getComponents(Component.VEVENT));
		logger.info("Found " + events.size() + " matching events in calendar");
		events.stream().forEach((ev) -> {
			logger.debug("Found class type " + ev.getClass());
			VEvent event = (VEvent) ev;
			logger.info("Found event with summary " + event.getProperty(Property.SUMMARY).getValue());
		});
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
