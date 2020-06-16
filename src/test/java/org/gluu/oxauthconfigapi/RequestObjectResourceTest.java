package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import org.checkerframework.checker.units.qual.A;
import org.gluu.oxauthconfigapi.rest.model.RequestObject;

@QuarkusTest
@Tag("integration")
public class RequestObjectResourceTest {
	
	private static final String API_V1_OXAUTH_REQUEST_OBJECT = "/api/v1/oxauth/request_object";
	
	@Test
	public void getRequestObjectConfiguration() {
		given().log().all()
		.when()
		.get(API_V1_OXAUTH_REQUEST_OBJECT).then().statusCode(OK.getStatusCode());
	}
	
	@Test
	public void updateRequestObjectConfiguration() {
		RequestObject requestObject = new RequestObject();
		given().log().all()
		.body(requestObject)
		.header(ACCEPT,APPLICATION_JSON)
		.header(CONTENT_TYPE,APPLICATION_JSON)
		.when()
		.put(API_V1_OXAUTH_REQUEST_OBJECT).then().statusCode(OK.getStatusCode());
	}

}
