/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

/**
 * The base {@link Throwable} type for LDAP Mapping.
 */
public class BasePersistenceException extends RuntimeException {

    private static final long serialVersionUID = 1071769232087073304L;

    public BasePersistenceException(Throwable root) {
        super(root);
    }

    public BasePersistenceException(String string, Throwable root) {
        super(string, root);
    }

    public BasePersistenceException(String s) {
        super(s);
    }

}
