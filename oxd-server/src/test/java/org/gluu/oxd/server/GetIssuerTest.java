package org.gluu.oxd.server;

import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetIssuerParams;
import org.gluu.oxd.common.response.GetIssuerResponse;
import org.gluu.oxd.server.Tester;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class GetIssuerTest {
    private static final String EMAIL_ADDRESS_INPUT = "admin@jenkins-ldap.gluu.org";
    private static final String URL_INPUT = "https://jenkins-ldap.gluu.org/admin";
    private static final String HOSTNAME_PORT_INPUT = "jenkins-ldap.gluu.org";

    @Parameters({"host"})
    @Test
    public void emailInputTest(String host) {
        ClientInterface client = Tester.newClient(host);

        final GetIssuerParams params = new GetIssuerParams();
        params.setResource(EMAIL_ADDRESS_INPUT);

        final GetIssuerResponse resp = client.getIssuer(params);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), EMAIL_ADDRESS_INPUT);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), "https://jenkins-ldap.gluu.org");
        });
    }

    @Parameters({"host"})
    @Test
    public void urlInputTest(String host) {
        ClientInterface client = Tester.newClient(host);

        final GetIssuerParams params = new GetIssuerParams();
        params.setResource(URL_INPUT);

        final GetIssuerResponse resp = client.getIssuer(params);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), URL_INPUT);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), "https://jenkins-ldap.gluu.org");
        });
    }

    @Parameters({"host"})
    @Test
    public void hostnameInputTest(String host) {
        ClientInterface client = Tester.newClient(host);

        final GetIssuerParams params = new GetIssuerParams();
        params.setResource(HOSTNAME_PORT_INPUT);

        final GetIssuerResponse resp = client.getIssuer(params);
        assertNotNull(resp);
        assertEquals(resp.getSubject(), HOSTNAME_PORT_INPUT);
        resp.getLinks().forEach((link) -> {
            assertEquals(link.getHref(), "https://jenkins-ldap.gluu.org");
        });
    }
}
