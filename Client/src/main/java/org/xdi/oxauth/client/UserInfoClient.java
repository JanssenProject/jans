/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.common.AuthorizationMethod;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.userinfo.UserInfoErrorResponseType;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates functionality to make user info request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version December 26, 2016
 */
public class UserInfoClient extends BaseClient<UserInfoRequest, UserInfoResponse> {

    private String sharedKey;
    private PrivateKey privateKey;
    private String jwksUri;

    /**
     * Constructs an User Info client by providing a REST url where the service is located.
     *
     * @param url The REST Service location.
     */
    public UserInfoClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        if (request.getAuthorizationMethod() == null
                || request.getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                || request.getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
            return HttpMethod.GET;
        } else /*if (request.getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER)*/ {
            return HttpMethod.POST;
        }
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @param accessToken The access token obtained from the oxAuth authorization request.
     * @return The service response.
     */
    public UserInfoResponse execUserInfo(String accessToken) {
        setRequest(new UserInfoRequest(accessToken));

        return exec();
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The service response.
     */
    public UserInfoResponse exec() {
        // Prepare request parameters
        initClientRequest();
        clientRequest.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        clientRequest.setHttpMethod(getHttpMethod());

        if (getRequest().getAuthorizationMethod() == null
                || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
                clientRequest.header("Authorization", "Bearer " + getRequest().getAccessToken());
            }
        } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
                clientRequest.formParameter("access_token", getRequest().getAccessToken());
            }
        } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
                clientRequest.queryParameter("access_token", getRequest().getAccessToken());
            }
        }

        // Call REST Service and handle response
        try {
            if (getRequest().getAuthorizationMethod() == null
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
                clientResponse = clientRequest.get(String.class);
            } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
                clientResponse = clientRequest.post(String.class);
            }

            int status = clientResponse.getStatus();

            setResponse(new UserInfoResponse(status));

            String entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            if (StringUtils.isNotBlank(entity)) {
                List<Object> contentType = clientResponse.getHeaders().get("Content-Type");
                if (contentType != null && contentType.contains("application/jwt")) {
                    String[] jwtParts = entity.split("\\.");
                    if (jwtParts.length == 5) {
                        byte[] sharedSymmetricKey = sharedKey != null ? sharedKey.getBytes(Util.UTF8_STRING_ENCODING) : null;
                        Jwe jwe = Jwe.parse(entity, privateKey, sharedSymmetricKey);
                        getResponse().setClaims(jwe.getClaims().toMap());
                    } else {
                        Jwt jwt = Jwt.parse(entity);

                        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();
                        boolean signatureVerified = cryptoProvider.verifySignature(
                                jwt.getSigningInput(),
                                jwt.getEncodedSignature(),
                                jwt.getHeader().getKeyId(),
                                JwtUtil.getJSONWebKeys(jwksUri),
                                sharedKey,
                                jwt.getHeader().getAlgorithm());

                        if (signatureVerified) {
                            getResponse().setClaims(jwt.getClaims().toMap());
                        }
                    }
                } else {
                    try {
                        JSONObject jsonObj = new JSONObject(entity);

                        if (jsonObj.has("error")) {
                            getResponse().setErrorType(UserInfoErrorResponseType.fromString(jsonObj.getString("error")));
                            jsonObj.remove("error");
                        }
                        if (jsonObj.has("error_description")) {
                            getResponse().setErrorDescription(jsonObj.getString("error_description"));
                            jsonObj.remove("error_description");
                        }
                        if (jsonObj.has("error_uri")) {
                            getResponse().setErrorUri(jsonObj.getString("error_uri"));
                            jsonObj.remove("error_uri");
                        }

                        for (Iterator<String> iterator = jsonObj.keys(); iterator.hasNext(); ) {
                            String key = iterator.next();
                            List<String> values = new ArrayList<String>();

                            JSONArray jsonArray = jsonObj.optJSONArray(key);
                            if (jsonArray != null) {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    String value = jsonArray.optString(i);
                                    if (value != null) {
                                        values.add(value);
                                    }
                                }
                            } else {
                                String value = jsonObj.optString(key);
                                if (value != null) {
                                    values.add(value);
                                }
                            }

                            getResponse().getClaims().put(key, values);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }
}