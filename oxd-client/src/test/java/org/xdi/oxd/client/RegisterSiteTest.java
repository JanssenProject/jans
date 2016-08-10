package org.xdi.oxd.client;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.params.UpdateSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.UpdateSiteResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static junit.framework.Assert.*;
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

            RegisterSiteResponse resp = registerSite(client, opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
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

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            // more specific site registration
            final UpdateSiteParams commandParams = new UpdateSiteParams();
            commandParams.setOxdId(oxdId);
            commandParams.setClientSecretExpiresAt(calendar.getTime());
            commandParams.setScope(Lists.newArrayList("profile"));

            final Command command = new Command(CommandType.UPDATE_SITE);
            command.setParamsObject(commandParams);

            UpdateSiteResponse resp = client.send(command).dataAsResponse(UpdateSiteResponse.class);
            assertNotNull(resp);
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

            commandParams.setRedirectUris(Arrays.asList("https://gluu.loc/wp-login.php?option=oxdOpenId", "https://gluu.loc/wp-login.php?action=logout&amp;_wpnonce=1fd6fda129"));
            commandParams.setAcrValues(new ArrayList<String>());
            commandParams.setContacts(Arrays.asList("vlad.karapetyan.1988@gmail.com"));

//            commandParams.setClientLogoutUri("https://mag.gluu/index.php/customer/account/logout/");
            commandParams.setClientLogoutUri(Lists.newArrayList("https://gluu.loc/index.php/customer/account/logout/"));
            commandParams.setScope(Lists.newArrayList("openid", "profile", "email"));
            commandParams.setGrantType(Lists.newArrayList("authorization_code"));

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

        final RegisterSiteParams commandParams = new RegisterSiteParams();
        commandParams.setOpHost(opHost);
        commandParams.setAuthorizationRedirectUri(redirectUrl);
        commandParams.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        commandParams.setClientLogoutUri(Lists.newArrayList(logoutUri));
        commandParams.setScope(Lists.newArrayList("openid", "uma_protection", "uma_authorization"));

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(commandParams);

        final RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }

}
