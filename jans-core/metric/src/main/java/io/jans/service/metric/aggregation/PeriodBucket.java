/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import io.jans.model.metric.MetricAggregationType;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * One aggregation period: the half-open interval {@code [start, end)} in UTC for an aggregation type.
 * Instances are created by {@link PeriodMath}, so start and end are always aligned to the type.
 */
public final class PeriodBucket implements Comparable<PeriodBucket> {

    private final MetricAggregationType type;
    private final Instant start;
    private final Instant end;

    PeriodBucket(MetricAggregationType type, Instant start, Instant end) {
        this.type = Objects.requireNonNull(type, "type");
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
    }

    public MetricAggregationType getType() {
        return type;
    }

    /**
     * @return first instant of the period (inclusive)
     */
    public Instant getStart() {
        return start;
    }

    /**
     * @return first instant after the period (exclusive)
     */
    public Instant getEnd() {
        return end;
    }

    public Date getStartDate() {
        return Date.from(start);
    }

    public Date getEndDate() {
        return Date.from(end);
    }

    /**
     * @return {@code true} if {@code start <= instant < end}
     */
    public boolean contains(Instant instant) {
        return !instant.isBefore(start) && instant.isBefore(end);
    }

    /**
     * @return length of the period in seconds
     */
    public long getLengthSeconds() {
        return end.getEpochSecond() - start.getEpochSecond();
    }

    @Override
    public int compareTo(PeriodBucket other) {
        int result = type.compareTo(other.type);
        return result != 0 ? result : start.compareTo(other.start);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PeriodBucket)) {
            return false;
        }
        PeriodBucket other = (PeriodBucket) obj;
        return type == other.type && start.equals(other.start) && end.equals(other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, start, end);
    }

    @Override
    public String toString() {
        return type + "[" + start + ", " + end + ")";
    }

}
