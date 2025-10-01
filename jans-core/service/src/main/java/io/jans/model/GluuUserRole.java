/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * User role
 * 
 * @author Yuriy Movchan Date: 11.03.2010
 */
public enum GluuUserRole implements AttributeEnum {

	ADMIN("admin"), OWNER("owner"), MANAGER("manager"), USER("user"), WHITEPAGES("whitePages");

	private String value;

	private static Map<String, GluuUserRole> mapByValues = new HashMap<String, GluuUserRole>();

	static {
		for (GluuUserRole enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuUserRole(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String getRoleName() {
		return value;
	}

	public String getDisplayName() {
		return value;
	}

	public static GluuUserRole getByValue(String value) {
		return mapByValues.get(value);
	}

	public static GluuUserRole[] getByValues(String[] values) {
		GluuUserRole[] roles = new GluuUserRole[values.length];
		for (int i = 0; i < values.length; i++) {
			roles[i] = getByValue(values[i]);
		}

		return roles;
	}

	public static boolean equals(GluuUserRole[] roles1, GluuUserRole[] roles2) {
		Arrays.sort(roles1);
		Arrays.sort(roles2);
		return Arrays.equals(roles1, roles2);
	}

	public static boolean containsRole(GluuUserRole[] roles, GluuUserRole role) {
		if ((roles == null) || (role == null)) {
			return false;
		}

		for (int i = 0; i < roles.length; i++) {
			if (role.equals(roles[i])) {
				return true;
			}
		}

		return false;
	}

	public Enum<? extends AttributeEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
