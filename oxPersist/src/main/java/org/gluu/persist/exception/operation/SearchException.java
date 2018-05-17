/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.operation;

/**
 * Exception thrown when a search problem occurs
 *
 * @author Yuriy Movchan Date: 2017/12/29
 */
public class SearchException extends PersistenceException {

    private static final long serialVersionUID = 5017957214447362606L;

    private int resultCode;

    public SearchException(String message, Throwable ex, int resultCode) {
        super(message, ex);
        this.resultCode = resultCode;
    }

    public SearchException(String message, int resultCode) {
        super(message);
        this.resultCode = resultCode;
    }

    public SearchException(String message) {
        super(message);
    }

    public SearchException(String message, Throwable ex) {
        super(message, ex);
    }

    public SearchException(Throwable ex, int resultCode) {
        super(ex);
        this.resultCode = resultCode;
    }

    public final int getResultCode() {
        return resultCode;
    }

}
