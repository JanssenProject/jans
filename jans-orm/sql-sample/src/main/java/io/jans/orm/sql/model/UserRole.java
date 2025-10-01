/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
* @author Yuriy Movchan Date: 01/15/2020
 */
public enum UserRole implements AttributeEnum {

	ADMIN("admin"), OWNER("owner"), MANAGER("manager"), USER("user");

	private String value;

	private static Map<String, UserRole> mapByValues = new HashMap<String, UserRole>();

	static {
		for (UserRole enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private UserRole(String value) {
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

	public static UserRole getByValue(String value) {
		return mapByValues.get(value);
	}

	public static UserRole[] getByValues(String[] values) {
		UserRole[] roles = new UserRole[values.length];
		for (int i = 0; i < values.length; i++) {
			roles[i] = getByValue(values[i]);
		}

		return roles;
	}

	public static boolean equals(UserRole[] roles1, UserRole[] roles2) {
		Arrays.sort(roles1);
		Arrays.sort(roles2);
		return Arrays.equals(roles1, roles2);
	}

	public static boolean containsRole(UserRole[] roles, UserRole role) {
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
