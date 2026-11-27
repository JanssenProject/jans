/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.cluster.Fido2ClusterLockService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.service.cluster.ClusterLockService;
import io.jans.service.metric.aggregation.MetricAggregationManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Builds the {@code jansMetricAggregation} rows of the FIDO2 application type from its raw
 * {@code jansMetric} rows. This is not {@link Fido2MetricsAggregationScheduler}, which writes
 * {@code jansFido2MetricsAggregation}.
 */
@ApplicationScoped
public class Fido2MetricAggregationManager extends MetricAggregationManager {

    @Inject
    private MetricService metricService;

    @Inject
    private Fido2ClusterLockService clusterLockService;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    protected MetricService getMetricService() {
        return metricService;
    }

    @Override
    protected ClusterLockService getClusterLockService() {
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
