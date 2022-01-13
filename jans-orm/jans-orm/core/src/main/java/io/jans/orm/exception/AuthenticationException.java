/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

/**
 * An exception is a result if user don't have required permissions or failed to authenticate him
 *
 * @author Yuriy Movchan Date: 10.26.2010
 */
public class AuthenticationException extends EntryPersistenceException {

    private static final long serialVersionUID = -3321766232087075304L;

    public AuthenticationException(Throwable root) {
        super(root);
    }

    public AuthenticationException(String string, Throwable root) {
        super(string, root);
    }

    public AuthenticationException(String s) {
        super(s);
    }

}
