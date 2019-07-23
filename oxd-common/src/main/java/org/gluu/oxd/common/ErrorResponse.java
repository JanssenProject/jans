/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.gluu.oxd.common.response.IOpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse implements Serializable, IOpResponse {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorResponse.class);

    @JsonProperty(value = "error")
    private String error;
    @JsonProperty(value = "error_description")
    private String error_description;
    @JsonProperty(value = "details")
    private JsonNode details;
    @JsonProperty(value = "reason")
    private JsonNode reason;

    public ErrorResponse() {
    }

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(ErrorResponseCode code) {
        this.error = code.getCode();
        this.error_description = code.getDescription();
    }

    public ErrorResponse(String error, String errorDescription) {
        this.error = error;
        this.error_description = errorDescription;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @JsonProperty(value = "error_description")
    public String getErrorDescription() {
        return error_description;
    }

    public void setErrorDescription(String error_description) {
        this.error_description = error_description;
    }

    public JsonNode getDetails() {
        return details;
    }

    public void setDetails(JsonNode details) {
        this.details = details;
    }

    public JsonNode getReason() {
        return reason;
    }

    public void setReason(JsonNode reason) {
        this.reason = reason;
    }

    public <T> T detailsAs(Class<T> p_class) {
        if (details != null && p_class != null) {
            final String asString = details.toString();
            try {
                return Jackson2.createJsonMapper().readValue(asString, p_class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            LOG.error("Unable to parse string to response, string: {}", asString);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", error_description='" + error_description + '\'' +
                ", details=" + details +
                ", reason=" + reason +
                '}';
    }
}
