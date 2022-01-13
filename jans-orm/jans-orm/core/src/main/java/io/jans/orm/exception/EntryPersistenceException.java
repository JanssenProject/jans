/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

import io.jans.orm.exception.BasePersistenceException;

/**
 * An exception is a result if LDAP entry defined incorrectly.
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public class EntryPersistenceException extends BasePersistenceException {

    private static final long serialVersionUID = 1321766232087075304L;

    public EntryPersistenceException(Throwable root) {
        super(root);
    }

    public EntryPersistenceException(String string, Throwable root) {
        super(string, root);
    }

    public EntryPersistenceException(String s) {
        super(s);
    }

}
