/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.RegisterResponse;

import static io.jans.as.test.TestUtil.assertNotBlank;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/10/2013
 */

public class ClientTestUtil {

    private ClientTestUtil() {
    }

    public static void assert_(RegisterResponse response) {
        assertNotNull(response);
        assertNotBlank(response.getClientId());
        assertNotBlank(response.getClientSecret());
        assertNotBlank(response.getRegistrationAccessToken());
        assertNotBlank(response.getRegistrationClientUri());
        assertNotNull(response.getClientIdIssuedAt());
        assertNotNull(response.getClientSecretExpiresAt());
    }
}
