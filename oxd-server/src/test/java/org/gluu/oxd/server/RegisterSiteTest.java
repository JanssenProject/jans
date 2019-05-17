package org.gluu.oxd.server;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.UpdateSiteResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class RegisterSiteTest {

    private String oxdId = null;

    @Parameters({"host", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void register(String host, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException {
        RegisterSiteResponse resp = registerSite(Tester.newClient(host), opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
        assertNotNull(resp);

        notEmpty(resp.getOxdId());

        // more specific site registration
        final RegisterSiteParams params = new RegisterSiteParams();
        //commandParams.setProtectionAccessToken(setupClient.getClientRegistrationAccessToken());
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setRedirectUris(Lists.newArrayList(redirectUrl));
        params.setAcrValues(new ArrayList<String>());
        params.setScope(Lists.newArrayList("openid", "profile"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

        resp = Tester.newClient(host).registerSite(params);
        assertNotNull(resp);
        assertNotNull(resp.getOxdId());
        oxdId = resp.getOxdId();
    }

    @Parameters({"host"})
    @Test(dependsOnMethods = {"register"})
    public void update(String host) {
        notEmpty(oxdId);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // more specific site registration
        final UpdateSiteParams params = new UpdateSiteParams();
        params.setOxdId(oxdId);
        params.setScope(Lists.newArrayList("profile"));

        UpdateSiteResponse resp = Tester.newClient(host).updateSite(Tester.getAuthorization(), params);
        assertNotNull(resp);
    }

    public static RegisterSiteResponse registerSite(ClientInterface client, String opHost, String redirectUrl) {
        return registerSite(client, opHost, redirectUrl, redirectUrl, "");
    }

    public static RegisterSiteResponse registerSite(ClientInterface client, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }

    public static RegisterSiteResponse registerWithMultipleRedirectUrls(ClientInterface client, String opHost, String authorizationRedirectUrl, String postLogoutRedirectUrl, String logoutUrl, String redirectUrls) throws IOException {
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(authorizationRedirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(",")));
        params.setTrustedClient(true);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(",")));
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;

    }
}
