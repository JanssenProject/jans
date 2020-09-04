package org.gluu.configapi.rest.resource;

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
                .patch("/api/v1/oxauth/config/properties")
                .then()
                .statusCode(200);
    }
}
