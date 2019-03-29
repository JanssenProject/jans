/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.exception;

/**
 * @author Yuriy Movchan
 * @version 0.1, 08/02/2013
 */
public class OxIntializationException extends Exception {

    private static final long serialVersionUID = 2148886143372789053L;

    public OxIntializationException(String message) {
        super(message);
    }

    public OxIntializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
