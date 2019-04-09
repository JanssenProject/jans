/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.operation;

/**
 * Generic operation layer exception
 *
 * @author Yuriy Movchan Date: 29/01/2018
 */
public class PersistenceException extends Exception {

    /**
     * S Serialization ID
     */
    private static final long serialVersionUID = 1518677859552078437L;

    /**
     * LDAP error code
     */
    private int errorCode = 0;

    /**
     * Constructor with wrapped exception
     *
     * @param e
     *            Wrapped LDAP exception
     */
    public PersistenceException(final Throwable e) {
        super(e);
    }

    /**
     * Constructor with detailed error
     *
     * @param message
     *            Detailed message
     */
    public PersistenceException(final String message) {
        super(message);
    }

    /**
     * Constructor with error and wrapped exception
     *
     * @param message
     *            Detailed error
     * @param e
     *            Wrapped LDAP exception
     */
    public PersistenceException(final String message, final Throwable e) {
        super(message, e);
    }

    /**
     * Get the LDAP error code
     *
     * @return The LDAP error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Set the LDAP error code
     *
     * @param code
     *            Error code
     */
    public void setErrorCode(final int code) {
        errorCode = code;
    }

    /**
     * Get the message for display
     *
     * @return Message for display
     */
    @Override
    public String toString() {
        return getMessage();
    }
}
