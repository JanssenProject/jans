/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.authorize.AuthorizeRequestParam;
import org.gluu.oxauth.model.authorize.CodeVerifier;
import org.gluu.oxauth.model.common.Display;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseMode;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an authorization request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class AuthorizationRequest extends BaseRequest {

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
        prompts = new ArrayList<Prompt>();
        useNoRedirectHeader = false;
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
     * @param p_requestSessionId session id.
     */
    public void setRequestSessionId(boolean p_requestSessionId) {
        requestSessionId = p_requestSessionId;
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
     * @param p_sessionId session id
     */
    public void setSessionId(String p_sessionId) {
        sessionId = p_sessionId;
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
        return Util.mapAsString(customResponseHeaders);
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

    /**
     * Returns a query string with the parameters of the authorization request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            // OAuth 2.0 request parameters
            final String responseTypesAsString = getResponseTypesAsString();
            final String scopesAsString = getScopesAsString();
            final String promptsAsString = getPromptsAsString();
            final String customResponseHeadersAsString = getCustomResponseHeadersAsString();

            if (StringUtils.isNotBlank(responseTypesAsString)) {
                queryStringBuilder.append(AuthorizeRequestParam.RESPONSE_TYPE)
                        .append("=").append(URLEncoder.encode(responseTypesAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(clientId)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.CLIENT_ID)
                        .append("=").append(URLEncoder.encode(clientId, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(scopesAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.SCOPE)
                        .append("=").append(URLEncoder.encode(scopesAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(redirectUri)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.REDIRECT_URI)
                        .append("=").append(URLEncoder.encode(redirectUri, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(state)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.STATE)
                        .append("=").append(URLEncoder.encode(state, Util.UTF8_STRING_ENCODING));
            }

            // OpenID Connect request parameters
            final String uiLocalesAsString = getUiLocalesAsString();
            final String claimLocalesAsString = getClaimsLocalesAsString();
            final String acrValuesAsString = getAcrValuesAsString();
            final String claimsAsString = getClaimsAsString();

            if (responseMode != null) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.RESPONSE_MODE)
                        .append("=").append(URLEncoder.encode(responseMode.toString(), Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(nonce)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.NONCE)
                        .append("=").append(URLEncoder.encode(nonce, Util.UTF8_STRING_ENCODING));
            }
            if (display != null) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.DISPLAY)
                        .append("=").append(URLEncoder.encode(display.toString(), Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(promptsAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.PROMPT)
                        .append("=").append(URLEncoder.encode(promptsAsString, Util.UTF8_STRING_ENCODING));
            }
            if (maxAge != null) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.MAX_AGE)
                        .append("=").append(maxAge);
            }
            if (StringUtils.isNotBlank(uiLocalesAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.UI_LOCALES)
                        .append("=").append(URLEncoder.encode(uiLocalesAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(claimLocalesAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.CLAIMS_LOCALES)
                        .append("=").append(URLEncoder.encode(claimLocalesAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(idTokenHint)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.ID_TOKEN_HINT)
                        .append("=").append(idTokenHint);
            }
            if (StringUtils.isNotBlank(loginHint)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.LOGIN_HINT)
                        .append("=").append(loginHint);
            }
            if (StringUtils.isNotBlank(acrValuesAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.ACR_VALUES)
                        .append("=").append(URLEncoder.encode(acrValuesAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(claimsAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.CLAIMS)
                        .append("=").append(URLEncoder.encode(claimsAsString, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(registration)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.REGISTRATION)
                        .append("=").append(registration);
            }
            if (StringUtils.isNotBlank(request)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.REQUEST)
                        .append("=").append(URLEncoder.encode(request, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(requestUri)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.REQUEST_URI)
                        .append("=").append(URLEncoder.encode(requestUri, Util.UTF8_STRING_ENCODING));
            }
            if (requestSessionId) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.REQUEST_SESSION_ID)
                        .append("=").append(URLEncoder.encode(Boolean.toString(requestSessionId), Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(sessionId)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.SESSION_ID)
                        .append("=").append(URLEncoder.encode(sessionId, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(accessToken)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.ACCESS_TOKEN)
                        .append("=").append(URLEncoder.encode(accessToken, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(codeChallenge)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.CODE_CHALLENGE)
                        .append("=").append(codeChallenge);
            }
            if (StringUtils.isNotBlank(codeChallengeMethod)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.CODE_CHALLENGE_METHOD)
                        .append("=").append(codeChallengeMethod);
            }
            if (StringUtils.isNotBlank(customResponseHeadersAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS)
                        .append("=").append(URLEncoder.encode(customResponseHeadersAsString, Util.UTF8_STRING_ENCODING));
            }
            for (String key : getCustomParameters().keySet()) {
                queryStringBuilder.append("&");
                queryStringBuilder.append(key).append("=").append(getCustomParameters().get(key));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }

    /**
     * Returns a collection of parameters of the authorization request. Any
     * <code>null</code> or empty parameter will be omitted.
     *
     * @return A collection of parameters.
     */
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<String, String>();

        try {
            // OAuth 2.0 request parameters
            final String responseTypesAsString = getResponseTypesAsString();
            final String scopesAsString = getScopesAsString();
            final String promptsAsString = getPromptsAsString();
            final String customResponseHeadersAsString = getCustomResponseHeadersAsString();

            if (StringUtils.isNotBlank(responseTypesAsString)) {
                parameters.put(AuthorizeRequestParam.RESPONSE_TYPE, responseTypesAsString);
            }
            if (StringUtils.isNotBlank(clientId)) {
                parameters.put(AuthorizeRequestParam.CLIENT_ID, clientId);
            }
            if (StringUtils.isNotBlank(scopesAsString)) {
                parameters.put(AuthorizeRequestParam.SCOPE, scopesAsString);
            }
            if (StringUtils.isNotBlank(redirectUri)) {
                parameters.put(AuthorizeRequestParam.REDIRECT_URI, redirectUri);
            }
            if (StringUtils.isNotBlank(state)) {
                parameters.put(AuthorizeRequestParam.STATE, state);
            }

            // OpenID Connect request parameters
            final String uiLocalesAsString = getUiLocalesAsString();
            final String claimLocalesAsString = getClaimsLocalesAsString();
            final String acrValuesAsString = getAcrValuesAsString();
            final String claimsAsString = getClaimsAsString();

            if (responseMode != null) {
                parameters.put(AuthorizeRequestParam.RESPONSE_MODE, responseMode.toString());
            }
            if (StringUtils.isNotBlank(nonce)) {
                parameters.put(AuthorizeRequestParam.NONCE, nonce);
            }
            if (display != null) {
                parameters.put(AuthorizeRequestParam.DISPLAY, display.toString());
            }
            if (StringUtils.isNotBlank(promptsAsString)) {
                parameters.put(AuthorizeRequestParam.PROMPT, promptsAsString);
            }
            if (maxAge != null) {
                parameters.put(AuthorizeRequestParam.MAX_AGE, maxAge.toString());
            }
            if (StringUtils.isNotBlank(uiLocalesAsString)) {
                parameters.put(AuthorizeRequestParam.UI_LOCALES, uiLocalesAsString);
            }
            if (StringUtils.isNotBlank(claimLocalesAsString)) {
                parameters.put(AuthorizeRequestParam.CLAIMS_LOCALES, claimLocalesAsString);
            }
            if (StringUtils.isNotBlank(idTokenHint)) {
                parameters.put(AuthorizeRequestParam.ID_TOKEN_HINT, idTokenHint);
            }
            if (StringUtils.isNotBlank(loginHint)) {
                parameters.put(AuthorizeRequestParam.LOGIN_HINT, loginHint);
            }
            if (StringUtils.isNotBlank(acrValuesAsString)) {
                parameters.put(AuthorizeRequestParam.ACR_VALUES, acrValuesAsString);
            }
            if (StringUtils.isNotBlank(claimsAsString)) {
                parameters.put(AuthorizeRequestParam.CLAIMS, claimsAsString);
            }
            if (StringUtils.isNotBlank(registration)) {
                parameters.put(AuthorizeRequestParam.REGISTRATION, registration);
            }
            if (StringUtils.isNotBlank(request)) {
                parameters.put(AuthorizeRequestParam.REQUEST, request);
            }
            if (StringUtils.isNotBlank(requestUri)) {
                parameters.put(AuthorizeRequestParam.REQUEST_URI, requestUri);
            }
            if (requestSessionId) {
                parameters.put(AuthorizeRequestParam.REQUEST_SESSION_ID, Boolean.toString(requestSessionId));
            }
            if (StringUtils.isNotBlank(sessionId)) {
                parameters.put(AuthorizeRequestParam.SESSION_ID, sessionId);
            }
            if (StringUtils.isNotBlank(accessToken)) {
                parameters.put(AuthorizeRequestParam.ACCESS_TOKEN, accessToken);
            }
            if (StringUtils.isNotBlank(codeChallenge)) {
                parameters.put(AuthorizeRequestParam.CODE_CHALLENGE, codeChallenge);
            }
            if (StringUtils.isNotBlank(codeChallengeMethod)) {
                parameters.put(AuthorizeRequestParam.CODE_CHALLENGE_METHOD, codeChallengeMethod);
            }
            if (StringUtils.isNotBlank(customResponseHeadersAsString)) {
                parameters.put(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS, customResponseHeadersAsString);
            }

            for (String key : getCustomParameters().keySet()) {
                parameters.put(key, getCustomParameters().get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return parameters;
    }
}