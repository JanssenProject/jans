/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception.operation;

/**
 * Failed to convert value to DB value with specific type
 *
 * @author Yuriy Movchan Date: 05/04/2021
 */
public class IncompatibleTypeException extends PersistenceException {

    private static final long serialVersionUID = 1321766232087075304L;

    public IncompatibleTypeException(Throwable root) {
        super(root);
    }

    public IncompatibleTypeException(String string, Throwable root) {
        super(string, root);
    }

    public IncompatibleTypeException(String s) {
        super(s);
    }

}
