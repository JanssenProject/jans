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

package io.jans.lock.service;

import org.slf4j.Logger;

import io.jans.lock.service.config.ConfigurationFactory;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * 
 * Lock services initializer
 *
 * @author Yuriy Movchan Date: 12/18/2023
 */
@ApplicationScoped
public class ServiceInitializer {

	@Inject
	private Logger log;

	@Inject
    private ConfigurationFactory configurationFactory;

	public void applicationInitialized(@Observes ApplicationInitializedEvent applicationInitializedEvent) {
		log.info("Initializing Lock service module services");

		configurationFactory.initTimer();

		log.debug("Initializing Lock service module services complete");
	}

}
