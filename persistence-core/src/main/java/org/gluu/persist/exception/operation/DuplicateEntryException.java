/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.operation;

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
