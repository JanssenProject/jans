package org.xdi.oxd.client;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterClientParams;
import org.xdi.oxd.common.response.ObtainPatOpResponse;
import org.xdi.oxd.common.response.RegisterClientOpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/11/2013
 */

public class CompleteFlowTest {

    private static CommandClient g_client;

    @Parameters({"host", "port"})
    @BeforeClass
    public static void beforeClass(String host, int port) throws IOException {
        g_client = new CommandClient(host, port);
    }

    @AfterClass
    public static void afterClass() {
        CommandClient.closeQuietly(g_client);
    }

    @Parameters({"discoveryUrl", "umaDiscoveryUrl", "redirectUrl", "clientName"})
    @Test
    public void test(String discoveryUrl, String umaDiscoveryUrl, String redirectUrl, String clientName) throws IOException {
        // 1. register client
        final RegisterClientOpResponse r = registerClient(discoveryUrl, redirectUrl, clientName);
        Assert.assertNotNull(r);
        final String clientId = r.getClientId();
        final String clientSecret = r.getClientSecret();

        // 2. obtain PAT
        final ObtainPatOpResponse patResponse = TestUtils.obtainPat(g_client, discoveryUrl, umaDiscoveryUrl, redirectUrl,
                clientId, clientSecret);
        Assert.assertNotNull(patResponse);
        Assert.assertNotNull(patResponse.getPatToken());

        final String patToken = patResponse.getPatToken();

        // 3. register resource
        final List<String> scopes = Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all");
        TestUtils.registerResource(g_client, umaDiscoveryUrl, patToken, scopes);
    }

    public RegisterClientOpResponse registerClient(String discoveryUrl, String redirectUrl, String clientName) {
        final RegisterClientParams params = new RegisterClientParams();
        params.setDiscoveryUrl(discoveryUrl);
        params.setRedirectUrl(Lists.newArrayList(redirectUrl));
        params.setClientName(clientName);

        final Command command = new Command(CommandType.REGISTER_CLIENT);
        command.setParamsObject(params);
        final CommandResponse response = g_client.send(command);
        Assert.assertNotNull(response);
        System.out.println(response);
        final RegisterClientOpResponse r = response.dataAsResponse(RegisterClientOpResponse.class);
        Assert.assertNotNull(r);
        return r;
    }
}
