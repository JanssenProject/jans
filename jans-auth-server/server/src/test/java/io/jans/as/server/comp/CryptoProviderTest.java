/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JWKParameter;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.ConfigurableTest;
import org.json.JSONObject;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.security.interfaces.ECPrivateKey;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static io.jans.eleven.model.GenerateKeyResponseParam.KEY_ID;
import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version December 16, 2021
 */
public class CryptoProviderTest extends BaseComponentTest {

    private final static int NUM_KEY_GENS = 100;

    private final static byte testByte01 = (byte) 0x01;

    private final static String SIGNING_INPUT = "Signing Input";
    private final static String SHARED_SECRET = "secret";

    private final static String DEF_GEN_KEYS_PROVIDER = "GenerateKeysDataProvider";

    private final static Object[][] DEF_EC_ALGS_DATA = new Object[][]{
            {Algorithm.ES256},
            {Algorithm.ES384},
            {Algorithm.ES512},
    };

    private static Long expirationTime;
    private static String hs256Signature;
    private static String hs384Signature;
    private static String hs512Signature;
    private static String rs256Key;
    private static String rs256Signature;
    private static String rs384Key;
    private static String rs384Signature;
    private static String rs512Key;
    private static String rs512Signature;
    private static String es256Key;
    private static String es256Signature;
    private static String es384Key;
    private static String es384Signature;
    private static String es512Key;
    private static String es512Signature;

