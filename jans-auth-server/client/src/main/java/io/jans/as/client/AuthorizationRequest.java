/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.authorize.CodeVerifier;
import io.jans.as.model.common.Display;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.jans.as.model.util.StringUtils.addQueryStringParam;
import static io.jans.as.model.util.Util.putNotBlank;

/**
 * Represents an authorization request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version April 25, 2022
 */
public class AuthorizationRequest extends BaseRequest {

    private static final Logger LOG = Logger.getLogger(AuthorizationRequest.class);

    public static final String NO_REDIRECT_HEADER = "X-Gluu-NoRedirect";

    private List<ResponseType> responseTypes;
    private String clientId;
    private List<String> scopes;
    private String redirectUri;
    private String state;

    private ResponseMode responseMode;
    private String nonce;
    private Display display;
    private List<Prompt> prompts;
    private Integer maxAge;
    private List<String> uiLocales;
    private List<String> claimsLocales;
    private String idTokenHint;
    private String loginHint;
    private List<String> acrValues;
    private JSONObject claims;
    private String registration;
    private String request;
    private String requestUri;

    private boolean requestSessionId;
    private String sessionId;

    private String accessToken;
    private boolean useNoRedirectHeader;

    // code verifier according to PKCE spec
    private String codeChallenge;
    private String codeChallengeMethod;

    private Map<String, String> customResponseHeaders;

    /**
     * Constructs an authorization request.
     *
     * @param responseTypes The response type informs the authorization server of the desired response type:
     *                      <strong>code</strong>, <strong>token</strong>, <strong>id_token</strong>
     *                      a combination of them. The response type parameter is mandatory.
     * @param clientId      The client identifier is mandatory.
     * @param scopes        The scope of the access request.
     * @param redirectUri   Redirection URI
     * @param nonce         A string value used to associate a user agent session with an ID Token,
     *                      and to mitigate replay attacks.
     */
    public AuthorizationRequest(List<ResponseType> responseTypes, String clientId, List<String> scopes,
                                String redirectUri, String nonce) {
        super();
        this.responseTypes = responseTypes;
        this.clientId = clientId;
        this.scopes = scopes;
        this.redirectUri = redirectUri;
        this.nonce = nonce;
        prompts = new ArrayList<>();
        useNoRedirectHeader = false;
    }

    public AuthorizationRequest(String requestUri) {
        this.requestUri = requestUri;
    }

