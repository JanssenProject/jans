/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.entry;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * PublicKeyCredentialHints https://w3c.github.io/webauthn/#enumdef-publickeycredentialhints
 * 
 * @author Shekhar L. Date: 16/08/2024
 */
public enum PublicKeyCredentialHints implements AttributeEnum {

    SECURITYKEY("security-key", "Security Key"),
	CLIENTDEVICE("client-device", "Client Device"),
	HYBRID("hybrid", "hybrid"),
	NONE("", "" );

    private final String value;
	private final String displayName;

	private static Map<String, PublicKeyCredentialHints> mapByValues = new HashMap<>();

	static {
		for (PublicKeyCredentialHints enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

    private PublicKeyCredentialHints(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static PublicKeyCredentialHints fromString(String param) {
    	return getByValue(param);
    }

	@Override
	public String getValue() {
		return value;
	}

	/**
     * Gets display name
     *
     * @return display name name
     */
    public String getDisplayName() {
		return displayName;
	}

	public static PublicKeyCredentialHints getByValue(String value) {
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