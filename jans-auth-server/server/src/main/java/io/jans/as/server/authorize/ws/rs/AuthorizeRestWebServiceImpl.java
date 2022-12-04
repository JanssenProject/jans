/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Maps;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.*;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.binding.TokenBindingMessage;
import io.jans.as.model.crypto.binding.TokenBindingParseException;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.Util;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBAPingCallbackService;
import io.jans.as.server.ciba.CIBAPushTokenDeliveryService;
import io.jans.as.server.model.authorize.AuthorizeParamsValidator;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.*;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.exception.AcrChangedException;
import io.jans.as.server.model.exception.InvalidRedirectUrlException;
import io.jans.as.server.model.exception.InvalidSessionStateException;
import io.jans.as.server.model.ldap.ClientAuthorization;
import io.jans.as.server.model.token.JwrService;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.as.server.service.external.ExternalPostAuthnService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalPostAuthnContext;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.service.external.session.SessionEvent;
import io.jans.as.server.service.external.session.SessionEventType;
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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static io.jans.as.model.util.StringUtils.implode;
import static org.apache.commons.lang3.BooleanUtils.*;

/**
 * Implementation for request authorization through REST web services.
 *
 * @author Javier Rojas Blum
 * @version March 17, 2022
 */
@Path("/")
public class AuthorizeRestWebServiceImpl implements AuthorizeRestWebService {

    private static final String SUCCESSFUL_RP_REDIRECT_COUNT = "successful_rp_redirect_count";

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

        authorizeRestWebServiceValidator.validateNotWebView(httpRequest);

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
            String codeChallenge, String codeChallengeMethod, String customResponseHeaders, String claims, String authReqId,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {

        authorizeRestWebServiceValidator.validateNotWebView(httpRequest);

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
        authzRequest.setAuthReqId(authReqId);
        authzRequest.setHttpRequest(httpRequest);
        authzRequest.setHttpResponse(httpResponse);
        authzRequest.setSecurityContext(securityContext);

        return requestAuthorization(authzRequest);
    }

