/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.util.security.SecurityProviderUtility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static io.jans.as.model.jwk.JWKParameter.*;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JwtUtilTest extends BaseTest {

    public static String CERT_PEM = "MIIDJzCCAg8CCQCp6GMQxw8GgzANBgkqhkiG9w0BAQsFADB3MQswCQYDVQQGEwJV\n" +
            "UzELMAkGA1UECAwCVFgxDzANBgNVBAcMBkF1c3RpbjEYMBYGA1UECgwPSmFuc3Nl\n" +
            "biBQcm9qZWN0MRMwEQYDVQQDDApKYW5zc2VuIENBMRswGQYJKoZIhvcNAQkBFgxz\n" +
            "QGphbnMubG9jYWwwHhcNMjEwNDIwMTg1NjM4WhcNMjIwNDIwMTg1NjM4WjA0MTIw\n" +
            "MAYDVQQDDCkxODAxLjdmNzM0OGQ4LWFjOWUtNDk1MS1hYmRmLWYyMjUzNzhmMzJm\n" +
            "ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKdQcPocZ3rmmly1LLxQ\n" +
            "dUk0VcKq3uuR3qYd1+tISpWVOMVTuIhz8j9286WfcFMyzQKDRHJCsYiCCKsenuBs\n" +
            "BE98nYmIqVOnJxYMBue9IDIYi9I5njzPy9pWnisCG5fKjHmnP288ifrEtwXESzw2\n" +
            "eQViZL0sgdo1ziPXyV5kaYjOgApWY56PE/vuv3+XxJ2iMzdEz6yOtOmJMHE3ZZCu\n" +
            "ruhW5AGIwg6KazgaNoKWil8+u8/ZqruUdErvN21oXWFJWFrh/rSQq96V7S8e0nPF\n" +
            "gLLpeH/YGWHmVGS77XPz2c6XhQM0uRCIBcuvnvJeQyvZxrlHetUWBYG8n7d1ZSer\n" +
            "YEkCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAbtaPcipL7GpLtQqY47DpV6jsQl9n\n" +
            "ws9url8SpKThIuATRw77Cj4XjL2DkNxANDTaueobQkt4vFw1edfbwExvavUpmsnR\n" +
            "WeBtKMHDNEN+fCyBbhBi67K1ArZHkx5OWLERd4qL64T5CiAwWXVbE3gCaXMV9/A3\n" +
            "8/Vvly5b4YRojK5UrpPIyG5gnE8YGVS7p3n4aIZ5r3/ynPvFwwTCIIlPdctOABQU\n" +
            "fIctm8i8/CHhdqFVvVxa0oZ9sTr4VJ3/Kw41M9pI+zY754tjrnadGBSO/tIibjjI\n" +
            "Scr80QwkiP7Cq6LRDT3VUok2OighFFAmyAfZQg9qR5udbMd+DynAyvJjBQ==";

    @Test
    public void transferIntoJwtClaims_validExampleJson_jsonObject() {
        showTitle("transferIntoJwtClaims_validExampleJson_jsonObject");
        JSONObject json = new JSONObject();
        json.put("active", true);
        json.put("key", "valueTest");

        Jwt jwt = new Jwt();
        JwtUtil.transferIntoJwtClaims(json, jwt);

        final JwtClaims claims = jwt.getClaims();
        assertEquals(claims.getClaimAsString("active"), "true");
        assertEquals(claims.getClaimAsString("key"), "valueTest");
    }

    @Test
    public void getPublicKey_algorithmFamilyRSA_validPublicKey() {
        showTitle("getPublicKey_algorithmFamilyRSA_validPublicKey");
        try (MockedStatic<JwtUtil> fooUtilsMocked = Mockito.mockStatic(JwtUtil.class, invocation -> {
            Method method = invocation.getMethod();
            if ("getPublicKey".equals(method.getName())) {
                return invocation.callRealMethod();
            } else {
                return invocation.getMock();
            }
        })) {
            fooUtilsMocked.when(() -> JwtUtil.getJsonKey(null, null, "keyId001")).thenReturn(getJsonDefault(null));
            io.jans.as.model.crypto.PublicKey publicKey = JwtUtil.getPublicKey(null, null, SignatureAlgorithm.RS256, "keyId001");
            assertNotNull(publicKey);
        }
    }

    @Test
    public void getPublicKey_algorithmFamilyEC_validPublicKey() {
        showTitle("getPublicKey_algorithmFamilyEC_validPublicKey");
        try (MockedStatic<JwtUtil> fooUtilsMocked = Mockito.mockStatic(JwtUtil.class, invocation -> {
            Method method = invocation.getMethod();
            if ("getPublicKey".equals(method.getName())) {
                return invocation.callRealMethod();
            } else {
                return invocation.getMock();
            }
        })) {
            fooUtilsMocked.when(() -> JwtUtil.getJsonKey(null, null, "keyId001")).thenReturn(getJsonDefault(null));
            io.jans.as.model.crypto.PublicKey publicKey = JwtUtil.getPublicKey(null, null, SignatureAlgorithm.ES256, "keyId001");
            assertNotNull(publicKey);
        }
    }

    @Test
    public void getPublicKey_algorithmFamilyED_validPublicKey() {
        showTitle("getPublicKey_algorithmFamilyED_validPublicKey");
        try (MockedStatic<JwtUtil> fooUtilsMocked = Mockito.mockStatic(JwtUtil.class, invocation -> {
            Method method = invocation.getMethod();
            if ("getPublicKey".equals(method.getName())) {
                return invocation.callRealMethod();
            } else {
                return invocation.getMock();
            }
        })) {
            fooUtilsMocked.when(() -> JwtUtil.getJsonKey(null, null, "keyId001")).thenReturn(getJsonDefault(null));
            io.jans.as.model.crypto.PublicKey publicKey = JwtUtil.getPublicKey(null, null, SignatureAlgorithm.EDDSA, "keyId001");
            assertNotNull(publicKey);
        }
    }

    @Test
    public void getPublicKey_algorithmFamilyHMAC_logWrongFamilyAndnullPublicKey() {
        showTitle("getPublicKey_algorithmFamilyHMAC_logWrongFamilyAndnullPublicKey");
        io.jans.as.model.crypto.PublicKey publicKey =  mockGetPublicKey(SignatureAlgorithm.HS256);
        assertNull(publicKey);
    }

    @Test
    public void fromJson_validStringJson_jsonObject() throws IOException {
        showTitle("fromJson_validStringJson_jsonObject");
        String json = "{\"key1\":\"value1\", \"key2\":\"value2\"}";
        JSONObject jsonObject =JwtUtil.fromJson(json);
        assertNotNull(jsonObject);
    }

    @Test
    public void transferIntoJwtClaims_validStringJson_jsonObject() throws IOException {
        showTitle("transferIntoJwtClaims_validStringJson_jsonObject");
        String json = "{\"claim1\":\"value1\", \"claim2\":\"value2\"}";
        JSONObject jsonObject =JwtUtil.fromJson(json);
        Jwt jwt = new Jwt();
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim("claim0", "value0");
        jwt.setClaims(jwtClaims);
        assertTrue(jwt.getClaims().hasClaim("claim0"));
        assertFalse(jwt.getClaims().hasClaim("claim1"));
        assertFalse(jwt.getClaims().hasClaim("claim2"));
        JwtUtil.transferIntoJwtClaims(jsonObject, jwt);
        assertTrue(jwt.getClaims().hasClaim("claim1"));
        assertTrue(jwt.getClaims().hasClaim("claim2"));
    }

    @Test
    public void bytesToHex_validBytes_stringHexCharacters(){
        showTitle("bytesToHex_validBytes_stringHexCharacters");
        String textValue = "Hello World";
        String hexValue = JwtUtil.bytesToHex(textValue.getBytes(StandardCharsets.UTF_8));
        assertEquals(hexValue, "48656c6c6f20576f726c64");
    }

    @Test
    public void getMessageDigestSHA256_validString_bytesMessageDigets() throws NoSuchAlgorithmException, NoSuchProviderException {
        showTitle("getMessageDigestSHA256_validString_bytesMessageDigets");
        SecurityProviderUtility.installBCProvider(true);
        String textValue = "Hello World";
        byte[] result = JwtUtil.getMessageDigestSHA256(textValue);
        byte[] expected = {-91, -111, -90, -44, 11, -12, 32, 64, 74, 1, 23, 51, -49, -73, -79, -112, -42, 44, 101, -65, 11, -51, -93, 43, 87, -78, 119, -39, -83, -97, 20, 110};
        assertEquals(result, expected);
    }

    @Test
    public void getMessageDigestSHA384_validString_bytesMessageDigets() throws NoSuchAlgorithmException, NoSuchProviderException {
        showTitle("getMessageDigestSHA384_validString_bytesMessageDigets");
        SecurityProviderUtility.installBCProvider(true);
        String textValue = "Hello World";
        byte[] result = JwtUtil.getMessageDigestSHA384(textValue);
        byte[] expected = {-103, 81, 67, 41, 24, 107, 47, 106, -28, -95, 50, -98, 126, -26, -58, 16, -89, 41, 99, 99, 53, 23, 74, -58, -73, 64, -7, 2, -125, -106, -4, -56, 3, -48, -23, 56, 99, -89, -61, -39, 15, -122, -66, -18, 120, 47, 79, 63};
        assertEquals(result, expected);
    }

    @Test
    public void getMessageDigestSHA512_validString_bytesMessageDigets() throws NoSuchAlgorithmException, NoSuchProviderException {
        showTitle("getMessageDigestSHA512_validString_bytesMessageDigets");
        SecurityProviderUtility.installBCProvider(true);
        String textValue = "Hello World";
        byte[] result = JwtUtil.getMessageDigestSHA512(textValue);
        byte[] expected = {44, 116, -3, 23, -19, -81, -40, 14, -124, 71, -80, -44, 103, 65, -18, 36, 59, 126, -73, 77, -46, 20, -102, 10, -79, -71, 36, 111, -77, 3, -126, -14, 126, -123, 61, -123, -123, 113, -98, 14, 103, -53, -38, 13, -86, -113, 81, 103, 16, 100, 97, 93, 100, 90, -30, 122, -53, 21, -65, -79, 68, 127, 69, -101};
        assertEquals(result, expected);
    }

    private io.jans.as.model.crypto.PublicKey mockGetPublicKey(SignatureAlgorithm signatureAlgorithm){
        try (MockedStatic<JwtUtil> jwtUtilMockedStatic = Mockito.mockStatic(JwtUtil.class, invocation -> {
            Method method = invocation.getMethod();
            if ("getPublicKey".equals(method.getName())) {
                return invocation.callRealMethod();
            } else {
                return invocation.getMock();
            }
        })) {
            jwtUtilMockedStatic.when(() -> JwtUtil.getJsonKey(null, null, "keyId001")).thenReturn(getJsonDefault(null));
            io.jans.as.model.crypto.PublicKey publicKey = JwtUtil.getPublicKey(null, null, signatureAlgorithm, "keyId001");
            return publicKey;
        }
    }

    private JSONObject getJsonDefault(String certificatePem) {
        JSONObject json = new JSONObject();
        JSONObject jsonPublicKey = new JSONObject();
        jsonPublicKey.put(EXPONENT, "NjU1Mzc");
        jsonPublicKey.put(X, "NjU1Mzc");
        jsonPublicKey.put(MODULUS, "MjExMjE0NjQwMjA4NTY1ODg4MDI4NDY2NTYwNDczMDAzMDE2ODkxODM3NzE0OTQ1Nzg5NjE4ODM2NDY5NzM0Njc1MjY3ODY4MjM0MDY5NzkyNjQ4NTUxMDUxMTcxMjI0MzA2NDAyNDExMzM1MzI5MTEwNTY4MDYyNTcyMjAzMzM0MDIwOTQzOTk2MjI5MTE3NDc2MDY2ODA1MDY4NTU0MTAwNDU5MDU2MDEwNTgyNTYwMDg4MzA1NzI2NDIwNDQ4NDI3NTY4NDUxODc0MDgwOTA0Nzk5NTAxODM1OTI4NTI1MzEyMzM2NzY5NTQ3NDkyMjEzMjY1Mzk1MDE4NzMzNTQ3MTc5MDM1NzQ0MzQyNzU0NDM0MjUxMjczNzM4MzQ3ODUwNTIwMzY2NDgwNDkzMjk2Njk2NDEzMzIxNDYyNjI4NzA4MTU1ODM5MDc4NzMzMTY3NDUwODEyNTU4ODI4MDQzNDcwNjYwNzI4NDg5ODQ1OTM5MjkzMzkwNTIwMTEzNjM0OTIyOTMwOTU4NjUwNzg1ODA1ODU2NTAwNjc2NTYxNjQ4MTYxODgzNDExOTI3MTk0ODk1MTgwNTkxNTYxNzU0MzUzNjY2MjY0NjU4MDU5MzUyMjk5Mjc5MTA3NjMyODk0NjExMTA4MDAyNjUxNDM1NjU2OTQ4MTU5MzAyNTAwNzY5MjU3NjgxMTgxMDg5NzEyNzk0ODcwNTIxNDM5NDgzOTk0MTUxODI2OTY1ODUyMzg1MjU2MDM2MTQxNjI0MDQzMDM3MzEzNzExMzcwODU0NDU4MDgwOTUyODM2MTUzMDA4MDQ2ODE");
        jsonPublicKey.put(Y, "MjExMjE0NjQwMjA4NTY1ODg4MDI4NDY2NTYwNDczMDAzMDE2ODkxODM3NzE0OTQ1Nzg5NjE4ODM2NDY5NzM0Njc1MjY3ODY4MjM0MDY5NzkyNjQ4NTUxMDUxMTcxMjI0MzA2NDAyNDExMzM1MzI5MTEwNTY4MDYyNTcyMjAzMzM0MDIwOTQzOTk2MjI5MTE3NDc2MDY2ODA1MDY4NTU0MTAwNDU5MDU2MDEwNTgyNTYwMDg4MzA1NzI2NDIwNDQ4NDI3NTY4NDUxODc0MDgwOTA0Nzk5NTAxODM1OTI4NTI1MzEyMzM2NzY5NTQ3NDkyMjEzMjY1Mzk1MDE4NzMzNTQ3MTc5MDM1NzQ0MzQyNzU0NDM0MjUxMjczNzM4MzQ3ODUwNTIwMzY2NDgwNDkzMjk2Njk2NDEzMzIxNDYyNjI4NzA4MTU1ODM5MDc4NzMzMTY3NDUwODEyNTU4ODI4MDQzNDcwNjYwNzI4NDg5ODQ1OTM5MjkzMzkwNTIwMTEzNjM0OTIyOTMwOTU4NjUwNzg1ODA1ODU2NTAwNjc2NTYxNjQ4MTYxODgzNDExOTI3MTk0ODk1MTgwNTkxNTYxNzU0MzUzNjY2MjY0NjU4MDU5MzUyMjk5Mjc5MTA3NjMyODk0NjExMTA4MDAyNjUxNDM1NjU2OTQ4MTU5MzAyNTAwNzY5MjU3NjgxMTgxMDg5NzEyNzk0ODcwNTIxNDM5NDgzOTk0MTUxODI2OTY1ODUyMzg1MjU2MDM2MTQxNjI0MDQzMDM3MzEzNzExMzcwODU0NDU4MDgwOTUyODM2MTUzMDA4MDQ2ODE");
        json.put(PUBLIC_KEY, jsonPublicKey);
        if (certificatePem != null && !certificatePem.isEmpty()) {
            JSONArray jsonArrayCertificateChain = new JSONArray();
            jsonArrayCertificateChain.put(CERT_PEM);
            json.put(CERTIFICATE_CHAIN, jsonArrayCertificateChain);
        }
        json.put(KEY_ID, "KeyId001");
        return json;
    }

}
