/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto;

import org.apache.commons.httpclient.HttpStatus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.client.*;
import org.gluu.oxeleven.model.JwksRequestParam;
import org.gluu.oxeleven.model.KeyRequestParam;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithmFamily;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version May 4, 2016
 */
public class OxElevenCryptoProvider extends AbstractCryptoProvider {

    private String generateKeyEndpoint;
    private String signEndpoint;
    private String verifySignatureEndpoint;
    private String deleteKeyEndpoint;
    private String jwksEndpoint;

    public OxElevenCryptoProvider(String generateKeyEndpoint, String signEndpoint, String verifySignatureEndpoint, String deleteKeyEndpoint, String jwksEndpoint) {
        this.generateKeyEndpoint = generateKeyEndpoint;
        this.signEndpoint = signEndpoint;
        this.verifySignatureEndpoint = verifySignatureEndpoint;
        this.deleteKeyEndpoint = deleteKeyEndpoint;
        this.jwksEndpoint = jwksEndpoint;
    }

    @Override
    public JSONObject generateKey(SignatureAlgorithm signatureAlgorithm, Long expirationTime) throws Exception {
        GenerateKeyRequest request = new GenerateKeyRequest();
        request.setSignatureAlgorithm(signatureAlgorithm.getName());
        request.setExpirationTime(expirationTime);

        GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
        client.setRequest(request);

        GenerateKeyResponse response = client.exec();
        if (response.getStatus() == HttpStatus.SC_OK && response.getKeyId() != null) {
            return response.getJSONEntity();
        } else {
            throw new Exception(response.getEntity());
        }
    }

    @Override
    public String sign(String signingInput, String keyId, String shardSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        SignRequest request = new SignRequest();
        request.getSignRequestParam().setSigningInput(signingInput);
        request.getSignRequestParam().setAlias(keyId);
        request.getSignRequestParam().setSharedSecret(shardSecret);
        request.getSignRequestParam().setSignatureAlgorithm(signatureAlgorithm.getName());

        SignClient client = new SignClient(signEndpoint);
        client.setRequest(request);

        SignResponse response = client.exec();
        if (response.getStatus() == HttpStatus.SC_OK && response.getSignature() != null) {
            return response.getSignature();
        } else {
            throw new Exception(response.getEntity());
        }
    }

    @Override
    public boolean verifySignature(String signingInput, String signature, String keyId, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        VerifySignatureRequest request = new VerifySignatureRequest();
        request.getVerifySignatureRequestParam().setSigningInput(signingInput);
        request.getVerifySignatureRequestParam().setSignature(signature);
        request.getVerifySignatureRequestParam().setAlias(keyId);
        request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
        request.getVerifySignatureRequestParam().setSignatureAlgorithm(signatureAlgorithm.getName());
        if (jwks != null) {
            request.getVerifySignatureRequestParam().setJwksRequestParam(getJwksRequestParam(jwks));
        }

        VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
        client.setRequest(request);

        VerifySignatureResponse response = client.exec();
        if (response.getStatus() == HttpStatus.SC_OK) {
            return response.isVerified();
        } else {
            throw new Exception(response.getEntity());
        }
    }

    @Override
    public boolean deleteKey(String keyId) throws Exception {
        DeleteKeyRequest request = new DeleteKeyRequest();
        request.setAlias(keyId);

        DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
        client.setRequest(request);

        DeleteKeyResponse response = client.exec();
        if (response.getStatus() == org.apache.http.HttpStatus.SC_OK) {
            return response.isDeleted();
        } else {
            throw new Exception(response.getEntity());
        }
    }

    @Override
    public JSONObject jwks(JSONWebKeySet jsonWebKeySet) throws Exception {
        JwksRequestParam jwks = getJwksRequestParam(jsonWebKeySet);

        JwksRequest request = new JwksRequest();
        request.setJwksRequestParam(jwks);

        JwksClient client = new JwksClient(jwksEndpoint);
        client.setRequest(request);

        JwksResponse response = client.exec();
        if (response.getStatus() == HttpStatus.SC_OK && response.getJwksRequestParam() != null) {
            JSONObject jwkJsonObject = new JSONObject();
            JSONArray keysJsonArray = new JSONArray();
            for (KeyRequestParam key : response.getJwksRequestParam().getKeyRequestParams()) {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(key.getAlg());

                JSONObject keyJsonObject = new JSONObject();
                keyJsonObject.put(ALGORITHM, key.getAlg());
                keyJsonObject.put(KEY_ID, key.getKid());
                keyJsonObject.put(KEY_TYPE, key.getKty());
                keyJsonObject.put(KEY_USE, key.getUse());

                if (SignatureAlgorithmFamily.RSA.equals( signatureAlgorithm.getFamily())) {
                    keyJsonObject.put(MODULUS, key.getN());
                    keyJsonObject.put(EXPONENT, key.getE());
                } else if (SignatureAlgorithmFamily.EC.equals( signatureAlgorithm.getFamily())) {
                    keyJsonObject.put(CURVE, key.getCrv());
                    keyJsonObject.put(X, key.getX());
                    keyJsonObject.put(Y, key.getY());
                }

                keysJsonArray.put(keyJsonObject);
            }
            jwkJsonObject.put(JSON_WEB_KEY_SET, keysJsonArray);

            return jwkJsonObject;
        } else {
            throw new Exception(response.getEntity());
        }
    }
}
