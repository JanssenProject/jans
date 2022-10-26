/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.common.Display;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates functionality to make authorization request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version November 12, 2021
 */
public class AuthorizeClient extends BaseClient<AuthorizationRequest, AuthorizationResponse> {

    private static final Logger LOG = Logger.getLogger(AuthorizeClient.class);

    /**
     * Constructs an authorize client by providing a REST url where the
     * authorize service is located.
     *
     * @param url The REST Service location.
     */
    public AuthorizeClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        if (request.getAuthorizationMethod() == null
                || request.getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                || request.getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
            return HttpMethod.POST;
        } else { // AuthorizationMethod.URL_QUERY_PARAMETER
            return HttpMethod.GET;
        }
    }

    /**
     * The authorization code grant type is used to obtain both access tokens
     * and refresh tokens and is optimized for confidential clients. As a
     * redirection-based flow, the client must be capable of interacting with
     * the resource owner's user-agent (typically a web browser) and capable of
     * receiving incoming requests (via redirection) from the authorization
     * server.
     *
     * @param clientId    The client identifier. This parameter is required.
     * @param scopes      The scope of the access request. This parameter is optional.
     * @param redirectUri The redirection URI. This parameter is optional.
     * @param nonce       A string value used to associate a user agent session with an ID Token,
     *                    and to mitigate replay attacks.
     *                    forgery. This parameter is recommended.
     * @param state       An opaque value used by the client to maintain state between
     *                    the request and callback. The authorization server includes
     *                    this value when redirecting the user-agent back to the client.
     *                    The parameter should be used for preventing cross-site request
     *                    forgery.
     * @param req         A JWT  encoded OpenID Request Object.
     * @param reqUri      An URL that points to an OpenID Request Object.
     * @param display     An ASCII string value that specifies how the Authorization Server displays the
     *                    authentication page to the End-User.
     * @param prompt      A space delimited list of ASCII strings that can contain the values login, consent,
     *                    select_account, and none.
     * @return The authorization response.
     */
    public AuthorizationResponse execAuthorizationCodeGrant(
            String clientId, List<String> scopes, String redirectUri, String nonce,
            String state, String req, String reqUri, Display display, List<Prompt> prompt) {
        List<ResponseType> responseTypes = new ArrayList<>();
        responseTypes.add(ResponseType.CODE);
        setRequest(new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce));
        getRequest().setRedirectUri(redirectUri);
        getRequest().setState(state);
        getRequest().setRequest(req);
        getRequest().setRedirectUri(reqUri);
        getRequest().setDisplay(display);
        getRequest().getPrompts().addAll(prompt);

        return exec();
    }

    /**
     * <p>
     * The implicit grant type is used to obtain access tokens (it does not
     * support the issuance of refresh tokens) and is optimized for public
     * clients known to operate a particular redirection URI. These clients are
     * typically implemented in a browser using a scripting language such as
     * JavaScript.
     * </p>
     * <p>
     * As a redirection-based flow, the client must be capable of interacting
     * with the resource owner's user-agent (typically a web browser) and
     * capable of receiving incoming requests (via redirection) from the
     * authorization server.
     * </p>
     * <p>
     * Unlike the authorization code grant type in which the client makes
     * separate requests for authorization and access token, the client receives
     * the access token as the result of the authorization request.
     * </p>
     * <p>
     * The implicit grant type does not include client authentication, and
     * relies on the presence of the resource owner and the registration of the
     * redirection URI. Because the access token is encoded into the redirection
     * URI, it may be exposed to the resource owner and other applications
     * residing on its device.
     * </p>
     *
     * @param clientId    The client identifier. This parameter is required.
     * @param scopes      The scope of the access request. This parameter is optional.
     * @param redirectUri The redirection URI. This parameter is optional.
     * @param nonce       A string value used to associate a user agent session with an ID Token,
     *                    and to mitigate replay attacks.
     *                    forgery. This parameter is recommended.
     * @param state       An opaque value used by the client to maintain state between
     *                    the request and callback. The authorization server includes
     *                    this value when redirecting the user-agent back to the client.
     *                    The parameter should be used for preventing cross-site request
     *                    forgery.
     * @param req         A JWT  encoded OpenID Request Object.
     * @param reqUri      An URL that points to an OpenID Request Object.
     * @param display     An ASCII string value that specifies how the Authorization Server displays the
     *                    authentication page to the End-User.
     * @param prompt      A space delimited list of ASCII strings that can contain the values login, consent,
     *                    select_account, and none.
     * @return The authorization response.
     */
    @SuppressWarnings("deprecation")
    @Deprecated // it produces confusion since we have parameters and request object at the same time
    public AuthorizationResponse execImplicitGrant(
            String clientId, List<String> scopes, String redirectUri, String nonce,
            String state, String req, String reqUri, Display display, List<Prompt> prompt) {
        List<ResponseType> responseTypes = new ArrayList<>();
        responseTypes.add(ResponseType.TOKEN);
        setRequest(new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce));
        getRequest().setRedirectUri(redirectUri);
        getRequest().setState(state);
        getRequest().setRequest(req);
        getRequest().setRedirectUri(reqUri);
        getRequest().setDisplay(display);
        getRequest().getPrompts().addAll(prompt);

        return exec();
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The authorization response.
     */
    public AuthorizationResponse exec() {
        AuthorizationResponse response = null;

        try {
            initClient();
            response = exec_();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return response;
    }

    /**
     * @deprecated Engine should be shared between clients
     */
    @SuppressWarnings("java:S1133")
    @Deprecated
    public AuthorizationResponse exec(ClientHttpEngine engine) {
        AuthorizationResponse response = null;

        try {
            resteasyClient = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
            webTarget = resteasyClient.target(getUrl());

            response = exec_();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        // Do not close the connection for this case.

        return response;
    }

    private AuthorizationResponse exec_() throws Exception {
        final String responseTypesAsString = getRequest().getResponseTypesAsString();
        final String scopesAsString = getRequest().getScopesAsString();
        final String promptsAsString = getRequest().getPromptsAsString();
        final String uiLocalesAsString = getRequest().getUiLocalesAsString();
        final String claimLocalesAsString = getRequest().getClaimsLocalesAsString();
        final String acrValuesAsString = getRequest().getAcrValuesAsString();
        final String claimsAsString = getRequest().getClaimsAsString();

        addReqParam(AuthorizeRequestParam.RESPONSE_TYPE, responseTypesAsString);
        addReqParam(AuthorizeRequestParam.CLIENT_ID, getRequest().getClientId());
        addReqParam(AuthorizeRequestParam.SCOPE, scopesAsString);
        addReqParam(AuthorizeRequestParam.REDIRECT_URI, getRequest().getRedirectUri());
        addReqParam(AuthorizeRequestParam.STATE, getRequest().getState());
        addReqParam(AuthorizeRequestParam.RESPONSE_MODE, getRequest().getResponseMode());
        addReqParam(AuthorizeRequestParam.NONCE, getRequest().getNonce());
        addReqParam(AuthorizeRequestParam.DISPLAY, getRequest().getDisplay());
        addReqParam(AuthorizeRequestParam.PROMPT, promptsAsString);
        if (getRequest().getMaxAge() != null) {
            addReqParam(AuthorizeRequestParam.MAX_AGE, getRequest().getMaxAge().toString());
        }
        addReqParam(AuthorizeRequestParam.UI_LOCALES, uiLocalesAsString);
        addReqParam(AuthorizeRequestParam.CLAIMS_LOCALES, claimLocalesAsString);
        addReqParam(AuthorizeRequestParam.ID_TOKEN_HINT, getRequest().getIdTokenHint());
        addReqParam(AuthorizeRequestParam.LOGIN_HINT, getRequest().getLoginHint());
        addReqParam(AuthorizeRequestParam.ACR_VALUES, acrValuesAsString);
        addReqParam(AuthorizeRequestParam.CLAIMS, claimsAsString);
        addReqParam(AuthorizeRequestParam.REGISTRATION, getRequest().getRegistration());
        addReqParam(AuthorizeRequestParam.REQUEST, getRequest().getRequest());
        addReqParam(AuthorizeRequestParam.REQUEST_URI, getRequest().getRequestUri());
        addReqParam(AuthorizeRequestParam.ACCESS_TOKEN, getRequest().getAccessToken());
        addReqParam(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS, getRequest().getCustomResponseHeadersAsString());

        // PKCE
        addReqParam(AuthorizeRequestParam.CODE_CHALLENGE, getRequest().getCodeChallenge());
        addReqParam(AuthorizeRequestParam.CODE_CHALLENGE_METHOD, getRequest().getCodeChallengeMethod());

        if (getRequest().isRequestSessionId()) {
            addReqParam(AuthorizeRequestParam.REQUEST_SESSION_ID, Boolean.toString(getRequest().isRequestSessionId()));
        }
        addReqParam(AuthorizeRequestParam.SESSION_ID, getRequest().getSessionId());

        // Custom params
        for (String key : request.getCustomParameters().keySet()) {
            addReqParam(key, request.getCustomParameters().get(key));
        }

        Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        // Prepare request parameters
        clientRequest.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        if (getRequest().isUseNoRedirectHeader()) {
            clientRequest.header(AuthorizationRequest.NO_REDIRECT_HEADER, "true");
        }

        if (request.getAuthorizationMethod() != AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER && request.hasCredentials()) {
            clientRequest.header("Authorization", "Basic " + request.getEncodedCredentials());
        }

        // Call REST Service and handle response
        if (request.getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
            clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();
        } else {
            clientResponse = clientRequest.buildGet().invoke();
        }

        setResponse(new AuthorizationResponse(clientResponse));

        return getResponse();
    }
}
