package de.heinersyndikat.tools.calendarmail;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import static de.heinersyndikat.tools.calendarmail.SardineDAVAccess.resource2uri;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handling a remote calendar ressource.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class RemoteCalendar {

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	public static final String CONFIG_KEYWORD = "calendars";
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private String hostname;
	private String address;
	private String username;
	private String password;

	/**
	 * Read the CalDAV calendar from given ressource address.
	 *
	 * @return collection of events in this calencar
	 * @throws java.io.IOException
	 */
	public Collection getEvents() throws IOException {
		if (getAddress().equals("")) {
			throw new IOException("No address given for calendar '" + getHostname() + "'");
		}
		Sardine webdav = new SardineTrustAlways(getUsername(), getPassword());
		// handle CalDAV directory of iCal files
		try {
			// get list of resources at given address
			URI base = new URI(getAddress());
			logger.debug("Investigating URL " + base);
			List<DavResource> resources = webdav.list(getAddress());
			// reformat resources to get correct URI
			List<URI> calendars = resources.stream()
							.filter(r -> r.getContentType().contains("calendar"))
							.map(r -> resource2uri(base, r))
							.collect(Collectors.toList());
			// read the events from all calendars
			Collection events = calendars.stream()
							// read and parse calendar from given URI
							.map(url -> {
								try {
									logger.debug("Found calendar " + url);
									InputStream is = webdav.get(url.toString());
									CalendarBuilder builder = new CalendarBuilder();
									return builder.build(is);
								} catch (IOException ex) {
									logger.warn("Error reading address " + url + ": " + ex.getLocalizedMessage());
								} catch (ParserException ex) {
									logger.warn("Error parsing calendar " + url + ": " + ex.getLocalizedMessage());
								}
								return new Calendar();
							})
							// extract a list of all events
							.map(cal -> cal.getComponents(Component.VEVENT))
							// flatten the list of lists of events
							.flatMap(l -> l.stream())
							.collect(Collectors.toList());
			logger.info("Found " + events.size() + " Events in " + calendars.size()
							+ " Calendar files in calendar " + getHostname());
			return events;
		} catch (IOException | URISyntaxException ex) {
			logger.debug(ex.getLocalizedMessage());
		}
		// handle single iCal file
		InputStream in_stream = webdav.get(getAddress());
		if (in_stream != null) {
			Collection events = new ArrayList();
			try {
				CalendarBuilder builder = new CalendarBuilder();
				Calendar iCal = builder.build(in_stream);
				events = iCal.getComponents(Component.VEVENT);
			} catch (ParserException ex) {
				logger.warn("Unable to parse calendar file at " + getAddress());
				logger.warn(ex.getLocalizedMessage());
			}
			logger.info("Found iCal file with " + events.size()
							+ " entries for calendar " + getHostname());
			return events;
		}
		logger.warn("Could not get valid calendar information for calendar "
						+ getHostname());
		return new ArrayList();
	}

	/**
	 * Convert a list of calendar events into textual representation.
	 *
	 * @param events_ list of calendar events
	 * @return textual representation
	 */
	public static String eventlist_to_string(Collection events_) {
		return events_.stream()
						.map(ev -> event_to_string((VEvent) ev))
						.collect(Collectors.joining("\n"))
						.toString();
	}

	/**
	 * Convert an event to a string representation.
	 *
	 * @param event the given event
	 * @return string representation of event
	 */
	public static String event_to_string(VEvent event) {
		StringBuilder builder = new StringBuilder();
		// Event time
		DtStart start = event.getStartDate();
		if (start != null) {
			Instant startDate = start.getDate().toInstant();
			LocalDateTime startLocal = LocalDateTime.ofInstant(startDate, ZoneId.systemDefault());
			builder.append(startLocal.format(DATE_FORMAT));
			logger.debug("Start: " + startLocal.format(DATE_FORMAT));
			DtEnd ending = event.getEndDate(true);
			if (ending != null) {
				LocalDateTime endLocal = LocalDateTime
								.ofInstant(ending.getDate().toInstant(), ZoneId.systemDefault());
				builder.append(" - ").append(endLocal.format(DATE_FORMAT));
			}
			builder.append("\n");
		}
		// event title
		builder.append(event.getProperty(Property.SUMMARY).getValue()).append("\n");
		// event location
		Location loc = event.getLocation();
		if (loc != null) {
			builder.append("Ort: ").append(loc.getValue()).append("\n");
			logger.debug("Location: " + loc.getValue());
		}
		return builder.toString();
	}

	/**
	 * Static method to filter all given calendars for events and generate a
	 * string representation.
	 *
	 * @param calendars calendars to be combined and filtered
	 * @param filter_ filter for events
	 * @return a collection of all filtered events
	 */
	public static Collection filterAll(List<RemoteCalendar> calendars, Filter filter_) {
		// collect events of all calendars and sort them chronologically
		Collection events = (Collection) calendars.stream()
						.map(c -> {
							try {
								return c.getEvents();
							} catch (IOException ex) {
								logger.warn(ex.getLocalizedMessage());
								return new ArrayList<VEvent>();
							}
						})
						.flatMap(l -> l.stream())
						.sorted(new EventComparator())
						.collect(Collectors.toList());
		// filter the events using the given filter
		Collection filtered_events;
		if (filter_ != null) {
			filtered_events = filter_.filter(events);
		} else {
			filtered_events = events;
		}
		return filtered_events;
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
	public void setPassword(String password) throws NoSuchElementException, MailExceptionWrapper {
		try {
			// Decrypt if encrypted
			Encryption encryption = new Encryption();
			this.password = encryption.check_decrypt(password);
		} catch (GeneralSecurityException ex) {
			throw new MailExceptionWrapper(ex);
		}
		this.password = password;
	}

}
