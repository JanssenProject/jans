/*
 * Copyright [2024] [Janssen Project]
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
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.model.ApplicationType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.metric.inject.ReportMetric;
import io.jans.service.net.NetworkService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Store and retrieve metric
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
@Named(MetricService.METRIC_SERVICE_COMPONENT_NAME)
public class MetricService extends io.jans.service.metric.MetricService {

	public static final String METRIC_SERVICE_COMPONENT_NAME = "metricService";

	public static final String PERSISTENCE_METRIC_ENTRY_MANAGER_NAME = "persistenceMetricEntryManager";
    
    public static final String PERSISTENCE_METRIC_CONFIG_GROUP_NAME = "metric";

	private static final long serialVersionUID = 7875838160379126796L;

	@Inject
	private Instance<MetricService> instance;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private NetworkService networkService;

	@Inject
	@Named(MetricService.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME)
	@ReportMetric
	private PersistenceEntryManager persistenceEntryManager;

	public void initTimer() {
		initTimer(this.appConfiguration.getMetricReporterInterval(),
				this.appConfiguration.getMetricReporterKeepDataDays());
	}

	@Override
	public String baseDn() {
		return staticConfiguration.getBaseDn().getMetric();
	}

	public io.jans.service.metric.MetricService getMetricServiceInstance() {
		return instance.get();
	}

	@Override
	public boolean isMetricReporterEnabled() {
		return this.appConfiguration.getMetricReporterEnabled();
	}

	@Override
	public ApplicationType getApplicationType() {
		return ApplicationType.JANS_LOCK;
	}

	@Override
	public PersistenceEntryManager getEntryManager() {
		return persistenceEntryManager;
	}

	@Override
	public String getNodeIndetifier() {
		return networkService.getMacAdress();
	}

}