/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception;

/**
 * An exception is a result if server doesn't support operation.
 *
 * @author Yuriy Movchan Date: 08.07.2012
 */
public class UnsupportedOperationException extends BasePersistenceException {

    private static final long serialVersionUID = 2321766232087075304L;

    public UnsupportedOperationException(Throwable root) {
        super(root);
    }

    public UnsupportedOperationException(String string, Throwable root) {
        super(string, root);
    }

    public UnsupportedOperationException(String s) {
        super(s);
    }

}
