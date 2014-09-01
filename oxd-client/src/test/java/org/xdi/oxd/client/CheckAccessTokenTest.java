package org.xdi.oxd.client;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.CheckAccessTokenParams;
import org.xdi.oxd.common.params.DiscoveryParams;
import org.xdi.oxd.common.response.CheckAccessTokenResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenTest {
    @Parameters({"host", "port", "discoveryUrl", "umaDiscoveryUrl", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String discoveryUrl, String umaDiscoveryUrl, String redirectUrl,
                     String clientId, String clientSecret, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final Command command = new Command(CommandType.DISCOVERY);
            command.setParamsObject(new DiscoveryParams(discoveryUrl));
            final CommandResponse response = client.send(command);
            Assert.assertNotNull(response);

            final String authorizationEndpoint = response.getData().get("authorization_endpoint").asText();
            final String tokenEndpoint = response.getData().get("token_endpoint").asText();

            final TokenResponse tokenResponse = TestUtils.obtainAccessToken(userId, userSecret,
                    clientId, clientSecret, redirectUrl, authorizationEndpoint, tokenEndpoint);

            final CheckAccessTokenParams params = new CheckAccessTokenParams();
            params.setDiscoveryUrl(discoveryUrl);
            params.setAccessToken(tokenResponse.getAccessToken());
            params.setIdToken(tokenResponse.getIdToken());

            final Command checkIdTokenCommand = new Command(CommandType.CHECK_ACCESS_TOKEN);
            checkIdTokenCommand.setParamsObject(params);
            final CommandResponse r = client.send(checkIdTokenCommand);
            Assert.assertNotNull(r);

            final CheckAccessTokenResponse checkR = r.dataAsResponse(CheckAccessTokenResponse.class);
            Assert.assertNotNull(checkR);
            Assert.assertTrue(checkR.isActive());
            Assert.assertNotNull(checkR.getExpiresAt());
            Assert.assertNotNull(checkR.getIssuedAt());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

}
