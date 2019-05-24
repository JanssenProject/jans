package io.swagger.client.api;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.model.RegisterSiteParams;
import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.UpdateSiteParams;
import io.swagger.client.model.UpdateSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.model.common.GrantType;

import java.util.ArrayList;
import java.util.Calendar;

import static io.swagger.client.api.Tester.*;
import static org.testng.Assert.*;


/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb Khan
 * @version 11/07/2018
 */

@Test
public class RegisterSiteTest {

    private String oxdId = null;

    @Parameters({"opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrls", "clientJwksUri", "accessTokenSigningAlg"})
    @Test
    public void register(String opHost, String redirectUrl, String logoutUrl, String postLogoutRedirectUrls,  String clientJwksUri, String accessTokenSigningAlg) throws Exception {
            DevelopersApi client = api();

            registerSite(client, opHost, redirectUrl, logoutUrl, postLogoutRedirectUrls, clientJwksUri, accessTokenSigningAlg);

            // more specific site registration
            final RegisterSiteParams params = new RegisterSiteParams();
            params.setOpHost(opHost);
            params.setAuthorizationRedirectUri(redirectUrl);
            params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
            params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
            params.setRedirectUris(Lists.newArrayList(redirectUrl));
            params.setAcrValues(new ArrayList<>());
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
        params.setScope(Lists.newArrayList("profile", "oxd"));

        UpdateSiteResponse resp = api().updateSite(getAuthorization(), params);
        assertNotNull(resp);
    }

    public static RegisterSiteResponse registerSite(DevelopersApi apiClient, String opHost, String redirectUrl) throws Exception {
        return registerSite(apiClient, opHost, redirectUrl, redirectUrl, "", "", "");
    }

    public static RegisterSiteResponse registerSite(DevelopersApi apiClient, String opHost, String redirectUrl, String logoutUri, String postLogoutRedirectUrls, String clientJwksUri, String accessTokenSigningAlg) throws Exception {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setClientJwksUri(clientJwksUri);
        params.setAccessTokenSigningAlg(accessTokenSigningAlg);
        final RegisterSiteResponse resp = apiClient.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }

    @Parameters({"opHost", "redirectUrl", "postLogoutRedirectUrls", "clientJwksUri"})
    @Test
    public void registerWithInvalidAlgorithm(String opHost, String redirectUrl, String postLogoutRedirectUrls, String clientJwksUri) {

        final DevelopersApi client = api();

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(""));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setClientJwksUri(clientJwksUri);
        params.setAccessTokenSigningAlg("blahBlah");

        try {
            client.registerSite(params);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);  //BAD Request
        }

    }

}
