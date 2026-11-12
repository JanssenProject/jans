/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.register.ws.rs.action;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBARegisterParamsValidatorService;
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
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static io.jans.as.model.register.RegisterRequestParam.ACCESS_TOKEN_SIGNING_KID;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_LOGOUT_URI;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNING_KID;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class RegisterUpdateAction {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private TokenService tokenService;

    @Inject
    private RegisterValidator registerValidator;

    @Inject
    private RegisterService registerService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private RegisterParamsValidator registerParamsValidator;

    @Inject
    private CIBARegisterParamsValidatorService cibaRegisterParamsValidatorService;

    @Inject
    private ClientService clientService;

    @Inject
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Inject
    private RegisterJsonService registerJsonService;

    /**
     * @param authorization authorization
     * @param clientId      client id
     * @return returns access token
     */
    private String validateAccessToken(String authorization, String clientId) {
        final String accessToken = tokenService.getToken(authorization);

        registerValidator.validateNotBlank(accessToken, "access token is blank");
        registerValidator.validateAuthorizationAccessToken(accessToken, clientId);
        return accessToken;
    }

    public Response updateClient(String requestParams, String clientId, String authorization, HttpServletRequest httpRequest, SecurityContext securityContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.REGISTRATION);

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_UPDATE);
        oAuth2AuditLog.setClientId(clientId);
        try {
            log.debug("Attempting to UPDATE client, client_id: {}, requestParams = {}, isSecure = {}",
                    clientId, requestParams, securityContext.isSecure());

            registerValidator.validateNotBlank(authorization, "Authorization header is blank");
            registerValidator.validateNotBlank(clientId, "clientId is blank");
            registerValidator.validateNotBlank(requestParams, "requestParams is blank");

            final String accessToken = validateAccessToken(authorization, clientId);

            registerValidator.validateAuthorizationAccessToken(accessToken, clientId);

            final JSONObject requestObject = registerService.parseRequestObjectWithoutValidation(requestParams);
            final JSONObject softwareStatement = registerValidator.validateSoftwareStatement(httpRequest, requestObject);
            if (softwareStatement != null) {
                log.trace("Override request parameters by software_statement");
                for (String key : softwareStatement.keySet()) {
                    requestObject.putOpt(key, softwareStatement.get(key));
                }
            }
            if (isTrue(appConfiguration.getDcrSignatureValidationEnabled())) {
                registerValidator.validateRequestObject(requestParams, softwareStatement, httpRequest);
            }

            // use merged object so software_statement values take precedence over outer metadata
            final RegisterRequest request = RegisterRequest.fromJson(requestObject);

            validateRedirectUris(request);
            validateCiba(request);
            validateSubjectType(request);
            registerValidator.validatePasswordGrantType(request);
            registerParamsValidator.validateAlgorithms(request);

            final Client client = clientService.getClient(clientId, accessToken);
            validateClientNotNull(client);
            validateSubjectIdentifierAttribute(request, client);
            final boolean redirectUrisChanged = isRedirectUrisChanged(request, client);

            registerService.updateClientFromRequestObject(client, request, true);

            validateSigningKids(client);

            if (requestObject.has(BACKCHANNEL_LOGOUT_URI.toString())
                    && StringUtils.isBlank(requestObject.optString(BACKCHANNEL_LOGOUT_URI.toString()))) {
                client.getAttributes().setBackchannelLogoutUri(new ArrayList<>());
            }

            validateEphemeralSubjectTypeBackchannelLogout(client);
            validateBackchannelLogoutUri(request, client, redirectUrisChanged);

            boolean updateClient = true;
            if (externalDynamicClientRegistrationService.isEnabled()) {
                updateClient = externalDynamicClientRegistrationService.executeExternalUpdateClientMethods(httpRequest, request, client);
            }

            if (updateClient) {
                validateEphemeralSubjectTypeBackchannelLogout(client);
                clientService.merge(client);

                JSONObject jsonObject = registerJsonService.getJSONObject(client);
                jsonObject = modifyPutScript(jsonObject, new ExecutionContext(httpRequest, null).setClient(client));

                final Response response = Response.ok().entity(registerJsonService.jsonObjectToString(jsonObject)).build();

                oAuth2AuditLog.setScope(registerService.clientScopesToString(client));
                oAuth2AuditLog.setSuccess(true);
                applicationAuditLogger.sendMessage(oAuth2AuditLog);

                return response;
            } else {
                clientService.removeFromCache(client); // clear cache to force reload from persistence
                log.trace("The Access Token is not valid for the Client ID, returns invalid_token error, client_id: {}", clientId);
                applicationAuditLogger.sendMessage(oAuth2AuditLog);
                return Response.status(Response.Status.BAD_REQUEST).
                        type(MediaType.APPLICATION_JSON_TYPE).
                        entity(errorResponseFactory.errorAsJson(RegisterErrorResponseType.INVALID_TOKEN, "External registration script returned false.")).build();
            }

        } catch (WebApplicationException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }

            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return registerService.createInternalErrorResponse(Constants.UNKNOWN_DOT).build();
    }

    private void validateClientNotNull(Client client) {
        if (client != null) {
            return;
        }

        log.trace("The Access Token is not valid for the Client ID, returns invalid_token error.");
        throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, RegisterErrorResponseType.INVALID_TOKEN, "The Access Token is not valid for the Client ID.");
    }

    private void validateSigningKids(Client client) {
        try {
            registerParamsValidator.validateSigningKid(client.getAttributes().getAccessTokenSigningKid(), ACCESS_TOKEN_SIGNING_KID.toString());
            registerParamsValidator.validateSigningKid(client.getAttributes().getIdTokenSigningKid(), ID_TOKEN_SIGNING_KID.toString());
        } catch (WebApplicationException e) {
            clientService.removeFromCache(client); // discard in-memory changes
            throw e;
        }
    }

    private void validateEphemeralSubjectTypeBackchannelLogout(Client client) {
        try {
            registerValidator.validateEphemeralSubjectTypeBackchannelLogout(client);
        } catch (WebApplicationException e) {
            clientService.removeFromCache(client); // discard in-memory changes
            throw e;
        }
    }

    private static boolean isRedirectUrisChanged(RegisterRequest request, Client client) {
        final List<String> requested = request.getRedirectUris();
        if (requested == null || requested.isEmpty()) {
            return false;
        }
        final String[] stored = client.getRedirectUris();
        return !new HashSet<>(requested).equals(stored != null ? new HashSet<>(Arrays.asList(stored)) : new HashSet<>());
    }

    private void validateBackchannelLogoutUri(RegisterRequest request, Client client, boolean redirectUrisChanged) {
        try {
            final String[] redirectUris = client.getRedirectUris();
            final List<String> redirectUriList = redirectUris != null ? Arrays.asList(redirectUris) : new ArrayList<>();
            if (StringUtils.isNotBlank(request.getBackchannelLogoutUri())) {
                registerParamsValidator.validateLogoutUri(request.getBackchannelLogoutUri(), redirectUriList, errorResponseFactory);
            } else if (redirectUrisChanged) {
                // stored logout uri is kept, so it must still fit the new redirect_uris
                registerParamsValidator.validateLogoutUri(client.getAttributes().getBackchannelLogoutUri(), redirectUriList, errorResponseFactory);
            }
        } catch (WebApplicationException e) {
            clientService.removeFromCache(client); // discard in-memory changes
            throw e;
        }
    }

    private void validateSubjectIdentifierAttribute(RegisterRequest request, Client client) {
        final RegisterRequest effective = new RegisterRequest();
        effective.setSubjectIdentifierAttribute(request.getSubjectIdentifierAttribute());
        effective.setRedirectUrisRegex(request.getRedirectUrisRegex());
        // partial updates may omit subject_type, so fall back to the stored one
        effective.setSubjectType(request.getSubjectType() != null ? request.getSubjectType() : client.getSubjectType());
        registerValidator.validateSubjectIdentifierAttribute(effective);

        // a stored attribute is kept when the request omits it, so it must stay paired with a public subject type
        if (StringUtils.isBlank(request.getSubjectIdentifierAttribute())
                && StringUtils.isNotBlank(client.getAttributes().getPublicSubjectIdentifierAttribute())
                && effective.getSubjectType() != SubjectType.PUBLIC) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST,
                    RegisterErrorResponseType.INVALID_PUBLIC_SUBJECT_IDENTIFIER_ATTRIBUTE, "The custom subject identifier requires public subject type.");
        }
    }

    private void validateSubjectType(RegisterRequest request) {
        if (request.getSubjectType() != null
                && !appConfiguration.getSubjectTypesSupported().contains(request.getSubjectType().toString())) {
            log.debug("Failed to perform client action, reason: subject_type is invalid.");

            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "subject_type is invalid");
        }
    }

    private void validateCiba(RegisterRequest request) {
        if (!cibaRegisterParamsValidatorService.validateParams(
                request.getBackchannelTokenDeliveryMode(),
                request.getBackchannelClientNotificationEndpoint(),
                request.getBackchannelAuthenticationRequestSigningAlg(),
                request.getGrantTypes(),
                request.getSubjectType(),
                request.getSectorIdentifierUri(),
                request.getJwks(),
                request.getJwksUri()
        )) {
            log.debug("Failed to perform client action, reason: unable to validate CIBA parameters");
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "Invalid Client Metadata registering to use CIBA.");
        }
    }

    private void validateRedirectUris(RegisterRequest request) {
        boolean redirectUrisValidated = true;
        if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
            redirectUrisValidated = registerParamsValidator.validateRedirectUris(
                    request.getGrantTypes(), request.getResponseTypes(),
                    request.getApplicationType(), request.getSubjectType(),
                    request.getRedirectUris(), request.getSectorIdentifierUri());
        }
        if (!redirectUrisValidated) {
            log.debug("Failed to perform client action, reason: unable to validate redirectUris");
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "");
        }
    }

    private JSONObject modifyPutScript(JSONObject jsonObject, ExecutionContext executionContext) throws StringEncrypter.EncryptionException {
        if (!externalDynamicClientRegistrationService.modifyPutResponse(jsonObject, executionContext)) {
            return registerJsonService.getJSONObject(executionContext.getClient()); // script forbids modification, re-create json object
        }
        return jsonObject;
    }
}
