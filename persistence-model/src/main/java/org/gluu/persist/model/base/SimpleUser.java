/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gluu.persist.annotation.LdapAttribute;
import org.gluu.persist.annotation.@AttributesList;
import org.gluu.persist.annotation.LdapCustomObjectClass;
import org.gluu.persist.annotation.LdapDN;
import org.gluu.persist.annotation.LdapEntry;
import org.gluu.persist.annotation.LdapObjectClass;
import org.gluu.util.StringHelper;

/**
 * @author Javier Rojas Blum Date: 11.25.2011
 */
@Entry
@ObjectClass(values = {"top"})
public class SimpleUser implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;

    @DN
    private String dn;
    @Attribute(name = "uid")
    private String userId;

    @Attribute(name = "oxAuthPersistentJWT")
    private String[] oxAuthPersistentJwt;

    @AttributesList(name = "name", value = "values", sortByName = true)
    protected List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

    @CustomObjectClass
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
