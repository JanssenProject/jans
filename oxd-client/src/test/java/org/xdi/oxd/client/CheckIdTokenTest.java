package org.xdi.oxd.client;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.CheckIdTokenParams;
import org.xdi.oxd.common.params.DiscoveryParams;
import org.xdi.oxd.common.response.CheckIdTokenResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

public class CheckIdTokenTest {

    @Parameters({"host", "port", "discoveryUrl", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String discoveryUrl, String redirectUrl,
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

            final CheckIdTokenParams params = new CheckIdTokenParams();
            params.setDiscoveryUrl(discoveryUrl);
            params.setIdToken(tokenResponse.getIdToken());

            final Command checkIdTokenCommand = new Command(CommandType.CHECK_ID_TOKEN);
            checkIdTokenCommand.setParamsObject(params);
            final CommandResponse r = client.send(checkIdTokenCommand);
            Assert.assertNotNull(r);

            final CheckIdTokenResponse checkR = r.dataAsResponse(CheckIdTokenResponse.class);
            Assert.assertNotNull(checkR);
            Assert.assertTrue(checkR.isActive());
            Assert.assertNotNull(checkR.getExpiresAt());
            Assert.assertNotNull(checkR.getIssuedAt());
            Assert.assertNotNull(checkR.getClaims());

            final Map<String, List<String>> claims = checkR.getClaims();
            assertClaim(claims, "aud");
            assertClaim(claims, "iss");
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static void assertClaim(Map<String, List<String>> p_claims, String p_claimName) {
        final List<String> claimValueList = p_claims.get(p_claimName);
        Assert.assertTrue(claimValueList != null && !claimValueList.isEmpty());
    }
}
