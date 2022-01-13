/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

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
