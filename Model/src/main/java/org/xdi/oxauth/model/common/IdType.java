/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */

public enum IdType {

    PEOPLE("people", "people", "0000", "New Unique People Inum Generator"),
    ORGANIZATION("organization", "0001", "organization","New Unique Organization Inum Generator"),
    CONFIGURATION("configuration", "configuration", "0002", "New Unique configuration Inum Generator"),
    GROUP("group", "group", "0003", "New Unique Group Inum Generator"),
    SERVER("server", "server", "0004", "New Unique Server Inum Generator"),
    ATTRIBUTE("attribute", "attribute", "0005", "New Unique Attribute Inum Generator"),
    TRUST_RELATIONSHIP("trelationship", "0006", "trustRelationship", "New Unique Trust Relationship Inum Generator"),
    LINK_CONTRACTS("lcontracts", "linkContracts", "0007", "New Unique Link Contracts Inum Generator"),
    CLIENTS("oclient", "openidConnectClient", "0008", "New Unique Openid Connect Client Inum Generator");

    private final String m_type;
    private final String m_value;
    private final String m_inum;
    private final String m_htmlText;

    private IdType(String p_type, String p_value, String p_inum, String p_htmlText) {
        m_type = p_type;
        m_value = p_value;
        m_inum = p_inum;
        m_htmlText = p_htmlText;
    }

    public String getInum() {
        return m_inum;
    }

    public String getHtmlText() {
        return m_htmlText;
    }

    public String getType() {
        return m_type;
    }

    public String getValue() {
        return m_value;
    }

    public static IdType fromString(String p_string) {
        if (StringUtils.isNotBlank(p_string)) {
            for (IdType t : values()) {
                if (t.getType().equalsIgnoreCase(p_string) || t.getValue().equalsIgnoreCase(p_string)) {
                    return t;
                }
            }
        }
        return null;
    }
}
