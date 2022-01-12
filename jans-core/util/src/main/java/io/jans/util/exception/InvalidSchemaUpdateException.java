/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.exception;

/**
 * @author Yuriy Movchan Date: 10.15.2010
 */
public class InvalidSchemaUpdateException extends RuntimeException {

    private static final long serialVersionUID = 3071969232087073304L;

    public InvalidSchemaUpdateException(Throwable root) {
        super(root);
    }

    public InvalidSchemaUpdateException(String string, Throwable root) {
        super(string, root);
    }

    public InvalidSchemaUpdateException(String s) {
        super(s);
    }

}
