/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.model;

import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.model.attribute.Attribute;
import org.xdi.model.user.UserRole;

/**
 * Attribute Metadata
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version February 9, 2015
 */
@LdapEntry(sortBy = { "displayName" })
@LdapObjectClass(values = { "top", "gluuAttribute" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GluuAttribute extends Attribute {

    private static final long serialVersionUID = 5817004894646725606L;

    private transient boolean selected;

    @Transient
    private boolean custom;

    @Transient
    private boolean requred;

    public final boolean isSelected() {
        return selected;
    }

    public final void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean isRequred() {
        return requred;
    }

    public void setRequred(boolean requred) {
        this.requred = requred;
    }

    public boolean allowEditBy(UserRole role) {
        return UserRole.containsRole(getEditType(), role);
    }

    public boolean allowViewBy(UserRole role) {
        return UserRole.containsRole(getViewType(), role);
    }

    public boolean isAdminCanAccess() {
        return isAdminCanView() | isAdminCanEdit();
    }

    public boolean isAdminCanView() {
        return allowViewBy(UserRole.ADMIN);
    }

    public boolean isAdminCanEdit() {
        return allowEditBy(UserRole.ADMIN);
    }

    public boolean isUserCanAccess() {
        return isUserCanView() | isUserCanEdit();
    }

    public boolean isUserCanView() {
        return allowViewBy(UserRole.USER);
    }

    public boolean isWhitePagesCanView() {
        return allowViewBy(UserRole.WHITEPAGES);
    }

    public boolean isUserCanEdit() {
        return allowEditBy(UserRole.USER);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GluuAttribute other = (GluuAttribute) obj;
        if (custom != other.custom) {
            return false;
        }
        if (requred != other.requred) {
            return false;
        }
        if (selected != other.selected) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (custom ? 1231 : 1237);
        result = prime * result + (requred ? 1231 : 1237);
        result = prime * result + (selected ? 1231 : 1237);
        return result;
    }
   
}
