package org.xdi.oxd.server;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.GetClientTokenParams;
import org.xdi.oxd.common.params.IntrospectAccessTokenParams;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.common.response.SetupClientResponse;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author yuriyz
 */
public class IntrospectAccessTokenTest {

    @Parameters({"host", "opHost", "redirectUrl"})
    @Test
    public void introspectAccessToken(String host, String opHost, String redirectUrl) {

        ClientInterface client = Tester.newClient(host);

        SetupClientResponse setupResponse = SetupClientTest.setupClient(client, opHost, redirectUrl);

        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid"));
        params.setClientId(setupResponse.getClientId());
        params.setClientSecret(setupResponse.getClientSecret());

        GetClientTokenResponse tokenResponse = client.getClientToken(params).dataAsResponse(GetClientTokenResponse.class);

        assertNotNull(tokenResponse);
        notEmpty(tokenResponse.getAccessToken());

        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setOxdId(setupResponse.getSetupClientOxdId());
        introspectParams.setAccessToken(tokenResponse.getAccessToken());

        IntrospectionResponse introspectionResponse = client.introspectAccessToken("Bearer " + tokenResponse.getAccessToken(), introspectParams).dataAsResponse(IntrospectionResponse.class);
        assertNotNull(introspectionResponse);
        Assert.assertTrue(introspectionResponse.isActive());

    }
}
