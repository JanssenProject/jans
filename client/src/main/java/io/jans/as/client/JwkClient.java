/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientExecutor;
import org.json.JSONObject;

import io.jans.as.model.crypto.PublicKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.jwk.JSONWebKeySet;

/**
 * Encapsulates functionality to make JWK request calls to an authorization
 * server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version December 26, 2016
 */
public class JwkClient extends BaseClient<JwkRequest, JwkResponse> {

    /**
     * Constructs a JSON Web Key (JWK) client by providing a REST url where the
     * validate token service is located.
     *
     * @param url The REST Service location.
     */
    public JwkClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }

    /**
     * Executes the call to the REST Service requesting the JWK and processes
     * the response.
     *
     * @return The service response.
     */
    public JwkResponse exec() {
        if (getRequest() == null) {
            setRequest(new JwkRequest());
        }

        // Prepare request parameters
        initClientRequest();
        if (getRequest().hasCredentials()) {
            String encodedCredentials = getRequest().getEncodedCredentials();
            clientRequest.header("Authorization", "Basic " + encodedCredentials);
        }
        clientRequest.accept(MediaType.APPLICATION_JSON);
        clientRequest.setHttpMethod(getHttpMethod());

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.get(String.class);
            int status = clientResponse.getStatus();

            setResponse(new JwkResponse(status));
            getResponse().setHeaders(clientResponse.getMetadata());

            String entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            if (StringUtils.isNotBlank(entity)) {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(JSON_WEB_KEY_SET)) {
                    JSONWebKeySet jwks = JSONWebKeySet.fromJSONObject(jsonObj);
                    getResponse().setJwks(jwks);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    public static RSAPublicKey getRSAPublicKey(String jwkSetUri, String keyId) {
        return getRSAPublicKey(jwkSetUri, keyId, null);
    }

    public static RSAPublicKey getRSAPublicKey(String jwkSetUri, String keyId, ClientExecutor clientExecutor) {
        RSAPublicKey publicKey = null;

        JwkClient jwkClient = new JwkClient(jwkSetUri);
        jwkClient.setExecutor(clientExecutor);
        JwkResponse jwkResponse = jwkClient.exec();
        if (jwkResponse != null && jwkResponse.getStatus() == 200) {
            PublicKey pk = jwkResponse.getPublicKey(keyId);
            if (pk instanceof RSAPublicKey) {
                publicKey = (RSAPublicKey) pk;
            }
        }

        return publicKey;
    }

    public static ECDSAPublicKey getECDSAPublicKey(String jwkSetUrl, String keyId) {
        return getECDSAPublicKey(jwkSetUrl, keyId, null);
    }

    public static ECDSAPublicKey getECDSAPublicKey(String jwkSetUrl, String keyId, ClientExecutor clientExecutor) {
        ECDSAPublicKey publicKey = null;

        JwkClient jwkClient = new JwkClient(jwkSetUrl);
        if (clientExecutor != null) {
            jwkClient.setExecutor(clientExecutor);
        }
        JwkResponse jwkResponse = jwkClient.exec();
        if (jwkResponse != null && jwkResponse.getStatus() == 200) {
            PublicKey pk = jwkResponse.getPublicKey(keyId);
            if (pk instanceof ECDSAPublicKey) {
                publicKey = (ECDSAPublicKey) pk;
            }
        }

        return publicKey;
    }
}