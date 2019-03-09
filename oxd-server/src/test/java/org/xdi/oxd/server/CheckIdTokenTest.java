package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.CheckIdTokenParams;
import org.xdi.oxd.common.response.CheckIdTokenResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

public class CheckIdTokenTest {

    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        String nonce = CoreUtils.secureRandomString();
        GetTokensByCodeResponse response = GetTokensByCodeTest.tokenByCode(client, site, userId, userSecret, nonce);

        final CheckIdTokenParams params = new CheckIdTokenParams();
        params.setOxdId(site.getOxdId());
        params.setIdToken(response.getIdToken());
        params.setNonce(nonce);

        final CheckIdTokenResponse checkR = client.checkIdToken(Tester.getAuthorization(), params);
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
