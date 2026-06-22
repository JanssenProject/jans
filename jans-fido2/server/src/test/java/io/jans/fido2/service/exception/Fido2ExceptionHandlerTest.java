/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.fido2.exception.Fido2RuntimeException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class Fido2ExceptionHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @InjectMocks
    private Fido2ExceptionHandler handler;

    @Mock
    private Logger log;

    @Test
    void toResponse_returnsFailedEnvelopeAsJson() throws Exception {
        Response response = handler.toResponse(new Fido2RuntimeException("Invalid challenge"));

        assertEquals(400, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());

        JsonNode node = MAPPER.readTree(String.valueOf(response.getEntity()));
        assertEquals("failed", node.get("status").asText());
        assertEquals("Invalid challenge", node.get("errorMessage").asText());
    }

    @Test
    void toResponse_withNullMessage_stillHasNonEmptyErrorMessage() throws Exception {
        Response response = handler.toResponse(new Fido2RuntimeException((String) null));

        JsonNode node = MAPPER.readTree(String.valueOf(response.getEntity()));
        assertEquals("failed", node.get("status").asText());
        assertFalse(node.get("errorMessage").asText().trim().isEmpty());
    }
}
