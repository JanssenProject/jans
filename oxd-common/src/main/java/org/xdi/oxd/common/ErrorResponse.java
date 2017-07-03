/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.response.IOpResponse;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ErrorResponse implements Serializable, IOpResponse {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorResponse.class);

    @JsonProperty(value = "error")
    private String error;
    @JsonProperty(value = "error_description")
    private String errorDescription;
    @JsonProperty(value = "details")
    private JsonNode details;

    public ErrorResponse() {
    }

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(ErrorResponseCode code) {
        this.error = code.getCode();
        this.errorDescription = code.getDescription();
    }

    public ErrorResponse(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public JsonNode getDetails() {
        return details;
    }

    public void setDetails(JsonNode details) {
        this.details = details;
    }

    public <T> T detailsAs(Class<T> p_class) {
        if (details != null && p_class != null) {
            final String asString = details.toString();
            try {
                return CoreUtils.createJsonMapper().readValue(asString, p_class);
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
                ", errorDescription='" + errorDescription + '\'' +
                ", details=" + details +
                '}';
    }
}
