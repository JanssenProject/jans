package org.gluu.oxd.server.manual;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.client.TokenClient;
import org.gluu.oxauth.client.TokenResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 29/10/2013
 */

public class ClientCredentialsTest {

    @Parameters({"clientId", "clientSecret"})
    @Test(enabled = false) // for manual run
    public void test(String clientId, String clientSecret) {
        final String tokenEndpoint = "https://ce-dev.gluu.org/oxauth/seam/resource/restv1/oxauth/token";
        final TokenClient tokenClient = new TokenClient(tokenEndpoint);
        final TokenResponse response = tokenClient.execClientCredentialsGrant("openid", clientId, clientSecret);
        System.out.println(response);
    }
}
