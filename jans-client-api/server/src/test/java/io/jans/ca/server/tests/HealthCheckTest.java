package io.jans.ca.server.tests;

import io.jans.ca.common.Jackson2;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class HealthCheckTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Test
    public void testHealthCheck() throws IOException {
        showTitle("testHealthCheck");
        String resp = Tester.newClient(getApiTagetURL(url)).healthCheck();

        assertNotNull(resp);
        Map<String, String> map = Jackson2.createRpMapper().readValue(resp, Map.class);

        assertEquals(map.get("application"), "jans-client-api");
        assertEquals(map.get("status"), "running");
//        assertEquals(map.get("version"), System.getProperty("projectVersion"));
    }
}
