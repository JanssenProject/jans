package org.xdi.oxd.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class HealthCheckTest {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckTest.class);

    @Parameters({"host", "opHost"})
    @Test
    public void testHealthCheck(String host, String opHost) {

        LOG.debug("Test Health Check- "+host+opHost + "Response ::");
        String resp=Tester.newClient(host).healthCheck();
        assertNotNull(resp);
        assertEquals("{\"status\":\"running\"}",resp);

        //LOG.debug("Health Check Response :: "+clientInterface.healthCheck());
    }
}
