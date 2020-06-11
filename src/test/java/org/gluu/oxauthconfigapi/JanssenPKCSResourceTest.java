package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import org.gluu.oxauthconfigapi.rest.model.JanssenPKCS;
/**
 * @author Puja Sharma
 *
 */
@QuarkusTest
@Tag("integration")
public class JanssenPKCSResourceTest {

	private static final String API_V1_OXAUTH_JANSSENPKCS = "/api/v1/oxauth/janssenpkcs";
	
	@Test
	public void getJanssenPKCSConfiguration() {
		given().log().all()
		.when()
		.get(API_V1_OXAUTH_JANSSENPKCS).then().statusCode(OK.getStatusCode());
	}
	
	@Test
	public void updateJanssenPKCSConfiguration() {
		JanssenPKCS janssenPKCS = new JanssenPKCS();
		given().log().all()
		.body(janssenPKCS)
		.header(CONTENT_TYPE,APPLICATION_JSON)
		.header(ACCEPT,APPLICATION_JSON)
		.when()
		.put(API_V1_OXAUTH_JANSSENPKCS).then().statusCode(OK.getStatusCode());
	}





}
