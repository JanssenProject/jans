package org.gluu.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class HealthCheckTest {


    @Parameters({"host", "opHost"})
    @Test
    public void testHealthCheck(String host, String opHost) {

        String resp = Tester.newClient(host).healthCheck();
        assertNotNull(resp);
        assertEquals("{\"status\":\"running\"}", resp);

    }
}
