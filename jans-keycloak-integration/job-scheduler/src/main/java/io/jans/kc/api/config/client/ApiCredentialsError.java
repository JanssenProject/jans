package io.jans.kc.api.config.client;

public class ApiCredentialsError extends RuntimeException {
    
    public ApiCredentialsError(String message) {
        super(message);
    }

    public ApiCredentialsError(String message, Throwable cause) {
        super(message,cause);
    }
}
