package org.xdi.oxd.server;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxd.client.ClientInterface;
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

    @Parameters({"host", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void setupClient(String host, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException {
        SetupClientResponse resp = setupClient(Tester.newClient(host), opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
        assertResponse(resp);

        // more specific client setup
        final SetupClientParams params = new SetupClientParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setRedirectUris(Arrays.asList(redirectUrl));
        params.setAcrValues(new ArrayList<String>());
        params.setScope(Lists.newArrayList("openid", "profile"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

        resp = Tester.newClient(host).setupClient(params).dataAsResponse(SetupClientResponse.class);
        assertResponse(resp);
    }

    public static void assertResponse(SetupClientResponse resp) {
        assertNotNull(resp);

        notEmpty(resp.getClientId());
        notEmpty(resp.getClientSecret());
    }

    public static SetupClientResponse setupClient(ClientInterface client, String opHost, String redirectUrl) {
        return setupClient(client, opHost, redirectUrl, redirectUrl, "");
    }

    public static SetupClientResponse setupClient(ClientInterface client, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) {

        final SetupClientParams params = new SetupClientParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final Command command = new Command(CommandType.SETUP_CLIENT);
        command.setParamsObject(params);

        final SetupClientResponse resp = client.setupClient(params).dataAsResponse(SetupClientResponse.class);
        assertResponse(resp);
        return resp;
    }
}
