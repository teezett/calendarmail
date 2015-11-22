package de.heinersyndikat.tools.calendarmail;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connect to WebDAV using Sardine.
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class SardineDAVAccess {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	private final String hostname;
	private final Sardine connection;

	/**
	 * Initialization constructor.
	 * 
	 * Set \c username_ or \c password_ to \c null for connection without authentication.
	 * 
	 * @param hostname_ host name
	 * @param username_ user name
	 * @param password_ password
	 */
	public SardineDAVAccess(String hostname_, String username_, String password_) {
		hostname = hostname_;
		if ((username_ != null) && (password_ != null)) {
			connection = SardineFactory.begin(username_, password_);
		} else {
			connection = SardineFactory.begin();
		}
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	public void read(String uri_) {
		try {
			InputStream is = connection.get(uri_);
			String content = IOUtils.toString(is);
			logger.info("Reading content: " + content);
		} catch (IOException ex) {
			logger.error(ex.getLocalizedMessage());
		}
	}
	
	public void list(String uri_) {
		try {
			List<DavResource> resources = connection.list(uri_);
			resources.stream().forEach((res) -> {
				logger.info(res.toString());
			});
		} catch (IOException ex) {
			logger.error(ex.getLocalizedMessage());
		}
	}
}
