package io.jans.kc.api.admin.client;

public class AdminClientApiError extends RuntimeException {
    
    public AdminClientApiError(String message) {
        super(message);
    }

    public AdminClientApiError(String message, Throwable cause) {
        super(message,cause);
    }
}
