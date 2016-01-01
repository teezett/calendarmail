package de.heinersyndikat.tools.calendarmail;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Password encryption in configuration files.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class Encryption {

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	private static final Pattern ENCRYPTED = Pattern.compile("^ENC\\((.+)\\)$");
	private final static String DEFAULT_SALT = "12345678";
	private static final String ALGORITHM_NAME = "PBEWithMD5AndDES";
	private final PBEParameterSpec pbeParamSpec;
	private final Cipher cipher;

	/**
	 * Standard constructor.
	 *
	 * Initializes the encryptor.
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.spec.InvalidKeySpecException
	 * @throws javax.crypto.NoSuchPaddingException
	 */
	public Encryption() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException {
		pbeParamSpec = new PBEParameterSpec(DEFAULT_SALT.getBytes(), 20);
		// Create PBE Cipher
		cipher = Cipher.getInstance(ALGORITHM_NAME);
	}

	/**
	 * Encrypt a given string.
	 *
	 * @param unencrypted string to be encrypted
	 */
	public static void encrypt_string(String unencrypted) {
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
	public static void decrypt_string(String encrypted) {
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
	 * Create key from password.
	 * 
	 * @return created key
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException 
	 */
	protected SecretKey createKey() throws NoSuchElementException, NoSuchAlgorithmException, InvalidKeySpecException {
		String password = CalendarMailConfiguration.INSTANCE.getPassword();
		// Convert password to SecretKey object
		SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
		return keyFac.generateSecret(pbeKeySpec);
	}
	
	/**
	 * Encrypt a given string.
	 * 
	 * @param string_to_enc string to encrypt
	 * @return  encrypted and Base64 encoded string
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException 
	 */
	protected String encrypt(String string_to_enc) throws GeneralSecurityException {
		// initialize cipher
		cipher.init(Cipher.ENCRYPT_MODE, createKey(), pbeParamSpec);
		// encrypt string
		byte[] encrypted = cipher.doFinal(string_to_enc.getBytes());
		// Base64 encoding
		String encoded = Base64.getEncoder().encodeToString(encrypted);
		return encoded;
	}

	/**
	 * Decrypt a string with the given password.
	 * 
	 * @param encoded  string to be decoded
	 * @return  decoded string
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException 
	 */
	protected String decrypt(String encoded) throws NoSuchElementException, GeneralSecurityException {
		// Base64 decoding
		byte[] encrypted = Base64.getDecoder().decode(encoded);
		// initialize cipher
		cipher.init(Cipher.DECRYPT_MODE, createKey(), pbeParamSpec);
		// decrypt
		byte[] decrypted = cipher.doFinal(encrypted);
		return new String(decrypted);
	}

	/**
	 * Check, if given string is encrypted.
	 * 
	 * @param given  string to be checked
	 * @return decrypted string
	 * @throws GeneralSecurityException 
	 */
	public String check_decrypt(String given) throws NoSuchElementException, GeneralSecurityException {
		Matcher match = ENCRYPTED.matcher(given);
		if (match.matches()) {
			String encrypted = match.group(1);
			logger.info("Found encrypted entry: " + encrypted);
			return decrypt(encrypted);
		} else {
			return given;
		}
	}
}
