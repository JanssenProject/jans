/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception;

/**
 * An exception is a result of something wrong in input parameters.
 */
public class InvalidArgumentException extends MappingException {

    private static final long serialVersionUID = -2223352885909511209L;

    public InvalidArgumentException(String msg, Throwable root) {
        super(msg, root);
    }

    public InvalidArgumentException(Throwable root) {
        super(root);
    }

    public InvalidArgumentException(String s) {
        super(s);
    }

}
