/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.register.RegisterRequestParam.*;
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
        RegisterRequest registerRequest1 = new RegisterRequest(ApplicationType.WEB, "jans test app",
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
        AssertBuilder.registerResponse(registerResponse1).created().check();
        assertRegisterResponseClaimsNotNull(registerResponse1, SCOPE);

        String clientId = registerResponse1.getClientId();
        String registrationAccessToken = registerResponse1.getRegistrationAccessToken();
        String registrationClientUri = registerResponse1.getRegistrationClientUri();

        // 2. Client Read
        RegisterRequest registerRequest2 = new RegisterRequest(registrationAccessToken);

        RegisterClient registerClient2 = new RegisterClient(registrationClientUri);
        registerClient2.setRequest(registerRequest2);
        RegisterResponse registerResponse2 = registerClient2.exec();

        showClient(registerClient2);
        AssertBuilder.registerResponse(registerResponse2).ok()
                .notNullRegistrationClientUri()
                .check();
        assertRegisterResponseClaimsNotNull(registerResponse2, APPLICATION_TYPE, POLICY_URI, REQUEST_OBJECT_SIGNING_ALG, CONTACTS, SECTOR_IDENTIFIER_URI);
        assertRegisterResponseClaimsNotNull(registerResponse2, SUBJECT_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, JWKS_URI, CLIENT_NAME, LOGO_URI, REQUEST_URIS, SCOPE);
    }
}