/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * Cache refresh update methods
 * 
 * @author Yuriy Movchan Date: 06.27.2012
 */
public enum CacheRefreshUpdateMethod implements AttributeEnum {

	VDS("vds", "VDS"), COPY("copy", "Copy");

	private boolean booleanValue;
	private String value;
	private String displayName;

	private static Map<String, CacheRefreshUpdateMethod> mapByValues = new HashMap<String, CacheRefreshUpdateMethod>();
	static {
		for (CacheRefreshUpdateMethod enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private CacheRefreshUpdateMethod(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public boolean isBooleanValue() {
		return booleanValue;
	}

	public static CacheRefreshUpdateMethod getByValue(String value) {
		return mapByValues.get(value);
	}

	public String getDisplayName() {
		return displayName;
	}

	public Enum<? extends AttributeEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
