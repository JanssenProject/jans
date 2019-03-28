/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Metric application type
 *
 * @author Yuriy Movchan Date: 07/28/2015
 */
public enum ApplicationType implements LdapEnum {

    OX_AUTH("oxauth", "oxAuth"),
    OX_TRUST("oxtrust", "oxTrust");

    private String value;
    private String displayName;

    private static Map<String, ApplicationType> MAP_BY_VALUES = new HashMap<String, ApplicationType>();

    static {
        for (ApplicationType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    ApplicationType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ApplicationType getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public Enum<? extends LdapEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
