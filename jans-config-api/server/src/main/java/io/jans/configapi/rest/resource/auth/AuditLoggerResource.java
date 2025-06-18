package io.jans.configapi.rest.resource.auth;


import io.jans.ads.model.Deployment;
import io.jans.configapi.core.util.ApiErrorResponse;
import io.jans.config.GluuConfiguration;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.auth.AgamaDeploymentsService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.LdapConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.model.PagedResult;
import io.jans.service.custom.CustomScriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

public class AuditLoggerResource {
    
    public static final String AUDIT_LOGGING_WRITE_SCOPE = "https://jans.io/oauth/jans-auth-server/config/adminui/logging.write";
    static final String AUDIT = "/audit";

   
    @Inject
    Logger log;
    
    @POST
    @ProtectedApi(scopes = {AUDIT_LOGGING_WRITE_SCOPE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response auditLogging(@Valid @NotNull Map<String, Object> loggingRequest) {
        try {
            log.info(loggingRequest.toString());
            return Response.ok("{'status': 'success'}").build();
        } catch (Exception e) {
            log.error(ApiErrorResponse.AUDIT_LOGGING_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(createGenericResponse(false, 500, ApiErrorResponse.AUDIT_LOGGING_ERROR.getDescription()))
                    .build();
        }
        
        public static GenericResponse createGenericResponse(boolean result, int responseCode, String responseMessage, JsonNode node) {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setResponseCode(responseCode);
            genericResponse.setResponseMessage(responseMessage);
            genericResponse.setSuccess(result);
            genericResponse.setResponseObject(node);
            return genericResponse;
        }
}
