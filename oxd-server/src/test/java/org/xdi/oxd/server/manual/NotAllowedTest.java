package org.xdi.oxd.server.manual;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.client.UmaFullTest;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsProtectResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/11/2016
 */

public class NotAllowedTest {

    private static final String HOST = "localhost";
    private static final int PORT = 8099;

    private static final String rsProtect = "{\"resources\":[{\"path\":\"/scim\",\"conditions\":[{\"httpMethods\":[\"GET\"],\"scopes\":[\"https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1\"],\"ticketScopes\":[\"https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1\"]}]}]}";

    public static void main(String[] args) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(HOST, PORT);

            RegisterSiteResponse site = registerSite(client);

            final RsProtectParams commandParams = new RsProtectParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setResources(UmaFullTest.resourceList(rsProtect).getResources());

            final Command command = new Command(CommandType.RS_PROTECT)
                    .setParamsObject(commandParams);

            final RsProtectResponse resp = client.send(command).dataAsResponse(RsProtectResponse.class);
            assertNotNull(resp);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RegisterSiteResponse registerSite(CommandClient client) {

        final RegisterSiteParams commandParams = new RegisterSiteParams();
        commandParams.setOpHost("https://ce-dev.gluu.org");
        commandParams.setAuthorizationRedirectUri("https://192.168.200.58:5053");
        commandParams.setScope(Lists.newArrayList("openid", "profile", "email", "address", "clientinfo", "mobile_phone", "phone", "uma_protection"));
        commandParams.setPostLogoutRedirectUri("https://192.168.200.58:5053");
        commandParams.setClientFrontchannelLogoutUri(Lists.newArrayList("https://192.168.200.58:5053/logout"));
        commandParams.setAcrValues(Lists.newArrayList("gplus", "basic", "duo", "u2f"));
        commandParams.setGrantType(Lists.newArrayList("authorization_code"));

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(commandParams);

        final RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
