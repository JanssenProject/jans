/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.ClusterLockService;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the hooks of the jans-auth metric aggregation classes without a CDI container.
 */
public class MetricAggregationWiringTest {

    @Test
    public void managerHooksComeFromAppConfiguration() throws Exception {
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setMetricReporterInterval(120);
        appConfiguration.setMetricReporterKeepDataDays(9);
        MetricAggregationConfiguration aggregationConfiguration = new MetricAggregationConfiguration();
        appConfiguration.setMetricAggregationConfiguration(aggregationConfiguration);

        ClusterLockService lockService = new ClusterLockService();
        MetricAggregationManager manager = newManager(appConfiguration, lockService, true);

        assertEquals(120, manager.getMetricReporterIntervalSeconds());
        assertEquals(9, manager.getMetricReporterKeepDataDays());
        assertSame(aggregationConfiguration, manager.getAggregationConfiguration());
        assertSame(lockService, manager.getClusterLockService());
        assertTrue(manager.isEnabled());
    }

    @Test
    public void managerWithoutConfigurationBlockUsesDefaultsAndFollowsTheReporterSwitch() throws Exception {
        AppConfiguration appConfiguration = new AppConfiguration();

        MetricAggregationManager enabled = newManager(appConfiguration, new ClusterLockService(), true);
        assertNull(enabled.getAggregationConfiguration());
        assertTrue(enabled.isEnabled());

        MetricAggregationManager disabled = newManager(appConfiguration, new ClusterLockService(), false);
        assertFalse(disabled.isEnabled());
    }

    private static TestMetricService newMetricService(boolean reporterEnabled) {
        return new TestMetricService(reporterEnabled);
    }

    private static MetricAggregationManager newManager(AppConfiguration appConfiguration, ClusterLockService lockService,
                                                       boolean reporterEnabled) throws Exception {
        MetricAggregationManager manager = new MetricAggregationManager();
        set(manager, "appConfiguration", appConfiguration);
        set(manager, "metricService", newMetricService(reporterEnabled));
        set(manager, "clusterLockService", lockService);
        return manager;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestMetricService extends MetricService {

        private final boolean reporterEnabled;

        private TestMetricService(boolean reporterEnabled) {
            this.reporterEnabled = reporterEnabled;
        }

        @Override
        public boolean isMetricReporterEnabled() {
            return reporterEnabled;
        }

        @Override
        public String getNodeIdentifier() {
            return "00:00:5e:00:53:af";
        }
    }
}
