/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapAttributesList;
import org.gluu.site.ldap.persistence.annotation.LdapCustomObjectClass;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.gluu.util.StringHelper;

/**
 * @author Javier Rojas Blum Date: 11.25.2011
 */
@LdapEntry
@LdapObjectClass(values = {"top"})
public class SimpleUser implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;

    @LdapDN
    private String dn;
    @LdapAttribute(name = "uid")
    private String userId;

    @LdapAttribute(name = "oxAuthPersistentJWT")
    private String[] oxAuthPersistentJwt;

    @LdapAttributesList(name = "name", value = "values", sortByName = true)
    protected List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

    @LdapCustomObjectClass
    private String[] customObjectClasses;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String[] getOxAuthPersistentJwt() {
        return oxAuthPersistentJwt;
    }

    public void setOxAuthPersistentJwt(String[] oxAuthPersistentJwt) {
        this.oxAuthPersistentJwt = oxAuthPersistentJwt;
    }

    public List<CustomAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String getAttribute(String ldapAttribute) {
        String attribute = null;
        if (ldapAttribute != null && !ldapAttribute.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (customAttribute.getName().equals(ldapAttribute)) {
                    attribute = customAttribute.getValue();
                    break;
                }
            }
        }

        return attribute;
    }

    public List<String> getAttributeValues(String ldapAttribute) {
        List<String> values = null;
        if (ldapAttribute != null && !ldapAttribute.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), ldapAttribute)) {
                    values = customAttribute.getValues();
                    break;
                }
            }
        }

        return values;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
