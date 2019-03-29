/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gluu.oxauth.model.userinfo.UserInfoErrorResponseType;

/**
 * Represents an client info response received from the authorization server.
 *
 * @author Javier Rojas Blum Date: 07.19.2012
 */
public class ClientInfoResponse extends BaseResponse {

    private Map<String, List<String>> claims;

    private UserInfoErrorResponseType errorType;
    private String errorDescription;
    private String errorUri;

    /**
     * Constructs a Client Info response.
     *
     * @param status The response status code.
     */
    public ClientInfoResponse(int status) {
        super(status);
        claims = new HashMap<String, List<String>>();
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    /**
     * Returns the error code when the request fails, otherwise will return <code>null</code>.
     *
     * @return The error code when the request fails.
     */
    public UserInfoErrorResponseType getErrorType() {
        return errorType;
    }

    /**
     * Sets the error code when the request fails, otherwise will return
     * <code>null</code>.
     *
     * @param errorType The error code when the request fails.
     */
    public void setErrorType(UserInfoErrorResponseType errorType) {
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

    public List<String> getClaim(String claimName) {
        if (claims.containsKey(claimName)) {
            return claims.get(claimName);
        }

        return null;
    }
}