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

	private Collection events;

	/**
	 * Read the CalDAV calendar from given ressource address.
	 *
	 * @return collection of events in this calencar
	 * @throws java.io.IOException
	 * @throws net.fortuna.ical4j.data.ParserException
	 */
	public Collection getEvents() throws IOException, ParserException {
		if (getAddress().equals("")) {
			throw new IOException("No address given for calendar '" + getHostname() + "'");
		}
		Sardine webdav = new SardineTrustAlways(getUsername(), getPassword());
		try {
			URI base = new URI(getAddress());
			logger.info("Investigating URL " + base);
			List<DavResource> resources = webdav.list(getAddress());
			List<URI> calendars = resources.stream()
							.filter(r -> r.getContentType().contains("calendar"))
							.map(r -> resource2uri(base, r))
							.collect(Collectors.toList());
			events = new ArrayList();
			for (URI calendar : calendars) {
				logger.info("Found calendar " + calendar);
				try {
					InputStream is = webdav.get(calendar.toString());
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
		return (Collection) events.stream()
						.map(ev -> (VEvent) ev)
						.sorted(new EventComparator())
						.collect(Collectors.toList());
	}

	/**
	 * Create a string representation of the calendar.
	 * 
	 * @param filter_  filter for events
	 * @return  string representation for the calendar events
	 * @throws IOException
	 * @throws ParserException 
	 */
	public String toString(Filter filter_) throws IOException, ParserException {
		Collection filtered_events;
		if (filter_ != null) {
			filtered_events = filter_.filter(getEvents());
		} else {
			filtered_events = getEvents();
		}
		return filtered_events.stream()
						.map(ev -> event_to_string((VEvent) ev))
						.collect(Collectors.joining("\n"))
						.toString();
	}

	/**
	 * Convert an event to a string representation.
	 * 
	 * @param event  the given event
	 * @return  string representation of event
	 */
	protected String event_to_string(VEvent event) {
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
	 * Static method to filter all given calendars for events and generate
	 * a string representation.
	 * 
	 * @param calendars  calendars to be combined and filtered
	 * @param filter  filter for events
	 * @return  string representation
	 */
	public static String filterAll(List<RemoteCalendar> calendars, Filter filter) {
		String all_events = calendars.stream()
						.map(cal -> {
							try {
								return cal.toString(filter);
							} catch (IOException | ParserException ex) {
								logger.warn(ex.getLocalizedMessage());
							}
							return "";
						})
						.filter(txt -> !txt.isEmpty())
						.collect(Collectors.joining("\n"));
		return all_events;
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
