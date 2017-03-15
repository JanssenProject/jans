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
 * Script languages
 * 
 * @author Yuriy Movchan Date: 07/10/2013
 */
public enum ProgrammingLanguage implements LdapEnum {

	PYTHON("python", "Python"), JAVA_SCRIPT("javascript", "JavaScript");

	private String value;
	private String displayName;

	private static Map<String, ProgrammingLanguage> mapByValues = new HashMap<String, ProgrammingLanguage>();

	static {
		for (ProgrammingLanguage enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private ProgrammingLanguage(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static ProgrammingLanguage getByValue(String value) {
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
