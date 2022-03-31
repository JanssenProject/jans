/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.register.ws.rs.action;

import com.google.common.base.Strings;
import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.model.util.Pair;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBARegisterParamsValidatorService;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
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
    private CIBARegisterParamsValidatorService cibaRegisterParamsValidatorService;

    @Inject
    private RegisterValidator registerValidator;

    @Inject
    private RegisterJsonService registerJsonService;

    @Inject
    private RegisterService registerService;

    @SuppressWarnings("java:S3776")
    public Response createClient(String requestParams, HttpServletRequest httpRequest, SecurityContext securityContext) {
        errorResponseFactory.validateComponentEnabled(ComponentType.REGISTRATION);

        Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_REGISTRATION);
        try {
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

            final RegisterRequest r = RegisterRequest.fromJson(requestObject);

            log.info("Attempting to register client: applicationType = {}, clientName = {}, redirectUris = {}, isSecure = {}, sectorIdentifierUri = {}, defaultAcrValues = {}",
                    r.getApplicationType(), r.getClientName(), r.getRedirectUris(), securityContext.isSecure(), r.getSectorIdentifierUri(), r.getDefaultAcrValues());
            log.trace("Registration request = {}", requestParams);

            if (isFalse(appConfiguration.getDynamicRegistrationPasswordGrantTypeEnabled())
                    && registerParamsValidator.checkIfThereIsPasswordGrantType(r.getGrantTypes())) {
                log.info("Password Grant Type is not allowed for Dynamic Client Registration.");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.ACCESS_DENIED, "Password Grant Type is not allowed for Dynamic Client Registration.");
            }

            if (isTrue(appConfiguration.getDcrAuthorizationWithClientCredentials()) && !r.getGrantTypes().contains(GrantType.CLIENT_CREDENTIALS)) {
                log.info("Register request does not contain grant_type=client_credentials, however dcrAuthorizationWithClientCredentials=true which is forbidden.");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.ACCESS_DENIED, "Client Credentials Grant Type is not present in Dynamic Client Registration request.");
            }

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

            registerParamsValidator.validateAlgorithms(r); // Throws a WebApplicationException whether a validation doesn't pass

            // Default Signature Algorithm
            if (r.getIdTokenSignedResponseAlg() == null) {
                r.setIdTokenSignedResponseAlg(SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm()));
            }
            if (r.getAccessTokenSigningAlg() == null) {
                r.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm()));
            }

            if (r.getClaimsRedirectUris() != null &&
                    !r.getClaimsRedirectUris().isEmpty() &&
                    !registerParamsValidator.validateRedirectUris(r.getGrantTypes(), r.getResponseTypes(), r.getApplicationType(), r.getSubjectType(), r.getClaimsRedirectUris(), r.getSectorIdentifierUri())) {
                log.debug("Value of one or more claims_redirect_uris is invalid, claims_redirect_uris: {}", r.getClaimsRedirectUris());
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLAIMS_REDIRECT_URI, "Value of one or more claims_redirect_uris is invalid");
            }

            if (!Strings.isNullOrEmpty(r.getInitiateLoginUri()) && !registerParamsValidator.validateInitiateLoginUri(r.getInitiateLoginUri())) {
                log.debug("The Initiate Login Uri is invalid. The initiate_login_uri must use the https schema: {}", r.getInitiateLoginUri());
                throw errorResponseFactory.createWebApplicationException(
                        Response.Status.BAD_REQUEST,
                        RegisterErrorResponseType.INVALID_CLIENT_METADATA,
                        "The Initiate Login Uri is invalid. The initiate_login_uri must use the https schema.");
            }

            final Pair<Boolean, String> validateResult = registerParamsValidator.validateParamsClientRegister(
                    r.getApplicationType(), r.getSubjectType(),
                    r.getGrantTypes(), r.getResponseTypes(),
                    r.getRedirectUris());
            if (isFalse(validateResult.getFirst())) {
                log.trace("Client parameters are invalid, returns invalid_request error. Reason: {}", validateResult.getSecond());
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, validateResult.getSecond());
            }

            if (!registerParamsValidator.validateRedirectUris(
                    r.getGrantTypes(), r.getResponseTypes(),
                    r.getApplicationType(), r.getSubjectType(),
                    r.getRedirectUris(), r.getSectorIdentifierUri())) {
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_REDIRECT_URI, "Failed to validate redirect uris.");
            }

            registerValidator.validateSubjectIdentifierAttribute(r);

            if (!cibaRegisterParamsValidatorService.validateParams(
                    r.getBackchannelTokenDeliveryMode(),
                    r.getBackchannelClientNotificationEndpoint(),
                    r.getBackchannelAuthenticationRequestSigningAlg(),
                    r.getGrantTypes(),
                    r.getSubjectType(),
                    r.getSectorIdentifierUri(),
                    r.getJwks(),
                    r.getJwksUri()
            )) { // CIBA
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA,
                        "Invalid Client Metadata registering to use CIBA (Client Initiated Backchannel Authentication).");
            }

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

            boolean registerClient = true;
            if (externalDynamicClientRegistrationService.isEnabled()) {
                registerClient = externalDynamicClientRegistrationService.executeExternalCreateClientMethods(r, client, httpRequest);
            }

            if (!registerClient) {
                clientService.removeFromCache(client); // clear cache to force reload from persistence
                log.trace("Client parameters are invalid, returns invalid_request error. External registration script returned false.");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "External registration script returned false.");
            }

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
        } catch (StringEncrypter.EncryptionException e) {
            builder = registerService.createInternalErrorResponse("Encryption exception occured.");
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            builder = registerService.createInternalErrorResponse("Failed to parse JSON.");
            log.error(e.getMessage(), e);
        } catch (WebApplicationException e) {
            if (log.isErrorEnabled())
                log.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            builder = registerService.createInternalErrorResponse(Constants.UNKNOWN_DOT);
            log.error(e.getMessage(), e);
        }

        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header(Constants.PRAGMA, Constants.NO_CACHE);
        builder.type(MediaType.APPLICATION_JSON_TYPE);
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
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
