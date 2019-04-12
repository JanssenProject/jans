/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.persist.annotation.AttributeEnum;

/**
 * Attribute Data Type
 * 
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 *
 * @version September 27, 2017
 */
public enum GluuAttributeDataType implements AttributeEnum {

	STRING("string", "Text"),
	NUMERIC("numeric", "Numeric"),
	BOOLEAN("boolean", "Boolean"),
	BINARY("binary", "Binary"),
	CERTIFICATE("certificate", "Certificate"),
	DATE("generalizedTime", "Date");

	private String value;
	private String displayName;

	private static Map<String, GluuAttributeDataType> mapByValues = new HashMap<String, GluuAttributeDataType>();

	static {
		for (GluuAttributeDataType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuAttributeDataType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static GluuAttributeDataType getByValue(String value) {
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
