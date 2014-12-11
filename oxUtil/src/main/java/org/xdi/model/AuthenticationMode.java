/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Allowed authentication modes
 * 
 * @author Yuriy Movchan Date: 08/08/2013
 */
public enum AuthenticationMode {

	BASIC("basic", "Basic"), OAUTH("oauth", "OAuth"), UMA("uma", "UMA");

	private String value;
	private String displayName;

	private static Map<String, AuthenticationMode> mapByValues = new HashMap<String, AuthenticationMode>();

	static {
		for (AuthenticationMode enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private AuthenticationMode(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public static AuthenticationMode getByValue(String value) {
		return mapByValues.get(value);
	}

	public String getDisplayName() {
		return displayName;
	}

	public AuthenticationMode resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
