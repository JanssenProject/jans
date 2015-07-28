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
 * Statistic Counter Type
 * 
 * @author Yuriy Movchan Date: 07/28/2015
 */
public enum CounterType implements LdapEnum {

	SUM("sum", "Sum"),
	AVERAGE("average", "Average");

	private String value;
	private String displayName;

	private static Map<String, CounterType> mapByValues = new HashMap<String, CounterType>();

	static {
		for (CounterType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private CounterType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static CounterType getByValue(String value) {
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
