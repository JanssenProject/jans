package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetIssuerParams;
import io.jans.ca.common.response.GetIssuerResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class GetIssuerTest extends BaseTest {
    @ArquillianResource
    URI url;

    @Parameters({"host", "opHost", "emailWebfingerInput"})
    @Test(enabled = false)
    public void emailInputTest(String host, String opHost, String emailWebfingerInput) {
        String hostTargetURL = getApiTagetURL(url);
        ClientInterface client = Tester.newClient(hostTargetURL);

        final GetIssuerParams params = new GetIssuerParams();
        params.setResource(emailWebfingerInput);
        params.setOpHost(opHost);
        final GetIssuerResponse resp = client.getIssuer(params);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), emailWebfingerInput);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), opHost);
        });
    }

    @Parameters({"host", "opHost", "urlWebfingerInput"})
    @Test(enabled = false)
    public void urlInputTest(String host, String opHost, String urlWebfingerInput) {
        String hostTargetURL = getApiTagetURL(url);
        ClientInterface client = Tester.newClient(hostTargetURL);

        final GetIssuerParams params = new GetIssuerParams();
        params.setResource(urlWebfingerInput);
        params.setOpHost(opHost);
        final GetIssuerResponse resp = client.getIssuer(params);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), urlWebfingerInput);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), opHost);
        });
    }

    @Parameters({"host", "opHost", "hostnameWebfingerInput"})
    @Test
    public void hostnameInputTest(String host, String opHost, String hostnameWebfingerInput) {
        String hostTargetURL = getApiTagetURL(url);
        ClientInterface client = Tester.newClient(hostTargetURL);

        final GetIssuerParams params = new GetIssuerParams();
        params.setResource(hostnameWebfingerInput);
        params.setOpHost(opHost);
        final GetIssuerResponse resp = client.getIssuer(params);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), hostnameWebfingerInput);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), opHost);
        });
    }
}
