/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.exception;

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
