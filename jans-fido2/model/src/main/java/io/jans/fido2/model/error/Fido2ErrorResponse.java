/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * FIDO2 server response envelope required by the FIDO Alliance conformance test
 * suite. Every attestation/assertion response must carry a {@code status} of
 * {@code "ok"} or {@code "failed"}; on failure {@code errorMessage} must be a
 * non-empty string describing the problem.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "status", "errorMessage" })
public class Fido2ErrorResponse {

    public static final String STATUS_OK = "ok";
    public static final String STATUS_FAILED = "failed";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String status;
    private final String errorMessage;

    public Fido2ErrorResponse() {
        this(STATUS_FAILED, "");
    }

    public Fido2ErrorResponse(String status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }

    /**
     * Builds a {@code failed} envelope, guaranteeing a non-empty errorMessage as
     * the conformance suite requires.
     */
    public static Fido2ErrorResponse failed(String message) {
        String safeMessage = (message == null || message.trim().isEmpty()) ? "Request failed" : message;
        return new Fido2ErrorResponse(STATUS_FAILED, safeMessage);
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            // Last-resort hand-built envelope so we never leak a serialization failure to the client.
            String safe = errorMessage == null ? "" : errorMessage.replace("\\", "\\\\").replace("\"", "\\\"");
            return "{\"status\":\"" + status + "\",\"errorMessage\":\"" + safe + "\"}";
        }
    }
}
