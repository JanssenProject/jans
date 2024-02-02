package io.jans.kc.scheduler.config;

public class AppConfigException extends RuntimeException {
    
    public AppConfigException(String message) {
        super(message);
    }

    public AppConfigException(String message, Throwable cause) {
        super(message,cause);
    }
}
