/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;

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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList("https://not_registered"));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setResponseTypes(responseTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 400, "Unexpected response code: " + registerResponse.getStatus());
        assertNotNull(registerResponse.getErrorType(), "The error type is null");
        assertNotNull(registerResponse.getErrorDescription(), "The error description is null");
    }
}