/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

/**
 * @author Yuriy Zabrovarnyy
 * @version August 20, 2019
 */
public class DefaultErrorResponse extends ErrorResponse {

    private IErrorType type;

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
    public String getErrorCode() {
        if (type != null)
            return type.toString();
        return super.getErrorCode();
    }
}
