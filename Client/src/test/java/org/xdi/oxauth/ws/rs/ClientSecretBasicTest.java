package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;

import static org.testng.Assert.assertEquals;

/**
 * http://tools.ietf.org/html/rfc2617#section-2
 *
 * @author Javier Rojas Blum
 * @version January 20, 2016
 */
public class ClientSecretBasicTest extends BaseTest {

    @Test
    public void testEncode1() {
        showTitle("testEncode1");

        String clientId = "Aladdin";
        String clientSecret = "open sesame";
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        assertEquals(tokenRequest.getEncodedCredentials(), "QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
    }

    @Test
    public void testEncode2() {
        showTitle("testEncode2");

        String clientId = "a+b";
        String clientSecret = "c+d";
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        assertEquals(tokenRequest.getEncodedCredentials(), "YStiOmMrZA==");
    }
}
