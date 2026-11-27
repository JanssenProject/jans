/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric.aggregation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Common part of the {@code jansData} JSON of a {@code jansMetricAggregation} row.
 *
 * <ul>
 * <li>{@code kind}: {@code counter} or {@code timer}.</li>
 * <li>{@code sourceRows}: number of raw {@code jansMetric} rows inside the period that the aggregate
 * covers. For a rollup it is the sum over the child aggregates.</li>
 * <li>{@code nodes}: number of distinct nodes ({@code jansHost}) that wrote those raw rows. For a
 * rollup it is the largest value of any child aggregate, so it is a lower bound.</li>
 * <li>{@code complete}: the aggregate was computed after its period closed. For a rollup: the source
 * level was fully processed up to the end of the period and every child aggregate is complete.</li>
 * <li>{@code legacyDelta}: at least one count came from cumulative counts of rows without
 * {@code intervalCount}, so the total can be lower than the true number of events.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AggregateData implements Serializable {

    private static final long serialVersionUID = -4512879461259093118L;

    private long count;
    private long sourceRows;
    private int nodes;
    private boolean complete;
    private boolean legacyDelta;

    /**
     * @return {@code counter} or {@code timer}
     */
    public abstract String getKind();

    /**
     * @return number of events in the period
     */
    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getSourceRows() {
        return sourceRows;
    }

    public void setSourceRows(long sourceRows) {
        this.sourceRows = sourceRows;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isLegacyDelta() {
        return legacyDelta;
    }

    public void setLegacyDelta(boolean legacyDelta) {
        this.legacyDelta = legacyDelta;
    }

}
