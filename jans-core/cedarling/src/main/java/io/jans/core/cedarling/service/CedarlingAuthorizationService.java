/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.core.cedarling.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import io.jans.core.cedarling.config.BootstrapConfig;
import io.jans.core.cedarling.model.CedarlingConfiguration;
import io.jans.core.cedarling.service.policy.CedarlingPolicyStoreFileProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uniffi.cedarling_uniffi.CedarlingException;
import uniffi.cedarling_uniffi.MultiIssuerAuthorizeResult;

/**
 * @author Yuriy Movchan Date: 10/08/2022
 */
@ApplicationScoped
public class CedarlingAuthorizationService {

	public static final String CEDARLING_JANS_ACCESS_TOKEN = "Jans::Access_token";

	@Inject
	private Logger log;

	@Inject
	private CedarlingConfiguration cedarConf;

	@Inject
	private CedarlingPolicyStoreFileProvider cedarlingPolicyStoreFileProvider;

	private CedarlingAdapter cedarlingAdapter;
	private boolean initialized = false;

	@PostConstruct
	public void init() {
		log.info("Initialising Cedarling service");

		if (cedarConf.isEnabled()) {
			cedarlingPolicyStoreFileProvider.prepare();
			try {
				this.cedarlingAdapter = initAdapter(cedarConf);
			} finally {
				cedarlingPolicyStoreFileProvider.cleanup();
			}

			initialized = this.cedarlingAdapter != null;
			if (!initialized) {
				log.error("Cedarling initialization failed, authorization requests will be denied until this is resolved");
			}
		} else {
			log.info("Cedarling was disabled");
		}
	}

	@PreDestroy
	public void destroy() {
		log.info("Destroying Cedarling service");

		if (initialized && cedarlingAdapter != null) {
			this.cedarlingAdapter.close();
		}

		log.info("Cedarling is destroyed");
	}

	private CedarlingAdapter initAdapter(CedarlingConfiguration cedarConf) {
	    BootstrapConfig config = prepareBootstrapConfig(cedarConf);

	    CedarlingAdapter initCedarlingAdapter = null;
	    try {
	        initCedarlingAdapter = new CedarlingAdapter();
	        String jsonConfig = config.toJsonConfig();
	        if (log.isTraceEnabled()) {
	            log.trace("Cedarling JSON configuration: {}", jsonConfig);
	        }
	        initCedarlingAdapter.loadFromJson(jsonConfig);
	        log.info("Cedarling initialized successfully with trusted issuers count: {}", initCedarlingAdapter.loadedTrustedIssuersCount());
	        return initCedarlingAdapter;
	    } catch (CedarlingException ex) {
	        log.error("Failed to initialize Cedarling!", ex);
	        log.trace("Configuration: {}", config.toJsonConfig());
	        
	        // Close adapter if initialization failed
	        if (initCedarlingAdapter != null) {
	            try {
	                initCedarlingAdapter.close();
	            } catch (Exception closeEx) {
	                log.warn("Failed to close Cedarling adapter after initialization failure", closeEx);
	            }
	        }
	    }

	    return null;
	}

	protected BootstrapConfig prepareBootstrapConfig(CedarlingConfiguration cedarConf) {
		BootstrapConfig config = BootstrapConfig.builder()
	        .applicationName("Lock Server")
	        .policyStoreLocalFn(cedarlingPolicyStoreFileProvider.getPolicyStorePath())
	        .jwtStatusValidation(false)
	        .jwtSigValidation(false)
	        .logType(cedarConf.getLogType())
	        .logLevel(cedarConf.getLogLevel())
	        .build();
		return config;
	}

	public boolean authorize(Map<String, String> tokens, String action, Map<String, Object> resource, Map<String, Object> context) {
		JSONObject resourceObject = new JSONObject(Optional.ofNullable(
				resource).map(Map.class::cast).orElse(Collections.emptyMap()));

		JSONObject contextObject = new JSONObject(Optional.ofNullable(
				context).map(Map.class::cast).orElse(Collections.emptyMap()));

		return authorize(tokens, action, resourceObject, contextObject);
	}

	public boolean authorize(Map<String, String> tokens, String action, JSONObject resource, JSONObject context) {
		if (!initialized || cedarlingAdapter == null) {
			log.error("Cedarling is not initialized. Failed to execute Cedarling authorize: tokens: {}, action: {}, resource: {}, context: {}",
					tokens, action, resource, context);
			return false;
		}

		try {
			if (log.isDebugEnabled()) {
				log.debug("Before executing authorization request. tokens: {}, action: {}, resource: {}, context: {}",
						tokens, action, resource, context);
			}

			if (!tokens.containsKey(CEDARLING_JANS_ACCESS_TOKEN)) {
				log.error("Missing token '{}' in tokens map. Failed to execute Cedarling authorize", CEDARLING_JANS_ACCESS_TOKEN);
				return false;
			}

			MultiIssuerAuthorizeResult res = cedarlingAdapter.authorizeMultiIssuer(tokens, action, resource, context);
			if (res == null) {
				log.error("Authorization response is empty for request with tokens: {}, action: {}, resource: {}, context: {}",
						tokens, action, resource, context);
				return false;
			}

			String requestId = res.getRequestId();
			if (res.getDecision()) {
				log.debug("Authorization decision is PERMIT for requestId {}, tokens: {}, action: {}, resource: {}, context: {}",
						requestId, tokens, action, resource, context);
			} else {
				log.debug("Authorization decision is DENY for requestId {}, tokens: {}, action: {}, resource: {}, context: {}",
						requestId, tokens, action, resource, context);
			}

			return res.getDecision();
		} catch (Exception ex) {
			log.error("Failed to execute Cedarling authorize: tokens: {}, action: {}, resource: {}, context: {}", tokens, action, resource, context, ex);
		}
		
		return false;
	}

}
