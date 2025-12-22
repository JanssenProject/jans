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

import io.jans.configapi.util.ApiAccessConstants;
import jakarta.ws.rs.client.Invocation.Builder;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class AssetResourceTest extends ConfigServerBaseTest {
	
	private static final String INVALID_SCOPE = ApiAccessConstants.ATTRIBUTES_READ_ACCESS;
	private static final String ASSET_ADMIN_SCOPE = ApiAccessConstants.ASSET_ADMIN_ACCESS;

    @Parameters({ "test.issuer", "assetUrl" })
    @Test
    public void getAssetsUsingInvalidToken(final String issuer, final String assetUrl) {
        log.info("getAssetsUsingInvalidToken() - accessToken:{}, issuer:{}, assetUrl:{}", accessToken, issuer,
                assetUrl);
		String invalidToken = this.getAccessTokenForGivenScope(INVALID_SCOPE);
        log.info("getAssetsUsingInvalidToken() - invalidToken:{}, issuer:{}, assetUrl:{}", invalidToken, issuer, assetUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + assetUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + invalidToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.info("Response for getAssetsUsingInvalidToken() -  response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());

    }
	
	@Parameters({ "test.issuer", "assetUrl" })
    @Test
    public void getAssets(final String issuer, final String assetUrl) {
        log.info("getAssets() - accessToken:{}, issuer:{}, assetUrl:{}", accessToken, issuer,
                assetUrl);
		String adminToken = this.getAccessTokenForGivenScope(ASSET_ADMIN_SCOPE);
        log.info("getAssets() - adminToken:{}, issuer:{}, assetUrl:{}", adminToken, issuer, assetUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + assetUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + adminToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.info("Response for getAssets() -  response:{}, response.getStatus():{}", response,
                response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

    }


}
