package io.jans.ca.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.ca.client.ClientInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class GetRpJwksTest {
    private static final Logger LOG = LoggerFactory.getLogger(GetRpJwksTest.class);
    @Test
    @Parameters({"host"})
    public void test(String host) {

        final ClientInterface client = Tester.newClient(host);

        final JsonNode jwks = client.getRpJwks();
        assertNotNull(jwks);
        assertNotNull(jwks.get("keys"));
    }
}
