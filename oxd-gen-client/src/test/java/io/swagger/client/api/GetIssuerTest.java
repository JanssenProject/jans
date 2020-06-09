package io.swagger.client.api;

import io.swagger.client.model.GetDiscoveryParams;
import io.swagger.client.model.GetDiscoveryResponse;
import io.swagger.client.model.GetIssuerParams;
import io.swagger.client.model.GetIssuerResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class GetIssuerTest {
    private static final String EMAIL_ADDRESS_INPUT = "admin@jenkins-ldap.gluu.org";
    private static final String URL_INPUT = "https://jenkins-ldap.gluu.org/admin";
    private static final String HOSTNAME_PORT_INPUT = "jenkins-ldap.gluu.org";

    @Test
    public void emailInputTest() throws Exception {
        DevelopersApi api = Tester.api();

        final GetIssuerParams commandParams = new GetIssuerParams();
        commandParams.setResource(EMAIL_ADDRESS_INPUT);

        final GetIssuerResponse resp = api.getIssuer(commandParams);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), EMAIL_ADDRESS_INPUT);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), "https://jenkins-ldap.gluu.org");
        });
    }

    @Test
    public void urlInputTest() throws Exception {
        DevelopersApi api = Tester.api();

        final GetIssuerParams commandParams = new GetIssuerParams();
        commandParams.setResource(URL_INPUT);

        final GetIssuerResponse resp = api.getIssuer(commandParams);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), URL_INPUT);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), "https://jenkins-ldap.gluu.org");
        });
    }

    @Test
    public void hostnameInputTest() throws Exception {
        DevelopersApi api = Tester.api();

        final GetIssuerParams commandParams = new GetIssuerParams();
        commandParams.setResource(HOSTNAME_PORT_INPUT);

        final GetIssuerResponse resp = api.getIssuer(commandParams);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), HOSTNAME_PORT_INPUT);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), "https://jenkins-ldap.gluu.org");
        });
    }
}
