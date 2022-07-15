package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.GetTokensByCodeResponse2;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.params.CheckAccessTokenParams;
import io.jans.ca.common.response.CheckAccessTokenResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenTest extends BaseTest {
    @ArquillianResource
    URI url;

    @Parameters({"host", "redirectUrls", "userId", "userSecret", "opHost"})
    @Test
    public void test(String host, String redirectUrls, String userId, String userSecret, String opHost) {
        String hostTargetURL = getApiTagetURL(url);

        ClientInterface client = Tester.newClient(hostTargetURL);
        String nonce = CoreUtils.secureRandomString();
        String state = CoreUtils.secureRandomString();
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        GetTokensByCodeResponse2 response = GetTokensByCodeTest.tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, nonce, state);

        final CheckAccessTokenParams params = new CheckAccessTokenParams();
        params.setAccessToken(response.getAccessToken());
        params.setIdToken(response.getIdToken());
        params.setRpId(site.getRpId());

        String strAuthorization = Tester.getAuthorization(hostTargetURL, site);
        final CheckAccessTokenResponse checkR = client.checkAccessToken(strAuthorization, params.getRpId(), params);
        assertNotNull(checkR);
        assertTrue(checkR.isActive());
        assertNotNull(checkR.getExpiresAt());
        assertNotNull(checkR.getIssuedAt());
    }

}