    public CodeVerifier generateAndSetCodeChallengeWithMethod() {
        CodeVerifier verifier = new CodeVerifier(CodeVerifier.CodeChallengeMethod.S256);
        codeChallenge = verifier.getCodeChallenge();
        codeChallengeMethod = verifier.getTransformationType().getPkceString();
        return verifier;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    /**
     * Returns the response types.
     *
     * @return The response types.
     */
    public List<ResponseType> getResponseTypes() {
        return responseTypes;
    }

    /**
     * Sets the response types.
     *
     * @param responseTypes The response types.
     */
    public void setResponseTypes(List<ResponseType> responseTypes) {
        this.responseTypes = responseTypes;
    }

    /**
     * Returns the client identifier.
     *
     * @return The client identifier.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client identifier.
     *
     * @param clientId The client identifier.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the scopes of the access request. The authorization endpoint allow
     * the client to specify the scope of the access request using the scope
     * request parameter. In turn, the authorization server uses the scope
     * response parameter to inform the client of the scope of the access token
     * issued. The value of the scope parameter is expressed as a list of
     * space-delimited, case sensitive strings.
     *
     * @return The scopes of the access request.
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Sets the scope of the access request. The authorization endpoint allow
     * the client to specify the scope of the access request using the scope
     * request parameter. In turn, the authorization server uses the scope
     * response parameter to inform the client of the scope of the access token
     * issued. The value of the scope parameter is expressed as a list of
     * space-delimited, case sensitive strings.
     *
     * @param scopes The scope of the access request.
     */
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    /**
     * Returns the redirection URI.
     *
     * @return The redirection URI.
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the redirection URI.
     *
     * @param redirectUri The redirection URI.
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Returns the state. The state is an opaque value used by the client to
     * maintain state between the request and callback. The authorization server
     * includes this value when redirecting the user-agent back to the client.
     * The parameter should be used for preventing cross-site request forgery.
     *
     * @return The state.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state. The state is an opaque value used by the client to
     * maintain state between the request and callback. The authorization server
     * includes this value when redirecting the user-agent back to the client.
     * The parameter should be used for preventing cross-site request forgery.
     *
     * @param state The state.
     */
    public void setState(String state) {
        this.state = state;
    }

    public ResponseMode getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(ResponseMode responseMode) {
        this.responseMode = responseMode;
    }

    /**
     * Returns a string value used to associate a user agent session with an ID Token,
     * and to mitigate replay attacks.
     *
     * @return The nonce value.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Sets a string value used to associate a user agent session with an ID Token,
     * and to mitigate replay attacks.
     *
     * @param nonce The nonce value.
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Returns an ASCII string value that specifies how the Authorization Server displays the
     * authentication page to the End-User.
     *
     * @return The display value.
     */
    public Display getDisplay() {
        return display;
    }

    /**
     * Sets an ASCII string value that specifies how the Authorization Server displays the
     * authentication page to the End-User.
     *
     * @param display The display value.
     */
    public void setDisplay(Display display) {
        this.display = display;
    }

    /**
     * Returns a space delimited list of ASCII strings that can contain the values login, consent,
     * select_account, and none.
     *
     * @return The prompt list.
     */
    public List<Prompt> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<Prompt> prompts) {
        this.prompts = prompts;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public List<String> getUiLocales() {
        return uiLocales;
    }

    public void setUiLocales(List<String> uiLocales) {
        this.uiLocales = uiLocales;
    }

    public List<String> getClaimsLocales() {
        return claimsLocales;
    }

    public void setClaimsLocales(List<String> claimsLocales) {
        this.claimsLocales = claimsLocales;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public JSONObject getClaims() {
        return claims;
    }

    public void setClaims(JSONObject claims) {
        this.claims = claims;
    }

    public String getRegistration() {
        return registration;
    }

    public void setRegistration(String registration) {
        this.registration = registration;
    }

    /**
     * Returns a JWT  encoded OpenID Request Object.
     *
     * @return A JWT  encoded OpenID Request Object.
     */
    public String getRequest() {
        return request;
    }

    /**
     * Sets a JWT  encoded OpenID Request Object.
     *
     * @param request A JWT  encoded OpenID Request Object.
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Returns an URL that points to an OpenID Request Object.
     *
     * @return An URL that points to an OpenID Request Object.
     */
    public String getRequestUri() {
        return requestUri;
    }

    /**
     * Sets an URL that points to an OpenID Request Object.
     *
     * @param requestUri An URL that points to an OpenID Request Object.
     */
    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    /**
     * Returns whether session id is requested.
     *
     * @return whether session id is requested
     */
    public boolean isRequestSessionId() {
        return requestSessionId;
    }

    /**
     * Sets whether session id should be requested.
     *
     * @param requestSessionId session id.
     */
    public void setRequestSessionId(boolean requestSessionId) {
        this.requestSessionId = requestSessionId;
    }

    /**
     * Gets session id.
     *
     * @return session id.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets session id.
     *
     * @param sessionId session id
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isUseNoRedirectHeader() {
        return useNoRedirectHeader;
    }

    public void setUseNoRedirectHeader(boolean useNoRedirectHeader) {
        this.useNoRedirectHeader = useNoRedirectHeader;
    }

    public String getResponseTypesAsString() {
        return Util.asString(responseTypes);
    }

    public String getScopesAsString() {
        return Util.listAsString(scopes);
    }

    public String getPromptsAsString() {
        return Util.asString(prompts);
    }

    public String getUiLocalesAsString() {
        return Util.listAsString(uiLocales);
    }

    public String getClaimsLocalesAsString() {
        return Util.listAsString(claimsLocales);
    }

    public String getAcrValuesAsString() {
        return Util.listAsString(acrValues);
    }

    public String getCustomResponseHeadersAsString() throws JSONException {
        String header = Util.mapAsString(customResponseHeaders);
        if (header == null) {
            return null;
        }

        try {
            return URLEncoder.encode(header, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.warn("Failed to encode string", e);
        }

        return null;
    }

    public Map<String, String> getCustomResponseHeaders() {
        return customResponseHeaders;
    }

    public void setCustomResponseHeaders(Map<String, String> customResponseHeaders) {
        this.customResponseHeaders = customResponseHeaders;
    }

    public String getClaimsAsString() {
        if (claims != null) {
            return claims.toString();
        } else {
            return null;
        }
    }

    public String getParQueryString() {
        StringBuilder builder = new StringBuilder();

        try {
            if (StringUtils.isNotBlank(state)) {
                builder.append("&").append(AuthorizeRequestParam.STATE)
                        .append("=").append(URLEncoder.encode(state, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(nonce)) {
                builder.append("&").append(AuthorizeRequestParam.NONCE)
                        .append("=").append(URLEncoder.encode(nonce, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(requestUri)) {
                builder.append("&").append(AuthorizeRequestParam.REQUEST_URI)
                        .append("=").append(URLEncoder.encode(requestUri, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(clientId)) {
                builder.append("&").append(AuthorizeRequestParam.CLIENT_ID)
                        .append("=").append(URLEncoder.encode(clientId, Util.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
        }
        return builder.toString();
    }

    /**
     * Returns a query string with the parameters of the authorization request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        if (Util.isPar(requestUri)) {
            return getParQueryString();
        }

        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            // OAuth 2.0 request parameters
            final String responseTypesAsString = getResponseTypesAsString();
            final String scopesAsString = getScopesAsString();
            final String promptsAsString = getPromptsAsString();
            final String customResponseHeadersAsString = getCustomResponseHeadersAsString();

            // OpenID Connect request parameters
            final String uiLocalesAsString = getUiLocalesAsString();
            final String claimLocalesAsString = getClaimsLocalesAsString();
            final String acrValuesAsString = getAcrValuesAsString();
            final String claimsAsString = getClaimsAsString();

            if (StringUtils.isNotBlank(responseTypesAsString)) {
                queryStringBuilder.append(AuthorizeRequestParam.RESPONSE_TYPE)
                        .append("=").append(URLEncoder.encode(responseTypesAsString, Util.UTF8_STRING_ENCODING));
            }

            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.CLIENT_ID, clientId);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.SCOPE, scopesAsString);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.REDIRECT_URI, redirectUri);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.STATE, state);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.RESPONSE_MODE, responseMode != null ? responseMode.toString() : null);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.NONCE, nonce);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.DISPLAY, display != null ? display.toString() : null);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.PROMPT, promptsAsString);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.MAX_AGE, maxAge);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.UI_LOCALES, uiLocalesAsString);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.CLAIMS_LOCALES, claimLocalesAsString);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.ID_TOKEN_HINT, idTokenHint);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.LOGIN_HINT, loginHint);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.ACR_VALUES, acrValuesAsString);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.CLAIMS, claimsAsString);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.REGISTRATION, registration);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.REQUEST, request);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.REQUEST_URI, requestUri);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.REQUEST_SESSION_ID, requestSessionId);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.SESSION_ID, sessionId);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.ACCESS_TOKEN, accessToken);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.CODE_CHALLENGE, codeChallenge);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.CODE_CHALLENGE_METHOD, codeChallengeMethod);
            addQueryStringParam(queryStringBuilder, AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS, customResponseHeadersAsString);

            for (String key : getCustomParameters().keySet()) {
                queryStringBuilder.append("&");
                queryStringBuilder.append(key).append("=").append(getCustomParameters().get(key));
            }
        } catch (UnsupportedEncodingException | JSONException e) {
            LOG.error(e.getMessage(), e);
        }

        return queryStringBuilder.toString();
    }

    /**
     * Returns a collection of parameters of the authorization request. Any
     * <code>null</code> or empty parameter will be omitted.
     *
     * @return A collection of parameters.
     */
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();

        try {
            // OAuth 2.0 request parameters
            final String responseTypesAsString = getResponseTypesAsString();
            final String scopesAsString = getScopesAsString();
            final String promptsAsString = getPromptsAsString();
            final String customResponseHeadersAsString = getCustomResponseHeadersAsString();

            // OpenID Connect request parameters
            final String uiLocalesAsString = getUiLocalesAsString();
            final String claimLocalesAsString = getClaimsLocalesAsString();
            final String acrValuesAsString = getAcrValuesAsString();
            final String claimsAsString = getClaimsAsString();

            putNotBlank(parameters, AuthorizeRequestParam.RESPONSE_TYPE, responseTypesAsString);
            putNotBlank(parameters, AuthorizeRequestParam.CLIENT_ID, clientId);
            putNotBlank(parameters, AuthorizeRequestParam.SCOPE, scopesAsString);
            putNotBlank(parameters, AuthorizeRequestParam.REDIRECT_URI, redirectUri);
            putNotBlank(parameters, AuthorizeRequestParam.STATE, state);
            putNotBlank(parameters, AuthorizeRequestParam.RESPONSE_MODE, responseMode != null ? responseMode.toString() : null);
            putNotBlank(parameters, AuthorizeRequestParam.NONCE, nonce);
            putNotBlank(parameters, AuthorizeRequestParam.DISPLAY, display != null ? display.toString() : null);
            putNotBlank(parameters, AuthorizeRequestParam.PROMPT, promptsAsString);
            putNotBlank(parameters, AuthorizeRequestParam.MAX_AGE, maxAge != null ? maxAge.toString() : null);
            putNotBlank(parameters, AuthorizeRequestParam.UI_LOCALES, uiLocalesAsString);
            putNotBlank(parameters, AuthorizeRequestParam.CLAIMS_LOCALES, claimLocalesAsString);
            putNotBlank(parameters, AuthorizeRequestParam.ID_TOKEN_HINT, idTokenHint);
            putNotBlank(parameters, AuthorizeRequestParam.LOGIN_HINT, loginHint);
            putNotBlank(parameters, AuthorizeRequestParam.ACR_VALUES, acrValuesAsString);
            putNotBlank(parameters, AuthorizeRequestParam.CLAIMS, claimsAsString);
            putNotBlank(parameters, AuthorizeRequestParam.REGISTRATION, registration);
            putNotBlank(parameters, AuthorizeRequestParam.REQUEST, request);
            putNotBlank(parameters, AuthorizeRequestParam.REQUEST_URI, requestUri);
            putNotBlank(parameters, AuthorizeRequestParam.REQUEST_SESSION_ID, requestSessionId ? Boolean.toString(requestSessionId) : null);
            putNotBlank(parameters, AuthorizeRequestParam.SESSION_ID, sessionId);
            putNotBlank(parameters, AuthorizeRequestParam.ACCESS_TOKEN, accessToken);
            putNotBlank(parameters, AuthorizeRequestParam.CODE_CHALLENGE, codeChallenge);
            putNotBlank(parameters, AuthorizeRequestParam.CODE_CHALLENGE_METHOD, codeChallengeMethod);
            putNotBlank(parameters, AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS, customResponseHeadersAsString);

            for (String key : getCustomParameters().keySet()) {
                parameters.put(key, getCustomParameters().get(key));
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        }

        return parameters;
    }
}