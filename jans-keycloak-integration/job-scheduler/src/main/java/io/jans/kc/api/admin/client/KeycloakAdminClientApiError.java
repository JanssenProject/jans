package io.jans.kc.api.admin.client;

public class KeycloakAdminClientApiError extends RuntimeException {
    
    public KeycloakAdminClientApiError(String message) {
        super(message);
    }

    public KeycloakAdminClientApiError(String message, Throwable cause) {
        super(message,cause);
    }
}
