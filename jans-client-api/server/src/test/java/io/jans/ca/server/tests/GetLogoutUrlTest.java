package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetLogoutUrlParams;
import io.jans.ca.common.response.GetLogoutUriResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.UUID;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class GetLogoutUrlTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls", "postLogoutRedirectUrl"})
    @Test
    public void test(String host, String opHost, String redirectUrls, String postLogoutRedirectUrl) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, postLogoutRedirectUrl, "", false);

        final GetLogoutUrlParams params = new GetLogoutUrlParams();
        params.setRpId(site.getRpId());
        params.setIdTokenHint("dummy_token");
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setState(UUID.randomUUID().toString());
        params.setSessionState(UUID.randomUUID().toString()); // here must be real session instead of dummy UUID

        final GetLogoutUriResponse resp = client.getLogoutUri(Tester.getAuthorization(getApiTagetURL(url), site), params.getRpId(), params);
        assertNotNull(resp);
        assertTrue(resp.getUri().contains(URLEncoder.encode(postLogoutRedirectUrl, "UTF-8")));
    }
}
