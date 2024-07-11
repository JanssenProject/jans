package io.jans.configapi.core.model.exception;

public class ConfigApiApplicationException extends Exception {
    private final int errorCode;
    private final String message;

    public ConfigApiApplicationException(int errorCode, String message) {
        super("Error Code: " + errorCode + ", Description: " + message);
        this.errorCode = errorCode;
        this.message = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
