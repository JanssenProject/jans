/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.register.ws.rs;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.model.metric.MetricType;
import org.gluu.persist.model.base.CustomAttribute;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.model.audit.Action;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.register.RegisterErrorResponseType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.registration.RegisterParamsValidator;
import org.xdi.oxauth.model.token.HandleTokenFactory;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.*;
import org.xdi.oxauth.service.external.ExternalDynamicClientRegistrationService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.*;

import static org.xdi.oxauth.model.register.RegisterRequestParam.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;
import static org.xdi.oxauth.model.util.StringUtils.implode;
import static org.xdi.oxauth.model.util.StringUtils.toList;

/**
 * Implementation for register REST web services.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version March 13, 2019
 */
@Path("/")
public class RegisterRestWebServiceImpl implements RegisterRestWebService {

    @Inject
    private Logger log;
    @Inject
    private ApplicationAuditLogger applicationAuditLogger;
    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ScopeService scopeService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private InumService inumService;
    @Inject
    private ClientService clientService;
    @Inject
    private TokenService tokenService;

    @Inject
    private MetricService metricService;

    @Inject
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Inject
    private RegisterParamsValidator registerParamsValidator;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

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
            final JSONObject requestObject = new JSONObject(requestParams);
            if (requestParams != null && requestObject.has(SOFTWARE_STATEMENT.toString())) {
                Jwt softwareStatement = Jwt.parse(requestObject.getString(SOFTWARE_STATEMENT.toString()));

                // Validate the crypto segment
                String keyId = softwareStatement.getHeader().getKeyId();
                JSONObject jwks = Strings.isNullOrEmpty(softwareStatement.getClaims().getClaimAsString(JWKS_URI.toString())) ?
                        new JSONObject(softwareStatement.getClaims().getClaimAsString(JWKS.toString())) :
                        JwtUtil.getJSONWebKeys(softwareStatement.getClaims().getClaimAsString(JWKS_URI.toString()));
                AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration);
                boolean validSignature = cryptoProvider.verifySignature(softwareStatement.getSigningInput(),
                        softwareStatement.getEncodedSignature(),
                        keyId, jwks, null, softwareStatement.getHeader().getAlgorithm());

                if (!validSignature) {
                    throw new InvalidJwtException("Invalid cryptographic segment in the software statement");
                }

