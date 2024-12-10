/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.core.test.BaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Parameters;

public class UserResourceTest extends BaseTest {

    @Parameters({"issuer", "userUrl"})
    //@Test
    public void getUserResourceData(final String issuer, final String userUrl) {
        log.error("\n\n getUserResourceData() - accessToken:{}, issuer:{}, userUrl:{}", accessToken, issuer, userUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + userUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("\n\cn Response for getUserResourceData() -  response:{}", response);
    }
    
	

}
