/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.JwtUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
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

    private static final Logger LOG = Logger.getLogger(UserInfoClient.class);

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
     * @param accessToken The access token obtained from the Jans Auth authorization request.
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

            setResponse(new UserInfoResponse(clientResponse));

            String entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            if (StringUtils.isNotBlank(entity)) {
                List<Object> contentType = clientResponse.getHeaders().get("Content-Type");
                if (contentType != null && contentType.contains("application/jwt")) {
                    String[] jwtParts = entity.split("\\.");
                    if (jwtParts.length == 5) {
                        byte[] sharedSymmetricKey = sharedKey != null ? sharedKey.getBytes(StandardCharsets.UTF_8) : null;
                        Jwe jwe = Jwe.parse(entity, privateKey, sharedSymmetricKey);
                        getResponse().setClaims(jwe.getClaims().toMap());
                    } else {
                        Jwt jwt = Jwt.parse(entity);

                        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
                        boolean signatureVerified = cryptoProvider.verifySignature(
                                jwt.getSigningInput(),
                                jwt.getEncodedSignature(),
                                jwt.getHeader().getKeyId(),
                                JwtUtil.getJSONWebKeys(jwksUri),
                                sharedKey,
                                jwt.getHeader().getSignatureAlgorithm());

                        if (signatureVerified) {
                            getResponse().setClaims(jwt.getClaims().toMap());
                        }
                    }
                } else {
                    try {
                        getResponse().injectErrorIfExistSilently(entity);
                        JSONObject jsonObj = new JSONObject(entity);

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
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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