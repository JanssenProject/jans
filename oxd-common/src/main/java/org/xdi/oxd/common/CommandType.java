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

public enum CommandType {

    // Register
    REGISTER_SITE("register_site"),
    UPDATE_SITE("update_site_registration"),

    // Connect (stateful)
    GET_AUTHORIZATION_URL("get_authorization_url"),
    GET_AUTHORIZATION_CODE("get_authorization_code"),
    GET_TOKENS_BY_CODE("get_tokens_by_code"),
    GET_USER_INFO("get_user_info"),
    GET_LOGOUT_URI("get_logout_uri"),

    CHECK_ID_TOKEN("id_token_status"),
    CHECK_ACCESS_TOKEN("access_token_status"),
    LICENSE_STATUS("license_status"),

    // UMA
    RP_GET_RPT("uma_rp_get_rpt"),

    // stateless
    AUTHORIZATION_CODE_FLOW("authorization_code_flow"),
    IMPLICIT_FLOW("implicit_flow");



    private final String value;

    private CommandType(String p_value) {
        value = p_value;
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
