/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

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
