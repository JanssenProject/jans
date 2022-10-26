/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.register.ws.rs.action;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.register.ws.rs.RegisterJsonService;
import io.jans.as.server.register.ws.rs.RegisterService;
import io.jans.as.server.register.ws.rs.RegisterValidator;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.security.StringEncrypter;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class RegisterReadAction {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ClientService clientService;

    @Inject
    private TokenService tokenService;

    @Inject
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Inject
    private RegisterParamsValidator registerParamsValidator;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private RegisterValidator registerValidator;

    @Inject
    private RegisterService registerService;

    @Inject
    private RegisterJsonService registerJsonService;

    public Response readClient(String clientId, String authorization, HttpServletRequest httpRequest,
                               SecurityContext securityContext) {
        String accessToken = tokenService.getToken(authorization);
        log.debug("Attempting to read client: clientId = {}, registrationAccessToken = {} isSecure = {}",
                clientId, accessToken, securityContext.isSecure());

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.REGISTRATION);

        Response.ResponseBuilder builder = Response.ok();

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_READ);
        oAuth2AuditLog.setClientId(clientId);
        try {
            if (registerParamsValidator.validateParamsClientRead(clientId, accessToken)) {
                if (isTrue(appConfiguration.getDcrAuthorizationWithClientCredentials())) {
                    registerValidator.validateAuthorizationAccessToken(accessToken, clientId);
                }

                Client client = clientService.getClient(clientId, accessToken);
                if (client != null) {
                    oAuth2AuditLog.setScope(registerService.clientScopesToString(client));
                    oAuth2AuditLog.setSuccess(true);

                    JSONObject jsonObject = registerJsonService.getJSONObject(client);
                    jsonObject = modifyReadScript(jsonObject, new ExecutionContext(httpRequest, null).setClient(client));
                    builder.entity(registerJsonService.jsonObjectToString(jsonObject));
                } else {
                    log.trace("The Access Token is not valid for the Client ID, returns invalid_token error.");
                    builder = Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).type(MediaType.APPLICATION_JSON_TYPE);
                    builder.entity(errorResponseFactory.errorAsJson(RegisterErrorResponseType.INVALID_TOKEN, "The Access Token is not valid for the Client"));
                }
            } else {
                log.trace("Client ID or Access Token is not valid.");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "Client ID or Access Token is not valid.");
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "Failed to parse json.");
        } catch (StringEncrypter.EncryptionException e) {
            log.error(e.getMessage(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "Encryption exception occurred.");
        }

        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header(Constants.PRAGMA, Constants.NO_CACHE);
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    private JSONObject modifyReadScript(JSONObject jsonObject, ExecutionContext executionContext) throws StringEncrypter.EncryptionException {
        if (!externalDynamicClientRegistrationService.modifyReadResponse(jsonObject, executionContext)) {
            return registerJsonService.getJSONObject(executionContext.getClient()); // script forbids modification, re-create json object
        }
        return jsonObject;
    }
}
