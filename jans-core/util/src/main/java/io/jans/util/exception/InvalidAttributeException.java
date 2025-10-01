/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.exception;

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
