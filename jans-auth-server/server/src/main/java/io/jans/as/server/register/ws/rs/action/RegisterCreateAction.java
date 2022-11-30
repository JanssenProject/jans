/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.register.ws.rs.action;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.register.ws.rs.RegisterJsonService;
import io.jans.as.server.register.ws.rs.RegisterService;
import io.jans.as.server.register.ws.rs.RegisterValidator;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
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
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class RegisterCreateAction {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private InumService inumService;

    @Inject
    private ClientService clientService;

    @Inject
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Inject
    private RegisterParamsValidator registerParamsValidator;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private RegisterValidator registerValidator;

    @Inject
    private RegisterJsonService registerJsonService;

    @Inject
    private RegisterService registerService;

    public Response createClient(String requestParams, HttpServletRequest httpRequest, SecurityContext securityContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.REGISTRATION);

        Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_REGISTRATION);
        try {
            log.trace("Registration request = {}", requestParams);

            final JSONObject requestObject = registerService.parseRequestObjectWithoutValidation(requestParams);
            final JSONObject softwareStatement = registerValidator.validateSoftwareStatement(httpRequest, requestObject);
            overrideRequestObjectFromSoftwareStatement(requestObject, softwareStatement);

            if (isTrue(appConfiguration.getDcrSignatureValidationEnabled())) {
                registerValidator.validateRequestObject(requestParams, softwareStatement, httpRequest);
            }

            final RegisterRequest r = RegisterRequest.fromJson(requestObject);

            log.info("Attempting to register client: applicationType = {}, clientName = {}, redirectUris = {}, isSecure = {}, sectorIdentifierUri = {}, defaultAcrValues = {}",
                    r.getApplicationType(), r.getClientName(), r.getRedirectUris(), securityContext.isSecure(), r.getSectorIdentifierUri(), r.getDefaultAcrValues());

            registerValidator.validatePasswordGrantType(r);
            registerValidator.validateDcrAuthorizationWithClientCredentials(r);

            setSubjectType(r);
            setIdTokenSignedResponseAlg(r);
            setAccessTokenSigningAlgFallback(r);

            registerParamsValidator.validateAlgorithms(r);

            registerValidator.validateClaimsRedirectUris(r);
            registerValidator.validateInitiateLoginUri(r);
            registerValidator.validateParamsClientRegister(r);
            registerValidator.validateRedirectUris(r);
            registerValidator.validateSubjectIdentifierAttribute(r);
            registerValidator.validateCiba(r);

            registerParamsValidator.validateLogoutUri(r.getFrontChannelLogoutUri(), r.getRedirectUris(), errorResponseFactory);
            registerParamsValidator.validateLogoutUri(r.getBackchannelLogoutUris(), r.getRedirectUris(), errorResponseFactory);

            String clientsBaseDN = staticConfiguration.getBaseDn().getClients();

            String inum = inumService.generateClientInum();
            String generatedClientSecret = UUID.randomUUID().toString();

            final Client client = new Client();
            client.setDn("inum=" + inum + "," + clientsBaseDN);
            client.setClientId(inum);
            client.setDeletable(true);
            client.setClientSecret(clientService.encryptSecret(generatedClientSecret));
            client.setRegistrationAccessToken(HandleTokenFactory.generateHandleToken());
            client.setIdTokenTokenBindingCnf(r.getIdTokenTokenBindingCnf());

            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            client.setClientIdIssuedAt(calendar.getTime());

            if (appConfiguration.getDynamicRegistrationExpirationTime() > 0) { // #883 : expiration can be -1, mean does not expire
                calendar.add(Calendar.SECOND, appConfiguration.getDynamicRegistrationExpirationTime());
                client.setClientSecretExpiresAt(calendar.getTime());
                client.setExpirationDate(calendar.getTime());
                client.setTtl(appConfiguration.getDynamicRegistrationExpirationTime());
            }
            client.setDeletable(client.getClientSecretExpiresAt() != null);

            setClientName(r, client);

            registerService.updateClientFromRequestObject(client, r, false);

            executeDynamicScrypt(r, client, httpRequest);

            Date currentTime = Calendar.getInstance().getTime();
            client.setLastAccessTime(currentTime);
            client.setLastLogonTime(currentTime);
            client.setPersistClientAuthorizations(isTrue(appConfiguration.getDynamicRegistrationPersistClientAuthorizations()));

            clientService.persist(client);

            JSONObject jsonObject = registerJsonService.getJSONObject(client);

            jsonObject = modifyPostScript(jsonObject, new ExecutionContext(httpRequest, null).setClient(client));

            builder.entity(registerJsonService.jsonObjectToString(jsonObject));

            log.info("Client registered: clientId = {}, applicationType = {}, clientName = {}, redirectUris = {}, sectorIdentifierUri = {}, redirectUrisRegex = {}",
                    client.getClientId(), client.getApplicationType(), client.getClientName(), client.getRedirectUris(), client.getSectorIdentifierUri(), client.getAttributes().getRedirectUrisRegex());

            oAuth2AuditLog.setClientId(client.getClientId());
            oAuth2AuditLog.setScope(registerService.clientScopesToString(client));
            oAuth2AuditLog.setSuccess(true);
        } catch (WebApplicationException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            builder = registerService.createInternalErrorResponse(Constants.UNKNOWN);
            log.error(e.getMessage(), e);
        }

        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header(Constants.PRAGMA, Constants.NO_CACHE);
        builder.type(MediaType.APPLICATION_JSON_TYPE);
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    private void executeDynamicScrypt(RegisterRequest r, Client client, HttpServletRequest httpRequest) {
        boolean registerClient = true;
        if (externalDynamicClientRegistrationService.isEnabled()) {
            registerClient = externalDynamicClientRegistrationService.executeExternalCreateClientMethods(r, client, httpRequest);
        }

        if (!registerClient) {
            clientService.removeFromCache(client); // clear cache to force reload from persistence
            log.trace("Client parameters are invalid, returns invalid_request error. External registration script returned false.");
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "External registration script returned false.");
        }
    }

    private void overrideRequestObjectFromSoftwareStatement(JSONObject requestObject, JSONObject softwareStatement) {
        if (softwareStatement == null) {
            return;
        }

        log.trace("Override request parameters by software_statement");
        for (String key : softwareStatement.keySet()) {
            requestObject.putOpt(key, softwareStatement.get(key));
        }
    }

    private void setSubjectType(RegisterRequest r) {
        if (r.getSubjectType() == null) {
            SubjectType defaultSubjectType = SubjectType.fromString(appConfiguration.getDefaultSubjectType());
            if (defaultSubjectType != null) {
                r.setSubjectType(defaultSubjectType);
            } else if (appConfiguration.getSubjectTypesSupported().contains(io.jans.as.model.common.SubjectType.PUBLIC.toString())) {
                r.setSubjectType(io.jans.as.model.common.SubjectType.PUBLIC);
            } else if (appConfiguration.getSubjectTypesSupported().contains(io.jans.as.model.common.SubjectType.PAIRWISE.toString())) {
                r.setSubjectType(io.jans.as.model.common.SubjectType.PAIRWISE);
            }
        }
    }

    private void setAccessTokenSigningAlgFallback(RegisterRequest r) {
        if (r.getAccessTokenSigningAlg() == null) {
            r.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm()));
        }
    }

    private void setIdTokenSignedResponseAlg(RegisterRequest r) {
        if (r.getIdTokenSignedResponseAlg() == null) {
            r.setIdTokenSignedResponseAlg(SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm()));
        }
    }

    private void setClientName(RegisterRequest r, Client client) {
        if (StringUtils.isBlank(r.getClientName()) && r.getRedirectUris() != null && !r.getRedirectUris().isEmpty()) {
            try {
                URI redUri = new URI(r.getRedirectUris().get(0));
                client.setClientName(redUri.getHost());
            } catch (Exception e) {
                //ignore
                log.error(e.getMessage(), e);
                client.setClientName(Constants.UNKNOWN);
            }
        }
    }

    private JSONObject modifyPostScript(JSONObject jsonObject, ExecutionContext executionContext) throws StringEncrypter.EncryptionException {
        if (!externalDynamicClientRegistrationService.modifyPostResponse(jsonObject, executionContext)) {
            return registerJsonService.getJSONObject(executionContext.getClient()); // script forbids modification, re-create json object
        }
        return jsonObject;
    }
}
