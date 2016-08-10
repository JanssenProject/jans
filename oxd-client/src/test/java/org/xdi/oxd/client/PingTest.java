package org.xdi.oxd.client;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class PingTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void register(String host, int port, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse resp = RegisterSiteTest.registerSite(client, opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
            assertNotNull(resp);

            notEmpty(resp.getOxdId());

            // more specific site registration
            final RegisterSiteParams commandParams = new RegisterSiteParams();
            commandParams.setOpHost(opHost);
            commandParams.setAuthorizationRedirectUri(redirectUrl);
            commandParams.setPostLogoutRedirectUri(postLogoutRedirectUrl);
            commandParams.setClientLogoutUri(Lists.newArrayList(logoutUrl));
            commandParams.setRedirectUris(Arrays.asList(redirectUrl));
            commandParams.setAcrValues(new ArrayList<String>());
            commandParams.setScope(Lists.newArrayList("openid", "profile"));
            commandParams.setGrantType(Lists.newArrayList("authorization_code"));
            commandParams.setResponseTypes(Lists.newArrayList("code"));

            final Command command = new Command(CommandType.REGISTER_SITE);
            command.setParamsObject(commandParams);

            resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
            assertNotNull(resp);
            assertNotNull(resp.getOxdId());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
