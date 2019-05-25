package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertNotNull;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/03/2017
 */

public class SetupClientTest {

    @Parameters({"host", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrls"})
    @Test
    public void setupClient(String host, String opHost, String redirectUrl, String logoutUrl, String postLogoutRedirectUrls) throws IOException {
        RegisterSiteResponse resp = setupClient(Tester.newClient(host), opHost, redirectUrl, postLogoutRedirectUrls, logoutUrl);
        assertResponse(resp);

        // more specific client setup
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setRedirectUris(Arrays.asList(redirectUrl));
        params.setAcrValues(new ArrayList<String>());
        params.setScope(Lists.newArrayList("openid", "profile"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

        resp = Tester.newClient(host).registerSite(params);
        assertResponse(resp);
    }

    public static void assertResponse(RegisterSiteResponse resp) {
        assertNotNull(resp);

        notEmpty(resp.getClientId());
        notEmpty(resp.getClientSecret());
        notEmpty(resp.getOxdId());
    }

    public static RegisterSiteResponse setupClient(ClientInterface client, String opHost, String redirectUrl) {
        return setupClient(client, opHost, redirectUrl, redirectUrl, "");
    }

    public static RegisterSiteResponse setupClient(ClientInterface client, String opHost, String redirectUrl, String postLogoutRedirectUrls, String logoutUri) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertResponse(resp);
        return resp;
    }
}
