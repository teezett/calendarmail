package de.heinersyndikat.tools.calendarmail;

import java.util.Comparator;
import net.fortuna.ical4j.model.component.VEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class EventComparator implements Comparator<VEvent> {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	@Override
	public int compare(VEvent t, VEvent t1) {
		return t.getStartDate().getDate().compareTo(t1.getStartDate().getDate());
	}

}
