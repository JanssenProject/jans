package io.jans.ca.server.manual;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.RsProtectParams2;
import io.jans.ca.common.Command;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RsProtectResponse;
import io.jans.ca.server.Tester;

import java.io.IOException;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

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
        params.setRpId(site.getRpId());
        params.setResources(Jackson2.createJsonMapper().readTree(rsProtect));

        final RsProtectResponse resp = client.umaRsProtect(Tester.getAuthorization(client.getApitargetURL(), site), null, params);
        assertNotNull(resp);
    }

    public static RegisterSiteResponse registerSite(ClientInterface client) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost("https://ce-dev.gluu.org");
        params.setRedirectUris(Lists.newArrayList("https://192.168.200.58:5053"));
        params.setScope(Lists.newArrayList("openid", "profile", "email", "address", "clientinfo", "mobile_phone", "phone", "uma_protection"));
        params.setPostLogoutRedirectUris(Lists.newArrayList("https://192.168.200.58:5053"));
        params.setClientFrontchannelLogoutUri("https://192.168.200.58:5053/logout");
        params.setAcrValues(Lists.newArrayList("gplus", "basic", "duo", "u2f"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(params);

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getRpId()));
        return resp;
    }
}
