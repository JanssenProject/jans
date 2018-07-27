package org.xdi.oxd.common;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 * @author Yuriy Zabrovarnyy
 */
public class HttpErrorResponseException extends RuntimeException {

    public static final String KEY_PREFIX = "http_status_";
    private final int httpStatus;
    private final String entity;

    public HttpErrorResponseException(int httpStatus, String entity) {
        this.httpStatus = httpStatus;
        this.entity = entity;
    }

    public HttpErrorResponseException(ClientResponseFailure e) {
        this(e.getResponse().getStatus(), (String) e.getResponse().getEntity(String.class));
    }

    public static HttpErrorResponseException parseSilently(ErrorResponse errorResponse) {
        return parseSilently(errorResponse.getError(), errorResponse.getErrorDescription());
    }

    public static HttpErrorResponseException parseSilently(String error, String errorDescription) {
        if (StringUtils.isNotBlank(error) && error.startsWith(KEY_PREFIX)) {
            try {
                return new HttpErrorResponseException(Integer.parseInt(error.substring(KEY_PREFIX.length())), errorDescription);
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    public ErrorResponse createErrorResponse() {
        ErrorResponse error = new ErrorResponse();
        error.setError(KEY_PREFIX + httpStatus);
        error.setErrorDescription(entity);
        return error;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "HttpErrorResponseException{" +
                "httpStatus=" + httpStatus +
                ", entity='" + entity + '\'' +
                "} ";
    }
}
