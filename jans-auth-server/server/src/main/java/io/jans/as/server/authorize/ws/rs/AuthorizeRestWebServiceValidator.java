/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.server.model.authorize.AuthorizeParamsValidator;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.exception.AcrChangedException;
import io.jans.as.server.model.exception.InvalidRedirectUrlException;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.session.SessionEvent;
import io.jans.as.server.service.external.session.SessionEventType;
import io.jans.as.server.util.RedirectUtil;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.exception.EntryPersistenceException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static io.jans.as.model.crypto.signature.SignatureAlgorithm.NONE;
import static io.jans.as.model.crypto.signature.SignatureAlgorithm.RS256;
import static io.jans.as.model.util.StringUtils.implode;
import static org.apache.commons.lang.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version December 15, 2021
 */
@Named
@Stateless
public class AuthorizeRestWebServiceValidator {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ClientService clientService;

    @Inject
    private RedirectionUriService redirectionUriService;

    @Inject
    private DeviceAuthorizationService deviceAuthorizationService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private Identity identity;

    public Client validateClient(String clientId, String state) {
        return validateClient(clientId, state, false);
    }

    public Client validateClient(AuthzRequest authzRequest, boolean isPar) {
        final Client client = validateClient(authzRequest.getClientId(), authzRequest.getState(), isPar);
        authzRequest.setClient(client);
        return client;
    }

