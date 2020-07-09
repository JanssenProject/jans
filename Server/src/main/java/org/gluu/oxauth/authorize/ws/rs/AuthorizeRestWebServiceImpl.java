/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.authorize.ws.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.ciba.CIBAPingCallbackService;
import org.gluu.oxauth.ciba.CIBAPushTokenDeliveryService;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.authorize.*;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.config.ConfigurationFactory;
import org.gluu.oxauth.model.config.Constants;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.binding.TokenBindingMessage;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.exception.AcrChangedException;
import org.gluu.oxauth.model.exception.InvalidSessionStateException;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.ldap.ClientAuthorization;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.token.JwrService;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.security.Identity;
import org.gluu.oxauth.service.*;
import org.gluu.oxauth.service.ciba.CibaRequestService;
import org.gluu.oxauth.service.external.ExternalPostAuthnService;
import org.gluu.oxauth.service.external.context.ExternalPostAuthnContext;
import org.gluu.oxauth.service.external.session.SessionEvent;
import org.gluu.oxauth.service.external.session.SessionEventType;
import org.gluu.oxauth.util.QueryStringDecoder;
import org.gluu.oxauth.util.RedirectUri;
import org.gluu.oxauth.util.RedirectUtil;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static org.gluu.oxauth.model.util.StringUtils.implode;

