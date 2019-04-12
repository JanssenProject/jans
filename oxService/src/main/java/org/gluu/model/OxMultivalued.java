/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.persist.annotation.AttributeEnum;

/**
 * SCIM Custom Attribute
 * 
 * @author Reda Zerrad Date: 08.02.2012
 */
public enum OxMultivalued implements AttributeEnum {

	TRUE("true", "True"), FALSE("false", "False");

	private String value;
	private String displayName;

	private static Map<String, OxMultivalued> mapByValues = new HashMap<String, OxMultivalued>();

	static {
		for (OxMultivalued enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private OxMultivalued(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static OxMultivalued getByValue(String value) {
		return mapByValues.get(value);
	}

	public Enum<? extends AttributeEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}