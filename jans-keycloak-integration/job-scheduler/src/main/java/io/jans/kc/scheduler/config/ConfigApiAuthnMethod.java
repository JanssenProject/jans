package io.jans.kc.scheduler.config;

public enum ConfigApiAuthnMethod {
    BASIC_AUTHN("basic"),
    POST_AUTHN("post"),
    PRIVATE_KEY_JWT_AUTHN("private_key_jwt"),
    UNKNOWN_AUTHN("unknown");

    private final String value;

    private ConfigApiAuthnMethod(final String value) {
        this.value = value;
    }

    public static final ConfigApiAuthnMethod fromString(String value) {

        if(value == null) {
            return UNKNOWN_AUTHN;
        }

        ConfigApiAuthnMethod ret = UNKNOWN_AUTHN;
        for(ConfigApiAuthnMethod method: ConfigApiAuthnMethod.values()) {
            if(method.value.equalsIgnoreCase(value.trim())) {
                ret = method;
                break;
            }
        }
        return ret;
    }
}
