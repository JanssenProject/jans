/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.federation;

import org.xdi.oxauth.model.error.IErrorType;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/09/2012
 */

public enum FederationErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an unsupported
     * parameter or parameter value, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The request value of "federation_id" is invalid or not supported.
     */
    INVALID_FEDERATION_ID("invalid_federation_id"),

    /**
     * The request value of "invalid_entity_type" is invalid or not supported.
     */
    INVALID_ENTITY_TYPE("invalid_entity_type"),

    /**
     * The request value of "invalid_display_name" is invalid or not supported.
     */
    INVALID_DISPLAY_NAME("invalid_display_name"),

    /**
     * The request value of "invalid_op_id" is invalid or not supported.
     */
    INVALID_OP_ID("invalid_op_id"),

    /**
     * The request value of "invalid_domain" is invalid or not supported.
     */
    INVALID_DOMAIN("invalid_domain"),

    /**
     * The request value of "invalid_redirect_uri" is invalid or not supported.
     */
    INVALID_REDIRECT_URI("invalid_redirect_uri"),

    /**
     * The request value of "invalid_x509_url" is invalid or not supported.
     */
    INVALID_X509_URL("invalid_x509_url"),

    /**
     * The request value of "invalid_x509_pem" is invalid or not supported.
     */
    INVALID_X509_PEM("invalid_x509_pem"),

    /**
     * The request is forbidden and is not processed. (Federation does not join new members right now.)
     */
    REQUEST_FORBIDDEN("request_forbidden");

    private final String paramName;

    /**
     * Constructor
     *
     * @param paramName param name
     */
    private FederationErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Return the corresponding enumeration object from a string parameter.
     *
     * @param param The parameter to be match.
     * @return The <code>enumeration</code> if found, otherwise
     *         <code>null</code>.
     */
    public static FederationErrorResponseType fromString(String param) {
        if (param != null) {
            for (FederationErrorResponseType err : FederationErrorResponseType
                    .values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }


    /**
     * Returns a string representation of the object. In this case, the lower
     * case code of the error.
     */
    @Override
    public String toString() {
        return paramName;
    }

    /**
     * Gets error parameter.
     *
     * @return error parameter
     */
    @Override
    public String getParameter() {
        return paramName;
    }
}
