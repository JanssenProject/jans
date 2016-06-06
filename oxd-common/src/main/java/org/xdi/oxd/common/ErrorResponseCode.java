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

    INTERNAL_ERROR_UNKNOWN("internal_error", "Unknown internal server error occurs."),
    INTERNAL_ERROR_NO_PARAMS("internal_error", "Command parameters are not specified or otherwise malformed."),
    BAD_REQUEST_NO_OXD_ID("bad_request", "oxd_id is empty or not specified."),
    INVALID_OXD_ID("invalid_oxd_id", "Invalid oxd_id. Unable to find site for oxd_id. Please use register_site command for site registration."),
    INVALID_REQUEST("invalid_request", "Request is invalid. It doesn't contains all required parameters or otherwise is malformed."),
    RPT_NOT_AUTHORIZED("rpt_not_authorized", "Unable to authorize RPT."),
    UNSUPPORTED_OPERATION("unsupported_operation", "Operation is not supported by server error."),
    INVALID_OP_HOST("invalid_op_host", "Invalid op_host (empty or blank)."),
    NO_CONNECT_DISCOVERY_RESPONSE("no_connect_discovery_response", "Unable to fetch Connect discovery response /.well-known/openid-configuration"),
    NO_UMA_DISCOVERY_RESPONSE("no_uma_discovery_response", "Unable to fetch UMA discovery response /.well-known/uma-configuration"),
    NO_UMA_RESOURCES_TO_PROTECT("invalid_uma_request", "Resources list to protect is empty or blank. Please check it according to protocol definition at https://www.gluu.org/docs-oxd");

    private final String code;
    private final String description;

    private ErrorResponseCode(String p_value, String p_description) {
        code = p_value;
        description = p_description;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ErrorResponseCode fromValue(String v) {
        if (StringUtils.isNotBlank(v)) {
            for (ErrorResponseCode t : values()) {
                if (t.getCode().equalsIgnoreCase(v)) {
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ErrorResponseCode");
        sb.append("{value='").append(code).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
