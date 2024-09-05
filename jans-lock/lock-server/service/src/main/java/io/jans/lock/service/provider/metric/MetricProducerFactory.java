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

package io.jans.lock.service.provider.metric;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.qualifier.Implementation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Metric producer factory
 *
 * @author Yuriy Movchan Date: 05/03/2024
 */
@ApplicationScoped
public class MetricProducerFactory {

	@Inject
	private Logger log;

    @Inject
	private AppConfiguration appConfiguration;

	@Inject
	@Implementation
	private Instance<MetricProvider> metricProviderInstances;

	private boolean appStarted = false;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        this.appStarted  = true;
	}

	@Asynchronous
    public void configurationUpdateEvent(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
		if (!appStarted) {
			return;
		}
	}

	@Produces
	@ApplicationScoped
	public MetricProvider produceMetricProvider() {
		return new NullMetricProvider();
	}

}
