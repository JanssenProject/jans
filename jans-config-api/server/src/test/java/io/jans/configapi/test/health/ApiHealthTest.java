/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.health;

import static io.restassured.RestAssured.given;
import io.jans.configapi.BaseTest;
import jakarta.ws.rs.core.MediaType;

import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class ApiHealthTest extends BaseTest{

    @Parameters({"issuer", "healthUrl"})
    @Test
    public void getHealthResponse(final String issuer, final String healthUrl) {
        log.error("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", accessToken, null)
                .get(issuer+healthUrl).then().statusCode(200);
    }
    
	
	@Parameters({"issuer", "healthUrl"})
    @Test
    public void getServerStat(final String issuer, final String healthUrl) {
        log.error("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", accessToken, null)
                .get(issuer+healthUrl+"/server-stat").then().statusCode(200);
    }
	
	@Parameters({"issuer", "healthUrl"})
    @Test
    public void getApplicationVersion(final String issuer, final String healthUrl) {
        log.error("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", accessToken, null)
                .get(issuer+healthUrl+"/app-version").then().statusCode(200);
    }
	
	@Parameters({"issuer", "healthUrl"})
    @Test
    public void getServiceStatus(final String issuer, final String healthUrl) {
        log.error("accessToken:{}, issuer:{}, healthUrl:{}", accessToken, issuer, healthUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", accessToken, null)
                .get(issuer+healthUrl+"/service-status").then().statusCode(200);
    }
}
