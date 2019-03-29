/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import java.util.Arrays;
import java.util.List;

import static org.gluu.oxauth.model.register.RegisterRequestParam.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * OC5:FeatureTest-Support Registration Read
 *
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class SupportRegistrationRead extends BaseTest {

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void supportRegistrationRead(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Support Registration Read");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest1 = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest1.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest1.setLogoUri("http://www.gluu.org/wp-content/themes/gluursn/images/logo.png");
        registerRequest1.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest1.setPolicyUri("http://www.gluu.org/policy");
        registerRequest1.setJwksUri("http://www.gluu.org/jwks");
        registerRequest1.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest1.setSubjectType(SubjectType.PUBLIC);
        registerRequest1.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest1.setRequestUris(Arrays.asList("http://www.gluu.org/request"));

        RegisterClient registerClient1 = new RegisterClient(registrationEndpoint);
        registerClient1.setRequest(registerRequest1);
        RegisterResponse registerResponse1 = registerClient1.exec();

        showClient(registerClient1);
        assertEquals(registerResponse1.getStatus(), 200, "Unexpected response code: " + registerResponse1.getEntity());
        assertNotNull(registerResponse1.getClientId());
        assertNotNull(registerResponse1.getClientSecret());
        assertNotNull(registerResponse1.getRegistrationAccessToken());
        assertNotNull(registerResponse1.getClientSecretExpiresAt());
        assertNotNull(registerResponse1.getClaims().get(SCOPE.toString()));

        String clientId = registerResponse1.getClientId();
        String registrationAccessToken = registerResponse1.getRegistrationAccessToken();
        String registrationClientUri = registerResponse1.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest registerRequest2 = new RegisterRequest(registrationAccessToken);

        RegisterClient registerClient2 = new RegisterClient(registrationClientUri);
        registerClient2.setRequest(registerRequest2);
        RegisterResponse registerResponse2 = registerClient2.exec();

        showClient(registerClient2);
        assertEquals(registerResponse2.getStatus(), 200, "Unexpected response code: " + registerResponse2.getEntity());
        assertNotNull(registerResponse2.getClientId());
        assertNotNull(registerResponse2.getClientSecret());
        assertNotNull(registerResponse2.getRegistrationAccessToken());
        assertNotNull(registerResponse2.getRegistrationClientUri());
        assertNotNull(registerResponse2.getClientSecretExpiresAt());
        assertNotNull(registerResponse2.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(registerResponse2.getClaims().get(POLICY_URI.toString()));
        assertNotNull(registerResponse2.getClaims().get(REQUEST_OBJECT_SIGNING_ALG.toString()));
        assertNotNull(registerResponse2.getClaims().get(CONTACTS.toString()));
        assertNotNull(registerResponse2.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(registerResponse2.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(registerResponse2.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(registerResponse2.getClaims().get(JWKS_URI.toString()));
        assertNotNull(registerResponse2.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(registerResponse2.getClaims().get(LOGO_URI.toString()));
        assertNotNull(registerResponse2.getClaims().get(REQUEST_URIS.toString()));
        assertNotNull(registerResponse2.getClaims().get(SCOPE.toString()));
    }
}