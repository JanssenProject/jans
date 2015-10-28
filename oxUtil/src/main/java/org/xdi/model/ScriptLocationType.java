/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Specify type of script location
 * 
 * @author Yuriy Movchan Date: 10/07/2015
 */
public enum ScriptLocationType implements LdapEnum {

	LDAP("ldap", "Ldap"), FILE("file", "File");

	private String value;
	private String displayName;

	private static Map<String, ScriptLocationType> mapByValues = new HashMap<String, ScriptLocationType>();

	static {
		for (ScriptLocationType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private ScriptLocationType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static ScriptLocationType getByValue(String value) {
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
