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
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class OryxDistanceInterceptor implements Interceptor {

	private final static Logger logger = LoggerFactory
			.getLogger(OryxDistanceInterceptor.class);

	private final Context context;
	private final String oryxDistanceResource;
	private Client restClient = null;

	private static final String ORYX_SERVER_PROP_NAME = "oryxServer";
	private static final String ORYX_DISTANCE_RESOURCE = "distanceToNearest";

	protected OryxDistanceInterceptor(Context context) {
		Preconditions.checkNotNull(context);
		this.context = context;
		this.oryxDistanceResource = "http://" + context.getString(ORYX_SERVER_PROP_NAME) + "/" + ORYX_DISTANCE_RESOURCE + "/";
		logger.info("Using this resource to get distance: {}.", this.oryxDistanceResource);
		
		//TODO get threadsafe HTTP connection to server
		//TODO figure out most efficient way to do REST calls
		//TODO use a connection pool to increase throughput?
		DefaultClientConfig restClientConfig = new DefaultClientConfig();
		this.restClient = Client.create(restClientConfig);
	}

	@Override
	public void close() {

	}

	@Override
	public void initialize() {

	}

	@Override
	public Event intercept(Event event) {
		String body = new String(event.getBody());
		
		//make rest call to get distance
		WebResource webResource = this.restClient.resource(this.oryxDistanceResource + body);
		ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
		if (response.getStatus() != 200) {
			logger.error("Got return code {}.", response.getStatus());
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		
		String output = response.getEntity(String.class);
		Double distance = new Double(output);
		
		//interpret distance to decide if this is an outlier
		boolean isEvent = isOutlier(distance);
		
		if (logger.isDebugEnabled()) {
			logger.debug("isEvent={} for distance {}", isEvent, distance);
		}
		
	    event.getHeaders().put("outlier", (isEvent ? "1" : "0"));
		
		return event;
	}

	//TODO implement
	private boolean isOutlier(Double distance) {
		boolean isOutlier = Math.round(distance) % 2 == 0;
		
		return isOutlier;
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
		public OryxDistanceInterceptor build() {
			return new OryxDistanceInterceptor(context);
		}

		@Override
		public void configure(Context context) {
			this.context = context;
		}

	}

}
