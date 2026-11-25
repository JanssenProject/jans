/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.metric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.cluster.Fido2ClusterLockService;
import io.jans.fido2.service.cluster.Fido2ClusterNodeService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.model.ApplicationType;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.service.metric.aggregation.MetricAggregationManager;

class Fido2MetricAggregationWiringTest {

	private static void set(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	void lockServiceUsesNodeBaseDnAndFido2LockKey() throws Exception {
		BaseDnConfiguration baseDn = new BaseDnConfiguration();
		baseDn.setNode("ou=node,o=jans");
		StaticConfiguration staticConfiguration = new StaticConfiguration();
		staticConfiguration.setBaseDn(baseDn);
		MetricService metricService = mock(MetricService.class);
		when(metricService.getNodeIdentifier()).thenReturn("00:11:22:33:44:55");

		Fido2ClusterLockService service = new Fido2ClusterLockService();
		set(service, "staticConfiguration", staticConfiguration);
		set(service, "metricService", metricService);

		assertEquals("ou=node,o=jans", service.getBaseDn());
		assertEquals(Fido2ClusterNodeService.LOCK_KEY, service.getLockKey());
		assertEquals("00:11:22:33:44:55", service.getNodeIdentifier());
	}

	private static Fido2ClusterLockService lockService(String node, String configuration) throws Exception {
		BaseDnConfiguration baseDn = new BaseDnConfiguration();
		baseDn.setNode(node);
		baseDn.setConfiguration(configuration);
		StaticConfiguration staticConfiguration = new StaticConfiguration();
		staticConfiguration.setBaseDn(baseDn);
		Fido2ClusterLockService service = new Fido2ClusterLockService();
		set(service, "staticConfiguration", staticConfiguration);
		return service;
	}

	private static void assertClaimDn(String expectedBase, Fido2ClusterLockService service) {
		String task = MetricAggregationManager.TASK_PREFIX + ApplicationType.FIDO2.getValue();
		assertEquals(expectedBase, service.getBaseDn());
		assertEquals("jansId=" + task + "_123," + expectedBase, service.buildClaimDn(task, "123"));
	}

	@Test
	void baseDnUsesNodeWhenSet() throws Exception {
		assertClaimDn("ou=custom,o=jans", lockService("ou=custom,o=jans", "ou=configuration,o=jans"));
	}

	@Test
	void baseDnIgnoresBlankNode() throws Exception {
		assertClaimDn("ou=node,o=jans", lockService("  ", "ou=configuration,o=jans"));
	}

	@Test
	void baseDnDerivedFromConfigurationWhenNodeMissing() throws Exception {
		assertClaimDn("ou=node,o=jans", lockService(null, "ou=configuration,o=jans"));
		assertClaimDn("ou=node,o=other", lockService(null, "ou=configuration,o=other"));
	}

	@Test
	void baseDnFallsBackForUnusualConfigurationDn() throws Exception {
		assertClaimDn("ou=node,o=jans", lockService(null, "ou=cfg,o=other"));
		assertClaimDn("ou=node,o=jans", lockService(null, "ou=configuration"));
	}

	@Test
	void baseDnFallsBackWhenNodeAndConfigurationMissing() throws Exception {
		assertClaimDn("ou=node,o=jans", lockService(null, null));
	}

	@Test
	void managerReadsHooksFromAppConfigurationAndGatesOnTheReporter() throws Exception {
		AppConfiguration appConfiguration = new AppConfiguration();
		appConfiguration.setMetricReporterEnabled(true);
		appConfiguration.setMetricReporterInterval(120);
		appConfiguration.setMetricReporterKeepDataDays(7);
		MetricService metricService = mock(MetricService.class);
		when(metricService.isMetricReporterEnabled()).thenReturn(true);
		Fido2ClusterLockService lockService = mock(Fido2ClusterLockService.class);

		Fido2MetricAggregationManager manager = new Fido2MetricAggregationManager();
		set(manager, "metricService", metricService);
		set(manager, "clusterLockService", lockService);
		set(manager, "appConfiguration", appConfiguration);

		assertSame(metricService, manager.getMetricService());
		assertSame(lockService, manager.getClusterLockService());
		assertEquals(120, manager.getMetricReporterIntervalSeconds());
		assertEquals(7, manager.getMetricReporterKeepDataDays());
		assertNull(manager.getAggregationConfiguration());
		assertTrue(manager.isEnabled());

		MetricAggregationConfiguration configuration = new MetricAggregationConfiguration();
		configuration.setEnabled(false);
		appConfiguration.setMetricAggregationConfiguration(configuration);
		assertFalse(manager.isEnabled());

		appConfiguration.setMetricAggregationConfiguration(null);
		when(metricService.isMetricReporterEnabled()).thenReturn(false);
		assertFalse(manager.isEnabled());
	}
}
