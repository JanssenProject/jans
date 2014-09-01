package org.xdi.oxd.client;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RptStatusParams;
import org.xdi.oxd.common.response.ObtainAatOpResponse;
import org.xdi.oxd.common.response.ObtainPatOpResponse;
import org.xdi.oxd.common.response.RegisterResourceOpResponse;
import org.xdi.oxd.common.response.RptStatusOpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class RptStatusTest {

    private static final List<String> SCOPES = Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all");

    private static CommandClient g_client = null;
    private static String g_patToken;
    private static String g_aatToken;
    private static String g_rpt;
    private static String g_umaDiscoveryUrl;
    private static RegisterResourceOpResponse g_resource;

    @Parameters({"host", "port", "discoveryUrl", "umaDiscoveryUrl", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret", "amHost"})
    @BeforeClass
    public static void setUp(String host, int port, String discoveryUrl, String umaDiscoveryUrl, String redirectUrl,
                             String clientId, String clientSecret, String userId, String userSecret, String amHost) throws IOException {
        g_client = new CommandClient(host, port);
        g_umaDiscoveryUrl = umaDiscoveryUrl;

        final ObtainPatOpResponse patResponse = TestUtils.obtainPat(g_client, discoveryUrl, umaDiscoveryUrl, redirectUrl,
                clientId, clientSecret, userId, userSecret);
        Assert.assertNotNull(patResponse);
        g_patToken = patResponse.getPatToken();
        Assert.assertTrue(StringUtils.isNotBlank(g_patToken));


        final ObtainAatOpResponse aatResponse = TestUtils.obtainAat(g_client, discoveryUrl, umaDiscoveryUrl, redirectUrl,
                clientId, clientSecret, userId, userSecret);
        Assert.assertNotNull(aatResponse);
        g_aatToken = aatResponse.getAatToken();
        Assert.assertTrue(StringUtils.isNotBlank(g_aatToken));

        g_resource = TestUtils.registerResource(g_client, umaDiscoveryUrl, g_patToken, SCOPES);
        Assert.assertNotNull(g_resource);

        g_rpt = TestUtils.obtainRpt(g_aatToken, umaDiscoveryUrl, amHost);
        Assert.assertNotNull(StringUtils.isNotBlank(g_rpt));
    }

    @AfterClass
    public static void tearDown() {
        CommandClient.closeQuietly(g_client);
    }

    @Test
    public void test() throws IOException {
        final RptStatusParams params = new RptStatusParams();
        params.setPatToken(g_patToken);
        params.setRpt(g_rpt);
        params.setUmaDiscoveryUrl(g_umaDiscoveryUrl);

        final Command command = new Command(CommandType.RPT_STATUS);
        command.setParamsObject(params);

        final CommandResponse response = g_client.send(command);
        Assert.assertNotNull(response);
        System.out.println(response);

        final RptStatusOpResponse r = response.dataAsResponse(RptStatusOpResponse.class);
        Assert.assertNotNull(r);

    }
}
