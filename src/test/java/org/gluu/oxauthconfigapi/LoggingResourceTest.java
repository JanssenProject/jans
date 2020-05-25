package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

import org.gluu.oxauthconfigapi.rest.model.Logging;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("integration")
public class LoggingResourceTest {

	private static final String API_V1_OXAUTH_LOGGING = "/api/v1/oxauth/logging";

	@Test
	public void getLoggingSettings() {
		given().when().get(API_V1_OXAUTH_LOGGING).then().statusCode(200);
	}

	@Test
	void updateLoggingSettings() {
		Logging logging = new Logging();
		given().body(logging).header(CONTENT_TYPE, APPLICATION_JSON).header(ACCEPT, APPLICATION_JSON).when()
				.put(API_V1_OXAUTH_LOGGING).then().statusCode(OK.getStatusCode());
	}

}
