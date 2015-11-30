package de.heinersyndikat.tools.calendarmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email server configuration.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class EmailServer {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	public static final String CONFIG_KEYWORD = "emailserver";
	
	private String hostname;
	private int smtp_port;
	private String username;
	private String password;
	private boolean ssl_connect;
	private String from;

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
	 * @return the smtp_port
	 */
	public int getSmtp_port() {
		return smtp_port;
	}

	/**
	 * @param smtp_port the smtp_port to set
	 */
	public void setSmtp_port(int smtp_port) {
		this.smtp_port = smtp_port;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the ssl_connect
	 */
	public boolean isSsl_connect() {
		return ssl_connect;
	}

	/**
	 * @param ssl_connect the ssl_connect to set
	 */
	public void setSsl_connect(boolean ssl_connect) {
		this.ssl_connect = ssl_connect;
	}

	/**
	 * @return the from
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * @param from the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}
	
}
