package org.xdi.oxd.license.client.js;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public enum LicenseType {
    FREE("Free", 1),
    SHAREWARE("Shareware", 1),
    PAID("Paid", 100),
    PREMIUM("Premium", Integer.MAX_VALUE);

    private final String value;
    private final int threadsCount;

    private LicenseType(String value, int threadsCount) {
        this.value = value;
        this.threadsCount = threadsCount;
    }

    public int getThreadsCount() {
        return threadsCount;
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
