package de.heinersyndikat.tools.calendarmail;

import java.util.NoSuchElementException;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Password encryption in configuration files.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class Encryption {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	PBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

	String encrypt(String string_to_enc) throws NoSuchElementException {
		String password = CalendarMailConfiguration.INSTANCE.getPassword();
		encryptor.setPassword(password);
		return encryptor.encrypt(string_to_enc);
	}
	
}