/**
 * Implementation for request authorization through REST web services.
 *
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
@Path("/")
public class AuthorizeRestWebServiceImpl implements AuthorizeRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ClientService clientService;

    @Inject
    private UserService userService;

    @Inject
    private Identity identity;

    @Inject
    private AuthenticationFilterService authenticationFilterService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject CookieService cookieService;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private ClientAuthorizationsService clientAuthorizationsService;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ConfigurationFactory сonfigurationFactory;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Inject
    private CIBAPushTokenDeliveryService cibaPushTokenDeliveryService;

    @Inject
    private CIBAPingCallbackService cibaPingCallbackService;

    @Inject
    private ExternalPostAuthnService externalPostAuthnService;

    @Inject
    private CibaRequestService cibaRequestService;

    @Inject
    private DeviceAuthorizationService deviceAuthorizationService;

    @Context
    private HttpServletRequest servletRequest;

    @Override
    public Response requestAuthorizationGet(
            String scope, String responseType, String clientId, String redirectUri, String state, String responseMode,
            String nonce, String display, String prompt, Integer maxAge, String uiLocales, String idTokenHint,
            String loginHint, String acrValues, String amrValues, String request, String requestUri,
            String requestSessionId, String sessionId, String originHeaders, String codeChallenge,
            String codeChallengeMethod, String customResponseHeaders, String claims, String authReqId, String userCode,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {
        return requestAuthorization(scope, responseType, clientId, redirectUri, state, responseMode, nonce, display,
                prompt, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri,
                requestSessionId, sessionId, HttpMethod.GET, originHeaders, codeChallenge, codeChallengeMethod,
                customResponseHeaders, claims, authReqId, userCode, httpRequest, httpResponse, securityContext);
    }

    @Override
    public Response requestAuthorizationPost(
            String scope, String responseType, String clientId, String redirectUri, String state, String responseMode,
            String nonce, String display, String prompt, Integer maxAge, String uiLocales, String idTokenHint,
            String loginHint, String acrValues, String amrValues, String request, String requestUri,
            String requestSessionId, String sessionId, String originHeaders,
            String codeChallenge, String codeChallengeMethod, String customResponseHeaders, String claims,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {
        return requestAuthorization(scope, responseType, clientId, redirectUri, state, responseMode, nonce, display,
                prompt, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri,
                requestSessionId, sessionId, HttpMethod.POST, originHeaders, codeChallenge, codeChallengeMethod,
                customResponseHeaders, claims, null, null, httpRequest, httpResponse, securityContext);
    }

    private Response requestAuthorization(
            String scope, String responseType, String clientId, String redirectUri, String state, String respMode,
            String nonce, String display, String prompt, Integer maxAge, String uiLocalesStr, String idTokenHint,
            String loginHint, String acrValuesStr, String amrValuesStr, String request, String requestUri, String requestSessionId,
            String sessionId, String method, String originHeaders, String codeChallenge, String codeChallengeMethod,
            String customRespHeaders, String claims, String authReqId, String userCode,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {
        scope = ServerUtil.urlDecode(scope); // it may be encoded in uma case

        String tokenBindingHeader = httpRequest.getHeader("Sec-Token-Binding");

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.USER_AUTHORIZATION);
        oAuth2AuditLog.setClientId(clientId);
        oAuth2AuditLog.setScope(scope);

        // ATTENTION : please do not add more parameter in this debug method because it will not work with Seam 2.2.2.Final ,
        // there is limit of 10 parameters (hardcoded), see: org.jboss.seam.core.Interpolator#interpolate
        log.debug("Attempting to request authorization: "
                        + "responseType = {}, clientId = {}, scope = {}, redirectUri = {}, nonce = {}, "
                        + "state = {}, request = {}, isSecure = {}, requestSessionId = {}, sessionId = {}",
                responseType, clientId, scope, redirectUri, nonce,
                state, request, securityContext.isSecure(), requestSessionId, sessionId);

        log.debug("Attempting to request authorization: "
                        + "acrValues = {}, amrValues = {}, originHeaders = {}, codeChallenge = {}, codeChallengeMethod = {}, "
                        + "customRespHeaders = {}, claims = {}, tokenBindingHeader = {}",
                acrValuesStr, amrValuesStr, originHeaders, codeChallenge, codeChallengeMethod, customRespHeaders, claims, tokenBindingHeader);

        ResponseBuilder builder = Response.ok();

        List<String> uiLocales = Util.splittedStringAsList(uiLocalesStr, " ");
        List<ResponseType> responseTypes = ResponseType.fromString(responseType, " ");
        List<Prompt> prompts = Prompt.fromString(prompt, " ");
        List<String> acrValues = Util.splittedStringAsList(acrValuesStr, " ");
        List<String> amrValues = Util.splittedStringAsList(amrValuesStr, " ");
        ResponseMode responseMode = ResponseMode.getByValue(respMode);

        Map<String, String> customParameters = requestParameterService.getCustomParameters(
                QueryStringDecoder.decode(httpRequest.getQueryString()));

        SessionId sessionUser = identity.getSessionId();
        User user = sessionIdService.getUser(sessionUser);

        try {
            Map<String, String> customResponseHeaders = Util.jsonObjectArrayStringAsMap(customRespHeaders);

            checkAcrChanged(acrValuesStr, prompts, sessionUser);
            updateSessionForROPC(httpRequest, sessionUser);

            Client client = authorizeRestWebServiceValidator.validateClient(clientId, state);
            redirectUri = authorizeRestWebServiceValidator.validateRedirectUri(client, redirectUri, state, userCode, httpRequest);
            RedirectUriResponse redirectUriResponse = new RedirectUriResponse(new RedirectUri(redirectUri, responseTypes, responseMode), state, httpRequest, errorResponseFactory);
            redirectUriResponse.setFapiCompatible(appConfiguration.getFapiCompatibility());

            Set<String> scopes = scopeChecker.checkScopesPolicy(client, scope);

            JwtAuthorizationRequest jwtRequest = null;
            if (StringUtils.isNotBlank(request) || StringUtils.isNotBlank(requestUri)) {
                try {
                    jwtRequest = JwtAuthorizationRequest.createJwtRequest(request, requestUri, client, redirectUriResponse, cryptoProvider, appConfiguration);

                    if (jwtRequest == null) {
                        throw createInvalidJwtRequestException(redirectUriResponse, "Failed to parse jwt.");
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getState())) {
                        state = jwtRequest.getState();
                        redirectUriResponse.setState(state);
                    }
                    if (appConfiguration.getFapiCompatibility() && StringUtils.isBlank(jwtRequest.getState())) {
                        state = ""; // #1250 - FAPI : discard state if in JWT we don't have state
                        redirectUriResponse.setState("");
                    }

                    authorizeRestWebServiceValidator.validateRequestObject(jwtRequest, redirectUriResponse);

                    // MUST be equal
                    if (!jwtRequest.getResponseTypes().containsAll(responseTypes) || !responseTypes.containsAll(jwtRequest.getResponseTypes())) {
                        throw createInvalidJwtRequestException(redirectUriResponse, "The responseType parameter is not the same in the JWT");
                    }
                    if (StringUtils.isBlank(jwtRequest.getClientId()) || !jwtRequest.getClientId().equals(clientId)) {
                        throw createInvalidJwtRequestException(redirectUriResponse, "The clientId parameter is not the same in the JWT");
                    }

                    // JWT wins
                    if (!jwtRequest.getScopes().isEmpty()) {
                        if (!scopes.contains("openid")) { // spec: Even if a scope parameter is present in the Request Object value, a scope parameter MUST always be passed using the OAuth 2.0 request syntax containing the openid scope value
                            throw new WebApplicationException(Response
                                    .status(Response.Status.BAD_REQUEST)
                                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_SCOPE, state, "scope parameter does not contain openid value which is required."))
                                    .build());
                        }
                        scopes = scopeChecker.checkScopesPolicy(client, Lists.newArrayList(jwtRequest.getScopes()));
                    }
                    if (jwtRequest.getRedirectUri() != null && !jwtRequest.getRedirectUri().equals(redirectUri)) {
                        throw createInvalidJwtRequestException(redirectUriResponse, "The redirect_uri parameter is not the same in the JWT");
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getNonce())) {
                        nonce = jwtRequest.getNonce();
                    }
                    if (jwtRequest.getDisplay() != null && StringUtils.isNotBlank(jwtRequest.getDisplay().getParamName())) {
                        display = jwtRequest.getDisplay().getParamName();
                    }
                    if (!jwtRequest.getPrompts().isEmpty()) {
                        prompts = Lists.newArrayList(jwtRequest.getPrompts());
                    }

                    final IdTokenMember idTokenMember = jwtRequest.getIdTokenMember();
                    if (idTokenMember != null) {
                        if (idTokenMember.getMaxAge() != null) {
                            maxAge = idTokenMember.getMaxAge();
                        }
                        final Claim acrClaim = idTokenMember.getClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
                        if (acrClaim != null && acrClaim.getClaimValue() != null) {
                            acrValuesStr = acrClaim.getClaimValue().getValueAsString();
                            acrValues = Util.splittedStringAsList(acrValuesStr, " ");
                        }

                        Claim userIdClaim = idTokenMember.getClaim(JwtClaimName.SUBJECT_IDENTIFIER);
                        if (userIdClaim != null && userIdClaim.getClaimValue() != null
                                && userIdClaim.getClaimValue().getValue() != null) {
                            String userIdClaimValue = userIdClaim.getClaimValue().getValue();

                            if (user != null) {
                                String userId = user.getUserId();

                                if (!userId.equalsIgnoreCase(userIdClaimValue)) {
                                    builder = redirectUriResponse.createErrorBuilder(AuthorizeErrorResponseType.USER_MISMATCHED);
                                    applicationAuditLogger.sendMessage(oAuth2AuditLog);
                                    return builder.build();
                                }
                            }
                        }
                    }
                    requestParameterService.getCustomParameters(jwtRequest, customParameters);
                } catch (WebApplicationException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Invalid JWT authorization request. Message : " + e.getMessage(), e);
                    throw createInvalidJwtRequestException(redirectUriResponse, "Invalid JWT authorization request");
                }
            }
            if (!cibaRequestService.hasCibaCompatibility(client)) {
                if (appConfiguration.getFapiCompatibility() && jwtRequest == null) {
                    throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST);
                }
                authorizeRestWebServiceValidator.validateRequestJwt(request, requestUri, redirectUriResponse);
            }
            authorizeRestWebServiceValidator.validate(responseTypes, prompts, nonce, state, redirectUri, httpRequest, client, responseMode);

            if (CollectionUtils.isEmpty(acrValues) && !ArrayUtils.isEmpty(client.getDefaultAcrValues())) {
                acrValues = Lists.newArrayList(client.getDefaultAcrValues());
            }

            if (scopes.contains(ScopeConstants.OFFLINE_ACCESS)) {
                if (!responseTypes.contains(ResponseType.CODE)) {
                    log.trace("Removed (ignored) offline_scope. Can't find `code` in response_type which is required.");
                    scopes.remove(ScopeConstants.OFFLINE_ACCESS);
                }

                if (scopes.contains(ScopeConstants.OFFLINE_ACCESS) && !prompts.contains(Prompt.CONSENT)) {
                    log.error("Removed offline_access. Can't find prompt=consent. Consent is required for offline_access.");
                    scopes.remove(ScopeConstants.OFFLINE_ACCESS);
                }
            }

            final boolean isResponseTypeValid = AuthorizeParamsValidator.validateResponseTypes(responseTypes, client)
                    && AuthorizeParamsValidator.validateGrantType(responseTypes, client.getGrantTypes(), appConfiguration.getGrantTypesSupported());

            if (!isResponseTypeValid) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNSUPPORTED_RESPONSE_TYPE, state, ""))
                        .build());
            }

            AuthorizationGrant authorizationGrant = null;

            if (user == null) {
                identity.logout();
                if (prompts.contains(Prompt.NONE)) {
                    if (authenticationFilterService.isEnabled()) {
                        Map<String, String> params;
                        if (method.equals(HttpMethod.GET)) {
                            params = QueryStringDecoder.decode(httpRequest.getQueryString());
                        } else {
                            params = getGenericRequestMap(httpRequest);
                        }

                        String userDn = authenticationFilterService.processAuthenticationFilters(params);
                        if (userDn != null) {
                            Map<String, String> genericRequestMap = getGenericRequestMap(httpRequest);

                            Map<String, String> parameterMap = Maps.newHashMap(genericRequestMap);
                            Map<String, String> requestParameterMap = requestParameterService.getAllowedParameters(parameterMap);

                            sessionUser = sessionIdService.generateAuthenticatedSessionId(httpRequest, userDn, prompt);
                            sessionUser.setSessionAttributes(requestParameterMap);

                            cookieService.createSessionIdCookie(sessionUser, httpRequest, httpResponse, false);
                            sessionIdService.updateSessionId(sessionUser);
                            user = userService.getUserByDn(sessionUser.getUserDn());
                        } else {
                            builder = redirectUriResponse.createErrorBuilder(AuthorizeErrorResponseType.LOGIN_REQUIRED);
                            applicationAuditLogger.sendMessage(oAuth2AuditLog);
                            return builder.build();
                        }
                    } else {
                        builder = redirectUriResponse.createErrorBuilder(AuthorizeErrorResponseType.LOGIN_REQUIRED);
                        applicationAuditLogger.sendMessage(oAuth2AuditLog);
                        return builder.build();
                    }
                } else {
                    if (prompts.contains(Prompt.LOGIN)) {
                        unauthenticateSession(sessionId, httpRequest);
                        sessionId = null;
                        prompts.remove(Prompt.LOGIN);
                    }

                    return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                            redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                            idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                            codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                            customParameters, oAuth2AuditLog, httpRequest);
                }
            }

            boolean validAuthenticationMaxAge = authorizeRestWebServiceValidator.validateAuthnMaxAge(maxAge, sessionUser, client);
            if (!validAuthenticationMaxAge) {
                unauthenticateSession(sessionId, httpRequest);
                sessionId = null;

                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                        customParameters, oAuth2AuditLog, httpRequest);
            }

            oAuth2AuditLog.setUsername(user.getUserId());

            ExternalPostAuthnContext postAuthnContext = new ExternalPostAuthnContext(client, sessionUser, httpRequest, httpResponse);
            final boolean forceReAuthentication = externalPostAuthnService.externalForceReAuthentication(client, postAuthnContext);
            if (forceReAuthentication) {
                unauthenticateSession(sessionId, httpRequest);
                sessionId = null;

                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                        customParameters, oAuth2AuditLog, httpRequest);
            }

            final boolean forceAuthorization = externalPostAuthnService.externalForceAuthorization(client, postAuthnContext);
            if (forceAuthorization) {
                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                        customParameters, oAuth2AuditLog, httpRequest);
            }

            ClientAuthorization clientAuthorization = clientAuthorizationsService.find(
                    user.getAttribute("inum"),
                    client.getClientId());
            if (scopes.size() > 0) {
                if (prompts.contains(Prompt.CONSENT)) {
                    return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                            redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                            idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                            codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                            customParameters, oAuth2AuditLog, httpRequest);
                }
                if (client.getTrustedClient()) {
                    sessionUser.addPermission(clientId, true);
                    sessionIdService.updateSessionId(sessionUser);
                } else if (clientAuthorization != null && clientAuthorization.getScopes() != null) {
                    log.trace("ClientAuthorization - scope: " + scope + ", dn: " + clientAuthorization.getDn() + ", requestedScope: " + scopes);
                    if (Arrays.asList(clientAuthorization.getScopes()).containsAll(scopes)) {
                        sessionUser.addPermission(clientId, true);
                        sessionIdService.updateSessionId(sessionUser);
                    } else {
                        return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                                redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                                idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                                codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                                customParameters, oAuth2AuditLog, httpRequest);
                    }
                }
            }

            if (prompts.contains(Prompt.LOGIN)) {

                //  workaround for #1030 - remove only authenticated session, for set up acr we set it unauthenticated and then drop in AuthorizeAction
                if (identity.getSessionId().getState() == SessionIdState.AUTHENTICATED) {
                    unauthenticateSession(sessionId, httpRequest);
                }
                sessionId = null;
                prompts.remove(Prompt.LOGIN);

                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                        customParameters, oAuth2AuditLog, httpRequest);
            }

            if (prompts.contains(Prompt.CONSENT) || !sessionUser.isPermissionGrantedForClient(clientId)) {
                clientAuthorizationsService.clearAuthorizations(clientAuthorization,
                        client.getPersistClientAuthorizations());

                prompts.remove(Prompt.CONSENT);

                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                        customParameters, oAuth2AuditLog, httpRequest);
            }

            if (prompts.contains(Prompt.SELECT_ACCOUNT)) {
                return redirectToSelectAccountPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode,
                        customParameters, oAuth2AuditLog, httpRequest);
            }

            AuthorizationCode authorizationCode = null;
            if (responseTypes.contains(ResponseType.CODE)) {
                authorizationGrant = authorizationGrantList.createAuthorizationCodeGrant(user, client,
                        sessionUser.getAuthenticationTime());
                authorizationGrant.setNonce(nonce);
                authorizationGrant.setJwtAuthorizationRequest(jwtRequest);
                authorizationGrant.setTokenBindingHash(TokenBindingMessage.getTokenBindingIdHashFromTokenBindingMessage(tokenBindingHeader, client.getIdTokenTokenBindingCnf()));
                authorizationGrant.setScopes(scopes);
                authorizationGrant.setCodeChallenge(codeChallenge);
                authorizationGrant.setCodeChallengeMethod(codeChallengeMethod);
                authorizationGrant.setClaims(claims);

                // Store acr_values
                authorizationGrant.setAcrValues(acrValuesStr);
                authorizationGrant.setSessionDn(sessionUser.getDn());
                authorizationGrant.save(); // call save after object modification!!!

                authorizationCode = authorizationGrant.getAuthorizationCode();

                redirectUriResponse.getRedirectUri().addResponseParameter("code", authorizationCode.getCode());
            }

            AccessToken newAccessToken = null;
            if (responseTypes.contains(ResponseType.TOKEN)) {
                if (authorizationGrant == null) {
                    authorizationGrant = authorizationGrantList.createImplicitGrant(user, client,
                            sessionUser.getAuthenticationTime());
                    authorizationGrant.setNonce(nonce);
                    authorizationGrant.setJwtAuthorizationRequest(jwtRequest);
                    authorizationGrant.setScopes(scopes);
                    authorizationGrant.setClaims(claims);

                    // Store acr_values
                    authorizationGrant.setAcrValues(acrValuesStr);
                    authorizationGrant.setSessionDn(sessionUser.getDn());
                    authorizationGrant.save(); // call save after object modification!!!
                }
                newAccessToken = authorizationGrant.createAccessToken(httpRequest.getHeader("X-ClientCert"), new ExecutionContext(httpRequest, httpResponse));

                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.ACCESS_TOKEN, newAccessToken.getCode());
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.TOKEN_TYPE, newAccessToken.getTokenType().toString());
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.EXPIRES_IN, newAccessToken.getExpiresIn() + "");
            }

            if (responseTypes.contains(ResponseType.ID_TOKEN)) {
                boolean includeIdTokenClaims = Boolean.TRUE.equals(appConfiguration.getLegacyIdTokenClaims());
                if (authorizationGrant == null) {
                    includeIdTokenClaims = true;
                    authorizationGrant = authorizationGrantList.createImplicitGrant(user, client,
                            sessionUser.getAuthenticationTime());
                    authorizationGrant.setNonce(nonce);
                    authorizationGrant.setJwtAuthorizationRequest(jwtRequest);
                    authorizationGrant.setScopes(scopes);
                    authorizationGrant.setClaims(claims);

                    // Store authentication acr values
                    authorizationGrant.setAcrValues(acrValuesStr);
                    authorizationGrant.setSessionDn(sessionUser.getDn());
                    authorizationGrant.save(); // call save after object modification, call is asynchronous!!!
                }
                IdToken idToken = authorizationGrant.createIdToken(
                        nonce, authorizationCode, newAccessToken, null,
                        state, authorizationGrant, includeIdTokenClaims,
                        JwrService.wrapWithSidFunction(TokenBindingMessage.createIdTokenTokingBindingPreprocessing(tokenBindingHeader, client.getIdTokenTokenBindingCnf()), sessionUser.getId()));

                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.ID_TOKEN, idToken.getCode());
            }

            if (authorizationGrant != null && StringHelper.isNotEmpty(acrValuesStr) && !appConfiguration.getFapiCompatibility()) {
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.ACR_VALUES, acrValuesStr);
            }

            if (sessionUser.getId() == null) {
                final SessionId newSessionUser = sessionIdService.generateAuthenticatedSessionId(httpRequest, sessionUser.getUserDn(), prompt);
                String newSessionId = newSessionUser.getId();
                sessionUser.setId(newSessionId);
                log.trace("newSessionId = {}", newSessionId);
            }
            if (!appConfiguration.getFapiCompatibility()) {
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SESSION_ID, sessionUser.getId());
            }
            redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SESSION_STATE, sessionIdService.computeSessionState(sessionUser, clientId, redirectUri));
            redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.STATE, state);
            if (scope != null && !scope.isEmpty() && authorizationGrant != null && !appConfiguration.getFapiCompatibility()) {
                scope = authorizationGrant.checkScopesPolicy(scope);

                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SCOPE, scope);
            }

            clientService.updateAccessTime(client, false);
            oAuth2AuditLog.setSuccess(true);

            builder = RedirectUtil.getRedirectResponseBuilder(redirectUriResponse.getRedirectUri(), httpRequest);

            if (appConfiguration.getCustomHeadersWithAuthorizationResponse()) {
                for (String key : customResponseHeaders.keySet()) {
                    builder.header(key, customResponseHeaders.get(key));
                }
            }

            if (StringUtils.isNotBlank(authReqId)) {
                runCiba(authReqId, httpRequest, httpResponse);
            }
            if (StringUtils.isNotBlank(userCode)) {
                processDeviceAuthorization(userCode, user, httpRequest, httpResponse);
            }
        } catch (WebApplicationException e) {
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            log.error(e.getMessage(), e);
            throw e;
        } catch (AcrChangedException e) { // Acr changed
            log.error("ACR is changed, please provide a supported and enabled acr value");
            log.error(e.getMessage(), e);

            RedirectUri redirectUriResponse = new RedirectUri(redirectUri, responseTypes, responseMode);
            redirectUriResponse.parseQueryString(errorResponseFactory.getErrorAsQueryString(
                    AuthorizeErrorResponseType.SESSION_SELECTION_REQUIRED, state));
            redirectUriResponse.addResponseParameter("hint", "Use prompt=login in order to alter existing session.");
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            return RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, httpRequest).build();
        } catch (EntryPersistenceException e) { // Invalid clientId
            builder = Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, ""))
                    .type(MediaType.APPLICATION_JSON_TYPE);
            log.error(e.getMessage(), e);
        } catch (InvalidSessionStateException ex) { // Allow to handle it via GlobalExceptionHandler
            throw ex;
        } catch (Exception e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        }

        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    private void runCiba(String authReqId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CibaRequestCacheControl cibaRequest = cibaRequestService.getCibaRequest(authReqId);

        if (cibaRequest == null || cibaRequest.getStatus() == CibaRequestStatus.EXPIRED) {
            log.trace("User responded too late and the grant {} has expired, {}", authReqId, cibaRequest);
            return;
        }

        cibaRequestService.removeCibaRequest(authReqId);
        CIBAGrant cibaGrant = authorizationGrantList.createCIBAGrant(cibaRequest);

        RefreshToken refreshToken = cibaGrant.createRefreshToken();
        log.debug("Issuing refresh token: {}", refreshToken.getCode());

        AccessToken accessToken = cibaGrant.createAccessToken(httpRequest.getHeader("X-ClientCert"), new ExecutionContext(httpRequest, httpResponse));
        log.debug("Issuing access token: {}", accessToken.getCode());

        IdToken idToken = cibaGrant.createIdToken(
                null, null, accessToken, refreshToken,
                null, cibaGrant, false, null);

        cibaGrant.setTokensDelivered(true);
        cibaGrant.save();

        if (cibaRequest.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH) {
            cibaPushTokenDeliveryService.pushTokenDelivery(
                    cibaGrant.getAuthReqId(),
                    cibaGrant.getClient().getBackchannelClientNotificationEndpoint(),
                    cibaRequest.getClientNotificationToken(),
                    accessToken.getCode(),
                    refreshToken.getCode(),
                    idToken.getCode(),
                    accessToken.getExpiresIn()
            );
        } else if (cibaGrant.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PING) {
            cibaGrant.setTokensDelivered(false);
            cibaGrant.save();

            cibaPingCallbackService.pingCallback(
                    cibaGrant.getAuthReqId(),
                    cibaGrant.getClient().getBackchannelClientNotificationEndpoint(),
                    cibaRequest.getClientNotificationToken()
            );
        } else if (cibaGrant.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.POLL) {
            cibaGrant.setTokensDelivered(false);
            cibaGrant.save();
        }
    }

    private WebApplicationException createInvalidJwtRequestException(RedirectUriResponse redirectUriResponse, String reason) {
        if (appConfiguration.getFapiCompatibility()) {
            log.debug(reason); // in FAPI case log reason but don't send it since it's `reason` is not known.
            return redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }
        return redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, reason);
    }

    private void updateSessionForROPC(HttpServletRequest httpRequest, SessionId sessionUser) {
        if (sessionUser == null) {
            return;
        }

        Map<String, String> sessionAttributes = sessionUser.getSessionAttributes();
        String authorizedGrant = sessionUser.getSessionAttributes().get(Constants.AUTHORIZED_GRANT);
        if (StringHelper.isNotEmpty(authorizedGrant) && GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS == GrantType.fromString(authorizedGrant)) {
            // Remove from session to avoid execution on next AuthZ request
            sessionAttributes.remove(Constants.AUTHORIZED_GRANT);

            // Reset AuthZ parameters
            Map<String, String> parameterMap = getGenericRequestMap(httpRequest);
            Map<String, String> requestParameterMap = requestParameterService.getAllowedParameters(parameterMap);
            sessionAttributes.putAll(requestParameterMap);
            sessionIdService.updateSessionId(sessionUser, true, true, true);
        }
    }

    private void checkAcrChanged(String acrValuesStr, List<Prompt> prompts, SessionId sessionUser) throws AcrChangedException {
        try {
            sessionIdService.assertAuthenticatedSessionCorrespondsToNewRequest(sessionUser, acrValuesStr);
        } catch (AcrChangedException e) { // Acr changed
            //See https://github.com/GluuFederation/oxTrust/issues/797
            if (e.isForceReAuthentication()) {
                if (!prompts.contains(Prompt.LOGIN)) {
                    log.info("ACR is changed, adding prompt=login to prompts");
                    prompts.add(Prompt.LOGIN);

                    sessionUser.setState(SessionIdState.UNAUTHENTICATED);
                    sessionUser.getSessionAttributes().put("prompt", org.gluu.oxauth.model.util.StringUtils.implode(prompts, " "));
                    sessionIdService.persistSessionId(sessionUser);
                    sessionIdService.externalEvent(new SessionEvent(SessionEventType.UNAUTHENTICATED, sessionUser));
                }
            } else {
                throw e;
            }
        }
    }

    private Map<String, String> getGenericRequestMap(HttpServletRequest httpRequest) {
        Map<String, String> result = new HashMap<>();
        for (Entry<String, String[]> entry : httpRequest.getParameterMap().entrySet()) {
            result.put(entry.getKey(), entry.getValue()[0]);
        }

        return result;
    }

    private Response redirectToAuthorizationPage(RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
                                                 String redirectUri, String state, ResponseMode responseMode, String nonce, String display,
                                                 List<Prompt> prompts, Integer maxAge, List<String> uiLocales, String idTokenHint, String loginHint,
                                                 List<String> acrValues, List<String> amrValues, String request, String requestUri, String originHeaders,
                                                 String codeChallenge, String codeChallengeMethod, String sessionId, String claims, String authReqId,
                                                 String userCode, Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog,
                                                 HttpServletRequest httpRequest) {
        return redirectTo("/authorize", redirectUriResponse, responseTypes, scope, clientId, redirectUri,
                state, responseMode, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode, customParameters, oAuth2AuditLog, httpRequest);
    }

    private Response redirectToSelectAccountPage(RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
                                                 String redirectUri, String state, ResponseMode responseMode, String nonce, String display,
                                                 List<Prompt> prompts, Integer maxAge, List<String> uiLocales, String idTokenHint, String loginHint,
                                                 List<String> acrValues, List<String> amrValues, String request, String requestUri, String originHeaders,
                                                 String codeChallenge, String codeChallengeMethod, String sessionId, String claims, String authReqId,
                                                 String userCode, Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog,
                                                 HttpServletRequest httpRequest) {
        return redirectTo("/selectAccount", redirectUriResponse, responseTypes, scope, clientId, redirectUri,
                state, responseMode, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, userCode, customParameters, oAuth2AuditLog, httpRequest);
    }

    private Response redirectTo(String pathToRedirect,
            RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
            String redirectUri, String state, ResponseMode responseMode, String nonce, String display,
            List<Prompt> prompts, Integer maxAge, List<String> uiLocales, String idTokenHint, String loginHint,
            List<String> acrValues, List<String> amrValues, String request, String requestUri, String originHeaders,
            String codeChallenge, String codeChallengeMethod, String sessionId, String claims, String authReqId,
            String userCode, Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog,
            HttpServletRequest httpRequest) {

        final URI contextUri = URI.create(appConfiguration.getIssuer()).resolve(servletRequest.getContextPath() + pathToRedirect + сonfigurationFactory.getFacesMapping());

        redirectUriResponse.setBaseRedirectUri(contextUri.toString());
        redirectUriResponse.setResponseMode(ResponseMode.QUERY);

        // oAuth parameters
        String responseType = implode(responseTypes, " ");
        if (StringUtils.isNotBlank(responseType)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.RESPONSE_TYPE, responseType);
        }
        if (StringUtils.isNotBlank(scope)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.SCOPE, scope);
        }
        if (StringUtils.isNotBlank(clientId)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CLIENT_ID, clientId);
        }
        if (StringUtils.isNotBlank(redirectUri)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.REDIRECT_URI, redirectUri);
        }
        if (StringUtils.isNotBlank(state)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.STATE, state);
        }
        if (responseMode != null) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.RESPONSE_MODE, responseMode.getParamName());
        }

        // OIC parameters
        if (StringUtils.isNotBlank(nonce)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.NONCE, nonce);
        }
        if (StringUtils.isNotBlank(display)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.DISPLAY, display);
        }
        String prompt = implode(prompts, " ");
        if (StringUtils.isNotBlank(prompt)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.PROMPT, prompt);
        }
        if (maxAge != null) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.MAX_AGE, maxAge.toString());
        }
        String uiLocalesStr = implode(uiLocales, " ");
        if (StringUtils.isNotBlank(uiLocalesStr)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.UI_LOCALES, uiLocalesStr);
        }
        if (StringUtils.isNotBlank(idTokenHint)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ID_TOKEN_HINT, idTokenHint);
        }
        if (StringUtils.isNotBlank(loginHint)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.LOGIN_HINT, loginHint);
        }
        String acrValuesStr = implode(acrValues, " ");
        if (StringUtils.isNotBlank(acrValuesStr)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ACR_VALUES, acrValuesStr);
        }
        String amrValuesStr = implode(amrValues, " ");
        if (StringUtils.isNotBlank(amrValuesStr)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.AMR_VALUES, amrValuesStr);
        }
        if (StringUtils.isNotBlank(request)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.REQUEST, request);
        }
        if (StringUtils.isNotBlank(requestUri)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.REQUEST_URI, requestUri);
        }
        if (StringUtils.isNotBlank(codeChallenge)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CODE_CHALLENGE, codeChallenge);
        }
        if (StringUtils.isNotBlank(codeChallengeMethod)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        }
        if (StringUtils.isNotBlank(sessionId) && appConfiguration.getSessionIdRequestParameterEnabled()) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.SESSION_ID, sessionId);
        }
        if (StringUtils.isNotBlank(claims)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CLAIMS, claims);
        }
        if (StringUtils.isNotBlank(userCode)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.USER_CODE, userCode);
        }

        // CIBA param
        if (StringUtils.isNotBlank(authReqId)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.AUTH_REQ_ID, authReqId);
        }

        // mod_ox param
        if (StringUtils.isNotBlank(originHeaders)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ORIGIN_HEADERS, originHeaders);
        }

        if (customParameters != null && customParameters.size() > 0) {
            for (Map.Entry<String, String> entry : customParameters.entrySet()) {
                redirectUriResponse.addResponseParameter(entry.getKey(), entry.getValue());
            }
        }

        ResponseBuilder builder = RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, httpRequest);
        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    private void unauthenticateSession(String sessionId, HttpServletRequest httpRequest) {
        identity.logout();

        SessionId sessionUser = identity.getSessionId();

        if (sessionUser != null) {
            sessionUser.setUserDn(null);
            sessionUser.setUser(null);
            sessionUser.setAuthenticationTime(null);
        }

        if (StringHelper.isEmpty(sessionId)) {
            sessionId = cookieService.getSessionIdFromCookie(httpRequest);
        }

        SessionId persistenceSessionId = sessionIdService.getSessionId(sessionId);
        if (persistenceSessionId == null) {
            log.error("Failed to load session from LDAP by session_id: '{}'", sessionId);
            return;
        }

        persistenceSessionId.setState(SessionIdState.UNAUTHENTICATED);
        persistenceSessionId.setUserDn(null);
        persistenceSessionId.setUser(null);
        persistenceSessionId.setAuthenticationTime(null);
        boolean result = sessionIdService.updateSessionId(persistenceSessionId);
        sessionIdService.externalEvent(new SessionEvent(SessionEventType.UNAUTHENTICATED, persistenceSessionId).setHttpRequest(httpRequest));
        if (!result) {
            log.error("Failed to update session_id '{}'", sessionId);
        }
    }

    /**
     * Processes an authorization granted for device code grant type.
     * @param userCode User code used in the device code flow.
     * @param user Authenticated user that is giving the permissions.
     * @param httpRequest
     * @param httpResponse
     */
    private void processDeviceAuthorization(String userCode, User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        DeviceAuthorizationCacheControl cacheData = deviceAuthorizationService.getDeviceAuthzByUserCode(userCode);
        if (cacheData == null || cacheData.getStatus() == DeviceAuthorizationStatus.EXPIRED) {
            log.trace("User responded too late and the authorization {} has expired, {}", userCode, cacheData);
            return;
        }

        deviceAuthorizationService.removeDeviceAuthRequestInCache(userCode, cacheData.getDeviceCode());
        DeviceCodeGrant deviceCodeGrant = authorizationGrantList.createDeviceGrant(cacheData, user);

        RefreshToken refreshToken = deviceCodeGrant.createRefreshToken();
        log.debug("Issuing refresh token: {}", refreshToken.getCode());

        AccessToken accessToken = deviceCodeGrant.createAccessToken(httpRequest.getHeader("X-ClientCert"), new ExecutionContext(httpRequest, httpResponse));
        log.debug("Issuing access token: {}", accessToken.getCode());

        deviceCodeGrant.createIdToken(
                null, null, accessToken, refreshToken,
                null, deviceCodeGrant, false, null);

        deviceCodeGrant.setDeviceCode(cacheData.getDeviceCode());
        deviceCodeGrant.setTokensDelivered(false);
        deviceCodeGrant.save();
        log.info("Issued all tokens needed for device authorization request, user_code: {}", userCode);
    }

}