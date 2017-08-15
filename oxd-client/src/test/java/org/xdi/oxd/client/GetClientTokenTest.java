package org.xdi.oxd.client;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetAccessTokenByRefreshTokenParams;
import org.xdi.oxd.common.params.GetClientTokenParams;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.common.response.SetupClientResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/03/2017
 */

public class GetClientTokenTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void getClientToken(String host, int port, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            SetupClientResponse setup = SetupClientTest.setupClient(client, opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);

            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(opHost);
            params.setScope(Lists.newArrayList("openid"));
            params.setClientId(setup.getClientId());
            params.setClientSecret(setup.getClientSecret());

            GetClientTokenResponse resp = client.send(new Command(CommandType.GET_CLIENT_TOKEN).setParamsObject(params)).dataAsResponse(GetClientTokenResponse.class);

            assertNotNull(resp);
            notEmpty(resp.getAccessToken());
            notEmpty(resp.getRefreshToken());

            // refresh token
            String oxdId = setup.getOxdId();
            final GetAccessTokenByRefreshTokenParams refreshParams = new GetAccessTokenByRefreshTokenParams();
            refreshParams.setOxdId(oxdId);
            refreshParams.setScope(Lists.newArrayList("openid"));
            refreshParams.setRefreshToken(resp.getRefreshToken());

            GetClientTokenResponse refreshResponse = client.send(new Command(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN).setParamsObject(refreshParams)).dataAsResponse(GetClientTokenResponse.class);

            assertNotNull(refreshResponse);
            notEmpty(refreshResponse.getAccessToken());
            notEmpty(refreshResponse.getRefreshToken());

        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
