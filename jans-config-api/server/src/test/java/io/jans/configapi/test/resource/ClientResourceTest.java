/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.resource;

import static io.restassured.RestAssured.given;
import io.jans.configapi.BaseTest;
import io.jans.model.net.HttpServiceResponse;
import jakarta.ws.rs.core.MediaType;

import static org.testng.Assert.*;

import java.util.Map;

import org.apache.http.entity.ContentType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


public class ClientResourceTest extends BaseTest{

    private String clientId;
    
    @Parameters({"issuer", "openidClientsUrl"})
    @Test
    public void getClients(final String issuer, final String openidClientsUrl) {
        log.error("accessToken:{}, issuer:{}, openidClientsUrl:{}", accessToken, issuer, openidClientsUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", accessToken, null)
                .get(issuer+openidClientsUrl).then().statusCode(200);
    }
    
    @Parameters({"issuer", "openidClientsUrl", "openid_client1"})
    @Test
    public void postClient(final String issuer, final String openidClientsUrl, final String json) {
        log.error("accessToken:{}, issuer:{}, openidClientsUrl:{}, json:{}", accessToken, issuer, openidClientsUrl, json);
        log.info("Creating client using json string");

        HttpServiceResponse httpServiceResponse = getHttpService().executePost(issuer+openidClientsUrl, "Basic ", null, json,
                ContentType.APPLICATION_JSON, null);
        assertFalse(httpServiceResponse==null);
        
        int statusCode = httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode();

        Status status = Status.fromStatusCode(statusCode);
        
        assertEquals(status, Status.OK.getStatusCode());
    }
}
