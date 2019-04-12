/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import org.gluu.persistence.annotation.LdapAttribute;
import org.gluu.persistence.annotation.LdapEntry;
import org.gluu.persistence.annotation.LdapObjectClass;

/**
 * Provides global inum search ability.
 *
 * @author Oleksiy Tataryn
 */
@Entry
@ObjectClass(values = { "top" })
public class InumEntry extends Entry {

    @Attribute(ignoreDuringUpdate = true)
    private String inum;

    /**
     * @param inum
     *            the inum to set
     */
    public void setInum(String inum) {
        this.inum = inum;
    }

    /**
     * @return the inum
     */
    public String getInum() {
        return inum;
    }

    @Override
    public String toString() {
        return String.format("Entry [dn=%s, inum=%s]", getDn(), getInum());
    }

}
