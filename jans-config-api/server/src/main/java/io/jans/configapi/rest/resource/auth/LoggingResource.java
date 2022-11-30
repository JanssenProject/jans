/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.model.Logging;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.LOGGING)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoggingResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Operation(summary = "Returns Jans Authorization Server logging settings", description = "Returns Jans Authorization Server logging settings", operationId = "get-config-logging", tags = {
            "Configuration – Logging" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.LOGGING_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Logging.class) , examples = @ExampleObject(name = "Response json example", value = "example/logging/logging.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.LOGGING_READ_ACCESS } , groupScopes = {
            ApiAccessConstants.LOGGING_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getLogging() {
        return Response.ok(this.getLoggingConfiguration()).build();
    }

    @Operation(summary = "Updates Jans Authorization Server logging settings", description = "Updates Jans Authorization Server logging settings", operationId = "put-config-logging", tags = {
            "Configuration – Logging" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.LOGGING_WRITE_ACCESS }))
    @RequestBody(description = "Logging object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Logging.class) , examples = @ExampleObject(name = "Request json example", value = "example/logging/logging.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Logging.class) , examples = @ExampleObject(name = "Response json example", value = "example/logging/logging.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.LOGGING_WRITE_ACCESS }, groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateLogConf(@Valid Logging logging) {
        log.debug("LOGGING configuration to be updated -logging:{}", logging);
        Conf conf = configurationService.findConf();

        if (!StringUtils.isBlank(logging.getLoggingLevel())) {
            conf.getDynamic().setLoggingLevel(logging.getLoggingLevel());
        }
        if (!StringUtils.isBlank(logging.getLoggingLayout())) {
            conf.getDynamic().setLoggingLayout(logging.getLoggingLayout());
        }

        conf.getDynamic().setHttpLoggingEnabled(logging.isHttpLoggingEnabled());
        conf.getDynamic().setDisableJdkLogger(logging.isDisableJdkLogger());
        conf.getDynamic().setEnabledOAuthAuditLogging(logging.isEnabledOAuthAuditLogging());

        if (!StringUtils.isBlank(logging.getExternalLoggerConfiguration())) {
            conf.getDynamic().setExternalLoggerConfiguration(logging.getExternalLoggerConfiguration());
        }
        conf.getDynamic().setHttpLoggingExcludePaths(logging.getHttpLoggingExcludePaths());

        configurationService.merge(conf);

        logging = this.getLoggingConfiguration();
        return Response.ok(logging).build();
    }

    private Logging getLoggingConfiguration() {
        Logging logging = new Logging();
        AppConfiguration appConfiguration = configurationService.find();

        logging.setLoggingLevel(appConfiguration.getLoggingLevel());
        logging.setLoggingLayout(appConfiguration.getLoggingLayout());
        logging.setHttpLoggingEnabled(appConfiguration.getHttpLoggingEnabled());
        logging.setDisableJdkLogger(appConfiguration.getDisableJdkLogger());
        if (appConfiguration.getEnabledOAuthAuditLogging() == null) {
            logging.setEnabledOAuthAuditLogging(false);
        } else {
            logging.setEnabledOAuthAuditLogging(appConfiguration.getEnabledOAuthAuditLogging());
        }
        logging.setExternalLoggerConfiguration(appConfiguration.getExternalLoggerConfiguration());
        logging.setHttpLoggingExcludePaths(appConfiguration.getHttpLoggingExcludePaths());
        return logging;
    }

}
