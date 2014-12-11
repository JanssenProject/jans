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
 * Attribute Privacy Level
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
public enum GluuAttributePrivacyLevel implements LdapEnum {

	LEVEL_1("level1", "1"), LEVEL_2("level2", "2"), LEVEL_3("level3", "3"), LEVEL_4("level4", "4"), LEVEL_5("level5", "5");

	private String value;
	private String displayName;

	private static Map<String, GluuAttributePrivacyLevel> mapByValues = new HashMap<String, GluuAttributePrivacyLevel>();

	static {
		for (GluuAttributePrivacyLevel enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuAttributePrivacyLevel(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static GluuAttributePrivacyLevel getByValue(String value) {
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
