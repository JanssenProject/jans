package io.jans.ca.server;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.GetTokensByCodeResponse2;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.params.CheckIdTokenParams;
import io.jans.ca.common.response.CheckIdTokenResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

public class CheckIdTokenTest {

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void test(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        String state = CoreUtils.secureRandomString();
        String nonce = CoreUtils.secureRandomString();
        GetTokensByCodeResponse2 response = GetTokensByCodeTest.tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, nonce, state);

        final CheckIdTokenParams params = new CheckIdTokenParams();
        params.setOxdId(site.getOxdId());
        params.setIdToken(response.getIdToken());
        params.setNonce(nonce);

        final CheckIdTokenResponse checkR = client.checkIdToken(Tester.getAuthorization(site), null, params);
        assertNotNull(checkR);
        assertTrue(checkR.isActive());
        assertNotNull(checkR.getExpiresAt());
        assertNotNull(checkR.getIssuedAt());
        assertNotNull(checkR.getClaims());

        final Map<String, List<String>> claims = checkR.getClaims();
        assertClaim(claims, "aud");
        assertClaim(claims, "iss");
    }

    public static void assertClaim(Map<String, List<String>> p_claims, String p_claimName) {
        final List<String> claimValueList = p_claims.get(p_claimName);
        assertTrue(claimValueList != null && !claimValueList.isEmpty());
    }
}
