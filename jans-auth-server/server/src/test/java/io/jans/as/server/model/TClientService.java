/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.server.util.TestUtil;
import io.jans.as.server.BaseTest;
import io.jans.as.server.register.ws.rs.RegisterRestWebService;
import io.jans.as.server.util.ServerUtil;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import java.net.URI;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named

public class TClientService {

    private TClientService() {
    }

    public static RegisterResponse register(RegisterRequest registerRequest, String url) throws JsonProcessingException {
        RegisterRestWebService registerWs = WebServiceFactory.instance().createRegisterWs(url);
        Response response = registerWs.requestRegister(ServerUtil.toPrettyJson(registerRequest.getJSONParameters()), null, null);
        String entity = response.readEntity(String.class);

        BaseTest.showResponse("TClientService", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
        TestUtil.assert_(registerResponse);
        return registerResponse;
    }
}
