/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import com.google.common.base.Strings;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.authorize.AuthorizeParamsValidator;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.DeviceAuthorizationService;
import io.jans.as.server.service.RedirectUriResponse;
import io.jans.as.server.service.RedirectionUriService;
import io.jans.as.server.util.RedirectUtil;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.exception.EntryPersistenceException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static org.apache.commons.lang.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version November 5, 2021
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

    public Client validateClient(String clientId, String state) {
        return validateClient(clientId, state, false);
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

    public boolean validateAuthnMaxAge(Integer maxAge, SessionId sessionUser, Client client) {
        if (maxAge == null) {
            maxAge = client.getDefaultMaxAge();
        }

        GregorianCalendar userAuthnTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        if (sessionUser.getAuthenticationTime() != null) {
            userAuthnTime.setTime(sessionUser.getAuthenticationTime());
        }
        if (maxAge != null) {
            userAuthnTime.add(Calendar.SECOND, maxAge);
            return userAuthnTime.after(ServerUtil.now());
        }
        return true;
    }

    public void validateRequestJwt(String request, String requestUri, RedirectUriResponse redirectUriResponse) {
        if (appConfiguration.isFapi() && StringUtils.isBlank(request) && StringUtils.isBlank(requestUri)) {
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST, "request and request_uri are both not specified which is forbidden for FAPI.");
        }
        if (StringUtils.isNotBlank(request) && StringUtils.isNotBlank(requestUri)) {
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST, "Both request and request_uri are specified which is not allowed.");
        }
    }

    public void validate(List<io.jans.as.model.common.ResponseType> responseTypes, List<Prompt> prompts, String nonce, String state, String redirectUri, HttpServletRequest httpRequest, Client client, io.jans.as.model.common.ResponseMode responseMode) {
        if (!AuthorizeParamsValidator.validateParams(responseTypes, prompts, nonce, appConfiguration.isFapi(), responseMode)) {
            if (redirectUri != null && redirectionUriService.validateRedirectionUri(client, redirectUri) != null) {
                RedirectUri redirectUriResponse = new RedirectUri(redirectUri, responseTypes, responseMode);
                redirectUriResponse.parseQueryString(errorResponseFactory.getErrorAsQueryString(
                        AuthorizeErrorResponseType.INVALID_REQUEST, state));
                throw new WebApplicationException(RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, httpRequest).build());
            } else {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST.getStatusCode())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Invalid redirect uri."))
                        .build());
            }
        }
    }

    public void validateRequestObject(JwtAuthorizationRequest jwtRequest, RedirectUriResponse redirectUriResponse) {
        if (!jwtRequest.getAud().isEmpty() && !jwtRequest.getAud().contains(appConfiguration.getIssuer())) {
            log.error("Failed to match aud to AS, aud: {}", jwtRequest.getAud());
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }

        if (!appConfiguration.isFapi()) {
            return;
        }

        // FAPI related validation
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
            throw redirectUriResponse.createWebException(AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
        }
    }

    /**
     * Validates expiration, audience and scopes in the JWT request.
     *
     * @param jwtRequest Object to be validated.
     */
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
        if (appConfiguration.isFapi()) {
            return redirectUri; // FAPI validator will check it in the request object.
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
}
