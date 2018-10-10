package io.swagger.client.api;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.model.RegisterSiteParams;
import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.UpdateSiteParams;
import io.swagger.client.model.UpdateSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.GrantType;

import java.util.ArrayList;
import java.util.Calendar;

import static io.swagger.client.api.Tester.*;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb Khan
 * @version 07/26/2018
 */

@Test
public class RegisterSiteTest {

    private String oxdId = null;

    @Parameters({"opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void register(String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws Exception {

        DevelopersApi client = api();

        registerSite(client, opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);

        // more specific site registration
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setRedirectUris(Lists.newArrayList(redirectUrl));
        params.setAcrValues(new ArrayList<String>());
        params.setScope(Lists.newArrayList("openid", "profile"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertNotNull(resp.getOxdId());
        oxdId = resp.getOxdId();
    }

    @Test(dependsOnMethods = {"register"})
    public void update() throws Exception {
        notEmpty(oxdId);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // more specific site registration
        final UpdateSiteParams params = new UpdateSiteParams();
        params.setOxdId(oxdId);
        params.setClientSecretExpiresAt(calendar.getTime().getTime());
        params.setScope(Lists.newArrayList("profile", "oxd"));

        UpdateSiteResponse resp = api().updateSite(getAuthorization(), params);
        assertNotNull(resp);
    }

    public static RegisterSiteResponse registerSite(DevelopersApi apiClient, String opHost, String redirectUrl) throws Exception {
        return registerSite(apiClient, opHost, redirectUrl, redirectUrl, "");
    }

    public static RegisterSiteResponse registerSite(DevelopersApi apiClient, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) throws Exception {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = apiClient.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
