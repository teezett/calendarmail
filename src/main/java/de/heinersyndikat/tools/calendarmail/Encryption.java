package de.heinersyndikat.tools.calendarmail;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
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

	private final static String DEFAULT_SALT = "12345678";
	private final PBEParameterSpec pbeParamSpec;
	private final SecretKey pbeKey;
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
		String password = CalendarMailConfiguration.INSTANCE.getPassword();
		pbeParamSpec = new PBEParameterSpec(DEFAULT_SALT.getBytes(), 20);
		// Convert password to SecretKey object
		SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
		pbeKey = keyFac.generateSecret(pbeKeySpec);
		// Create PBE Cipher
		cipher = Cipher.getInstance("PBEWithMD5AndDES");
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
	String encrypt(String string_to_enc) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		// initialize cipher
		cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
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
	String decrypt(String encoded) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		// Base64 decoding
		byte[] encrypted = Base64.getDecoder().decode(encoded);
		// initialize cipher
		cipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
		// decrypt
		byte[] decrypted = cipher.doFinal(encrypted);
		return new String(decrypted);
	}
}
