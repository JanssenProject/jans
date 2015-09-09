/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public enum ErrorResponseCode {
    INTERNAL_ERROR("internal_error", "Internal server error occurs."),
    INVALID_REQUEST("invalid_request", "Request is invalid. It doesn't contains all required parameters or otherwise is malformed."),
    RPT_NOT_AUTHORIZED("rpt_not_authorized", "Unable to authorize RPT."),
    UNSUPPORTED_OPERATION("unsupported_operation", "Operation is not supported by server error.");

    private final String value;
    private final String description;

    private ErrorResponseCode(String p_value, String p_description) {
        value = p_value;
        description = p_description;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ErrorResponseCode fromValue(String v) {
        if (StringUtils.isNotBlank(v)) {
            for (ErrorResponseCode t : values()) {
                if (t.getValue().equalsIgnoreCase(v)) {
                    return t;
                }
            }
        }
        return null;
    }
}
