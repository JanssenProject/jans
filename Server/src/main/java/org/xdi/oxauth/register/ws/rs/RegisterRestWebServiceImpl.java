/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.register.ws.rs;

import static org.xdi.oxauth.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static org.xdi.oxauth.model.register.RegisterRequestParam.CLIENT_NAME;
import static org.xdi.oxauth.model.register.RegisterRequestParam.CLIENT_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.CONTACTS;
import static org.xdi.oxauth.model.register.RegisterRequestParam.DEFAULT_ACR_VALUES;
import static org.xdi.oxauth.model.register.RegisterRequestParam.DEFAULT_MAX_AGE;
import static org.xdi.oxauth.model.register.RegisterRequestParam.GRANT_TYPES;
import static org.xdi.oxauth.model.register.RegisterRequestParam.ID_TOKEN_ENCRYPTED_RESPONSE_ALG;
import static org.xdi.oxauth.model.register.RegisterRequestParam.ID_TOKEN_ENCRYPTED_RESPONSE_ENC;
import static org.xdi.oxauth.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static org.xdi.oxauth.model.register.RegisterRequestParam.INITIATE_LOGIN_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.JWKS;
import static org.xdi.oxauth.model.register.RegisterRequestParam.JWKS_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.LOGOUT_SESSION_REQUIRED;
import static org.xdi.oxauth.model.register.RegisterRequestParam.LOGOUT_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.LOGO_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.POLICY_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.POST_LOGOUT_REDIRECT_URIS;
import static org.xdi.oxauth.model.register.RegisterRequestParam.REDIRECT_URIS;
import static org.xdi.oxauth.model.register.RegisterRequestParam.REQUEST_OBJECT_ENCRYPTION_ALG;
import static org.xdi.oxauth.model.register.RegisterRequestParam.REQUEST_OBJECT_ENCRYPTION_ENC;
import static org.xdi.oxauth.model.register.RegisterRequestParam.REQUEST_OBJECT_SIGNING_ALG;
import static org.xdi.oxauth.model.register.RegisterRequestParam.REQUEST_URIS;
import static org.xdi.oxauth.model.register.RegisterRequestParam.REQUIRE_AUTH_TIME;
import static org.xdi.oxauth.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static org.xdi.oxauth.model.register.RegisterRequestParam.SECTOR_IDENTIFIER_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.SUBJECT_TYPE;
import static org.xdi.oxauth.model.register.RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD;
import static org.xdi.oxauth.model.register.RegisterRequestParam.TOKEN_ENDPOINT_AUTH_SIGNING_ALG;
import static org.xdi.oxauth.model.register.RegisterRequestParam.TOS_URI;
import static org.xdi.oxauth.model.register.RegisterRequestParam.USERINFO_ENCRYPTED_RESPONSE_ALG;
import static org.xdi.oxauth.model.register.RegisterRequestParam.USERINFO_ENCRYPTED_RESPONSE_ENC;
import static org.xdi.oxauth.model.register.RegisterRequestParam.USERINFO_SIGNED_RESPONSE_ALG;
import static org.xdi.oxauth.model.register.RegisterResponseParam.CLIENT_ID_ISSUED_AT;
import static org.xdi.oxauth.model.register.RegisterResponseParam.CLIENT_SECRET;
import static org.xdi.oxauth.model.register.RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT;
import static org.xdi.oxauth.model.register.RegisterResponseParam.REGISTRATION_CLIENT_URI;
import static org.xdi.oxauth.model.util.StringUtils.toList;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.model.metric.MetricType;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.model.audit.Action;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.config.StaticConf;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.register.RegisterErrorResponseType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.registration.RegisterParamsValidator;
import org.xdi.oxauth.model.token.HandleTokenFactory;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.InumService;
import org.xdi.oxauth.service.MetricService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.external.ExternalDynamicClientRegistrationService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.security.StringEncrypter;

/**
 * Implementation for register REST web services.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version October 31, 2016
 */
@Name("registerRestWebService")
public class RegisterRestWebServiceImpl implements RegisterRestWebService {

