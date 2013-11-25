/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloudera.solutions.cis.flume;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.serialization.EventSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class simply writes a single header in the event to the output stream and
 * appends a newline after each event.
 */
public class HeaderTextEventSerializer implements EventSerializer {

	private final static Logger logger = LoggerFactory
			.getLogger(HeaderTextEventSerializer.class);

	// for legacy reasons, by default, append a newline to each event written
	// out
	private final String APPEND_NEWLINE = "appendNewline";
	private final boolean APPEND_NEWLINE_DFLT = true;

	private final OutputStream out;
	private final boolean appendNewline;
	
	private final String HEADER_KEY = "record";

	private HeaderTextEventSerializer(OutputStream out, Context ctx) {
		this.appendNewline = ctx
				.getBoolean(APPEND_NEWLINE, APPEND_NEWLINE_DFLT);
		this.out = out;
	}

	@Override
	public boolean supportsReopen() {
		return true;
	}

	@Override
	public void afterCreate() {
		// noop
	}

	@Override
	public void afterReopen() {
		// noop
	}

	@Override
	public void beforeClose() {
		// noop
	}

	@Override
	public void write(Event e) throws IOException {
		if (e.getHeaders().containsKey(HEADER_KEY)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Writing event header: " + e.getHeaders().get(HEADER_KEY));
			}
			out.write(e.getHeaders().get(HEADER_KEY).getBytes());
			if (appendNewline) {
				out.write('\n');
			}
		} else {
			logger.warn("Event " + e + " did not contain a '" + HEADER_KEY + "' header, so no data serialized.");
			logger.warn("Invalid record: " + e.getHeaders().get("record2"));
		}
	}

	@Override
	public void flush() throws IOException {
		// noop
	}

	public static class Builder implements EventSerializer.Builder {

		@Override
		public EventSerializer build(Context context, OutputStream out) {
			HeaderTextEventSerializer s = new HeaderTextEventSerializer(
					out, context);
			return s;
		}

	}

}