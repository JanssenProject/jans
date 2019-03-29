/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.token.TokenRevocationErrorResponseType;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public class TokenRevocationResponse extends BaseResponse {

    private TokenRevocationErrorResponseType errorType;
    private String errorDescription;
    private String errorUri;

    /**
     * Constructs an token revocation response.
     */
    public TokenRevocationResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        if (StringUtils.isNotBlank(entity)) {
            try {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has("error")) {
                    errorType = TokenRevocationErrorResponseType.getByValue(jsonObj.getString("error"));
                }
                if (jsonObj.has("error_description")) {
                    errorDescription = jsonObj.getString("error_description");
                }
                if (jsonObj.has("error_uri")) {
                    errorUri = jsonObj.getString("error_uri");
                }
            } catch (JSONException e) {
                e.printStackTrace();
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
