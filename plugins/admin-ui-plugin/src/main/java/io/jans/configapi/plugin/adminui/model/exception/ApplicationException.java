package io.jans.configapi.plugin.adminui.model.exception;

public class ApplicationException extends Exception {
    private int errorCode;
    private String message;

    public ApplicationException(int errorCode, String message) {
        super("Error Code: " + errorCode + ", Description: " + message);
        this.errorCode = errorCode;
        this.message = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
