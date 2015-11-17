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

    AUTHORIZE_RPT("authorize_rpt"),
    REGISTER_CLIENT("register_client"),
    CLIENT_READ("client_read"),
    AUTHORIZATION_CODE_FLOW("authorization_code_flow"),
    IMPLICIT_FLOW("implicit_flow"),
    OBTAIN_PAT("obtain_pat"),
    OBTAIN_AAT("obtain_aat"),
    OBTAIN_RPT("obtain_rpt"),
    REGISTER_RESOURCE("register_resource"),
    RPT_STATUS("rpt_status"),
    REGISTER_TICKET("register_ticket"),
    CHECK_ID_TOKEN("id_token_status"),
    CHECK_ACCESS_TOKEN("access_token_status"),
    LICENSE_STATUS("license_status"),
    DISCOVERY("discovery"),
    GET_AUTHORIZATION_URL("get_authorization_url"),
    GET_AUTHORIZATION_CODE("get_authorization_code"),
    GET_TOKENS_BY_CODE("get_tokens_by_code"),
    GET_USER_INFO("get_user_info"),
    LOGOUT("logout"),
    REGISTER_SITE("register_site");

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
