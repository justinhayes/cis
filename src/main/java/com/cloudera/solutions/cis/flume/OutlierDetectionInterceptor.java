package com.cloudera.solutions.cis.flume;

import java.util.ArrayList;
import java.util.List;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * This interceptor determines whether or not this data point is an outlier. It adds an 
 * "outlier" header entry with 0 if the point is not considered to be an outlier, and 1
 * if it is.
 * 
 * The current implementation makes a REST call to the Oryx serving layer to get the distance 
 * from this point to the nearest cluster center, according to the current clustering model.
 * 
 * TODO see if there is a more efficient way to do REST calls; perhaps use a pool of clients?
 * 
 * TODO need a more dynamic way to determine whether a distance is an outlier. The threshold
 * for what constitutes an outlier should be change as the model evolves (ie as new training
 * data is added to the model as part of normal operation.) This will require work within Oryx.
 * 
 * @author jhayes
 *
 */
public class OutlierDetectionInterceptor implements Interceptor {

	private final static Logger logger = LoggerFactory
			.getLogger(OutlierDetectionInterceptor.class);

	private final Context context;
	private static String oryxDistanceResource;
	
	//default to max int in case the provided property doesn't parse;
	//that way there will be no false positives, which is the least bad default option
	private static int outlierDistanceThreshold = Integer.MAX_VALUE;
	
	private static Client restClient = null;

	private static final String ORYX_SERVER_PROP_NAME = "oryxServer";
	private static final String OUTLIER_DISTANCE_THRESHOLD_PROP_NAME = "outlierDistanceThreshold";
	private static final String ORYX_DISTANCE_RESOURCE = "distanceToNearest";

	protected OutlierDetectionInterceptor(Context context) {
		Preconditions.checkNotNull(context);
		this.context = context;
		
		oryxDistanceResource = "http://" + context.getString(ORYX_SERVER_PROP_NAME) + "/" + ORYX_DISTANCE_RESOURCE + "/";
		
		String outlierDistanceThresholdStr = context.getString(OUTLIER_DISTANCE_THRESHOLD_PROP_NAME);
		try {
			outlierDistanceThreshold = Integer.parseInt(outlierDistanceThresholdStr);
		} catch (NumberFormatException ex) {
			logger.error("Unable to parse {} property: {}. It should be an integer.", 
					OUTLIER_DISTANCE_THRESHOLD_PROP_NAME, outlierDistanceThresholdStr);
		}
		
		logger.info("Using this resource to get distance: {}.", oryxDistanceResource);
		logger.info("Using this outlier distance threshold: {}.", outlierDistanceThreshold);
		
		//initialize the rest client object
		if (restClient == null) {
			DefaultClientConfig restClientConfig = new DefaultClientConfig();
			restClient = Client.create(restClientConfig);
		}
	}

	@Override
	public void close() {
		//do nothing
	}

	@Override
	public void initialize() {
		//do nothing
	}

	@Override
	public Event intercept(Event event) {
		String body = new String(event.getBody());
		
		//make rest call to get distance
		ClientResponse response = restClient.resource(oryxDistanceResource + body)
					.accept("application/json")
					.get(ClientResponse.class);
		if (response.getStatus() != 200) {
			logger.error("Got return code {}.", response.getStatus());
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		
		String output = response.getEntity(String.class);
		Double distance = new Double(output);
		
		//interpret distance to decide if this is an outlier
		boolean isEvent = isOutlier(distance);

		if (logger.isDebugEnabled()) {
			logger.debug("isEvent={} for distance {} on data point {}", new Object[] {isEvent, distance, body});
		}

		//log the positive event if necessary
		if (isEvent) {
			logger.info("Found outlier with distance {}. Data point is {}", distance, body);
		}
		
	    event.getHeaders().put("outlier", (isEvent ? "1" : "0"));
		
		return event;
	}

	private boolean isOutlier(Double distance) {
		//just for testing
		//return Math.round(distance) % 2 == 0;
		
		return distance > outlierDistanceThreshold;
	}

	@Override
	public List<Event> intercept(List<Event> events) {
		List results = new ArrayList(events.size());
		for (Event event : events) {
			event = intercept(event);
			if (event != null) {
				results.add(event);
			}
		}
		return results;
	}

	// /////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	// /////////////////////////////////////////////////////////////////////////////
	/** Builder implementations MUST have a public no-arg constructor */
	public static class Builder implements Interceptor.Builder {

		private Context context;

		public Builder() {
		}

		@Override
		public OutlierDetectionInterceptor build() {
			return new OutlierDetectionInterceptor(context);
		}

		@Override
		public void configure(Context context) {
			this.context = context;
		}

	}

}
