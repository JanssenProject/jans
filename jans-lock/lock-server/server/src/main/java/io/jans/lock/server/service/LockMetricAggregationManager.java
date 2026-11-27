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

import io.jans.lock.model.config.AppConfiguration;
import io.jans.model.metric.aggregation.MetricAggregationConfiguration;
import io.jans.service.cluster.ClusterLockService;
import io.jans.service.metric.aggregation.MetricAggregationManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Builds the {@code jansMetricAggregation} rows of the Lock application type from its raw
 * {@code jansMetric} rows.
 *
 * <p>The Lock server does not start its metric reporter, so it writes no raw rows, although
 * {@code metricReporterEnabled} can be true in the configuration. Aggregation therefore stays idle (no claim
 * rows, no queries) until {@link MetricService#initTimer()} is called, see {@link #isEnabled()}.</p>
 */
@ApplicationScoped
public class LockMetricAggregationManager extends MetricAggregationManager {

	@Inject
	private MetricService metricService;

	@Inject
	private LockClusterLockService clusterLockService;

	@Inject
	private AppConfiguration appConfiguration;

	@Override
	public boolean isEnabled() {
		return metricService.isReporterStarted() && super.isEnabled();
	}

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
