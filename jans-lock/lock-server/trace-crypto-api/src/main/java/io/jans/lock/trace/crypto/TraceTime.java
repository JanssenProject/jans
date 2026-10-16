/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Renders and parses the TRACE receipt timestamp (design §9): an RFC 3339 UTC string
 * with exactly three fractional digits and a {@code Z} suffix, for example
 * {@code 2026-06-11T00:41:02.123Z}.
 *
 * {@link Instant#toString()} is not used because it omits the fraction when the millisecond part is
 * zero, which would change the receipt-hash input.
 *
 * @author Yuriy Movchan
 */
public final class TraceTime {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
			.withZone(ZoneOffset.UTC).withResolverStyle(ResolverStyle.STRICT);

	private TraceTime() {
	}

	/**
	 * @return {@code epochMs} as {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} in UTC
	 */
	public static String formatMillis(long epochMs) {
		return FORMAT.format(Instant.ofEpochMilli(epochMs));
	}

	/**
	 * @return epoch milliseconds for a value produced by {@link #formatMillis(long)}
	 * @throws IllegalArgumentException if the text is not in the exact three-digit-fraction UTC form
	 */
	public static long parseMillis(String text) {
		if (text == null) {
			throw new IllegalArgumentException("received_at must not be null");
		}
		try {
			return Instant.from(FORMAT.parse(text)).toEpochMilli();
		} catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("Not an RFC 3339 UTC timestamp with millisecond precision: " + text, ex);
		}
	}

}
