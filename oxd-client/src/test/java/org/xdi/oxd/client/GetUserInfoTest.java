package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.params.GetUserInfoParams;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.GetUserInfoResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2015
 */

public class GetUserInfoTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
            final GetTokensByCodeResponse tokens = requestTokens(client, site, userId, userSecret);

            GetUserInfoParams params = new GetUserInfoParams();
            params.setOxdId(site.getOxdId());
            params.setAccessToken(tokens.getAccessToken());

            final GetUserInfoResponse resp = client.send(new Command(CommandType.GET_USER_INFO).setParamsObject(params)).dataAsResponse(GetUserInfoResponse.class);
            assertNotNull(resp);
            notEmpty(resp.getClaims().get("sub"));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private GetTokensByCodeResponse requestTokens(CommandClient client, RegisterSiteResponse site, String userId, String userSecret) {

        final GetTokensByCodeParams commandParams = new GetTokensByCodeParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setCode(GetTokensByCodeTest.codeRequest(client, site.getOxdId(), userId, userSecret));

        final Command command = new Command(CommandType.GET_TOKENS_BY_CODE).setParamsObject(commandParams);

        final GetTokensByCodeResponse resp = client.send(command).dataAsResponse(GetTokensByCodeResponse.class);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}
