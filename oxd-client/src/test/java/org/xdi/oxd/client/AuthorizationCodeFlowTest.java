package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.AuthorizationCodeFlowParams;
import org.xdi.oxd.common.response.AuthorizationCodeFlowResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.util.UUID;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

public class AuthorizationCodeFlowTest {

    @Parameters({"host", "port", "opHost", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl,
                     String clientId, String clientSecret, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            final AuthorizationCodeFlowParams commandParams = new AuthorizationCodeFlowParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setClientId(clientId);
            commandParams.setClientSecret(clientSecret);
            commandParams.setNonce(UUID.randomUUID().toString());
            commandParams.setRedirectUrl(redirectUrl);
            commandParams.setScope("openid");
            commandParams.setUserId(userId);
            commandParams.setUserSecret(userSecret);

            final Command command = new Command(CommandType.AUTHORIZATION_CODE_FLOW);
            command.setParamsObject(commandParams);

            final AuthorizationCodeFlowResponse resp = client.send(command).dataAsResponse(AuthorizationCodeFlowResponse.class);
            assertNotNull(resp);

            notEmpty(resp.getAccessToken());
            notEmpty(resp.getAuthorizationCode());
            notEmpty(resp.getIdToken());
            notEmpty(resp.getRefreshToken());
            notEmpty(resp.getScope());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
