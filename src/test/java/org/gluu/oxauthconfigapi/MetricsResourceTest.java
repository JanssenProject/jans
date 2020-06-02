/**
 * 
 */
package org.gluu.oxauthconfigapi;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import org.gluu.oxauthconfigapi.rest.model.Metrics;

/**
 * @author Puja Sharma
 *
 */
@QuarkusTest
@Tag("integration")
public class MetricsResourceTest {
	
	private static final String API_V1_OXAUTH_METRICS = "/api/v1/oxauth/metrics";

	@Test
	public void getMetricsConfiguration() {
		given().log().all()
		.when()
		.get(API_V1_OXAUTH_METRICS).then().statusCode(OK.getStatusCode());
	}
	
	@Test
	public void updateMetricsConfiguration() {
		Metrics metrics = new Metrics();
		given().log().all()
		.body(metrics)
		.header(CONTENT_TYPE, APPLICATION_JSON)
		.header(ACCEPT,APPLICATION_JSON)
		.when()
		.put(API_V1_OXAUTH_METRICS).then().statusCode(OK.getStatusCode());
		
	}
}
