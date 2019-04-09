/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception;

/**
 * Indicates that an expected getter or setter method could not be found on a
 * class.
 */
public class PropertyNotFoundException extends MappingException {

    private static final long serialVersionUID = 2351260797243441135L;

    public PropertyNotFoundException(String s) {
        super(s);
    }

}
