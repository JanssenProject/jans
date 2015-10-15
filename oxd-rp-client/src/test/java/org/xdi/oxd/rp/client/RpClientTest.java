package org.xdi.oxd.rp.client;

import com.google.common.base.Strings;
import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/10/2015
 */

public class RpClientTest {

    @Parameters({"host", "port", "redirectUrl"})
    @Test
    public void test(String host, int port, String redirectUrl) {
        final RpClient client = RpClientFactory.newSocketClient(host, port)
                .register(redirectUrl);

        Assert.assertNotNull(client.getRegistrationDetails());
        Assert.assertFalse(Strings.isNullOrEmpty(client.getOxdId()));
    }
}
