/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.TokenType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.jans.as.model.authorize.AuthorizeResponseParam.*;

/**
 * Represents an authorization response received from the authorization server.
 *
 * @author Javier Rojas Blum
 * @version November 22, 2021
 */
public class AuthorizationResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(AuthorizationResponse.class);

    private String code;
    private String accessToken;
    private TokenType tokenType;
    private Integer expiresIn;
    private String scope;
    private String idToken;
    private String state;
    private String sessionId;
    private String sid;
    private String deviceSecret;
    private Map<String, String> customParams;
    private ResponseMode responseMode;

    // JARM
    protected String response;
    protected String issuer;
    protected String audience;
    protected Integer exp;

    private AuthorizeErrorResponseType errorType;
    private String errorDescription;
    private String errorUri;

    private String sharedKey;
    private PrivateKey privateKey;
    private String jwksUri;

    /**
     * Constructs an authorization response.
     */
    public AuthorizationResponse(Response clientResponse) {
        super(clientResponse);
        customParams = new HashMap<>();

        if (StringUtils.isNotBlank(entity)) {
            try {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(Constants.ERROR)) {
                    errorType = AuthorizeErrorResponseType.fromString(jsonObj.getString(Constants.ERROR));
                }
                if (jsonObj.has(Constants.ERROR_DESCRIPTION)) {
                    errorDescription = jsonObj.getString(Constants.ERROR_DESCRIPTION);
                }
                if (jsonObj.has(Constants.ERROR_URI)) {
                    errorUri = jsonObj.getString(Constants.ERROR_URI);
                }
                if (jsonObj.has(Constants.STATE)) {
                    state = jsonObj.getString(Constants.STATE);
                }
                if (jsonObj.has(Constants.REDIRECT)) {
                    location = jsonObj.getString(Constants.REDIRECT);
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        processLocation();
    }

    public AuthorizationResponse(String location) {
        this.location = location;
        customParams = new HashMap<>();

        processLocation();
    }

    public AuthorizationResponse(String location, String sharedKey, PrivateKey privateKey, String jwksUri) {
        this.location = location;
        this.sharedKey = sharedKey;
        this.privateKey = privateKey;
        this.jwksUri = jwksUri;

        customParams = new HashMap<>();

        processLocation();
    }

    private void processLocation() {
        try {
            if (StringUtils.isNotBlank(location)) {
                Map<String, String> params = null;
                int fragmentIndex = location.indexOf("#");
                if (fragmentIndex != -1) {
                    String fragment = location.substring(fragmentIndex + 1);
                    params = QueryStringDecoder.decode(fragment);

                    if (params.containsKey("response")) {
                        responseMode = ResponseMode.FRAGMENT_JWT;
                    } else {
                        responseMode = ResponseMode.FRAGMENT;
                    }
                } else {
                    int queryStringIndex = location.indexOf("?");
                    if (queryStringIndex != -1) {
                        String queryString = location.substring(queryStringIndex + 1);
                        params = QueryStringDecoder.decode(queryString);

                        if (params.containsKey("response")) {
                            responseMode = ResponseMode.QUERY_JWT;
                        } else {
                            responseMode = ResponseMode.QUERY;
                        }
                    }
                }

                if (params != null) {
                    if (params.containsKey(RESPONSE)) {
                        response = params.get(RESPONSE);
                        params.remove(RESPONSE);

                        String[] jwtParts = response.split("\\.");

                        if (jwtParts.length == 5) {
                            byte[] sharedSymmetricKey = sharedKey != null ? sharedKey.getBytes(StandardCharsets.UTF_8) : null;
                            Jwe jwe = Jwe.parse(response, privateKey, sharedSymmetricKey);

                            if (jwe != null) {
                                for (Map.Entry<String, List<String>> entry : jwe.getClaims().toMap().entrySet()) {
                                    params.put(entry.getKey(), entry.getValue().get(0));
                                }
                            }
                        } else {
                            Jwt jwt = Jwt.parse(response);

                            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
                            boolean signatureVerified = cryptoProvider.verifySignature(
                                    jwt.getSigningInput(),
                                    jwt.getEncodedSignature(),
                                    jwt.getHeader().getKeyId(),
                                    JwtUtil.getJSONWebKeys(jwksUri),
                                    sharedKey,
                                    jwt.getHeader().getSignatureAlgorithm());

                            if (signatureVerified) {
                                for (Map.Entry<String, List<String>> entry : jwt.getClaims().toMap().entrySet()) {
                                    params.put(entry.getKey(), entry.getValue().get(0));
                                }
                            }
                        }
                    }

                    loadParams(params);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("java:S3776")
    private void loadParams(Map<String, String> params) throws UnsupportedEncodingException {
        if (params.containsKey(CODE)) {
            code = params.get(CODE);
            params.remove(CODE);
        }
        if (params.containsKey(SESSION_ID)) {
            sessionId = params.get(SESSION_ID);
            params.remove(SESSION_ID);
        }
        if (params.containsKey(SID)) {
            sid = params.get(SID);
            params.remove(SID);
        }
        if (params.containsKey(DEVICE_SECRET)) {
            deviceSecret = params.get(DEVICE_SECRET);
            params.remove(DEVICE_SECRET);
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

        // JARM
        if (params.containsKey(ISS)) {
            issuer = params.get(ISS);
            params.remove(ISS);
        }
        if (params.containsKey(AUD)) {
            audience = params.get(AUD);
            params.remove(AUD);
        }
        if (params.containsKey(EXP)) {
            exp = Integer.parseInt(params.get(EXP));
        }

        // Error
        if (params.containsKey(Constants.ERROR)) {
            errorType = AuthorizeErrorResponseType.fromString(params.get(Constants.ERROR));
            params.remove(Constants.ERROR);
        }
        if (params.containsKey(Constants.ERROR_DESCRIPTION)) {
            errorDescription = URLDecoder.decode(params.get(Constants.ERROR_DESCRIPTION), Util.UTF8_STRING_ENCODING);
            params.remove(Constants.ERROR_DESCRIPTION);
        }
        if (params.containsKey(Constants.ERROR_URI)) {
            errorUri = URLDecoder.decode(params.get(Constants.ERROR_URI), Util.UTF8_STRING_ENCODING);
            params.remove(Constants.ERROR_URI);
        }

        for (Iterator<String> it = params.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            getCustomParams().put(key, params.get(key));
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

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
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
     * @param sessionId session id.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
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