    public Client validateClient(String clientId, String state, boolean isPar) {
        if (StringUtils.isBlank(clientId)) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, "client_id is empty or blank."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        try {
            final Client client = clientService.getClient(clientId);
            if (client == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, "Unable to find client."))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
            }
            if (client.isDisabled()) {
                throw new WebApplicationException(Response
                        .status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.DISABLED_CLIENT, state, "Client is disabled."))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
            }

            if (!isPar && isTrue(client.getAttributes().getRequirePar())) {
                log.debug("Client can performa only PAR requests.");
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Client can performa only PAR requests."))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
            }

            return client;
        } catch (EntryPersistenceException e) { // Invalid clientId
            throw new WebApplicationException(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, "Unable to find client on AS."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
    }

    public boolean isAuthnMaxAgeValid(Integer maxAge, SessionId sessionUser, Client client) {
        if (maxAge == null) {
            maxAge = client.getDefaultMaxAge();
        }
        if (maxAge == null) { // if not set, it's still valid
            return true;
        }

        if (maxAge == 0) { // issue #2361: allow authentication for max_age=0
            if (BooleanUtils.isTrue(appConfiguration.getDisableAuthnForMaxAgeZero())) {
                return false;
            }
            return true;
        }


        GregorianCalendar userAuthnTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        if (sessionUser.getAuthenticationTime() != null) {
            userAuthnTime.setTime(sessionUser.getAuthenticationTime());
        }

        userAuthnTime.add(Calendar.SECOND, maxAge);
        return userAuthnTime.after(ServerUtil.now());

    }

    public void validateRequestJwt(String request, String requestUri, RedirectUriResponse redirectUriResponse) {
        if (appConfiguration.isFapi() && StringUtils.isBlank(request) && StringUtils.isBlank(requestUri)) {
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST, "request and request_uri are both not specified which is forbidden for FAPI.");
        }
        if (StringUtils.isNotBlank(request) && StringUtils.isNotBlank(requestUri)) {
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST, "Both request and request_uri are specified which is not allowed.");
        }
    }

    public void validate(AuthzRequest authzRequest, List<io.jans.as.model.common.ResponseType> responseTypes, Client client) {
        final ResponseMode responseMode = authzRequest.getResponseModeEnum();
        final String redirectUri = authzRequest.getRedirectUri();
        if (!AuthorizeParamsValidator.validateParams(responseTypes, authzRequest.getPromptList(), authzRequest.getNonce(), appConfiguration.isFapi(), responseMode)) {

            if (redirectUri != null && redirectionUriService.validateRedirectionUri(client, redirectUri) != null) {
                RedirectUri redirectUriResponse = new RedirectUri(redirectUri, responseTypes, responseMode);
                redirectUriResponse.parseQueryString(errorResponseFactory.getErrorAsQueryString(
                        AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState()));
                throw new WebApplicationException(RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, authzRequest.getHttpRequest()).build());
            } else {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST.getStatusCode())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState(), "Invalid redirect uri."))
                        .build());
            }
        }
    }

    @SuppressWarnings("java:S3776")
    public void validateRequestObject(JwtAuthorizationRequest jwtRequest, RedirectUriResponse redirectUriResponse) {
        if (!jwtRequest.getAud().isEmpty() && !jwtRequest.getAud().contains(appConfiguration.getIssuer())) {
            log.error("Failed to match aud to AS, aud: {}", jwtRequest.getAud());
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }

        if (!appConfiguration.isFapi()) {
            return;
        }

        // FAPI related validation
        if (jwtRequest.getNestedJwt() != null) {
            SignatureAlgorithm nestedJwtSigAlg = jwtRequest.getNestedJwt().getHeader().getSignatureAlgorithm();
            if (appConfiguration.isFapi() && (nestedJwtSigAlg == RS256 || nestedJwtSigAlg == NONE)) {
                log.error("The Nested JWT signature algorithm is not valid.");
                throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
            }
        } 
        String redirectUri = jwtRequest.getRedirectUri();
        Client client = clientService.getClient(jwtRequest.getClientId());
        if (redirectUri != null && redirectionUriService.validateRedirectionUri(client, redirectUri) == null) {
            log.error(" unregistered redirect uri");
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT,
                            jwtRequest.getState(), "The request has unregistered request_uri"))
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
        if (jwtRequest.getExp() == null) {
            log.error("The exp claim is not set");
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }
        final long expInMillis = jwtRequest.getExp() * 1000L;
        final long now = new Date().getTime();
        if (expInMillis < now) {
            log.error("Request object expired. Exp: {}, now: {}", expInMillis, now);
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }
        if (jwtRequest.getScopes() == null || jwtRequest.getScopes().isEmpty()) {
            log.error("Request object does not have scope claim.");
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }
        if (StringUtils.isBlank(jwtRequest.getNonce())) {
            log.error("Request object does not have nonce claim.");
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }
        if (StringUtils.isBlank(jwtRequest.getRedirectUri())) {
            log.error("Request object does not have redirect_uri claim.");
            if (redirectUriResponse.getRedirectUri().getBaseRedirectUri() != null) {
                throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
            } else {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT,
                                jwtRequest.getState(), "Request object does not have redirect_uri claim."))
                        .type(MediaType.APPLICATION_JSON_TYPE).build());
            }
        }
    }

    /**
     * Validates expiration, audience and scopes in the JWT request.
     *
     * @param jwtRequest Object to be validated.
     */
    @SuppressWarnings("java:S3776")
    public void validateCibaRequestObject(JwtAuthorizationRequest jwtRequest, String clientId) {
        if (jwtRequest.getAud().isEmpty() || !jwtRequest.getAud().contains(appConfiguration.getIssuer())) {
            log.error("Failed to match aud to AS, aud: {}", jwtRequest.getAud());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }

        if (!appConfiguration.isFapi()) {
            return;
        }

        // FAPI related validation
        if (jwtRequest.getExp() == null) {
            log.error("The exp claim is not set");
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
        final long expInMillis = jwtRequest.getExp() * 1000L;
        final long now = new Date().getTime();
        if (expInMillis < now) {
            log.error("Request object expired. Exp: {}, now: {}", expInMillis, now);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
        if (jwtRequest.getScopes() == null || jwtRequest.getScopes().isEmpty()) {
            log.error("Request object does not have scope claim.");
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
        if (StringUtils.isEmpty(jwtRequest.getIss()) || !jwtRequest.getIss().equals(clientId)) {
            log.error("Request object has a wrong iss claim, iss: {}", jwtRequest.getIss());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
        if (jwtRequest.getIat() == null || jwtRequest.getIat() == 0) {
            log.error("Request object has a wrong iat claim, iat: {}", jwtRequest.getIat());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
        int nowInSeconds = Math.toIntExact(System.currentTimeMillis() / 1000);
        if (jwtRequest.getNbf() == null || jwtRequest.getNbf() > nowInSeconds
                || jwtRequest.getNbf() < nowInSeconds - appConfiguration.getCibaMaxExpirationTimeAllowedSec()) {
            log.error("Request object has a wrong nbf claim, nbf: {}", jwtRequest.getNbf());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
        if (StringUtils.isEmpty(jwtRequest.getJti())) {
            log.error("Request object has a wrong jti claim, jti: {}", jwtRequest.getJti());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
        int result = (StringUtils.isNotBlank(jwtRequest.getLoginHint()) ? 1 : 0)
                + (StringUtils.isNotBlank(jwtRequest.getLoginHintToken()) ? 1 : 0)
                + (StringUtils.isNotBlank(jwtRequest.getIdTokenHint()) ? 1 : 0);
        if (result != 1) {
            log.error("Request object has too many hints or doesnt have any");
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST))
                    .build());
        }
    }

    public String validateRedirectUri(@NotNull Client client, @Nullable String redirectUri, @Nullable String state, @Nullable String deviceAuthzUserCode, @Nullable HttpServletRequest httpRequest) {
        return validateRedirectUri(client, redirectUri, state, deviceAuthzUserCode, httpRequest, AuthorizeErrorResponseType.INVALID_REQUEST_REDIRECT_URI);
    }

    public String validateRedirectUri(@NotNull Client client, @Nullable String redirectUri, @Nullable String state,
                                      @Nullable String deviceAuthzUserCode, @Nullable HttpServletRequest httpRequest, @NotNull AuthorizeErrorResponseType error) {
        if (appConfiguration.isFapi() && StringUtils.isNotBlank(redirectUri) && StringUtils.isBlank(redirectionUriService.validateRedirectionUri(client, redirectUri))) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(error, state, ""))
                    .build());
        }

        if (StringUtils.isNotBlank(deviceAuthzUserCode)) {
            DeviceAuthorizationCacheControl deviceAuthorizationCacheControl = deviceAuthorizationService
                    .getDeviceAuthzByUserCode(deviceAuthzUserCode);
            redirectUri = deviceAuthorizationService.getDeviceAuthorizationPage(deviceAuthorizationCacheControl, client, state, httpRequest);
        } else {
            redirectUri = redirectionUriService.validateRedirectionUri(client, redirectUri);
        }
        if (StringUtils.isNotBlank(redirectUri)) {
            return redirectUri;
        }
        throw new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity(errorResponseFactory.getErrorAsJson(error, state, ""))
                .build());
    }

    public void throwInvalidJwtRequestExceptionAsJwtMode(RedirectUriResponse redirectUriResponse, String reason, String state, HttpServletRequest httpRequest) {
        log.debug(reason); // in FAPI case log reason but don't send it since it's `reason` is not known
        log.debug("Invalid JWT authorization request.");
        redirectUriResponse.getRedirectUri().parseQueryString(errorResponseFactory
                .getErrorAsQueryString(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, state));
        throw new WebApplicationException(
                RedirectUtil.getRedirectResponseBuilder(redirectUriResponse.getRedirectUri(), httpRequest).build());
    }
    
    public WebApplicationException createInvalidJwtRequestException(RedirectUriResponse redirectUriResponse, String reason) {
        if (appConfiguration.isFapi()) {
            log.debug(reason); // in FAPI case log reason but don't send it since it's `reason` is not known.
            return redirectUriResponse.createWebException(io.jans.as.model.authorize.AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }
        return redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT, reason);
    }

    public void validatePkce(String codeChallenge, RedirectUriResponse redirectUriResponse) {
        if (isTrue(appConfiguration.getRequirePkce()) && Strings.isNullOrEmpty(codeChallenge)) {
            log.error("PKCE is required but code_challenge is blank.");
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST);
        }
    }

    public void validateAcrs(AuthzRequest authzRequest, Client client) throws AcrChangedException {
        if (!client.getAttributes().getAuthorizedAcrValues().isEmpty() &&
                !client.getAttributes().getAuthorizedAcrValues().containsAll(authzRequest.getAcrValuesList())) {
            throw authzRequest.getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.INVALID_REQUEST,
                    "Restricted acr value request, please review the list of authorized acr values for this client");
        }
        checkAcrChanged(authzRequest, identity.getSessionId()); // check after redirect uri is validated
    }


    private void checkAcrChanged(AuthzRequest authzRequest, SessionId sessionUser) throws AcrChangedException {
        try {
            sessionIdService.assertAuthenticatedSessionCorrespondsToNewRequest(sessionUser, authzRequest.getAcrValues());
        } catch (AcrChangedException e) { // Acr changed
            //See https://github.com/GluuFederation/oxTrust/issues/797
            if (e.isForceReAuthentication()) {
                final List<Prompt> promptList = Lists.newArrayList(authzRequest.getPromptList());
                if (!promptList.contains(Prompt.LOGIN)) {
                    log.info("ACR is changed, adding prompt=login to prompts");
                    promptList.add(Prompt.LOGIN);
                    authzRequest.setPrompt(implode(promptList, " "));

                    sessionUser.setState(SessionIdState.UNAUTHENTICATED);
                    sessionUser.getSessionAttributes().put("prompt", authzRequest.getPrompt());
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

    public void validateJwtRequest(String clientId, String state, HttpServletRequest httpRequest, List<ResponseType> responseTypes, RedirectUriResponse redirectUriResponse, JwtAuthorizationRequest jwtRequest) {
        try {
            jwtRequest.validate();

            validateRequestObject(jwtRequest, redirectUriResponse);

            // MUST be equal
            if (!jwtRequest.getResponseTypes().containsAll(responseTypes) || !responseTypes.containsAll(jwtRequest.getResponseTypes())) {
                throw createInvalidJwtRequestException(redirectUriResponse, "The responseType parameter is not the same in the JWT");
            }
            if (StringUtils.isBlank(jwtRequest.getClientId()) || !jwtRequest.getClientId().equals(clientId)) {
                throw createInvalidJwtRequestException(redirectUriResponse, "The clientId parameter is not the same in the JWT");
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

    public void checkSignedRequestRequired(AuthzRequest authzRequest) {
        if (Boolean.TRUE.equals(appConfiguration.getForceSignedRequestObject()) && StringUtils.isBlank(authzRequest.getRequest()) && StringUtils.isBlank(authzRequest.getRequestUri())) {
            throw createInvalidJwtRequestException(authzRequest.getRedirectUriResponse(), "A signed request object is required");
        }
    }

    public void validateNotWebView(HttpServletRequest httpRequest) {
        if (appConfiguration.getBlockWebviewAuthorizationEnabled()) {
            String headerRequestedWith = httpRequest.getHeader("X-Requested-With");
            if (headerRequestedWith != null) {
                log.error("Unauthorized, request contains X-Requested-With: {}", headerRequestedWith);
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }
    }
}
