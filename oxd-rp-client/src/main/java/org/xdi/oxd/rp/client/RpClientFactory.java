package org.xdi.oxd.rp.client;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/10/2015
 */

public class RpClientFactory {

    private RpClientFactory() {
    }

    public static RpClient newSocketClient(String host, int port) {
        return new RpSocketClient(host, port);
    }

    public static void close(RpClient rpClient) {
        if (rpClient != null) {
            rpClient.close();
        }
    }

}
