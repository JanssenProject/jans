/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.register.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.model.metric.MetricType;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.model.common.CustomAttribute;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.config.ConfigurationFactory;
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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.*;

import static org.xdi.oxauth.model.register.RegisterRequestParam.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;
import static org.xdi.oxauth.model.util.StringUtils.toList;

/**
 * Implementation for register REST web services.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version October 16, 2015
 */
@Name("registerRestWebService")
public class RegisterRestWebServiceImpl implements RegisterRestWebService {

    @Logger
    private Log log;
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

    @Override
    public Response requestRegister(String requestParams, String authorization, HttpServletRequest httpRequest, SecurityContext securityContext) {
        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.DYNAMIC_CLIENT_REGISTRATION_RATE).time();
        try {
            return registerClientImpl(requestParams, securityContext);
        } finally {
            timerContext.stop();
        }
    }

    private Response registerClientImpl(String requestParams, SecurityContext securityContext) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            final RegisterRequest r = RegisterRequest.fromJson(requestParams);

            if (r.getSubjectType() == null) {
                if (ConfigurationFactory.instance().getConfiguration().getSubjectTypesSupported().contains(SubjectType.PUBLIC.toString())) {
                    r.setSubjectType(SubjectType.PUBLIC);
                } else if (ConfigurationFactory.instance().getConfiguration().getSubjectTypesSupported().contains(SubjectType.PAIRWISE.toString())) {
                    r.setSubjectType(SubjectType.PAIRWISE);
                }
            }

            if (r.getIdTokenSignedResponseAlg() == null) {
                r.setIdTokenSignedResponseAlg(SignatureAlgorithm.fromName(ConfigurationFactory.instance().getConfiguration().getDefaultSignatureAlgorithm()));
            }

            log.debug("Attempting to register client: applicationType = {0}, clientName = {1}, redirectUris = {2}, isSecure = {3}, sectorIdentifierUri = {4}",
                    r.getApplicationType(), r.getClientName(), r.getRedirectUris(), securityContext.isSecure(), r.getSectorIdentifierUri());

            if (ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationEnabled()) {

                if (RegisterParamsValidator.validateParamsClientRegister(r.getApplicationType(), r.getSubjectType(),
                        r.getRedirectUris(), r.getSectorIdentifierUri())) {
                    if (!RegisterParamsValidator.validateRedirectUris(r.getApplicationType(), r.getSubjectType(),
                            r.getRedirectUris(), r.getSectorIdentifierUri())) {
                        builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode());
                        builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_REDIRECT_URI));
                    } else {
                        String clientsBaseDN = ConfigurationFactory.instance().getBaseDn().getClients();

                        String inum = inumService.generateClientInum();
                        String generatedClientSecret = UUID.randomUUID().toString();

                        String[] scopes = new String[0];
                        if (ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationScopesParamEnabled() != null
                                && ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationScopesParamEnabled()
                                && r.getScopes().size() > 0) {
                            scopes = scopeService.getScopesDn(r.getScopes()).toArray(scopes);
                        } else {
                            scopes = scopeService.getDefaultScopesDn().toArray(scopes);
                        }

                        final Client client = new Client();
                        client.setDn("inum=" + inum + "," + clientsBaseDN);
                        client.setClientId(inum);
                        client.setClientSecret(generatedClientSecret);
                        client.setScopes(scopes);
                        client.setRegistrationAccessToken(HandleTokenFactory.generateHandleToken());

                        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                        client.setClientIdIssuedAt(calendar.getTime());

                        if (ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationExpirationTime() > 0) {
                            calendar.add(Calendar.SECOND, ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationExpirationTime());
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

                        boolean persistClientAuthorizations = ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationPersistClientAuthorizations();
                        client.setPersistClientAuthorizations(persistClientAuthorizations);

                        clientService.persist(client);

                        JSONObject jsonObject = getJSONObject(client);
                        builder.entity(jsonObject.toString(4).replace("\\/", "/"));
                    }
                } else {
                    log.trace("Client parameters are invalid, returns invalid_request error.");
                    builder = Response.status(Response.Status.BAD_REQUEST).
                            entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
                }
            } else {
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
        return builder.build();
    }

    public Response.ResponseBuilder internalErrorResponse() {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
    }

    // yuriyz - ATTENTION : this method is used for both registration and update client metadata cases, therefore any logic here
    // will be applied for both cases.
    public static void updateClientFromRequestObject(Client p_client, RegisterRequest p_request) throws JSONException {
        List<String> redirectUris = p_request.getRedirectUris();
        if (redirectUris != null && !redirectUris.isEmpty()) {
            redirectUris = new ArrayList<String>(new HashSet<String>(redirectUris)); // Remove repeated elements
            p_client.setRedirectUris(redirectUris.toArray(new String[redirectUris.size()]));
        }
        if (p_request.getApplicationType() != null) {
            p_client.setApplicationType(p_request.getApplicationType().toString());
        }
        if (StringUtils.isNotBlank(p_request.getClientName())) {
            p_client.setClientName(p_request.getClientName());
        }
        if (StringUtils.isNotBlank(p_request.getSectorIdentifierUri())) {
            p_client.setSectorIdentifierUri(p_request.getSectorIdentifierUri());
        }
        List<ResponseType> responseTypes = p_request.getResponseTypes();
        if (responseTypes != null && !responseTypes.isEmpty()) {
            responseTypes = new ArrayList<ResponseType>(new HashSet<ResponseType>(responseTypes)); // Remove repeated elements
            p_client.setResponseTypes(responseTypes.toArray(new ResponseType[responseTypes.size()]));
        }

        List<String> contacts = p_request.getContacts();
        if (contacts != null && !contacts.isEmpty()) {
            contacts = new ArrayList<String>(new HashSet<String>(contacts)); // Remove repeated elements
            p_client.setContacts(contacts.toArray(new String[contacts.size()]));
        }
        if (StringUtils.isNotBlank(p_request.getLogoUri())) {
            p_client.setLogoUri(p_request.getLogoUri());
        }
        if (StringUtils.isNotBlank(p_request.getClientUri())) {
            p_client.setClientUri(p_request.getClientUri());
        }
        if (p_request.getTokenEndpointAuthMethod() != null) {
            p_client.setTokenEndpointAuthMethod(p_request.getTokenEndpointAuthMethod().toString());
        }
        if (StringUtils.isNotBlank(p_request.getPolicyUri())) {
            p_client.setPolicyUri(p_request.getPolicyUri());
        }
        if (StringUtils.isNotBlank(p_request.getTosUri())) {
            p_client.setTosUri(p_request.getTosUri());
        }
        if (StringUtils.isNotBlank(p_request.getJwksUri())) {
            p_client.setJwksUri(p_request.getJwksUri());
        }
        if (StringUtils.isNotBlank(p_request.getJwks())) {
            p_client.setJwks(p_request.getJwks());
        }
        if (p_request.getSubjectType() != null) {
            p_client.setSubjectType(p_request.getSubjectType().toString());
        }
        if (p_request.getRequestObjectSigningAlg() != null) {
            p_client.setRequestObjectSigningAlg(p_request.getRequestObjectSigningAlg().toString());
        }
        if (p_request.getUserInfoSignedResponseAlg() != null) {
            p_client.setUserInfoSignedResponseAlg(p_request.getUserInfoSignedResponseAlg().toString());
        }
        if (p_request.getUserInfoEncryptedResponseAlg() != null) {
            p_client.setUserInfoEncryptedResponseAlg(p_request.getUserInfoEncryptedResponseAlg().toString());
        }
        if (p_request.getUserInfoEncryptedResponseEnc() != null) {
            p_client.setUserInfoEncryptedResponseEnc(p_request.getUserInfoEncryptedResponseEnc().toString());
        }
        if (p_request.getIdTokenSignedResponseAlg() != null) {
            p_client.setIdTokenSignedResponseAlg(p_request.getIdTokenSignedResponseAlg().toString());
        }
        if (p_request.getIdTokenEncryptedResponseAlg() != null) {
            p_client.setIdTokenEncryptedResponseAlg(p_request.getIdTokenEncryptedResponseAlg().toString());
        }
        if (p_request.getIdTokenEncryptedResponseEnc() != null) {
            p_client.setIdTokenEncryptedResponseEnc(p_request.getIdTokenEncryptedResponseEnc().toString());
        }
        if (p_request.getDefaultMaxAge() != null) {
            p_client.setDefaultMaxAge(p_request.getDefaultMaxAge());
        }
        if (p_request.getRequireAuthTime() != null) {
            p_client.setRequireAuthTime(p_request.getRequireAuthTime());
        }
        List<String> defaultAcrValues = p_request.getDefaultAcrValues();
        if (defaultAcrValues != null && !defaultAcrValues.isEmpty()) {
            defaultAcrValues = new ArrayList<String>(new HashSet<String>(defaultAcrValues)); // Remove repeated elements
            p_client.setDefaultAcrValues(defaultAcrValues.toArray(new String[defaultAcrValues.size()]));
        }
        if (StringUtils.isNotBlank(p_request.getInitiateLoginUri())) {
            p_client.setInitiateLoginUri(p_request.getInitiateLoginUri());
        }
        List<String> postLogoutRedirectUris = p_request.getPostLogoutRedirectUris();
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            postLogoutRedirectUris = new ArrayList<String>(new HashSet<String>(postLogoutRedirectUris)); // Remove repeated elements
            p_client.setPostLogoutRedirectUris(postLogoutRedirectUris.toArray(new String[postLogoutRedirectUris.size()]));
        }

        List<String> requestUris = p_request.getRequestUris();
        if (requestUris != null && !requestUris.isEmpty()) {
            requestUris = new ArrayList<String>(new HashSet<String>(requestUris)); // Remove repeated elements
            p_client.setRequestUris(requestUris.toArray(new String[requestUris.size()]));
        }

        // Federation params
        if (StringUtils.isNotBlank(p_request.getFederationUrl())) {
            p_client.setFederationURI(p_request.getFederationUrl());
        }
        if (StringUtils.isNotBlank(p_request.getFederationId())) {
            p_client.setFederationId(p_request.getFederationId());
        }

        if (p_request.getJsonObject() != null) {
            // Custom params
            putCustomStuffIntoObject(p_client, p_request.getJsonObject());
        }
    }

    @Override
    public Response requestClientUpdate(String requestParams, String clientId, @HeaderParam("Authorization") String authorization, @Context HttpServletRequest httpRequest, @Context SecurityContext securityContext) {
        try {
            log.debug("Attempting to UPDATE client, client_id: {0}, requestParams = {1}, isSecure = {3}",
                    clientId, requestParams, securityContext.isSecure());
            final String accessToken = tokenService.getTokenFromAuthorizationParameter(authorization);
            if (StringUtils.isNotBlank(accessToken) && StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(requestParams)) {
                final RegisterRequest request = RegisterRequest.fromJson(requestParams);
                if (request != null) {
                    if (request.getSubjectType() != null
                            && !ConfigurationFactory.instance().getConfiguration().getSubjectTypesSupported().contains(request.getSubjectType())) {
                        log.debug("Client UPDATE : parameter subject_type is invalid. Returns BAD_REQUEST response.");
                        return Response.status(Response.Status.BAD_REQUEST).
                                entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA)).build();
                    }

                    final Client client = clientService.getClient(clientId, accessToken);
                    if (client != null) {
                        updateClientFromRequestObject(client, request);
                        clientService.merge(client);
                        return Response.status(Response.Status.OK).entity(clientAsEntity(client)).build();
                    } else {
                        log.trace("The Access Token is not valid for the Client ID, returns invalid_token error.");
                        return Response.status(Response.Status.BAD_REQUEST).
                                entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_TOKEN)).build();
                    }
                }
            }

            log.debug("Client UPDATE : parameters are invalid. Returns BAD_REQUEST response.");
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA)).build();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return internalErrorResponse().build();
    }

    @Override
    public Response requestClientRead(String clientId, String authorization, HttpServletRequest httpRequest,
                                      SecurityContext securityContext) {
        String accessToken = tokenService.getTokenFromAuthorizationParameter(authorization);
        log.debug("Attempting to read client: clientId = {0}, registrationAccessToken = {1} isSecure = {2}",
                clientId, accessToken, securityContext.isSecure());
        Response.ResponseBuilder builder = Response.ok();

        try {
            if (ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationEnabled()) {
                if (RegisterParamsValidator.validateParamsClientRead(clientId, accessToken)) {
                    Client client = clientService.getClient(clientId, accessToken);
                    if (client != null) {
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
                ConfigurationFactory.instance().getConfiguration().getRegistrationEndpoint() + "?" +
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
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOKEN_ENDPOINT_AUTH_METHOD.toString(), client.getTokenEndpointAuthMethod());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POLICY_URI.toString(), client.getPolicyUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOS_URI.toString(), client.getTosUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS_URI.toString(), client.getJwksUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS.toString(), client.getJwks());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SECTOR_IDENTIFIER_URI.toString(), client.getSectorIdentifierUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SUBJECT_TYPE.toString(), client.getSubjectType());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_OBJECT_SIGNING_ALG.toString(), client.getRequestObjectSigningAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_SIGNED_RESPONSE_ALG.toString(), client.getUserInfoSignedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_ENCRYPTED_RESPONSE_ALG.toString(), client.getUserInfoEncryptedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_ENCRYPTED_RESPONSE_ENC.toString(), client.getUserInfoEncryptedResponseEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), client.getIdTokenSignedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), client.getIdTokenEncryptedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), client.getIdTokenEncryptedResponseEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, DEFAULT_MAX_AGE.toString(), client.getDefaultMaxAge());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUIRE_AUTH_TIME.toString(), client.getRequireAuthTime());
        Util.addToJSONObjectIfNotNull(responseJsonObject, DEFAULT_ACR_VALUES.toString(), client.getDefaultAcrValues());
        Util.addToJSONObjectIfNotNull(responseJsonObject, INITIATE_LOGIN_URI.toString(), client.getInitiateLoginUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POST_LOGOUT_REDIRECT_URIS.toString(), client.getPostLogoutRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_URIS.toString(), client.getRequestUris());

        // Federation Params
        Util.addToJSONObjectIfNotNull(responseJsonObject, FEDERATION_METADATA_URL.toString(), client.getFederationURI());
        Util.addToJSONObjectIfNotNull(responseJsonObject, FEDERATION_METADATA_ID.toString(), client.getFederationId());

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
    private static void putCustomStuffIntoObject(Client p_client, JSONObject p_requestObject) throws JSONException {
        // custom object class
        final String customOC = ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationCustomObjectClass();
        if (StringUtils.isNotBlank(customOC)) {
            p_client.setCustomObjectClasses(new String[]{customOC});
        }

        // custom attributes (custom attributes must be in custom object class)
        final List<String> attrList = ConfigurationFactory.instance().getConfiguration().getDynamicRegistrationCustomAttributes();
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
}