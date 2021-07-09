package io.jans.ca.server;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetIssuerParams;
import io.jans.ca.common.response.GetIssuerResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class GetIssuerTest {

    @Parameters({"host", "opHost", "emailWebfingerInput"})
    @Test(enabled = false)
    public void emailInputTest(String host, String opHost, String emailWebfingerInput) {
        ClientInterface client = Tester.newClient(host);

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
        ClientInterface client = Tester.newClient(host);

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
        ClientInterface client = Tester.newClient(host);

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
