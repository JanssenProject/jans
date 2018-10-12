package org.xdi.oxd.client;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.params.UpdateSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.UpdateSiteResponse;

import java.io.IOException;
import java.util.ArrayList;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class RegisterSiteTest {

    private String oxdId = null;

    @Parameters({"host", "port", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void register(String host, int port, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

//            final SetupClientResponse setupClient = SetupClientTest.setupClient(client, opHost, redirectUrl);

            RegisterSiteResponse resp = registerSite(client, opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
            assertNotNull(resp);

            notEmpty(resp.getOxdId());

            // more specific site registration
            final RegisterSiteParams commandParams = new RegisterSiteParams();
            //commandParams.setProtectionAccessToken(setupClient.getClientRegistrationAccessToken());
            commandParams.setOpHost(opHost);
            commandParams.setAuthorizationRedirectUri(redirectUrl);
            commandParams.setPostLogoutRedirectUri(postLogoutRedirectUrl);
            commandParams.setClientFrontchannelLogoutUri(Lists.newArrayList(logoutUrl));
            commandParams.setRedirectUris(Lists.newArrayList(redirectUrl));
            commandParams.setAcrValues(new ArrayList<String>());
            commandParams.setScope(Lists.newArrayList("openid", "profile"));
            commandParams.setGrantType(Lists.newArrayList("authorization_code"));
            commandParams.setResponseTypes(Lists.newArrayList("code"));

            final Command command = new Command(CommandType.REGISTER_SITE);
            command.setParamsObject(commandParams);

            resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
            assertNotNull(resp);
            assertNotNull(resp.getOxdId());
            oxdId = resp.getOxdId();
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    @Parameters({"host", "port"})
    @Test(dependsOnMethods = {"register"})
    public void update(String host, int port) throws IOException {
        notEmpty(oxdId);

        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            // more specific site registration
            final UpdateSiteParams commandParams = new UpdateSiteParams();
            commandParams.setOxdId(oxdId);

            final Command command = new Command(CommandType.UPDATE_SITE);
            command.setParamsObject(commandParams);

            UpdateSiteResponse resp = client.send(command).dataAsResponse(UpdateSiteResponse.class);
            UpdateSiteResponse resp2 = client.send(command).dataAsResponse(UpdateSiteResponse.class); // send 2 more update calls to make sure we are consistent
            UpdateSiteResponse resp3 = client.send(command).dataAsResponse(UpdateSiteResponse.class);
            assertNotNull(resp);
            assertNotNull(resp2);
            assertNotNull(resp3);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    @Test(enabled = false)
    public void manual() throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient("localhost", 8099);

            final RegisterSiteParams commandParams = new RegisterSiteParams();

            commandParams.setAuthorizationRedirectUri("https://gluu.loc/wp-login.php?option=oxdOpenId");
            commandParams.setPostLogoutRedirectUri("https://gluu.loc/wp-login.php?action=logout&amp;_wpnonce=1fd6fda129");

            commandParams.setRedirectUris(Lists.newArrayList("https://gluu.loc/wp-login.php?option=oxdOpenId", "https://gluu.loc/wp-login.php?action=logout&amp;_wpnonce=1fd6fda129"));
            commandParams.setClaimsRedirectUri(Lists.newArrayList("https://gluu.loc/wp-login.php?option=oxdOpenId", "https://gluu.loc/wp-login.php?action=logout&amp;_wpnonce=1fd6fda129"));
            commandParams.setAcrValues(new ArrayList<String>());
            commandParams.setContacts(Lists.newArrayList("vlad.karapetyan.1988@gmail.com"));

//            commandParams.setClientLogoutUri("https://mag.gluu/index.php/customer/account/logout/");
            commandParams.setClientFrontchannelLogoutUri(Lists.newArrayList("https://gluu.loc/index.php/customer/account/logout/"));
            commandParams.setScope(Lists.newArrayList("openid", "profile", "email"));
            commandParams.setGrantType(Lists.newArrayList("authorization_code"));
            commandParams.setOxdRpProgrammingLanguage("java");

            commandParams.setResponseTypes(Lists.newArrayList("code"));

            final Command command = new Command(CommandType.REGISTER_SITE);
            command.setParamsObject(commandParams);

            RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
            assertNotNull(resp);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RegisterSiteResponse registerSite(CommandClient client, String opHost, String redirectUrl) {
        return registerSite(client, opHost, redirectUrl, redirectUrl, "");
    }

    public static RegisterSiteResponse registerSite(CommandClient client, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUri(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantType(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setOxdRpProgrammingLanguage("java");

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(params);

        final RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
