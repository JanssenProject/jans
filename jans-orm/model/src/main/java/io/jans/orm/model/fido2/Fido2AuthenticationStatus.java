/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.fido2;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
public enum Fido2AuthenticationStatus implements AttributeEnum {

	pending("pending", "Pending"), authenticated("authenticated", "Authenticated");

    private String value;
    private String displayName;

    private static Map<String, Fido2AuthenticationStatus> mapByValues = new HashMap<String, Fido2AuthenticationStatus>();

    static {
        for (Fido2AuthenticationStatus enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private Fido2AuthenticationStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Fido2AuthenticationStatus getByValue(String value) {
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
