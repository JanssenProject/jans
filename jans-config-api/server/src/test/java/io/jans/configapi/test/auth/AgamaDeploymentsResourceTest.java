/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import static io.restassured.RestAssured.given;
import io.jans.configapi.ConfigServerBaseTest;
import jakarta.ws.rs.core.MediaType;

import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class AgamaDeploymentsResourceTest extends ConfigServerBaseTest{

    @Parameters({"issuer", "agamaDeploymentUrl"})
    @Test
    public void getDeployments(final String issuer, final String agamaDeploymentUrl) {
        log.error("accessToken:{}, issuer:{}, agamaDeploymentUrl:{}", accessToken, issuer, agamaDeploymentUrl);
            given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", AUTHORIZATION_TYPE + " "+ accessToken, null)
                .get(issuer+agamaDeploymentUrl).then().statusCode(200);
    }
    
	
	
}
