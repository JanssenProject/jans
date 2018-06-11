/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.operation;

/**
 * Exception thrown when a there is no entry
 *
 * @author Yuriy Movchan Date: 2017/12/29
 */
public class EntryNotFoundException extends PersistenceException {

    private static final long serialVersionUID = 5017957214447362606L;

    private int resultCode;

    public EntryNotFoundException(String message, Throwable ex, int resultCode) {
        super(message, ex);
        this.resultCode = resultCode;
    }

    public EntryNotFoundException(String message, int resultCode) {
        super(message);
        this.resultCode = resultCode;
    }

    public EntryNotFoundException(String message) {
        super(message);
    }

    public EntryNotFoundException(String message, Throwable ex) {
        super(message, ex);
    }

    public EntryNotFoundException(Throwable ex, int resultCode) {
        super(ex);
        this.resultCode = resultCode;
    }

    public final int getResultCode() {
        return resultCode;
    }

}
