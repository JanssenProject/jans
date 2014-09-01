package org.xdi.oxd.client;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.response.ObtainPatOpResponse;
import org.xdi.oxd.common.response.RegisterResourceOpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class RegisterTicketTest {

    private static final List<String> SCOPES = Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all");

    private static CommandClient g_client = null;
    private static String g_patToken;
    private static RegisterResourceOpResponse g_resource;

    @Parameters({"host", "port", "discoveryUrl", "umaDiscoveryUrl", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret"})
    @BeforeClass
    public static void setUp(String host, int port, String discoveryUrl, String umaDiscoveryUrl, String redirectUrl,
                             String clientId, String clientSecret, String userId, String userSecret) throws IOException {
        g_client = new CommandClient(host, port);

        final ObtainPatOpResponse patResponse = TestUtils.obtainPat(g_client, discoveryUrl, umaDiscoveryUrl, redirectUrl,
                clientId, clientSecret, userId, userSecret);
        Assert.assertNotNull(patResponse);
        g_patToken = patResponse.getPatToken();
        Assert.assertTrue(StringUtils.isNotBlank(g_patToken));

        g_resource = TestUtils.registerResource(g_client, umaDiscoveryUrl, g_patToken, SCOPES);
        Assert.assertNotNull(g_resource);
    }

    @AfterClass
    public static void tearDown() {
        CommandClient.closeQuietly(g_client);
    }

    @Parameters({"umaDiscoveryUrl", "amHost", "rsHost", "request_url", "request_http_method"})
    @Test
    public void test(String umaDiscoveryUrl, String amHost, String rsHost, String requestUrl, String requestHttpMethod) throws IOException {
        TestUtils.registerTicket(g_client, umaDiscoveryUrl,
                g_patToken, g_resource.getId(), amHost, rsHost,
                SCOPES, requestHttpMethod, requestUrl);
    }

}
