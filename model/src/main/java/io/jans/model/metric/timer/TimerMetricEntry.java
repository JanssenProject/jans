/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric.timer;

import java.util.Date;

import io.jans.model.metric.ldap.MetricEntry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.JsonObject;

/**
 * Timer entry which represents timer value
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public class TimerMetricEntry extends MetricEntry {

    @JsonObject
    @AttributeName(name = "jansData")
    private TimerMetricData metricData;

    public TimerMetricEntry() { }

    public TimerMetricEntry(String dn, String id, Date creationDate, TimerMetricData metricData) {
        super(dn, id, creationDate);

        this.metricData = metricData;
    }

    public TimerMetricData getMetricData() {
        return metricData;
    }

    public void setMetricData(TimerMetricData metricData) {
        this.metricData = metricData;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TimerMetricEntry [metricData=").append(metricData).append(", toString()=").append(super.toString()).append("]");
        return builder.toString();
    }

}
