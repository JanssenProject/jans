package org.gluu.oxd.server.manual;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.client.RsProtectParams2;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.CommandType;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RsProtectResponse;
import org.gluu.oxd.server.Jackson2;
import org.gluu.oxd.server.Tester;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/11/2016
 */

public class NotAllowedTest {

    private static final String HOST = "http://localhost:8084";

    private static final String rsProtect = "{\"resources\":[{\"path\":\"/scim\",\"conditions\":[{\"httpMethods\":[\"GET\"],\"scopes\":[\"https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1\"],\"ticketScopes\":[\"https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1\"]}]}]}";

    public static void main(String[] args) throws IOException {

        ClientInterface client = Tester.newClient(HOST);

        RegisterSiteResponse site = registerSite(client);

        final RsProtectParams2 params = new RsProtectParams2();
        params.setOxdId(site.getOxdId());
        params.setResources(Jackson2.createJsonMapper().readTree(rsProtect));

        final RsProtectResponse resp = client.umaRsProtect(Tester.getAuthorization(), params);
        assertNotNull(resp);
    }

    public static RegisterSiteResponse registerSite(ClientInterface client) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost("https://ce-dev.gluu.org");
        params.setAuthorizationRedirectUri("https://192.168.200.58:5053");
        params.setScope(Lists.newArrayList("openid", "profile", "email", "address", "clientinfo", "mobile_phone", "phone", "uma_protection"));
        params.setPostLogoutRedirectUris(Lists.newArrayList("https://192.168.200.58:5053"));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList("https://192.168.200.58:5053/logout"));
        params.setAcrValues(Lists.newArrayList("gplus", "basic", "duo", "u2f"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(params);

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
