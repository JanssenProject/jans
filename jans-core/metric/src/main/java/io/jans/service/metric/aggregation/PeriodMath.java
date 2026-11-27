/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import io.jans.model.metric.MetricAggregationType;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Period arithmetic for metric aggregation. All periods are in UTC and do not depend on the default
 * time zone of the JVM. An HOURLY or DAILY period starts at the full hour or day, a WEEKLY period is an
 * ISO week that starts on Monday 00:00, and a MONTHLY period starts on the first day of the month.
 */
public final class PeriodMath {

    private PeriodMath() {
    }

    /**
     * @return start of the period of {@code type} that contains {@code instant}
     */
    public static Instant bucketStart(MetricAggregationType type, Instant instant) {
        Objects.requireNonNull(type, "type");
        ZonedDateTime time = instant.atZone(ZoneOffset.UTC);
        switch (type) {
        case HOURLY:
            return time.truncatedTo(ChronoUnit.HOURS).toInstant();
        case DAILY:
            return time.truncatedTo(ChronoUnit.DAYS).toInstant();
        case WEEKLY:
            return time.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toInstant();
        case MONTHLY:
            return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant();
        default:
            throw new IllegalArgumentException("Unsupported aggregation type: " + type);
        }
    }

    /**
     * @return end (exclusive) of the period of {@code type} that contains {@code instant}
     */
    public static Instant bucketEnd(MetricAggregationType type, Instant instant) {
        ZonedDateTime start = bucketStart(type, instant).atZone(ZoneOffset.UTC);
        switch (type) {
        case HOURLY:
            return start.plusHours(1).toInstant();
        case DAILY:
            return start.plusDays(1).toInstant();
        case WEEKLY:
            return start.plusWeeks(1).toInstant();
        case MONTHLY:
            return start.plusMonths(1).toInstant();
        default:
            throw new IllegalArgumentException("Unsupported aggregation type: " + type);
        }
    }

    /**
     * @return the period of {@code type} that contains {@code instant}
     */
    public static PeriodBucket bucketContaining(MetricAggregationType type, Instant instant) {
        return new PeriodBucket(type, bucketStart(type, instant), bucketEnd(type, instant));
    }

    /**
     * @return the period that follows {@code bucket}
     */
    public static PeriodBucket next(PeriodBucket bucket) {
        return bucketContaining(bucket.getType(), bucket.getEnd());
    }

    /**
     * @return the period that precedes {@code bucket}
     */
    public static PeriodBucket previous(PeriodBucket bucket) {
        return bucketContaining(bucket.getType(), bucket.getStart().minusMillis(1));
    }

    /**
     * Lists the periods of {@code type} that overlap {@code [from, to)}, oldest first. The first period
     * is the one that contains {@code from}; the last one is the period that contains the instant just
     * before {@code to}. Returns an empty list when {@code to} is not after {@code from}.
     */
    public static List<PeriodBucket> buckets(MetricAggregationType type, Instant from, Instant to) {
        List<PeriodBucket> result = new ArrayList<>();
        if (!to.isAfter(from)) {
            return result;
        }

        PeriodBucket bucket = bucketContaining(type, from);
        while (bucket.getStart().isBefore(to)) {
            result.add(bucket);
            bucket = next(bucket);
        }

        return result;
    }

    /**
     * Grace period after the end of a period before it counts as closed: two reporter intervals plus 60
     * seconds, so that the rows of the last interval have been written.
     *
     * @param metricReporterIntervalSeconds interval of the raw metric reporter, in seconds
     */
    public static Duration grace(int metricReporterIntervalSeconds) {
        return Duration.ofSeconds(2L * Math.max(0, metricReporterIntervalSeconds) + 60L);
    }

    /**
     * @return {@code true} when {@code now} is at or after {@code bucket end + grace}
     */
    public static boolean isClosed(PeriodBucket bucket, Instant now, Duration grace) {
        return !now.isBefore(bucket.getEnd().plus(grace));
    }

}
