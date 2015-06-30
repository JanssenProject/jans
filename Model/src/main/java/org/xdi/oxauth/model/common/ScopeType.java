/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Scope types
 * 
 * @author Yuriy Movchan Date: 06/30/2015
 */
public enum ScopeType implements LdapEnum {

    LDAP("ldap", "Ldap"),
	DYNAMIC("dynamic", "Dynamic");

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