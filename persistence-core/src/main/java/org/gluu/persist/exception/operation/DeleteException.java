/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.operation;

/**
 * Exception thrown when a delte problem occurs
 *
 * @author Yuriy Movchan Date: 2019/08/26
 */
public class DeleteException extends PersistenceException {

    private static final long serialVersionUID = 5017957214447362606L;

    public DeleteException(String message, Throwable ex, int errorCode) {
        super(message, ex, errorCode);
    }

    public DeleteException(String message, int errorCode) {
        super(message, errorCode);
    }

    public DeleteException(String message) {
        super(message);
    }

    public DeleteException(String message, Throwable ex) {
        super(message, ex);
    }

    public DeleteException(Throwable ex, int errorCode) {
        super(ex, errorCode);
    }

}
