/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.exception;

/**
 * @author Yuriy Movchan Date: 10.25.2010
 */
public class InvalidAttributeException extends RuntimeException {

    private static final long serialVersionUID = -3072969232087073304L;

    public InvalidAttributeException(Throwable root) {
        super(root);
    }

    public InvalidAttributeException(String string, Throwable root) {
        super(string, root);
    }

    public InvalidAttributeException(String s) {
        super(s);
    }

}
