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
import io.jans.as.model.common.*;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.binding.TokenBindingMessage;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBAPingCallbackService;
import io.jans.as.server.ciba.CIBAPushTokenDeliveryService;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.authorize.*;
import io.jans.as.server.model.common.*;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.exception.AcrChangedException;
import io.jans.as.server.model.exception.InvalidRedirectUrlException;
import io.jans.as.server.model.exception.InvalidSessionStateException;
import io.jans.as.server.model.ldap.ClientAuthorization;
import io.jans.as.server.model.token.JwrService;
import io.jans.as.server.par.ws.rs.ParService;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
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
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;

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
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.*;
import java.util.Map.Entry;
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
        authzRequest.setScope(ServerUtil.urlDecode(authzRequest.getScope())); // it may be encoded in uma case

        String tokenBindingHeader = authzRequest.getHttpRequest().getHeader("Sec-Token-Binding");

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(authzRequest.getHttpRequest()), Action.USER_AUTHORIZATION);
        oAuth2AuditLog.setClientId(authzRequest.getClientId());
        oAuth2AuditLog.setScope(authzRequest.getScope());

        log.debug("Attempting to request authorization: {}", authzRequest);

        ResponseBuilder builder = null;

        Map<String, String> customParameters = requestParameterService.getCustomParameters(QueryStringDecoder.decode(authzRequest.getHttpRequest().getQueryString()));

        boolean isPar = Util.isPar(authzRequest.getRequestUri());
        if (!isPar && isTrue(appConfiguration.getRequirePar())) {
            log.debug("Server configured for PAR only (via requirePar conf property). Failed to find PAR by request_uri (id): {}", authzRequest.getRequestUri());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState(), "Failed to find par by request_uri"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        if (isPar) {
            final Par par = parService.getParAndValidateForAuthorizationRequest(authzRequest.getRequestUri(), authzRequest.getState(), authzRequest.getClientId());

            authzRequest.setRequestUri(null); // set it to null, we don't want to follow request uri for PAR
            authzRequest.setRequest(null); // request is validated and parameters parsed by PAR endpoint before PAR persistence

            log.debug("Setting request parameters from PAR - {}", par);

            authzRequest.setResponseType(par.getAttributes().getResponseType());
            authzRequest.setResponseMode(par.getAttributes().getResponseMode());
            authzRequest.setScope(par.getAttributes().getScope());
            authzRequest.setPrompt(par.getAttributes().getPrompt());
            authzRequest.setRedirectUri(par.getAttributes().getRedirectUri());
            authzRequest.setAcrValues(par.getAttributes().getAcrValuesStr());
            authzRequest.setAmrValues(par.getAttributes().getAmrValuesStr());
            authzRequest.setCodeChallenge(par.getAttributes().getCodeChallenge());
            authzRequest.setCodeChallengeMethod(par.getAttributes().getCodeChallengeMethod());

            authzRequest.setState(StringUtils.isNotBlank(par.getAttributes().getState()) ? par.getAttributes().getState() : "");

            if (StringUtils.isNotBlank(par.getAttributes().getNonce()))
                authzRequest.setNonce(par.getAttributes().getNonce());
            if (StringUtils.isNotBlank(par.getAttributes().getSessionId()))
                authzRequest.setSessionId(par.getAttributes().getSessionId());
            if (StringUtils.isNotBlank(par.getAttributes().getCustomResponseHeaders()))
                authzRequest.setCustomResponseHeaders(par.getAttributes().getCustomResponseHeaders());
            if (StringUtils.isNotBlank(par.getAttributes().getClaims()))
                authzRequest.setClaims(par.getAttributes().getClaims());
            if (StringUtils.isNotBlank(par.getAttributes().getOriginHeaders()))
                authzRequest.setOriginHeaders(par.getAttributes().getOriginHeaders());
            if (StringUtils.isNotBlank(par.getAttributes().getUiLocales()))
                authzRequest.setUiLocales(par.getAttributes().getUiLocales());
            if (!par.getAttributes().getCustomParameters().isEmpty())
                customParameters.putAll(par.getAttributes().getCustomParameters());
        }

        List<String> uiLocales = Util.splittedStringAsList(authzRequest.getUiLocales(), " ");
        List<ResponseType> responseTypes = ResponseType.fromString(authzRequest.getResponseType(), " ");
        List<Prompt> prompts = Prompt.fromString(authzRequest.getPrompt(), " ");
        List<String> acrValues = Util.splittedStringAsList(authzRequest.getAcrValues(), " ");
        List<String> amrValues = Util.splittedStringAsList(authzRequest.getAmrValues(), " ");
        ResponseMode responseMode = ResponseMode.getByValue(authzRequest.getResponseMode());

        SessionId sessionUser = identity.getSessionId();
        User user = sessionIdService.getUser(sessionUser);

        try {
            Map<String, String> customResponseHeaders = Util.jsonObjectArrayStringAsMap(authzRequest.getCustomResponseHeaders());

            updateSessionForROPC(authzRequest.getHttpRequest(), sessionUser);

            Client client = authorizeRestWebServiceValidator.validateClient(authzRequest.getClientId(), authzRequest.getState(), isPar);
            String deviceAuthzUserCode = deviceAuthorizationService.getUserCodeFromSession(authzRequest.getHttpRequest());
            authzRequest.setRedirectUri(authorizeRestWebServiceValidator.validateRedirectUri(client, authzRequest.getRedirectUri(), authzRequest.getState(), deviceAuthzUserCode, authzRequest.getHttpRequest()));
            RedirectUriResponse redirectUriResponse = new RedirectUriResponse(new RedirectUri(authzRequest.getRedirectUri(), responseTypes, responseMode), authzRequest.getState(), authzRequest.getHttpRequest(), errorResponseFactory);
            redirectUriResponse.setFapiCompatible(appConfiguration.isFapi());

            if (!client.getAttributes().getAuthorizedAcrValues().isEmpty() &&
                    !client.getAttributes().getAuthorizedAcrValues().containsAll(acrValues)) {
                throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST,
                        "Restricted acr value request, please review the list of authorized acr values for this client");
            }
            checkAcrChanged(authzRequest.getAcrValues(), prompts, sessionUser); // check after redirect uri is validated

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
                        responseMode = jwtRequest.getResponseMode();
                        redirectUriResponse.getRedirectUri().setResponseMode(responseMode);
                    }

                    final IdTokenMember idTokenMember = jwtRequest.getIdTokenMember();
                    if (idTokenMember != null) {
                        if (idTokenMember.getMaxAge() != null) {
                            authzRequest.setMaxAge(idTokenMember.getMaxAge());
                        }
                        final Claim acrClaim = idTokenMember.getClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
                        if (acrClaim != null && acrClaim.getClaimValue() != null) {
                            authzRequest.setAcrValues(acrClaim.getClaimValue().getValueAsString());
                            acrValues = Util.splittedStringAsList(authzRequest.getAcrValues(), " ");
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
                    JsonWebResponse jwr = parseRequestToJwr(authzRequest.getRequest());
                    if (jwr != null) {
                        String checkForAlg = jwr.getClaims().getClaimAsString("alg"); // to handle Jans Issue#310
                        if ("none".equals(checkForAlg)) {
                            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                    .entity(errorResponseFactory.getErrorAsJson(
                                            AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, "",
                                            "The None algorithm in nested JWT is not allowed for FAPI"))
                                    .type(MediaType.APPLICATION_JSON_TYPE).build());
                        }
                        responseMode = ResponseMode.getByValue(jwr.getClaims().getClaimAsString("response_mode"));
                        if (responseMode == ResponseMode.JWT) {
                            redirectUriResponse.getRedirectUri().setResponseMode(ResponseMode.JWT);
                            fillRedirectUriResponseforJARM(redirectUriResponse, jwr, client);
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
            if (responseMode == ResponseMode.QUERY_JWT || responseMode == ResponseMode.FRAGMENT_JWT ||
                    responseMode == ResponseMode.JWT || responseMode == ResponseMode.FORM_POST_JWT) {
                JsonWebResponse jwe = parseRequestToJwr(authzRequest.getRequest());
                fillRedirectUriResponseforJARM(redirectUriResponse, jwe, client);
            }
            // Validate JWT request object after JARM check, because we want to return errors well formatted (JSON/JWT).
            if (jwtRequest != null) {
                validateJwtRequest(authzRequest.getClientId(), authzRequest.getState(), authzRequest.getHttpRequest(), responseTypes, redirectUriResponse, jwtRequest);
            }

            if (!cibaRequestService.hasCibaCompatibility(client) && !isPar) {
                if (appConfiguration.isFapi() && jwtRequest == null) {
                    throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST);
                }
                authorizeRestWebServiceValidator.validateRequestJwt(authzRequest.getRequest(), authzRequest.getRequestUri(), redirectUriResponse);
            }

            authorizeRestWebServiceValidator.validate(responseTypes, prompts, authzRequest.getNonce(), authzRequest.getState(), authzRequest.getRedirectUri(), authzRequest.getHttpRequest(), client, responseMode);
            authorizeRestWebServiceValidator.validatePkce(authzRequest.getCodeChallenge(), redirectUriResponse);

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
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNSUPPORTED_RESPONSE_TYPE, authzRequest.getState(), ""))
                        .build());
            }

            AuthorizationGrant authorizationGrant = null;

            if (user == null) {
                identity.logout();
                if (prompts.contains(Prompt.NONE)) {
                    if (authenticationFilterService.isEnabled()) {
                        Map<String, String> params;
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

                            sessionUser = sessionIdService.generateAuthenticatedSessionId(authzRequest.getHttpRequest(), userDn, authzRequest.getPrompt());
                            sessionUser.setSessionAttributes(requestParameterMap);

                            cookieService.createSessionIdCookie(sessionUser, authzRequest.getHttpRequest(), authzRequest.getHttpResponse(), false);
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
                        unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
                        authzRequest.setSessionId(null);
                        prompts.remove(Prompt.LOGIN);
                    }

                    return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                            authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                            acrValues, amrValues, customParameters, oAuth2AuditLog);
                }
            }

            boolean validAuthenticationMaxAge = authorizeRestWebServiceValidator.validateAuthnMaxAge(authzRequest.getMaxAge(), sessionUser, client);
            if (!validAuthenticationMaxAge) {
                unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
                authzRequest.setSessionId(null);

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                        authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                        acrValues, amrValues, customParameters, oAuth2AuditLog);
            }

            oAuth2AuditLog.setUsername(user != null ? user.getUserId() : "");

            ExternalPostAuthnContext postAuthnContext = new ExternalPostAuthnContext(client, sessionUser, authzRequest.getHttpRequest(), authzRequest.getHttpResponse());
            final boolean forceReAuthentication = externalPostAuthnService.externalForceReAuthentication(client, postAuthnContext);
            if (forceReAuthentication) {
                unauthenticateSession(authzRequest.getSessionId(), authzRequest.getHttpRequest());
                authzRequest.setSessionId(null);

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                        authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                        acrValues, amrValues, customParameters, oAuth2AuditLog);
            }

            final boolean forceAuthorization = externalPostAuthnService.externalForceAuthorization(client, postAuthnContext);
            if (forceAuthorization) {
                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                        authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                        acrValues, amrValues, customParameters, oAuth2AuditLog);
            }

            ClientAuthorization clientAuthorization = null;
            boolean clientAuthorizationFetched = false;
            if (!scopes.isEmpty()) {
                if (prompts.contains(Prompt.CONSENT)) {
                    return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                            authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                            acrValues, amrValues, customParameters, oAuth2AuditLog);
                }
                // There is no need to present the consent page:
                // If Client is a Trusted Client.
                // If a client is configured for pairwise identifiers, and the openid scope is the only scope requested.
                // Also, we should make sure that the claims request is not enabled.
                final boolean isPairwiseWithOnlyOpenIdScope = client.getSubjectType() == SubjectType.PAIRWISE
                        && scopes.size() == 1
                        && scopes.contains(DefaultScope.OPEN_ID.toString())
                        && authzRequest.getClaims() == null
                        && (jwtRequest == null || (jwtRequest.getUserInfoMember() == null && jwtRequest.getIdTokenMember() == null));
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
                            return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                                    authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                                    acrValues, amrValues, customParameters, oAuth2AuditLog);
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

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                        authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                        acrValues, amrValues, customParameters, oAuth2AuditLog);
            }

            if (prompts.contains(Prompt.CONSENT) || !isTrue(sessionUser.isPermissionGrantedForClient(authzRequest.getClientId()))) {
                if (!clientAuthorizationFetched) {
                    clientAuthorization = clientAuthorizationsService.find(user.getAttribute("inum"), client.getClientId());
                }
                clientAuthorizationsService.clearAuthorizations(clientAuthorization, client.getPersistClientAuthorizations());

                prompts.remove(Prompt.CONSENT);

                return redirectToAuthorizationPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                        authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                        acrValues, amrValues, customParameters, oAuth2AuditLog);
            }

            if (prompts.contains(Prompt.SELECT_ACCOUNT)) {
                return redirectToSelectAccountPage(authzRequest, redirectUriResponse.getRedirectUri(), responseTypes, authzRequest.getScope(), authzRequest.getClientId(),
                        authzRequest.getRedirectUri(), authzRequest.getState(), responseMode, prompts, authzRequest.getMaxAge(), uiLocales,
                        acrValues, amrValues, customParameters, oAuth2AuditLog);
            }

            AuthorizationCode authorizationCode = null;
            if (responseTypes.contains(ResponseType.CODE)) {
                authorizationGrant = authorizationGrantList.createAuthorizationCodeGrant(user, client,
                        sessionUser.getAuthenticationTime());
                authorizationGrant.setNonce(authzRequest.getNonce());
                authorizationGrant.setJwtAuthorizationRequest(jwtRequest);
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
                    authorizationGrant.setJwtAuthorizationRequest(jwtRequest);
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
                    authorizationGrant.setJwtAuthorizationRequest(jwtRequest);
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

            for (Map.Entry<String, String> customParam : requestParameterService.getCustomParameters(customParameters, true).entrySet()) {
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

            RedirectUri redirectUriResponse = new RedirectUri(authzRequest.getRedirectUri(), responseTypes, responseMode);
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

    @Nullable
    private JsonWebResponse parseRequestToJwr(String request) {
        if (request == null) {
            return null;
        }
        String[] parts = request.split("\\.");
        try {
            if (parts.length == 5) {
                String encodedHeader = parts[0];
                JwtHeader jwtHeader = new JwtHeader(encodedHeader);
                String keyId = jwtHeader.getKeyId();
                PrivateKey privateKey = null;
                KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm
                        .fromName(jwtHeader.getClaimAsString(JwtHeaderName.ALGORITHM));
                if (AlgorithmFamily.RSA.equals(keyEncryptionAlgorithm.getFamily())) {
                    privateKey = cryptoProvider.getPrivateKey(keyId);
                }
                return Jwe.parse(request, privateKey, null);
            }
            return Jwt.parseSilently(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private void fillRedirectUriResponseforJARM(RedirectUriResponse redirectUriResponse, JsonWebResponse jwr, Client client) {
        try {
            if (jwr != null) {
                String tempRedirectUri = jwr.getClaims().getClaimAsString("redirect_uri");
                if (StringUtils.isNotBlank(tempRedirectUri)) {
                    redirectUriResponse.getRedirectUri().setBaseRedirectUri(URLDecoder.decode(tempRedirectUri, "UTF-8"));
                }
            }
            String clientId = client.getClientId();
            redirectUriResponse.getRedirectUri().setIssuer(appConfiguration.getIssuer());
            redirectUriResponse.getRedirectUri().setAudience(clientId);
            redirectUriResponse.getRedirectUri().setAuthorizationCodeLifetime(appConfiguration.getAuthorizationCodeLifetime());
            redirectUriResponse.getRedirectUri().setSignatureAlgorithm(SignatureAlgorithm.fromString(client.getAttributes().getAuthorizationSignedResponseAlg()));
            redirectUriResponse.getRedirectUri().setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.fromName(client.getAttributes().getAuthorizationEncryptedResponseAlg()));
            redirectUriResponse.getRedirectUri().setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.fromName(client.getAttributes().getAuthorizationEncryptedResponseEnc()));
            redirectUriResponse.getRedirectUri().setCryptoProvider(cryptoProvider);

            String keyId = null;
            if (client.getAttributes().getAuthorizationEncryptedResponseAlg() != null
                    && client.getAttributes().getAuthorizationEncryptedResponseEnc() != null) {
                if (client.getAttributes().getAuthorizationSignedResponseAlg() != null) { // Signed then Encrypted
                    // response
                    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
                            .fromString(client.getAttributes().getAuthorizationSignedResponseAlg());

                    String nestedKeyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration,
                            Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE);

                    JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(client.getJwksUri());
                    redirectUriResponse.getRedirectUri().setNestedJsonWebKeys(jsonWebKeys);

                    String clientSecret = clientService.decryptSecret(client.getClientSecret());
                    redirectUriResponse.getRedirectUri().setNestedSharedSecret(clientSecret);
                    redirectUriResponse.getRedirectUri().setNestedKeyId(nestedKeyId);
                }

                // Encrypted response
                JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(client.getJwksUri());
                if (jsonWebKeys != null) {
                    keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys),
                            Algorithm.fromString(client.getAttributes().getAuthorizationEncryptedResponseAlg()),
                            Use.ENCRYPTION);
                }
                String sharedSecret = clientService.decryptSecret(client.getClientSecret());
                byte[] sharedSymmetricKey = sharedSecret.getBytes(StandardCharsets.UTF_8);
                redirectUriResponse.getRedirectUri().setSharedSymmetricKey(sharedSymmetricKey);
                redirectUriResponse.getRedirectUri().setJsonWebKeys(jsonWebKeys);
                redirectUriResponse.getRedirectUri().setKeyId(keyId);
            } else { // Signed response
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
                if (client.getAttributes().getAuthorizationSignedResponseAlg() != null) {
                    signatureAlgorithm = SignatureAlgorithm
                            .fromString(client.getAttributes().getAuthorizationSignedResponseAlg());
                }

                keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration,
                        Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE);

                JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(client.getJwksUri());
                redirectUriResponse.getRedirectUri().setJsonWebKeys(jsonWebKeys);

                String clientSecret = clientService.decryptSecret(client.getClientSecret());
                redirectUriResponse.getRedirectUri().setSharedSecret(clientSecret);
                redirectUriResponse.getRedirectUri().setKeyId(keyId);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void validateJwtRequest(String clientId, String state, HttpServletRequest httpRequest, List<ResponseType> responseTypes, RedirectUriResponse redirectUriResponse, JwtAuthorizationRequest jwtRequest) {
        try {
            jwtRequest.validate();

            authorizeRestWebServiceValidator.validateRequestObject(jwtRequest, redirectUriResponse);

            // MUST be equal
            if (!jwtRequest.getResponseTypes().containsAll(responseTypes) || !responseTypes.containsAll(jwtRequest.getResponseTypes())) {
                throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "The responseType parameter is not the same in the JWT");
            }
            if (StringUtils.isBlank(jwtRequest.getClientId()) || !jwtRequest.getClientId().equals(clientId)) {
                throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "The clientId parameter is not the same in the JWT");
            }
        } catch (WebApplicationException | InvalidRedirectUrlException e) {
            throw e;
        } catch (InvalidJwtException e) {
            log.debug("Invalid JWT authorization request. {}", e.getMessage());
            redirectUriResponse.getRedirectUri().parseQueryString(errorResponseFactory.getErrorAsQueryString(
                    AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, state));
            throw new WebApplicationException(RedirectUtil.getRedirectResponseBuilder(redirectUriResponse.getRedirectUri(), httpRequest).build());
        } catch (Exception e) {
            log.error("Unexpected exception. " + e.getMessage(), e);
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

    private Response redirectToAuthorizationPage(AuthzRequest authzRequest, RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
                                                 String redirectUri, String state, ResponseMode responseMode,
                                                 List<Prompt> prompts, Integer maxAge, List<String> uiLocales,
                                                 List<String> acrValues, List<String> amrValues,
                                                 Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog) {
        return redirectTo("/authorize", authzRequest, redirectUriResponse, responseTypes, scope, clientId, redirectUri,
                state, responseMode, prompts, maxAge, uiLocales, acrValues, amrValues,
                customParameters, oAuth2AuditLog);
    }

    private Response redirectToSelectAccountPage(AuthzRequest authzRequest, RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
                                                 String redirectUri, String state, ResponseMode responseMode,
                                                 List<Prompt> prompts, Integer maxAge, List<String> uiLocales,
                                                 List<String> acrValues, List<String> amrValues, Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog) {
        return redirectTo("/selectAccount", authzRequest, redirectUriResponse, responseTypes, scope, clientId, redirectUri,
                state, responseMode, prompts, maxAge, uiLocales, acrValues, amrValues,
                customParameters, oAuth2AuditLog);
    }

    private Response redirectTo(String pathToRedirect, AuthzRequest authzRequest,
                                RedirectUri redirectUriResponse, List<ResponseType> responseTypes, String scope, String clientId,
                                String redirectUri, String state, ResponseMode responseMode,
                                List<Prompt> prompts, Integer maxAge, List<String> uiLocales,
                                List<String> acrValues, List<String> amrValues,
                                Map<String, String> customParameters, OAuth2AuditLog oAuth2AuditLog) {

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
        if (maxAge != null) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.MAX_AGE, maxAge.toString());
        }
        String uiLocalesStr = implode(uiLocales, " ");
        if (StringUtils.isNotBlank(uiLocalesStr)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.UI_LOCALES, uiLocalesStr);
        }
        if (StringUtils.isNotBlank(authzRequest.getIdTokenHint())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ID_TOKEN_HINT, authzRequest.getIdTokenHint());
        }
        if (StringUtils.isNotBlank(authzRequest.getLoginHint())) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.LOGIN_HINT, authzRequest.getLoginHint());
        }
        String acrValuesStr = implode(acrValues, " ");
        if (StringUtils.isNotBlank(acrValuesStr)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.ACR_VALUES, acrValuesStr);
        }
        String amrValuesStr = implode(amrValues, " ");
        if (StringUtils.isNotBlank(amrValuesStr)) {
            redirectUriResponse.addResponseParameter(AuthorizeRequestParam.AMR_VALUES, amrValuesStr);
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
