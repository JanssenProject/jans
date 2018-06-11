/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception;

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
