/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import java.util.HashMap;
import java.util.Map;
import io.jans.orm.annotation.AttributeEnum;


public enum EntityType implements AttributeEnum {

	SingleSP("Single SP", "Single SP"), FederationAggregate("Federation/Aggregate", "Federation/Aggregate");

	private final String value;
	private final String displayName;

	private static final Map<String, EntityType> mapByValues = new HashMap<>();
	static {
		for (EntityType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private EntityType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

        @Override
	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static EntityType getByValue(String value) {
		return mapByValues.get(value);
	}

        @Override
	public Enum<? extends AttributeEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
