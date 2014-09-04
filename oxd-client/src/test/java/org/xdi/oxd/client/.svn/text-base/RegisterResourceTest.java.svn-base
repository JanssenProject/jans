package org.xdi.oxd.client;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.response.ObtainPatOpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class RegisterResourceTest {

    private static CommandClient g_client = null;
    private static String g_patToken;

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
    }


    @AfterClass
    public static void tearDown() {
        CommandClient.closeQuietly(g_client);
    }

    @Parameters({"umaDiscoveryUrl"})
    @Test
    public void test(String umaDiscoveryUrl) throws IOException {
        final List<String> scopes = Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all");
        TestUtils.registerResource(g_client, umaDiscoveryUrl, g_patToken, scopes);
    }

}
