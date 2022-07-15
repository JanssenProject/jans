package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.AuthorizationCodeFlowParams;
import io.jans.ca.common.response.AuthorizationCodeFlowResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.TestUtils;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.UUID;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

public class AuthorizationCodeFlowTest extends BaseTest {

    @ArquillianResource
    URI url;

    @Parameters({"host", "opHost", "redirectUrls", "clientId", "clientSecret", "userId", "userSecret"})
    @Test(enabled = false)
    public void test(String host, String opHost, String redirectUrls, String clientId, String clientSecret, String userId, String userSecret) {
        String hostTargetURL = getApiTagetURL(url);

        ClientInterface client = Tester.newClient(hostTargetURL);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        final AuthorizationCodeFlowParams params = new AuthorizationCodeFlowParams();
        params.setRpId(site.getRpId());
        params.setClientId(clientId);
        params.setClientSecret(clientSecret);
        params.setNonce(UUID.randomUUID().toString());
        params.setRedirectUrl(redirectUrls.split(" ")[0]);
        params.setScope("openid");
        params.setUserId(userId);
        params.setUserSecret(userSecret);

        String strAuthorization = Tester.getAuthorization(hostTargetURL, site);
        final AuthorizationCodeFlowResponse resp = client.authorizationCodeFlow(strAuthorization, params.getRpId(), params);
        assertNotNull(resp);

        TestUtils.notEmpty(resp.getAccessToken());
        TestUtils.notEmpty(resp.getAuthorizationCode());
        TestUtils.notEmpty(resp.getIdToken());
        TestUtils.notEmpty(resp.getRefreshToken());
        TestUtils.notEmpty(resp.getScope());
    }
}
