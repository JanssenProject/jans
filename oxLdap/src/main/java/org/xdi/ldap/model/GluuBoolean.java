/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.ldap.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Boolean value
 * 
 * @author Yuriy Movchan Date: 11.20.2010
 */
public enum GluuBoolean implements LdapEnum {

	DISABLED(false, "disabled", "Disabled"), ENABLED(true, "enabled", "Enabled"), FALSE(false, "false", "False"), TRUE(true, "true", "True");

	private boolean booleanValue;
	private String value;
	private String displayName;

	private static Map<String, GluuBoolean> mapByValues = new HashMap<String, GluuBoolean>();

	static {
		for (GluuBoolean enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuBoolean(boolean booleanValue, String value, String displayName) {
		this.booleanValue = booleanValue;
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public boolean isBooleanValue() {
		return booleanValue;
	}

	public static GluuBoolean getByValue(String value) {
		return mapByValues.get(value);
	}

	public String getDisplayName() {
		return displayName;
	}

	public Enum<? extends LdapEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