    @Test
    public void configuration() {
        try {
            AppConfiguration appConfiguration = getConfigurationFactory().getAppConfiguration();
            assertNotNull(appConfiguration);

            assertNotNull(getAbstractCryptoProvider());

            GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            calendar.add(GregorianCalendar.MINUTE, 5);
            expirationTime = calendar.getTimeInMillis();
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testSignHS256() {
        try {
            hs256Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, null, SHARED_SECRET, SignatureAlgorithm.HS256);
            assertNotNull(hs256Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignHS256"})
    public void testVerifyHS256() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, hs256Signature, null, null,
                    SHARED_SECRET, SignatureAlgorithm.HS256);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testSignHS384() {
        try {
            hs384Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, null, SHARED_SECRET, SignatureAlgorithm.HS384);
            assertNotNull(hs384Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignHS384"})
    public void testVerifyHS384() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, hs384Signature, null, null,
                    SHARED_SECRET, SignatureAlgorithm.HS384);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testSignHS512() {
        try {
            hs512Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, null, SHARED_SECRET, SignatureAlgorithm.HS512);
            assertNotNull(hs512Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignHS512"})
    public void testVerifyHS512() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, hs512Signature, null, null,
                    SHARED_SECRET, SignatureAlgorithm.HS512);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testGenerateKeyRS256() {
        try {
            JSONObject response = getAbstractCryptoProvider().generateKey(Algorithm.RS256, expirationTime);
            rs256Key = response.optString(KEY_ID);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testGenerateKeyRS256"})
    public void testSignRS256() {
        try {
            rs256Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, rs256Key, null, SignatureAlgorithm.RS256);
            assertNotNull(rs256Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignRS256"})
    public void testVerifyRS256() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, rs256Signature, rs256Key, null,
                    null, SignatureAlgorithm.RS256);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testVerifyRS256"})
    public void testDeleteKeyRS256() {
        try {
            getAbstractCryptoProvider().deleteKey(rs256Key);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testGenerateKeyRS384() {
        try {
            JSONObject response = getAbstractCryptoProvider().generateKey(Algorithm.RS384, expirationTime);
            rs384Key = response.optString(KEY_ID);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testGenerateKeyRS384"})
    public void testSignRS384() {
        try {
            rs384Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, rs384Key, null, SignatureAlgorithm.RS384);
            assertNotNull(rs384Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignRS384"})
    public void testVerifyRS384() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, rs384Signature, rs384Key, null,
                    null, SignatureAlgorithm.RS384);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testVerifyRS384"})
    public void testDeleteKeyRS384() {
        try {
            getAbstractCryptoProvider().deleteKey(rs384Key);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testGenerateKeyRS512() {
        try {
            JSONObject response = getAbstractCryptoProvider().generateKey(Algorithm.RS512, expirationTime);
            rs512Key = response.optString(KEY_ID);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testGenerateKeyRS512"})
    public void testSignRS512() {
        try {
            rs512Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, rs512Key, null, SignatureAlgorithm.RS512);
            assertNotNull(rs512Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignRS512"})
    public void testVerifyRS512() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, rs512Signature, rs512Key, null,
                    null, SignatureAlgorithm.RS512);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testVerifyRS512"})
    public void testDeleteKeyRS512() {
        try {
            getAbstractCryptoProvider().deleteKey(rs512Key);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testGenerateKeyES256() {
        try {
            JSONObject response = getAbstractCryptoProvider().generateKey(Algorithm.ES256, expirationTime);
            es256Key = response.optString(KEY_ID);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testGenerateKeyES256"})
    public void testSignES256() {
        try {
            es256Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, es256Key, null, SignatureAlgorithm.ES256);
            assertNotNull(es256Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignES256"})
    public void testVerifyES256() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, es256Signature, es256Key, null,
                    null, SignatureAlgorithm.ES256);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testVerifyES256"})
    public void testDeleteKeyES256() {
        try {
            getAbstractCryptoProvider().deleteKey(es256Key);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testGenerateKeyES384() {
        try {
            JSONObject response = getAbstractCryptoProvider().generateKey(Algorithm.ES384, expirationTime);
            es384Key = response.optString(KEY_ID);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testGenerateKeyES384"})
    public void testSignES384() {
        try {
            es384Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, es384Key, null, SignatureAlgorithm.ES384);
            assertNotNull(es384Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignES384"})
    public void testVerifyES384() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, es384Signature, es384Key, null,
                    null, SignatureAlgorithm.ES384);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testVerifyES384"})
    public void testDeleteKeyES384() {
        try {
            getAbstractCryptoProvider().deleteKey(es384Key);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"configuration"})
    public void testGenerateKeyES512() {
        try {
            JSONObject response = getAbstractCryptoProvider().generateKey(Algorithm.ES512, expirationTime);
            es512Key = response.optString(KEY_ID);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testGenerateKeyES512"})
    public void testSignES512() {
        try {
            es512Signature = getAbstractCryptoProvider().sign(SIGNING_INPUT, es512Key, null, SignatureAlgorithm.ES512);
            assertNotNull(es512Signature);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testSignES512"})
    public void testVerifyES512() {
        try {
            boolean signatureVerified = getAbstractCryptoProvider().verifySignature(SIGNING_INPUT, es512Signature, es512Key, null,
                    null, SignatureAlgorithm.ES512);
            assertTrue(signatureVerified);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test(dependsOnMethods = {"testVerifyES512"})
    public void testDeleteKeyES512() {
        try {
            getAbstractCryptoProvider().deleteKey(es512Key);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @DataProvider(name = DEF_GEN_KEYS_PROVIDER)
    public Object[][] testGenerateKeysDataProvider() {
        return ConfigurableTest.ArquillianDataProvider.provide(
                "CryptoProviderTest#" + DEF_GEN_KEYS_PROVIDER,
                DEF_EC_ALGS_DATA
        );
    }

    @Test(dependsOnMethods = {"testDeleteKeyRS256", "testDeleteKeyRS384", "testDeleteKeyRS512",
            "testDeleteKeyES256", "testDeleteKeyES384", "testDeleteKeyES512"},
            dataProvider = DEF_GEN_KEYS_PROVIDER)
    public void testGenerateKeys(Algorithm algorithm) {
        for (int i = 0; i < NUM_KEY_GENS; i++) {
            System.out.println("----------------------");
            System.out.println("Algorithm: " + algorithm);
            try {
                JSONObject response = getAbstractCryptoProvider().generateKey(algorithm, expirationTime);
                String keyId = response.optString(JWKParameter.KEY_ID);
                ECPrivateKey ecPrivateKey = (ECPrivateKey) getAbstractCryptoProvider().getPrivateKey(keyId);
                getAbstractCryptoProvider().deleteKey(keyId);

                byte[] s = Base64Util.bigIntegerToUnsignedByteArray(ecPrivateKey.getS());

                System.out.println("s.length = " + s.length);
                System.out.println("s (hex) = " + Base64Util.bytesToHex(s));

                String keyX = (String) response.get(JWKParameter.X);
                String keyY = (String) response.get(JWKParameter.Y);

                System.out.println("keyX = " + keyX);
                System.out.println("keyY = " + keyY);

                byte[] x = Base64Util.base64urldecode(keyX);
                byte[] y = Base64Util.base64urldecode(keyY);

                System.out.println("x.length = " + x.length);
                System.out.println("y.length = " + y.length);

                System.out.println("x (hex) = " + Base64Util.bytesToHex(x));
                System.out.println("y (hex) = " + Base64Util.bytesToHex(y));

                assertTrue(s.length <= algorithm.getKeyLength());
                assertTrue(x.length <= algorithm.getKeyLength());
                assertTrue(y.length <= algorithm.getKeyLength());

                if (algorithm == Algorithm.ES512) {
                    if (s.length * 8 == algorithm.getKeyLength()) {
                        assertEquals(s[0], testByte01);
                    }
                    if (x.length * 8 == algorithm.getKeyLength()) {
                        assertEquals(x[0], testByte01);
                    }
                    if (y.length * 8 == algorithm.getKeyLength()) {
                        assertEquals(y[0], testByte01);
                    }
                }

            } catch (Exception e) {
                fail(e.getMessage(), e);
            }
            System.out.println("----------------------");
        }
    }
}