/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterLockService;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Metric aggregation of jans-auth. Reads the raw rows written by the auth {@link MetricService} and writes the
 * hourly, daily, weekly and monthly aggregates.
 */
@ApplicationScoped
public class MetricAggregationManager extends io.jans.service.metric.aggregation.MetricAggregationManager {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private MetricService metricService;

    @Inject
    private ClusterLockService clusterLockService;

    @Override
    protected io.jans.service.metric.MetricService getMetricService() {
        return metricService;
    }

    @Override
    protected io.jans.service.cluster.ClusterLockService getClusterLockService() {
        return clusterLockService;
    }

    @Override
    protected MetricAggregationConfiguration getAggregationConfiguration() {
        return appConfiguration.getMetricAggregationConfiguration();
    }

    @Override
    protected int getMetricReporterIntervalSeconds() {
        return appConfiguration.getMetricReporterInterval();
    }

    @Override
    protected int getMetricReporterKeepDataDays() {
        return appConfiguration.getMetricReporterKeepDataDays();
    }

}
