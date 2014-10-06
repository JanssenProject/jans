package org.xdi.oxd.license.client.js;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public enum LicenseType {
    FREE("Free"),
    SHAREWARE("Shareware"),
    PAID("Paid"),
    PREMIUM("Premium");

    private final String value;

    private LicenseType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LicenseType fromValue(String value) {
        for (LicenseType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
