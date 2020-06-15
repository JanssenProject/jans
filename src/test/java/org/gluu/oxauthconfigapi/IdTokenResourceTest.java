package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import org.gluu.oxauthconfigapi.rest.model.IdToken;

@QuarkusTest
@Tag("integration")
public class IdTokenResourceTest {
	
	private static final String API_V1_OXAUTH_IDTOKEN = "/api/v1/oxauth/idtoken";
	
	@Test
	public void getIdTokenConfiguration() {
		given().log().all()
		.when().get(API_V1_OXAUTH_IDTOKEN).then().statusCode(OK.getStatusCode());
	}
	
	@Test
	public void updateIdTokenConfiguration() {
		IdToken idToken = new IdToken();
		given().log().all()
		.body(idToken)
		.header(CONTENT_TYPE,APPLICATION_JSON)
		.header(ACCEPT,APPLICATION_JSON)
		.when()
		.put(API_V1_OXAUTH_IDTOKEN).then().statusCode(OK.getStatusCode());
	}

}
