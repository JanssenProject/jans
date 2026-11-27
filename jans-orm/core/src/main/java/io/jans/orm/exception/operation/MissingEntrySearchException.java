/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception.operation;

/** A completed lookup found no row for the requested key. */
public final class MissingEntrySearchException extends SearchException {

    private static final long serialVersionUID = 1L;

    public MissingEntrySearchException(String message) {
        super(message);
    }
}
