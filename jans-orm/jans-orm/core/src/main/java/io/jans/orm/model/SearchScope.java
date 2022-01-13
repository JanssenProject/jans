/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

/**
 * LDAP search scope
 *
 * @author Yuriy Movchan Date: 11/18/2016
 */
public enum SearchScope {

    /**
     * A predefined baseObject scope value, which indicates that only the entry
     * specified by the base DN should be considered.
     */
    BASE,

    /**
     * A predefined singleLevel scope value, which indicates that only entries
     * that are immediate subordinates of the entry specified by the base DN
     * (but not the base entry itself) should be considered.
     */
    ONE,

    /**
     * A predefined wholeSubtree scope value, which indicates that the base
     * entry itself and any subordinate entries (to any depth) should be
     * considered.
     */
    SUB;

}
