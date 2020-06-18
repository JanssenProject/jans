/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.exception;

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
