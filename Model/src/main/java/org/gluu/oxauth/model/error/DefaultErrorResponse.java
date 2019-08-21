/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.error;

/**
 * @author Yuriy Zabrovarnyy
 * @version August 20, 2019
 */
public class DefaultErrorResponse extends ErrorResponse {

    private IErrorType type;
    private String state;

    /**
     * Returns the error response type.
     *
     * @return The error response type.
     */
    public IErrorType getType() {
        return type;
    }

    /**
     * Sets the {@link IErrorType} that represents the code of the error that occurred.
     *
     * @param type The error response type.
     */
    public void setType(IErrorType type) {
        this.type = type;
    }

    @Override
    public String getState() {
        return state;
    }

    public void setState(String p_state) {
        state = p_state;
    }

    @Override
    public String getErrorCode() {
        if (type != null)
            return type.toString();
        return null;
    }
}
