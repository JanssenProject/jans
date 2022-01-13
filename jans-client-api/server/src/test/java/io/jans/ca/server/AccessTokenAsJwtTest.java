package io.jans.ca.server;

import com.google.common.collect.Lists;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.common.params.GetClientTokenParams;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.response.GetClientTokenResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static io.jans.ca.server.SetupClientTest.assertResponse;
import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;

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

        final RegisterSiteResponse resp = Tester.newClient(host).registerSite(params);
        assertResponse(resp);

        final GetClientTokenParams tokenParams = new GetClientTokenParams();
        tokenParams.setOpHost(opHost);
        tokenParams.setScope(Lists.newArrayList("openid"));
        tokenParams.setClientId(resp.getClientId());
        tokenParams.setClientSecret(resp.getClientSecret());

        GetClientTokenResponse tokenResponse = Tester.newClient(host).getClientToken(tokenParams);

        assertNotNull(tokenResponse);
        notEmpty(tokenResponse.getAccessToken());

        final Jwt parse = Jwt.parse(tokenResponse.getAccessToken());
        assertNotNull(parse);
        System.out.println("access token as JWT: " + tokenResponse.getAccessToken() + ", claims: " + parse.getClaims());
    }
}
