/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception.operation;

/**
 * Failed to convert DB result to entry 
 *
 * @author Yuriy Movchan Date: 01/18/2021
 */
public class EntryConvertationException extends PersistenceException {

    private static final long serialVersionUID = 1321766232087075304L;

    public EntryConvertationException(Throwable root) {
        super(root);
    }

    public EntryConvertationException(String string, Throwable root) {
        super(string, root);
    }

    public EntryConvertationException(String s) {
        super(s);
    }

}
