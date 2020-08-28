package org.gluu.oxauthconfigapi.rest.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;

/**
 * @author Yuriy Zabrovarnyy
 */
@QuarkusTest
public class AuthConfigurationResourceTest {

    @Test
    public void patchAppConfigurationProperty() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body("[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\": \"DEBUG\" } ]")
                .patch("/api/v1/oxauth/config/properties")
                .then()
                .statusCode(200);
    }
}
