/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.fido.u2f;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Device registration types
 * 
 * @author Yuriy Movchan Date: 06/02/2015
 */
public enum DeviceRegistrationStatus implements LdapEnum {

    ACTIVE("active", "Active device registration"),
	COMPROMISED("compromised", "Compromised device registration");

    private final String value;
	private final String displayName;

	private static Map<String, DeviceRegistrationStatus> mapByValues = new HashMap<String, DeviceRegistrationStatus>();

	static {
		for (DeviceRegistrationStatus enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

    private DeviceRegistrationStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static DeviceRegistrationStatus fromString(String param) {
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

	public static DeviceRegistrationStatus getByValue(String value) {
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