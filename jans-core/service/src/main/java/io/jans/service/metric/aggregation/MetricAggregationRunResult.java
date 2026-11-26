/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.metric.aggregation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import io.jans.model.metric.MetricAggregationType;

/**
 * Outcome of one {@link MetricAggregationManager#runOnce()} call.
 */
public class MetricAggregationRunResult {

    public enum Status {
        /** Gating is off: nothing was read or written. */
        DISABLED,
        /** Another node holds the slot, or the claim could not be inserted. */
        NOT_CLAIMED,
        /** The slot was claimed, but another run of the task is still active; released as {@code skipped}. */
        SKIPPED,
        /** The task state could not be read or saved; released as {@code failed}. */
        STATE_ERROR,
        /** The run finished; released as {@code done}. */
        DONE,
        /** Work stopped, but release could not be confirmed; completion is not claimed. */
        RELEASE_UNCONFIRMED,
        /** The claim was lost during the run (heartbeat failed); the run stopped without release. */
        LOST,
        /** An unexpected error stopped the run; released as {@code failed}. */
        FAILED
    }

    private final Status status;
    private final long slot;
    private final Map<MetricAggregationType, Integer> buckets;
    private final Map<MetricAggregationType, Integer> failedBuckets;
    private final int rowsWritten;
    private final long durationMillis;

    MetricAggregationRunResult(Status status, long slot, Map<MetricAggregationType, Integer> buckets,
            Map<MetricAggregationType, Integer> failedBuckets, int rowsWritten, long durationMillis) {
        this.status = status;
        this.slot = slot;
        this.buckets = copy(buckets);
        this.failedBuckets = copy(failedBuckets);
        this.rowsWritten = rowsWritten;
        this.durationMillis = durationMillis;
    }

    static MetricAggregationRunResult of(Status status, long slot) {
        return new MetricAggregationRunResult(status, slot, null, null, 0, 0);
    }

    public Status getStatus() {
        return status;
    }

    /**
     * @return the run slot, or -1 when no slot was computed
     */
    public long getSlot() {
        return slot;
    }

    /**
     * @return number of buckets processed successfully at the level
     */
    public int getBuckets(MetricAggregationType type) {
        return buckets.getOrDefault(type, 0);
    }

    /**
     * @return number of buckets that failed at the level
     */
    public int getFailedBuckets(MetricAggregationType type) {
        return failedBuckets.getOrDefault(type, 0);
    }

    public int getTotalFailedBuckets() {
        int result = 0;
        for (Integer value : failedBuckets.values()) {
            result += value;
        }
        return result;
    }

    public int getRowsWritten() {
        return rowsWritten;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    private static Map<MetricAggregationType, Integer> copy(Map<MetricAggregationType, Integer> source) {
        Map<MetricAggregationType, Integer> result = new EnumMap<>(MetricAggregationType.class);
        if (source != null) {
            result.putAll(source);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public String toString() {
        return "MetricAggregationRunResult [status=" + status + ", slot=" + slot + ", buckets=" + buckets + ", failedBuckets="
                + failedBuckets + ", rowsWritten=" + rowsWritten + ", durationMillis=" + durationMillis + "]";
    }

}
