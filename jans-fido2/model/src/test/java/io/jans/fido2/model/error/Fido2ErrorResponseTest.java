/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Verifies the FIDO2 conformance response envelope {@code {status, errorMessage}}.
 */
class Fido2ErrorResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode parse(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    @Test
    void okEnvelope_hasOkStatusAndEmptyMessage() throws Exception {
        JsonNode node = parse(new Fido2ErrorResponse(Fido2ErrorResponse.STATUS_OK, "").toJson());
        assertEquals("ok", node.get("status").asText());
        assertEquals("", node.get("errorMessage").asText());
    }

    @Test
    void failed_setsFailedStatusAndKeepsMessage() throws Exception {
        JsonNode node = parse(Fido2ErrorResponse.failed("Username is a mandatory parameter").toJson());
        assertEquals("failed", node.get("status").asText());
        assertEquals("Username is a mandatory parameter", node.get("errorMessage").asText());
    }

    @Test
    void failed_withBlankMessage_fallsBackToNonEmptyMessage() throws Exception {
        JsonNode node = parse(Fido2ErrorResponse.failed("   ").toJson());
        assertEquals("failed", node.get("status").asText());
        assertFalse(node.get("errorMessage").asText().trim().isEmpty(),
                "conformance requires a non-empty errorMessage on failure");
    }

    @Test
    void failed_withNullMessage_fallsBackToNonEmptyMessage() throws Exception {
        JsonNode node = parse(Fido2ErrorResponse.failed(null).toJson());
        assertEquals("failed", node.get("status").asText());
        assertFalse(node.get("errorMessage").asText().trim().isEmpty());
    }

    @Test
    void toJson_escapesQuotesAndBackslashes() throws Exception {
        String tricky = "bad \"json\" \\x";
        JsonNode node = parse(Fido2ErrorResponse.failed(tricky).toJson());
        // round-trips back to the exact original string
        assertEquals(tricky, node.get("errorMessage").asText());
    }

    @Test
    void toJson_onlyContainsEnvelopeFields() throws Exception {
        JsonNode node = parse(Fido2ErrorResponse.failed("x").toJson());
        assertTrue(node.has("status"));
        assertTrue(node.has("errorMessage"));
        assertEquals(2, node.size(), "envelope must not leak extra fields");
    }
}
