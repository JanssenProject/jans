/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import java.util.HashMap;
import java.util.Map;
import io.jans.orm.annotation.AttributeEnum;

/**
 * Metadata source type
 * 
 */
public enum MetadataSourceType implements AttributeEnum {

	FILE("file", "File",1),  MANUAL("manual", "Manual",2);

	private final String value;
	private final String displayName;
	private final int rank; // used for ordering 

	private static final Map<String, MetadataSourceType> mapByValues = new HashMap<String, MetadataSourceType>();
	static {
		for (MetadataSourceType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private MetadataSourceType(String value, String displayName,int rank) {
		this.value = value;
		this.displayName = displayName;
		this.rank = rank;
	}

	@Override
	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getRank() {

		return this.rank;
	}

	public static MetadataSourceType getByValue(String value) {
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
	
	public static boolean contains(String name) {
		boolean result = false;
	    for (MetadataSourceType direction : values()) {
	        if (direction.name().equalsIgnoreCase(name)) {
	            result = true;
	            break;
	        }
	    }
	    return result;
	}

}
