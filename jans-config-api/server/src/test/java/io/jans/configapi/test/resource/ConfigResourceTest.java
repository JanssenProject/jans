/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.resource;

import static io.restassured.RestAssured.given;
import io.jans.configapi.BaseTest;
import jakarta.ws.rs.core.MediaType;

import org.testng.annotations.Test;
import org.testng.annotations.Parameters;


public class ConfigResourceTest extends BaseTest{

    @Parameters({"issuer", "authConfigurationUrl"})
    @Test
    public void getAuthAppConfigurationProperty(final String issuer, final String authConfigurationUrl) {
        log.error("accessToken:{}, issuer:{}, authConfigurationUrl:{}", accessToken, issuer, authConfigurationUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", accessToken, null)
                .get(issuer+authConfigurationUrl).then().statusCode(200);
    }
    
    @Parameters({"issuer", "authConfigurationUrl"})
    @Test
    public void patchAppConfigurationProperty(final String issuer, final String authConfigurationUrl) {
        log.error("accessToken:{}, issuer:{}, authConfigurationUrl:{}", accessToken, issuer, authConfigurationUrl);
        given().when().contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", accessToken, null)
                .body("[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\": \"DEBUG\" } ]")
                .patch(issuer+authConfigurationUrl).then().statusCode(200);
    }
}
