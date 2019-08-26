/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception;

/**
 * An exception is a result if LDAP entry defined incorrectly.
 *
 * @author Yuriy Movchan Date: 2019/08/26
 */
public class EntryDeleteException extends BasePersistenceException {

    private static final long serialVersionUID = 1321766232087075304L;

    public EntryDeleteException(Throwable root) {
        super(root);
    }

    public EntryDeleteException(String string, Throwable root) {
        super(string, root);
    }

    public EntryDeleteException(String s) {
        super(s);
    }

}
