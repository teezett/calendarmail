package de.heinersyndikat.tools.calendarmail;

import java.util.Properties;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email server configuration.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class EmailServer {

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	public static final String CONFIG_KEYWORD = "emailserver";

	private Session session = null;

	private String hostname;
	private int smtp_port;
	private String username;
	private String password;
	private boolean ssl_connect;
	private String from;

	protected Session getSession() {
		if (session == null) {
			Properties properties = new Properties();
			properties.put("mail.transport.protocol", "smtp");
			properties.setProperty("mail.smtp.host", getHostname());
			properties.put("mail.smtp.port", getSmtp_port());
			properties.put("mail.smtp.auth", "true");
			Authenticator auth = new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(getUsername(), getPassword());
				}
			};
			session = Session.getDefaultInstance(properties, auth);
		}
		return session;
	}

	/**
	 * Creates a new message with predefined headers.
	 * 
	 * @return created Message
	 * @throws MessagingException 
	 */
	public Message createMessage() throws MessagingException {
		MimeMessage message = new MimeMessage(getSession());
		Address fromAddr = new InternetAddress(getFrom(), true);
		logger.debug("From set to: " + fromAddr.toString());
		message.setFrom(fromAddr);
		message.addRecipient(Message.RecipientType.TO, fromAddr);
		return message;
	}

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
		this.session = null;
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
		this.session = null;
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
		this.session = null;
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
		this.session = null;
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
		this.session = null;
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
		this.session = null;
		this.from = from;
	}

}
