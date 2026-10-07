/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class ConstraintViolationExceptionMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @InjectMocks
    private ConstraintViolationExceptionMapper mapper;

    @Mock
    private Logger log;

    @Test
    void toResponse_validationFailure_returnsFailedEnvelopeWithMessage() throws Exception {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be null");
        Set<ConstraintViolation<?>> violations = Collections.singleton(violation);
        ConstraintViolationException ex = new ConstraintViolationException(violations);

        Response response = mapper.toResponse(ex);

        assertEquals(400, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());

        JsonNode node = MAPPER.readTree(String.valueOf(response.getEntity()));
        assertEquals("failed", node.get("status").asText());
        assertTrue(node.get("errorMessage").asText().contains("must not be null"));
    }

    @Test
    void toResponse_emptyViolations_returnsNonEmptyMessage() throws Exception {
        ConstraintViolationException ex = new ConstraintViolationException(Collections.emptySet());

        Response response = mapper.toResponse(ex);

        JsonNode node = MAPPER.readTree(String.valueOf(response.getEntity()));
        assertEquals("failed", node.get("status").asText());
        assertTrue(node.get("errorMessage").asText().trim().length() > 0);
    }
}
