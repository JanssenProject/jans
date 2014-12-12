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
 * Status
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
public enum GluuStatus implements LdapEnum {

	ACTIVE("active", "Active"), INACTIVE("inactive", "Inactive"), EXPIRED("expired", "Expired"), REGISTER("register", "Register");

	private String value;
	private String displayName;

	private static Map<String, GluuStatus> mapByValues = new HashMap<String, GluuStatus>();

	static {
		for (GluuStatus enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuStatus(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static GluuStatus getByValue(String value) {
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
