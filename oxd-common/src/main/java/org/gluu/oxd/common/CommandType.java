/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 */

public enum CommandType {

    // Register
    REGISTER_SITE("register_site", false, MediaType.APPLICATION_JSON),
    UPDATE_SITE("update_site", false, MediaType.APPLICATION_JSON),
    REMOVE_SITE("remove_site", false, MediaType.APPLICATION_JSON),

    // Connect (stateful)
    GET_AUTHORIZATION_URL("get_authorization_url", true, MediaType.APPLICATION_JSON),
    GET_AUTHORIZATION_CODE("get_authorization_code", true, MediaType.APPLICATION_JSON),
    GET_TOKENS_BY_CODE("get_tokens_by_code", true, MediaType.APPLICATION_JSON),
    GET_USER_INFO("get_user_info", true, MediaType.APPLICATION_JSON),
    GET_LOGOUT_URI("get_logout_uri", true, MediaType.APPLICATION_JSON),
    GET_ACCESS_TOKEN_BY_REFRESH_TOKEN("get_access_token_by_refresh_token", true, MediaType.APPLICATION_JSON),
    INTROSPECT_ACCESS_TOKEN("introspect_access_token", true, MediaType.APPLICATION_JSON),

    VALIDATE("validate", true, MediaType.APPLICATION_JSON),
    CHECK_ID_TOKEN("id_token_status", true, MediaType.APPLICATION_JSON),
    CHECK_ACCESS_TOKEN("access_token_status", true, MediaType.APPLICATION_JSON),

    // UMA
    RS_PROTECT("uma_rs_protect", true, MediaType.APPLICATION_JSON),
    RS_MODIFY("uma_rs_modify", true, MediaType.APPLICATION_JSON),
    RS_CHECK_ACCESS("uma_rs_check_access", true, MediaType.APPLICATION_JSON),
    INTROSPECT_RPT("introspect_rpt", true, MediaType.APPLICATION_JSON),
    RP_GET_RPT("uma_rp_get_rpt", true, MediaType.APPLICATION_JSON),
    RP_GET_CLAIMS_GATHERING_URL("uma_rp_get_claims_gathering_url", true, MediaType.APPLICATION_JSON),

    // stateless
    AUTHORIZATION_CODE_FLOW("authorization_code_flow", true, MediaType.APPLICATION_JSON),
    IMPLICIT_FLOW("implicit_flow", true, MediaType.APPLICATION_JSON),
    GET_CLIENT_TOKEN("get_client_token", false, MediaType.APPLICATION_JSON),
    GET_RP("get_rp", false, MediaType.APPLICATION_JSON),
    GET_JWKS("get_jwks", false, MediaType.APPLICATION_JSON),
    GET_DISCOVERY("get_discovery", false, MediaType.APPLICATION_JSON),
    ISSUER_DISCOVERY("issuer_discovery", false, MediaType.APPLICATION_JSON),
    GET_RP_JWKS("get_rp_jwks", false, MediaType.APPLICATION_JSON),
    GET_REQUEST_OBJECT_JWT("get_request_object_jwt", false, MediaType.TEXT_PLAIN),
    GET_REQUEST_URI("get_request_uri", true, MediaType.APPLICATION_JSON);

    private final String value;
    private final boolean authorizationRequired;
    private final String returnType;

    CommandType(String value, boolean authorizationRequired, String returnType) {
        this.value = value;
        this.authorizationRequired = authorizationRequired;
        this.returnType = returnType;
    }

    @JsonIgnore
    public boolean isAuthorizationRequired() {
        return authorizationRequired;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getReturnType() {
        return returnType;
    }

    @JsonCreator
    public static CommandType fromValue(String v) {
        if (StringUtils.isNotBlank(v)) {
            for (CommandType t : values()) {
                if (t.getValue().equalsIgnoreCase(v)) {
                    return t;
                }
            }
        }
        return null;
    }
}
