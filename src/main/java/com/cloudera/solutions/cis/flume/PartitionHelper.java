package com.cloudera.solutions.cis.flume;

public class PartitionHelper {

	private static final int SECS_IN_HOUR = 60 * 60;
	
	public static long getHourPartition(long tsInMillis) {
		long tsInSecs = tsInMillis / 1000;
		return (tsInSecs / SECS_IN_HOUR) * SECS_IN_HOUR;
	}
	

}
