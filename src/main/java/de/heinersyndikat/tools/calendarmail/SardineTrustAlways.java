package de.heinersyndikat.tools.calendarmail;

import com.github.sardine.impl.SardineImpl;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import org.apache.http.conn.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * Sardine implemtation to trust all SSL keys.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class SardineTrustAlways extends SardineImpl {

	/**
	 * TrustStrategy to trust all certificates.
	 */
	private class TrustAnyTrustStrategy implements TrustStrategy {

		/**
		 * @return Returns <tt>true</tt> always
		 */
		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
			return true;
		}
	}

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	SardineTrustAlways(String username_, String password_) {
		super(username_, password_);
	}

	SardineTrustAlways() {
		super();
	}

	@Override
	protected SSLSocketFactory createDefaultSecureSocketFactory() {
		SSLSocketFactory sslSf = null;
		try {
			TrustStrategy sslTs = new TrustAnyTrustStrategy();
			sslSf = new SSLSocketFactory(sslTs,
							SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | UnrecoverableKeyException e) {
			throw new RuntimeException(
							"Unable to construct HttpClientProvider.", e);
		}
		return sslSf;
	}

//	@Override
//	protected SchemeRegistry createDefaultSchemeRegistry() {
//		SchemeRegistry schemeRegistry = new SchemeRegistry();
//		schemeRegistry.register(new Scheme("http", 8080,
//						PlainSocketFactory.getSocketFactory()));
//		schemeRegistry.register(new Scheme("https", 443,
//						createDefaultSecureSocketFactory()));
//		return schemeRegistry;
//	}

}
