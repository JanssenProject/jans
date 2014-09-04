package org.xdi.oxd.client;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.ResponseStatus;
import org.xdi.oxd.common.params.DiscoveryParams;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class DiscoveryTest {

    @Parameters({"host", "port", "discoveryUrl"})
    @Test
    public void test(String host, int port, String discoveryUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final Command command = new Command(CommandType.DISCOVERY);
            command.setParamsObject(new DiscoveryParams(discoveryUrl));
            final CommandResponse response = client.send(command);
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getStatus() == ResponseStatus.OK);
            System.out.println(response);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
