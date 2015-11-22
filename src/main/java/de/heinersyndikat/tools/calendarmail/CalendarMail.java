package de.heinersyndikat.tools.calendarmail;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
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
		Option readFile = OptionBuilder.withArgName("file")
						.hasArg()
						.withDescription("configuration file to be read")
						.create("f");
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
			// @todo: implement
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

	private static void connectDav() {
//		WebDAVAccess colabori = new WebDAVAccess("colabori.de", "info@heinersyndikat.de", "*****");
//		colabori.getProperties("https://colabori.de/remote.php/caldav/calendars/info%40heinersyndikat.de/defaultcalendar1_shared_by_heinersyndikat@Sven.bauhan.name");
//		colabori.listResources("https://colabori.de/remote.php/caldav/calendars/info%40heinersyndikat.de/defaultcalendar1_shared_by_heinersyndikat@Sven.bauhan.name");
//		JackrabbitDAVAccess website = new JackrabbitDAVAccess("www.heinersyndikat.de", null, null);
		String websiteCal = "http://www.heinersyndikat.de/?plugin=all-in-one-event-calendar&controller=ai1ec_exporter_controller&action=export_events&cb=1544323684";
//		website.getProperties(websiteCal);
//		website.listResources(websiteCal);
//		String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
//		logger.info("CWD: " + cwd);
//		website.copy(websiteCal, "cal.ics", true);
		SardineDAVAccess website2 = new SardineDAVAccess("www.heinersyndikat.de", null, null);
		website2.read(websiteCal);
		website2.list(websiteCal);
		final String cloudCal = "https://colabori.de/remote.php/caldav/calendars/info%40heinersyndikat.de/defaultcalendar1_shared_by_heinersyndikat@Sven.bauhan.name";
		SardineDAVAccess cloud = new SardineDAVAccess("colabori.de", username, password);
		cloud.list(cloudCal);
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
		logger.info(app_name + " " + version + ": Sending reminder for calendar entries via email");
		// parse command line parameters
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine line = parser.parse(defineOptions(), args);
			start(line);
		} catch (ParseException ex) {
			logger.error("Parse Error: " + ex.getLocalizedMessage());
			System.exit(1);
		}
		// read credentials
		readPassword();
		// WebDAV access
		connectDav();
	}

}
