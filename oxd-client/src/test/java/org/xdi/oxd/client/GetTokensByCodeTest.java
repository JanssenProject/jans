package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetAuthorizationCodeParams;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.response.GetAuthorizationCodeResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetTokensByCodeTest {

    @Parameters({"host", "port", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String redirectUrl, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, redirectUrl);

            final GetTokensByCodeParams commandParams = new GetTokensByCodeParams();
            commandParams.setOxdId(site.getSiteId());
            commandParams.setCode(codeRequest(client, site.getSiteId(), userId, userSecret));
//               commandParams.setState();

            final Command command = new Command(CommandType.GET_TOKENS_BY_CODE).setParamsObject(commandParams);

            final GetTokensByCodeResponse resp = client.send(command).dataAsResponse(GetTokensByCodeResponse.class);
            assertNotNull(resp);
//            notEmpty(resp.getAuthorizationUrl());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private String codeRequest(CommandClient client, String siteId, String userId, String userSecret) {
        GetAuthorizationCodeParams params = new GetAuthorizationCodeParams();
        params.setOxdId(siteId);
        params.setUsername(userId);
        params.setPassword(userSecret);

        final Command command = new Command(CommandType.GET_AUTHORIZATION_CODE).setParamsObject(params);
        return client.send(command).dataAsResponse(GetAuthorizationCodeResponse.class).getCode();
    }

}
