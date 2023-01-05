/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.config.Constants;
import io.jans.as.model.token.TokenRevocationErrorResponseType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.core.Response;

/**
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public class TokenRevocationResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(TokenRevocationResponse.class);

    private TokenRevocationErrorResponseType errorType;
    private String errorDescription;
    private String errorUri;

    /**
     * Constructs an token revocation response.
     */
    public TokenRevocationResponse(Response clientResponse) {
        super(clientResponse);

        if (StringUtils.isNotBlank(entity)) {
            try {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(Constants.ERROR)) {
                    errorType = TokenRevocationErrorResponseType.getByValue(jsonObj.getString(Constants.ERROR));
                }
                if (jsonObj.has(Constants.ERROR_DESCRIPTION)) {
                    errorDescription = jsonObj.getString(Constants.ERROR_DESCRIPTION);
                }
                if (jsonObj.has(Constants.ERROR_URI)) {
                    errorUri = jsonObj.getString(Constants.ERROR_URI);
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the error code when the request fails, otherwise will return
     * <code>null</code>.
     *
     * @return The error code when the request fails.
     */
    public TokenRevocationErrorResponseType getErrorType() {
        return errorType;
    }

    /**
     * Sets the error code when the request fails, otherwise will return
     * <code>null</code>.
     *
     * @param errorType The error code when the request fails.
     */
    public void setErrorType(TokenRevocationErrorResponseType errorType) {
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
