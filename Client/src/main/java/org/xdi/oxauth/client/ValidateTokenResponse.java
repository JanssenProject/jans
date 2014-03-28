package org.xdi.oxauth.client;

import org.xdi.oxauth.model.token.ValidateTokenErrorResponseType;

/**
 * Represents a validate token response received from the authorization server.
 *
 * @author Javier Rojas Blum Date: 10.27.2011
 */
public class ValidateTokenResponse extends BaseResponse {

    private boolean valid;
    private Integer expiresIn;

    private ValidateTokenErrorResponseType errorType;
    private String errorDescription;
    private String errorUri;

    /**
     * Constructs a validate token response.
     *
     * @param status The response status code.
     */
    public ValidateTokenResponse(int status) {
        super(status);
    }

    /**
     * Returns whether the validation was <code>true</code> or <code>false</code>.
     *
     * @return The validation result.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets whether the validation was <code>true</code> or <code>false</code>.
     *
     * @param valid The validation result.
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Returns the lifetime in seconds of the access token.
     *
     * @return The lifetime in seconds of the access token.
     */
    public Integer getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the lifetime in seconds of the access token.
     *
     * @param expiresIn The lifetime in seconds of the access token.
     */
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Returns the error code when the request fails, otherwise will return
     * <code>null</code>.
     *
     * @return The error code when the request fails.
     */
    public ValidateTokenErrorResponseType getErrorType() {
        return errorType;
    }

    /**
     * Sets the error code when the request fails, otherwise will return
     * <code>null</code>.
     *
     * @param errorType The error code when the request fails.
     */
    public void setErrorType(ValidateTokenErrorResponseType errorType) {
        this.errorType = errorType;
    }

    /**
     * Returns a human-readable UTF-8 encoded text providing additional
     * information, used to assist the client developer in understanding the
     * error that occurred.
     *
     * @return The error description.
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Sets a human-readable UTF-8 encoded text providing additional
     * information, used to assist the client developer in understanding the
     * error that occurred.
     *
     * @param errorDescription The error description.
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    /**
     * Returns a URI identifying a human-readable web page with information
     * about the error, used to provide the client developer with additional
     * information about the error.
     *
     * @return A URI with information about the error.
     */
    public String getErrorUri() {
        return errorUri;
    }

    /**
     * Sets a URI identifying a human-readable web page with information about
     * the error, used to provide the client developer with additional
     * information about the error.
     *
     * @param errorUri A URI with information about the error.
     */
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }
}