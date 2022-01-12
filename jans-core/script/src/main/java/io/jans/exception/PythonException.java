/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.exception;

/**
 * Python exception
 *
 * @author Yuriy Movchan Date: 07.10.2012
 */
public class PythonException extends Exception {

    private static final long serialVersionUID = -5416921979568687942L;

    public PythonException(Throwable root) {
        super(root);
    }

    public PythonException(String string, Throwable root) {
        super(string, root);
    }

    public PythonException(String s) {
        super(s);
    }
}
