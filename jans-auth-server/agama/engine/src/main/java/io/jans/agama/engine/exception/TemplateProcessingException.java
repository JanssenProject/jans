package io.jans.agama.engine.exception;

public class TemplateProcessingException extends Exception {
    
    public TemplateProcessingException(String message) {
        super(message);
    }
    
    public TemplateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
