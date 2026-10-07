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
     * Converts a parsed UTC LocalDateTime to java.util.Date for use in ORM filters.
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
