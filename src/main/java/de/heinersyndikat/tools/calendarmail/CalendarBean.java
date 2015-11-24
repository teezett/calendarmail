package de.heinersyndikat.tools.calendarmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean class for a CalDAV calender.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class CalendarBean {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	private String hostname;
	private String address;

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @param hostname the hostname to set
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	
}
