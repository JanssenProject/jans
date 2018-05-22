package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.AuthorizationCodeFlowParams;
import org.xdi.oxd.common.response.AuthorizationCodeFlowResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.util.UUID;

import static org.testng.AssertJUnit.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

public class AuthorizationCodeFlowTest {

    @Parameters({"host", "opHost", "redirectUrl", "clientId", "clientSecret", "userId", "userSecret"})
    @Test(enabled = false)
    public void test(String host, String opHost, String redirectUrl, String clientId, String clientSecret, String userId, String userSecret) {

        ClientInterface client = Tester.newClient(host);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        final AuthorizationCodeFlowParams params = new AuthorizationCodeFlowParams();
        params.setOxdId(site.getOxdId());
        params.setClientId(clientId);
        params.setClientSecret(clientSecret);
        params.setNonce(UUID.randomUUID().toString());
        params.setRedirectUrl(redirectUrl);
        params.setScope("openid");
        params.setUserId(userId);
        params.setUserSecret(userSecret);

        final AuthorizationCodeFlowResponse resp = client.authorizationCodeFlow(Tester.getAuthorization(), params).dataAsResponse(AuthorizationCodeFlowResponse.class);
        assertNotNull(resp);

        notEmpty(resp.getAccessToken());
        notEmpty(resp.getAuthorizationCode());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        notEmpty(resp.getScope());
    }
}
