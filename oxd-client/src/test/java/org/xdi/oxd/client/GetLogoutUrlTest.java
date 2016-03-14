package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetLogoutUrlParams;
import org.xdi.oxd.common.response.LogoutResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.util.UUID;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Dummy test because we can't check real session management which is handled via browser cookies.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/11/2015
 */

public class GetLogoutUrlTest {

    @Parameters({"host", "port", "redirectUrl", "userId", "userSecret", "postLogoutRedirectUrl"})
    @Test
    public void test(String host, int port, String redirectUrl, String userId, String userSecret, String postLogoutRedirectUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, redirectUrl, postLogoutRedirectUrl, "");
//
//            GetTokensByCodeTest.tokenByCode(client, site, redirectUrl, userId, userSecret);

            final GetLogoutUrlParams commandParams = new GetLogoutUrlParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setIdTokenHint("dummy_token");
            commandParams.setPostLogoutRedirectUri(postLogoutRedirectUrl);
            commandParams.setState(UUID.randomUUID().toString());
            commandParams.setSessionState(UUID.randomUUID().toString()); // here must be real session instead of dummy UUID

            final Command command = new Command(CommandType.GET_LOGOUT_URI).setParamsObject(commandParams);

            final LogoutResponse resp = client.send(command).dataAsResponse(LogoutResponse.class);
            assertNotNull(resp);
            assertTrue(resp.getUri().contains(postLogoutRedirectUrl));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
