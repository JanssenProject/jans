/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * @author Yuriy Zabrovarnyy
 */

public enum CommandType {

    // Register
    REGISTER_SITE("register_site"),
    UPDATE_SITE("update_site"),
    REMOVE_SITE("remove_site"),

    // Connect (stateful)
    GET_AUTHORIZATION_URL("get_authorization_url"),
    GET_AUTHORIZATION_CODE("get_authorization_code"),
    GET_TOKENS_BY_CODE("get_tokens_by_code"),
    GET_USER_INFO("get_user_info"),
    GET_LOGOUT_URI("get_logout_uri"),
    GET_ACCESS_TOKEN_BY_REFRESH_TOKEN("get_access_token_by_refresh_token"),
    INTROSPECT_ACCESS_TOKEN("introspect_access_token"),

    VALIDATE("validate"),
    CHECK_ID_TOKEN("id_token_status"),
    CHECK_ACCESS_TOKEN("access_token_status"),

    // UMA
    RS_PROTECT("uma_rs_protect"),
    RS_CHECK_ACCESS("uma_rs_check_access"),
    INTROSPECT_RPT("introspect_rpt"),
    RP_GET_RPT("uma_rp_get_rpt"),
    RP_GET_CLAIMS_GATHERING_URL("uma_rp_get_claims_gathering_url"),

    // stateless
    AUTHORIZATION_CODE_FLOW("authorization_code_flow"),
    IMPLICIT_FLOW("implicit_flow"),
    GET_CLIENT_TOKEN("get_client_token"),
    GET_RP("get_rp"),
    GET_JWKS("get_jwks"),
    GET_DISCOVERY("get_discovery");

    private final String value;

    private CommandType(String p_value) {
        value = p_value;
    }

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    @com.fasterxml.jackson.annotation.JsonCreator
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
