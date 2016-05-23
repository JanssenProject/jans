/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class ErrorResponse implements Serializable {

    @JsonProperty(value = "error")
    private ErrorResponseCode error;
    @JsonProperty(value = "error_description")
    private String errorDescription;

    public ErrorResponse() {
    }

    public ErrorResponse(ErrorResponseCode p_error) {
        error = p_error;
    }

    public ErrorResponse(ErrorResponseCode p_error, String p_errorDescription) {
        error = p_error;
        errorDescription = p_errorDescription;
    }

    public ErrorResponseCode getError() {
        return error;
    }

    public void setError(ErrorResponseCode p_error) {
        error = p_error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String p_errorDescription) {
        errorDescription = p_errorDescription;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ResponseError");
        sb.append("{error=").append(error);
        sb.append(", errorDescription='").append(errorDescription).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
