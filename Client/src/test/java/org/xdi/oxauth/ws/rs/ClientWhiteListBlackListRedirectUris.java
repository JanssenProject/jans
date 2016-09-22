package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;
import org.xdi.oxauth.model.util.URLPatternList;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version September 21, 2016
 */
public class ClientWhiteListBlackListRedirectUris extends BaseTest {

    @Test
    public void testUrlPatterList() {
        showTitle("testUrlPatterList");

        List<String> urlPatterns = Arrays.asList(
                "*.gluu.org/foo*bar",
                "https://example.org/foo/bar.html",
                "*.attacker.com/*");

        URLPatternList urlPatternList = new URLPatternList(urlPatterns);
        assertFalse(urlPatternList.isUrlListed("gluu.org"));
        assertFalse(urlPatternList.isUrlListed("www.gluu.org"));
        assertTrue(urlPatternList.isUrlListed("http://gluu.org/foo/bar"));
        assertTrue(urlPatternList.isUrlListed("https://mail.gluu.org/foo/bar"));
        assertTrue(urlPatternList.isUrlListed("http://www.gluu.org/foobar"));
        assertTrue(urlPatternList.isUrlListed("https://www.gluu.org/foo/baz/bar"));
        assertFalse(urlPatternList.isUrlListed("http://example.org"));
        assertFalse(urlPatternList.isUrlListed("http://example.org/foo/bar.html"));
        assertTrue(urlPatternList.isUrlListed("https://example.org/foo/bar.html"));
        assertTrue(urlPatternList.isUrlListed("http://attacker.com"));
        assertTrue(urlPatternList.isUrlListed("https://www.attacker.com"));
        assertTrue(urlPatternList.isUrlListed("https://www.attacker.com/foo/bar"));
    }

    @Test
    public void requestClientAssociateInBlackList() throws Exception {
        showTitle("requestClientAssociateInBlackList");

        final String redirectUris = "https://www.attacker.com";

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse response = registerClient.execRegister(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }
}