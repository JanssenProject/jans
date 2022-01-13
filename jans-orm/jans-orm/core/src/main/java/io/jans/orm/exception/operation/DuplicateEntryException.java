/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.exception.operation;

/**
 * Duplicate LDAP entry exception
 *
 * @author Pankaj
 */
public class DuplicateEntryException extends PersistenceException {
    /**
     * Serialization ID
     */
    private static final long serialVersionUID = 6749290172742578916L;

    /**
     * Default constructor
     */
    public DuplicateEntryException() {
        super("Entry already exists");
    }

    /**
     * Constructor for returning the offending DN
     *
     * @param dn
     *            DN that returned a duplicate
     */
    public DuplicateEntryException(final String dn) {
        super("Entry already exists: " + dn);
    }
}
