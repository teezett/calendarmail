package de.heinersyndikat.tools.calendarmail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class CalendarMail {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	/** Application properties */
	static Properties appProperties = new Properties();
	final static String APP_PROP_RESSOURCE = "/application.properties";

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
	 * Main function.
	 * 
	 * @param args command line options
	 */
	public static void main(String[] args) {
		load_properties();
		String version = appProperties.getProperty("application.version", "");
		String app_name = appProperties.getProperty("application.name", "CalendarMail");
		logger.info(app_name + " " + version + ": Sending reminder for calendar entries via email");
	}
}
