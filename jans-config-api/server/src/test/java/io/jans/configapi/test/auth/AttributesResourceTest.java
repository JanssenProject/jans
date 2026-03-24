/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc.
 */

package io.jans.configapi.test.auth;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.configapi.ConfigServerBaseTest;
import io.jans.configapi.core.configuration.ObjectMapperContextResolver;
import io.jans.configapi.core.test.listener.SkipTest;
import io.jans.model.GluuAttributeUsageType;
import io.jans.model.JansAttribute;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@SkipTest(databases = { "LDAP", "COUCHBASE", "SPANNER" })
public class AttributesResourceTest extends ConfigServerBaseTest {

    private static final String ATTRIBUTE_NAME = "departmentNumber";
    private static final String USAGE_TYPE_PATCH = "[{\"op\":\"add\",\"path\":\"/usageType\",\"value\":[\"openid\"]}]";

    @Parameters({ "test.issuer", "attributesUrl" })
    @Test
    public void putAndPatchUsageTypeRoundTrip(final String issuer, final String attributesUrl) throws Exception {
        ObjectMapper objectMapper = ObjectMapperContextResolver.createDefaultMapper();
        JansAttribute originalAttribute = getAttributeByName(issuer, attributesUrl, ATTRIBUTE_NAME, objectMapper);
        JansAttribute restoreAttribute = copyAttribute(objectMapper, originalAttribute);

        try {
            JansAttribute putRequest = copyAttribute(objectMapper, originalAttribute);
            putRequest.setUsageType(new GluuAttributeUsageType[] { GluuAttributeUsageType.OPENID });

            JansAttribute updatedAfterPut = putAttribute(issuer, attributesUrl, putRequest, objectMapper);
            assertUsageType(updatedAfterPut);

            JansAttribute patchedAttribute = patchAttribute(
                    issuer,
                    attributesUrl,
                    originalAttribute.getInum(),
                    USAGE_TYPE_PATCH,
                    objectMapper);
            assertUsageType(patchedAttribute);

            JansAttribute fetchedAfterPatch = getAttributeByInum(
                    issuer,
                    attributesUrl,
                    originalAttribute.getInum(),
                    objectMapper);
            assertUsageType(fetchedAfterPatch);
        } finally {
            putAttribute(issuer, attributesUrl, restoreAttribute, objectMapper);
        }
    }

    private JansAttribute getAttributeByName(
            String issuer,
            String attributesUrl,
            String attributeName,
            ObjectMapper objectMapper) throws IOException {
        String url = issuer
                + attributesUrl
                + "?pattern="
                + URLEncoder.encode(attributeName, StandardCharsets.UTF_8)
                + "&limit=10";

        Builder request = authorizedJsonRequest(url);
        try (Response response = request.get()) {
            String responseBody = response.readEntity(String.class);
            assertEquals(response.getStatus(), Status.OK.getStatusCode(), responseBody);

            JsonNode rootNode = objectMapper.readTree(responseBody);
            for (JsonNode entryNode : rootNode.path("entries")) {
                if (attributeName.equals(entryNode.path("name").asText())) {
                    return objectMapper.treeToValue(entryNode, JansAttribute.class);
                }
            }
            throw new IllegalStateException("Failed to find attribute '" + attributeName + "' in response: " + responseBody);
        }
    }

    private JansAttribute getAttributeByInum(
            String issuer,
            String attributesUrl,
            String inum,
            ObjectMapper objectMapper) throws IOException {
        Builder request = authorizedJsonRequest(issuer + attributesUrl + "/" + inum);
        try (Response response = request.get()) {
            String responseBody = response.readEntity(String.class);
            assertEquals(response.getStatus(), Status.OK.getStatusCode(), responseBody);
            return objectMapper.readValue(responseBody, JansAttribute.class);
        }
    }

    private JansAttribute putAttribute(
            String issuer,
            String attributesUrl,
            JansAttribute attribute,
            ObjectMapper objectMapper) throws IOException {
        Builder request = authorizedJsonRequest(issuer + attributesUrl);
        String payload = objectMapper.writeValueAsString(attribute);
        try (Response response = request.put(Entity.entity(payload, MediaType.APPLICATION_JSON))) {
            String responseBody = response.readEntity(String.class);
            assertEquals(response.getStatus(), Status.OK.getStatusCode(), responseBody);
            return objectMapper.readValue(responseBody, JansAttribute.class);
        }
    }

    private JansAttribute patchAttribute(
            String issuer,
            String attributesUrl,
            String inum,
            String payload,
            ObjectMapper objectMapper) throws IOException {
        Builder request = authorizedJsonRequest(issuer + attributesUrl + "/" + inum);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);
        try (Response response = request.method(HttpMethod.PATCH, Entity.entity(payload, MediaType.APPLICATION_JSON_PATCH_JSON))) {
            String responseBody = response.readEntity(String.class);
            assertEquals(response.getStatus(), Status.OK.getStatusCode(), responseBody);
            return objectMapper.readValue(responseBody, JansAttribute.class);
        }
    }

    private Builder authorizedJsonRequest(String url) {
        Builder request = getResteasyService().getClientBuilder(url);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return request;
    }

    private JansAttribute copyAttribute(ObjectMapper objectMapper, JansAttribute attribute) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(attribute), JansAttribute.class);
    }

    private void assertUsageType(JansAttribute attribute) {
        assertNotNull(attribute.getUsageType(), "usageType must be present");
        assertEquals(attribute.getUsageType().length, 1, "usageType must contain one value");
        assertEquals(attribute.getUsageType()[0], GluuAttributeUsageType.OPENID);
    }
}
