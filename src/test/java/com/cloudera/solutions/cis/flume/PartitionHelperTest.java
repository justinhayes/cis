package com.cloudera.solutions.cis.flume;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PartitionHelperTest {
	
	
	@Test
	public void testHourPartition() {
		long tsInMillis1  = 1385426818000L;
		long tsInHours1 = 1385424000;

		long tsInMillis2  = 1385477093000L;
		long tsInHours2 = 1385474400;

		assertTrue("Calculation failed.", PartitionHelper.getHourPartition(tsInMillis1) == tsInHours1);
		assertTrue("Calculation failed.", PartitionHelper.getHourPartition(tsInMillis2) == tsInHours2);
	}

}
