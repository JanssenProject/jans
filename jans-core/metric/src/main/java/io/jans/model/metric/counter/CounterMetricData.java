/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.counter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.model.metric.MetricData;

/**
 * Numeric counter metric data class.
 *
 * <p>{@code count} is cumulative since the reporting process started; it is reset only by a process
 * restart. {@code intervalCount} is the number of events counted in the reporting interval of the row
 * that holds this data. Rows written by older versions do not contain {@code intervalCount}; when such
 * a row is read, the field is 0.</p>
 *
 * @author Yuriy Movchan Date: 07/28/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CounterMetricData extends MetricData {

    private static final long serialVersionUID = -2322501012136295255L;

    private long count;

    private long intervalCount;

    public CounterMetricData() {
    }

    public CounterMetricData(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getIntervalCount() {
        return intervalCount;
    }

    public void setIntervalCount(long intervalCount) {
        this.intervalCount = intervalCount;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CounterMetricData [count=").append(count).append(", intervalCount=").append(intervalCount).append(", toString()=").append(super.toString()).append("]");
        return builder.toString();
    }

}
