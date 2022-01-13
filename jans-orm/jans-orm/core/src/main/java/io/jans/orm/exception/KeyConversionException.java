/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

/**
 * Exception thrown when a dn to key conversion problem occurs
 *
 * @author Yuriy Movchan Date: 30/05/2018
 */
public class KeyConversionException extends BasePersistenceException {

    private static final long serialVersionUID = -5254637442590218891L;

    public KeyConversionException(String message) {
        super(message);
    }

}
