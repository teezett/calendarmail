package de.heinersyndikat.tools.calendarmail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access to WebDAV calendar.
 *
 * @author Sven Bauhan <sde@sven.bauhan.name>
 */
public class JackrabbitDAVAccess {

	/**
	 * Logger instance
	 */
	private static transient final Logger logger
					= LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
	/**
	 * host configuration
	 */
	private HostConfiguration hostConfig;
	private HttpClient client;
	private final String HOSTNAME;
	private static final int MAX_HOST_CONNECTIONS = 20;
	private final Credentials creds;

	public JackrabbitDAVAccess(String hostname_, String username_, String password_) {
		HOSTNAME = hostname_;
		if ((username_ != null) && (password_ != null)) {
			creds = new UsernamePasswordCredentials(username_, password_);
		} else {
			creds = null;
		}
		init();
	}

	public final void init() {
		hostConfig = new HostConfiguration();
		hostConfig.setHost(HOSTNAME);
		HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		params.setMaxConnectionsPerHost(hostConfig, MAX_HOST_CONNECTIONS);
		connectionManager.setParams(params);
		client = new HttpClient(connectionManager);
		client.getState().setCredentials(AuthScope.ANY, creds);
		client.setHostConfiguration(hostConfig);
	}

	public void getProperties(String uri_) {
		try {
			DavMethod pFind = new PropFindMethod(uri_, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_INFINITY);
			client.executeMethod(pFind);
			logger.info("PropFind executed");
			
			MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
			logger.info("multistatus built");
			
			//Not quite nice, but for a example ok
			DavPropertySet props = multiStatus.getResponses()[0].getProperties(200);
			logger.info("Got property set with " + props.getContentSize()+ "properties");
			
			Collection<? extends PropEntry> propertyColl=props.getContent();
			propertyColl.iterator();
			for(Iterator<? extends PropEntry> iterator = propertyColl.iterator(); iterator.hasNext();){
				DefaultDavProperty tmpProp=(DefaultDavProperty) iterator.next();
				logger.info(tmpProp.getName() +"  "+ tmpProp.getValue());
			}
		} catch (IOException ex) {
			logger.error("IOError: " + ex.getLocalizedMessage());
		} catch (DavException ex) {
			logger.error("DAVError: " + ex.getLocalizedMessage());
		}
	}
	
	public void listResources(String uri_) {
		try {
			logger.info("Try to list resources");
//			String resourcePath = "https://colabori.de/remote.php/caldav/calendars/info%40heinersyndikat.de/defaultcalendar1_shared_by_heinersyndikat@Sven.bauhan.name";
			DavMethod pFind = new PropFindMethod(uri_, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
			client.executeMethod(pFind);
			logger.info("Got properties");
			String responseText = pFind.getResponseBodyAsString();
			logger.info("Got response: " + responseText);
			
			MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
			MultiStatusResponse[] responses = multiStatus.getResponses();
			MultiStatusResponse currResponse;
			ArrayList files = new ArrayList();
			System.out.println("Folders and files in " + uri_ + ":");
			for (MultiStatusResponse response : responses) {
				currResponse = response;
				//				if (!(currResponse.getHref().equals(path) || currResponse.getHref().equals(path + "/"))) {
				System.out.println(currResponse.getHref());
//				}
			}
		} catch (IOException | DavException ex) {
		}
	}
	
	public int copy(String from_, String to_, boolean override_) {
		DavMethod copyM = new CopyMethod(from_, to_, override_);
		logger.info("Copy '" + from_ + "' to '" + to_ + "'");
		try {
			client.executeMethod(copyM);
		} catch (IOException ex) {
			logger.error("IOError: " + ex.getLocalizedMessage());
		}
		logger.info(copyM.getStatusText());
		return copyM.getStatusCode();
	}
}
