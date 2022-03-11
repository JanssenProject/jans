/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import io.jans.as.client.RegisterResponse;
import io.jans.as.model.register.RegisterRequestParam;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version February 11, 2022
 */

public class Asserter {

    private Asserter() {

    }

    public static void assertRegisterResponseClaimsNotNull(RegisterResponse response, RegisterRequestParam... claimsToVerify) {
        if (response == null || claimsToVerify == null) {
            return;
        }
        for (RegisterRequestParam claim : claimsToVerify) {
            assertNotNull(response.getClaims().get(claim.toString()), "Claim " + claim.toString() + " is null in response claims - code" + response.getEntity());
        }
    }

    public static void assertRegisterResponseClaimsAreContained(RegisterResponse response, RegisterRequestParam... claimsToVerify) {
        if (response == null || claimsToVerify == null) {
            return;
        }
        for (RegisterRequestParam claim : claimsToVerify) {
            assertTrue(response.getClaims().containsKey(claim.toString()), "Claim " + claim.toString() + " is not contained in response claims - code" + response.getEntity());
        }
    }
}
