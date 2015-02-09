package org.xdi.oxauth.model.common;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/02/2015
 */

public enum SessionIdState {
    UNAUTHENTICATED("unauthenticated"),
    AUTHENTICATED("authenticated");

    private final String value;

    private SessionIdState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SessionIdState fromValue(String value) {
        for (SessionIdState s : values()) {
            if (s.getValue().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }
}
