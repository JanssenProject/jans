package org.gluu.oxauth.interop;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.gluu.oxauth.model.common.GrantType.AUTHORIZATION_CODE;
import static org.gluu.oxauth.model.common.ResponseType.CODE;
import static org.gluu.oxauth.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static org.gluu.oxauth.model.register.RegisterRequestParam.INITIATE_LOGIN_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * OP-3rd_party-init-login
 *
 * @author Javier Rojas Blum
 * @version October 22, 2019
 */
public class Supports3rdPartyInitLogin extends BaseTest {

    @Parameters({"redirectUri", "clientJwksUri", "initiateLoginUri", "postLogoutRedirectUri"})
    @Test
    public void supports3rdPartyInitLogin(final String redirectUri, final String clientJwksUri, final String initiateLoginUri, final String postLogoutRedirectUri) throws Exception {
        showTitle("supports3rdPartyInitLogin");

        // 1. Register Client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUri));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org"));
        registerRequest.setGrantTypes(Arrays.asList(AUTHORIZATION_CODE));
        registerRequest.setResponseTypes(Arrays.asList(CODE));
        registerRequest.setInitiateLoginUri(initiateLoginUri);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
        assertEquals(registerResponse.getClaims().get(APPLICATION_TYPE.toString()), ApplicationType.WEB.toString());
        assertEquals(registerResponse.getClaims().get(INITIATE_LOGIN_URI.toString()), initiateLoginUri);
    }
}
