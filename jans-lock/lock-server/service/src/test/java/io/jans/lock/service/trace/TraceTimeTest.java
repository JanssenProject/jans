/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TraceTimeTest {

	@Test
	void testFormatMillis_zeroMillis_keepsThreeFractionDigits() {
		assertEquals("2027-01-15T08:00:00.000Z", TraceTime.formatMillis(1_800_000_000_000L));
	}

	@Test
	void testFormatMillis_nonZeroMillis_padsToThreeDigits() {
		assertEquals("2027-01-15T08:00:00.123Z", TraceTime.formatMillis(1_800_000_000_123L));
		assertEquals("2027-01-15T08:00:00.007Z", TraceTime.formatMillis(1_800_000_000_007L));
		assertEquals("2027-01-15T08:00:00.120Z", TraceTime.formatMillis(1_800_000_000_120L));
	}

	@Test
	void testFormatMillis_epoch() {
		assertEquals("1970-01-01T00:00:00.000Z", TraceTime.formatMillis(0L));
	}

	@Test
	void testParseMillis_roundTrip() {
		long[] values = { 0L, 7L, 1_800_000_000_000L, 1_800_000_000_999L, 4_102_444_800_001L };
		for (long ms : values) {
			assertEquals(ms, TraceTime.parseMillis(TraceTime.formatMillis(ms)));
		}
	}

	@Test
	void testParseMillis_wrongPrecision_throws() {
		assertThrows(IllegalArgumentException.class, () -> TraceTime.parseMillis("2027-01-15T08:00:00Z"));
		assertThrows(IllegalArgumentException.class, () -> TraceTime.parseMillis("2027-01-15T08:00:00.1Z"));
		assertThrows(IllegalArgumentException.class, () -> TraceTime.parseMillis("2027-01-15T08:00:00.123456Z"));
		assertThrows(IllegalArgumentException.class, () -> TraceTime.parseMillis("2027-01-15T08:00:00.123+00:00"));
		assertThrows(IllegalArgumentException.class, () -> TraceTime.parseMillis(null));
	}

}
