package de.heinersyndikat.tools.calendarmail;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.util.List;
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
	
	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	private Config config;
	private String configurationFile;
	private static final String TOP_KEY = "calendarmail";
	private List<CalendarBean> calendars;
	
	/**
	 * @param confFile the confFile to set
	 */
	public void setConfigurationFile(String confFile) {
		this.configurationFile = confFile;
	}
	
	public void load() {
		try {
			if (configurationFile != null ) {
				Config fileConf = ConfigFactory.parseFile(new File(configurationFile));
				config = fileConf.getConfig(TOP_KEY);
			} else {
				Config defaultConf = ConfigFactory.load();
				config = defaultConf.getConfig(TOP_KEY);
			}
			calendars = config.getConfigList("calendars").stream()
							.map(c -> ConfigBeanFactory.create(c, CalendarBean.class))
							.collect(Collectors.toList());
			String calendar_names = getCalendars().stream().map(CalendarBean::getHostname)
							.collect(Collectors.joining(", "));
			logger.info("Loaded calendars: " + calendar_names);
		} catch (ConfigException ex) {
			logger.warn("Configuration: " + ex.getLocalizedMessage());
		}
	}

	/**
	 * @return the calendars
	 */
	public List<CalendarBean> getCalendars() {
		return calendars;
	}
	
}
