/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

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
    CLIENTS("oclient", "openidConnectClient", "0008", "New Unique Openid Connect Client Inum Generator");

    private final String type;
    private final String value;
    private final String inum;
    private final String htmlText;

    IdType(String type, String value, String inum, String htmlText) {
        this.type = type;
        this.value = value;
        this.inum = inum;
        this.htmlText = htmlText;
    }

    public String getInum() {
        return inum;
    }

    public String getHtmlText() {
        return htmlText;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public static IdType fromString(String string) {
        if (StringUtils.isNotBlank(string)) {
            for (IdType t : values()) {
                if (t.getType().equalsIgnoreCase(string) || t.getValue().equalsIgnoreCase(string)) {
                    return t;
                }
            }
        }
        return null;
    }
}
