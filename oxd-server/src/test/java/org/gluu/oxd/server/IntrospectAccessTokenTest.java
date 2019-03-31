package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetClientTokenParams;
import org.gluu.oxd.common.params.IntrospectAccessTokenParams;
import org.gluu.oxd.common.response.GetClientTokenResponse;
import org.gluu.oxd.common.response.IntrospectAccessTokenResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author yuriyz
 */
public class IntrospectAccessTokenTest {

    @Parameters({"host", "opHost", "redirectUrl"})
    @Test
    public void introspectAccessToken(String host, String opHost, String redirectUrl) {

        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse setupResponse = SetupClientTest.setupClient(client, opHost, redirectUrl);

        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid"));
        params.setClientId(setupResponse.getClientId());
        params.setClientSecret(setupResponse.getClientSecret());

        GetClientTokenResponse tokenResponse = client.getClientToken(params);

        assertNotNull(tokenResponse);
        notEmpty(tokenResponse.getAccessToken());

        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setOxdId(setupResponse.getOxdId());
        introspectParams.setAccessToken(tokenResponse.getAccessToken());

        IntrospectAccessTokenResponse introspectionResponse = client.introspectAccessToken("Bearer " + tokenResponse.getAccessToken(), introspectParams);

        assertNotNull(introspectionResponse);
        assertTrue(introspectionResponse.isActive());
        assertNotNull(introspectionResponse.getIssuedAt());
        assertNotNull(introspectionResponse.getExpiresAt());
        assertTrue(introspectionResponse.getExpiresAt() >= introspectionResponse.getIssuedAt());
    }
}
