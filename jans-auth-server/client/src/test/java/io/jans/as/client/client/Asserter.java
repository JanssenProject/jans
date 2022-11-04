/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.RegisterResponse;
import io.jans.as.model.register.RegisterRequestParam;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version April 6, 2022
 */
public class Asserter {

    private Asserter() {
    }

    public static void assertBlank(String value) {
        assertTrue(StringUtils.isNotBlank(value));
    }

    public static void assertBlank(String value, String message) {
        assertTrue(StringUtils.isBlank(value), message);
    }

    public static void assertNotBlank(String value) {
        assertTrue(StringUtils.isNotBlank(value));
    }

    public static void assertNotBlank(String value, String message) {
        assertTrue(StringUtils.isNotBlank(value), message);
    }

    public static void assertRegisterResponseClaimsNotNull(RegisterResponse response, RegisterRequestParam... claimsToVerify) {
        if (response == null || claimsToVerify == null) {
            return;
        }
        for (RegisterRequestParam claim : claimsToVerify) {
            assertNotNull(response.getClaims().get(claim.toString()), "Claim " + claim + " is null in response claims - code" + response.getEntity());
        }
    }

    public static void assertRegisterResponseClaimsNotNull(RegisterResponse response, String... claimsToVerify) {
        if (response == null || claimsToVerify == null) {
            return;
        }
        Arrays.stream(claimsToVerify).forEach(
                claim -> assertNotNull(response.getClaims().get(claim), "Claim " + claim + " is null in response claims - code" + response.getEntity()));
    }

    public static void assertRegisterResponseClaimsAreContained(RegisterResponse response, RegisterRequestParam... claimsToVerify) {
        if (response == null || claimsToVerify == null) {
            return;
        }
        for (RegisterRequestParam claim : claimsToVerify) {
            assertTrue(response.getClaims().containsKey(claim.toString()), "Claim " + claim + " is not contained in response claims - code" + response.getEntity());
        }
    }

    public static void assertOpenIdConfigurationResponse(OpenIdConfigurationResponse response) {
        assertEquals(response.getStatus(), 200, "Unexpected response code");
        assertNotNull(response.getIssuer(), "The issuer is null");
        assertNotNull(response.getAuthorizationEndpoint(), "The authorizationEndpoint is null");
        assertNotNull(response.getTokenEndpoint(), "The tokenEndpoint is null");
        assertNotNull(response.getRevocationEndpoint(), "The tokenRevocationEndpoint is null");
        assertNotNull(response.getUserInfoEndpoint(), "The userInfoEndPoint is null");
        assertNotNull(response.getClientInfoEndpoint(), "The clientInfoEndPoint is null");
        assertNotNull(response.getCheckSessionIFrame(), "The checkSessionIFrame is null");
        assertNotNull(response.getEndSessionEndpoint(), "The endSessionEndpoint is null");
        assertNotNull(response.getJwksUri(), "The jwksUri is null");
        assertNotNull(response.getRegistrationEndpoint(), "The registrationEndpoint is null");
        assertNotNull(response.getIntrospectionEndpoint(), "The introspectionEndpoint is null");
        assertNotNull(response.getParEndpoint(), "The parEndpoint is null");

        assertTrue(response.getScopesSupported().size() > 0, "The scopesSupported is empty");
        assertTrue(response.getResponseTypesSupported().size() > 0, "The responseTypesSupported is empty");
        assertTrue(response.getGrantTypesSupported().size() > 0, "The grantTypesSupported is empty");
        assertTrue(response.getSubjectTypesSupported().size() > 0, "The subjectTypesSupported is empty");
        assertTrue(response.getIdTokenSigningAlgValuesSupported().size() > 0, "The idTokenSigningAlgValuesSupported is empty");
        assertTrue(response.getRequestObjectSigningAlgValuesSupported().size() > 0, "The requestObjectSigningAlgValuesSupported is empty");
        assertTrue(response.getTokenEndpointAuthMethodsSupported().size() > 0, "The tokenEndpointAuthMethodsSupported is empty");
        assertTrue(response.getTokenEndpointAuthSigningAlgValuesSupported().size() > 0, "The tokenEndpointAuthSigningAlgValuesSupported is empty");
        assertTrue(response.getClaimsSupported().size() > 0, "The claimsSupported is empty");

    }
}
