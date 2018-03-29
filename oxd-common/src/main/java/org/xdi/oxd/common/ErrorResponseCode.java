/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;

import java.util.Arrays;

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
    NO_ID_TOKEN_PARAM("no_id_token", "id_token is not provided in request to oxd."),
    NO_ACCESS_TOKEN_RETURNED("no_access_token", "access_token is not returned."),
    INVALID_NONCE("invalid_nonce", "Nonce value is not registered by oxd."),
    INVALID_STATE("invalid_state", "State value is not registered by oxd."),
    INVALID_ID_TOKEN("invalid_id_token", "id_token is invalid."),
    INVALID_ID_TOKEN_BAD_NONCE("invalid_id_token_bad_nonce", "Invalid id_token. Nonce value from token does not match nonce from request."),
    INVALID_ID_TOKEN_BAD_AUDIENCE("invalid_id_token_bad_audience", "Invalid id_token. Audience value from token does not match audience from request."),
    INVALID_ID_TOKEN_EXPIRED("invalid_id_token_expired", "Invalid id_token. id_token expired."),
    INVALID_ID_TOKEN_BAD_ISSUER("invalid_id_token_bad_issuer", "Invalid id_token. Bad issuer."),
    INVALID_ID_TOKEN_BAD_SIGNATURE("invalid_id_token_bad_signature", "Invalid id_token. Bad signature."),
    INVALID_ID_TOKEN_UNKNOWN("invalid_id_token_unknown", "Invalid id_token, validation fail due to exception, please check oxd-server.log for details."),
    INVALID_ACCESS_TOKEN_BAD_HASH("invalid_access_token_bad_hash", "access_token is invalid. Hash of access_token does not match hash from id_token (at_hash)."),
    INVALID_AUTHORIZATION_CODE_BAD_HASH("invalid_authorization_code_bad_hash", "Authorization code is invalid. Hash of authorization code does not match hash from id_token (c_hash)."),
    INVALID_REGISTRATION_CLIENT_URL("invalid_registration_client_url", "Registration client URL is invalid. Please check registration_client_url response parameter from IDP (http://openid.net/specs/openid-connect-registration-1_0.html#RegistrationResponse)."),
    INVALID_OXD_ID("invalid_oxd_id", "Invalid oxd_id. Unable to find site for oxd_id. It does not exist or removed from the server. Please use register_site command to register a site."),
    INVALID_REQUEST("invalid_request", "Request is invalid. It doesn't contains all required parameters or otherwise is malformed."),
    INVALID_REQUEST_SCOPES_REQUIRED("invalid_request", "Request is invalid. Scopes are required parameter in request."),
    RPT_NOT_AUTHORIZED("rpt_not_authorized", "Unable to authorize RPT."),
    UNSUPPORTED_OPERATION("unsupported_operation", "Operation is not supported by server error."),
    INVALID_OP_HOST("invalid_op_host", "Invalid op_host (empty or blank)."),
    NO_SETUP_CLIENT_FOR_OXD_ID("no_setup_client_for_oxd_id", "There are no setup client for given oxd_id. Please obtain oxd_id via setup_client command in order to force protection_access_token validation."),
    BLANK_PROTECTION_ACCESS_TOKEN("blank_protection_access_token", "protection_access_token is blank. Command is protected by protection_access_token, please provide valid token or otherwise switch off protection in configuration with protect_commands_with_access_token=false"),
    INVALID_PROTECTION_ACCESS_TOKEN("invalid_protection_access_token", "Invalid protection_access_token. Command is protected by protection_access_token, please provide valid token or otherwise switch off protection in configuration with protect_commands_with_access_token=false"),
    NO_CLIENT_ID_IN_INTROSPECTION_RESPONSE("invalid_introspection_response", "AS returned introspection response with empty/blank client_id which is required by oxd. Please check your AS installation and make sure AS return client_id for introspection call (CE 3.1.0 or later)."),
    INACTIVE_PROTECTION_ACCESS_TOKEN("inactive_protection_access_token", "Inactive protection_access_token. Command is protected by protection_access_token, please provide valid token or otherwise switch off protection in configuration with protect_commands_with_access_token=false"),
    INVALID_AUTHORIZATION_REDIRECT_URI("invalid_authorization_redirect_uri", "Invalid authorization_redirect_uri (empty or blank)."),
    INVALID_SCOPE("invalid_scope", "Invalid scope parameter (empty or blank)."),
    INVALID_ACR_VALUES("invalid_acr_values", "Invalid acr_values parameter (empty or blank)."),
    INVALID_ALGORITHM("invalid_algorithm", "Invalid algorithm provided. Valid algorithms are: " + Arrays.toString(SignatureAlgorithm.values())),
    NO_CONNECT_DISCOVERY_RESPONSE("no_connect_discovery_response", "Unable to fetch Connect discovery response /.well-known/openid-configuration"),
    NO_REGISTRATION_ENDPOINT("invalid_request", "OP does not support dynamic client registration. Please register client manually and provide client_id and client_secret to register_site command."),
    NO_UMA_DISCOVERY_RESPONSE("no_uma_discovery_response", "Unable to fetch UMA discovery response /.well-known/uma2-configuration"),
    NO_UMA_RESOURCES_TO_PROTECT("invalid_uma_request", "Resources list to protect is empty or blank. Please check it according to protocol definition at https://www.gluu.org/docs-oxd"),
    NO_UMA_HTTP_METHOD("invalid_http_method", "http_method is not specified or otherwise not GET or POST or PUT or DELETE. Please check it according to protocol definition at https://www.gluu.org/docs-oxd"),
    NO_UMA_PATH_PARAMETER("invalid_path_parameter", "path parameter is not specified or otherwise not valid"),
    NO_UMA_TICKET_PARAMETER("invalid_ticket_parameter", "ticket parameter is not specified or otherwise is not valid"),
    NO_UMA_CLAIMS_REDIRECT_URI_PARAMETER("invalid_claims_redirect_uri_parameter", "claims_redirect_uri parameter is not specified or otherwise is not valid"),
    NO_UMA_RPT_PARAMETER("invalid_rpt_parameter", "rpt parameter is not specified or otherwise is not valid"),
    UMA_NEED_INFO("need_info", "The authorization server needs additional information in order to determine whether the client is authorized to have these permissions."),
    UMA_HTTP_METHOD_NOT_UNIQUE("http_method_not_unique", "HTTP method defined in JSON must be unique within given PATH (but occurs more then one time)."),
    UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION("invalid_scope_expressioin", "Scope expression is not valid. Please check documentation and make sure expression is valid JsonLogic expression."),
    UMA_PROTECTION_FAILED_BECAUSE_RESOURCES_ALREADY_EXISTS("uma_protection_exists", "Server already has UMA Resources registered for this oxd_id. It is possible to overwrite it if provide overwrite=true for uma_rs_protect command (existing resources will be removed and new UMA Resources added)."),
    FAILED_TO_GET_END_SESSION_ENDPOINT("no_end_session_endpoint_at_op", "OP does not provide end_session_endpoint at /.well-known/openid-configuration."),
    FAILED_TO_GET_REVOCATION_ENDPOINT("no_revocation_endpoint_at_op", "Failed to get revocation_endpoint at https://accounts.google.com/.well-known/openid-configuration"),
    FAILED_TO_GET_RPT("internal_error", "Failed to get RPT."),
    FAILED_TO_REMOVE_SITE("remove_site_failed", "Failed to remove site."),;

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
