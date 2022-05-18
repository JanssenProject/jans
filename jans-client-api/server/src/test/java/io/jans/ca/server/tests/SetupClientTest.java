package io.jans.ca.server.tests;

import com.google.common.collect.Lists;
import io.jans.as.model.common.GrantType;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.ArrayList;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/03/2017
 */

public class SetupClientTest extends BaseTest {

    @ArquillianResource
    private static URI url;

    @Parameters({"host", "opHost", "redirectUrls", "logoutUrl", "postLogoutRedirectUrls"})
    @Test
    public void setupClient(String host, String opHost, String redirectUrls, String logoutUrl, String postLogoutRedirectUrls) {
        String hostTargetUrl = getApiTagetURL(url);
        RegisterSiteResponse resp = setupClient(Tester.newClient(hostTargetUrl), opHost, redirectUrls, postLogoutRedirectUrls, logoutUrl);
        assertResponse(resp);

        // more specific client setup
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUri(logoutUrl);
        params.setAcrValues(new ArrayList<String>());
        params.setScope(Lists.newArrayList("openid", "profile"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

        resp = Tester.newClient(hostTargetUrl).registerSite(params);
        assertResponse(resp);
    }

    public static void assertResponse(RegisterSiteResponse resp) {
        assertNotNull(resp);

        notEmpty(resp.getClientId());
        notEmpty(resp.getClientSecret());
        notEmpty(resp.getRpId());
    }

    public static RegisterSiteResponse setupClient(ClientInterface client, String opHost, String redirectUrls) {
        return setupClient(client, opHost, redirectUrls, redirectUrls, "");
    }

    public static RegisterSiteResponse setupClient(ClientInterface client, String opHost, String redirectUrls, String postLogoutRedirectUrls, String logoutUri) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUri(logoutUri);
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "jans_client_api"));
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertResponse(resp);
        return resp;
    }
}
