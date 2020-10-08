/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ws.rs;

import static org.testng.Assert.assertNotNull;
import static org.gluu.oxauth.model.uma.TestUtil.assertNotBlank;

import org.gluu.oxauth.client.RegisterResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/10/2013
 */

public class ClientTestUtil {

    private ClientTestUtil() {
    }

    public static void assert_(RegisterResponse p_response) {
        assertNotNull(p_response);
        assertNotBlank(p_response.getClientId());
        assertNotBlank(p_response.getClientSecret());
        assertNotBlank(p_response.getRegistrationAccessToken());
        assertNotBlank(p_response.getRegistrationClientUri());
        assertNotNull(p_response.getClientIdIssuedAt());
        assertNotNull(p_response.getClientSecretExpiresAt());
    }
}
