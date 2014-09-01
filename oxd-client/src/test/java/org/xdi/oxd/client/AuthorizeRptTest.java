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
import org.xdi.oxd.common.ResponseStatus;
import org.xdi.oxd.common.params.AuthorizeRptParams;
import org.xdi.oxd.common.response.ObtainAatOpResponse;
import org.xdi.oxd.common.response.ObtainPatOpResponse;
import org.xdi.oxd.common.response.RegisterPermissionTicketOpResponse;
import org.xdi.oxd.common.response.RegisterResourceOpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class AuthorizeRptTest {

    private static final List<String> SCOPES = Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all");

    private static CommandClient g_client = null;
    private static String g_patToken;
    private static String g_aatToken;
    private static String g_rpt;
    private static String g_umaDiscoveryUrl;
    private static String g_amHost;
    private static String g_rsHost;
    private static RegisterResourceOpResponse g_resource;

    @Parameters({"host", "port", "discoveryUrl", "umaDiscoveryUrl", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret", "amHost", "rsHost"})
    @BeforeClass
    public static void setUp(String host, int port, String discoveryUrl, String umaDiscoveryUrl, String redirectUrl,
                             String clientId, String clientSecret, String userId, String userSecret, String amHost, String rsHost) throws IOException {
        g_client = new CommandClient(host, port);
        g_umaDiscoveryUrl = umaDiscoveryUrl;
        g_amHost = amHost;
        g_rsHost = rsHost;

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
        final RegisterPermissionTicketOpResponse ticketResponse = TestUtils.registerTicket(g_client, g_umaDiscoveryUrl,
                g_patToken, g_resource.getId(), g_amHost, g_rsHost, SCOPES, "GET", "http://example.com/object/1234");
        Assert.assertTrue(ticketResponse != null && StringUtils.isNotBlank(ticketResponse.getTicket()));

        final AuthorizeRptParams params = new AuthorizeRptParams();
        params.setAatToken(g_aatToken);
        params.setRptToken(g_rpt);
        params.setAmHost(g_amHost);
        params.setTicket(ticketResponse.getTicket());

        final Command command = new Command(CommandType.AUTHORIZE_RPT);
        command.setParamsObject(params);

        final CommandResponse response = g_client.send(command);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), ResponseStatus.OK);
        System.out.println(response);

    }

//    public void checkRptStatus() throws IOException {
//        final RptStatusParams params = new RptStatusParams();
//        params.setPatToken(g_patToken);
//        params.setRpt(g_rpt);
//        params.setUmaDiscoveryUrl(g_umaDiscoveryUrl);
//
//        final Command command = new Command(CommandType.RPT_STATUS);
//        command.setParamsObject(params);
//
//        final CommandResponse response = g_client.send(command);
//        Assert.assertNotNull(response);
//        System.out.println(response);
//
//        final RptStatusOpResponse r = response.dataAsResponse(RptStatusOpResponse.class);
//        Assert.assertNotNull(r);
//
//    }
}
