/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.model.jwk;

import com.nimbusds.jose.jwk.JWKException;
import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.util.security.SecurityProviderUtility;

import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class JSONWebKeyTest {

    static {
        Security.addProvider(SecurityProviderUtility.getBCProvider());
    }

    @Test
    public void rsaPublicKey() throws UnsupportedEncodingException, JWKException, NoSuchAlgorithmException, NoSuchProviderException {
        final String publicKeyStr = "{\n" +
                "      \"kty\": \"RSA\",\n" +
                "      \"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\",\n" +
                "      \"e\": \"AQAB\",\n" +
                "      \"alg\": \"RS256\",\n" +
                "      \"kid\": \"2011-04-29\"\n" +
                "}";

        JSONObject jwkJSONObject = new JSONObject(publicKeyStr);
        JSONWebKey jsonWebKey = JSONWebKey.fromJSONObject(jwkJSONObject);

        assertEquals(jsonWebKey.getKty(), KeyType.RSA);
        assertEquals(jsonWebKey.getAlg(), Algorithm.RS256);
        assertNotNull(jsonWebKey.getN());
        assertNotNull(jsonWebKey.getE());
        assertNotNull(jsonWebKey.getKid());

        String jwkThumbprint = jsonWebKey.getJwkThumbprint();
        assertEquals(jwkThumbprint, "NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs");
    }

    @Test
    public void ecPublicKey() throws UnsupportedEncodingException, JWKException, NoSuchAlgorithmException, NoSuchProviderException {
        final String publicKeyStr = "{\n" +
                "      \"kty\": \"EC\",\n" +
                "      \"crv\": \"P-256\",\n" +
                "      \"x\": \"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\n" +
                "      \"y\": \"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\n" +
                "      \"use\": \"enc\",\n" +
                "      \"kid\": \"1\"\n" +
                "}";

        JSONObject jwkJSONObject = new JSONObject(publicKeyStr);
        JSONWebKey jsonWebKey = JSONWebKey.fromJSONObject(jwkJSONObject);

        assertEquals(jsonWebKey.getKty(), KeyType.EC);
        assertEquals(jsonWebKey.getCrv(), EllipticEdvardsCurve.P_256);
        assertNotNull(jsonWebKey.getX());
        assertNotNull(jsonWebKey.getY());
        assertNotNull(jsonWebKey.getKid());

        String jwkThumbprint = jsonWebKey.getJwkThumbprint();
        assertEquals(jwkThumbprint, "cn-I_WNMClehiVp51i_0VpOENW1upEerA8sEam5hn-s");
    }
}
