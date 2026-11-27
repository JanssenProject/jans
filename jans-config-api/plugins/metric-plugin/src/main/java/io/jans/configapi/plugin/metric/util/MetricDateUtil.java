/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.util;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Parses the start_date/end_date query parameters accepted by the metric endpoints: ISO-8601
 * date-time (with or without offset), ISO-8601 date, or the legacy dd-MM-yyyy format.
 *
 * Note for maintainers: an ISO date-only value (e.g. "2026-08-01") must be parsed with
 * {@link LocalDate#parse(CharSequence, DateTimeFormatter)} and then {@code atStartOfDay()} -
 * {@code LocalDateTime.parse} cannot build a time-of-day out of a date-only pattern and throws
 * for every input on that branch. Do not "simplify" this back to LocalDateTime.parse.
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public class MetricDateUtil {

    // Raw jansMetric rows and jansMetricAggregation rows end 1 ms before the period end
    // (hh:59:59.999), and the data stores milliseconds. A whole-second end bound is therefore
    // widened to the last millisecond of that second.
    private static final int LAST_MILLISECOND_NANOS = 999_000_000;

    private static final String LEGACY_DATE_FORMAT = "dd-MM-yyyy";
    private static final DateTimeFormatter LEGACY_FORMATTER = DateTimeFormatter.ofPattern(LEGACY_DATE_FORMAT);

    public static final String ACCEPTED_FORMATS_MESSAGE = "Accepted formats: ISO-8601 date-time "
            + "(e.g. 2026-08-01T00:00:00Z), ISO-8601 date (e.g. 2026-08-01), or legacy dd-MM-yyyy (e.g. 01-08-2026).";

    private MetricDateUtil() {
    }

    /**
     * Parses a user-supplied date or date-time string, returning it normalized to UTC.
     *
     * @param value the raw query-parameter value
     * @return the parsed LocalDateTime (UTC wall-clock), or null if value is blank
     * @throws DateTimeParseException if value does not match any supported format
     */
    public static LocalDateTime parseDateTime(String value) throws DateTimeParseException {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();

        // ISO-8601 offset date-time, e.g. 2026-08-01T00:00:00Z, 2026-08-01T00:00:00+01:00
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            // Normalize to UTC before dropping the offset, otherwise the wall-clock time would be
            // read as if it were already UTC and the range would shift by the caller's offset.
            return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // continue
        }

        // ISO local date-time, e.g. 2026-08-01T00:00:00
        try {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // continue
        }

        // ISO date only, e.g. 2026-08-01
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            // continue
        }

        // Legacy dd-MM-yyyy
        return LocalDate.parse(trimmed, LEGACY_FORMATTER).atStartOfDay();
    }

    /**
     * Parses an end bound and makes it inclusive up to the last millisecond of the precision the
     * caller gave. Rows end 1 ms before a period boundary (for example 23:59:59.999), so the plain
     * value of {@code 2026-08-01T23:59:59Z} (23:59:59.000) would miss them.
     * <ul>
     * <li>a date-time without a fractional second (or with .000) ends at the end of that second:
     * {@code 2026-08-01T23:59:59Z} becomes 23:59:59.999;</li>
     * <li>a date-time with a non-zero fractional second is used as given;</li>
     * <li>a date only (ISO-8601 or legacy dd-MM-yyyy) ends at the end of that day: 23:59:59.999.</li>
     * </ul>
     *
     * @param value the raw query-parameter value
     * @return the inclusive end bound (UTC wall-clock), or null if value is blank
     * @throws DateTimeParseException if value does not match any supported format
     */
    public static LocalDateTime parseEndDateTime(String value) throws DateTimeParseException {
        LocalDateTime parsed = parseDateTime(value);
        if (parsed == null) {
            return null;
        }
        if (isDateOnly(value.trim())) {
            return parsed.toLocalDate().atTime(23, 59, 59, LAST_MILLISECOND_NANOS);
        }
        if (parsed.getNano() == 0) {
            return parsed.withNano(LAST_MILLISECOND_NANOS);
        }
        return parsed;
    }

    private static boolean isDateOnly(String trimmed) {
        // A date-time always contains the 'T' separator; the two date-only formats never do.
        return trimmed.indexOf('T') < 0 && trimmed.indexOf('t') < 0;
    }

    /**
     * Converts a parsed UTC LocalDateTime to java.util.Date for use in ORM filters.
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
