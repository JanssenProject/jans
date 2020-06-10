package io.swagger.client.api;

import io.swagger.client.model.GetIssuerParams;
import io.swagger.client.model.GetIssuerResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class GetIssuerTest {

    @Parameters({"opHost", "emailWebfingerInput"})
    @Test
    public void emailInputTest(String opHost, String emailWebfingerInput) throws Exception {
        DevelopersApi api = Tester.api();

        final GetIssuerParams commandParams = new GetIssuerParams();
        commandParams.setResource(emailWebfingerInput);

        final GetIssuerResponse resp = api.getIssuer(commandParams);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), emailWebfingerInput);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), opHost);
        });
    }

    @Parameters({"opHost", "urlWebfingerInput"})
    @Test
    public void urlInputTest(String opHost, String urlWebfingerInput) throws Exception {
        DevelopersApi api = Tester.api();

        final GetIssuerParams commandParams = new GetIssuerParams();
        commandParams.setResource(urlWebfingerInput);

        final GetIssuerResponse resp = api.getIssuer(commandParams);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), urlWebfingerInput);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), opHost);
        });
    }

    @Parameters({"opHost", "hostnameWebfingerInput"})
    @Test
    public void hostnameInputTest(String opHost, String hostnameWebfingerInput) throws Exception {
        DevelopersApi api = Tester.api();

        final GetIssuerParams commandParams = new GetIssuerParams();
        commandParams.setResource(hostnameWebfingerInput);

        final GetIssuerResponse resp = api.getIssuer(commandParams);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), hostnameWebfingerInput);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), opHost);
        });
    }
}
