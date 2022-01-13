/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception;

/**
 * An exception is a result of something screwy in the O-R mappings.
 */
public class MappingException extends BasePersistenceException {

    private static final long serialVersionUID = 1113352885909511209L;

    public MappingException(String msg, Throwable root) {
        super(msg, root);
    }

    public MappingException(Throwable root) {
        super(root);
    }

    public MappingException(String s) {
        super(s);
    }

}
