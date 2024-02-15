package io.jans.kc.api.admin.client;

public class KeycloakConfigurationError extends RuntimeException {
    
    public KeycloakConfigurationError(String message) {
        super(message);
    }

    public KeycloakConfigurationError(String message, Throwable cause) {
        super(message,cause);
    }
}
