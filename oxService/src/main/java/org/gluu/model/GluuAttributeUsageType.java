/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.persist.annotation.AttributeEnum;

/**
 * Attribute Usage Type
 * 
 * @author Yuriy Movchan Date: 02/12/2014
 */
public enum GluuAttributeUsageType implements AttributeEnum {

	OPENID("openid", "OpenID");

	private String value;
	private String displayName;

	private static Map<String, GluuAttributeUsageType> mapByValues = new HashMap<String, GluuAttributeUsageType>();

	static {
		for (GluuAttributeUsageType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuAttributeUsageType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static GluuAttributeUsageType getByValue(String value) {
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
