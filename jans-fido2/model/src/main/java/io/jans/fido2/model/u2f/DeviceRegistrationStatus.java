/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Device registration types
 *
 * @author Yuriy Movchan Date: 06/02/2015
 */
public enum DeviceRegistrationStatus implements AttributeEnum {
    //TODO: remove this class and reuse the one found in fido2-model
    ACTIVE("active", "Active device registration"),
    COMPROMISED("compromised", "Compromised device registration"),
    MIGRATED("migrated", "Migrated to Fido2");

    private final String value;
    private final String displayName;

    private static final Map<String, DeviceRegistrationStatus> mapByValues = new HashMap<>();

    static {
        for (DeviceRegistrationStatus enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    DeviceRegistrationStatus(String value, String displayName) {
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

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}