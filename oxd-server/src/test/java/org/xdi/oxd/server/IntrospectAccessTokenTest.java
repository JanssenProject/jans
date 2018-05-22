package org.xdi.oxd.server;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetClientTokenParams;
import org.xdi.oxd.common.params.IntrospectAccessTokenParams;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.common.response.SetupClientResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author yuriyz
 */
public class IntrospectAccessTokenTest {

    @Parameters({"host", "port", "opHost", "redirectUrl"})
    @Test
    public void introspectAccessToken(String host, int port, String opHost, String redirectUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            SetupClientResponse setupResponse = SetupClientTest.setupClient(client, opHost, redirectUrl);

            final GetClientTokenParams getTokenParams = new GetClientTokenParams();
            getTokenParams.setOpHost(opHost);
            getTokenParams.setScope(Lists.newArrayList("openid"));
            getTokenParams.setClientId(setupResponse.getClientId());
            getTokenParams.setClientSecret(setupResponse.getClientSecret());

            GetClientTokenResponse tokenResponse = client.send(new Command(CommandType.GET_CLIENT_TOKEN).setParamsObject(getTokenParams)).dataAsResponse(GetClientTokenResponse.class);

            assertNotNull(tokenResponse);
            notEmpty(tokenResponse.getAccessToken());

            IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
            introspectParams.setOxdId(setupResponse.getSetupClientOxdId());
            introspectParams.setAccessToken(tokenResponse.getAccessToken());

            IntrospectionResponse introspectionResponse = client.send(new Command(CommandType.INTROSPECT_ACCESS_TOKEN).setParamsObject(introspectParams)).dataAsResponse(IntrospectionResponse.class);
            assertNotNull(introspectionResponse);
            Assert.assertTrue(introspectionResponse.isActive());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