    @Logger
    private Log log;
    @In
    private ApplicationAuditLogger applicationAuditLogger;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private ScopeService scopeService;
    @In
    private InumService inumService;
    @In
    private ClientService clientService;
    @In
    private TokenService tokenService;

    @In
    private MetricService metricService;

    @In
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;
    
    @In
    private RegisterParamsValidator registerParamsValidator;

    @In
    private AppConfiguration appConfiguration;

    @In
    private StaticConf staticConfiguration;

    @Override
    public Response requestRegister(String requestParams, String authorization, HttpServletRequest httpRequest, SecurityContext securityContext) {
        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.DYNAMIC_CLIENT_REGISTRATION_RATE).time();
        try {
            return registerClientImpl(requestParams, httpRequest, securityContext);
        } finally {
            timerContext.stop();
        }
    }

    private Response registerClientImpl(String requestParams, HttpServletRequest httpRequest, SecurityContext securityContext) {
        Response.ResponseBuilder builder = Response.ok();
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_REGISTRATION);
        try {
            if (appConfiguration.getDynamicRegistrationEnabled()) {
                final RegisterRequest r = RegisterRequest.fromJson(requestParams);

                log.debug("Attempting to register client: applicationType = {0}, clientName = {1}, redirectUris = {2}, isSecure = {3}, sectorIdentifierUri = {4}, params = {5}",
                        r.getApplicationType(), r.getClientName(), r.getRedirectUris(), securityContext.isSecure(), r.getSectorIdentifierUri(), requestParams);

                if (r.getSubjectType() == null) {
                    SubjectType defaultSubjectType = SubjectType.fromString(appConfiguration.getDefaultSubjectType());
                    if (defaultSubjectType != null) {
                        r.setSubjectType(defaultSubjectType);
                    } else if (appConfiguration.getSubjectTypesSupported().contains(SubjectType.PUBLIC.toString())) {
                        r.setSubjectType(SubjectType.PUBLIC);
                    } else if (appConfiguration.getSubjectTypesSupported().contains(SubjectType.PAIRWISE.toString())) {
                        r.setSubjectType(SubjectType.PAIRWISE);
                    }
                }

                if (r.getIdTokenSignedResponseAlg() == null) {
                    r.setIdTokenSignedResponseAlg(SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm()));
                }

                if (r.getIdTokenSignedResponseAlg() != SignatureAlgorithm.NONE) {
                    if (registerParamsValidator.validateParamsClientRegister(r.getApplicationType(), r.getSubjectType(),
                            r.getRedirectUris(), r.getSectorIdentifierUri())) {
                        if (!registerParamsValidator.validateRedirectUris(r.getApplicationType(), r.getSubjectType(),
                                r.getRedirectUris(), r.getSectorIdentifierUri())) {
                            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode());
                            builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_REDIRECT_URI));
                        } else {
                            registerParamsValidator.validateLogoutUri(r.getLogoutUris(), r.getRedirectUris(), errorResponseFactory);

                            String clientsBaseDN = staticConfiguration.getBaseDn().getClients();

                            String inum = inumService.generateClientInum();
                            String generatedClientSecret = UUID.randomUUID().toString();

                            final Client client = new Client();
                            client.setDn("inum=" + inum + "," + clientsBaseDN);
                            client.setClientId(inum);
                            client.setClientSecret(generatedClientSecret);
                            client.setRegistrationAccessToken(HandleTokenFactory.generateHandleToken());

                            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                            client.setClientIdIssuedAt(calendar.getTime());

                            if (appConfiguration.getDynamicRegistrationExpirationTime() > 0) {
                                calendar.add(Calendar.SECOND, appConfiguration.getDynamicRegistrationExpirationTime());
                                client.setClientSecretExpiresAt(calendar.getTime());
                            }

                            if (StringUtils.isBlank(r.getClientName()) && r.getRedirectUris() != null && !r.getRedirectUris().isEmpty()) {
                                try {
                                    URI redUri = new URI(r.getRedirectUris().get(0));
                                    client.setClientName(redUri.getHost());
                                } catch (Exception e) {
                                    //ignore
                                    log.error(e.getMessage(), e);
                                    client.setClientName("Unknown");
                                }
                            }

                            updateClientFromRequestObject(client, r);

                            if (externalDynamicClientRegistrationService.isEnabled()) {
                                externalDynamicClientRegistrationService.executeExternalUpdateClientMethods(r, client);
                            }

                            Date currentTime = Calendar.getInstance().getTime();
                            client.setLastAccessTime(currentTime);
                            client.setLastLogonTime(currentTime);

                            Boolean persistClientAuthorizations = appConfiguration.getDynamicRegistrationPersistClientAuthorizations();
                            client.setPersistClientAuthorizations(persistClientAuthorizations != null ? persistClientAuthorizations : false);

                            clientService.persist(client);

                            JSONObject jsonObject = getJSONObject(client);
                            builder.entity(jsonObject.toString(4).replace("\\/", "/"));

                            oAuth2AuditLog.setClientId(client.getClientId());
                            oAuth2AuditLog.setScope(clientScopesToString(client));
                            oAuth2AuditLog.setSuccess(true);
                        }
                    } else {
                        log.trace("Client parameters are invalid, returns invalid_request error.");
                        builder = Response.status(Response.Status.BAD_REQUEST).
                                entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
                    }
                } else {
                    log.debug("The signature algorithm for id_token cannot be none.");
                    builder = Response.status(Response.Status.BAD_REQUEST).
                            entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
                }
            } else {
                log.debug("Dynamic client registration is disabled.");
                builder = Response.status(Response.Status.BAD_REQUEST).
                        entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.ACCESS_DENIED));
            }
        } catch (StringEncrypter.EncryptionException e) {
            builder = internalErrorResponse();
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            builder = internalErrorResponse();
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            builder = internalErrorResponse();
            log.error(e.getMessage(), e);
        }

        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    public Response.ResponseBuilder internalErrorResponse() {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
    }

    // yuriyz - ATTENTION : this method is used for both registration and update client metadata cases, therefore any logic here
    // will be applied for both cases.
    private void updateClientFromRequestObject(Client p_client, RegisterRequest requestObject) throws JSONException {
        List<String> redirectUris = requestObject.getRedirectUris();
        if (redirectUris != null && !redirectUris.isEmpty()) {
            redirectUris = new ArrayList<String>(new HashSet<String>(redirectUris)); // Remove repeated elements
            p_client.setRedirectUris(redirectUris.toArray(new String[redirectUris.size()]));
        }
        if (requestObject.getApplicationType() != null) {
            p_client.setApplicationType(requestObject.getApplicationType().toString());
        }
        if (StringUtils.isNotBlank(requestObject.getClientName())) {
            p_client.setClientName(requestObject.getClientName());
        }
        if (StringUtils.isNotBlank(requestObject.getSectorIdentifierUri())) {
            p_client.setSectorIdentifierUri(requestObject.getSectorIdentifierUri());
        }
        List<ResponseType> responseTypes = requestObject.getResponseTypes();
        if (responseTypes != null && !responseTypes.isEmpty()) {
            responseTypes = new ArrayList<ResponseType>(new HashSet<ResponseType>(responseTypes)); // Remove repeated elements
            p_client.setResponseTypes(responseTypes.toArray(new ResponseType[responseTypes.size()]));
        }

        List<String> contacts = requestObject.getContacts();
        if (contacts != null && !contacts.isEmpty()) {
            contacts = new ArrayList<String>(new HashSet<String>(contacts)); // Remove repeated elements
            p_client.setContacts(contacts.toArray(new String[contacts.size()]));
        }
        if (StringUtils.isNotBlank(requestObject.getLogoUri())) {
            p_client.setLogoUri(requestObject.getLogoUri());
        }
        if (StringUtils.isNotBlank(requestObject.getClientUri())) {
            p_client.setClientUri(requestObject.getClientUri());
        }
        if (StringUtils.isNotBlank(requestObject.getPolicyUri())) {
            p_client.setPolicyUri(requestObject.getPolicyUri());
        }
        if (StringUtils.isNotBlank(requestObject.getTosUri())) {
            p_client.setTosUri(requestObject.getTosUri());
        }
        if (StringUtils.isNotBlank(requestObject.getJwksUri())) {
            p_client.setJwksUri(requestObject.getJwksUri());
        }
        if (StringUtils.isNotBlank(requestObject.getJwks())) {
            p_client.setJwks(requestObject.getJwks());
        }
        if (requestObject.getSubjectType() != null) {
            p_client.setSubjectType(requestObject.getSubjectType().toString());
        }
        if (requestObject.getIdTokenSignedResponseAlg() != null
                && requestObject.getIdTokenSignedResponseAlg() != SignatureAlgorithm.NONE) {
            p_client.setIdTokenSignedResponseAlg(requestObject.getIdTokenSignedResponseAlg().toString());
        }
        if (requestObject.getIdTokenEncryptedResponseAlg() != null) {
            p_client.setIdTokenEncryptedResponseAlg(requestObject.getIdTokenEncryptedResponseAlg().toString());
        }
        if (requestObject.getIdTokenEncryptedResponseEnc() != null) {
            p_client.setIdTokenEncryptedResponseEnc(requestObject.getIdTokenEncryptedResponseEnc().toString());
        }
        if (requestObject.getUserInfoSignedResponseAlg() != null) {
            p_client.setUserInfoSignedResponseAlg(requestObject.getUserInfoSignedResponseAlg().toString());
        }
        if (requestObject.getUserInfoEncryptedResponseAlg() != null) {
            p_client.setUserInfoEncryptedResponseAlg(requestObject.getUserInfoEncryptedResponseAlg().toString());
        }
        if (requestObject.getUserInfoEncryptedResponseEnc() != null) {
            p_client.setUserInfoEncryptedResponseEnc(requestObject.getUserInfoEncryptedResponseEnc().toString());
        }
        if (requestObject.getRequestObjectSigningAlg() != null) {
            p_client.setRequestObjectSigningAlg(requestObject.getRequestObjectSigningAlg().toString());
        }
        if (requestObject.getRequestObjectEncryptionAlg() != null) {
            p_client.setRequestObjectEncryptionAlg(requestObject.getRequestObjectEncryptionAlg().toString());
        }
        if (requestObject.getRequestObjectEncryptionEnc() != null) {
            p_client.setRequestObjectEncryptionEnc(requestObject.getRequestObjectEncryptionEnc().toString());
        }
        if (requestObject.getTokenEndpointAuthMethod() != null) {
            p_client.setTokenEndpointAuthMethod(requestObject.getTokenEndpointAuthMethod().toString());
        } else { // If omitted, the default is client_secret_basic
            p_client.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        }
        if (requestObject.getTokenEndpointAuthSigningAlg() != null) {
            p_client.setTokenEndpointAuthSigningAlg(requestObject.getTokenEndpointAuthSigningAlg().toString());
        }
        if (requestObject.getDefaultMaxAge() != null) {
            p_client.setDefaultMaxAge(requestObject.getDefaultMaxAge());
        }
        if (requestObject.getRequireAuthTime() != null) {
            p_client.setRequireAuthTime(requestObject.getRequireAuthTime());
        }
        List<String> defaultAcrValues = requestObject.getDefaultAcrValues();
        if (defaultAcrValues != null && !defaultAcrValues.isEmpty()) {
            defaultAcrValues = new ArrayList<String>(new HashSet<String>(defaultAcrValues)); // Remove repeated elements
            p_client.setDefaultAcrValues(defaultAcrValues.toArray(new String[defaultAcrValues.size()]));
        }
        if (StringUtils.isNotBlank(requestObject.getInitiateLoginUri())) {
            p_client.setInitiateLoginUri(requestObject.getInitiateLoginUri());
        }
        List<String> postLogoutRedirectUris = requestObject.getPostLogoutRedirectUris();
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            postLogoutRedirectUris = new ArrayList<String>(new HashSet<String>(postLogoutRedirectUris)); // Remove repeated elements
            p_client.setPostLogoutRedirectUris(postLogoutRedirectUris.toArray(new String[postLogoutRedirectUris.size()]));
        }

        if (requestObject.getLogoutUris() != null && !requestObject.getLogoutUris().isEmpty()) {
            p_client.setLogoutUri(requestObject.getLogoutUris().toArray(new String[requestObject.getLogoutUris().size()]));
        }
        p_client.setLogoutSessionRequired(requestObject.getLogoutSessionRequired());

        List<String> requestUris = requestObject.getRequestUris();
        if (requestUris != null && !requestUris.isEmpty()) {
            requestUris = new ArrayList<String>(new HashSet<String>(requestUris)); // Remove repeated elements
            p_client.setRequestUris(requestUris.toArray(new String[requestUris.size()]));
        }

        List<String> scopes = requestObject.getScopes();
        List<String> scopesDn;
        if (scopes != null && !scopes.isEmpty()
                && appConfiguration.getDynamicRegistrationScopesParamEnabled() != null
                && appConfiguration.getDynamicRegistrationScopesParamEnabled()) {
            List<String> defaultScopes = scopeService.getDefaultScopesDn();
            List<String> requestedScopes = scopeService.getScopesDn(scopes);
            if (defaultScopes.containsAll(requestedScopes)) {
                scopesDn = requestedScopes;
                p_client.setScopes(scopesDn.toArray(new String[scopesDn.size()]));
            } else {
                scopesDn = defaultScopes;
                p_client.setScopes(scopesDn.toArray(new String[scopesDn.size()]));
            }
        } else {
            scopesDn = scopeService.getDefaultScopesDn();
            p_client.setScopes(scopesDn.toArray(new String[scopesDn.size()]));
        }

        Date clientSecretExpiresAt = requestObject.getClientSecretExpiresAt();
        if (clientSecretExpiresAt != null) {
            p_client.setClientSecretExpiresAt(clientSecretExpiresAt);
        }

        if (requestObject.getJsonObject() != null) {
            // Custom params
            putCustomStuffIntoObject(p_client, requestObject.getJsonObject());
        }
    }

    @Override
    public Response requestClientUpdate(String requestParams, String clientId, @HeaderParam("Authorization") String authorization, @Context HttpServletRequest httpRequest, @Context SecurityContext securityContext) {
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_UPDATE);
        oAuth2AuditLog.setClientId(clientId);
        try {
            log.debug("Attempting to UPDATE client, client_id: {0}, requestParams = {1}, isSecure = {3}",
                    clientId, requestParams, securityContext.isSecure());
            final String accessToken = tokenService.getTokenFromAuthorizationParameter(authorization);

            if (StringUtils.isNotBlank(accessToken) && StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(requestParams)) {
                final RegisterRequest request = RegisterRequest.fromJson(requestParams);
                if (request != null) {
                    boolean redirectUrisValidated = true;
                    if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
                        redirectUrisValidated = registerParamsValidator.validateRedirectUris(request.getApplicationType(), request.getSubjectType(),
                                request.getRedirectUris(), request.getSectorIdentifierUri());
                    }

                    if (redirectUrisValidated) {
                        if (request.getSubjectType() != null
                                && !appConfiguration.getSubjectTypesSupported().contains(request.getSubjectType())) {
                            log.debug("Client UPDATE : parameter subject_type is invalid. Returns BAD_REQUEST response.");
                            applicationAuditLogger.sendMessage(oAuth2AuditLog);
                            return Response.status(Response.Status.BAD_REQUEST).
                                    entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA)).build();
                        }

                        final Client client = clientService.getClient(clientId, accessToken);
                        if (client != null) {
                            updateClientFromRequestObject(client, request);
                            clientService.merge(client);

                            oAuth2AuditLog.setScope(clientScopesToString(client));
                            oAuth2AuditLog.setSuccess(true);
                            applicationAuditLogger.sendMessage(oAuth2AuditLog);
                            return Response.status(Response.Status.OK).entity(clientAsEntity(client)).build();
                        } else {
                            log.trace("The Access Token is not valid for the Client ID, returns invalid_token error.");
                            applicationAuditLogger.sendMessage(oAuth2AuditLog);
                            return Response.status(Response.Status.BAD_REQUEST).
                                    entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_TOKEN)).build();
                        }
                    }
                }
            }

            log.debug("Client UPDATE : parameters are invalid. Returns BAD_REQUEST response.");
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA)).build();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return internalErrorResponse().build();
    }

    @Override
    public Response requestClientRead(String clientId, String authorization, HttpServletRequest httpRequest,
                                      SecurityContext securityContext) {
        String accessToken = tokenService.getTokenFromAuthorizationParameter(authorization);
        log.debug("Attempting to read client: clientId = {0}, registrationAccessToken = {1} isSecure = {2}",
                clientId, accessToken, securityContext.isSecure());
        Response.ResponseBuilder builder = Response.ok();

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_READ);
        oAuth2AuditLog.setClientId(clientId);
        try {
            if (appConfiguration.getDynamicRegistrationEnabled()) {
                if (registerParamsValidator.validateParamsClientRead(clientId, accessToken)) {
                    Client client = clientService.getClient(clientId, accessToken);
                    if (client != null) {
                        oAuth2AuditLog.setScope(clientScopesToString(client));
                        oAuth2AuditLog.setSuccess(true);
                        builder.entity(clientAsEntity(client));
                    } else {
                        log.trace("The Access Token is not valid for the Client ID, returns invalid_token error.");
                        builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode());
                        builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_TOKEN));
                    }
                } else {
                    log.trace("Client parameters are invalid.");
                    builder = Response.status(Response.Status.BAD_REQUEST);
                    builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
                }
            } else {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.ACCESS_DENIED));
            }
        } catch (JSONException e) {
            builder = Response.status(500);
            builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
            log.error(e.getMessage(), e);
        } catch (StringEncrypter.EncryptionException e) {
            builder = Response.status(500);
            builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
            log.error(e.getMessage(), e);
        }

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        cacheControl.setNoStore(true);
        builder.cacheControl(cacheControl);
        builder.header("Pragma", "no-cache");
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    private String clientAsEntity(Client p_client) throws JSONException, StringEncrypter.EncryptionException {
        final JSONObject jsonObject = getJSONObject(p_client);
        return jsonObject.toString(4).replace("\\/", "/");
    }

    private JSONObject getJSONObject(Client client) throws JSONException, StringEncrypter.EncryptionException {
        JSONObject responseJsonObject = new JSONObject();

        Util.addToJSONObjectIfNotNull(responseJsonObject, RegisterResponseParam.CLIENT_ID.toString(), client.getClientId());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_SECRET.toString(), client.getClientSecret());
        Util.addToJSONObjectIfNotNull(responseJsonObject, RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString(), client.getRegistrationAccessToken());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REGISTRATION_CLIENT_URI.toString(),
        		appConfiguration.getRegistrationEndpoint() + "?" +
                        RegisterResponseParam.CLIENT_ID.toString() + "=" + client.getClientId());
        responseJsonObject.put(CLIENT_ID_ISSUED_AT.toString(), client.getClientIdIssuedAt().getTime() / 1000);
        responseJsonObject.put(CLIENT_SECRET_EXPIRES_AT.toString(), client.getClientSecretExpiresAt() != null && client.getClientSecretExpiresAt().getTime() > 0 ?
                client.getClientSecretExpiresAt().getTime() / 1000 : 0);

        Util.addToJSONObjectIfNotNull(responseJsonObject, REDIRECT_URIS.toString(), client.getRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, RESPONSE_TYPES.toString(), ResponseType.toStringArray(client.getResponseTypes()));
        Util.addToJSONObjectIfNotNull(responseJsonObject, GRANT_TYPES.toString(), client.getGrantTypes());
        Util.addToJSONObjectIfNotNull(responseJsonObject, APPLICATION_TYPE.toString(), client.getApplicationType());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CONTACTS.toString(), client.getContacts());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_NAME.toString(), client.getClientName());
        Util.addToJSONObjectIfNotNull(responseJsonObject, LOGO_URI.toString(), client.getLogoUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_URI.toString(), client.getClientUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POLICY_URI.toString(), client.getPolicyUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOS_URI.toString(), client.getTosUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS_URI.toString(), client.getJwksUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS.toString(), client.getJwks());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SECTOR_IDENTIFIER_URI.toString(), client.getSectorIdentifierUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SUBJECT_TYPE.toString(), client.getSubjectType());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), client.getIdTokenSignedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), client.getIdTokenEncryptedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), client.getIdTokenEncryptedResponseEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_SIGNED_RESPONSE_ALG.toString(), client.getUserInfoSignedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_ENCRYPTED_RESPONSE_ALG.toString(), client.getUserInfoEncryptedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_ENCRYPTED_RESPONSE_ENC.toString(), client.getUserInfoEncryptedResponseEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_OBJECT_SIGNING_ALG.toString(), client.getRequestObjectSigningAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_OBJECT_ENCRYPTION_ALG.toString(), client.getRequestObjectEncryptionAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_OBJECT_ENCRYPTION_ENC.toString(), client.getRequestObjectEncryptionEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOKEN_ENDPOINT_AUTH_METHOD.toString(), client.getTokenEndpointAuthMethod());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString(), client.getTokenEndpointAuthSigningAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, DEFAULT_MAX_AGE.toString(), client.getDefaultMaxAge());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUIRE_AUTH_TIME.toString(), client.getRequireAuthTime());
        Util.addToJSONObjectIfNotNull(responseJsonObject, DEFAULT_ACR_VALUES.toString(), client.getDefaultAcrValues());
        Util.addToJSONObjectIfNotNull(responseJsonObject, INITIATE_LOGIN_URI.toString(), client.getInitiateLoginUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POST_LOGOUT_REDIRECT_URIS.toString(), client.getPostLogoutRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_URIS.toString(), client.getRequestUris());

        // Logout params
        Util.addToJSONObjectIfNotNull(responseJsonObject, LOGOUT_URI.toString(), client.getLogoutUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, LOGOUT_SESSION_REQUIRED.toString(), client.getLogoutSessionRequired());

        // Custom Params
        String[] scopeNames = null;
        String[] scopeDns = client.getScopes();
        if (scopeDns != null) {
            scopeNames = new String[scopeDns.length];
            for (int i = 0; i < scopeDns.length; i++) {
                Scope scope = scopeService.getScopeByDn(scopeDns[i]);
                scopeNames[i] = scope.getDisplayName();
            }
        }
        Util.addToJSONObjectIfNotNull(responseJsonObject, "scopes", scopeNames);

        return responseJsonObject;
    }

    /**
     * Puts custom object class and custom attributes in client object for persistence.
     *
     * @param p_client        client object
     * @param p_requestObject request object
     */
    private void putCustomStuffIntoObject(Client p_client, JSONObject p_requestObject) throws JSONException {
        // custom object class
        final String customOC = appConfiguration.getDynamicRegistrationCustomObjectClass();
        if (StringUtils.isNotBlank(customOC)) {
            p_client.setCustomObjectClasses(new String[]{customOC});
        }

        // custom attributes (custom attributes must be in custom object class)
        final List<String> attrList = appConfiguration.getDynamicRegistrationCustomAttributes();
        if (attrList != null && !attrList.isEmpty()) {
            final Log staticLog = Logging.getLog(RegisterRestWebServiceImpl.class);
            for (String attr : attrList) {
                if (p_requestObject.has(attr)) {
                    final JSONArray parameterValuesJsonArray = p_requestObject.optJSONArray(attr);
                    final List<String> parameterValues = parameterValuesJsonArray != null ?
                            toList(parameterValuesJsonArray) :
                            Arrays.asList(p_requestObject.getString(attr));
                    if (parameterValues != null && !parameterValues.isEmpty()) {
                        try {
                            p_client.getCustomAttributes().add(new CustomAttribute(attr, parameterValues));
                        } catch (Exception e) {
                            staticLog.debug(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private String clientScopesToString(Client client){
        String[] scopeDns = client.getScopes();
        if (scopeDns != null) {
            String[] scopeNames = new String[scopeDns.length];
            for (int i = 0; i < scopeDns.length; i++) {
                Scope scope = scopeService.getScopeByDn(scopeDns[i]);
                scopeNames[i] = scope.getDisplayName();
            }
            return StringUtils.join(scopeNames, " ");
        }
        return null;
    }
}