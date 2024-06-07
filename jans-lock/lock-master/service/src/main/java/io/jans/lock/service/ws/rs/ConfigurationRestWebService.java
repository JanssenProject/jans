/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.lock.service.ws.rs;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.service.net.NetworkService;

/**
 * Lock metadata configuration
 *
 * @author Yuriy Movchan Date: 12/19/2018
 */
@ApplicationScoped
@Path("/configuration")
public class ConfigurationRestWebService {

    @Inject
	private AppConfiguration appConfiguration;
    
    @Inject
    private NetworkService networkService;

    private ObjectMapper objectMapper;

	@PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }
    
	@GET
	@Produces({ "application/json" })
	public Response getConfiguration() {
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
        response.set("config", config);
        config.put("config_endpoint", baseEndpointUri + "/config");
        config.put("issuers_endpoint", baseEndpointUri + "/config/issuers");
        config.put("policy_endpoint", baseEndpointUri + "/config/policy");
        config.put("schema_endpoint", baseEndpointUri + "/config/schema");

        config.put("sse_endpoint", baseEndpointUri + "/sse");

        ResponseBuilder builder = Response.ok().entity(response.toString());
        return builder.build();
	}

}
