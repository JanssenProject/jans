package org.xdi.oxd.client;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.response.ObtainAatOpResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/01/2014
 */
public class ObtainAatTest {

    @Parameters({"host", "port", "discoveryUrl", "umaDiscoveryUrl", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String discoveryUrl, String umaDiscoveryUrl, String redirectUrl,
                     String clientId, String clientSecret, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final ObtainAatOpResponse r = TestUtils.obtainAat(client, discoveryUrl, umaDiscoveryUrl, redirectUrl,
                    clientId, clientSecret, userId, userSecret);
            Assert.assertNotNull(r);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

}
