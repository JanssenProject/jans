/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import io.jans.model.metric.MetricAggregationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodMathTest {

    private TimeZone defaultTimeZone;

    @BeforeEach
    void saveTimeZone() {
        defaultTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(defaultTimeZone);
    }

    private static Instant at(String value) {
        return Instant.parse(value);
    }

    @Test
    void hourlyBucket() {
        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.HOURLY, at("2026-11-23T10:59:59.999Z"));
        assertEquals(at("2026-11-23T10:00:00Z"), bucket.getStart());
        assertEquals(at("2026-11-23T11:00:00Z"), bucket.getEnd());
        assertEquals(3600, bucket.getLengthSeconds());
    }

    @Test
    void dailyBucket() {
        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.DAILY, at("2026-11-23T23:30:00Z"));
        assertEquals(at("2026-11-23T00:00:00Z"), bucket.getStart());
        assertEquals(at("2026-11-24T00:00:00Z"), bucket.getEnd());
    }

    @Test
    void weekBucketOfMondayStartsThatMonday() {
        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.WEEKLY, at("2026-11-23T10:00:00Z"));
        assertEquals(at("2026-11-23T00:00:00Z"), bucket.getStart());
        assertEquals(at("2026-11-30T00:00:00Z"), bucket.getEnd());
    }

    @Test
    void weekBucketOfSundayStartsPreviousMonday() {
        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.WEEKLY, at("2026-11-29T23:59:59Z"));
        assertEquals(at("2026-11-23T00:00:00Z"), bucket.getStart());
    }

    @Test
    void weekBucketCrossesYearAsIsoWeek() {
        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.WEEKLY, at("2027-01-01T12:00:00Z"));
        assertEquals(at("2026-12-28T00:00:00Z"), bucket.getStart());
        assertEquals(at("2027-01-04T00:00:00Z"), bucket.getEnd());
    }

    @Test
    void monthBucketOfFebruaryEndsFirstOfMarch() {
        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.MONTHLY, at("2026-02-15T08:00:00Z"));
        assertEquals(at("2026-02-01T00:00:00Z"), bucket.getStart());
        assertEquals(at("2026-03-01T00:00:00Z"), bucket.getEnd());
        assertEquals(28L * 86400, bucket.getLengthSeconds());
    }

    @Test
    void bucketsAreHalfOpen() {
        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.HOURLY, at("2026-11-23T10:00:00Z"));
        assertTrue(bucket.contains(at("2026-11-23T10:00:00Z")));
        assertFalse(bucket.contains(at("2026-11-23T11:00:00Z")));
        assertEquals(at("2026-11-23T11:00:00Z"), PeriodMath.bucketContaining(MetricAggregationType.HOURLY, at("2026-11-23T11:00:00Z")).getStart());
    }

    @Test
    void nextAndPrevious() {
        PeriodBucket january = PeriodMath.bucketContaining(MetricAggregationType.MONTHLY, at("2026-01-31T00:00:00Z"));
        PeriodBucket february = PeriodMath.next(january);
        assertEquals(at("2026-02-01T00:00:00Z"), february.getStart());
        assertEquals(january, PeriodMath.previous(february));
    }

    @Test
    void bucketsBetweenInstants() {
        List<PeriodBucket> buckets = PeriodMath.buckets(MetricAggregationType.HOURLY, at("2026-11-23T10:30:00Z"),
                at("2026-11-23T13:00:00Z"));
        assertEquals(3, buckets.size());
        assertEquals(at("2026-11-23T10:00:00Z"), buckets.get(0).getStart());
        assertEquals(at("2026-11-23T12:00:00Z"), buckets.get(2).getStart());

        assertEquals(4, PeriodMath.buckets(MetricAggregationType.HOURLY, at("2026-11-23T10:30:00Z"), at("2026-11-23T13:00:01Z")).size());
        assertTrue(PeriodMath.buckets(MetricAggregationType.HOURLY, at("2026-11-23T10:30:00Z"), at("2026-11-23T10:30:00Z")).isEmpty());
        assertEquals(5, PeriodMath.buckets(MetricAggregationType.WEEKLY, at("2026-11-01T00:00:00Z"), at("2026-11-24T00:00:00Z")).size());
    }

    @Test
    void dstDoesNotMatter() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        PeriodBucket springDay = PeriodMath.bucketContaining(MetricAggregationType.DAILY, at("2026-03-08T12:00:00Z"));
        assertEquals(at("2026-03-08T00:00:00Z"), springDay.getStart());
        assertEquals(86400, springDay.getLengthSeconds());
        assertEquals(24, PeriodMath.buckets(MetricAggregationType.HOURLY, springDay.getStart(), springDay.getEnd()).size());

        PeriodBucket autumnDay = PeriodMath.bucketContaining(MetricAggregationType.DAILY, at("2026-11-01T06:30:00Z"));
        assertEquals(at("2026-11-01T00:00:00Z"), autumnDay.getStart());
        assertEquals(86400, autumnDay.getLengthSeconds());
        assertEquals(24, PeriodMath.buckets(MetricAggregationType.HOURLY, autumnDay.getStart(), autumnDay.getEnd()).size());

        PeriodBucket week = PeriodMath.bucketContaining(MetricAggregationType.WEEKLY, at("2026-11-01T06:30:00Z"));
        assertEquals(at("2026-10-26T00:00:00Z"), week.getStart());
        assertEquals(7L * 86400, week.getLengthSeconds());

        PeriodBucket march = PeriodMath.bucketContaining(MetricAggregationType.MONTHLY, at("2026-03-08T07:00:00Z"));
        assertEquals(at("2026-03-01T00:00:00Z"), march.getStart());
        assertEquals(31L * 86400, march.getLengthSeconds());
    }

    @Test
    void graceAndClosed() {
        Duration grace = PeriodMath.grace(300);
        assertEquals(Duration.ofSeconds(660), grace);

        PeriodBucket bucket = PeriodMath.bucketContaining(MetricAggregationType.HOURLY, at("2026-11-23T10:00:00Z"));
        assertFalse(PeriodMath.isClosed(bucket, at("2026-11-23T11:10:59Z"), grace));
        assertTrue(PeriodMath.isClosed(bucket, at("2026-11-23T11:11:00Z"), grace));
    }

}
