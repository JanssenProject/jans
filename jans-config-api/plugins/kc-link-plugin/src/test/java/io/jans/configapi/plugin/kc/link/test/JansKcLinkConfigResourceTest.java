/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.kc.link.test;

import io.jans.configapi.plugin.kc.link.KcLinkBaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Parameters;

public class JansKcLinkConfigResourceTest extends KcLinkBaseTest {

    @Parameters({ "test.issuer", "kcLinkConfigUrl" })
    @Test
    public void getKcLinkConfiguration(final String issuer, final String kcLinkConfigUrl) {
        log.info("getKcLinkConfiguration() - accessToken:{}, issuer:{}, kcLinkConfigUrl:{}", accessToken, issuer,
                kcLinkConfigUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + kcLinkConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("Response for getKcLinkConfiguration -  response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

}
