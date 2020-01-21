package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.common.params.GetClientTokenParams;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.response.GetClientTokenResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.gluu.oxd.server.SetupClientTest.assertResponse;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AccessTokenAsJwtTest {

    @Parameters({"host", "opHost", "redirectUrls", "postLogoutRedirectUrls"})
    @Test
    public void getClientToken(String host, String opHost, String redirectUrls, String postLogoutRedirectUrls) throws InvalidJwtException {
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setAccessTokenAsJwt(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = org.gluu.oxd.server.Tester.newClient(host).registerSite(params);
        assertResponse(resp);

        final GetClientTokenParams tokenParams = new GetClientTokenParams();
        tokenParams.setOpHost(opHost);
        tokenParams.setScope(Lists.newArrayList("openid"));
        tokenParams.setClientId(resp.getClientId());
        tokenParams.setClientSecret(resp.getClientSecret());

        GetClientTokenResponse tokenResponse = org.gluu.oxd.server.Tester.newClient(host).getClientToken(tokenParams);

        assertNotNull(tokenResponse);
        notEmpty(tokenResponse.getAccessToken());

        final Jwt parse = Jwt.parse(tokenResponse.getAccessToken());
        assertNotNull(parse);
        System.out.println("access token as JWT: " + tokenResponse.getAccessToken() + ", claims: " + parse.getClaims());
    }
}
