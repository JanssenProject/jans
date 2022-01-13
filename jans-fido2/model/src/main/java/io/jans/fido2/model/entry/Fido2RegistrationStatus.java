/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.entry;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

public enum Fido2RegistrationStatus implements AttributeEnum {

    pending("pending", "Pending"), registered("registered", "Registered"), compromised("compromised", "Compromised");

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
