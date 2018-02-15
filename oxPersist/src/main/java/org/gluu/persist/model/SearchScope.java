/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model;

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
