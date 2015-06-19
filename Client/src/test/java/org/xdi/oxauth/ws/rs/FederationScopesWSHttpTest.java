/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.dev.HostnameVerifierType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * clientId=@!1111!0008!FF81!2D39
 * client redirectUri=https://client.example.com/cb
 * federationTrustRedirectUri=https://client.example.com/cb?foo=bar
 * <p/>
 * <p/>
 * LDAP STUFF IF NEEDED TO GET TEST RUNNING
 * <p/>
 * dn: inum=@!1111!0008!D06B.16FA,ou=trust,ou=federation,o=@!1111,o=gluu
 * displayName: Demo (skip authorization)
 * inum: @!1111!0008!D06B.16FA
 * objectClass: oxAuthFederationTrust
 * objectClass: top
 * oxAuthFederationId: @!1111!0008!00F1!0001
 * oxAuthFederationMetadataURI: https://localhost:8443/oxauth/seam/resource/restv1/oxauth/federationmetadata
 * oxAuthFederationTrustStatus: active
 * oxAuthRedirectURI: https://client.example.com/cb?foo=bar
 * oxAuthReleasedScope: inum=@!1111!0009!BC01,ou=scopes,o=@!1111,o=gluu
 * oxAuthReleasedScope: inum=@!1111!0009!2B41,ou=scopes,o=@!1111,o=gluu
 * oxAuthSkipAuthorization: true
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version June 19, 2015
 */

public class FederationScopesWSHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestClientInfo(final String userId, final String userSecret,
                                  final String clientId, final String redirectUri) throws Exception {
        showTitle("FederationScopesWSHttpTest.requestClientInfo");

        // 1. Request authorization
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);
        List<String> scopes = new ArrayList<String>();
        scopes.add("clientinfo");
        scopes.add("email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec(
                new ApacheHttpClient4Executor(createHttpClient(HostnameVerifierType.ALLOW_ALL)));

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getState(), "The state is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getExpiresIn(), "The expires in value is null");
        assertNotNull(response1.getScope(), "The scope must be null");
        assertNotNull(response1.getIdToken(), "The id token must be null");

        String accessToken = response1.getAccessToken();

        // 2. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        clientInfoClient.setRequest(new ClientInfoRequest(accessToken));
        ClientInfoResponse response2 = clientInfoClient.exec(
                new ApacheHttpClient4Executor(createHttpClient(HostnameVerifierType.ALLOW_ALL)));

        showClient(clientInfoClient);
        assertEquals(response2.getStatus(), 200, "Unexpected response code: " + response2.getStatus());
        assertNotNull(response2.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(response2.getClaim("inum"), "Unexpected result: inum not found");
        assertNotNull(response2.getClaim("oxAuthAppType"), "Unexpected result: oxAuthAppType not found");
        assertNotNull(response2.getClaim("oxAuthIdTokenSignedResponseAlg"), "Unexpected result: oxAuthIdTokenSignedResponseAlg not found");
        assertNotNull(response2.getClaim("oxAuthRedirectURI"), "Unexpected result: oxAuthRedirectURI not found");
        assertNotNull(response2.getClaim("oxAuthScope"), "Unexpected result: oxAuthScope not found");
    }
}