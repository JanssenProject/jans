package io.jans.agama.engine.exception;

public class FlowCrashException extends Exception {
    
    public FlowCrashException(String message) {
        super(message);
    }
    
    public FlowCrashException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
