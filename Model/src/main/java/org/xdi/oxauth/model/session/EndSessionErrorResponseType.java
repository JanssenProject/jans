package org.xdi.oxauth.model.session;

import org.xdi.oxauth.model.error.IErrorType;

/**
 * Error codes for End Session error responses.
 *
 * @author Javier Rojas Blum Date: 12.16.2011
 */
public enum EndSessionErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats a
     * parameter, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The provided access token is invalid, or was issued to another client.
     */
    INVALID_GRANT("invalid_grant");

    private final String paramName;

    private EndSessionErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link EndSessionErrorResponseType} from a given string.
     *
     * @param param The string value to convert.
     * @return The corresponding {@link EndSessionErrorResponseType}, otherwise <code>null</code>.
     */
    public static EndSessionErrorResponseType fromString(String param) {
        if (param != null) {
            for (EndSessionErrorResponseType err : EndSessionErrorResponseType
                    .values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     *
     * @return The string representation of the object.
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