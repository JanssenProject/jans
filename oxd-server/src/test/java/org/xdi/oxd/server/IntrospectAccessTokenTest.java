package org.xdi.oxd.server;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.GetClientTokenParams;
import org.xdi.oxd.common.params.IntrospectAccessTokenParams;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.xdi.oxd.server.TestUtils.notEmpty;

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

        GetClientTokenResponse tokenResponse = client.getClientToken(params).dataAsResponse(GetClientTokenResponse.class);

        assertNotNull(tokenResponse);
        notEmpty(tokenResponse.getAccessToken());

        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setOxdId(setupResponse.getOxdId());
        introspectParams.setAccessToken(tokenResponse.getAccessToken());

        IntrospectionResponse introspectionResponse = client.introspectAccessToken("Bearer " + tokenResponse.getAccessToken(), introspectParams).dataAsResponse(IntrospectionResponse.class);

        assertNotNull(introspectionResponse);
        assertTrue(introspectionResponse.isActive());
        assertNotNull(introspectionResponse.getIssuedAt());
        assertNotNull(introspectionResponse.getExpiresAt());
        assertTrue(introspectionResponse.getExpiresAt() >= introspectionResponse.getIssuedAt());
    }
}
