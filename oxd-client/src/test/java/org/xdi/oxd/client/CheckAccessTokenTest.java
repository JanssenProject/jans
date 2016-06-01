package org.xdi.oxd.client;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.CheckAccessTokenParams;
import org.xdi.oxd.common.response.CheckAccessTokenResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenTest {
    @Parameters({"host", "port", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret", "opHost"})
    @Test
    public void test(String host, int port, String redirectUrl,
                     String clientId, String clientSecret, String userId, String userSecret, String opHost) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final TokenResponse tokenResponse = TestUtils.obtainAccessToken(userId, userSecret,
                    clientId, clientSecret, redirectUrl, opHost);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            final CheckAccessTokenParams params = new CheckAccessTokenParams();
            params.setAccessToken(tokenResponse.getAccessToken());
            params.setIdToken(tokenResponse.getIdToken());
            params.setOxdId(site.getOxdId());

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
