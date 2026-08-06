/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.metric;

import java.io.Serializable;
import java.util.Objects;

import com.codahale.metrics.MetricRegistry;

import io.jans.model.metric.MetricTypeDeclaration;

/**
 * Represents a single metric registered in the metric registry: a metric type
 * declaration with an optional sub type. The registry name combines both and
 * is used as the Dropwizard metric registry key.
 *
 * @author Yuriy Movchan Date: 08/04/2026
 */
public class MetricRegistration implements Serializable {

    private static final long serialVersionUID = 2854563137763828896L;

    private final MetricTypeDeclaration metricType;
    private final String metricSubType;
    private final String registryName;

    public MetricRegistration(MetricTypeDeclaration metricType, String metricSubType) {
        this.metricType = metricType;
        this.metricSubType = metricSubType;
        this.registryName = MetricRegistry.name(metricType.getMetricName(), metricSubType);
    }

    public MetricTypeDeclaration getMetricType() {
        return metricType;
    }

    public String getMetricSubType() {
        return metricSubType;
    }

    public String getRegistryName() {
        return registryName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return Objects.equals(registryName, ((MetricRegistration) obj).registryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName);
    }

    @Override
    public String toString() {
        return "MetricRegistration [metricType=" + metricType.getValue() + ", metricSubType=" + metricSubType
                + ", registryName=" + registryName + "]";
    }

}
