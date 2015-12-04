package de.heinersyndikat.tools.calendarmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for checked exceptions.
 * 
 * The checked exceptions do not work in combination with lambda expressions;
 * they have to be wrapped inside an unchecked expression.
 * @see <a href="http://www.nosid.org/java-about-checked-exceptions.html">Das Ende der Checked Exception</a>
 * 
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class MailExceptionWrapper extends RuntimeException {

	/** Logger instance */
	private static transient final Logger logger =
					LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

	/**
	 * @see RuntimeException 
	 * @param thrwbl  exception to be wrapped
	 */
	public MailExceptionWrapper(Throwable thrwbl) {
		super(thrwbl);
	}
	
}
