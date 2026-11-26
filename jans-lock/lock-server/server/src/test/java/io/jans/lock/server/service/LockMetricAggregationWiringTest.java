/*
 * Copyright [2026] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.BaseDnConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.service.metric.aggregation.MetricAggregationRunResult;

class LockMetricAggregationWiringTest {

	private static void set(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static LockClusterLockService lockService(String configurationDn) throws Exception {
		BaseDnConfiguration baseDn = new BaseDnConfiguration();
		baseDn.setConfiguration(configurationDn);
		StaticConfiguration staticConfiguration = new StaticConfiguration();
		staticConfiguration.setBaseDn(baseDn);

		LockClusterLockService service = new LockClusterLockService();
		set(service, "staticConfiguration", staticConfiguration);
		return service;
	}

	@Test
	void nodeDnIsDerivedFromConfigurationDn() throws Exception {
		assertEquals("ou=node,o=jans", lockService("ou=configuration,o=jans").getBaseDn());
		assertEquals("ou=node,o=example", lockService("ou=configuration,o=example").getBaseDn());
		assertEquals("ou=node,o=jans", lockService(null).getBaseDn());
		assertEquals("ou=node,o=jans", lockService("ou=other,o=jans").getBaseDn());
	}

	@Test
	void lockKeyIsStablePerInstanceAndDiffersBetweenInstances() throws Exception {
		LockClusterLockService first = lockService("ou=configuration,o=jans");
		LockClusterLockService second = lockService("ou=configuration,o=jans");
		assertEquals(first.getLockKey(), first.getLockKey());
		assertNotEquals(first.getLockKey(), second.getLockKey());
	}

	@Test
	void reporterFlagDefaultsToDisabledWhenMissing() throws Exception {
		AppConfiguration configuration = new AppConfiguration();
		MetricService service = new MetricService();
		set(service, "appConfiguration", configuration);

		assertFalse(service.isMetricReporterEnabled());
		configuration.setMetricReporterEnabled(true);
		assertTrue(service.isMetricReporterEnabled());
		configuration.setMetricReporterEnabled(false);
		assertFalse(service.isMetricReporterEnabled());
	}

	@Test
	void aggregationStaysIdleWhileTheReporterIsNotStarted() throws Exception {
		MetricService metricService = mock(MetricService.class);
		when(metricService.isMetricReporterEnabled()).thenReturn(true);
		when(metricService.isReporterStarted()).thenReturn(false);
		LockClusterLockService lockService = mock(LockClusterLockService.class);

		AppConfiguration appConfiguration = new AppConfiguration();
		appConfiguration.setMetricReporterEnabled(true);

		LockMetricAggregationManager manager = new LockMetricAggregationManager();
		set(manager, "metricService", metricService);
		set(manager, "clusterLockService", lockService);
		set(manager, "appConfiguration", appConfiguration);

		assertFalse(manager.isEnabled());
		assertEquals(MetricAggregationRunResult.Status.DISABLED, manager.runOnce().getStatus());
		verifyNoInteractions(lockService);
	}

	@Test
	void aggregationIsEnabledOnceTheReporterRuns() throws Exception {
		MetricService metricService = mock(MetricService.class);
		when(metricService.isMetricReporterEnabled()).thenReturn(true);
		when(metricService.isReporterStarted()).thenReturn(true);

		AppConfiguration appConfiguration = new AppConfiguration();
		appConfiguration.setMetricReporterEnabled(true);

		LockMetricAggregationManager manager = new LockMetricAggregationManager();
		set(manager, "metricService", metricService);
		set(manager, "clusterLockService", mock(LockClusterLockService.class));
		set(manager, "appConfiguration", appConfiguration);
		assertTrue(manager.isEnabled());

		MetricAggregationConfiguration configuration = new MetricAggregationConfiguration();
		configuration.setEnabled(false);
		appConfiguration.setMetricAggregationConfiguration(configuration);
		assertFalse(manager.isEnabled());
	}
}
