/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import static io.restassured.RestAssured.given;
import io.jans.configapi.BaseTest;
import jakarta.ws.rs.core.MediaType;

import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class AcrsResourceTest extends BaseTest{

    @Parameters({"issuer", "acrsUrl"})
    @Test
    public void getDefaultAuthenticationMethod(final String issuer, final String acrsUrl) {
        log.error("accessToken:{}, issuer:{}, acrsUrl:{}", accessToken, issuer, acrsUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", accessToken, null)
                .get(issuer+acrsUrl).then().statusCode(200);
    }
    
	
	@Parameters({"issuer", "acrsUrl", "default_acr1"})
    @Test
    public void postClient(final String issuer, final String openidClientsUrl, final String json) {
        log.error("accessToken:{}, issuer:{}, openidClientsUrl:{}, json:{}", accessToken, issuer, openidClientsUrl, json);
        log.info("Creating client using json string");

         given().when().contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", accessToken, null)
                .body(json)
                .put(issuer+acrsUrl).then().statusCode(200);
    }
}
