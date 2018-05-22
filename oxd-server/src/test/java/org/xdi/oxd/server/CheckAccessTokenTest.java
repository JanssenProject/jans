package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.CheckAccessTokenParams;
import org.xdi.oxd.common.response.CheckAccessTokenResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenTest {
    @Parameters({"host", "port", "redirectUrl", "userId", "userSecret", "opHost"})
    @Test
    public void test(String host, int port, String redirectUrl, String userId, String userSecret, String opHost) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            String nonce = CoreUtils.secureRandomString();
            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
            GetTokensByCodeResponse response = GetTokensByCodeTest.tokenByCode(client, site, userId, userSecret, nonce);

            final CheckAccessTokenParams params = new CheckAccessTokenParams();
            params.setAccessToken(response.getAccessToken());
            params.setIdToken(response.getIdToken());
            params.setOxdId(site.getOxdId());

            final Command checkIdTokenCommand = new Command(CommandType.CHECK_ACCESS_TOKEN);
            checkIdTokenCommand.setParamsObject(params);
            final CommandResponse r = client.send(checkIdTokenCommand);
            assertNotNull(r);

            final CheckAccessTokenResponse checkR = r.dataAsResponse(CheckAccessTokenResponse.class);
            assertNotNull(checkR);
            assertTrue(checkR.isActive());
            assertNotNull(checkR.getExpiresAt());
            assertNotNull(checkR.getIssuedAt());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

}
