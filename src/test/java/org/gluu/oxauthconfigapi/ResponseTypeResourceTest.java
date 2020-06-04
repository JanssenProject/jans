package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

import javax.annotation.meta.When;

import org.gluu.oxauthconfigapi.rest.model.ResponseType;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
/**
 * @author Puja Sharma
 *
 */
@QuarkusTest
@Tag("integration")
public class ResponseTypeResourceTest {

	private static final String API_V1_OXAUTH_RESPONSES_TYPES = "/api/v1/oxauth/responses_types";
	
	@Test
	public void getSupportedResponseTypes() {
		given().log().all()
		.when()
		.get(API_V1_OXAUTH_RESPONSES_TYPES).then().statusCode(OK.getStatusCode());
	}
	
	@Test
	public void updateSupportedResponseTypes() {
		ResponseType responseType = new ResponseType();
		given().log().all()
		.body(responseType)
		.header(ACCEPT,APPLICATION_JSON)
		.header(CONTENT_TYPE,APPLICATION_JSON)
		.when()
		.put(API_V1_OXAUTH_RESPONSES_TYPES).then().statusCode(OK.getStatusCode());
	}
}
