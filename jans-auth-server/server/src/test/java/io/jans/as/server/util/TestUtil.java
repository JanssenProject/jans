package io.jans.as.server.util;

import io.jans.as.client.RegisterResponse;
import io.jans.as.model.error.ErrorResponse;
import jakarta.ws.rs.core.Response;

import static io.jans.as.model.uma.TestUtil.assertNotBlank;
import static org.testng.Assert.assertEquals;
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

    public static void assertBadRequest(Response response) {
        assertEquals(response.getStatus(), 400);
    }

    public static void assertErrorCode(Response response, String expectedErrorCode) {
        assertEquals(response.readEntity(ErrorResponse.class).getErrorCode(), expectedErrorCode);
    }

    public static boolean testWithExternalApiUrl() {
        return System.getProperties().containsKey("test.jans.auth.url");
    }

    public static String readExternalApiUrl() {
        if (testWithExternalApiUrl()) {
            return System.getProperties().getProperty("test.jans.auth.url");
        }
        return null;
    }

}
