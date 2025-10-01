package io.jans.ca.plugin.adminui.model.exception;

public class ApplicationException extends Exception {
    private final int errorCode;
    private final String message;

    public ApplicationException(int errorCode, String message) {
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
