/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.metric;

import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.service.cluster.ClusterLockService;
import io.jans.service.metric.MetricService;
import io.jans.service.metric.aggregation.MetricAggregationManager;

/**
 * {@link MetricAggregationManager} with fixed hooks, for tests.
 */
public class TestMetricAggregationManager extends MetricAggregationManager {

    private final MetricService metricService;
    private final ClusterLockService clusterLockService;
    private volatile MetricAggregationConfiguration configuration;
    private volatile int reporterIntervalSeconds;
    private volatile int keepDataDays;

    public TestMetricAggregationManager(MetricService metricService, ClusterLockService clusterLockService,
            MetricAggregationConfiguration configuration, int reporterIntervalSeconds, int keepDataDays) {
        this.metricService = metricService;
        this.clusterLockService = clusterLockService;
        this.configuration = configuration;
        this.reporterIntervalSeconds = reporterIntervalSeconds;
        this.keepDataDays = keepDataDays;
    }

    public void setConfiguration(MetricAggregationConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setKeepDataDays(int keepDataDays) {
        this.keepDataDays = keepDataDays;
    }

    @Override
    public MetricService getMetricService() {
        return metricService;
    }

    @Override
    public ClusterLockService getClusterLockService() {
        return clusterLockService;
    }

    @Override
    protected MetricAggregationConfiguration getAggregationConfiguration() {
        return configuration;
    }

    @Override
    protected int getMetricReporterIntervalSeconds() {
        return reporterIntervalSeconds;
    }

    @Override
    protected int getMetricReporterKeepDataDays() {
        return keepDataDays;
    }

}
