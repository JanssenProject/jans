/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.exception;

/**
 * This class represents the configuration error
 *
 * @author Yuriy Movchan 11/13/2014
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 558936139180336683L;

    public ConfigurationException(final String message) {
        super(message);
    }

    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
