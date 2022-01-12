/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.counter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.model.metric.MetricData;

/**
 * Numeric counter metric data class
 *
 * @author Yuriy Movchan Date: 07/28/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CounterMetricData extends MetricData {

    private static final long serialVersionUID = -2322501012136295255L;

    private long count;

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CounterMetricData [count=").append(count).append(", toString()=").append(super.toString()).append("]");
        return builder.toString();
    }

}
