/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link ErrorResponseFactory} emits the FIDO2 conformance failure envelope
 * ({@code {status:"failed", errorMessage:"..."}}) instead of the legacy OAuth2 error shape
 * ({@code {error, error_description, reason}}).
 */
class ErrorResponseFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ErrorResponseFactory factory = new ErrorResponseFactory();

    private JsonNode entityJson(WebApplicationException ex) throws Exception {
        Object entity = ex.getResponse().getEntity();
        return MAPPER.readTree(String.valueOf(entity));
    }

    @Test
    void invalidRequest_producesFailedEnvelopeNotOauthShape() throws Exception {
        WebApplicationException ex = factory.invalidRequest("bad input");
        JsonNode node = entityJson(ex);

        assertEquals("failed", node.get("status").asText());
        assertEquals("bad input", node.get("errorMessage").asText());
        // must NOT be the old OAuth2 shape
        assertFalse(node.has("error"));
        assertFalse(node.has("error_description"));
        assertFalse(node.has("reason"));
    }

    @Test
    void badRequestException_producesFailedEnvelopeWithReason() throws Exception {
        WebApplicationException ex = factory.badRequestException(CommonErrorResponseType.INVALID_REQUEST, "missing username");
        JsonNode node = entityJson(ex);

        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("failed", node.get("status").asText());
        assertEquals("missing username", node.get("errorMessage").asText());
    }

    @Test
    void failedEnvelope_alwaysHasNonEmptyErrorMessage() throws Exception {
        // reason null -> falls back to the error description / type, never an empty errorMessage
        WebApplicationException ex = factory.badRequestException(CommonErrorResponseType.INVALID_REQUEST, null);
        JsonNode node = entityJson(ex);

        assertEquals("failed", node.get("status").asText());
        assertFalse(node.get("errorMessage").asText().trim().isEmpty());
    }
}
