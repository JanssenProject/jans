/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;


public enum ValidationStatus implements AttributeEnum {

	PENDING("In Progress", "In Progress"), SUCCESS("Success", "Success"), SCHEDULED("Scheduled",
			"Scheduled"), FAILED("Failed", "Failed");

	private String value;
	private String displayName;

	private static Map<String, ValidationStatus> mapByValues = new HashMap<String, ValidationStatus>();
	static {
		for (ValidationStatus enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private ValidationStatus(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static ValidationStatus getByValue(String value) {
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
