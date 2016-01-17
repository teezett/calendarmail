package de.heinersyndikat.tools.calendarmail;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;

/**
 * Connect to WebDAV using Sardine.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class SardineDAVAccess {

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	private final String hostname;
	private final Sardine connection;

	/**
	 * Initialization constructor.
	 *
	 * Set \c username_ or \c password_ to \c null for connection without
	 * authentication.
	 *
	 * @param hostname_ host name
	 * @param username_ user name
	 * @param password_ password
	 */
	public SardineDAVAccess(String hostname_, String username_, String password_) {
		hostname = hostname_;
		if ((username_ != null) && (password_ != null)) {
			connection = new SardineTrustAlways(username_, password_);
		} else {
			connection = new SardineTrustAlways();
		}
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Read the data from the given address
	 * 
	 * @param uri_ address to read from
	 * @return InputStream of the data at the given address
	 * @throws IOException 
	 */
	public InputStream read(String uri_) throws IOException {
		InputStream is;
		is = connection.get(uri_);
		return is;
	}

	public void list(String uri_) {
		try {
			URI base = new URI(uri_);
			logger.info("Investigating URL " + base);
			List<DavResource> resources = connection.list(uri_);
			List<URI> calendars = resources.stream()
							.filter(r -> r.getContentType().contains("calendar"))
							.map(r -> resource2uri(base, r))
							.collect(Collectors.toList());
			Collection events = new ArrayList();
			for (URI calendar : calendars) {
				logger.info("Found calendar " + calendar);
				try {
					InputStream is = connection.get(calendar.toString());
					CalendarBuilder builder = new CalendarBuilder();
					Calendar iCal = builder.build(is);
					events.addAll(iCal.getComponents(Component.VEVENT));
				} catch (IOException ex) {
					logger.warn("Error reading address " + calendar + ": " + ex.getLocalizedMessage());
				} catch (ParserException ex) {
					logger.warn("Error parsing calendar " + calendar + ": " + ex.getLocalizedMessage());
				}
			}
			logger.info("Found " + events.size() + " Events in " + calendars.size() + " Calendar files.");
		} catch (IOException | URISyntaxException ex) {
			logger.error(ex.getLocalizedMessage());
		}
	}

	/**
	 * Conversion of DavResource to URI.
	 * 
	 * @param base base URI address
	 * @param res resource to be converted
	 * @return converted URI
	 * @throws RuntimeException
	 */
	static public URI resource2uri(URI base, DavResource res) {
		try {
			return new URI(base.getScheme(), base.getHost(), res.getPath(), null);
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}
}
