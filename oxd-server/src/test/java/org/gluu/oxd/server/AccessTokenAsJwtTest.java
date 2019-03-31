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

import static junit.framework.Assert.assertNotNull;
import static org.gluu.oxd.server.SetupClientTest.assertResponse;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AccessTokenAsJwtTest {

    @Parameters({"host", "opHost", "redirectUrl"})
    @Test
    public void getClientToken(String host, String opHost, String redirectUrl) throws InvalidJwtException {
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(redirectUrl);
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setAccessTokenAsJwt(true);
        params.setTrustedClient(true);
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
