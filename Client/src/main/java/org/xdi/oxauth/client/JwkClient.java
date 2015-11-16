/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.KeyType;
import org.xdi.oxauth.model.jwk.Use;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * Encapsulates functionality to make JWK request calls to an authorization
 * server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version November 16, 2015
 */
public class JwkClient extends BaseClient<JwkRequest, JwkResponse> {

    private static final String mediaType = MediaType.APPLICATION_JSON;

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
    public JwkRequest getRequest() {
        if (request instanceof JwkRequest) {
            return (JwkRequest) request;
        } else {
            return null;
        }
    }

    @Override
    public void setRequest(JwkRequest request) {
        super.request = request;
    }

    @Override
    public JwkResponse getResponse() {
        if (response instanceof JwkResponse) {
            return (JwkResponse) response;
        } else {
            return null;
        }
    }

    @Override
    public void setResponse(JwkResponse response) {
        super.response = response;
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
        clientRequest.accept(mediaType);
        clientRequest.setHttpMethod(getHttpMethod());

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.get(String.class);
            int status = clientResponse.getStatus();

            setResponse(new JwkResponse(status));
            getResponse().setHeaders(clientResponse.getHeaders());

            String entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            if (StringUtils.isNotBlank(entity)) {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(JSON_WEB_KEY_SET)) {
                    JSONArray jwks = jsonObj.getJSONArray(JSON_WEB_KEY_SET);
                    List<JSONWebKey> jwkList = new ArrayList<JSONWebKey>();

                    for (int i = 0; i < jwks.length(); i++) {
                        JSONObject jsonKeyValue = jwks.getJSONObject(i);
                        JSONWebKey JSONWebKey = new JSONWebKey();

                        if (jsonKeyValue.has(KEY_TYPE)) {
                            JSONWebKey.setKeyType(KeyType.fromString(jsonKeyValue.getString(KEY_TYPE)));
                        }
                        if (jsonKeyValue.has(KEY_ID)) {
                            JSONWebKey.setKeyId(jsonKeyValue.getString(KEY_ID));
                        }
                        if (jsonKeyValue.has(KEY_USE)) {
                            JSONWebKey.setUse(Use.fromString(jsonKeyValue.getString(KEY_USE)));
                        }
                        if (jsonKeyValue.has(ALGORITHM)) {
                            JSONWebKey.setAlgorithm(jsonKeyValue.getString(ALGORITHM));
                        }
                        if (jsonKeyValue.has(MODULUS)) {
                            JSONWebKey.getPublicKey().setModulus(jsonKeyValue.getString(MODULUS));
                        }
                        if (jsonKeyValue.has(EXPONENT)) {
                            JSONWebKey.getPublicKey().setExponent(jsonKeyValue.getString(EXPONENT));
                        }
                        if (jsonKeyValue.has(CURVE)) {
                            JSONWebKey.setCurve(jsonKeyValue.getString(CURVE));
                        }
                        if (jsonKeyValue.has(X)) {
                            JSONWebKey.getPublicKey().setX(jsonKeyValue.getString(X));
                        }
                        if (jsonKeyValue.has(Y)) {
                            JSONWebKey.getPublicKey().setY(jsonKeyValue.getString(Y));
                        }

                        jwkList.add(JSONWebKey);
                    }

                    getResponse().setKeys(jwkList);
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
        RSAPublicKey publicKey = null;

        JwkClient jwkClient = new JwkClient(jwkSetUri);
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
        ECDSAPublicKey publicKey = null;

        JwkClient jwkClient = new JwkClient(jwkSetUrl);
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