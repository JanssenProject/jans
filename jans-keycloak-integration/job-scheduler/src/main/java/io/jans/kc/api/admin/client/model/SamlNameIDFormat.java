package io.jans.kc.api.admin.client.model;


public enum SamlNameIDFormat {
    USERNAME("username"),
    EMAIL("email"),
    PERSISTENT("persistent"),
    TRANSIENT("transient"),
    UNSPECIFIED("unspecified");

    private final String value;

    private SamlNameIDFormat(final String value) {

        this.value = value;
    }

    public String value() {

        return this.value;
    }
}
