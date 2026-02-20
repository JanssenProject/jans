/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.test;

import io.jans.configapi.plugin.shibboleth.ShibbolethBaseTest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class ShibbolethResourceTest extends ShibbolethBaseTest {

    @Test(priority = 1)
    public void getConfiguration_shouldReturnConfiguration() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .get(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_CONFIG_ENDPOINT)
                .then()
                .extract()
                .response();

        log.info("getConfiguration response.getStatusCode():{}", response.getStatusCode());
        assertEquals(response.getStatusCode(), 200, "GET /shibboleth/config should return 200");
        
        String body = response.getBody().asString();
        assertNotNull(body, "Response body should not be null");
        assertFalse(body.isEmpty(), "Response body should not be empty");
        
        io.restassured.path.json.JsonPath json = response.jsonPath();
        assertNotNull(json.get("enabled"), "Configuration should have 'enabled' field");
    }

    @Test(priority = 2)
    public void getTrustedServiceProviders_shouldReturnList() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .get(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_TRUST_ENDPOINT)
                .then()
                .extract()
                .response();

        log.info("getTrustedServiceProviders response.getStatusCode():{}", response.getStatusCode());
        assertEquals(response.getStatusCode(), 200, "GET /shibboleth/trust should return 200");
        
        String body = response.getBody().asString();
        assertNotNull(body, "Response body should not be null");
        assertTrue(body.startsWith("["), "Response should be a JSON array");
        
        java.util.List<?> list = response.jsonPath().getList("$");
        assertNotNull(list, "Response should be parseable as list");
        log.info("Found {} trusted service providers", list.size());
    }

    @Test(priority = 3)
    public void createTrustedServiceProvider_shouldReturn201() {
        String testEntityId = "https://test-sp-" + System.currentTimeMillis() + ".example.org";
        String spJson = String.format("""
                {
                    "entityId": "%s",
                    "name": "Test Service Provider",
                    "description": "Integration test SP",
                    "enabled": true,
                    "releasedAttributes": ["uid", "mail"],
                    "signAssertions": true
                }
                """, testEntityId);

        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .body(spJson)
                .when()
                .post(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_TRUST_ENDPOINT)
                .then()
                .extract()
                .response();

        log.info("createTrustedServiceProvider response.getStatusCode():{}", createResponse.getStatusCode());
        assertEquals(createResponse.getStatusCode(), 201, "POST /shibboleth/trust should return 201");
        
        String body = createResponse.getBody().asString();
        assertNotNull(body, "Response body should not be null");
        
        io.restassured.path.json.JsonPath json = createResponse.jsonPath();
        assertEquals(json.getString("entityId"), testEntityId, "Response entityId should match");
        assertEquals(json.getString("name"), "Test Service Provider", "Response name should match");
        assertTrue(json.getBoolean("enabled"), "Response enabled should be true");
        
        cleanupTestSp(testEntityId);
    }

    @Test(priority = 4)
    public void getTrustedServiceProvider_nonExistent_shouldReturn404() {
        String nonExistentId = "https://non-existent-" + System.currentTimeMillis() + ".example.org";
        
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .get(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_TRUST_ENDPOINT + "/" + nonExistentId)
                .then()
                .extract()
                .response();

        log.info("getTrustedServiceProvider nonExistent response.getStatusCode():{}", response.getStatusCode());
        assertEquals(response.getStatusCode(), 404, "GET non-existent SP should return 404");
    }

    @Test(priority = 5)
    public void deleteTrustedServiceProvider_nonExistent_shouldReturn404() {
        String nonExistentId = "https://non-existent-delete-" + System.currentTimeMillis() + ".example.org";
        
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_TRUST_ENDPOINT + "/" + nonExistentId)
                .then()
                .extract()
                .response();

        log.info("deleteTrustedServiceProvider nonExistent response.getStatusCode():{}", response.getStatusCode());
        assertEquals(response.getStatusCode(), 404, "DELETE non-existent SP should return 404");
    }

    @Test(priority = 6)
    public void updateConfiguration_shouldReturn200() {
        Response getResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .get(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_CONFIG_ENDPOINT)
                .then()
                .extract()
                .response();

        assertEquals(getResponse.getStatusCode(), 200, "GET config should succeed before update");
        
        String configJson = """
                {
                    "entityId": "https://test-idp.example.com/idp/shibboleth",
                    "scope": "example.com",
                    "enabled": true,
                    "signingKeyAlias": "idp-signing",
                    "encryptionKeyAlias": "idp-encryption",
                    "jansAuthEnabled": true,
                    "jansAuthScopes": "openid,profile,email"
                }
                """;

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .body(configJson)
                .when()
                .put(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_CONFIG_ENDPOINT)
                .then()
                .extract()
                .response();

        log.info("updateConfiguration response.getStatusCode():{}", response.getStatusCode());
        assertEquals(response.getStatusCode(), 200, "PUT /shibboleth/config should return 200");
        
        String body = response.getBody().asString();
        assertTrue(body.contains("example.com"), "Response should contain updated scope");
    }

    @Test(priority = 7)
    public void getIdpMetadata_shouldReturn200Or501() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", MediaType.APPLICATION_XML)
                .when()
                .get(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_METADATA_ENDPOINT)
                .then()
                .extract()
                .response();

        log.info("getIdpMetadata response.getStatusCode():{}", response.getStatusCode());
        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 501,
                "Metadata endpoint should return 200 (implemented) or 501 (not yet implemented)");
    }

    private void cleanupTestSp(String entityId) {
        try {
            given()
                    .header("Authorization", "Bearer " + accessToken)
                    .when()
                    .delete(propertiesMap.get("shibbolethUrl") + SHIBBOLETH_TRUST_ENDPOINT + "/" + entityId);
        } catch (Exception e) {
            log.warn("Cleanup failed for SP: {}", entityId);
        }
    }
}
