/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;
import org.xdi.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.xdi.oxauth.model.common.ResponseMode;
import org.xdi.oxauth.model.common.TokenType;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.xdi.oxauth.model.authorize.AuthorizeResponseParam.*;

/**
 * Represents an authorization response received from the authorization server.
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class AuthorizationResponse extends BaseResponse {

    private String code;
    private String accessToken;
    private TokenType tokenType;
    private Integer expiresIn;
    private String scope;
    private String idToken;
    private String state;
    private String sessionId;
    private Map<String, String> customParams;
    private ResponseMode responseMode;

    private AuthorizeErrorResponseType errorType;
    private String errorDescription;
    private String errorUri;

    /**
     * Constructs an authorization response.
     */
    public AuthorizationResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);
        customParams = new HashMap<String, String>();

        if (StringUtils.isNotBlank(entity)) {
            try {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has("error")) {
                    errorType = AuthorizeErrorResponseType.fromString(jsonObj.getString("error"));
                }
                if (jsonObj.has("error_description")) {
                    errorDescription = jsonObj.getString("error_description");
                }
                if (jsonObj.has("error_uri")) {
                    errorUri = jsonObj.getString("error_uri");
                }
                if (jsonObj.has("state")) {
                    state = jsonObj.getString("state");
                }
                if (jsonObj.has("redirect")) {
                    location = jsonObj.getString("redirect");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        processLocation();
    }

    public AuthorizationResponse(String location) {
        this.location = location;
        customParams = new HashMap<String, String>();

        processLocation();
    }

    private void processLocation() {
        try {
            if (StringUtils.isNotBlank(location)) {
                Map<String, String> params = null;
                int fragmentIndex = location.indexOf("#");
                if (fragmentIndex != -1) {
                    responseMode = ResponseMode.FRAGMENT;
                    String fragment = location.substring(fragmentIndex + 1);
                    params = QueryStringDecoder.decode(fragment);
                } else {
                    int queryStringIndex = location.indexOf("?");
                    if (queryStringIndex != -1) {
                        responseMode = ResponseMode.QUERY;
                        String queryString = location.substring(queryStringIndex + 1);
                        params = QueryStringDecoder.decode(queryString);
                    }
                }

                if (params != null) {
                    if (params.containsKey(CODE)) {
                        code = params.get(CODE);
                        params.remove(CODE);
                    }
                    if (params.containsKey(SESSION_ID)) {
                        sessionId = params.get(SESSION_ID);
                        params.remove(SESSION_ID);
                    }
                    if (params.containsKey(ACCESS_TOKEN)) {
                        accessToken = params.get(ACCESS_TOKEN);
                        params.remove(ACCESS_TOKEN);
                    }
                    if (params.containsKey(TOKEN_TYPE)) {
                        tokenType = TokenType.fromString(params.get(TOKEN_TYPE));
                        params.remove(TOKEN_TYPE);
                    }
                    if (params.containsKey(EXPIRES_IN)) {
                        expiresIn = Integer.parseInt(params.get(EXPIRES_IN));
                        params.remove(EXPIRES_IN);
                    }
                    if (params.containsKey(SCOPE)) {
                        scope = URLDecoder.decode(params.get(SCOPE), Util.UTF8_STRING_ENCODING);
                        params.remove(SCOPE);
                    }
                    if (params.containsKey(ID_TOKEN)) {
                        idToken = params.get(ID_TOKEN);
                        params.remove(ID_TOKEN);
                    }
                    if (params.containsKey(STATE)) {
                        state = params.get(STATE);
                        params.remove(STATE);
                    }
                    if (params.containsKey("error")) {
                        errorType = AuthorizeErrorResponseType.fromString(params.get("error"));
                        params.remove("error");
                    }
                    if (params.containsKey("error_description")) {
                        errorDescription = URLDecoder.decode(params.get("error_description"), Util.UTF8_STRING_ENCODING);
                        params.remove("error_description");
                    }
                    if (params.containsKey("error_uri")) {
                        errorUri = URLDecoder.decode(params.get("error_uri"), Util.UTF8_STRING_ENCODING);
                        params.remove("error_uri");
                    }

                    for (Iterator<String> it = params.keySet().iterator(); it.hasNext(); ) {
                        String key = it.next();
                        getCustomParams().put(key, params.get(key));
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
        }
    }

    /**
     * Returns the authorization code generated by the authorization server.
     *
     * @return The authorization code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the authorization code generated by the authorization server.
     *
     * @param code The authorization code.
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Returns the access token issued by the authorization server.
     *
     * @return The access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token issued by the authorization server.
     *
     * @param accessToken The access token.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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
     * @param p_sessionId session id.
     */
    public void setSessionId(String p_sessionId) {
        sessionId = p_sessionId;
    }

    public Map<String, String> getCustomParams() {
        return customParams;
    }

    public void setCustomParams(Map<String, String> customParams) {
        this.customParams = customParams;
    }

    public ResponseMode getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(ResponseMode responseMode) {
        this.responseMode = responseMode;
    }

    /**
     * Returns the type of the token issued (value is case insensitive).
     *
     * @return The type of the token.
     */
    public TokenType getTokenType() {
        return tokenType;
    }

    /**
     * Sets the type of the token issued (value is case insensitive).
     *
     * @param tokenType The type of the token.
     */
    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Returns the lifetime in seconds of the access token. For example, the
     * value 3600 denotes that the access token will expire in one hour from the
     * time the response was generated.
     *
     * @return The lifetime in seconds of the access token.
     */
    public Integer getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the lifetime in seconds of the access token. For example, the value
     * 3600 denotes that the access token will expire in one hour from the time
     * the response was generated.
     *
     * @param expiresIn The lifetime in seconds of the access token.
     */
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Returns the scope of the access token.
     *
     * @return The scope of the access token.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope of the access token.
     *
     * @param scope The scope of the access token.
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Returns the ID Token of the for the authentication session.
     *
     * @return The ID Token.
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Sets the ID Token of the for the authentication session.
     *
     * @param idToken The ID Token.
     */
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    /**
     * Returns the state. If the state parameter was present in the client
     * authorization request, the exact value received from the client.
     *
     * @return The state.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state. If the state parameter was present in the client
     * authorization request, the exact value received from the client.
     *
     * @param state The state.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Returns the error code when the request fails, otherwise will return
     * <code>null</code>.
     *
     * @return The error code when the request fails.
     */
    public AuthorizeErrorResponseType getErrorType() {
        return errorType;
    }

    /**
     * Sets the error code when the request fails, otherwise will return
     * <code>null</code>.
     *
     * @param errorType The error code when the request fails.
     */
    public void setErrorType(AuthorizeErrorResponseType errorType) {
        this.errorType = errorType;
    }

    /**
     * Returns a human-readable UTF-8 encoded text providing additional
     * information, used to assist the client developer in understanding the
     * error that occurred.
     *
     * @return The error description.
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Sets a human-readable UTF-8 encoded text providing additional
     * information, used to assist the client developer in understanding the
     * error that occurred.
     *
     * @param errorDescription The error description.
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    /**
     * Returns a URI identifying a human-readable web page with information
     * about the error, used to provide the client developer with additional
     * information about the error.
     *
     * @return A URI with information about the error.
     */
    public String getErrorUri() {
        return errorUri;
    }

    /**
     * Sets a URI identifying a human-readable web page with information about
     * the error, used to provide the client developer with additional
     * information about the error.
     *
     * @param errorUri A URI with information about the error.
     */
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }
}