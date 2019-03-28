/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.metric.ldap;

import java.util.Date;
import java.util.List;

/**
 * Metric report
 *
 * @author Yuriy Movchan Date: 03/16/2017
 */
public class MetricReport {

    private List<MetricEntry> metricEntries;

    private Date creationTime;

    public MetricReport(List<MetricEntry> metricEntries, Date creationTime) {
        this.metricEntries = metricEntries;
        this.creationTime = creationTime;
    }

    public List<MetricEntry> getMetricEntries() {
        return metricEntries;
    }

    public void setMetricEntries(List<MetricEntry> metricEntries) {
        this.metricEntries = metricEntries;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

}
