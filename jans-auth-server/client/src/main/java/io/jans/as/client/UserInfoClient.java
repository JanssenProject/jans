/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.JwtUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import java.security.PrivateKey;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates functionality to make user info request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class UserInfoClient extends BaseClient<UserInfoRequest, UserInfoResponse> {

    private static final Logger LOG = Logger.getLogger(UserInfoClient.class);

    private String jwksUri; // Url of the server, that returns jwks (JSON Web Keys), for example: https://ce.gluu.info/jans-auth/restv1/jwks.

    private PrivateKey privateKey = null;   // Private Asymmetric Key.
    
    private byte[] sharedKey = null;        // Shared Symmetric Key (byte []).
    private String sharedPassword = null;   // Shared Symmetric Password (String).

    // Note: Shared Key and Shared Password should be separated (distinguishable),
    // so, sharedKey is a Byte Array and sharedPassword is a String.

    // Shared Symmetric Key (sharedKey) is used, when are used follow KeyEncryptionAlgorithm values:
        // A128KW
        // A192KW
        // A256KW
        // A128GCMKW
        // A192GCMKW
        // A256GCMKW
        // DIR

    // Shared Symmetric Password (sharedPassword) is used, when are used follow KeyEncryptionAlgorithm values:
        // PBES2_HS256_PLUS_A128KW
        // PBES2_HS384_PLUS_A192KW
        // PBES2_HS512_PLUS_A256KW

    // PrivateKey is used,  when are used follow KeyEncryptionAlgorithm values:
        // RSA1_5
        // RSA_OAEP
        // RSA_OAEP_256
        // ECDH_ES
        // ECDH_ES_PLUS_A128KW
        // ECDH_ES_PLUS_A192KW
        // ECDH_ES_PLUS_A256KW

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
                Jwe jwe = null;
                if (privateKey != null) {
                    jwe = Jwe.parse(entity, privateKey);
                } else if (sharedKey != null) {
                    jwe = Jwe.parse(entity, null, sharedKey, null);
                } else if (sharedPassword != null) {
                    jwe = Jwe.parse(entity, null, null, sharedPassword);
                } else {
                    throw new InvalidJweException("privateKey, sharedKey, sharedPassword: keys aren't defined, jwe object hasn't been encrypted");
                }
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
                (sharedKey != null) ? new String(sharedKey) : null,
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

    /**
     * Sets Shared Key (byte[]) value.
     * 
     * @param sharedKey Shared Key (byte[]) value.
     */
    public void setSharedKey(byte[] sharedKey) {
        this.sharedKey = sharedKey;
    }

    /**
     * Sets Shared Password (String) value.
     * 
     * @param sharedPassword Shared Password (String) value.
     */
    public void setSharedPassword(String sharedPassword) {
        this.sharedPassword = sharedPassword;
    }

    /**
     * Sets Private Key (java.security.PrivateKey) value.
     * 
     * @param privateKey Private Key (java.security.PrivateKey) value.
     */
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