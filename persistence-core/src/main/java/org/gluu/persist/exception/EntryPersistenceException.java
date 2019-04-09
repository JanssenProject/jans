/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception;

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
