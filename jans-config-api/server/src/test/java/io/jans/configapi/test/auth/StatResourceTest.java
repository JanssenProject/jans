/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import static org.testng.Assert.assertEquals;

import io.jans.configapi.ConfigServerBaseTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class StatResourceTest extends ConfigServerBaseTest {
	
	private static final String INVALID_SCOPE = "https://jans.io/oauth/config/attributes.readonly";
	private static final String SUPER_ADMIN_SCOPE = "https://jans.io/oauth/config/stats.admin";


    @Parameters({ "test.issuer", "statUrl" })
    @Test
    public void getStatsInvalidToken(final String issuer, final String statUrl) {
        log.info("getStatsInvalidToken() - accessToken:{}, issuer:{}, statUrl:{}", accessToken, issuer,
                statUrl);
		String invalidToken = this.getAccessTokenForGivenScope(INVALID_SCOPE);
        log.info("getStatsInvalidToken() - invalidToken:{}, issuer:{}, statUrl:{}", invalidToken, issuer, statUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + statUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + invalidToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.info("Response for getStatsInvalidToken() - response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());

    }
	
	@Parameters({ "test.issuer", "statUrl" })
    @Test
    public void getStats(final String issuer, final String statUrl) {
        log.info("getStats() - accessToken:{}, issuer:{}, statUrl:{}", accessToken, issuer,
                statUrl);
		String adminToken = this.getAccessTokenForGivenScope(SUPER_ADMIN_SCOPE);
        log.info("getStats() - adminToken:{}, issuer:{}, statUrl:{}", adminToken, issuer, statUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + statUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + adminToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.info("Response for getStats() -  response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

    }


}
