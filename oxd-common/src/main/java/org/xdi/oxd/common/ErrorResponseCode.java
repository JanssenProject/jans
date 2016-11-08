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
    BAD_REQUEST_NO_OXD_ID("bad_request", "oxd_id is empty or not specified or is otherwise invalid (not registered)."),
    BAD_REQUEST_NO_CODE("bad_request", "'code' is empty or not specified."),
    BAD_REQUEST_NO_STATE("bad_request", "'state' is empty or not specified."),
    BAD_REQUEST_STATE_NOT_VALID("bad_request", "'state' is not registered."),
    NO_ID_TOKEN_RETURNED("no_id_token", "id_token is not returned. Please check whether 'openid' scope is present for 'get_authorization_url' command"),
    NO_ACCESS_TOKEN_RETURNED("no_access_token", "access_token is not returned."),
    INVALID_NONCE("invalid_nonce", "Nonce value is not registered by oxd."),
    INVALID_OXD_ID("invalid_oxd_id", "Invalid oxd_id. Unable to find site for oxd_id. Please use register_site command for site registration."),
    INVALID_REQUEST("invalid_request", "Request is invalid. It doesn't contains all required parameters or otherwise is malformed."),
    INVALID_REQUEST_SCOPES_REQUIRED("invalid_request", "Request is invalid. Scopes are required parameter in request."),
    RPT_NOT_AUTHORIZED("rpt_not_authorized", "Unable to authorize RPT."),
    UNSUPPORTED_OPERATION("unsupported_operation", "Operation is not supported by server error."),
    INVALID_OP_HOST("invalid_op_host", "Invalid op_host (empty or blank)."),
    INVALID_AUTHORIZATION_REDIRECT_URI("invalid_authorization_redirect_uri", "Invalid authorization_redirect_uri (empty or blank)."),
    INVALID_SCOPE("invalid_scope", "Invalid scope parameter (empty or blank)."),
    INVALID_ACR_VALUES("invalid_acr_values", "Invalid acr_values parameter (empty or blank)."),
    NO_CONNECT_DISCOVERY_RESPONSE("no_connect_discovery_response", "Unable to fetch Connect discovery response /.well-known/openid-configuration"),
    NO_REGISTRATION_ENDPOINT("invalid_request", "OP does not support dynamic client registration. Please register client manually and provide client_id and client_secret to register_site command."),
    NO_UMA_DISCOVERY_RESPONSE("no_uma_discovery_response", "Unable to fetch UMA discovery response /.well-known/uma-configuration"),
    NO_UMA_RESOURCES_TO_PROTECT("invalid_uma_request", "Resources list to protect is empty or blank. Please check it according to protocol definition at https://www.gluu.org/docs-oxd"),
    NO_UMA_HTTP_METHOD("invalid_http_method", "http_method is not specified or otherwise not GET or POST or PUT or DELETE. Please check it according to protocol definition at https://www.gluu.org/docs-oxd"),
    NO_UMA_PATH_PARAMETER("invalid_path_parameter", "path parameter is not specified or otherwise not valid"),
    NO_UMA_TICKET_PARAMETER("invalid_ticket_parameter", "ticket parameter is not specified or otherwise is not valid"),
    NO_UMA_RPT_PARAMETER("invalid_rpt_parameter", "rpt parameter is not specified or otherwise is not valid"),
    FAILED_TO_GET_END_SESSION_ENDPOINT("no_end_session_endpoint_at_op", "OP does not provide end_session_endpoint at /.well-known/openid-configuration."),
    FAILED_TO_GET_REVOCATION_ENDPOINT("no_revocation_endpoint_at_op", "Failed to get revocation_endpoint at https://accounts.google.com/.well-known/openid-configuration"),
    FAILED_TO_GET_RPT("internal_error", "Failed to get RPT."),
    FAILED_TO_GET_GAT("internal_error", "Failed to get GAT.");

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
