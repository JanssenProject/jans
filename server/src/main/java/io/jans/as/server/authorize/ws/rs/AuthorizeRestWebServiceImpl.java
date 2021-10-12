/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.binding.TokenBindingMessage;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBAPingCallbackService;
import io.jans.as.server.ciba.CIBAPushTokenDeliveryService;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.authorize.AuthorizeParamsValidator;
import io.jans.as.server.model.authorize.Claim;
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
import io.jans.as.server.model.exception.InvalidSessionStateException;
import io.jans.as.server.model.ldap.ClientAuthorization;
import io.jans.as.server.model.token.JwrService;
import io.jans.as.server.par.ws.rs.ParService;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.AttributeService;
import io.jans.as.server.service.AuthenticationFilterService;
import io.jans.as.server.service.ClientAuthorizationsService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.DeviceAuthorizationService;
import io.jans.as.server.service.RedirectUriResponse;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.ServerCryptoProvider;
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
import io.jans.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
 * @version September 9, 2021
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
    CookieService cookieService;

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
    private WebKeysConfiguration webKeysConfiguration;

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

    @Inject
    private AttributeService attributeService;

    @Inject
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    private ParService parService;

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
        return requestAuthorization(scope, responseType, clientId, redirectUri, state, responseMode, nonce, display,
                prompt, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri,
                sessionId, HttpMethod.GET, originHeaders, codeChallenge, codeChallengeMethod,
                customResponseHeaders, claims, authReqId, httpRequest, httpResponse, securityContext);
    }

    @Override
    public Response requestAuthorizationPost(
            String scope, String responseType, String clientId, String redirectUri, String state, String responseMode,
            String nonce, String display, String prompt, Integer maxAge, String uiLocales, String idTokenHint,
            String loginHint, String acrValues, String amrValues, String request, String requestUri,
            String sessionId, String originHeaders,
            String codeChallenge, String codeChallengeMethod, String customResponseHeaders, String claims,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {
        return requestAuthorization(scope, responseType, clientId, redirectUri, state, responseMode, nonce, display,
                prompt, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri,
                sessionId, HttpMethod.POST, originHeaders, codeChallenge, codeChallengeMethod,
                customResponseHeaders, claims, null, httpRequest, httpResponse, securityContext);
    }

    private Response requestAuthorization(
            String scope, String responseType, String clientId, String redirectUri, String state, String respMode,
            String nonce, String display, String prompt, Integer maxAge, String uiLocalesStr, String idTokenHint,
            String loginHint, String acrValuesStr, String amrValuesStr, String request, String requestUri,
            String sessionId, String method, String originHeaders, String codeChallenge, String codeChallengeMethod,
            String customRespHeaders, String claims, String authReqId,
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
                        + "state = {}, request = {}, isSecure = {}, sessionId = {}",
                responseType, clientId, scope, redirectUri, nonce,
                state, request, securityContext.isSecure(), sessionId);

        log.debug("Attempting to request authorization: "
                        + "acrValues = {}, amrValues = {}, originHeaders = {}, codeChallenge = {}, codeChallengeMethod = {}, "
                        + "customRespHeaders = {}, claims = {}, tokenBindingHeader = {}",
                acrValuesStr, amrValuesStr, originHeaders, codeChallenge, codeChallengeMethod, customRespHeaders, claims, tokenBindingHeader);

        ResponseBuilder builder = Response.ok();

        Map<String, String> customParameters = requestParameterService.getCustomParameters(QueryStringDecoder.decode(httpRequest.getQueryString()));

        boolean isPar = Util.isPar(requestUri);
        if (!isPar && isTrue(appConfiguration.getRequirePar())) {
            log.debug("Server configured for PAR only (via requirePar conf property). Failed to find PAR by request_uri (id): {}", requestUri);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Failed to find par by request_uri"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        if (isPar) {
            final Par par = parService.getParAndValidateForAuthorizationRequest(requestUri, state, clientId);

            requestUri = null; // set it to null, we don't want to follow request uri for PAR
            request = null; // request is validated and parameters parsed by PAR endpoint before PAR persistence

            log.debug("Setting request parameters from PAR - {}", par);

            responseType = par.getAttributes().getResponseType();
            respMode = par.getAttributes().getResponseMode();
            scope = par.getAttributes().getScope();
            prompt = par.getAttributes().getPrompt();
            redirectUri = par.getAttributes().getRedirectUri();
            acrValuesStr = par.getAttributes().getAcrValuesStr();
            amrValuesStr = par.getAttributes().getAmrValuesStr();
            codeChallenge = par.getAttributes().getCodeChallenge();
            codeChallengeMethod = par.getAttributes().getCodeChallengeMethod();

            if (StringUtils.isNotBlank(par.getAttributes().getNonce()))
                nonce = par.getAttributes().getNonce();
            if (StringUtils.isNotBlank(par.getAttributes().getState()))
                state = par.getAttributes().getState();
            if (StringUtils.isNotBlank(par.getAttributes().getSessionId()))
                sessionId = par.getAttributes().getSessionId();
            if (StringUtils.isNotBlank(par.getAttributes().getCustomResponseHeaders()))
                customRespHeaders = par.getAttributes().getCustomResponseHeaders();
            if (StringUtils.isNotBlank(par.getAttributes().getClaims()))
                claims = par.getAttributes().getClaims();
            if (StringUtils.isNotBlank(par.getAttributes().getOriginHeaders()))
                originHeaders = par.getAttributes().getOriginHeaders();
            if (StringUtils.isNotBlank(par.getAttributes().getUiLocales()))
                uiLocalesStr = par.getAttributes().getUiLocales();
            if (!par.getAttributes().getCustomParameters().isEmpty())
                customParameters.putAll(par.getAttributes().getCustomParameters());
        }

        List<String> uiLocales = Util.splittedStringAsList(uiLocalesStr, " ");
        List<ResponseType> responseTypes = ResponseType.fromString(responseType, " ");
        List<Prompt> prompts = Prompt.fromString(prompt, " ");
        List<String> acrValues = Util.splittedStringAsList(acrValuesStr, " ");
        List<String> amrValues = Util.splittedStringAsList(amrValuesStr, " ");
        ResponseMode responseMode = ResponseMode.getByValue(respMode);

        SessionId sessionUser = identity.getSessionId();
        User user = sessionIdService.getUser(sessionUser);

        try {
            Map<String, String> customResponseHeaders = Util.jsonObjectArrayStringAsMap(customRespHeaders);

            updateSessionForROPC(httpRequest, sessionUser);

            Client client = authorizeRestWebServiceValidator.validateClient(clientId, state, isPar);
            String deviceAuthzUserCode = deviceAuthorizationService.getUserCodeFromSession(httpRequest);
            redirectUri = authorizeRestWebServiceValidator.validateRedirectUri(client, redirectUri, state, deviceAuthzUserCode, httpRequest);
            checkAcrChanged(acrValuesStr, prompts, sessionUser); // check after redirect uri is validated

            RedirectUriResponse redirectUriResponse = new RedirectUriResponse(new RedirectUri(redirectUri, responseTypes, responseMode), state, httpRequest, errorResponseFactory);
            redirectUriResponse.setFapiCompatible(appConfiguration.isFapi());

            Set<String> scopes = scopeChecker.checkScopesPolicy(client, scope);

            JwtAuthorizationRequest jwtRequest = null;
            if (StringUtils.isNotBlank(request) || StringUtils.isNotBlank(requestUri)) {
                try {
                    jwtRequest = JwtAuthorizationRequest.createJwtRequest(request, requestUri, client, redirectUriResponse, cryptoProvider, appConfiguration);

                    if (jwtRequest == null) {
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "Failed to parse jwt.");
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getState())) {
                        state = jwtRequest.getState();
                        redirectUriResponse.setState(state);
                    }
                    if (appConfiguration.isFapi() && StringUtils.isBlank(jwtRequest.getState())) {
                        state = ""; // #1250 - FAPI : discard state if in JWT we don't have state
                        redirectUriResponse.setState("");
                    }

                    authorizeRestWebServiceValidator.validateRequestObject(jwtRequest, redirectUriResponse);

                    // MUST be equal
                    if (!jwtRequest.getResponseTypes().containsAll(responseTypes) || !responseTypes.containsAll(jwtRequest.getResponseTypes())) {
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "The responseType parameter is not the same in the JWT");
                    }
                    if (StringUtils.isBlank(jwtRequest.getClientId()) || !jwtRequest.getClientId().equals(clientId)) {
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "The clientId parameter is not the same in the JWT");
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
                        throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "The redirect_uri parameter is not the same in the JWT");
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getNonce())) {
                        nonce = jwtRequest.getNonce();
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getCodeChallenge())) {
                        codeChallenge = jwtRequest.getCodeChallenge();
                    }
                    if (StringUtils.isNotBlank(jwtRequest.getCodeChallengeMethod())) {
                        codeChallengeMethod = jwtRequest.getCodeChallengeMethod();
                    }
                    if (jwtRequest.getDisplay() != null && StringUtils.isNotBlank(jwtRequest.getDisplay().getParamName())) {
                        display = jwtRequest.getDisplay().getParamName();
                    }
                    if (!jwtRequest.getPrompts().isEmpty()) {
                        prompts = Lists.newArrayList(jwtRequest.getPrompts());
                    }
                    if (jwtRequest.getResponseMode() != null) {
                        redirectUriResponse.getRedirectUri().setResponseMode(jwtRequest.getResponseMode());
                        responseMode = jwtRequest.getResponseMode();
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
                    throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "Invalid JWT authorization request");
                }
            }
            if (!cibaRequestService.hasCibaCompatibility(client) && !isPar) {
                if (appConfiguration.isFapi() && jwtRequest == null) {
                    throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST);
                }
                authorizeRestWebServiceValidator.validateRequestJwt(request, requestUri, redirectUriResponse);
            }
            authorizeRestWebServiceValidator.validate(responseTypes, prompts, nonce, state, redirectUri, httpRequest, client, responseMode);

            if (CollectionUtils.isEmpty(acrValues) && !ArrayUtils.isEmpty(client.getDefaultAcrValues())) {
                acrValues = Lists.newArrayList(client.getDefaultAcrValues());
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
                            codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
                }
            }

            boolean validAuthenticationMaxAge = authorizeRestWebServiceValidator.validateAuthnMaxAge(maxAge, sessionUser, client);
            if (!validAuthenticationMaxAge) {
                unauthenticateSession(sessionId, httpRequest);
                sessionId = null;

                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
            }

            oAuth2AuditLog.setUsername(user != null ? user.getUserId() : "");

            ExternalPostAuthnContext postAuthnContext = new ExternalPostAuthnContext(client, sessionUser, httpRequest, httpResponse);
            final boolean forceReAuthentication = externalPostAuthnService.externalForceReAuthentication(client, postAuthnContext);
            if (forceReAuthentication) {
                unauthenticateSession(sessionId, httpRequest);
                sessionId = null;

                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
            }

            final boolean forceAuthorization = externalPostAuthnService.externalForceAuthorization(client, postAuthnContext);
            if (forceAuthorization) {
                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
            }

            ClientAuthorization clientAuthorization = null;
            boolean clientAuthorizationFetched = false;
            if (scopes.size() > 0) {
                if (prompts.contains(Prompt.CONSENT)) {
                    return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                            redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                            idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                            codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
                }
                if (client.getTrustedClient()) {
                    sessionUser.addPermission(clientId, true);
                    sessionIdService.updateSessionId(sessionUser);
                } else {
                    clientAuthorization = clientAuthorizationsService.find(user.getAttribute("inum"), client.getClientId());
                    clientAuthorizationFetched = true;
                    if (clientAuthorization != null && clientAuthorization.getScopes() != null) {
                        log.trace("ClientAuthorization - scope: " + scope + ", dn: " + clientAuthorization.getDn() + ", requestedScope: " + scopes);
                        if (Arrays.asList(clientAuthorization.getScopes()).containsAll(scopes)) {
                            sessionUser.addPermission(clientId, true);
                            sessionIdService.updateSessionId(sessionUser);
                        } else {
                            return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                                    redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                                    idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                                    codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
                        }
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
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
            }

            if (prompts.contains(Prompt.CONSENT) || !isTrue(sessionUser.isPermissionGrantedForClient(clientId))) {
                if (!clientAuthorizationFetched) {
                    clientAuthorization = clientAuthorizationsService.find(user.getAttribute("inum"), client.getClientId());
                }
                clientAuthorizationsService.clearAuthorizations(clientAuthorization, client.getPersistClientAuthorizations());

                prompts.remove(Prompt.CONSENT);

                return redirectToAuthorizationPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
            }

            if (prompts.contains(Prompt.SELECT_ACCOUNT)) {
                return redirectToSelectAccountPage(redirectUriResponse.getRedirectUri(), responseTypes, scope, clientId,
                        redirectUri, state, responseMode, nonce, display, prompts, maxAge, uiLocales,
                        idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                        codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
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
                authorizationGrant.setAcrValues(getAcrForGrant(acrValuesStr, sessionUser));
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
                    authorizationGrant.setAcrValues(getAcrForGrant(acrValuesStr, sessionUser));
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
                    authorizationGrant.setAcrValues(getAcrForGrant(acrValuesStr, sessionUser));
                    authorizationGrant.setSessionDn(sessionUser.getDn());
                    authorizationGrant.save(); // call save after object modification, call is asynchronous!!!
                }

                ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(httpRequest, authorizationGrant, client, appConfiguration, attributeService);
                Function<JsonWebResponse, Void> postProcessor = externalUpdateTokenService.buildModifyIdTokenProcessor(context);

                IdToken idToken = authorizationGrant.createIdToken(
                        nonce, authorizationCode, newAccessToken, null,
                        state, authorizationGrant, includeIdTokenClaims,
                        JwrService.wrapWithSidFunction(TokenBindingMessage.createIdTokenTokingBindingPreprocessing(tokenBindingHeader, client.getIdTokenTokenBindingCnf()), sessionUser.getOutsideSid()),
                        postProcessor);

                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.ID_TOKEN, idToken.getCode());
            }

            if (authorizationGrant != null && StringHelper.isNotEmpty(acrValuesStr) && !appConfiguration.isFapi()) {
                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.ACR_VALUES, acrValuesStr);
            }

            if (sessionUser.getId() == null) {
                final SessionId newSessionUser = sessionIdService.generateAuthenticatedSessionId(httpRequest, sessionUser.getUserDn(), prompt);
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
            redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SESSION_STATE, sessionIdService.computeSessionState(sessionUser, clientId, redirectUri));
            redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.STATE, state);
            if (scope != null && !scope.isEmpty() && authorizationGrant != null && !appConfiguration.isFapi()) {
                scope = authorizationGrant.checkScopesPolicy(scope);

                redirectUriResponse.getRedirectUri().addResponseParameter(AuthorizeResponseParam.SCOPE, scope);
            }

            clientService.updateAccessTime(client, false);
            oAuth2AuditLog.setSuccess(true);

            // JARM
            if (responseMode == ResponseMode.QUERY_JWT || responseMode == ResponseMode.FRAGMENT_JWT ||
                    responseMode == ResponseMode.JWT || responseMode == ResponseMode.FORM_POST_JWT) {
                redirectUriResponse.getRedirectUri().setIssuer(appConfiguration.getIssuer());
                redirectUriResponse.getRedirectUri().setAudience(clientId);
                redirectUriResponse.getRedirectUri().setAuthorizationCodeLifetime(appConfiguration.getAuthorizationCodeLifetime());
                redirectUriResponse.getRedirectUri().setSignatureAlgorithm(SignatureAlgorithm.fromString(client.getAttributes().getAuthorizationSignedResponseAlg()));
                redirectUriResponse.getRedirectUri().setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.fromName(client.getAttributes().getAuthorizationEncryptedResponseAlg()));
                redirectUriResponse.getRedirectUri().setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.fromName(client.getAttributes().getAuthorizationEncryptedResponseEnc()));
                redirectUriResponse.getRedirectUri().setCryptoProvider(cryptoProvider);

                String keyId = null;
                if (client.getAttributes().getAuthorizationEncryptedResponseAlg() != null && client.getAttributes().getAuthorizationEncryptedResponseEnc() != null) {
                    if (client.getAttributes().getAuthorizationSignedResponseAlg() != null) { // Signed then Encrypted response
                        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(client.getAttributes().getAuthorizationSignedResponseAlg());

                        String nestedKeyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration,
                                Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE);

                        JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(client.getJwksUri());
                        redirectUriResponse.getRedirectUri().setNestedJsonWebKeys(jsonWebKeys);

                        String clientSecret = clientService.decryptSecret(client.getClientSecret());
                        redirectUriResponse.getRedirectUri().setNestedSharedSecret(clientSecret);
                        redirectUriResponse.getRedirectUri().setNestedKeyId(nestedKeyId);
                    }

                    // Encrypted response
                    JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(authorizationGrant.getClient().getJwksUri());
                    if (jsonWebKeys != null) {
                        keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys),
                                Algorithm.fromString(client.getAttributes().getAuthorizationEncryptedResponseAlg()),
                                Use.ENCRYPTION);
                    }
                    String sharedSecret = clientService.decryptSecret(authorizationGrant.getClient().getClientSecret());
                    byte[] sharedSymmetricKey = sharedSecret.getBytes(StandardCharsets.UTF_8);
                    redirectUriResponse.getRedirectUri().setSharedSymmetricKey(sharedSymmetricKey);
                    redirectUriResponse.getRedirectUri().setJsonWebKeys(jsonWebKeys);
                    redirectUriResponse.getRedirectUri().setKeyId(keyId);
                } else { // Signed response
                    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
                    if (client.getAttributes().getAuthorizationSignedResponseAlg() != null) {
                        signatureAlgorithm = SignatureAlgorithm.fromString(client.getAttributes().getAuthorizationSignedResponseAlg());
                    }

                    keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration,
                            Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE);

                    JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(client.getJwksUri());
                    redirectUriResponse.getRedirectUri().setJsonWebKeys(jsonWebKeys);

                    String clientSecret = clientService.decryptSecret(client.getClientSecret());
                    redirectUriResponse.getRedirectUri().setSharedSecret(clientSecret);
                    redirectUriResponse.getRedirectUri().setKeyId(keyId);
                }
            }

            builder = RedirectUtil.getRedirectResponseBuilder(redirectUriResponse.getRedirectUri(), httpRequest);

            if (isTrue(appConfiguration.getCustomHeadersWithAuthorizationResponse())) {
                for (Entry<String, String> entry : customResponseHeaders.entrySet()) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }

            if (StringUtils.isNotBlank(authReqId)) {
                runCiba(authReqId, client, httpRequest, httpResponse);
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

        AccessToken accessToken = cibaGrant.createAccessToken(httpRequest.getHeader("X-ClientCert"), new ExecutionContext(httpRequest, httpResponse));
        log.debug("Issuing access token: {}", accessToken.getCode());

        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(httpRequest, cibaGrant, client, appConfiguration, attributeService);
        Function<JsonWebResponse, Void> postProcessor = externalUpdateTokenService.buildModifyIdTokenProcessor(context);

        final int refreshTokenLifetimeInSeconds = externalUpdateTokenService.getRefreshTokenLifetimeInSeconds(context);
        final RefreshToken refreshToken;
        if (refreshTokenLifetimeInSeconds > 0) {
            refreshToken = cibaGrant.createRefreshToken(refreshTokenLifetimeInSeconds);
        } else {
            refreshToken = cibaGrant.createRefreshToken();
        }
        log.debug("Issuing refresh token: {}", (refreshToken != null ? refreshToken.getCode() : ""));

        IdToken idToken = cibaGrant.createIdToken(
                null, null, accessToken, refreshToken,
                null, cibaGrant, false, null, postProcessor);

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
                    sessionUser.getSessionAttributes().put("prompt", implode(prompts, " "));
                    if (!sessionIdService.persistSessionId(sessionUser)) {
                        log.trace("Unable persist session_id, trying to update it.");
                        sessionIdService.updateSessionId(sessionUser);
                    }
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
                                                 Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog, HttpServletRequest httpRequest) {
        return redirectTo("/authorize", redirectUriResponse, responseTypes, scope, clientId, redirectUri,
                state, responseMode, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
    }

    private Response redirectToSelectAccountPage(RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
                                                 String redirectUri, String state, ResponseMode responseMode, String nonce, String display,
                                                 List<Prompt> prompts, Integer maxAge, List<String> uiLocales, String idTokenHint, String loginHint,
                                                 List<String> acrValues, List<String> amrValues, String request, String requestUri, String originHeaders,
                                                 String codeChallenge, String codeChallengeMethod, String sessionId, String claims, String authReqId,
                                                 Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog, HttpServletRequest httpRequest) {
        return redirectTo("/selectAccount", redirectUriResponse, responseTypes, scope, clientId, redirectUri,
                state, responseMode, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint, acrValues, amrValues, request, requestUri, originHeaders,
                codeChallenge, codeChallengeMethod, sessionId, claims, authReqId, customParameters, oAuth2AuditLog, httpRequest);
    }

    private Response redirectTo(String pathToRedirect,
                                RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
                                String redirectUri, String state, ResponseMode responseMode, String nonce, String display,
                                List<Prompt> prompts, Integer maxAge, List<String> uiLocales, String idTokenHint, String loginHint,
                                List<String> acrValues, List<String> amrValues, String request, String requestUri, String originHeaders,
                                String codeChallenge, String codeChallengeMethod, String sessionId, String claims, String authReqId,
                                Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog, HttpServletRequest httpRequest) {

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
        if (StringUtils.isNotBlank(sessionId) && isTrue(appConfiguration.getSessionIdRequestParameterEnabled())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.SESSION_ID, sessionId);
        }
        if (StringUtils.isNotBlank(claims)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.CLAIMS, claims);
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
            for (Entry<String, String> entry : customParameters.entrySet()) {
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