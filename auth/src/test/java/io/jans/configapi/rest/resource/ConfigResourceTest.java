/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;

/**
 * @author Yuriy Zabrovarnyy
 */
@QuarkusTest
public class ConfigResourceTest {

    @Test
    public void patchAppConfigurationProperty() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON)
                .header("Authorization", "Bearer 12345", null)
                .body("[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\": \"DEBUG\" } ]")
                .patch("/jans-config-api/api/v1/jans-auth-server/config")
                .then()
                .statusCode(200);
    }
}
