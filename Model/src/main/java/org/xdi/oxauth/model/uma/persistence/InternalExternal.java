/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma.persistence;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Internal/external type
 * 
 * @author Yuriy Movchan Date: 07/10/2013
 */
public enum InternalExternal implements LdapEnum {

	INTERNAL("internal", "Internal"), EXTERNAL("external", "External"), EXTERNAL_AUTO("external_auto", "External auto"), UMA("uma", "Uma");

	private String value;
	private String displayName;

	private static Map<String, InternalExternal> mapByValues = new HashMap<String, InternalExternal>();

	static {
		for (InternalExternal enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private InternalExternal(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static InternalExternal getByValue(String value) {
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
