/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import io.jans.configapi.ConfigServerBaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class AgamaDeploymentsResourceTest extends ConfigServerBaseTest {

    @Parameters({ "test.issuer", "agamaDeploymentUrl" })
    @Test
    public void getDeployments(final String issuer, final String agamaDeploymentUrl) {
        log.info("accessToken:{}, issuer:{}, agamaDeploymentUrl:{}", accessToken, issuer, agamaDeploymentUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + agamaDeploymentUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getDefaultAuthenticationMethod -  response:{}", response);

    }

}
