package io.jans.kc.api.config.client;


public class JansConfigApiError extends RuntimeException {
    
    public JansConfigApiError(String message) {
        super(message);
    }

    public JansConfigApiError(String message, Throwable cause) {
        super(message,cause);
    }
}
