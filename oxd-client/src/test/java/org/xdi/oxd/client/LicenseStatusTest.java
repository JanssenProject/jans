package org.xdi.oxd.client;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.response.LicenseStatusOpResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/11/2014
 */

public class LicenseStatusTest {

    @Parameters({"host", "port"})
    @Test
    public void test(String host, int port) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final LicenseStatusOpResponse response = client.licenseStatus();
            Assert.assertNotNull(response);

        } finally {
            CommandClient.closeQuietly(client);
        }
    }

}
