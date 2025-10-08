
/*
 * Copyright [2025] [Janssen Project]
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

package io.jans.lock.cedarling.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import io.jans.lock.cedarling.config.BootstrapConfig;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.cedarling.CedarlingConfiguration;
import io.jans.lock.model.config.cedarling.CedarlingPolicyConfiguration;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import uniffi.cedarling_uniffi.AuthorizeResult;
import uniffi.cedarling_uniffi.CedarlingException;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
@ApplicationScoped
public class CedarlingAuthorizationService {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private CedarlingPolicyConfiguration policyConfiguration;	

	private CedarlingAdapter cedarlingAdapter;
	private boolean initialized = false;

	@PostConstruct
	public void init() {
		log.info("Initialising Cedarling service");

		CedarlingConfiguration cedarConf = appConfiguration.getCedarlingConfiguration();
		if (cedarConf.isEnabled()) {
			this.cedarlingAdapter = initAdapter(cedarConf);
			initialized = true;
		} else {
			log.info("Cedarling is disabled");
		}
	}

	@PreDestroy
	public void destroy() {
		log.info("Destroying Cedarling service");
		
		if (initialized) {
			this.cedarlingAdapter.close();
		}

		log.info("Cedarling is destroyed");
	}

	/*
	 * Subscribe for application initialization event
	 */
	public void initEvent(@Observes @ApplicationInitialized(ApplicationScoped.class) ApplicationInitializedEvent event) {}

	private CedarlingAdapter initAdapter(CedarlingConfiguration cedarConf) {
		// Prepare Cedarling configuration
		BootstrapConfig config = BootstrapConfig.builder().applicationName("Lock Server")
				.policyStoreLocal(policyConfiguration.getPolicy()).userAuthz(false).workloadAuthz(true)
				.logType(cedarConf.getLogType()).logLevel(cedarConf.getLogLevel()).build();

		try {
			// Initialize the Cedarling instance
			CedarlingAdapter cedarlingAdapter = new CedarlingAdapter();

			String jsonConfig = config.toJsonConfig();
			if (log.isTraceEnabled()) {
				log.trace("Cedarling JSON configuration: {}", jsonConfig);
			}

			cedarlingAdapter.loadFromJson(jsonConfig);

			log.info("Cedarling initialized successfully");
			return cedarlingAdapter;
		} catch (CedarlingException ex) {
			log.error("Failed to initialize Cedarling!", ex);
			log.trace("Configuration: {}", config.toJsonConfig());
		}

		return null;
	}

	public boolean authorize(Map<String, String> tokens, String action, Map<String, Object> resource, Map<String, Object> context) {
		JSONObject resourceObject = new JSONObject(Optional.ofNullable(
				resource).map(Map.class::cast).orElse(Collections.emptyMap()));

		JSONObject contextObject = new JSONObject(Optional.ofNullable(
				context).map(Map.class::cast).orElse(Collections.emptyMap()));

		return authorize(tokens, action, resourceObject, contextObject);
	}
	
	public boolean authorize(Map<String, String> tokens, String action, JSONObject resource, JSONObject context) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Before executing authorization request. tokens: {}, action: {}, resource: {}, context: {}",
						tokens, action, resource, context);
			}
			AuthorizeResult res = cedarlingAdapter.authorize(tokens, action, resource, context);
			
			if (res == null) {
				log.error("Authorization response is empty for request with tokens: {}, action: {}, resource: {}, context: {}",
						tokens, action, resource, context);
				return false;
			}

			String requestId = res.getRequestId();
			if (log.isDebugEnabled()) {
				log.debug("Authorization workload decision {} for requestId {}, tokens: {}, action: {}, resource: {}, context: {}",
						res.getDecision(), requestId, tokens, action, resource, context);
			}

			return res.getDecision();
		} catch (Exception ex) {
			log.error("Failed to execute Cedarling authorize: tokens: {}, action: {}, resource: {}, context: {}", tokens, action, resource, context, ex);
		}
		
		return false;
	}

}
