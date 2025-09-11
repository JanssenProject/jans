
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

package io.jans.lock.cedarling;

import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import io.jans.lock.cedarling.config.BootstrapConfig;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.CedarlingConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import uniffi.cedarling_uniffi.AuthorizeException;
import uniffi.cedarling_uniffi.AuthorizeResult;
import uniffi.cedarling_uniffi.CedarlingException;
import uniffi.cedarling_uniffi.EntityException;

@ApplicationScoped
public class CedarlingAuthorizationService {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	private CedarlingAdapter cedarlingAdapter;

	@PostConstruct
	public void init() {
		log.info("Initialising Cedarling service");

		CedarlingConfiguration cedarConf = appConfiguration.getCedarlingConfiguration();
		if (cedarConf.isEnabled()) {
			this.cedarlingAdapter = initAdapter(cedarConf);
		} else {
			log.info("Cedarling is disabled");
		}
	}

	private CedarlingAdapter initAdapter(CedarlingConfiguration cedarConf) {
		// Prepare Cedarling configuration
		BootstrapConfig config = BootstrapConfig.builder().applicationName("Lock Server")
				.policyStoreLocal(cedarConf.getPolicyStoreLocal()).userAuthz(false).workloadAuthz(true)
				.logType(cedarConf.getLogType()).logLevel(cedarConf.getLogLevel()).build();

		try {
			// Initialize the Cedarling instance
			CedarlingAdapter cedarlingAdapter = new CedarlingAdapter();
			cedarlingAdapter.loadFromJson(config.toJsonConfig());

			log.info("Cedarling initialized successfully");
			return cedarlingAdapter;
		} catch (CedarlingException ex) {
			log.error("Failed to initialize Cedarling!", ex);
		}

		return null;
	}

	public boolean authorize(Map<String, String> tokens, String action, Map<String, Object> resource,
			JSONObject context) throws AuthorizeException, EntityException {

		AuthorizeResult res = cedarlingAdapter.authorize(tokens, action, new JSONObject(resource), context);
		String requestId = res.getRequestId();

		log.debug("Authorization result for requestId: {}, tokens: {}, action: {}, resource: {}, context: {}",
				requestId, tokens, action, resource, context);

		return res.getDecision();
	}

	public Response authorize(ContainerRequestContext requestContext, HttpHeaders httpHeaders,
			ResourceInfo resourceInfo) {
		// TODO Auto-generated method stub
		return null;
	}

}
