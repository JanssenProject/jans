package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.CheckAccessTokenParams;
import org.xdi.oxd.common.response.CheckAccessTokenResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenTest {
    @Parameters({"host", "redirectUrl", "userId", "userSecret", "opHost"})
    @Test
    public void test(String host, String redirectUrl, String userId, String userSecret, String opHost) {

        ClientInterface client = Tester.newClient(host);
        String nonce = CoreUtils.secureRandomString();
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        GetTokensByCodeResponse response = GetTokensByCodeTest.tokenByCode(client, site, userId, userSecret, nonce);

        final CheckAccessTokenParams params = new CheckAccessTokenParams();
        params.setAccessToken(response.getAccessToken());
        params.setIdToken(response.getIdToken());
        params.setOxdId(site.getOxdId());

        final CheckAccessTokenResponse checkR = client.checkAccessToken(Tester.getAuthorization(), params);
        assertNotNull(checkR);
        assertTrue(checkR.isActive());
        assertNotNull(checkR.getExpiresAt());
        assertNotNull(checkR.getIssuedAt());
    }

}
