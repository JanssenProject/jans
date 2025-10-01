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
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;


import static io.jans.as.model.common.GrantType.AUTHORIZATION_CODE;
import static io.jans.as.model.common.ResponseType.CODE;
import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.INITIATE_LOGIN_URI;
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
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
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
        AssertBuilder.registerResponse(registerResponse).created().check();
        assertEquals(registerResponse.getClaims().get(APPLICATION_TYPE.toString()), ApplicationType.WEB.toString());
        assertEquals(registerResponse.getClaims().get(INITIATE_LOGIN_URI.toString()), initiateLoginUri);
    }
}
