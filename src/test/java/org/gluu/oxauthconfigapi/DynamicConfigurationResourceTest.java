package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import org.gluu.oxauthconfigapi.rest.model.DynamicConfiguration;

@QuarkusTest
@Tag("integration")
public class DynamicConfigurationResourceTest {
	
	private static final String API_V1_OXAUTH_DYN_REGISTRATION = "/api/v1/oxauth/dyn_registration";
	
	@Test
	public void getDynamicConfiguration() {
		given().log().all()
		.when()
		.get(API_V1_OXAUTH_DYN_REGISTRATION).then().statusCode(OK.getStatusCode());
	}

	@Test
	public void updateDynamicConfiguration() {
		DynamicConfiguration dynamicConfiguration = new DynamicConfiguration();
		given().log().all()
		.body(dynamicConfiguration)
		.header(ACCEPT,APPLICATION_JSON)
		.header(CONTENT_TYPE,APPLICATION_JSON)
		.when()
		.put(APPLICATION_JSON).then().statusCode(OK.getStatusCode());
	}
	
}
