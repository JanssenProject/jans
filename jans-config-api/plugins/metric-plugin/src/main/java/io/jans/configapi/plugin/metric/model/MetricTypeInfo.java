/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * One (applicationType, metricType) combination discovered in jansMetric data, together with the
 * distinct jansMetricSubTyp values seen for it (empty when the metric type is never reported with
 * a subtype).
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public class MetricTypeInfo {

    private String appType;
    private String metricType;
    private Set<String> subTypes = new TreeSet<>();

    public MetricTypeInfo() {
    }

    public MetricTypeInfo(String appType, String metricType) {
        this.appType = appType;
        this.metricType = metricType;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public Set<String> getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(Set<String> subTypes) {
        this.subTypes = subTypes;
    }

    @Override
    public String toString() {
        return "MetricTypeInfo [appType=" + appType + ", metricType=" + metricType + ", subTypes=" + subTypes + "]";
    }
}
