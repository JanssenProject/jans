package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import org.gluu.oxauthconfigapi.rest.model.ResponseMode;

@QuarkusTest
@Tag("integration")
public class ResponseModeResourceTest {
	
	private static final String API_V1_OXAUTH_RESPONSES_MODES = "/api/v1/oxauth/responses_modes";
	
	@Test
	public void getSupportedResponseMode() {
		given().log().all()
		.when()
		.get(API_V1_OXAUTH_RESPONSES_MODES).then().statusCode(OK.getStatusCode());
	}
	
	@Test
	public void updateSupportedResponseMode() {
		ResponseMode responseMode = new ResponseMode();
		given().log().all()
		.body(responseMode)
		.header(ACCEPT,APPLICATION_JSON)
		.header(CONTENT_TYPE,APPLICATION_JSON)
		.when()
		.put(API_V1_OXAUTH_RESPONSES_MODES).then().statusCode(OK.getStatusCode());
	}

}
