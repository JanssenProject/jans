package io.jans.as.server.util;

import io.jans.as.client.RegisterResponse;

import static io.jans.as.model.uma.TestUtil.assertNotBlank;
import static org.testng.Assert.assertNotNull;

/**
 * @author yuriyz
 */
public class TestUtil {

    private TestUtil() {
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
