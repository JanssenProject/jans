/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

/**
 * An exception is a result if search operation fails
 *
 * @author Yuriy Movchan Date: 2022/11/04
 */
public class SearchEntryException extends BasePersistenceException {

    private static final long serialVersionUID = 1321766232087075304L;

    public SearchEntryException(Throwable root) {
        super(root);
    }

    public SearchEntryException(String string, Throwable root) {
        super(string, root);
    }

    public SearchEntryException(String s) {
        super(s);
    }

}
