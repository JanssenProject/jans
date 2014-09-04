package org.xdi.oxd.client;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/07/2013
 */
public class TransportClientTest {

    @Test
    public void test() throws IOException {
        TransportClient c = null;
        try {
            c = new TransportClient("localhost", 8099);

            final String oneR = c.sendCommand("one command");
            System.out.println(oneR);

            final String secondR = c.sendCommand("second command");
            System.out.println(secondR);

        } finally {
            TransportClient.closeQuietly(c);
        }
    }
}
