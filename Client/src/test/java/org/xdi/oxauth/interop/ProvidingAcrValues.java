package org.xdi.oxauth.interop;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version November 22, 2017
 */
public class ProvidingAcrValues extends BaseTest {

    @Parameters({"redirectUri", "clientJwksUri", "userId", "userSecret"})
    @Test
    public void providingAcrValues(final String redirectUri, final String jwksUri, final String userId, final String userSecret) throws Exception {
        showTitle("providingAcrValues");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT);
        List<String> contacts = Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUri));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setContacts(contacts);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getRegistrationClientUri());
        assertNotNull(response.getClientIdIssuedAt());
        assertNotNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getResponseTypes());
        assertTrue(response.getResponseTypes().containsAll(responseTypes));
        assertNotNull(response.getGrantTypes());
        assertTrue(response.getGrantTypes().containsAll(grantTypes));

        String clientId = response.getClientId();

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid");
        List<String> acrValues = Arrays.asList("basic");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAcrValues(acrValues);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getScope());
    }
}