                requestParams = softwareStatement.getClaims().toJsonObject().toString();
            }

            final RegisterRequest r = RegisterRequest.fromJson(requestParams, appConfiguration.getLegacyDynamicRegistrationScopeParam());
            if (requestObject.has(SOFTWARE_STATEMENT.toString())) {
                r.setSoftwareStatement(requestObject.getString(SOFTWARE_STATEMENT.toString()));
            }

            log.info("Attempting to register client: applicationType = {}, clientName = {}, redirectUris = {}, isSecure = {}, sectorIdentifierUri = {}, defaultAcrValues = {}",
                    r.getApplicationType(), r.getClientName(), r.getRedirectUris(), securityContext.isSecure(), r.getSectorIdentifierUri(), r.getDefaultAcrValues());
            log.trace("Registration request = {}", requestParams);

            if (appConfiguration.getDynamicRegistrationEnabled()) {

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
                if (r.getAccessTokenSigningAlg() == null) {
                    r.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm()));
                }

                if (r.getClaimsRedirectUris() != null && !r.getClaimsRedirectUris().isEmpty()) {
                    if (!registerParamsValidator.validateRedirectUris(r.getApplicationType(), r.getSubjectType(), r.getClaimsRedirectUris(), r.getSectorIdentifierUri())) {
                        log.error("Value of one or more claims_redirect_uris is invalid, claims_redirect_uris: " + r.getClaimsRedirectUris());
                        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                .entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLAIMS_REDIRECT_URI))
                                .build());
                    }
                }

                if (registerParamsValidator.validateParamsClientRegister(r.getApplicationType(), r.getSubjectType(),
                        r.getRedirectUris(), r.getSectorIdentifierUri())) {
                    if (!registerParamsValidator.validateRedirectUris(r.getApplicationType(), r.getSubjectType(),
                            r.getRedirectUris(), r.getSectorIdentifierUri())) {
                        builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode());
                        builder.entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_REDIRECT_URI));
                    } else {
                        registerParamsValidator.validateLogoutUri(r.getFrontChannelLogoutUris(), r.getRedirectUris(), errorResponseFactory);

                        String clientsBaseDN = staticConfiguration.getBaseDn().getClients();

                        String inum = inumService.generateClientInum();
                        String generatedClientSecret = UUID.randomUUID().toString();

                        final Client client = new Client();
                        client.setDn("inum=" + inum + "," + clientsBaseDN);
                        client.setClientId(inum);
                        client.setClientSecret(clientService.encryptSecret(generatedClientSecret));
                        client.setRegistrationAccessToken(HandleTokenFactory.generateHandleToken());
                        client.setIdTokenTokenBindingCnf(r.getIdTokenTokenBindingCnf());
                        client.setDeletable(true);

                        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                        client.setClientIdIssuedAt(calendar.getTime());

                        if (appConfiguration.getDynamicRegistrationExpirationTime() > 0) { // #883 : expiration can be -1, mean does not expire
                            calendar.add(Calendar.SECOND, appConfiguration.getDynamicRegistrationExpirationTime());
                            client.setClientSecretExpiresAt(calendar.getTime());
                            client.setExpirationDate(calendar.getTime());
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

                        updateClientFromRequestObject(client, r, false);

                        boolean registerClient = true;
                        if (externalDynamicClientRegistrationService.isEnabled()) {
                            registerClient = externalDynamicClientRegistrationService.executeExternalCreateClientMethods(r, client);
                        }

                        if (registerClient) {
                            Date currentTime = Calendar.getInstance().getTime();
                            client.setLastAccessTime(currentTime);
                            client.setLastLogonTime(currentTime);

                            Boolean persistClientAuthorizations = appConfiguration.getDynamicRegistrationPersistClientAuthorizations();
                            client.setPersistClientAuthorizations(persistClientAuthorizations != null ? persistClientAuthorizations : false);

                            clientService.persist(client);

                            JSONObject jsonObject = getJSONObject(client, appConfiguration.getLegacyDynamicRegistrationScopeParam());
                            builder.entity(jsonObject.toString(4).replace("\\/", "/"));

                            log.info("Client registered: clientId = {}, applicationType = {}, clientName = {}, redirectUris = {}, sectorIdentifierUri = {}",
                                    client.getClientId(), client.getApplicationType(), client.getClientName(), client.getRedirectUris(), client.getSectorIdentifierUri());

                            oAuth2AuditLog.setClientId(client.getClientId());
                            oAuth2AuditLog.setScope(clientScopesToString(client));
                            oAuth2AuditLog.setSuccess(true);
                        } else {
                            log.trace("Client parameters are invalid, returns invalid_request error.");
                            builder = Response.status(Response.Status.BAD_REQUEST).
                                    entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
                        }
                    }
                } else {
                    log.trace("Client parameters are invalid, returns invalid_request error.");
                    builder = Response.status(Response.Status.BAD_REQUEST).
                            entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
                }
            } else {
                log.info("Dynamic client registration is disabled.");
                builder = Response.status(Response.Status.BAD_REQUEST).
                        entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.ACCESS_DENIED));
            }
        } catch (StringEncrypter.EncryptionException e) {
            builder = internalErrorResponse();
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            builder = internalErrorResponse();
            log.error(e.getMessage(), e);
        } catch (WebApplicationException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (InvalidJwtException e) {
            builder = badRequestResponse();
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

    public Response.ResponseBuilder badRequestResponse() {
        return Response.status(Response.Status.BAD_REQUEST).entity(
                errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA));
    }

    // yuriyz - ATTENTION : this method is used for both registration and update client metadata cases, therefore any logic here
    // will be applied for both cases.
    private void updateClientFromRequestObject(Client p_client, RegisterRequest requestObject, boolean update) throws JSONException {
        List<String> redirectUris = requestObject.getRedirectUris();
        if (redirectUris != null && !redirectUris.isEmpty()) {
            redirectUris = new ArrayList<>(new HashSet<>(redirectUris)); // Remove repeated elements
            p_client.setRedirectUris(redirectUris.toArray(new String[redirectUris.size()]));
        }
        List<String> claimsRedirectUris = requestObject.getClaimsRedirectUris();
        if (claimsRedirectUris != null && !claimsRedirectUris.isEmpty()) {
            claimsRedirectUris = new ArrayList<>(new HashSet<>(claimsRedirectUris)); // Remove repeated elements
            p_client.setClaimRedirectUris(claimsRedirectUris.toArray(new String[claimsRedirectUris.size()]));
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

        Set<ResponseType> responseTypeSet = new HashSet<>();
        responseTypeSet.addAll(requestObject.getResponseTypes());

        Set<GrantType> grantTypeSet = new HashSet<>();
        grantTypeSet.addAll(requestObject.getGrantTypes());

        if (responseTypeSet.size() == 0 && grantTypeSet.size() == 0) {
            responseTypeSet.add(ResponseType.CODE);
        }
        if (responseTypeSet.contains(ResponseType.CODE)) {
            grantTypeSet.add(GrantType.AUTHORIZATION_CODE);
            grantTypeSet.add(GrantType.REFRESH_TOKEN);
        }
        if (responseTypeSet.contains(ResponseType.TOKEN) || responseTypeSet.contains(ResponseType.ID_TOKEN)) {
            grantTypeSet.add(GrantType.IMPLICIT);
        }
        if (grantTypeSet.contains(GrantType.AUTHORIZATION_CODE)) {
            responseTypeSet.add(ResponseType.CODE);
            grantTypeSet.add(GrantType.REFRESH_TOKEN);
        }
        if (grantTypeSet.contains(GrantType.IMPLICIT)) {
            responseTypeSet.add(ResponseType.TOKEN);
        }

        Set<Set<ResponseType>> responseTypesSupported = appConfiguration.getResponseTypesSupported();
        Set<GrantType> grantTypesSupported = appConfiguration.getGrantTypesSupported();

        if (!responseTypesSupported.contains(responseTypeSet)) {
            responseTypeSet.clear();
        }

        grantTypeSet.retainAll(grantTypesSupported);

        Set<GrantType> dynamicGrantTypeDefault = appConfiguration.getDynamicGrantTypeDefault();
        grantTypeSet.retainAll(dynamicGrantTypeDefault);

        if (!update || requestObject.getResponseTypes().size() > 0) {
            p_client.setResponseTypes(responseTypeSet.toArray(new ResponseType[responseTypeSet.size()]));
        }
        if (!update) {
            p_client.setGrantTypes(grantTypeSet.toArray(new GrantType[grantTypeSet.size()]));
        } else if (appConfiguration.getEnableClientGrantTypeUpdate() && requestObject.getGrantTypes().size() > 0) {
            p_client.setGrantTypes(grantTypeSet.toArray(new GrantType[grantTypeSet.size()]));
        }

        List<String> contacts = requestObject.getContacts();
        if (contacts != null && !contacts.isEmpty()) {
            contacts = new ArrayList<>(new HashSet<>(contacts)); // Remove repeated elements
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
        if (requestObject.getRptAsJwt() != null) {
            p_client.setRptAsJwt(requestObject.getRptAsJwt());
        }
        if (requestObject.getAccessTokenAsJwt() != null) {
            p_client.setAccessTokenAsJwt(requestObject.getAccessTokenAsJwt());
        }
        if (requestObject.getTlsClientAuthSubjectDn() != null) {
            p_client.getAttributes().setTlsClientAuthSubjectDn(requestObject.getTlsClientAuthSubjectDn());
        }
        if (requestObject.getAccessTokenSigningAlg() != null) {
            p_client.setAccessTokenSigningAlg(requestObject.getAccessTokenSigningAlg().toString());
        }
        if (requestObject.getIdTokenSignedResponseAlg() != null) {
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
            defaultAcrValues = new ArrayList<>(new HashSet<>(defaultAcrValues)); // Remove repeated elements
            p_client.setDefaultAcrValues(defaultAcrValues.toArray(new String[defaultAcrValues.size()]));
        }
        if (StringUtils.isNotBlank(requestObject.getInitiateLoginUri())) {
            p_client.setInitiateLoginUri(requestObject.getInitiateLoginUri());
        }
        List<String> postLogoutRedirectUris = requestObject.getPostLogoutRedirectUris();
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            postLogoutRedirectUris = new ArrayList<>(new HashSet<>(postLogoutRedirectUris)); // Remove repeated elements
            p_client.setPostLogoutRedirectUris(postLogoutRedirectUris.toArray(new String[postLogoutRedirectUris.size()]));
        }

        if (requestObject.getFrontChannelLogoutUris() != null && !requestObject.getFrontChannelLogoutUris().isEmpty()) {
            p_client.setFrontChannelLogoutUri(requestObject.getFrontChannelLogoutUris().toArray(new String[requestObject.getFrontChannelLogoutUris().size()]));
        }
        p_client.setFrontChannelLogoutSessionRequired(requestObject.getFrontChannelLogoutSessionRequired());

        List<String> requestUris = requestObject.getRequestUris();
        if (requestUris != null && !requestUris.isEmpty()) {
            requestUris = new ArrayList<>(new HashSet<>(requestUris)); // Remove repeated elements
            p_client.setRequestUris(requestUris.toArray(new String[requestUris.size()]));
        }

        List<String> authorizedOrigins = requestObject.getAuthorizedOrigins();
        if (authorizedOrigins != null && !authorizedOrigins.isEmpty()) {
            authorizedOrigins = new ArrayList<>(new HashSet<>(authorizedOrigins)); // Remove repeated elements
            p_client.setAuthorizedOrigins(authorizedOrigins.toArray(new String[authorizedOrigins.size()]));
        }

        List<String> scopes = requestObject.getScope();
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

        List<String> claims = requestObject.getClaims();
        if (claims != null && !claims.isEmpty()) {
            List<String> claimsDn = attributeService.getAttributesDn(claims);
            p_client.setClaims(claimsDn.toArray(new String[claimsDn.size()]));
        }

        if (requestObject.getJsonObject() != null) {
            // Custom params
            putCustomStuffIntoObject(p_client, requestObject.getJsonObject());
        }

        if (requestObject.getAccessTokenLifetime() != null) {
            p_client.setAccessTokenLifetime(requestObject.getAccessTokenLifetime());
        }

        if (StringUtils.isNotBlank(requestObject.getSoftwareId())) {
            p_client.setSoftwareId(requestObject.getSoftwareId());
        }
        if (StringUtils.isNotBlank(requestObject.getSoftwareVersion())) {
            p_client.setSoftwareVersion(requestObject.getSoftwareVersion());
        }
        if (StringUtils.isNotBlank(requestObject.getSoftwareStatement())) {
            p_client.setSoftwareStatement(requestObject.getSoftwareStatement());
        }
    }

    @Override
    public Response requestClientUpdate(String requestParams, String clientId, @HeaderParam("Authorization") String authorization, @Context HttpServletRequest httpRequest, @Context SecurityContext securityContext) {
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.CLIENT_UPDATE);
        oAuth2AuditLog.setClientId(clientId);
        try {
            log.debug("Attempting to UPDATE client, client_id: {}, requestParams = {}, isSecure = {}",
                    clientId, requestParams, securityContext.isSecure());
            final String accessToken = tokenService.getTokenFromAuthorizationParameter(authorization);

            if (StringUtils.isNotBlank(accessToken) && StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(requestParams)) {
                final RegisterRequest request = RegisterRequest.fromJson(requestParams, appConfiguration.getLegacyDynamicRegistrationScopeParam());
                if (request != null) {
                    boolean redirectUrisValidated = true;
                    if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
                        redirectUrisValidated = registerParamsValidator.validateRedirectUris(request.getApplicationType(), request.getSubjectType(),
                                request.getRedirectUris(), request.getSectorIdentifierUri());
                    }

                    if (redirectUrisValidated) {
                        if (request.getSubjectType() != null
                                && !appConfiguration.getSubjectTypesSupported().contains(request.getSubjectType().toString())) {
                            log.debug("Client UPDATE : parameter subject_type is invalid. Returns BAD_REQUEST response.");
                            applicationAuditLogger.sendMessage(oAuth2AuditLog);
                            return Response.status(Response.Status.BAD_REQUEST).
                                    entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA)).build();
                        }

                        final Client client = clientService.getClient(clientId, accessToken);
                        if (client != null) {
                            updateClientFromRequestObject(client, request, true);

                            boolean updateClient = true;
                            if (externalDynamicClientRegistrationService.isEnabled()) {
                                updateClient = externalDynamicClientRegistrationService.executeExternalUpdateClientMethods(request, client);
                            }

                            if (updateClient) {
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
        log.debug("Attempting to read client: clientId = {}, registrationAccessToken = {} isSecure = {}",
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
        final JSONObject jsonObject = getJSONObject(p_client, appConfiguration.getLegacyDynamicRegistrationScopeParam());
        return jsonObject.toString(4).replace("\\/", "/");
    }

    private JSONObject getJSONObject(Client client, boolean authorizationRequestCustomAllowedParameters) throws JSONException, StringEncrypter.EncryptionException {
        JSONObject responseJsonObject = new JSONObject();

        Util.addToJSONObjectIfNotNull(responseJsonObject, RegisterResponseParam.CLIENT_ID.toString(), client.getClientId());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_SECRET.toString(), clientService.decryptSecret(client.getClientSecret()));
        Util.addToJSONObjectIfNotNull(responseJsonObject, RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString(), client.getRegistrationAccessToken());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REGISTRATION_CLIENT_URI.toString(),
                appConfiguration.getRegistrationEndpoint() + "?" +
                        RegisterResponseParam.CLIENT_ID.toString() + "=" + client.getClientId());
        responseJsonObject.put(CLIENT_ID_ISSUED_AT.toString(), client.getClientIdIssuedAt().getTime() / 1000);
        responseJsonObject.put(CLIENT_SECRET_EXPIRES_AT.toString(), client.getClientSecretExpiresAt() != null && client.getClientSecretExpiresAt().getTime() > 0 ?
                client.getClientSecretExpiresAt().getTime() / 1000 : 0);

        Util.addToJSONObjectIfNotNull(responseJsonObject, REDIRECT_URIS.toString(), client.getRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLAIMS_REDIRECT_URIS.toString(), client.getClaimRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, RESPONSE_TYPES.toString(), ResponseType.toStringArray(client.getResponseTypes()));
        Util.addToJSONObjectIfNotNull(responseJsonObject, GRANT_TYPES.toString(), GrantType.toStringArray(client.getGrantTypes()));
        Util.addToJSONObjectIfNotNull(responseJsonObject, APPLICATION_TYPE.toString(), client.getApplicationType());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CONTACTS.toString(), client.getContacts());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_NAME.toString(), client.getClientName());
        Util.addToJSONObjectIfNotNull(responseJsonObject, LOGO_URI.toString(), client.getLogoUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_URI.toString(), client.getClientUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POLICY_URI.toString(), client.getPolicyUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOS_URI.toString(), client.getTosUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS_URI.toString(), client.getJwksUri());
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
        Util.addToJSONObjectIfNotNull(responseJsonObject, AUTHORIZED_ORIGINS.toString(), client.getAuthorizedOrigins());
        Util.addToJSONObjectIfNotNull(responseJsonObject, RPT_AS_JWT.toString(), client.isRptAsJwt());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TLS_CLIENT_AUTH_SUBJECT_DN.toString(), client.getAttributes().getTlsClientAuthSubjectDn());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ACCESS_TOKEN_AS_JWT.toString(), client.isAccessTokenAsJwt());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ACCESS_TOKEN_SIGNING_ALG.toString(), client.getAccessTokenSigningAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ACCESS_TOKEN_LIFETIME.toString(), client.getAccessTokenLifetime());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_ID.toString(), client.getSoftwareId());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_VERSION.toString(), client.getSoftwareVersion());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_STATEMENT.toString(), client.getSoftwareStatement());

        if (!Util.isNullOrEmpty(client.getJwks())) {
            Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS.toString(), new JSONObject(client.getJwks()));
        }

        // Logout params
        Util.addToJSONObjectIfNotNull(responseJsonObject, FRONT_CHANNEL_LOGOUT_URI.toString(), client.getFrontChannelLogoutUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString(), client.getFrontChannelLogoutSessionRequired());

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

        if (authorizationRequestCustomAllowedParameters) {
            Util.addToJSONObjectIfNotNull(responseJsonObject, SCOPES.toString(), scopeNames);
        } else {
            Util.addToJSONObjectIfNotNull(responseJsonObject, SCOPE.toString(), implode(scopeNames, " "));
        }

        String[] claimNames = null;
        String[] claimDns = client.getClaims();
        if (claimDns != null) {
            claimNames = new String[claimDns.length];
            for (int i = 0; i < claimDns.length; i++) {
                GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDns[i]);
                claimNames[i] = gluuAttribute.getOxAuthClaimName();
            }
        }

        if (claimNames != null && claimNames.length > 0) {
            Util.addToJSONObjectIfNotNull(responseJsonObject, CLAIMS.toString(), implode(claimNames, " "));
        }

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
            for (String attr : attrList) {
                if (p_requestObject.has(attr)) {
                    final JSONArray parameterValuesJsonArray = p_requestObject.optJSONArray(attr);
                    final List<String> parameterValues = parameterValuesJsonArray != null ?
                            toList(parameterValuesJsonArray) :
                            Arrays.asList(p_requestObject.getString(attr));
                    if (parameterValues != null && !parameterValues.isEmpty()) {
                        try {
                            boolean processed = processApplicationAttributes(p_client, attr, parameterValues);
                            if (!processed) {
                                p_client.getCustomAttributes().add(new CustomAttribute(attr, parameterValues));
                            }
                        } catch (Exception e) {
                            log.debug(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private boolean processApplicationAttributes(Client p_client, String attr, final List<String> parameterValues) {
        if (StringHelper.equalsIgnoreCase("oxAuthTrustedClient", attr)) {
            boolean trustedClient = StringHelper.toBoolean(parameterValues.get(0), false);
            p_client.setTrustedClient(trustedClient);

            return true;
        } else if (StringHelper.equalsIgnoreCase("oxIncludeClaimsInIdToken", attr)) {
            boolean includeClaimsInIdToken = StringHelper.toBoolean(parameterValues.get(0), false);
            p_client.setIncludeClaimsInIdToken(includeClaimsInIdToken);

            return true;
        }

        return false;
    }

    private String clientScopesToString(Client client) {
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