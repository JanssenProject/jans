package io.jans.ca.plugin.adminui.test;

import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class UserInfoJwtClaimsTest {

    private AUIConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        configuration = new AUIConfiguration();
        configuration.setAuiWebServerClientId("admin-ui-web");
        configuration.setAuiBackendApiServerClientId("admin-ui-api");
    }

    @Test
    public void acceptsFutureExpirationAndConfiguredAudienceString() {
        assertTrue(CommonUtils.hasValidUjwtClaims(claims(System.currentTimeMillis() / 1000 + 60, "admin-ui-web"), configuration));
    }

    @Test
    public void acceptsConfiguredAudienceInArray() {
        assertTrue(CommonUtils.hasValidUjwtClaims(
                claims(System.currentTimeMillis() / 1000 + 60, Arrays.asList("other-client", "admin-ui-api")),
                configuration));
    }

    @Test
    public void rejectsExpiredJwt() {
        assertFalse(CommonUtils.hasValidUjwtClaims(claims(System.currentTimeMillis() / 1000 - 1, "admin-ui-web"), configuration));
    }

    @Test
    public void rejectsMissingExpirationOrMismatchedAudience() {
        assertFalse(CommonUtils.hasValidUjwtClaims(claims(null, "admin-ui-web"), configuration));
        assertFalse(CommonUtils.hasValidUjwtClaims(
                claims(System.currentTimeMillis() / 1000 + 60, "other-client"),
                configuration));
    }

    private Map<String, Object> claims(Object expiration, Object audience) {
        Map<String, Object> claims = new HashMap<>();
        if (expiration != null) {
            claims.put("exp", expiration);
        }
        claims.put("aud", audience);
        return claims;
    }
}
