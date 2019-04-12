/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.io.Serializable;

import org.gluu.persist.annotation.LdapDN;

/**
 * Provides DN attribute
 *
 * @author Yuriy Movchan Date: 07/10/2010
 */
public class Entry implements Serializable, Cloneable {

    private static final long serialVersionUID = 6602706707181973761L;

    @DN
    private String dn;

    public Entry() {
    }

    public Entry(String dn) {
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
        return String.format("Entry [dn=%s]", dn);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dn == null) ? 0 : dn.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Entry other = (Entry) obj;
        if (dn == null) {
            if (other.dn != null) {
                return false;
            }
        } else if (!dn.equals(other.dn)) {
            return false;
        }

        return true;
    }

}
