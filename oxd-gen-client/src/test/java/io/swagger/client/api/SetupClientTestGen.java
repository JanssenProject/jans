package io.swagger.client.api;

import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.model.RegisterSiteParams;
import io.swagger.client.model.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.GrantType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static io.swagger.client.api.Tester.api;
import static org.junit.Assert.assertNotNull;


/**
 * @author yuriyz
 */
public class SetupClientTestGen {

    @Parameters({"host", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void setupClient(String host, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException, ApiException {
        RegisterSiteResponse resp = setupClient(opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
        assertResponse(resp);

        // more specific client setup
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(new ArrayList<>(Collections.singletonList(logoutUrl)));
        params.setRedirectUris(Arrays.asList(redirectUrl));
        params.setAcrValues(new ArrayList<String>());
        params.setScope(new ArrayList<>(Arrays.asList("openid", "profile")));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

        resp = api().registerSite(params);
        assertResponse(resp);
    }

    public static void assertResponse(RegisterSiteResponse resp) {
        assertNotNull(resp);

        Tester.notEmpty(resp.getData().getClientId());
        Tester.notEmpty(resp.getData().getClientSecret());
    }

    public static RegisterSiteResponse setupClient(String opHost, String redirectUrl) throws ApiException {
        return setupClient(opHost, redirectUrl, redirectUrl, "");
    }

    public static RegisterSiteResponse setupClient(String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) throws ApiException {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        RegisterSiteResponse resp = api().registerSite(params);

        assertResponse(resp);
        return resp;
    }

}
