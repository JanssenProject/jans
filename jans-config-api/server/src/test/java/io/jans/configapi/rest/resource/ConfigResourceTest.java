/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource;

import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.MediaType;
import static io.restassured.RestAssured.given;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ConfigResourceTest {

    @Test
    public void patchAppConfigurationProperty() {
        given().when().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer 0ea2ce99-b741-4f5a-8fd7-26f52d057c19", null)
                .body("[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\": \"DEBUG\" } ]")
                .patch("/jans-config-api/api/v1/jans-auth-server/config").then().statusCode(200);
    }
}
