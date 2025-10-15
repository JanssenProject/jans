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

package io.jans.lock.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.service.net.NetworkService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

/**
 * Lock metadata configuration
 *
 * @author Yuriy Movchan Date: 12/19/2018
 */
@ApplicationScoped
@Path("/configuration")
public class ConfigurationService {

    @Inject
	private AppConfiguration appConfiguration;
    
    @Inject
    private NetworkService networkService;

    private ObjectMapper objectMapper;

	@PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }
    
	public ObjectNode getLockConfiguration() {
	    final String baseEndpointUri = appConfiguration.getBaseEndpoint();
	    ObjectNode response = objectMapper.createObjectNode();
        
        response.put("version", "1.0");
        response.put("issuer", networkService.getHost(baseEndpointUri));
        
        ObjectNode audit = objectMapper.createObjectNode();
        response.set("audit", audit);
        audit.put("health_endpoint", baseEndpointUri + "/audit/health");
        audit.put("log_endpoint", baseEndpointUri + "/audit/log");
        audit.put("telemetry_endpoint", baseEndpointUri + "/audit/telemetry");

        ObjectNode config = objectMapper.createObjectNode();
        config.put("policy_endpoint", baseEndpointUri + "/policy");

        config.put("sse_endpoint", baseEndpointUri + "/sse");

        return response;
	}

}
