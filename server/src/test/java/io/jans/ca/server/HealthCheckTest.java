package io.jans.ca.server;

import io.jans.ca.common.Jackson2;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class HealthCheckTest {


    @Parameters({"host", "opHost"})
    @Test
    public void testHealthCheck(String host, String opHost) throws IOException {

        String resp = Tester.newClient(host).healthCheck();
        assertNotNull(resp);
        Map<String,String> map = Jackson2.createRpMapper().readValue(resp, Map.class);

        assertEquals(map.get("application"), "oxd");
        assertEquals(map.get("status"), "running");
        assertEquals(map.get("version"), System.getProperty("projectVersion"));

    }
}
