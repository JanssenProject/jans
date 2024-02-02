package io.jans.kc.api.config.client.impl;


public class CredentialsProviderError extends RuntimeException{
    
    public CredentialsProviderError(String message) {
        super(message);
    }

    public CredentialsProviderError(String message, Throwable cause) {
        super(message,cause);
    }
}
