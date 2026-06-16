/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class JsonProcessingExceptionMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @InjectMocks
    private JsonProcessingExceptionMapper mapper;

    @Mock
    private Logger log;

    @Mock
    private JsonProcessingException exception;

    @Test
    void toResponse_malformedJson_returnsFailedEnvelope() throws Exception {
        when(exception.getOriginalMessage()).thenReturn("Unexpected character");

        Response response = mapper.toResponse(exception);

        assertEquals(400, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());

        JsonNode node = MAPPER.readTree(String.valueOf(response.getEntity()));
        assertEquals("failed", node.get("status").asText());
        assertTrue(node.get("errorMessage").asText().contains("Unexpected character"));
    }
}
