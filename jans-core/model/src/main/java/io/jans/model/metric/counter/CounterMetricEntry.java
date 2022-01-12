/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.counter;

import java.util.Date;

import io.jans.model.metric.ldap.MetricEntry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.JsonObject;

/**
 * Metric entry which represents numeric value
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public class CounterMetricEntry extends MetricEntry {

    @JsonObject
    @AttributeName(name = "jansData")
    private CounterMetricData metricData;

    public CounterMetricEntry() { }

    public CounterMetricEntry(String dn, String id, Date creationDate, CounterMetricData metricData) {
        super(dn, id, creationDate);

        this.metricData = metricData;
    }

    public CounterMetricData getMetricData() {
        return metricData;
    }

    public void setMetricData(CounterMetricData metricData) {
        this.metricData = metricData;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CounterMetricEntry [metricData=").append(metricData).append(", toString()=").append(super.toString()).append("]");
        return builder.toString();
    }

}
