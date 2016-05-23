package org.xdi.oxd.common;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/05/2016
 */

public class ErrorResponseException extends RuntimeException {

    private final ErrorResponseCode errorResponseCode;

    public ErrorResponseException(ErrorResponseCode errorResponseCode) {
        this.errorResponseCode = errorResponseCode;
    }

    public ErrorResponseCode getErrorResponseCode() {
        return errorResponseCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ErrorResponseException");
        sb.append("{errorResponseCode=").append(errorResponseCode);
        sb.append('}');
        return sb.toString();
    }
}
