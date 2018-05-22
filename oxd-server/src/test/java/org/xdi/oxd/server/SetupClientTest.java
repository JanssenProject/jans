package org.xdi.oxd.server;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.SetupClientParams;
import org.xdi.oxd.common.response.SetupClientResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/03/2017
 */

public class SetupClientTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void setupClient(String host, int port, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            SetupClientResponse resp = setupClient(client, opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
            assertResponse(resp);

            // more specific client setup
            final SetupClientParams commandParams = new SetupClientParams();
            commandParams.setOpHost(opHost);
            commandParams.setAuthorizationRedirectUri(redirectUrl);
            commandParams.setPostLogoutRedirectUri(postLogoutRedirectUrl);
            commandParams.setClientFrontchannelLogoutUri(Lists.newArrayList(logoutUrl));
            commandParams.setRedirectUris(Arrays.asList(redirectUrl));
            commandParams.setAcrValues(new ArrayList<String>());
            commandParams.setScope(Lists.newArrayList("openid", "profile"));
            commandParams.setGrantType(Lists.newArrayList("authorization_code"));
            commandParams.setResponseTypes(Lists.newArrayList("code"));

            final Command command = new Command(CommandType.SETUP_CLIENT);
            command.setParamsObject(commandParams);

            resp = client.send(command).dataAsResponse(SetupClientResponse.class);
            assertResponse(resp);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static void assertResponse(SetupClientResponse resp) {
        assertNotNull(resp);

        notEmpty(resp.getOxdId());
        notEmpty(resp.getClientId());
        notEmpty(resp.getClientSecret());
    }

    public static SetupClientResponse setupClient(CommandClient client, String opHost, String redirectUrl) {
        return setupClient(client, opHost, redirectUrl, redirectUrl, "");
    }

    public static SetupClientResponse setupClient(CommandClient client, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) {

        final SetupClientParams params = new SetupClientParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUri(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantType(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setOxdRpProgrammingLanguage("java");

        final Command command = new Command(CommandType.SETUP_CLIENT);
        command.setParamsObject(params);

        final SetupClientResponse resp = client.send(command).dataAsResponse(SetupClientResponse.class);
        assertResponse(resp);
        return resp;
    }
}
