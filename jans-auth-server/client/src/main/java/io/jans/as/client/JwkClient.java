/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.crypto.PublicKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.EDDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;

/**
 * Encapsulates functionality to make JWK request calls to an authorization
 * server via REST Services.
 *
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021 
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

    /**
     * Returns "GET" (HttpMethod.GET).
     */
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
        initClient();

        Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        if (getRequest().hasCredentials()) {
            String encodedCredentials = getRequest().getEncodedCredentials();
            clientRequest.header("Authorization", "Basic " + encodedCredentials);
        }
        clientRequest.accept(MediaType.APPLICATION_JSON);

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.buildGet().invoke();
            int status = clientResponse.getStatus();

            setResponse(new JwkResponse(status));
            getResponse().setHeaders(clientResponse.getMetadata());

            String entity = clientResponse.readEntity(String.class);
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

    /**
     * Returns RSA public key.
     * 
     * @param jwkSetUri Url of the server, that returns jwks (JSON Web Keys), for example: https://ce.gluu.info/jans-auth/restv1/jwks.
     * @param keyId Key Id (kid).
     * @return RSA public key.
     */
    public static RSAPublicKey getRSAPublicKey(String jwkSetUri, String keyId) {
        return getRSAPublicKey(jwkSetUri, keyId, null);
    }

    /**
     * Returns RSA public key.
     * 
     * @param jwkSetUri Url of the server, that returns jwks (JSON Web Keys), for example: https://ce.gluu.info/jans-auth/restv1/jwks.
     * @param keyId Key Id (kid).
     * @param engine ClientHttpEngine (request and response).
     * @return RSA public key.
     */
    public static RSAPublicKey getRSAPublicKey(String jwkSetUri, String keyId, ClientHttpEngine engine) {
        RSAPublicKey publicKey = null;

        JwkClient jwkClient = new JwkClient(jwkSetUri);
        jwkClient.setExecutor(engine);
        JwkResponse jwkResponse = jwkClient.exec();
        if (jwkResponse != null && jwkResponse.getStatus() == 200) {
            PublicKey pk = jwkResponse.getPublicKey(keyId);
            if (pk instanceof RSAPublicKey) {
                publicKey = (RSAPublicKey) pk;
            }
        }

        return publicKey;
    }

    /**
     * Returns ECDSA public key.
     * 
     * @param jwkSetUrl Url of the server, that returns jwks (JSON Web Keys), for example: https://ce.gluu.info/jans-auth/restv1/jwks.
     * @param keyId Key Id (kid).
     * @return ECDSA public key.
     */
    public static ECDSAPublicKey getECDSAPublicKey(String jwkSetUrl, String keyId) {
        return getECDSAPublicKey(jwkSetUrl, keyId, null);
    }

    /**
     * Returns ECDSA public key.
     * 
     * @param jwkSetUrl Url of the server, that returns jwks (JSON Web Keys), for example: https://ce.gluu.info/jans-auth/restv1/jwks.
     * @param keyId Key Id (kid).
     * @param engine ClientHttpEngine (HTTP request and response).
     * @return ECDSA public key.
     */
    public static ECDSAPublicKey getECDSAPublicKey(String jwkSetUrl, String keyId, ClientHttpEngine engine) {
        ECDSAPublicKey publicKey = null;

        JwkClient jwkClient = new JwkClient(jwkSetUrl);
        if (engine != null) {
            jwkClient.setExecutor(engine);
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

    /**
     * Returns EDDSA public key.
     * 
     * @param jwkSetUrl Url of the server, that returns jwks (JSON Web Keys), for example: https://ce.gluu.info/jans-auth/restv1/jwks.
     * @param keyId Key Id (kid).
     * @return EDDSA public key.
     */
    public static EDDSAPublicKey getEDDSAPublicKey(String jwkSetUrl, String keyId) {
        return getEDDSAPublicKey(jwkSetUrl, keyId, null);
    }

    /**
     * Returns EDDSA public key.
     * 
     * @param jwkSetUrl Url of the server, that returns jwks (JSON Web Keys), for example: https://ce.gluu.info/jans-auth/restv1/jwks.
     * @param keyId Key Id (kid).
     * @param engine ClientHttpEngine (HTTP request and response).
     * @return EDDSA public key.
     */
    public static EDDSAPublicKey getEDDSAPublicKey(String jwkSetUrl, String keyId, ClientHttpEngine engine) {
        EDDSAPublicKey publicKey = null;

        JwkClient jwkClient = new JwkClient(jwkSetUrl);
        if (engine != null) {
            jwkClient.setExecutor(engine);
        }
        JwkResponse jwkResponse = jwkClient.exec();
        if (jwkResponse != null && jwkResponse.getStatus() == 200) {
            PublicKey pk = jwkResponse.getPublicKey(keyId);
            if (pk instanceof EDDSAPublicKey) {
                publicKey = (EDDSAPublicKey) pk;
            }
        }

        return publicKey;
    }
}