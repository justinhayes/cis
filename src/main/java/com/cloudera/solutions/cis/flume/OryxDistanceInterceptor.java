package com.cloudera.solutions.cis.flume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class OryxDistanceInterceptor implements Interceptor {

	private final static Logger logger = LoggerFactory
			.getLogger(OryxDistanceInterceptor.class);

	private final Context context;
	private final String oryxDistanceResource;

	private static final String ORYX_SERVER_PROP_NAME = "oryxServer";
	private static final String ORYX_DISTANCE_RESOURCE = "distanceToNearest";

	protected OryxDistanceInterceptor(Context context) {
		Preconditions.checkNotNull(context);
		this.context = context;
		this.oryxDistanceResource = "http://" + context.getString(ORYX_SERVER_PROP_NAME) + "/" + ORYX_DISTANCE_RESOURCE;
		logger.info("Using this resource to get distance: " + this.oryxDistanceResource);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

	@Override
	public Event intercept(Event event) {
		String body = new String(event.getBody());
		
//		logger.info("Getting distance via " + this.oryxDistanceResource + "/" + body);
		
//		logger.info("  Headers:");
//	    for (Entry<String, String> entry : event.getHeaders().entrySet()) {
//	    	logger.info("    " + entry.getKey() + "=" + entry.getValue());
//	    }
		
		//TODO get HTTP connection to rest server, make rest call, interpret result, add flag/field if this is a positive
	    boolean isEvent = false;
	    
	    event.getHeaders().put("outlier", (isEvent ? "1" : "0"));
		
		return event;
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
