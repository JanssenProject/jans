/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric;

import io.jans.model.metric.ldap.MetricEntry;

/**
 * Declaration of a single metric type. Applications which use the metric
 * service should declare their own metric types (typically as an enum)
 * implementing this interface instead of extending the shared
 * {@link MetricType} enum.
 *
 * @author Yuriy Movchan Date: 08/04/2026
 */
public interface MetricTypeDeclaration {

    /**
     * Unique metric name. This value is stored in the jansMetricTyp attribute
     * and used as the metric registry name.
     */
    String getValue();

    /**
     * Human readable metric description.
     */
    String getDisplayName();

    /**
     * Type which holds collected metric data of this metric type.
     */
    Class<? extends MetricData> getEventDataType();

    /**
     * Persistence entry type used to store snapshots of this metric type.
     */
    Class<? extends MetricEntry> getMetricEntryType();

    default String getMetricName() {
        return getValue();
    }

}
