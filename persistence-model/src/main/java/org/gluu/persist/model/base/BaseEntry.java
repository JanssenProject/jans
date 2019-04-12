/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import org.gluu.persist.annotation.LdapDN;

/**
 * Provides DN attribute
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public class BaseEntry {

    @DN
    private String dn;

    public BaseEntry() {
    }

    public BaseEntry(String dn) {
        super();
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getBaseDn() {
        return dn;
    }

    public void setBaseDn(String dn) {
        this.dn = dn;
    }

    @Override
    public String toString() {
        return String.format("BaseEntry [dn=%s]", dn);
    }

}
