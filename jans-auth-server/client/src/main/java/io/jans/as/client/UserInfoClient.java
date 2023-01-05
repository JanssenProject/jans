/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.JwtUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
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
        } else {
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
        initClient();

        Builder clientRequest = prepareAuthorizatedClientRequest(getRequest().getAuthorizationMethod(), getRequest().getAccessToken());

        // Call REST Service and handle response
        try {
            if (getRequest().getAuthorizationMethod() == null
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
                clientResponse = clientRequest.buildGet().invoke();
            } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
                clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();
            }

            setResponse(new UserInfoResponse(clientResponse));

            getResponse().setHeaders(clientResponse.getMetadata());
            parseEntity(getResponse().getEntity());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    private void parseEntity(String entity) throws Exception {
        if (StringUtils.isBlank(entity)) {
            return;
        }

        List<Object> contentType = clientResponse.getHeaders().get("Content-Type");
        if (contentType != null && contentType.contains("application/jwt")) {
            String[] jwtParts = entity.split("\\.");
            if (jwtParts.length == 5) {
                byte[] sharedSymmetricKey = sharedKey != null ? sharedKey.getBytes(StandardCharsets.UTF_8) : null;
                Jwe jwe = Jwe.parse(entity, privateKey, sharedSymmetricKey);
                getResponse().getClaimMap().putAll(jwe.getClaims().toMap());
            } else {
                parseJwt(entity);
            }
        } else {
            parseJson(entity);
        }
    }

    private void parseJwt(String entity) throws Exception {
        Jwt jwt = Jwt.parseSilently(entity);
        if (jwt == null) {
            return;
        }

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean signatureVerified = cryptoProvider.verifySignature(
                jwt.getSigningInput(),
                jwt.getEncodedSignature(),
                jwt.getHeader().getKeyId(),
                JwtUtil.getJSONWebKeys(jwksUri),
                sharedKey,
                jwt.getHeader().getSignatureAlgorithm());

        if (signatureVerified) {
            getResponse().getClaimMap().putAll(jwt.getClaims().toMap());
        }
    }

    private void parseJson(String entity) {
        try {
            JSONObject jsonObj = new JSONObject(entity);

            for (Iterator<String> iterator = jsonObj.keys(); iterator.hasNext(); ) {
                String key = iterator.next();
                getResponse().getClaimMap().put(key, ClientUtil.extractListByKeyOptString(jsonObj, key));
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        }
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