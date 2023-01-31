/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.EDDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidParameterException;
import io.jans.as.model.jwt.Jwt;
import io.jans.util.StringHelper;
import io.jans.util.security.SecurityProviderUtility;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import org.bouncycastle.openssl.PEMParser;
import java.util.Set;

import static io.jans.as.model.jwk.JWKParameter.ALGORITHM;
import static io.jans.as.model.jwk.JWKParameter.CERTIFICATE_CHAIN;
import static io.jans.as.model.jwk.JWKParameter.EXPONENT;
import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static io.jans.as.model.jwk.JWKParameter.KEY_ID;
import static io.jans.as.model.jwk.JWKParameter.MODULUS;
import static io.jans.as.model.jwk.JWKParameter.PUBLIC_KEY;
import static io.jans.as.model.jwk.JWKParameter.X;
import static io.jans.as.model.jwk.JWKParameter.Y;

/**
 * Utility class (can't be instantiated), that provides suite of additional functions,
 * which can be used, during JWT/JWE processing.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Sergey Manoylo
 * @version December 5, 2021
 */
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private JwtUtil() {
    }

    public static void printAlgorithmsAndProviders() {
        printAlgorithms("Signature");
        printAlgorithms("MessageDigest");
        printAlgorithms("Cipher");
        printAlgorithms("Mac");
        printAlgorithms("KeyStore");
        for (Provider provider : Security.getProviders()) {
            log.trace("Provider: {}", provider.getName());
        }
    }

    public static void printAlgorithms(String algorithmType) {
        Set<String> algorithms = Security.getAlgorithms(algorithmType);
        for (String algorithm : algorithms) {
            log.trace("Algorithm ({}}): {}", algorithmType, algorithm);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] getMessageDigestSHA256(String data) throws NoSuchProviderException, NoSuchAlgorithmException {
        return getMessageDigest(data, "SHA-256");
    }

    public static byte[] getMessageDigestSHA384(String data) throws NoSuchProviderException, NoSuchAlgorithmException {
        return getMessageDigest(data, "SHA-384");
    }

    public static byte[] getMessageDigestSHA512(String data) throws NoSuchProviderException, NoSuchAlgorithmException {
        return getMessageDigest(data, "SHA-512");
    }

    public static byte[] getMessageDigest(String data, String algorithm) throws NoSuchProviderException, NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm, SecurityProviderUtility.getBCProvider()).digest(data.getBytes(StandardCharsets.UTF_8));
    }

    public static io.jans.as.model.crypto.PublicKey getPublicKey(
            String jwksUri, String jwks, SignatureAlgorithm signatureAlgorithm, String keyId) {

        JSONObject jsonKeyValue = getJsonKey(jwksUri, jwks, keyId);

        if (jsonKeyValue == null) {
            return null;
        }

        io.jans.as.model.crypto.PublicKey publicKey = null;

        try {
            String resultKeyId = jsonKeyValue.getString(KEY_ID);
            if (signatureAlgorithm == null) {
                signatureAlgorithm = SignatureAlgorithm.fromString(jsonKeyValue.getString(ALGORITHM));
                if (signatureAlgorithm == null) {
                    log.error("Failed to determine key '{}' signature algorithm", resultKeyId);
                    return null;
                }
            }

            JSONObject jsonPublicKey = jsonKeyValue;
            if (jsonKeyValue.has(PUBLIC_KEY)) {
                // Use internal jwks.json format
                jsonPublicKey = jsonKeyValue.getJSONObject(PUBLIC_KEY);
            }

            AlgorithmFamily algorithmFamily = signatureAlgorithm.getFamily();
            if (algorithmFamily == AlgorithmFamily.RSA) {
                String exp = jsonPublicKey.getString(EXPONENT);
                String mod = jsonPublicKey.getString(MODULUS);

                BigInteger publicExponent = new BigInteger(1, Base64Util.base64urldecode(exp));
                BigInteger modulus = new BigInteger(1, Base64Util.base64urldecode(mod));

                publicKey = new RSAPublicKey(modulus, publicExponent);
            } else if (algorithmFamily == AlgorithmFamily.EC) {
                String xx = jsonPublicKey.getString(X);
                String yy = jsonPublicKey.getString(Y);

                BigInteger x = new BigInteger(1, Base64Util.base64urldecode(xx));
                BigInteger y = new BigInteger(1, Base64Util.base64urldecode(yy));

                publicKey = new ECDSAPublicKey(signatureAlgorithm, x, y);
            } else if (algorithmFamily == AlgorithmFamily.ED) {
                String xx = jsonPublicKey.getString(X);

                BigInteger x = new BigInteger(1, Base64Util.base64urldecode(xx));

                publicKey = new EDDSAPublicKey(signatureAlgorithm, x.toByteArray());
            } else {
                throw new InvalidParameterException("Wrong value of the AlgorithmFamily: algorithmFamily = " + algorithmFamily);
            }
            
            if (publicKey != null && jsonKeyValue.has(CERTIFICATE_CHAIN)) {
                final String BEGIN = "-----BEGIN CERTIFICATE-----";
                final String END = "-----END CERTIFICATE-----";

                JSONArray certChain = jsonKeyValue.getJSONArray(CERTIFICATE_CHAIN);
                String certificateString = BEGIN + "\n" + certChain.getString(0) + "\n" + END;
                StringReader sr = new StringReader(certificateString);
                PEMParser pemReader = new PEMParser(sr);
                X509Certificate cert = (X509Certificate) pemReader.readObject();
                Certificate certificate = new Certificate(signatureAlgorithm, cert);
                publicKey.setCertificate(certificate);
            }
            if (publicKey != null) {
                publicKey.setKeyId(resultKeyId);
                publicKey.setSignatureAlgorithm(signatureAlgorithm);
            }
            

            if (jsonKeyValue.has(CERTIFICATE_CHAIN)) {
                final String BEGIN = "-----BEGIN CERTIFICATE-----";
                final String END = "-----END CERTIFICATE-----";

                JSONArray certChain = jsonKeyValue.getJSONArray(CERTIFICATE_CHAIN);
                String certificateString = BEGIN + "\n" + certChain.getString(0) + "\n" + END;
                StringReader sr = new StringReader(certificateString);
                PEMParser pemReader = new PEMParser(sr);
                X509Certificate cert = (X509Certificate) pemReader.readObject();
                Certificate certificate = new Certificate(signatureAlgorithm, cert);
                publicKey.setCertificate(certificate);
            }

            publicKey.setKeyId(resultKeyId);
            publicKey.setSignatureAlgorithm(signatureAlgorithm);

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return publicKey;
    }

    private static String getJwks(String jwksUri) {
        String jwks = "";
        jakarta.ws.rs.client.Client clientRequest = ClientBuilder.newClient();
        try {
            Response clientResponse = clientRequest.target(jwksUri).request().buildGet().invoke();

            int status = clientResponse.getStatus();
            if (log.isDebugEnabled()) {
                log.debug(String.format("Status: %s", String.valueOf(status)));
            }

            if (status == 200) {
                jwks = clientResponse.readEntity(String.class);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("JWK: %s", jwks));
                }
            }
        } finally {
            clientRequest.close();
        }
        return jwks;
    }

    private static JSONObject buildJsonKey(String jwks, String keyId) {
        JSONObject jsonKey = null;
        JSONObject jsonObject = new JSONObject(jwks);
        JSONArray keys = jsonObject.getJSONArray(JSON_WEB_KEY_SET);
        if (keys.length() > 0) {
            if (StringHelper.isEmpty(keyId)) {
                jsonKey = keys.getJSONObject(0);
            } else {
                for (int i = 0; i < keys.length(); i++) {
                    JSONObject kv = keys.getJSONObject(i);
                    if (kv.getString(KEY_ID).equals(keyId)) {
                        jsonKey = kv;
                        break;
                    }
                }
            }
        }
        return jsonKey;
    }

    public static JSONObject getJsonKey(String jwksUri, String jwks, String keyId) {
        JSONObject jsonKey = null;
        try {
            if (StringHelper.isEmpty(jwks)) {
                jwks = getJwks(jwksUri);
            }
            if (StringHelper.isNotEmpty(jwks)) {
                jsonKey = buildJsonKey(jwks, keyId);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return jsonKey;
    }

    public static JSONObject getJSONWebKeys(String jwksUri) {
        return getJSONWebKeys(jwksUri, null);
    }

    public static JSONObject getJSONWebKeys(String jwksUri, ClientHttpEngine engine) {
        log.debug("Retrieving jwks {}...", jwksUri);

        JSONObject jwks = null;
        try {
            if (!StringHelper.isEmpty(jwksUri)) {
                ClientBuilder clientBuilder = ClientBuilder.newBuilder();
                if (engine != null) {
                    ((ResteasyClientBuilder) clientBuilder).httpEngine(engine);
                }

                jakarta.ws.rs.client.Client clientRequest = clientBuilder.build();
                try {
                    Response clientResponse = clientRequest.target(jwksUri).request().buildGet().invoke();

                    int status = clientResponse.getStatus();
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Status: %s", status));
                    }

                    if (status == 200) {
                        jwks = fromJson(clientResponse.readEntity(String.class));
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("JWK: %s", jwks));
                        }
                    }
                } finally {
                    clientRequest.close();
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return jwks;
    }

    public static JSONObject fromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        return mapper.readValue(json, JSONObject.class);
    }

    public static void transferIntoJwtClaims(JSONObject jsonObject, Jwt jwt) {
        if (jsonObject == null || jwt == null) {
            return;
        }

        for (String key : jsonObject.keySet()) {
            final Object value = jsonObject.opt(key);
            jwt.getClaims().setClaimObject(key, value, true);
        }
    }
}