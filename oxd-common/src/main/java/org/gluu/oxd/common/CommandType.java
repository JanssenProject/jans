/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 */

public enum CommandType {

    // Register
    REGISTER_SITE("register_site", false),
    UPDATE_SITE("update_site", false),
    REMOVE_SITE("remove_site", false),

    // Connect (stateful)
    GET_AUTHORIZATION_URL("get_authorization_url", true),
    GET_AUTHORIZATION_CODE("get_authorization_code", true),
    GET_TOKENS_BY_CODE("get_tokens_by_code", true),
    GET_USER_INFO("get_user_info", true),
    GET_LOGOUT_URI("get_logout_uri", true),
    GET_ACCESS_TOKEN_BY_REFRESH_TOKEN("get_access_token_by_refresh_token", true),
    INTROSPECT_ACCESS_TOKEN("introspect_access_token", true),

    VALIDATE("validate", true),
    CHECK_ID_TOKEN("id_token_status", true),
    CHECK_ACCESS_TOKEN("access_token_status", true),

    // UMA
    RS_PROTECT("uma_rs_protect", true),
    RS_MODIFY("uma_rs_modify", true),
    RS_CHECK_ACCESS("uma_rs_check_access", true),
    INTROSPECT_RPT("introspect_rpt", true),
    RP_GET_RPT("uma_rp_get_rpt", true),
    RP_GET_CLAIMS_GATHERING_URL("uma_rp_get_claims_gathering_url", true),

    // stateless
    AUTHORIZATION_CODE_FLOW("authorization_code_flow", true),
    IMPLICIT_FLOW("implicit_flow", true),
    GET_CLIENT_TOKEN("get_client_token", false),
    GET_RP("get_rp", false),
    GET_JWKS("get_jwks", false),
    GET_DISCOVERY("get_discovery", false),
    ISSUER_DISCOVERY("issuer_discovery", false),
    GET_RP_JWKS("get_rp_jwks", false),
    GET_REQUEST_OBJECT_JWT("get_request_object_jwt", false),
    GET_REQUEST_URI("get_request_uri", true);
    private final String value;
    private final boolean authorizationRequired;

    CommandType(String value, boolean authorizationRequired) {
        this.value = value;
        this.authorizationRequired = authorizationRequired;
    }

    @JsonIgnore
    public boolean isAuthorizationRequired() {
        return authorizationRequired;
    }

    @JsonValue
    public String getValue() {
        return value;
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
