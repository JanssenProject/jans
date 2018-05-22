package org.xdi.oxd.server.manual;

import com.google.common.base.Strings;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/08/2016
 */

public class GoogleTest {

    private static final String HOST = "localhost";
    private static final int PORT = 8099;

    private static final String OP_HOST = "https://accounts.google.com";
    private static final String REDIRECT_URI = "https://mytestproduct.com";

    private static final String CLIENT_ID = "470707636610-pr4nn6dr82the6kuktqfr7lq4g7n9rb1.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "vJVYPI4Ybwc9a2YOesq2bRCa";

    public static void main(String[] args) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(HOST, PORT);

            final RegisterSiteResponse site = registerSite(client);
            final String authorizationUrl = getAuthorizationUrl(client, site.getOxdId());

            // after successful login get redirect with code
            // https://mytestproduct.com/?state=af0ifjsldkj&code=4/2I5U130cxs7MObKVniVseBQkHQ0JQS0p5JfZm7NgZ-M&authuser=0&session_state=02bd0461002924877bee444d9dfd9f1279e44335..ae63&prompt=consent#
//            String code = "4/2I5U130cxs7MObKVniVseBQkHQ0JQS0p5JfZm7NgZ-M";
//            String code = "4/e8WrOQDMNRllHvMGAEKAovwWMnIy7k7oxRN4Xn9JPrg";
            String code = "4/nWqDxBS-c7MDN_1Gq2WtW2L6B9KnA5rpOOYzGEdg7lM";

            final GetTokensByCodeParams commandParams = new GetTokensByCodeParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setCode(code);

            final Command command = new Command(CommandType.GET_TOKENS_BY_CODE).setParamsObject(commandParams);

            final GetTokensByCodeResponse resp = client.send(command).dataAsResponse(GetTokensByCodeResponse.class);
            System.out.println(resp);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private static String getAuthorizationUrl(CommandClient client, String oxdId) {
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(oxdId);

        final Command command = new Command(CommandType.GET_AUTHORIZATION_URL);
        command.setParamsObject(commandParams);

        final GetAuthorizationUrlResponse resp = client.send(command).dataAsResponse(GetAuthorizationUrlResponse.class);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        System.out.println("Authorization url: " + resp.getAuthorizationUrl());
        return resp.getAuthorizationUrl();
    }

    public static RegisterSiteResponse registerSite(CommandClient client) {

        final RegisterSiteParams commandParams = new RegisterSiteParams();
        commandParams.setOpHost(OP_HOST);
        commandParams.setAuthorizationRedirectUri(REDIRECT_URI);
        commandParams.setClientId(CLIENT_ID);
        commandParams.setClientSecret(CLIENT_SECRET);

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(commandParams);

        final RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
