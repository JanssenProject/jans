package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import org.gluu.oxauthconfigapi.rest.model.UserInfo;

@QuarkusTest
@Tag("integration")
public class UserInfoResourceTest {
	
	private static final String API_V1_OXAUTH_USER_INFO = "/api/v1/oxauth/user_info";

	@Test
	public void getUserInfoConfiguration() {
		given().log().all()
		.when()
		.get(API_V1_OXAUTH_USER_INFO).then().statusCode(OK.getStatusCode());
	}
	
	@Test
	public void updateUserInfoConfiguration() {
		UserInfo userInfo = new UserInfo();
		given().log().all()
		.body(userInfo)
		.header(ACCEPT,APPLICATION_JSON)
		.header(CONTENT_TYPE,APPLICATION_JSON)
		.when()
	    .put(API_V1_OXAUTH_USER_INFO).then().statusCode(OK.getStatusCode());
	}
}
