package io.jans.ca.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class HealthCheckTest {


    @Parameters({"host", "opHost"})
    @Test
    public void testHealthCheck(String host, String opHost) {

        String resp = Tester.newClient(host).healthCheck();
        assertNotNull(resp);
        assertEquals("{\"status\":\"running\"}", resp);

    }
}
