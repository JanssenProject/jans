package org.gluu.oxd.server;

import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.AuthorizationCodeFlowParams;
import org.gluu.oxd.common.response.AuthorizationCodeFlowResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.gluu.oxd.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

public class AuthorizationCodeFlowTest {

    @Parameters({"host", "opHost", "redirectUrls", "clientId", "clientSecret", "userId", "userSecret"})
    @Test(enabled = false)
    public void test(String host, String opHost, String redirectUrls, String clientId, String clientSecret, String userId, String userSecret) {

        ClientInterface client = org.gluu.oxd.server.Tester.newClient(host);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        final AuthorizationCodeFlowParams params = new AuthorizationCodeFlowParams();
        params.setOxdId(site.getOxdId());
        params.setClientId(clientId);
        params.setClientSecret(clientSecret);
        params.setNonce(UUID.randomUUID().toString());
        params.setRedirectUrl(redirectUrls.split(" ")[0]);
        params.setScope("openid");
        params.setUserId(userId);
        params.setUserSecret(userSecret);

        final AuthorizationCodeFlowResponse resp = client.authorizationCodeFlow(Tester.getAuthorization(site), null, params);
        assertNotNull(resp);

        notEmpty(resp.getAccessToken());
        notEmpty(resp.getAuthorizationCode());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        notEmpty(resp.getScope());
    }
}
