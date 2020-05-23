package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HealthCheckTest {

	@Test
	public void testHelloEndpoint() {
		given().when().get("/api/v1/health").then().statusCode(200);
	}

}