/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.ScopeConstants;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.binding.TokenBindingMessage;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.util.Util;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBAPingCallbackService;
import io.jans.as.server.ciba.CIBAPushTokenDeliveryService;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.authorize.AuthorizeParamsValidator;
import io.jans.as.server.model.authorize.IdTokenMember;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.AccessToken;
import io.jans.as.server.model.common.AuthorizationCode;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.CIBAGrant;
import io.jans.as.server.model.common.CibaRequestCacheControl;
import io.jans.as.server.model.common.CibaRequestStatus;
import io.jans.as.server.model.common.DefaultScope;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.DeviceAuthorizationStatus;
import io.jans.as.server.model.common.DeviceCodeGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.common.IdToken;
import io.jans.as.server.model.common.RefreshToken;
import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.model.common.SessionIdState;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.exception.AcrChangedException;
import io.jans.as.server.model.exception.InvalidRedirectUrlException;
import io.jans.as.server.model.exception.InvalidSessionStateException;
import io.jans.as.server.model.ldap.ClientAuthorization;
import io.jans.as.server.model.token.JwrService;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.AttributeService;
import io.jans.as.server.service.AuthenticationFilterService;
import io.jans.as.server.service.ClientAuthorizationsService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.DeviceAuthorizationService;
import io.jans.as.server.service.RedirectUriResponse;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.UserService;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.as.server.service.external.ExternalPostAuthnService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalPostAuthnContext;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.service.external.session.SessionEvent;
import io.jans.as.server.service.external.session.SessionEventType;
import io.jans.as.server.util.QueryStringDecoder;
import io.jans.as.server.util.RedirectUtil;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import static io.jans.as.model.util.StringUtils.implode;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Implementation for request authorization through REST web services.
 *
 * @author Javier Rojas Blum
 * @version March 17, 2022
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

    @Inject
    private CookieService cookieService;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private ClientAuthorizationsService clientAuthorizationsService;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ConfigurationFactory configurationFactory;

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

    @Inject
    private AttributeService attributeService;

    @Inject
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private AuthzRequestService authzRequestService;

    @Context
    private HttpServletRequest servletRequest;

    @Override
    public Response requestAuthorizationGet(
            String scope, String responseType, String clientId, String redirectUri, String state, String responseMode,
            String nonce, String display, String prompt, Integer maxAge, String uiLocales, String idTokenHint,
            String loginHint, String acrValues, String amrValues, String request, String requestUri,
            String sessionId, String originHeaders,
            String codeChallenge, String codeChallengeMethod, String customResponseHeaders, String claims, String authReqId,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setHttpMethod(HttpMethod.GET);
        authzRequest.setScope(scope);
        authzRequest.setResponseType(responseType);
        authzRequest.setClientId(clientId);
        authzRequest.setRedirectUri(redirectUri);
        authzRequest.setState(state);
        authzRequest.setResponseMode(responseMode);
        authzRequest.setNonce(nonce);
        authzRequest.setDisplay(display);
        authzRequest.setPrompt(prompt);
        authzRequest.setMaxAge(maxAge);
        authzRequest.setUiLocales(uiLocales);
        authzRequest.setIdTokenHint(idTokenHint);
        authzRequest.setLoginHint(loginHint);
        authzRequest.setAcrValues(acrValues);
        authzRequest.setAmrValues(amrValues);
        authzRequest.setRequest(request);
        authzRequest.setRequestUri(requestUri);
        authzRequest.setSessionId(sessionId);
        authzRequest.setOriginHeaders(originHeaders);
        authzRequest.setCodeChallenge(codeChallenge);
        authzRequest.setCodeChallengeMethod(codeChallengeMethod);
        authzRequest.setCustomResponseHeaders(customResponseHeaders);
        authzRequest.setClaims(claims);
        authzRequest.setAuthReqId(authReqId);
        authzRequest.setHttpRequest(httpRequest);
        authzRequest.setHttpResponse(httpResponse);
        authzRequest.setSecurityContext(securityContext);

        return requestAuthorization(authzRequest);
    }

    @Override
    public Response requestAuthorizationPost(
            String scope, String responseType, String clientId, String redirectUri, String state, String responseMode,
            String nonce, String display, String prompt, Integer maxAge, String uiLocales, String idTokenHint,
            String loginHint, String acrValues, String amrValues, String request, String requestUri,
            String sessionId, String originHeaders,
            String codeChallenge, String codeChallengeMethod, String customResponseHeaders, String claims,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setHttpMethod(HttpMethod.POST);
        authzRequest.setScope(scope);
        authzRequest.setResponseType(responseType);
        authzRequest.setClientId(clientId);
        authzRequest.setRedirectUri(redirectUri);
        authzRequest.setState(state);
        authzRequest.setResponseMode(responseMode);
        authzRequest.setNonce(nonce);
        authzRequest.setDisplay(display);
        authzRequest.setPrompt(prompt);
        authzRequest.setMaxAge(maxAge);
        authzRequest.setUiLocales(uiLocales);
        authzRequest.setIdTokenHint(idTokenHint);
        authzRequest.setLoginHint(loginHint);
        authzRequest.setAcrValues(acrValues);
        authzRequest.setAmrValues(amrValues);
        authzRequest.setRequest(request);
        authzRequest.setRequestUri(requestUri);
        authzRequest.setSessionId(sessionId);
        authzRequest.setOriginHeaders(originHeaders);
        authzRequest.setCodeChallenge(codeChallenge);
        authzRequest.setCodeChallengeMethod(codeChallengeMethod);
        authzRequest.setCustomResponseHeaders(customResponseHeaders);
        authzRequest.setClaims(claims);
        authzRequest.setHttpRequest(httpRequest);
        authzRequest.setHttpResponse(httpResponse);
        authzRequest.setSecurityContext(securityContext);

        return requestAuthorization(authzRequest);
    }

    private Response requestAuthorization(AuthzRequest authzRequest) {
        authzRequest.setScope(ServerUtil.urlDecode(authzRequest.getScope())); // it may be encoded in uma case

        String tokenBindingHeader = authzRequest.getHttpRequest().getHeader("Sec-Token-Binding");

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(authzRequest.getHttpRequest()), Action.USER_AUTHORIZATION);
        oAuth2AuditLog.setClientId(authzRequest.getClientId());
        oAuth2AuditLog.setScope(authzRequest.getScope());

        log.debug("Attempting to request authorization: {}", authzRequest);

        ResponseBuilder builder = null;

        authzRequest.setCustomParameters(requestParameterService.getCustomParameters(QueryStringDecoder.decode(authzRequest.getHttpRequest().getQueryString())));

        boolean isPar = authzRequestService.processPar(authzRequest);

        List<ResponseType> responseTypes = ResponseType.fromString(authzRequest.getResponseType(), " ");
        List<Prompt> prompts = Prompt.fromString(authzRequest.getPrompt(), " ");

        SessionId sessionUser = identity.getSessionId();
        User user = sessionIdService.getUser(sessionUser);

        try {
            Map<String, String> customResponseHeaders = Util.jsonObjectArrayStringAsMap(authzRequest.getCustomResponseHeaders());

            updateSessionForROPC(authzRequest.getHttpRequest(), sessionUser);

            Client client = authorizeRestWebServiceValidator.validateClient(authzRequest.getClientId(), authzRequest.getState(), isPar);
            String deviceAuthzUserCode = deviceAuthorizationService.getUserCodeFromSession(authzRequest.getHttpRequest());
            authzRequest.setRedirectUri(authorizeRestWebServiceValidator.validateRedirectUri(client, authzRequest.getRedirectUri(), authzRequest.getState(), deviceAuthzUserCode, authzRequest.getHttpRequest()));

            RedirectUriResponse redirectUriResponse = new RedirectUriResponse(new RedirectUri(authzRequest.getRedirectUri(), responseTypes, authzRequest.getResponseModeEnum()), authzRequest.getState(), authzRequest.getHttpRequest(), errorResponseFactory);
            redirectUriResponse.setFapiCompatible(appConfiguration.isFapi());
            authzRequest.setRedirectUriResponse(redirectUriResponse);

            authorizeRestWebServiceValidator.validateAcrs(authzRequest, client);

            Set<String> scopes = scopeChecker.checkScopesPolicy(client, authzRequest.getScope());

            if (Boolean.TRUE.equals(appConfiguration.getForceSignedRequestObject()) && StringUtils.isBlank(authzRequest.getRequest()) && StringUtils.isBlank(authzRequest.getRequestUri())) {
                throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "A signed request object is required");
            }

            JwtAuthorizationRequest jwtRequest = null;
            if (StringUtils.isNotBlank(authzRequest.getRequest()) || StringUtils.isNotBlank(authzRequest.getRequestUri())) {
                try {
                    jwtRequest = JwtAuthorizationRequest.createJwtRequest(authzRequest.getRequest(), authzRequest.getRequestUri(), client, redirectUriResponse, cryptoProvider, appConfiguration);

                    if (jwtRequest == null) {
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "Failed to parse jwt.");
                    }
                    authzRequest.setJwtRequest(jwtRequest);

                    if (StringUtils.isNotBlank(jwtRequest.getState())) {
                        authzRequest.setState(jwtRequest.getState());
                        redirectUriResponse.setState(authzRequest.getState());
                    }
                    if (appConfiguration.isFapi() && StringUtils.isBlank(jwtRequest.getState())) {
                        authzRequest.setState(""); // #1250 - FAPI : discard state if in JWT we don't have state
                        redirectUriResponse.setState("");
                    }

                    if (jwtRequest.getRedirectUri() != null) {
                        redirectUriResponse.getRedirectUri().setBaseRedirectUri(jwtRequest.getRedirectUri());
                    }

                    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(jwtRequest.getAlgorithm());
                    if (Boolean.TRUE.equals(appConfiguration.getForceSignedRequestObject()) && signatureAlgorithm == SignatureAlgorithm.NONE) {
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "A signed request object is required");
                    }

                    // JWT wins
                    if (!jwtRequest.getScopes().isEmpty()) {
                        if (!scopes.contains("openid")) { // spec: Even if a scope parameter is present in the Request Object value, a scope parameter MUST always be passed using the OAuth 2.0 request syntax containing the openid scope value
                            throw new WebApplicationException(Response
                                    .status(Response.Status.BAD_REQUEST)
                                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_SCOPE, authzRequest.getState(), "scope parameter does not contain openid value which is required."))
                                    .build());
                        }
                        scopes = scopeChecker.checkScopesPolicy(client, Lists.newArrayList(jwtRequest.getScopes()));
                    }
                    if (jwtRequest.getRedirectUri() != null && !jwtRequest.getRedirectUri().equals(authzRequest.getRedirectUri())) {
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "The redirect_uri parameter is not the same in the JWT");
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getNonce())) {
                        authzRequest.setNonce(jwtRequest.getNonce());
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getCodeChallenge())) {
                        authzRequest.setCodeChallenge(jwtRequest.getCodeChallenge());
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getCodeChallengeMethod())) {
                        authzRequest.setCodeChallengeMethod(jwtRequest.getCodeChallengeMethod());
                    }
                    if (jwtRequest.getDisplay() != null && StringUtils.isNotBlank(jwtRequest.getDisplay().getParamName())) {
                        authzRequest.setDisplay(jwtRequest.getDisplay().getParamName());
                    }
                    if (!jwtRequest.getPrompts().isEmpty()) {
                        prompts = Lists.newArrayList(jwtRequest.getPrompts());
                    }
                    if (jwtRequest.getResponseMode() != null) {
                        authzRequest.setResponseMode(jwtRequest.getResponseMode().getValue());
                        redirectUriResponse.getRedirectUri().setResponseMode(jwtRequest.getResponseMode());
                    }

                    authzRequestService.checkIdTokenMember(authzRequest, redirectUriResponse, user, jwtRequest);
                    requestParameterService.getCustomParameters(jwtRequest, authzRequest.getCustomParameters());
                } catch (WebApplicationException e) {
                    JsonWebResponse jwr = authzRequestService.parseRequestToJwr(authzRequest.getRequest());
                    if (jwr != null) {
                        String checkForAlg = jwr.getClaims().getClaimAsString("alg"); // to handle Jans Issue#310
                        if ("none".equals(checkForAlg)) {
                            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                    .entity(errorResponseFactory.getErrorAsJson(
                                            AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, "",
                                            "The None algorithm in nested JWT is not allowed for FAPI"))
                                    .type(MediaType.APPLICATION_JSON_TYPE).build());
                        }
                        ResponseMode responseMode = ResponseMode.getByValue(jwr.getClaims().getClaimAsString("response_mode"));
                        if (responseMode == ResponseMode.JWT) {
                            authzRequest.setResponseMode(responseMode.getValue());
                            redirectUriResponse.getRedirectUri().setResponseMode(ResponseMode.JWT);
                            authzRequestService.fillRedirectUriResponseforJARM(redirectUriResponse, jwr, client);
                            if (appConfiguration.isFapi()) {
                                authorizeRestWebServiceValidator.throwInvalidJwtRequestExceptionAsJwtMode(
                                        redirectUriResponse, "Invalid JWT authorization request",
                                        jwr.getClaims().getClaimAsString("state"), authzRequest.getHttpRequest());
                            }
                        }
                    }
                    throw e;
                } catch (Exception e) {
                    log.error("Invalid JWT authorization request. Message : " + e.getMessage(), e);
                    throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "Invalid JWT authorization request");
                }
            }

            // JARM
            Set<ResponseMode> jwtResponseModes = Sets.newHashSet(ResponseMode.QUERY_JWT, ResponseMode.FRAGMENT_JWT, ResponseMode.JWT, ResponseMode.FORM_POST_JWT);
            if (jwtResponseModes.contains(authzRequest.getResponseModeEnum())) {
                JsonWebResponse jwe = authzRequestService.parseRequestToJwr(authzRequest.getRequest());
                authzRequestService.fillRedirectUriResponseforJARM(redirectUriResponse, jwe, client);
            }
            // Validate JWT request object after JARM check, because we want to return errors well formatted (JSON/JWT).
            if (jwtRequest != null) {
                authorizeRestWebServiceValidator.validateJwtRequest(authzRequest.getClientId(), authzRequest.getState(), authzRequest.getHttpRequest(), responseTypes, redirectUriResponse, jwtRequest);
            }

            if (!cibaRequestService.hasCibaCompatibility(client) && !isPar) {
                if (appConfiguration.isFapi() && authzRequest.getJwtRequest() == null) {
                    throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST);
                }
                authorizeRestWebServiceValidator.validateRequestJwt(authzRequest.getRequest(), authzRequest.getRequestUri(), redirectUriResponse);
            }

            authorizeRestWebServiceValidator.validate(authzRequest, responseTypes, client);
            authorizeRestWebServiceValidator.validatePkce(authzRequest.getCodeChallenge(), redirectUriResponse);

            if (StringUtils.isBlank(authzRequest.getAcrValues()) && !ArrayUtils.isEmpty(client.getDefaultAcrValues())) {
                authzRequest.setAcrValues(implode(client.getDefaultAcrValues(), " "));
            }

            if (scopes.contains(ScopeConstants.OFFLINE_ACCESS) && !client.getTrustedClient()) {
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
                    && AuthorizeParamsValidator.validateGrantType(responseTypes, client.getGrantTypes(), appConfiguration);

            if (!isResponseTypeValid) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNSUPPORTED_RESPONSE_TYPE, authzRequest.getState(), ""))
                        .build());
            }

            AuthorizationGrant authorizationGrant = null;

            if (user == null) {
                final Pair<User, SessionId> pair = ifUserIsNull(authzRequest, redirectUriResponse, oAuth2AuditLog);
                user = pair.getFirst();
                sessionUser = pair.getSecond();
            }

            boolean validAuthenticationMaxAge = authorizeRestWebServiceValidator.isAuthnMaxAgeValid(authzRequest.getMaxAge(), sessionUser, client);
            if (!validAuthenticationMaxAge) {
                unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
                authzRequest.setSessionId(null);

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), prompts, oAuth2AuditLog);
            }

            oAuth2AuditLog.setUsername(user.getUserId());

            ExternalPostAuthnContext postAuthnContext = new ExternalPostAuthnContext(client, sessionUser, authzRequest.getHttpRequest(), authzRequest.getHttpResponse());
            final boolean forceReAuthentication = externalPostAuthnService.externalForceReAuthentication(client, postAuthnContext);
            if (forceReAuthentication) {
                unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
                authzRequest.setSessionId(null);

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), prompts, oAuth2AuditLog);
            }

            final boolean forceAuthorization = externalPostAuthnService.externalForceAuthorization(client, postAuthnContext);
            if (forceAuthorization) {
                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), prompts, oAuth2AuditLog);
            }

            ClientAuthorization clientAuthorization = null;
            boolean clientAuthorizationFetched = false;
            if (!scopes.isEmpty()) {
                if (prompts.contains(Prompt.CONSENT)) {
                    return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), prompts, oAuth2AuditLog);
                }
                // There is no need to present the consent page:
                // If Client is a Trusted Client.
                // If a client is configured for pairwise identifiers, and the openid scope is the only scope requested.
                // Also, we should make sure that the claims request is not enabled.
                final boolean isPairwiseWithOnlyOpenIdScope = client.getSubjectType() == SubjectType.PAIRWISE
                        && scopes.size() == 1
                        && scopes.contains(DefaultScope.OPEN_ID.toString())
                        && authzRequest.getClaims() == null
                        && (authzRequest.getJwtRequest() == null || (authzRequest.getJwtRequest().getUserInfoMember() == null && authzRequest.getJwtRequest().getIdTokenMember() == null));
                if (client.getTrustedClient() || isPairwiseWithOnlyOpenIdScope) {
                    sessionUser.addPermission(authzRequest.getClientId(), true);
                    sessionIdService.updateSessionId(sessionUser);
                } else {
                    clientAuthorization = clientAuthorizationsService.find(user.getAttribute("inum"), client.getClientId());
                    clientAuthorizationFetched = true;
                    if (clientAuthorization != null && clientAuthorization.getScopes() != null) {
                        if (log.isTraceEnabled())
                            log.trace("ClientAuthorization - scope: {}, dn: {}, requestedScope: {}", authzRequest.getScope(), clientAuthorization.getDn(), scopes);
                        if (Arrays.asList(clientAuthorization.getScopes()).containsAll(scopes)) {
                            sessionUser.addPermission(authzRequest.getClientId(), true);
                            sessionIdService.updateSessionId(sessionUser);
                        } else {
                            return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), prompts, oAuth2AuditLog);
                        }
                    }
                }
            }

            if (identity != null && identity.getSessionId() != null && identity.getSessionId().getState() == SessionIdState.AUTHENTICATED
                    && client != null && Boolean.TRUE.equals(client.getAttributes().getDefaultPromptLogin())
                    && identity.getSessionId().getAuthenticationTime() != null
                    && new Date().getTime() - identity.getSessionId().getAuthenticationTime().getTime() > 200) {
                prompts.add(Prompt.LOGIN);
            }

            if (prompts.contains(Prompt.LOGIN)) {

                //  workaround for #1030 - remove only authenticated session, for set up acr we set it unauthenticated and then drop in AuthorizeAction
                if (identity.getSessionId().getState() == SessionIdState.AUTHENTICATED) {
                    unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
                }
                authzRequest.setSessionId(null);
                prompts.remove(Prompt.LOGIN);

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), prompts, oAuth2AuditLog);
            }

            if (prompts.contains(Prompt.CONSENT) || !isTrue(sessionUser.isPermissionGrantedForClient(authzRequest.getClientId()))) {
                if (!clientAuthorizationFetched) {
                    clientAuthorization = clientAuthorizationsService.find(user.getAttribute("inum"), client.getClientId());
                }
                clientAuthorizationsService.clearAuthorizations(clientAuthorization, client.getPersistClientAuthorizations());

                prompts.remove(Prompt.CONSENT);

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(),
                        prompts, oAuth2AuditLog);
            }

            if (prompts.contains(Prompt.SELECT_ACCOUNT)) {
                return redirectToSelectAccountPage(authzRequest, redirectUriResponse.getRedirectUri(),
                        prompts, oAuth2AuditLog);
            }

            AuthorizationCode authorizationCode = null;
            if (responseTypes.contains(ResponseType.CODE)) {
                authorizationGrant = authorizationGrantList.createAuthorizationCodeGrant(user, client,
                        sessionUser.getAuthenticationTime());
                authorizationGrant.setNonce(authzRequest.getNonce());
                authorizationGrant.setJwtAuthorizationRequest(authzRequest.getJwtRequest());
                authorizationGrant.setTokenBindingHash(TokenBindingMessage.getTokenBindingIdHashFromTokenBindingMessage(tokenBindingHeader, client.getIdTokenTokenBindingCnf()));
                authorizationGrant.setScopes(scopes);
                authorizationGrant.setCodeChallenge(authzRequest.getCodeChallenge());
                authorizationGrant.setCodeChallengeMethod(authzRequest.getCodeChallengeMethod());
                authorizationGrant.setClaims(authzRequest.getClaims());

                // Store acr_values
                authorizationGrant.setAcrValues(getAcrForGrant(authzRequest.getAcrValues(), sessionUser));
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
                    authorizationGrant.setNonce(authzRequest.getNonce());
                    authorizationGrant.setJwtAuthorizationRequest(authzRequest.getJwtRequest());
                    authorizationGrant.setScopes(scopes);
                    authorizationGrant.setClaims(authzRequest.getClaims());

                    // Store acr_values
                    authorizationGrant.setAcrValues(getAcrForGrant(authzRequest.getAcrValues(), sessionUser));
                    authorizationGrant.setSessionDn(sessionUser.getDn());
                    authorizationGrant.save(); // call save after object modification!!!
                }

                final ExecutionContext executionContext = new ExecutionContext(authzRequest.getHttpRequest(), authzRequest.getHttpResponse());
                executionContext.setCertAsPem(authzRequest.getHttpRequest().getHeader("X-ClientCert"));
                newAccessToken = authorizationGrant.createAccessToken(executionContext);

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
                    authorizationGrant.setNonce(authzRequest.getNonce());
                    authorizationGrant.setJwtAuthorizationRequest(authzRequest.getJwtRequest());
                    authorizationGrant.setScopes(scopes);
                    authorizationGrant.setClaims(authzRequest.getClaims());

                    // Store authentication acr values
                    authorizationGrant.setAcrValues(getAcrForGrant(authzRequest.getAcrValues(), sessionUser));
                    authorizationGrant.setSessionDn(sessionUser.getDn());
                    authorizationGrant.save(); // call save after object modification, call is asynchronous!!!
                }

                ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(authzRequest.getHttpRequest(), authorizationGrant, client, appConfiguration, attributeService);

                final Function<JsonWebResponse, Void> preProcessor = JwrService.wrapWithSidFunction(TokenBindingMessage.createIdTokenTokingBindingPreprocessing(tokenBindingHeader, client.getIdTokenTokenBindingCnf()), sessionUser.getOutsideSid());
                Function<JsonWebResponse, Void> postProcessor = externalUpdateTokenService.buildModifyIdTokenProcessor(context);

                final ExecutionContext executionContext = context.toExecutionContext();
                executionContext.setPreProcessing(preProcessor);
                executionContext.setPostProcessor(postProcessor);
                executionContext.setIncludeIdTokenClaims(includeIdTokenClaims);
                executionContext.setGrant(authorizationGrant);

                IdToken idToken = authorizationGrant.createIdToken(
                        authzRequest.getNonce(), authorizationCode, newAccessToken, null,
                        authzRequest.getState(), executionContext);

                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.ID_TOKEN, idToken.getCode());
            }

            if (authorizationGrant != null && StringHelper.isNotEmpty(authzRequest.getAcrValues()) && !appConfiguration.isFapi()) {
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.ACR_VALUES, authzRequest.getAcrValues());
            }

            for (Map.Entry<String, String> customParam : requestParameterService.getCustomParameters(authzRequest.getCustomParameters(), true).entrySet()) {
                redirectUriResponse.getRedirectUri().addResponseParameter(customParam.getKey(), customParam.getValue());
            }

            if (sessionUser.getId() == null) {
                final SessionId newSessionUser = sessionIdService.generateAuthenticatedSessionId(authzRequest.getHttpRequest(), sessionUser.getUserDn(), authzRequest.getPrompt());
                String newSessionId = newSessionUser.getId();
                sessionUser.setId(newSessionId);
                log.trace("newSessionId = {}", newSessionId);
            }
            if (!appConfiguration.isFapi() && isTrue(appConfiguration.getSessionIdRequestParameterEnabled())) {
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SESSION_ID, sessionUser.getId());
            }
            if (isTrue(appConfiguration.getIncludeSidInResponse())) { // by defalut we do not include sid in response. It should be read by RP from id_token
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SID, sessionUser.getOutsideSid());
            }
            redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SESSION_STATE, sessionIdService.computeSessionState(sessionUser, authzRequest.getClientId(), authzRequest.getRedirectUri()));
            redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.STATE, authzRequest.getState());
            if (StringUtils.isNotBlank(authzRequest.getScope()) && authorizationGrant != null && !appConfiguration.isFapi()) {
                authzRequest.setScope(authorizationGrant.checkScopesPolicy(authzRequest.getScope()));

                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SCOPE, authzRequest.getScope());
            }

            clientService.updateAccessTime(client, false);
            oAuth2AuditLog.setSuccess(true);

            builder = RedirectUtil.getRedirectResponseBuilder(redirectUriResponse.getRedirectUri(), authzRequest.getHttpRequest());

            if (isTrue(appConfiguration.getCustomHeadersWithAuthorizationResponse())) {
                for (Entry<String, String> entry : customResponseHeaders.entrySet()) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }

            if (StringUtils.isNotBlank(authzRequest.getAuthReqId())) {
                runCiba(authzRequest.getAuthReqId(), client, authzRequest.getHttpRequest(), authzRequest.getHttpResponse());
            }
            if (StringUtils.isNotBlank(deviceAuthzUserCode)) {
                processDeviceAuthorization(deviceAuthzUserCode, user);
            }
        } catch (WebApplicationException e) {
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            if (log.isErrorEnabled())
                log.error(e.getMessage(), e);
            throw e;
        } catch (AcrChangedException e) { // Acr changed
            log.error("ACR is changed, please provide a supported and enabled acr value");
            log.error(e.getMessage(), e);

            RedirectUri redirectUriResponse = new RedirectUri(authzRequest.getRedirectUri(), responseTypes, authzRequest.getResponseModeEnum());
            redirectUriResponse.parseQueryString(errorResponseFactory.getErrorAsQueryString(
                    AuthorizeErrorResponseType.SESSION_SELECTION_REQUIRED, authzRequest.getState()));
            redirectUriResponse.addResponseParameter("hint", "Use prompt=login in order to alter existing session.");
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
            return RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, authzRequest.getHttpRequest()).build();
        } catch (EntryPersistenceException e) { // Invalid clientId
            builder = Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, authzRequest.getState(), ""))
                    .type(MediaType.APPLICATION_JSON_TYPE);
            log.error(e.getMessage(), e);
        } catch (InvalidRedirectUrlException e) {
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST_REDIRECT_URI, authzRequest.getState(), ""))
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

    private Pair<User, SessionId> ifUserIsNull(AuthzRequest authzRequest, RedirectUriResponse redirectUriResponse, OAuth2AuditLog oAuth2AuditLog) throws SearchException {
        identity.logout();
        final List<Prompt> prompts = authzRequest.getPromptList();
        if (prompts.contains(Prompt.NONE)) {
            if (authenticationFilterService.isEnabled()) {
                final Map<String, String> params;
                if (authzRequest.getHttpMethod().equals(HttpMethod.GET)) {
                    params = QueryStringDecoder.decode(authzRequest.getHttpRequest().getQueryString());
                } else {
                    params = getGenericRequestMap(authzRequest.getHttpRequest());
                }

                String userDn = authenticationFilterService.processAuthenticationFilters(params);
                if (userDn != null) {
                    Map<String, String> genericRequestMap = getGenericRequestMap(authzRequest.getHttpRequest());

                    Map<String, String> parameterMap = Maps.newHashMap(genericRequestMap);
                    Map<String, String> requestParameterMap = requestParameterService.getAllowedParameters(parameterMap);

                    SessionId sessionUser = sessionIdService.generateAuthenticatedSessionId(authzRequest.getHttpRequest(), userDn, authzRequest.getPrompt());
                    sessionUser.setSessionAttributes(requestParameterMap);

                    cookieService.createSessionIdCookie(sessionUser, authzRequest.getHttpRequest(), authzRequest.getHttpResponse(), false);
                    sessionIdService.updateSessionId(sessionUser);
                    User user = userService.getUserByDn(sessionUser.getUserDn());
                    return new Pair<>(user, sessionUser);
                } else {
                    applicationAuditLogger.sendMessage(oAuth2AuditLog);
                    throw new WebApplicationException(redirectUriResponse.createErrorBuilder(AuthorizeErrorResponseType.LOGIN_REQUIRED).build());
                }
            } else {
                throw new WebApplicationException(redirectUriResponse.createErrorBuilder(AuthorizeErrorResponseType.LOGIN_REQUIRED).build());
            }
        } else {
            if (prompts.contains(Prompt.LOGIN)) {
                unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
                authzRequest.setSessionId(null);
                prompts.remove(Prompt.LOGIN);
                authzRequest.setPrompt(implode(prompts, " "));
            }

            throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), prompts, oAuth2AuditLog));
        }
    }

    private String getAcrForGrant(String acrValuesStr, SessionId sessionUser) {
        final String acr = sessionIdService.getAcr(sessionUser);
        return StringUtils.isNotBlank(acr) ? acr : acrValuesStr;
    }

    private void runCiba(String authReqId, Client client, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CibaRequestCacheControl cibaRequest = cibaRequestService.getCibaRequest(authReqId);

        if (cibaRequest == null || cibaRequest.getStatus() == CibaRequestStatus.EXPIRED) {
            log.trace("User responded too late and the grant {} has expired, {}", authReqId, cibaRequest);
            return;
        }

        cibaRequestService.removeCibaRequest(authReqId);
        CIBAGrant cibaGrant = authorizationGrantList.createCIBAGrant(cibaRequest);

        ExecutionContext executionContext = new ExecutionContext(httpRequest, httpResponse);
        executionContext.setAppConfiguration(appConfiguration);
        executionContext.setAttributeService(attributeService);
        executionContext.setGrant(cibaGrant);
        executionContext.setClient(client);
        executionContext.setCertAsPem(httpRequest.getHeader("X-ClientCert"));

        AccessToken accessToken = cibaGrant.createAccessToken(executionContext);
        log.debug("Issuing access token: {}", accessToken.getCode());

        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(httpRequest, cibaGrant, client, appConfiguration, attributeService);


        final int refreshTokenLifetimeInSeconds = externalUpdateTokenService.getRefreshTokenLifetimeInSeconds(context);
        final RefreshToken refreshToken;
        if (refreshTokenLifetimeInSeconds > 0) {
            refreshToken = cibaGrant.createRefreshToken(executionContext, refreshTokenLifetimeInSeconds);
        } else {
            refreshToken = cibaGrant.createRefreshToken(executionContext);
        }
        log.debug("Issuing refresh token: {}", (refreshToken != null ? refreshToken.getCode() : ""));

        executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));
        executionContext.setGrant(cibaGrant);
        executionContext.setIncludeIdTokenClaims(false);

        IdToken idToken = cibaGrant.createIdToken(null, null, accessToken, refreshToken, null, executionContext);

        cibaGrant.setTokensDelivered(true);
        cibaGrant.save();

        if (cibaRequest.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH) {
            cibaPushTokenDeliveryService.pushTokenDelivery(
                    cibaGrant.getAuthReqId(),
                    cibaGrant.getClient().getBackchannelClientNotificationEndpoint(),
                    cibaRequest.getClientNotificationToken(),
                    accessToken.getCode(),
                    refreshToken != null ? refreshToken.getCode() : null,
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

    private Map<String, String> getGenericRequestMap(HttpServletRequest httpRequest) {
        Map<String, String> result = new HashMap<>();
        for (Entry<String, String[]> entry : httpRequest.getParameterMap().entrySet()) {
            result.put(entry.getKey(), entry.getValue()[0]);
        }

        return result;
    }

    private Response redirectToAuthorizationPage(AuthzRequest authzRequest, RedirectUri redirectUriResponse,
                                                 List<Prompt> prompts, OAuth2AuditLog oAuth2AuditLog) {
        return redirectTo("/authorize", authzRequest, redirectUriResponse, prompts, oAuth2AuditLog);
    }

    private Response redirectToSelectAccountPage(AuthzRequest authzRequest, RedirectUri redirectUriResponse,
                                                 List<Prompt> prompts, OAuth2AuditLog oAuth2AuditLog) {
        return redirectTo("/selectAccount", authzRequest, redirectUriResponse, prompts, oAuth2AuditLog);
    }

    private Response redirectTo(String pathToRedirect, AuthzRequest authzRequest, RedirectUri redirectUriResponse,
                                List<Prompt> prompts, OAuth2AuditLog oAuth2AuditLog) {

        final URI contextUri = URI.create(appConfiguration.getIssuer()).resolve(servletRequest.getContextPath() + pathToRedirect + configurationFactory.getFacesMapping());

        redirectUriResponse.setBaseRedirectUri(contextUri.toString());
        redirectUriResponse.setResponseMode(ResponseMode.QUERY);

        // oAuth parameters
        if (StringUtils.isNotBlank(authzRequest.getResponseType())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.RESPONSE_TYPE, authzRequest.getResponseType());
        }
        if (StringUtils.isNotBlank(authzRequest.getScope())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.SCOPE, authzRequest.getScope());
        }
        if (StringUtils.isNotBlank(authzRequest.getClientId())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CLIENT_ID, authzRequest.getClientId());
        }
        if (StringUtils.isNotBlank(authzRequest.getRedirectUri())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.REDIRECT_URI, authzRequest.getRedirectUri());
        }
        if (StringUtils.isNotBlank(authzRequest.getState())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.STATE, authzRequest.getState());
        }
        if (StringUtils.isNotBlank(authzRequest.getResponseMode())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.RESPONSE_MODE, authzRequest.getResponseMode());
        }

        // OIC parameters
        if (StringUtils.isNotBlank(authzRequest.getNonce())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.NONCE, authzRequest.getNonce());
        }
        if (StringUtils.isNotBlank(authzRequest.getDisplay())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.DISPLAY, authzRequest.getDisplay());
        }
        String prompt = implode(prompts, " ");
        if (StringUtils.isNotBlank(prompt)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.PROMPT, prompt);
        }
        if (authzRequest.getMaxAge() != null) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.MAX_AGE, authzRequest.getMaxAge().toString());
        }
        if (StringUtils.isNotBlank(authzRequest.getUiLocales())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.UI_LOCALES, authzRequest.getUiLocales());
        }
        if (StringUtils.isNotBlank(authzRequest.getIdTokenHint())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ID_TOKEN_HINT, authzRequest.getIdTokenHint());
        }
        if (StringUtils.isNotBlank(authzRequest.getLoginHint())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.LOGIN_HINT, authzRequest.getLoginHint());
        }
        if (StringUtils.isNotBlank(authzRequest.getAcrValues())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ACR_VALUES, authzRequest.getAcrValues());
        }
        if (StringUtils.isNotBlank(authzRequest.getAmrValues())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.AMR_VALUES, authzRequest.getAmrValues());
        }
        if (StringUtils.isNotBlank(authzRequest.getRequest())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.REQUEST, authzRequest.getRequest());
        }
        if (StringUtils.isNotBlank(authzRequest.getRequestUri())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.REQUEST_URI, authzRequest.getRequestUri());
        }
        if (StringUtils.isNotBlank(authzRequest.getCodeChallenge())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CODE_CHALLENGE, authzRequest.getCodeChallenge());
        }
        if (StringUtils.isNotBlank(authzRequest.getCodeChallengeMethod())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CODE_CHALLENGE_METHOD, authzRequest.getCodeChallengeMethod());
        }
        if (StringUtils.isNotBlank(authzRequest.getSessionId()) && isTrue(appConfiguration.getSessionIdRequestParameterEnabled())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.SESSION_ID, authzRequest.getSessionId());
        }
        if (StringUtils.isNotBlank(authzRequest.getClaims())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CLAIMS, authzRequest.getClaims());
        }

        // CIBA param
        if (StringUtils.isNotBlank(authzRequest.getAuthReqId())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.AUTH_REQ_ID, authzRequest.getAuthReqId());
        }

        // mod_ox param
        if (StringUtils.isNotBlank(authzRequest.getOriginHeaders())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ORIGIN_HEADERS, authzRequest.getOriginHeaders());
        }

        final Map<String, String> customParameters = authzRequest.getCustomParameters();
        if (customParameters != null && customParameters.size() > 0) {
            for (Entry<String, String> entry : customParameters.entrySet()) {
                redirectUriResponse.addResponseParameter(entry.getKey(), entry.getValue());
            }
        }

        ResponseBuilder builder = RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, authzRequest.getHttpRequest());
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
     *
     * @param userCode User code used in the device code flow.
     * @param user     Authenticated user that is giving the permissions.
     */
    private void processDeviceAuthorization(String userCode, User user) {
        DeviceAuthorizationCacheControl cacheData = deviceAuthorizationService.getDeviceAuthzByUserCode(userCode);
        if (cacheData == null || cacheData.getStatus() == DeviceAuthorizationStatus.EXPIRED) {
            log.trace("User responded too late and the authorization {} has expired, {}", userCode, cacheData);
            return;
        }

        deviceAuthorizationService.removeDeviceAuthRequestInCache(userCode, cacheData.getDeviceCode());
        DeviceCodeGrant deviceCodeGrant = authorizationGrantList.createDeviceGrant(cacheData, user);

        log.info("Granted device authorization request, user_code: {}, device_code: {}, grant_id: {}", userCode, cacheData.getDeviceCode(), deviceCodeGrant.getGrantId());
    }

}
