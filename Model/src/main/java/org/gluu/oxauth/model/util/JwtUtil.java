/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.util;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.openssl.PEMParser;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.crypto.Certificate;
import org.gluu.oxauth.model.crypto.PublicKey;
import org.gluu.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.util.StringHelper;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.HttpMethod;

import static org.gluu.oxauth.model.jwk.JWKParameter.*;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version December 8, 2018
 */
public class JwtUtil {

    private static final Logger log = Logger.getLogger(JwtUtil.class);

    public static void printAlgorithmsAndProviders() {
        Set<String> algorithms = Security.getAlgorithms("Signature");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (Signature): " + algorithm);
        }
        algorithms = Security.getAlgorithms("MessageDigest");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (MessageDigest): " + algorithm);
        }
        algorithms = Security.getAlgorithms("Cipher");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (Cipher): " + algorithm);
        }
        algorithms = Security.getAlgorithms("Mac");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (Mac): " + algorithm);
        }
        algorithms = Security.getAlgorithms("KeyStore");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (KeyStore): " + algorithm);
        }
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            log.trace("Provider: " + provider.getName());
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

    public static byte[] getMessageDigestSHA256(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-256", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    public static byte[] getMessageDigestSHA384(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-384", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    public static byte[] getMessageDigestSHA512(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-512", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    public static PublicKey getPublicKey(
            String jwksUri, String jwks, SignatureAlgorithm signatureAlgorithm, String keyId) {
        log.debug("Retrieving JWK...");

        JSONObject jsonKeyValue = getJsonKey(jwksUri, jwks, keyId);

        if (jsonKeyValue == null) {
            return null;
        }

        org.gluu.oxauth.model.crypto.PublicKey publicKey = null;

        try {
            String resultKeyId = jsonKeyValue.getString(KEY_ID);
            if (signatureAlgorithm == null) {
                signatureAlgorithm = SignatureAlgorithm.fromString(jsonKeyValue.getString(ALGORITHM));
                if (signatureAlgorithm == null) {
                    log.error(String.format("Failed to determine key '%s' signature algorithm", resultKeyId));
                    return null;
                }
            }

            JSONObject jsonPublicKey = jsonKeyValue;
            if (jsonKeyValue.has(PUBLIC_KEY)) {
                // Use internal jwks.json format
                jsonPublicKey = jsonKeyValue.getJSONObject(PUBLIC_KEY);
            }

            if (signatureAlgorithm == SignatureAlgorithm.RS256 || signatureAlgorithm == SignatureAlgorithm.RS384 || signatureAlgorithm == SignatureAlgorithm.RS512) {
                //String alg = jsonKeyValue.getString(ALGORITHM);
                //String use = jsonKeyValue.getString(KEY_USE);
                String exp = jsonPublicKey.getString(EXPONENT);
                String mod = jsonPublicKey.getString(MODULUS);

                BigInteger publicExponent = new BigInteger(1, Base64Util.base64urldecode(exp));
                BigInteger modulus = new BigInteger(1, Base64Util.base64urldecode(mod));

                publicKey = new RSAPublicKey(modulus, publicExponent);
            } else if (signatureAlgorithm == SignatureAlgorithm.ES256 || signatureAlgorithm == SignatureAlgorithm.ES384 || signatureAlgorithm == SignatureAlgorithm.ES512) {
                //String alg = jsonKeyValue.getString(ALGORITHM);
                //String use = jsonKeyValue.getString(KEY_USE);
                //String crv = jsonKeyValue.getString(CURVE);
                String xx = jsonPublicKey.getString(X);
                String yy = jsonPublicKey.getString(Y);

                BigInteger x = new BigInteger(1, Base64Util.base64urldecode(xx));
                BigInteger y = new BigInteger(1, Base64Util.base64urldecode(yy));

                publicKey = new ECDSAPublicKey(signatureAlgorithm, x, y);
            }

            if (publicKey != null && jsonKeyValue.has(CERTIFICATE_CHAIN)) {
                final String BEGIN = "-----BEGIN CERTIFICATE-----";
                final String END = "-----END CERTIFICATE-----";

                JSONArray certChain = jsonKeyValue.getJSONArray(CERTIFICATE_CHAIN);
                String certificateString = BEGIN + "\n" + certChain.getString(0) + "\n" + END;
                StringReader sr = new StringReader(certificateString);
                PEMParser pemReader = new PEMParser(sr);
                X509Certificate cert = (X509CertificateObject) pemReader.readObject();
                Certificate certificate = new Certificate(signatureAlgorithm, cert);
                publicKey.setCertificate(certificate);
            }
            if (publicKey != null) {
                publicKey.setKeyId(resultKeyId);
                publicKey.setSignatureAlgorithm(signatureAlgorithm);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return publicKey;
    }

    public static JSONObject getJsonKey(String jwksUri, String jwks, String keyId) {
        log.debug("Retrieving JWK Key...");

        JSONObject jsonKey = null;
        try {
            if (StringHelper.isEmpty(jwks)) {
                ClientRequest clientRequest = new ClientRequest(jwksUri);
                clientRequest.setHttpMethod(HttpMethod.GET);
                ClientResponse<String> clientResponse = clientRequest.get(String.class);

                int status = clientResponse.getStatus();
                log.debug(String.format("Status: %n%d", status));

                if (status == 200) {
                    jwks = clientResponse.getEntity(String.class);
                    log.debug(String.format("JWK: %s", jwks));
                }
            }
            if (StringHelper.isNotEmpty(jwks)) {
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
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return jsonKey;
    }

    public static JSONObject getJSONWebKeys(String jwksUri) {
        log.debug("Retrieving jwks " + jwksUri + "...");

        JSONObject jwks = null;
        try {
            if (!StringHelper.isEmpty(jwksUri)) {
                ClientRequest clientRequest = new ClientRequest(jwksUri);
                clientRequest.setHttpMethod(HttpMethod.GET);
                ClientResponse<String> clientResponse = clientRequest.get(String.class);

                int status = clientResponse.getStatus();
                log.debug(String.format("Status: %n%d", status));

                if (status == 200) {
                    jwks = new JSONObject(clientResponse.getEntity(String.class));
                    log.debug(String.format("JWK: %s", jwks));
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return jwks;
    }
}