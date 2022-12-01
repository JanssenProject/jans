package io.jans.as.model.ssa;

/**
 * @author Yuriy Z
 */
public enum SsaValidationType {
    NONE("none"),
    SSA("ssa"),
    DCR("dcr");

    private final String value;

    SsaValidationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SsaValidationType of(String value) {
        for (SsaValidationType t : values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }
        return NONE;
    }
}
