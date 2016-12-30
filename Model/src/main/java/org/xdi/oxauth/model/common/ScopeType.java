/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Scope types
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version December 29, 2016
 */
public enum ScopeType implements LdapEnum {

    LDAP("ldap", "Ldap"),
    DYNAMIC("dynamic", "Dynamic"),
    OPENID("openid", "Openid");
    //OAUTH("oauth", "OAuth"),
    //UMA("uma", "UMA");

    private final String value;
    private final String displayName;

    private static Map<String, ScopeType> mapByValues = new HashMap<String, ScopeType>();

    static {
        for (ScopeType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private ScopeType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static ScopeType fromString(String param) {
        return getByValue(param);
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Gets display name
     *
     * @return display name name
     */
    public String getDisplayName() {
        return displayName;
    }

    public static ScopeType getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends LdapEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}