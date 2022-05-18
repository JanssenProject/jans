package io.jans.ca.server.tests;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.Assert.assertNotNull;

public class GetRpJwksTest extends BaseTest {

    @ArquillianResource
    URI url;

    @Test
    @Parameters({"host"})
    public void test(String host) {

        final ClientInterface client = Tester.newClient(getApiTagetURL(url));

        final JsonNode jwks = client.getRpJwks();
        assertNotNull(jwks);
        assertNotNull(jwks.get("keys"));
    }
}
