package io.swagger.client.api;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponse;
import io.swagger.client.model.RegisterSiteParams;
import io.swagger.client.model.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.jwt.Jwt;

import static io.swagger.client.api.Tester.api;
import static org.testng.Assert.*;


/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 */
public class AccessTokenAsJwtTest {


    @Parameters({"opHost", "redirectUrl",  "postLogoutRedirectUrl"})
    @Test
    public void testWithAccessTokenAsJwt(String opHost, String redirectUrl, String postLogoutRedirectUrl) throws Exception {

        final DevelopersApi apiClient = api();

        final RegisterSiteParams siteParams = new io.swagger.client.model.RegisterSiteParams();
        siteParams.setOpHost(opHost);
        siteParams.setAuthorizationRedirectUri(redirectUrl);
        siteParams.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        siteParams.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        siteParams.setAccessTokenAsJwt(true);
        siteParams.setTrustedClient(true);
        siteParams.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = apiClient.registerSite(siteParams);
        assertNotNull(resp);

        final GetClientTokenParams tokenParams = new GetClientTokenParams();
        tokenParams.setOpHost(opHost);
        tokenParams.setScope(Lists.newArrayList("openid"));
        tokenParams.setClientId(resp.getClientId());
        tokenParams.setClientSecret(resp.getClientSecret());

        GetClientTokenResponse tokenResponse = apiClient.getClientToken(tokenParams);

        assertNotNull(tokenResponse);
        assertTrue(!Strings.isNullOrEmpty(tokenResponse.getAccessToken()));

        final Jwt parse = Jwt.parse(tokenResponse.getAccessToken());
        assertNotNull(parse);
        System.out.println("access token as JWT: " + tokenResponse.getAccessToken() + ", claims: " + parse.getClaims());
    }


}
