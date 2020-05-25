package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("integration")
public class LoggingResourceTest {
	
	
	
	@Test
	public void getLoggingSettingst() {
		given().when().get("/api/v1/oxauth/logging").then().statusCode(200);
	}

}