    private Response requestAuthorization(AuthzRequest authzRequest) {
        authzRequest.setScope(ServerUtil.urlDecode(authzRequest.getScope())); // it may be encoded -> decode

        authzRequestService.createOauth2AuditLog(authzRequest);

        log.debug("Attempting to request authorization: {}", authzRequest);

        ResponseBuilder builder;

        authzRequest.setCustomParameters(requestParameterService.getCustomParameters(QueryStringDecoder.decode(authzRequest.getHttpRequest().getQueryString())));

        try {
            builder = authorize(authzRequest);
        } catch (WebApplicationException e) {
            applicationAuditLogger.sendMessage(authzRequest.getAuditLog());
            if (log.isErrorEnabled() && canLogWebApplicationException(e))
                log.error(e.getMessage(), e);
            throw e;
        } catch (AcrChangedException e) { // Acr changed
            log.error("ACR is changed, please provide a supported and enabled acr value");
            log.error(e.getMessage(), e);

            RedirectUri redirectUriResponse = new RedirectUri(authzRequest.getRedirectUri(), authzRequest.getResponseTypeList(), authzRequest.getResponseModeEnum());
            redirectUriResponse.parseQueryString(errorResponseFactory.getErrorAsQueryString(
                    AuthorizeErrorResponseType.SESSION_SELECTION_REQUIRED, authzRequest.getState()));
            redirectUriResponse.addResponseParameter("hint", "Use prompt=login in order to alter existing session.");
            applicationAuditLogger.sendMessage(authzRequest.getAuditLog());
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

        applicationAuditLogger.sendMessage(authzRequest.getAuditLog());
        return builder.build();
    }

    private static boolean canLogWebApplicationException(WebApplicationException e) {
        if (e == null || e.getResponse() == null) {
            return false;
        }
        final int status = e.getResponse().getStatus();
        return status != 302;
    }

    private ResponseBuilder authorize(AuthzRequest authzRequest) throws AcrChangedException, SearchException, TokenBindingParseException {
        String tokenBindingHeader = authzRequest.getHttpRequest().getHeader("Sec-Token-Binding");
        boolean isPar = authzRequestService.processPar(authzRequest);

        List<Prompt> prompts = Prompt.fromString(authzRequest.getPrompt(), " ");
        final List<ResponseType> responseTypes = authzRequest.getResponseTypeList();

        SessionId sessionUser = identity.getSessionId();
        User user = sessionIdService.getUser(sessionUser);

        updateSessionForROPC(authzRequest.getHttpRequest(), sessionUser);

        Client client = authorizeRestWebServiceValidator.validateClient(authzRequest, isPar);

        String deviceAuthzUserCode = deviceAuthorizationService.getUserCodeFromSession(authzRequest.getHttpRequest());
        authzRequest.setRedirectUri(authorizeRestWebServiceValidator.validateRedirectUri(client, authzRequest.getRedirectUri(), authzRequest.getState(), deviceAuthzUserCode, authzRequest.getHttpRequest()));
        authzRequestService.createRedirectUriResponse(authzRequest);

        authorizeRestWebServiceValidator.validateAcrs(authzRequest, client);

        Set<String> scopes = scopeChecker.checkScopesPolicy(client, authzRequest.getScope());

        authorizeRestWebServiceValidator.checkSignedRequestRequired(authzRequest);

        authzRequestService.processRequestObject(authzRequest, client, scopes, user, prompts);

        validateRequestJwt(authzRequest, isPar, client);

        authorizeRestWebServiceValidator.validate(authzRequest, responseTypes, client);
        authorizeRestWebServiceValidator.validatePkce(authzRequest.getCodeChallenge(), authzRequest.getRedirectUriResponse());

        authzRequestService.setAcrsIfNeeded(authzRequest);

        checkOfflineAccessScopes(responseTypes, prompts, client, scopes);
        checkResponseType(authzRequest, responseTypes, client);

        AuthorizationGrant authorizationGrant = null;

        if (user == null) {
            final Pair<User, SessionId> pair = ifUserIsNull(authzRequest);
            user = pair.getFirst();
            sessionUser = pair.getSecond();
        }

        validateMaxAge(authzRequest, prompts, sessionUser, client);

        authzRequest.getAuditLog().setUsername(user.getUserId());

        ExternalPostAuthnContext postAuthnContext = new ExternalPostAuthnContext(client, sessionUser, authzRequest.getHttpRequest(), authzRequest.getHttpResponse());
        checkForceReAuthentication(authzRequest, prompts, client, postAuthnContext);
        checkForceAuthorization(authzRequest, prompts, client, postAuthnContext);

        ClientAuthorization clientAuthorization = null;
        boolean clientAuthorizationFetched = false;
        if (!scopes.isEmpty()) {
            final Pair<ClientAuthorization, Boolean> pair = fetchClientAuthorization(authzRequest, client, sessionUser, user, scopes);
            clientAuthorization = pair.getFirst();
            clientAuthorizationFetched = pair.getSecond();
        }

        addPromptLoginIfNeeded(prompts, client);
        checkPromptLogin(authzRequest, prompts);
        checkPromptConsent(authzRequest, prompts, sessionUser, user, clientAuthorization, clientAuthorizationFetched);

        checkPromptSelectAccount(authzRequest, prompts);

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

            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter("code", authorizationCode.getCode());
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

            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.ACCESS_TOKEN, newAccessToken.getCode());
            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.TOKEN_TYPE, newAccessToken.getTokenType().toString());
            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.EXPIRES_IN, newAccessToken.getExpiresIn() + "");
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

            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.ID_TOKEN, idToken.getCode());
        }

        addResponseParameterAcrValues(authzRequest, authorizationGrant);
        addResponseParameterCustomParameters(authzRequest);

        if (sessionUser.getId() == null) {
            final SessionId newSessionUser = sessionIdService.generateAuthenticatedSessionId(authzRequest.getHttpRequest(), sessionUser.getUserDn(), authzRequest.getPrompt());
            String newSessionId = newSessionUser.getId();
            sessionUser.setId(newSessionId);
            log.trace("newSessionId = {}", newSessionId);
        }

        addRespnseParameterSessionId(authzRequest, sessionUser);
        addResponseParameterSid(authzRequest, sessionUser);

        authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.SESSION_STATE, sessionIdService.computeSessionState(sessionUser, authzRequest.getClientId(), authzRequest.getRedirectUri()));
        authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.STATE, authzRequest.getState());

        addResponseParameterScope(authzRequest, authorizationGrant);

        clientService.updateAccessTime(client, false);
        authzRequest.getAuditLog().setSuccess(true);

        ResponseBuilder builder = RedirectUtil.getRedirectResponseBuilder(authzRequest.getRedirectUriResponse().getRedirectUri(), authzRequest.getHttpRequest());

        addCustomHeaders(builder, authzRequest);
        updateSession(authzRequest, sessionUser);

        runCiba(authzRequest.getAuthReqId(), client, authzRequest.getHttpRequest(), authzRequest.getHttpResponse());
        processDeviceAuthorization(deviceAuthzUserCode, user);

        return builder;
    }

    private void addCustomHeaders(ResponseBuilder builder, AuthzRequest authzRequest) {
        if (isTrue(appConfiguration.getCustomHeadersWithAuthorizationResponse())) {

            Map<String, String> customResponseHeaders = Util.jsonObjectArrayStringAsMap(authzRequest.getCustomResponseHeaders());
            for (Entry<String, String> entry : customResponseHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
    }

    private void addResponseParameterScope(AuthzRequest authzRequest, AuthorizationGrant authorizationGrant) {
        if (authorizationGrant != null && !appConfiguration.isFapi()) {
            authzRequest.setScope(authorizationGrant.checkScopesPolicy(authzRequest.getScope()));

            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameterIfNotBlank(AuthorizeResponseParam.SCOPE, authzRequest.getScope());
        }
    }

    private void addResponseParameterSid(AuthzRequest authzRequest, SessionId sessionUser) {
        if (isTrue(appConfiguration.getIncludeSidInResponse())) { // by default we do not include sid in response. It should be read by RP from id_token
            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.SID, sessionUser.getOutsideSid());
        }
    }

    private void addRespnseParameterSessionId(AuthzRequest authzRequest, SessionId sessionUser) {
        if (!appConfiguration.isFapi() && isTrue(appConfiguration.getSessionIdRequestParameterEnabled())) {
            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(AuthorizeResponseParam.SESSION_ID, sessionUser.getId());
        }
    }

    private void addResponseParameterCustomParameters(AuthzRequest authzRequest) {
        for (Entry<String, String> customParam : requestParameterService.getCustomParameters(authzRequest.getCustomParameters(), true).entrySet()) {
            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameter(customParam.getKey(), customParam.getValue());
        }
    }

    private void addResponseParameterAcrValues(AuthzRequest authzRequest, AuthorizationGrant authorizationGrant) {
        if (authorizationGrant != null && !appConfiguration.isFapi()) {
            authzRequest.getRedirectUriResponse().getRedirectUri().addResponseParameterIfNotBlank(AuthorizeResponseParam.ACR_VALUES, authzRequest.getAcrValues());
        }
    }

    private void checkPromptSelectAccount(AuthzRequest authzRequest, List<Prompt> prompts) {
        if (prompts.contains(Prompt.SELECT_ACCOUNT)) {
            throw new WebApplicationException(redirectToSelectAccountPage(authzRequest, prompts));
        }
    }

    private void checkPromptConsent(AuthzRequest authzRequest, List<Prompt> prompts, SessionId sessionUser, User user, ClientAuthorization clientAuthorization, boolean clientAuthorizationFetched) {
        if (prompts.contains(Prompt.CONSENT) || !isTrue(sessionUser.isPermissionGrantedForClient(authzRequest.getClientId()))) {
            if (!clientAuthorizationFetched) {
                clientAuthorization = clientAuthorizationsService.find(user.getAttribute("inum"), authzRequest.getClient().getClientId());
            }
            clientAuthorizationsService.clearAuthorizations(clientAuthorization, authzRequest.getClient().getPersistClientAuthorizations());

            prompts.remove(Prompt.CONSENT);

            throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
        }
    }

    private void checkPromptLogin(AuthzRequest authzRequest, List<Prompt> prompts) {
        if (prompts.contains(Prompt.LOGIN)) {
            boolean sessionUnauthenticated = false;

            //  workaround for #1030 - remove only authenticated session, for set up acr we set it unauthenticated and then drop in AuthorizeAction
            if (identity.getSessionId().getState() == SessionIdState.AUTHENTICATED) {
                sessionUnauthenticated = unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest(), authzRequest.isPromptFromJwt());
            }
            authzRequest.setSessionId(null);
            prompts.remove(Prompt.LOGIN);

            if (sessionUnauthenticated) {
                throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
            }
        }
    }

    private void addPromptLoginIfNeeded(List<Prompt> prompts, Client client) {
        if (identity != null && identity.getSessionId() != null && identity.getSessionId().getState() == SessionIdState.AUTHENTICATED
                && Boolean.TRUE.equals(client.getAttributes().getDefaultPromptLogin())
                && identity.getSessionId().getAuthenticationTime() != null
                && new Date().getTime() - identity.getSessionId().getAuthenticationTime().getTime() > 500) {
            prompts.add(Prompt.LOGIN);
        }
    }

    private Pair<ClientAuthorization, Boolean> fetchClientAuthorization(AuthzRequest authzRequest, Client client, SessionId sessionUser, User user, Set<String> scopes) {
        ClientAuthorization clientAuthorization = null;
        boolean clientAuthorizationFetched = false;

        final List<Prompt> prompts = authzRequest.getPromptList();

        if (prompts.contains(Prompt.CONSENT)) {
            throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
        }
        // There is no need to present the consent page:
        // If Client is a Trusted Client.
        // If a client is configured for pairwise identifiers, and the openid scope is the only scope requested.
        // Also, we should make sure that the claims request is not enabled.
        if (client.getTrustedClient() || isPairwiseWithOnlyOpenIdScope(client, authzRequest, scopes)) {
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
                    throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
                }
            }
        }
        return new Pair<>(clientAuthorization, clientAuthorizationFetched);
    }

    private boolean isPairwiseWithOnlyOpenIdScope(Client client, AuthzRequest authzRequest, Set<String> scopes) {
        return client.getSubjectType() == SubjectType.PAIRWISE
                && scopes.size() == 1
                && scopes.contains(DefaultScope.OPEN_ID.toString())
                && authzRequest.getClaims() == null
                && (authzRequest.getJwtRequest() == null || (authzRequest.getJwtRequest().getUserInfoMember() == null && authzRequest.getJwtRequest().getIdTokenMember() == null));
    }

    private void validateRequestJwt(AuthzRequest authzRequest, boolean isPar, Client client) {
        if (!cibaRequestService.hasCibaCompatibility(client) && !isPar) {
            if (appConfiguration.isFapi() && authzRequest.getJwtRequest() == null) {
                throw authzRequest.getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.INVALID_REQUEST);
            }
            authorizeRestWebServiceValidator.validateRequestJwt(authzRequest.getRequest(), authzRequest.getRequestUri(), authzRequest.getRedirectUriResponse());
        }
    }

    private void checkResponseType(AuthzRequest authzRequest, List<ResponseType> responseTypes, Client client) {
        final boolean isResponseTypeValid = AuthorizeParamsValidator.validateResponseTypes(responseTypes, client)
                && AuthorizeParamsValidator.validateGrantType(responseTypes, client.getGrantTypes(), appConfiguration);

        if (!isResponseTypeValid) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNSUPPORTED_RESPONSE_TYPE, authzRequest.getState(), ""))
                    .build());
        }
    }

    private void checkForceAuthorization(AuthzRequest authzRequest, List<Prompt> prompts, Client client, ExternalPostAuthnContext postAuthnContext) {
        final boolean forceAuthorization = externalPostAuthnService.externalForceAuthorization(client, postAuthnContext);
        if (forceAuthorization) {
            throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
        }
    }

    private void checkForceReAuthentication(AuthzRequest authzRequest, List<Prompt> prompts, Client client, ExternalPostAuthnContext postAuthnContext) {
        final boolean forceReAuthentication = externalPostAuthnService.externalForceReAuthentication(client, postAuthnContext);
        if (forceReAuthentication) {
            unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
            authzRequest.setSessionId(null);

            throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
        }
    }

    private void validateMaxAge(AuthzRequest authzRequest, List<Prompt> prompts, SessionId sessionUser, Client client) {
        boolean validAuthenticationMaxAge = authorizeRestWebServiceValidator.isAuthnMaxAgeValid(authzRequest.getMaxAge(), sessionUser, client);
        if (!validAuthenticationMaxAge) {
            unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
            authzRequest.setSessionId(null);

            throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
        }
    }

    public void checkOfflineAccessScopes(List<ResponseType> responseTypes, List<Prompt> prompts, Client client, Set<String> scopes) {
        if (!scopes.contains(ScopeConstants.OFFLINE_ACCESS) || client.getTrustedClient()) {
            return;
        }

        if (!responseTypes.contains(ResponseType.CODE)) {
            log.trace("Removed (ignored) offline_scope. Can't find `code` in response_type which is required.");
            scopes.remove(ScopeConstants.OFFLINE_ACCESS);
        }

        if (scopes.contains(ScopeConstants.OFFLINE_ACCESS) && !prompts.contains(Prompt.CONSENT) && !toBoolean(client.getAttributes().getAllowOfflineAccessWithoutConsent())) {
            log.error("Removed offline_access. Can't find prompt=consent. Consent is required for offline_access.");
            scopes.remove(ScopeConstants.OFFLINE_ACCESS);
        }
    }

    private Pair<User, SessionId> ifUserIsNull(AuthzRequest authzRequest) throws SearchException {
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
                    applicationAuditLogger.sendMessage(authzRequest.getAuditLog());
                    throw new WebApplicationException(authzRequest.getRedirectUriResponse().createErrorBuilder(AuthorizeErrorResponseType.LOGIN_REQUIRED).build());
                }
            } else {
                throw new WebApplicationException(authzRequest.getRedirectUriResponse().createErrorBuilder(AuthorizeErrorResponseType.LOGIN_REQUIRED).build());
            }
        } else {
            if (prompts.contains(Prompt.LOGIN)) {
                unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest(), authzRequest.isPromptFromJwt());
                authzRequest.setSessionId(null);
                prompts.remove(Prompt.LOGIN);
                authzRequest.setPrompt(implode(prompts, " "));
            }

            throw new WebApplicationException(redirectToAuthorizationPage(authzRequest, prompts));
        }
    }

    private String getAcrForGrant(String acrValuesStr, SessionId sessionUser) {
        final String acr = sessionIdService.getAcr(sessionUser);
        return StringUtils.isNotBlank(acr) ? acr : acrValuesStr;
    }

    private void runCiba(String authReqId, Client client, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (StringUtils.isBlank(authReqId)) {
            return;
        }

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
        executionContext.setIncludeIdTokenClaims(Boolean.TRUE.equals(appConfiguration.getLegacyIdTokenClaims()));

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

    private Response redirectToAuthorizationPage(AuthzRequest authzRequest, List<Prompt> prompts) {
        return redirectTo("/authorize", authzRequest, prompts);
    }

    private Response redirectToSelectAccountPage(AuthzRequest authzRequest, List<Prompt> prompts) {
        return redirectTo("/selectAccount", authzRequest, prompts);
    }

    private Response redirectTo(String pathToRedirect, AuthzRequest authzRequest, List<Prompt> prompts) {

        final URI contextUri = URI.create(appConfiguration.getIssuer()).resolve(servletRequest.getContextPath() + pathToRedirect + configurationFactory.getFacesMapping());

        final RedirectUri redirect = authzRequest.getRedirectUriResponse().getRedirectUri();
        redirect.setBaseRedirectUri(contextUri.toString());
        redirect.setResponseMode(ResponseMode.QUERY);

        // oAuth parameters
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.RESPONSE_TYPE, authzRequest.getResponseType());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.SCOPE, authzRequest.getScope());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.CLIENT_ID, authzRequest.getClientId());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.REDIRECT_URI, authzRequest.getRedirectUri());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.STATE, authzRequest.getState());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.RESPONSE_MODE, authzRequest.getResponseMode());

        // OIC parameters
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.NONCE, authzRequest.getNonce());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.DISPLAY, authzRequest.getDisplay());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.PROMPT, implode(prompts, " "));
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.MAX_AGE, authzRequest.getMaxAge() != null ? authzRequest.getMaxAge().toString() : null);
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.UI_LOCALES, authzRequest.getUiLocales());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.ID_TOKEN_HINT, authzRequest.getIdTokenHint());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.LOGIN_HINT, authzRequest.getLoginHint());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.ACR_VALUES, authzRequest.getAcrValues());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.AMR_VALUES, authzRequest.getAmrValues());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.REQUEST, authzRequest.getRequest());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.REQUEST_URI, authzRequest.getRequestUri());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.CODE_CHALLENGE, authzRequest.getCodeChallenge());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.CODE_CHALLENGE_METHOD, authzRequest.getCodeChallengeMethod());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.SESSION_ID, authzRequest.getSessionId());
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.CLAIMS, authzRequest.getClaims());

        // CIBA param
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.AUTH_REQ_ID, authzRequest.getAuthReqId());

        // mod_ox param
        redirect.addResponseParameterIfNotBlank(AuthorizeRequestParam.ORIGIN_HEADERS, authzRequest.getOriginHeaders());

        final Map<String, String> customParameters = authzRequest.getCustomParameters();
        if (customParameters != null && customParameters.size() > 0) {
            for (Entry<String, String> entry : customParameters.entrySet()) {
                redirect.addResponseParameter(entry.getKey(), entry.getValue());
            }
        }

        ResponseBuilder builder = RedirectUtil.getRedirectResponseBuilder(redirect, authzRequest.getHttpRequest());
        if (authzRequest.getAuditLog() != null) {
            applicationAuditLogger.sendMessage(authzRequest.getAuditLog());
        }
        return builder.build();
    }

    private void updateSession(AuthzRequest authzRequest, SessionId sessionUser) {
        authzRequestService.addDeviceSecretToSession(authzRequest, sessionUser);

        int rpRedirectCount = Util.parseIntSilently(sessionUser.getSessionAttributes().get(SUCCESSFUL_RP_REDIRECT_COUNT), 0);
        rpRedirectCount++;

        sessionUser.getSessionAttributes().put(SUCCESSFUL_RP_REDIRECT_COUNT, Integer.toString(rpRedirectCount));
        sessionIdService.updateSessionId(sessionUser);
    }

    private boolean unauthenticateSession(String sessionId, HttpServletRequest httpRequest) {
        return unauthenticateSession(sessionId, httpRequest, false);
    }

    private boolean unauthenticateSession(String sessionId, HttpServletRequest httpRequest, boolean isPromptFromJwt) {
        SessionId sessionUser = identity.getSessionId();

        if (isPromptFromJwt && sessionUser != null && !sessionUser.getSessionAttributes().containsKey(SUCCESSFUL_RP_REDIRECT_COUNT)) {
            return false; // skip unauthentication because there were no at least one successful rp redirect
        }

        if (sessionUser != null) {
            sessionUser.setUserDn(null);
            sessionUser.setUser(null);
            sessionUser.setAuthenticationTime(null);
        }

        identity.logout();

        if (StringHelper.isEmpty(sessionId)) {
            sessionId = cookieService.getSessionIdFromCookie(httpRequest);
        }

        SessionId persistenceSessionId = sessionIdService.getSessionId(sessionId);
        if (persistenceSessionId == null) {
            log.error("Failed to load session from LDAP by session_id: '{}'", sessionId);
            return true;
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
        return result;
    }

    /**
     * Processes an authorization granted for device code grant type.
     *
     * @param userCode User code used in the device code flow.
     * @param user     Authenticated user that is giving the permissions.
     */
    private void processDeviceAuthorization(String userCode, User user) {
        if (StringUtils.isBlank(userCode)) {
            return;
        }

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
