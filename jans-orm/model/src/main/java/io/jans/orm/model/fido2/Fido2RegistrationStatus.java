/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.fido2;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

public enum Fido2RegistrationStatus implements AttributeEnum {

    pending("pending", "Pending"), registered("registered", "Registered"), compromised("compromised", "Compromised"), canceled("canceled", "Canceled");

    private String value;
    private String displayName;

    private static Map<String, Fido2RegistrationStatus> mapByValues = new HashMap<String, Fido2RegistrationStatus>();

    static {
        for (Fido2RegistrationStatus enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private Fido2RegistrationStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Fido2RegistrationStatus getByValue(String value) {
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
