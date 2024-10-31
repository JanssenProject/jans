/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs.controller;

import org.apache.commons.lang3.StringUtils;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.service.external.ExternalHealthCheckService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.util.StringHelper;
import io.jans.service.HealthCheckPluginService;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * Health check controller
 *
 * @author Yuriy Movchan
 * @version Jul 24, 2020
 */
@ApplicationScoped
@Path("/")
public class HealthCheckController {

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private ExternalDynamicScopeService externalDynamicScopeService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ExternalHealthCheckService externalHealthCheckService;

    @Inject
    private CustomScriptManager сustomScriptManager;

    @Inject
    private Instance<HealthCheckPluginService> healthCheckPluginServiceInstance;

    @GET
    @POST
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    public String healthCheckController(@Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.HEALTH_CHECK);
        boolean isConnected = persistenceEntryManager.getOperationService().isConnected();
        String dbStatus = isConnected ? "online" : "offline";
    	String appStatus = getAppStatus();

    	StringBuilder sb = new StringBuilder(String.format("{\"status\": \"%s\", \"db_status\":\"%s\"", appStatus, dbStatus));
    	for (HealthCheckPluginService healthCheckPluginService : healthCheckPluginServiceInstance) {
    		String healthCheckService = healthCheckPluginService.provideServiceName();
    		String healthCheckData = healthCheckPluginService.provideHealthCheckData();
    		if (StringHelper.isNotEmpty(healthCheckService) && StringHelper.isNotEmpty(healthCheckData)) {
        		sb.append(String.format(", \"%s\": %s", healthCheckService, healthCheckData));
    		}
    	}
    	
    	sb.append("}");
    	
    	final String responseString = sb.toString();

        if (externalHealthCheckService.isEnabled()) {
            final ExecutionContext executionContext = new ExecutionContext(httpRequest, httpResponse);
            final String externalResponse = externalHealthCheckService.externalHealthCheck(executionContext);
            if (StringUtils.isNotBlank(externalResponse)) {
                return externalResponse;
            }

        }

        return responseString;
    }

    public String getAppStatus() {
        if (externalAuthenticationService.isLoaded() && externalDynamicScopeService.isLoaded() && сustomScriptManager.isInitialized()) {
        	return "running";
        } else {
        	return "starting";
        }
    }
}
