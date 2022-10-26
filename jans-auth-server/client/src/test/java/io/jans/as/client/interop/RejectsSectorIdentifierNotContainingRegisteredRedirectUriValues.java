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
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * OC5:FeatureTest-Rejects Sector Identifier Not Containing Registered redirect uri Values
 *
 * @author Javier Rojas Blum Date: 08.22.2013
 */
public class RejectsSectorIdentifierNotContainingRegisteredRedirectUriValues extends BaseTest {

    @Parameters({"sectorIdentifierUri"})
    @Test
    public void rejectsSectorIdentifierNotContainingRegisteredRedirectUriValues(final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Rejects Sector Identifier Not Containing Registered redirect uri Values");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList("https://not_registered"));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setResponseTypes(responseTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).bad().check();
    }
}