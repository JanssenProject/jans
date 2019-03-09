package org.xdi.oxd.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class HealthCheckTest {


    @Parameters({"host", "opHost"})
    @Test
    public void testHealthCheck(String host, String opHost) {

        String resp = Tester.newClient(host).healthCheck();
        assertNotNull(resp);
        assertEquals(new String("{\"status\":\"running\"}").trim(), resp);

    }
}
