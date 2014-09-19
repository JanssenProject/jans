package org.xdi.oxd.license.test;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

public class UndertowTest {

    private static UndertowJaxrsServer server;

    @BeforeClass
    public static void init() throws Exception {
        server = new UndertowJaxrsServer().start();
    }

    @AfterClass
    public static void stop() throws Exception {
        server.stop();
    }


}
